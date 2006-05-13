/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.jsp.*;

import java.util.*;

import org.apache.commons.lang.*;

/**
 * de.codewave.mytunesrss.command.BrowseTrackCommandHandler
 */
public class BrowseTrackCommandHandler extends MyTunesRssCommandHandler {
    public static enum SortOrder {
        Album(), Artist();
    }

    @Override
    public void executeAuthorized() throws Exception {
        String searchTerm = getRequestParameter("searchTerm", null);
        String album = getRequestParameter("album", null);
        String artist = getRequestParameter("artist", null);
        String trackId = getRequestParameter("track", null);
        FindTrackQuery query = null;
        if (StringUtils.isNotEmpty(trackId)) {
            query = new FindTrackQuery(trackId);
        } else if (StringUtils.isNotEmpty(searchTerm)) {
            query = new FindTrackQuery("%" + searchTerm + "%", FindTrackQuery.Operation.Or);
        } else {
            query = new FindTrackQuery(album, artist);
        }
        getRequest().setAttribute("sortOrder", SortOrder.Album.name());
        getRequest().setAttribute("tracks", getTracks(getDataStore().executeQuery(query), SortOrder.Album));
        forward(MyTunesRssResource.BrowseTrack);
    }

    private Collection<EnhancedTrack> getTracks(Collection<Track> tracks, SortOrder sortOrder) {
        List<EnhancedTrack> enhancedTracks = new ArrayList<EnhancedTrack>(tracks.size());
        String album = getClass().getName();
        String artist = getClass().getName();
        List<EnhancedTrack> sectionTracks = new ArrayList<EnhancedTrack>();
        boolean variousPerSection = false;
        for (Track track : tracks) {
            EnhancedTrack enhancedTrack = new EnhancedTrack(track);
            boolean newAlbum = !album.equals(track.getAlbum());
            boolean newArtist = !artist.equals(track.getArtist());
            if ((sortOrder == SortOrder.Album && newAlbum) || (sortOrder == SortOrder.Artist && newArtist)) { // new section begins
                enhancedTrack.setNewSection(true);
                if (!sectionTracks.isEmpty() && !variousPerSection) { // previous section was simple
                    for (EnhancedTrack rememberedTrack : sectionTracks) {
                        rememberedTrack.setSimple(true);
                    }
                }
                sectionTracks.clear();
                variousPerSection = false;
            } else {
                if ((sortOrder == SortOrder.Album && newArtist) || (sortOrder == SortOrder.Artist && newAlbum)) {
                    variousPerSection = true;
                }
            }
            enhancedTracks.add(enhancedTrack);
            sectionTracks.add(enhancedTrack);
        }
        if (!sectionTracks.isEmpty() && !variousPerSection) { // last section was simple
            for (EnhancedTrack rememberedTrack : sectionTracks) {
                rememberedTrack.setSimple(true);
            }
        }
        return enhancedTracks;
    }

    public static class EnhancedTrack extends Track {
        private boolean myNewSection;
        private boolean mySimple;

        private EnhancedTrack(Track track) {
            setId(track.getId());
            setName(track.getName());
            setAlbum(track.getAlbum());
            setArtist(track.getArtist());
            setTime(track.getTime());
            setTrackNumber(track.getTrackNumber());
            setFile(track.getFile());
        }

        public boolean isNewSection() {
            return myNewSection;
        }

        private void setNewSection(boolean newSection) {
            myNewSection = newSection;
        }

        public boolean isSimple() {
            return mySimple;
        }

        private void setSimple(boolean simple) {
            mySimple = simple;
        }
    }
}