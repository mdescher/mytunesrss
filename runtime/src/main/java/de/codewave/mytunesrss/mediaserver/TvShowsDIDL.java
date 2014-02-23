package de.codewave.mytunesrss.mediaserver;

import de.codewave.mytunesrss.NotYetImplementedException;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.datastore.statement.FindTvShowsQuery;
import de.codewave.mytunesrss.datastore.statement.TvShow;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.DataStoreSession;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.PlaylistContainer;

import java.sql.SQLException;

public class TvShowsDIDL extends MyTunesRssContainerDIDL {

    @Override
    void createDirectChildren(User user, DataStoreSession tx, String oidParams, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws SQLException {
        executeAndProcess(
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

}