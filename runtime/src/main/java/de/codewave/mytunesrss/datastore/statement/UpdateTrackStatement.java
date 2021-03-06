/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.datastore.statement;

import de.codewave.mytunesrss.lucene.LuceneTrack;
import de.codewave.mytunesrss.lucene.UpdateLuceneTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.datastore.statement.UpdateTrackStatement
 */
public class UpdateTrackStatement extends InsertOrUpdateTrackStatement {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateTrackStatement.class);

    public UpdateTrackStatement(TrackSource source, String sourceId) {
        super(source, sourceId);
    }

    @Override
    protected LuceneTrack newLuceneTrack() {
        return new UpdateLuceneTrack();
    }

    @Override
    protected void logError(String id, SQLException e) {
        if (LOG.isErrorEnabled()) {
            LOG.error(String.format("Could not update track with ID \"%s\" in database.", id), e);
        }
    }

    @Override
    protected String getStatementName() {
        return "updateTrack";
    }


}
