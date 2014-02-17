package de.codewave.mytunesrss.mediaserver;

import de.codewave.mytunesrss.NotYetImplementedException;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.DataStoreSession;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.item.Movie;

public class MoviesDIDL extends MyTunesRssDIDLContent {

    private long myTotalMatches;

    @Override
    void createDirectChildren(final User user, DataStoreSession tx, String oidParams, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws Exception {
        myTotalMatches = executeAndProcess(
                tx,
                getQuery(user, oidParams),
                new DataStoreQuery.ResultProcessor<Track>() {
                    public void process(Track track) {
                        addItem(new Movie(getObjectId(track), getParentId(track), track.getName(), "MyTunesRSS", createTrackResource(track, user)));
                    }
                },
                firstResult,
                (int) maxResults
        );
    }

    protected FindTrackQuery getQuery(User user, String oidParams) {
        return FindTrackQuery.getMovies(user);
    }

    protected String getParentId(Track track) {
        return ObjectID.Movies.getValue();
    }

    protected String getObjectId(Track track) {
        return ObjectID.Movie.getValue() + ";" + encode(track.getId());
    }

    @Override
    void createMetaData(User user, DataStoreSession tx, String oidParams) throws Exception {
        throw new NotYetImplementedException();
    }

    @Override
    long getTotalMatches() {
        return myTotalMatches;
    }
}
