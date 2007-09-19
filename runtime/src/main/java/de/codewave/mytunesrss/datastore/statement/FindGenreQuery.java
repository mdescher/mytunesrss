/*
 * Copyright (c) 2007, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.datastore.statement;

import de.codewave.mytunesrss.*;
import de.codewave.utils.sql.*;

import java.sql.*;
import java.util.*;

import org.apache.commons.lang.*;

/**
 * de.codewave.mytunesrss.datastore.statement.FindAlbumQuery
 */
public class FindGenreQuery extends DataStoreQuery<Collection<Genre>> {
    private int myIndex;
    private String myRestrictedPlaylistId;

    public FindGenreQuery(User user, int index) {
       myIndex = index;
        myRestrictedPlaylistId = user.getPlaylistId();
    }

    public Collection<Genre> execute(Connection connection) throws SQLException {
      SmartStatement statement = MyTunesRssUtils.createStatement(connection, "findGenres" + (StringUtils.isEmpty(myRestrictedPlaylistId) ? "" : "Restricted"));
      statement.setInt("index", myIndex);
      statement.setString("restrictedPlaylistId", myRestrictedPlaylistId);
      return execute(statement, new GenreResultBuilder());
    }

    public static class GenreResultBuilder implements ResultBuilder<Genre> {
        private GenreResultBuilder() {
            // intentionally left blank
        }

        public Genre create(ResultSet resultSet) throws SQLException {
            Genre genre = new Genre();
            genre.setName(resultSet.getString("NAME"));
            genre.setTrackCount(resultSet.getInt("TRACK_COUNT"));
            genre.setArtistCount(resultSet.getInt("ARTIST_COUNT"));
            genre.setAlbumCount(resultSet.getInt("ALBUM_COUNT"));
            return genre;
        }
    }
}