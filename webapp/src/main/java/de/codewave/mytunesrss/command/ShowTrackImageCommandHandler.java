/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.mp3.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.command.ShowTrackImageCommandHandler
 */
public class ShowTrackImageCommandHandler extends ShowImageCommandHandler {
    private static final Log LOG = LogFactory.getLog(ShowTrackImageCommandHandler.class);

    @Override
    public void executeAuthorized() throws Exception {
        Image image = null;
        if (!isRequestAuthorized()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Not authorized to request track, sending default MyTunesRSS image.");
            }
        } else {
            String trackId = getRequest().getParameter("track");
            int size = getIntegerRequestParameter("size", 0);
            if (StringUtils.isNotEmpty(trackId)) {
                if (size == 0) {
                    Collection<Track> tracks = getDataStore().executeQuery(FindTrackQuery.getForId(new String[] {trackId}));
                    if (!tracks.isEmpty()) {
                        Track track = tracks.iterator().next();
                        image = ID3Utils.getImage(track);
                    }
                } else {
                    byte[] data = getDataStore().executeQuery(new FindTrackImageQuery(trackId, size));
                    if (data != null && data.length > 0) {
                        image = new Image("image/jpeg", data);
                    }
                }
            }
        }
        if (image == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("No tracks recognized in request or no images found in recognized tracks, sending default MyTunesRSS image.");
            }
            sendDefaultImage(256);
        } else {
            sendImage(image);
        }
    }
}