/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss;

import de.codewave.mytunesrss.task.*;
import org.apache.commons.logging.*;

import java.util.*;

/**
 * de.codewave.mytunesrss.DatabaseWatchdogTask
 */
public class DatabaseWatchdogTask extends TimerTask {
    private static final Log LOG = LogFactory.getLog(DatabaseWatchdogTask.class);

    private int myInterval;
    private Timer myTimer;

    public DatabaseWatchdogTask(Timer timer, int interval) {
        myTimer = timer;
        myInterval = interval;
    }

    public void run() {
        try {
            if (MyTunesRss.DATABASE_BUILDER_TASK.needsUpdate()) {
                MyTunesRss.DATABASE_BUILDER_TASK.execute();
            }
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not automatically update database.", e);
            }
        }
        try {
            myTimer.schedule(this, myInterval * 60000);
        } catch (IllegalStateException e) {
            // timer was cancelled, so we just don't schedule any further tasks
        }
    }
}