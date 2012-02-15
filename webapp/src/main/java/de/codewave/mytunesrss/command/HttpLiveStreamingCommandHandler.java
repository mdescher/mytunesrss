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
                sender = new StatusCodeSender(HttpServletResponse.SC_CONFLICT, "QUOTA_EXCEEDED");
            } else {
                sender = new FileSender(mediaFile, "video/MP2T", mediaFile.length());
                sender.setCounter(new MyTunesRssSendCounter(getAuthUser(), trackId, SessionManager.getSessionInfo(getRequest())));
            }
        } else {
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
                    InputStream mediaStream = MyTunesRssWebUtils.getMediaStream(getRequest(), track, track.getFile());
                    MyTunesRss.EXECUTOR_SERVICE.execute(new HttpLiveStreamingSegmenterRunnable(cacheItem.getPlaylist(playlistIdentifier), mediaStream));
                    MyTunesRss.HTTP_LIVE_STREAMING_CACHE.add(cacheItem);
                    getTransaction().executeStatement(new UpdatePlayCountAndDateStatement(new String[]{trackId}));
                    getTransaction().commit();
                    getAuthUser().playLastFmTrack(track);
                }
                playlist = cacheItem.getPlaylist(playlistIdentifier);
                // wait for at least 3 playlist items
                try {
                    long timeSlept = 0;
                    while (!playlist.isFailed() && !playlist.isDone() && playlist.getSize() < 3 && timeSlept < 30000) {
                        Thread.sleep(500);
                        timeSlept += 500;
                    }
                } catch (InterruptedException e) {
                    // we have been interrupted, so send the playlist file or an error now
                }
                if (playlist.isFailed() || playlist.getSize() == 0) {
                    cacheItem.removePlaylist(playlistIdentifier);
                    sender = new StatusCodeSender(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    byte[] playlistBytes = playlist.getAsString().getBytes("ISO-8859-1");
                    sender = new StreamSender(new ByteArrayInputStream(playlistBytes), "application/x-mpegURL", playlistBytes.length);
                }

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

        private InputStream myStream;

        public HttpLiveStreamingSegmenterRunnable(HttpLiveStreamingPlaylist playlist, InputStream stream) {
            myPlaylist = playlist;
            myStream = stream;
        }

        public void run() {
            try {
                List<String> command = new ArrayList<String>();
                command.add(getJavaExecutablePath());
                command.add("-Djna.library.path=" + MyTunesRss.PREFERENCES_DATA_PATH + "/native" + System.getProperty("path.separator") + MyTunesRssUtils.getNativeLibPath().getAbsolutePath());
                command.add("-cp");
                command.add(getClasspath());
                command.add("de.codewave.jna.ffmpeg.HttpLiveStreamingSegmenter");
                command.add(myPlaylist.getBaseDir().getAbsolutePath());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Executing HTTP Live Streaming command \"" + StringUtils.join(command, " ") + "\".");
                }
                BufferedReader reader = null;
                Process process = null;
                try {
                    process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
                    new LogStreamCopyThread(process.getErrorStream(), false, LoggerFactory.getLogger(getClass()), LogStreamCopyThread.LogLevel.Debug).start();
                    new StreamCopyThread(myStream, true, process.getOutputStream(), true).start();
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    final BufferedReader finalReader = reader;
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                for (String responseLine = finalReader.readLine(); responseLine != null; responseLine = finalReader.readLine()) {
                                    if (responseLine.startsWith(myPlaylist.getBaseDir().getAbsolutePath())) {
                                        myPlaylist.addFile(new File(StringUtils.trimToEmpty(responseLine)));
                                    } else if (responseLine.startsWith("ERR")) {
                                        if (LOG.isErrorEnabled()) {
                                            LOG.error("HTTP Live Streaming segmenter error: " + responseLine);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOG.warn("HTTP Live Streaming segmenter output reader thread exited with error.", e);
                            }
                        }
                    }).start();
                    process.waitFor();
                    if (process.exitValue() == 0) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Segmenter process exited with code 0.");
                        }
                        myPlaylist.setDone(true);
                    } else {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Segmenter process exited with code " + process.exitValue() + ".");
                        }
                        myPlaylist.setFailed(true);
                    }
                } catch (IOException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Segmenter exception", e);
                    }
                    myPlaylist.setFailed(true);
                } catch (InterruptedException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Segmenter thread interrupted exception", e);
                    }
                    myPlaylist.setFailed(true);
                } finally {
                    IOUtils.closeQuietly(reader);
                    if (process != null) {
                        process.destroy();
                    }
                }
            } catch (Exception e) {
                LOG.error("Error in http live streaming thread.", e);
            }
        }

        private String getJavaExecutablePath() {
            return System.getProperty("java.home") + "/bin/java";
        }

        private String getClasspath() {
            StringBuilder sb = new StringBuilder();
            for (String cpElement : StringUtils.split(System.getProperty("java.class.path"), System.getProperty("path.separator"))) {
                if (!cpElement.startsWith(System.getProperty("java.home"))) {
                    sb.append(cpElement).append(System.getProperty("path.separator"));
                }
            }
            return sb.substring(0, sb.length() - 1);
        }
    }
}

