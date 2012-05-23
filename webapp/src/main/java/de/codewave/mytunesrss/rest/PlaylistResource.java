/*
 * Copyright (c) 2012. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.rest;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.servlet.TransactionFilter;
import de.codewave.utils.sql.DataStoreQuery;
import org.hibernate.validator.constraints.NotBlank;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.spi.validation.ValidateRequest;

import javax.ws.rs.*;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ValidateRequest
@Path("playlist/{playlist}")
public class PlaylistResource extends RestResource {

    @GET
    @Path("tracks")
    @Produces({"application/json"})
    @GZIP
    public List<Track> getTracks(
            @PathParam("playlist") @NotBlank(message = "Playlist id must not be blank.") String playlist,
            @QueryParam("sort") @DefaultValue("KeepOrder") SortOrder sortOrder
    ) throws SQLException {
        DataStoreQuery.QueryResult<Track> queryResult = TransactionFilter.getTransaction().executeQuery(new FindPlaylistTracksQuery(getAuthUser(), playlist, sortOrder));
        return queryResult.getResults();
    }

    @GET
    @Path("playlists")
    @Produces({"application/json"})
    @GZIP
    public List<Playlist> getPlaylistChildren(
            @PathParam("playlist") @NotBlank(message = "Playlist id must not be blank.") String playlist,
            @QueryParam("hidden") @DefaultValue("false") boolean includeHidden,
            @QueryParam("owner") @DefaultValue("false") boolean matchingOwner,
            @QueryParam("type") List<PlaylistType> types
    ) throws SQLException {
        DataStoreQuery.QueryResult<Playlist> queryResult = TransactionFilter.getTransaction().executeQuery(new FindPlaylistQuery(getAuthUser(), types, null, playlist, includeHidden, matchingOwner));
        return queryResult.getResults();
    }

    @GET
    @Path("tags")
    @Produces({"application/json"})
    public List<String> getTags(
            @PathParam("playlist") @NotBlank(message = "Playlist id must not be blank.") String playlist
    ) throws SQLException {
        DataStoreQuery.QueryResult<String> queryResult = TransactionFilter.getTransaction().executeQuery(new FindAllTagsForPlaylistQuery(playlist));
        return queryResult.getResults();
    }

    @POST
    @Path("tags")
    @Consumes("application/x-www-form-urlencoded")
    public void getTags(
            @PathParam("playlist") String playlist,
            @FormParam("tag") List<String> tags
    ) throws SQLException {
        for (String tag : tags) {
            TransactionFilter.getTransaction().executeStatement(new SetTagToTracksStatement(getTracks(playlist), tag));
        }
    }

    private String[] getTracks(String playlist) throws SQLException {
        List<Track> tracks = getTracks(playlist, SortOrder.KeepOrder);
        Set<String> trackIds = new HashSet<String>();
        for (Track track : tracks) {
            trackIds.add(track.getId());
        }
        return trackIds.toArray(new String[trackIds.size()]);
    }

    @DELETE
    @Path("tags")
    @Consumes("application/x-www-form-urlencoded")
    public void deleteTags(
            @PathParam("playlist") String playlist,
            @FormParam("tag") List<String> tags
    ) throws SQLException {
        for (String tag : tags) {
            TransactionFilter.getTransaction().executeStatement(new RemoveTagFromTracksStatement(getTracks(playlist), tag));
        }
    }
}
