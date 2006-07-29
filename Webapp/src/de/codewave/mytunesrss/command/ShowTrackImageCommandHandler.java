/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.mp3.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import javax.servlet.http.*;
import java.util.*;
import java.io.*;

/**
 * de.codewave.mytunesrss.command.ShowTrackImageCommandHandler
 */
public class ShowTrackImageCommandHandler extends MyTunesRssCommandHandler {
    private static final Log LOG = LogFactory.getLog(ShowTrackImageCommandHandler.class);
    private static byte[] theDefaultImage;

    static {
        InputStream inputStream = MyTunesRssCommandHandler.class.getClassLoader().getResourceAsStream("de/codewave/mytunesrss/default_rss_image.png");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            for (int dataByte = inputStream.read(); dataByte > -1 && dataByte < 256; dataByte = inputStream.read()) {
                outputStream.write(dataByte);
            }
            theDefaultImage = outputStream.toByteArray();
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not copy default image data into byte array.", e);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Could not close input stream.", e);
                }
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Could not close output stream.", e);
                }
            }
        }
    }

    @Override
    public void execute() throws Exception {
        Image image = null;
        if (needsAuthorization()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Not authorized to request track, sending default MyTunesRSS image.");
            }
        } else {
            String trackId = getRequest().getParameter("track");
            if (StringUtils.isNotEmpty(trackId)) {
                Collection<Track> tracks = getDataStore().executeQuery(FindTrackQuery.getForId(new String[] {trackId}));
                if (!tracks.isEmpty()) {
                    Track track = tracks.iterator().next();
                    image = ID3Utils.getImage(track);
                }
            }
        }
        if (image == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("No tracks recognized in request or no images found in recognized tracks, sending default MyTunesRSS image.");
            }
            if (theDefaultImage != null) {
                getResponse().setContentType("image/png");
                getResponse().setContentLength(theDefaultImage.length);
                getResponse().getOutputStream().write(theDefaultImage);
            } else {
                getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } else {
            getResponse().setContentType(image.getMimeType());
            // it seems the first byte is always invalid, I don't know why, maybe a bug in the ID3 tag lib
            getResponse().setContentLength(image.getData().length - 1);
            getResponse().getOutputStream().write(image.getData(), 1, image.getData().length - 1);
        }
    }
}