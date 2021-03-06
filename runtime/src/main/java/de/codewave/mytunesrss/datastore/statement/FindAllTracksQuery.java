/*
 * Copyright (c) 2014. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.datastore.statement;

import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.config.MediaType;
import de.codewave.mytunesrss.config.User;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.QueryResult;
import de.codewave.utils.sql.SmartStatement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindAllTracksQuery extends DataStoreQuery<QueryResult<Track>> {
    private SortOrder mySortOrder;
    private List<String> myRestrictedPlaylistIds = Collections.emptyList();
    private List<String> myExcludedPlaylistIds = Collections.emptyList();
    private MediaType[] myMediaTypes;
    private String[] myPermittedDataSources;
    private Integer myOffset;
    private Integer myCount;

    public FindAllTracksQuery(SortOrder sortOrder) {
        mySortOrder = sortOrder;
    }

    public FindAllTracksQuery(User user, SortOrder sortOrder) {
        this(sortOrder);
        myRestrictedPlaylistIds = user.getRestrictedPlaylistIds();
        myExcludedPlaylistIds = user.getExcludedPlaylistIds();
        myMediaTypes = FindTrackQuery.getQueryMediaTypes(user);
        myPermittedDataSources = FindTrackQuery.getPermittedDataSources(user);
    }

    public FindAllTracksQuery(User user, SortOrder sortOrder, int offset, int count) {
        this(sortOrder);
        myOffset = offset;
        myCount = count;
        myRestrictedPlaylistIds = user.getRestrictedPlaylistIds();
        myExcludedPlaylistIds = user.getExcludedPlaylistIds();
        myMediaTypes = FindTrackQuery.getQueryMediaTypes(user);
        myPermittedDataSources = FindTrackQuery.getPermittedDataSources(user);
    }

    @Override
    public QueryResult<Track> execute(Connection connection) throws SQLException {
        SmartStatement statement;
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("restricted", !myRestrictedPlaylistIds.isEmpty());
        conditionals.put("excluded", !myExcludedPlaylistIds.isEmpty());
        conditionals.put("mediatype", myMediaTypes != null && myMediaTypes.length > 0);
        conditionals.put("datasource", myPermittedDataSources != null);
        conditionals.put("range", myOffset != null || myCount != null);
        conditionals.put("indexorder", mySortOrder != SortOrder.Album && mySortOrder != SortOrder.Artist);
        conditionals.put("albumorder", mySortOrder == SortOrder.Album);
        conditionals.put("artistorder", mySortOrder == SortOrder.Artist);
        statement = MyTunesRssUtils.createStatement(connection, "findAllTracks", conditionals);
        statement.setItems("restrictedPlaylistIds", myRestrictedPlaylistIds);
        statement.setItems("excludedPlaylistIds", myExcludedPlaylistIds);
        statement.setItems("datasources", myPermittedDataSources);
        if (myOffset != null || myCount != null) {
            statement.setInt("rangeOffset", myOffset != null ? myOffset : 0);
            statement.setInt("rangeCount", myCount != null ? myCount : Integer.MAX_VALUE);
        }
        FindTrackQuery.setQueryMediaTypes(statement, myMediaTypes);
        return execute(statement, new TrackResultBuilder());
    }
}
