/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssSendCounter;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.MyTunesRssWebUtils;
import de.codewave.mytunesrss.config.MediaType;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.datastore.statement.UpdatePlayCountAndDateStatement;
import de.codewave.mytunesrss.httplivestreaming.HttpLiveStreamingCacheItem;
import de.codewave.mytunesrss.httplivestreaming.HttpLiveStreamingPlaylist;
import de.codewave.mytunesrss.transcoder.Transcoder;
import de.codewave.utils.io.LogStreamCopyThread;
import de.codewave.utils.io.StreamCopyThread;
import de.codewave.utils.servlet.FileSender;
import de.codewave.utils.servlet.SessionManager;
import de.codewave.utils.servlet.StreamSender;
import de.codewave.utils.sql.DataStoreQuery;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HttpLiveStreamingCommandHandler extends MyTunesRssCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpLiveStreamingCommandHandler.class);

    @Override
    public void executeAuthorized() throws IOException, SQLException {
        String trackId = getRequestParameter("track", null);
        if (StringUtils.isBlank(trackId)) {
            getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "missing track id");
        }
        String[] pathInfo = StringUtils.split(getRequest().getPathInfo(), '/');
        if (pathInfo.length > 1) {
            if (StringUtils.endsWithIgnoreCase(pathInfo[pathInfo.length - 1], ".ts")) {
                sendMediaFile(trackId, pathInfo[pathInfo.length - 2], pathInfo[pathInfo.length - 1]);
            } else {
                sendPlaylist(trackId);
            }
        } else {
            getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void sendMediaFile(String trackId, String dirname, String filename) throws IOException {
        StreamSender sender;
        MyTunesRss.HTTP_LIVE_STREAMING_CACHE.touch(trackId);
        File mediaFile = new File(getBaseDir(), dirname + "/" + filename);
        if (mediaFile.isFile()) {
            if (getAuthUser().isQuotaExceeded()) {
                LOG.debug("Sending 409 QUOTA_EXCEEDED response.");
                sender = new StatusCodeSender(HttpServletResponse.SC_CONFLICT, "QUOTA_EXCEEDED");
            } else {
                sender = new FileSender(mediaFile, "video/MP2T", mediaFile.length());
                LOG.debug("Sending video/MP2T response \"" + mediaFile.getAbsolutePath() + "\".");
                sender.setCounter(new MyTunesRssSendCounter(getAuthUser(), trackId, SessionManager.getSessionInfo(getRequest())));
            }
        } else {
            LOG.debug("Sending 404 NOT_FOUND response.");
            sender = new StatusCodeSender(HttpServletResponse.SC_NOT_FOUND);
        }
        sender.sendGetResponse(getRequest(), getResponse(), false);
    }

    private File getBaseDir() {
        return new File(MyTunesRss.CACHE_DATA_PATH, MyTunesRss.CACHEDIR_HTTP_LIVE_STREAMING);
    }

    private void sendPlaylist(String trackId) throws SQLException, IOException {
        StreamSender sender;
        DataStoreQuery.QueryResult<Track> tracks = getTransaction().executeQuery(FindTrackQuery.getForIds(new String[]{trackId}));
        if (tracks.getResultSize() > 0) {
            Track track = tracks.nextResult();
            if (track.getMediaType() == MediaType.Video) {
                Transcoder transcoder = MyTunesRssWebUtils.getTranscoder(getRequest(), track);
                String playlistIdentifier = transcoder != null ? transcoder.getTranscoderId() : "";
                HttpLiveStreamingCacheItem cacheItem = MyTunesRss.HTTP_LIVE_STREAMING_CACHE.get(trackId);
                if (cacheItem == null) {
                    MyTunesRss.HTTP_LIVE_STREAMING_CACHE.putIfAbsent(new HttpLiveStreamingCacheItem(trackId, 3600000)); // TODO: timeout configuration?
                    cacheItem = MyTunesRss.HTTP_LIVE_STREAMING_CACHE.get(trackId);
                }
                HttpLiveStreamingPlaylist playlist = cacheItem.getPlaylist(playlistIdentifier);
                if (playlist == null && cacheItem.putIfAbsent(playlistIdentifier, new HttpLiveStreamingPlaylist(new File(getBaseDir(), UUID.randomUUID().toString())))) {
                    MyTunesRss.EXECUTOR_SERVICE.execute(new HttpLiveStreamingSegmenterRunnable(cacheItem.getPlaylist(playlistIdentifier), track.getFile()));
                    MyTunesRss.HTTP_LIVE_STREAMING_CACHE.add(cacheItem);
                    try {
                        getTransaction().executeStatement(new UpdatePlayCountAndDateStatement(new String[]{trackId}));
                    } finally {
                        getTransaction().commit();
                    }
                    getAuthUser().playLastFmTrack(track);
                }
                playlist = cacheItem.getPlaylist(playlistIdentifier);
                File playlistFile = new File(playlist.getBaseDir(), "playlist.m3u8");
                while (!playlistFile.exists()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOG.debug("Interrupted while waiting for file.");
                    }
                }
                byte[] playlistBytes = FileUtils.readFileToByteArray(playlistFile);
                LOG.debug("Sending playlist: " + new String(playlistBytes));
                sender = new StreamSender(new ByteArrayInputStream(playlistBytes), "application/x-mpegURL", playlistBytes.length);
            } else {
                sender = new StatusCodeSender(HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            sender = new StatusCodeSender(HttpServletResponse.SC_NOT_FOUND);
        }
        sender.sendGetResponse(getRequest(), getResponse(), false);
    }

    public class HttpLiveStreamingSegmenterRunnable implements Runnable {

        private HttpLiveStreamingPlaylist myPlaylist;

        private File myVideoFile;

        public HttpLiveStreamingSegmenterRunnable(HttpLiveStreamingPlaylist playlist, File videoFile) {
            myPlaylist = playlist;
            myVideoFile = videoFile;
        }

        public void run() {
            Process process = null;
            try {
                final String[] transcoderCommand = new String[] {
                        MyTunesRss.CONFIG.getVlcExecutable().getAbsolutePath(),
                        myVideoFile.getAbsolutePath(),
                        "vlc://quit",
                        "--intf=telnet",
                        "--sout-transcode-audio-sync",
                        "--sout=#transcode{height=320,canvas-aspect=1.5:1,vb=768,vcodec=h264,venc=x264{aud,profile=baseline,level=30,keyint=30,bframes=0,ref=1,nocabac},acodec=mp3,ab=128,samplerate=44100,channels=2,deinterlace,audio-sync}:std{access=livehttp{seglen=10,index=" + myPlaylist.getBaseDir().getAbsolutePath() + "/playlist.m3u8" + ",index-url=" + myPlaylist.getBaseDir().getName() + "/stream-########.ts},mux=ts{use-key-frames},dst=" + myPlaylist.getBaseDir().getAbsolutePath() + "/stream-########.ts}"
                };
                if (LOG.isDebugEnabled()) {
                    LOG.debug("executing HTTP Live Streaming command \"" + StringUtils.join(transcoderCommand, " ") + "\".");
                }
                process = new ProcessBuilder(transcoderCommand).start();
                new LogStreamCopyThread(process.getInputStream(), false, LoggerFactory.getLogger(getClass()), LogStreamCopyThread.LogLevel.Debug).start();
                new LogStreamCopyThread(process.getErrorStream(), false, LoggerFactory.getLogger(getClass()), LogStreamCopyThread.LogLevel.Debug).start();
                process.waitFor();
            } catch (Exception e) {
                LOG.error("Error in http live streaming thread.", e);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }
}

