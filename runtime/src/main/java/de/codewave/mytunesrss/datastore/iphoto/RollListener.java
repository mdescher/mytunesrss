/*
 * Copyright (c) 2011. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.datastore.iphoto;

import de.codewave.utils.sql.DataStoreSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * de.codewave.mytunesrss.datastore.itunes.PlaylistListenerr
 */
public class RollListener extends AlbumListener {
    private static final Logger LOG = LoggerFactory.getLogger(RollListener.class);

    public RollListener(Thread watchdogThread, DataStoreSession dataStoreSession, LibraryListener libraryListener, Map<Long, String> photoIdToPersId) {
        super(watchdogThread, dataStoreSession, libraryListener, photoIdToPersId);
    }


    @Override
    protected String getAlbumName(Map roll) {
        return (String) roll.get("RollName");
    }

    @Override
    protected String getAlbumId(Map roll) {
        return myLibraryListener.getLibraryId() + "_" + roll.get("RollID");
    }
}
