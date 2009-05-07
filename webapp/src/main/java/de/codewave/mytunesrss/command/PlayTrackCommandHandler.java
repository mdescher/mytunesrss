/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.camel.mp3.Id3Tag;
import de.codewave.camel.mp3.Id3v2Tag;
import de.codewave.camel.mp3.Mp3Utils;
import de.codewave.camel.mp3.exception.IllegalHeaderException;
import de.codewave.mytunesrss.FileSupportUtils;
import de.codewave.mytunesrss.MediaType;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssSendCounter;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.datastore.statement.UpdatePlayCountAndDateStatement;
import de.codewave.mytunesrss.transcoder.Transcoder;
import de.codewave.utils.servlet.*;
import de.codewave.utils.sql.DataStoreQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.command.PlayTrackCommandHandler
 */
public class PlayTrackCommandHandler extends MyTunesRssCommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PlayTrackCommandHandler.class);

    @Override
    public void executeAuthorized() throws IOException, SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(ServletUtils.getRequestInfo(getRequest()));
        }
        StreamSender streamSender;
        Track track = null;
        if (!isRequestAuthorized()) {
            streamSender = new StatusCodeSender(HttpServletResponse.SC_NO_CONTENT);
        } else {
            String trackId = getRequest().getParameter("track");
            DataStoreQuery.QueryResult<Track> tracks = getTransaction().executeQuery(FindTrackQuery.getForId(new String[] {trackId}));
            if (tracks.getResultSize() > 0) {
                track = tracks.nextResult();
                if (!getAuthUser().isQuotaExceeded()) {
                    File file = track.getFile();
                    String contentType = track.getContentType();
                    if (!file.exists()) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Requested file \"" + file.getAbsolutePath() + "\" does not exist.");
                        }
                        MyTunesRss.ADMIN_NOTIFY.notifyMissingFile(track);
                        streamSender = new StatusCodeSender(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        Transcoder transcoder = "false".equals(getRequestParameter("notranscode", "false")) ? Transcoder.createTranscoder(track, getWebConfig(), getRequest()) :
                                null;
                        if (transcoder != null) {
                            streamSender = transcoder.getStreamSender();
                        } else {
                            streamSender = new FileSender(file, contentType, (int)file.length());
                        }
                        getTransaction().executeStatement(new UpdatePlayCountAndDateStatement(new String[] {track.getId()}));
                        streamSender.setCounter(new MyTunesRssSendCounter(getAuthUser(), SessionManager.getSessionInfo(getRequest())));
                        getAuthUser().playLastFmTrack(track);
                    }
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("User limit exceeded, sending response code SC_NO_CONTENT instead.");
                    }
                    MyTunesRss.ADMIN_NOTIFY.notifyQuotaExceeded(getAuthUser());
                    streamSender = new StatusCodeSender(HttpServletResponse.SC_NO_CONTENT);
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("No tracks recognized in request, sending response code SC_NO_CONTENT instead.");
                }
                streamSender = new StatusCodeSender(HttpServletResponse.SC_NO_CONTENT);
            }
        }
        getTransaction().commit();
        if (ServletUtils.isHeadRequest(getRequest())) {
            streamSender.sendHeadResponse(getRequest(), getResponse());
        } else {
            int bitrate = 0;
            int dataOffset = 0;
            if (track != null && track.getFile().exists() && FileSupportUtils.isMp3(track.getFile())) {
                bitrate = Mp3Utils.getMp3Info(new FileInputStream(track.getFile())).getAvgBitrate();
                Id3Tag tag = null;
                try {
                    tag = Mp3Utils.readId3Tag(track.getFile());
                    if (tag != null && tag.isId3v2()) {
                        dataOffset = ((Id3v2Tag)tag).getHeader().getBodySize();
                    }
                } catch (IllegalHeaderException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Could not read ID3 information.", e);
                    }
                }
            }
            double factor = MyTunesRss.CONFIG.isBandwidthLimit() ? MyTunesRss.CONFIG.getBandwidthLimitFactor().doubleValue() : 0;
            streamSender.setOutputStreamWrapper(getAuthUser().getOutputStreamWrapper((int)(bitrate * factor),
                                                                                     dataOffset,
                                                                                     new RangeHeader(getRequest())));
            streamSender.sendGetResponse(getRequest(), getResponse(), false);
        }
    }

    private static class StatusCodeSender extends StreamSender {
        private int myStatusCode;

        public StatusCodeSender(int statusCode) throws MalformedURLException {
            super(null, null, -1);
            myStatusCode = statusCode;
        }

        @Override
        public void sendGetResponse(HttpServletRequest servletRequest, HttpServletResponse httpServletResponse, boolean throwException)
                throws IOException {
            httpServletResponse.setStatus(myStatusCode);
        }

        @Override
        public void sendHeadResponse(HttpServletRequest servletRequest, HttpServletResponse httpServletResponse) {
            httpServletResponse.setStatus(myStatusCode);
        }
    }
}