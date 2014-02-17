package de.codewave.mytunesrss.mediaserver;

import de.codewave.mytunesrss.NotYetImplementedException;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.datastore.statement.FindTvShowsQuery;
import de.codewave.mytunesrss.datastore.statement.TvShow;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.DataStoreSession;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.PlaylistContainer;

public class TvShowsDIDL extends MyTunesRssDIDLContent {

    private long myTotalMatches;

    @Override
    void createDirectChildren(User user, DataStoreSession tx, String oidParams, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws Exception {
        myTotalMatches = executeAndProcess(
                tx,
                new FindTvShowsQuery(user),
                new DataStoreQuery.ResultProcessor<TvShow>() {
                    public void process(TvShow tvShow) {
                        addContainer(new PlaylistContainer(ObjectID.TvShow.getValue() + ";" + encode(tvShow.getName()), ObjectID.TvShows.getValue(), tvShow.getName(), "MyTunesRSS", tvShow.getSeasonCount()));
                    }
                },
                firstResult,
                (int)maxResults
        );
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
