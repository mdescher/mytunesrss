/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.utils.sql.*;

/**
 * de.codewave.mytunesrss.command.DeletePlaylistCommandHandler
 */
public class DeletePlaylistCommandHandler extends MyTunesRssCommandHandler {
    @Override
    public void executeAuthorized() throws Exception {
        String playlistId = getRequestParameter("playlist", null);
        DataStoreSession storeSession = getDataStore().getTransaction();
        storeSession.begin();
        storeSession.executeStatement(new DeletePlaylistStatement(playlistId));
        storeSession.commit();
        forward(MyTunesRssCommand.ShowPlaylistManager);
    }
}