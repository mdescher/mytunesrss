/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
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
public class FindAlbumQuery extends DataStoreQuery<Collection<Album>> {

    private String myArtist;
    private String myGenre;
    private int myIndex;
    private String myRestrictedPlaylistId;

    public FindAlbumQuery(User user, String artist, String genre, int index) {
        myArtist = artist;
        myGenre = genre;
        myIndex = index;
        myRestrictedPlaylistId = user.getPlaylistId();
    }

    public Collection<Album> execute(Connection connection) throws SQLException {
        SmartStatement statement = MyTunesRssUtils.createStatement(connection, "findAlbums" + (StringUtils.isEmpty(myRestrictedPlaylistId) ? "" : "Restricted"));
        statement.setString("artist", myArtist);
        statement.setString("genre", myGenre);
        statement.setInt("index", myIndex);
        statement.setString("restrictedPlaylistId", myRestrictedPlaylistId);
        return execute(statement, new AlbumResultBuilder());
    }

    public static class AlbumResultBuilder implements ResultBuilder<Album> {
        private AlbumResultBuilder() {
            // intentionally left blank
        }

        public Album create(ResultSet resultSet) throws SQLException {
            Album album = new Album();
            album.setName(resultSet.getString("ALBUMNAME"));
            album.setTrackCount(resultSet.getInt("TRACK_COUNT"));
            album.setArtistCount(resultSet.getInt("ARTIST_COUNT"));
            album.setArtist(resultSet.getString("ARTIST"));
            album.setImage(resultSet.getInt("IMAGECOUNT") > 0);
            return album;
        }
    }
}