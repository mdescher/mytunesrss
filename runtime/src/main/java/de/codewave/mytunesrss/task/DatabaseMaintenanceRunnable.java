/*
 * Copyright (c) 2013. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.task;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.datastore.statement.MaintenanceStatement;
import de.codewave.mytunesrss.datastore.statement.RecreateHelpTablesStatement;
import de.codewave.mytunesrss.datastore.statement.RefreshSmartPlaylistsStatement;
import de.codewave.mytunesrss.event.MyTunesRssEvent;
import de.codewave.mytunesrss.event.MyTunesRssEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.task.BackupDatabaseRunnable
 */
public class DatabaseMaintenanceRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMaintenanceRunnable.class);

    @Override
    public void run() {
        MyTunesRss.EXECUTOR_SERVICE.cancelImageGenerators();
        try {
            MyTunesRssEvent event = MyTunesRssEvent.create(MyTunesRssEvent.EventType.DATABASE_UPDATE_STATE_CHANGED, "event.databaseMaintenance");
            MyTunesRssEventManager.getInstance().fireEvent(event);
            MyTunesRssEventManager.getInstance().fireEvent(MyTunesRssEvent.create(MyTunesRssEvent.EventType.MAINTENANCE_START));
            MyTunesRss.LAST_DATABASE_EVENT.set(event);
            if (MyTunesRss.CONFIG.isDefaultDatabase()) {
                MyTunesRssUtils.backupDatabase();
                File file = MyTunesRssUtils.exportDatabase();
                try {
                    MyTunesRssUtils.importDatabase(file);
                } finally {
                    if (!file.delete()) {
                        LOGGER.warn("Could not delete database export file \"" + file.getAbsolutePath() + "\".");
                        file.deleteOnExit();
                    }
                }
            }
            MyTunesRss.STORE.executeStatement(new RecreateHelpTablesStatement(true, true, true, true));
            MyTunesRss.STORE.executeStatement(new RefreshSmartPlaylistsStatement(RefreshSmartPlaylistsStatement.UpdateType.DEFAULT));
            MyTunesRss.STORE.executeStatement(new MaintenanceStatement());
        } catch (SQLException | IOException e) {
            LOGGER.error("Error during database maintenance.", e);
        } finally {
            MyTunesRssEventManager.getInstance().fireEvent(MyTunesRssEvent.create(MyTunesRssEvent.EventType.MAINTENANCE_STOP));
            MyTunesRssEventManager.getInstance().fireEvent(MyTunesRssEvent.create(MyTunesRssEvent.EventType.DATABASE_UPDATE_FINISHED));
            MyTunesRss.EXECUTOR_SERVICE.scheduleImageGenerators();
        }
    }
}