package de.codewave.mytunesrss;

import de.codewave.mytunesrss.datastore.statement.SaveTempPlaylistStatement;
import de.codewave.mytunesrss.datastore.statement.SortOrder;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.sql.DataStoreSession;
import de.codewave.utils.sql.DataStoreStatement;
import de.codewave.utils.sql.SmartStatement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * de.codewave.mytunesrss.TrackUtils
 */
public class TrackUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackUtils.class);

    public static EnhancedTracks getEnhancedTracks(DataStoreSession transaction, List<Track> tracks, int first, int count, SortOrder sortOrder) {
        EnhancedTracks enhancedTracks = new EnhancedTracks();
        enhancedTracks.setTracks(new ArrayList<EnhancedTrack>(count));
        String lastAlbum = TrackUtils.class.getName();// we need some dummy name
        String lastArtist = TrackUtils.class.getName();// we need some dummy name
        List<EnhancedTrack> sectionTracks = new ArrayList<>();
        boolean variousPerSection = false;
        int sectionCount = 0;
        for (int i = first; i < first + count && i < tracks.size(); i++) {
            Track track = tracks.get(i);
            EnhancedTrack enhancedTrack = new EnhancedTrack(track);
            boolean newAlbum = !lastAlbum.equalsIgnoreCase(track.getAlbum());
            boolean newArtist = !lastArtist.equalsIgnoreCase(track.getArtist());
            if ((sortOrder == SortOrder.Album && newAlbum) || (sortOrder == SortOrder.Artist && newArtist)) {// new section begins
                sectionCount++;
                enhancedTrack.setNewSection(true);
                finishSection(transaction, sectionTracks, variousPerSection);
                sectionTracks.clear();
                variousPerSection = sortOrder == SortOrder.Album && !track.getArtist().equals(track.getAlbumArtist());
            } else {
                if ((sortOrder == SortOrder.Album && !track.getArtist().equals(track.getAlbumArtist())) || (sortOrder == SortOrder.Artist && newAlbum)) {
                    variousPerSection = true;
                }
            }
            enhancedTracks.getTracks().add(enhancedTrack);
            sectionTracks.add(enhancedTrack);
            lastAlbum = track.getAlbum();
            lastArtist = track.getArtist();
        }
        for (int i = first + count; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            EnhancedTrack enhancedTrack = new EnhancedTrack(track);
            boolean newAlbum = !lastAlbum.equalsIgnoreCase(track.getAlbum());
            boolean newArtist = !lastArtist.equalsIgnoreCase(track.getArtist());
            if ((sortOrder == SortOrder.Album && newAlbum) || (sortOrder == SortOrder.Artist && newArtist)) {// new section begins
                finishSection(transaction, sectionTracks, variousPerSection);
                break; // now we are done with the last section
            }
            if ((sortOrder == SortOrder.Album && !track.getArtist().equals(track.getAlbumArtist())) || (sortOrder == SortOrder.Artist && newAlbum)) {
                variousPerSection = true;
            }
            sectionTracks.add(enhancedTrack);
            lastAlbum = track.getAlbum();
            lastArtist = track.getArtist();
        }
        finishSection(transaction, sectionTracks, variousPerSection);
        enhancedTracks.setSimpleResult(sectionCount == 1 && !variousPerSection);
        return enhancedTracks;
    }

    public static List<TvShowEpisode> getTvShowEpisodes(DataStoreSession transaction, Collection<Track> tracks) {
        List<TvShowEpisode> episodes = new ArrayList<>(tracks.size());
        String lastSeries = TrackUtils.class.getName(); // we need some dummy name
        Integer lastSeason = null;
        List<TvShowEpisode> seriesEpisodes = new ArrayList<>();
        List<TvShowEpisode> seasonEpisodes = new ArrayList<>();
        for (Track track : tracks) {
            TvShowEpisode episode = new TvShowEpisode(track);
            boolean newSeries = !lastSeries.equalsIgnoreCase(track.getSeries());
            boolean newSeason = newSeries || (lastSeason == null || !lastSeason.equals(track.getSeason()));
            if (newSeries) { // new series begins
                episode.setNewSeries(true);
                finishSeries(transaction, seriesEpisodes);
                seriesEpisodes.clear();
            }
            if (newSeason) {
                episode.setNewSeason(true);
                finishSeason(transaction, seasonEpisodes);
                seasonEpisodes.clear();
            }
            episodes.add(episode);
            seriesEpisodes.add(episode);
            seasonEpisodes.add(episode);
            lastSeries = track.getSeries();
            lastSeason = track.getSeason();
        }
        finishSeries(transaction, seriesEpisodes);
        finishSeason(transaction, seasonEpisodes);
        return episodes;
    }

    private static void finishSection(DataStoreSession transaction, List<EnhancedTrack> sectionTracks, boolean variousInSection) {
        String sectionIds = sectionIdsToString(sectionTracks);
        if (!sectionTracks.isEmpty()) {
            String sectionHash = sectionTracks.size() > 1 ? createTemporarySectionPlaylist(transaction, sectionIds) : null;
            for (EnhancedTrack rememberedTrack : sectionTracks) {
                rememberedTrack.setSectionPlaylistId(sectionHash);
                rememberedTrack.setSectionIds(sectionIds);
                if (!variousInSection) {
                    rememberedTrack.setSimple(true);
                }
            }
        }
    }

    private static void finishSeries(DataStoreSession transaction, List<TvShowEpisode> episodes) {
        String sectionIds = sectionIdsToString(episodes);
        if (!episodes.isEmpty()) {
            String sectionHash = episodes.size() > 1 ? createTemporarySectionPlaylist(transaction, sectionIds) : null;
            for (TvShowEpisode episode : episodes) {
                episode.setSeriesSectionPlaylistId(sectionHash);
                episode.setSeriesSectionIds(sectionIds);
            }
        }
    }

    private static void finishSeason(DataStoreSession transaction, List<TvShowEpisode> episodes) {
        String sectionIds = sectionIdsToString(episodes);
        if (!episodes.isEmpty()) {
            String sectionHash = episodes.size() > 1 ? createTemporarySectionPlaylist(transaction, sectionIds) : null;
            for (TvShowEpisode episode : episodes) {
                episode.setSeasonSectionPlaylistId(sectionHash);
                episode.setSeasonSectionIds(sectionIds);
            }
        }
    }

    private static String createTemporarySectionPlaylist(DataStoreSession transaction, String sectionIds) {
        try {
            final String sectionHash = MyTunesRssBase64Utils.encode(MyTunesRss.SHA1_DIGEST.get().digest(MiscUtils.getUtf8Bytes(sectionIds)));
            LOGGER.debug("Trying to create temporary playlist with id \"" + sectionHash + "\".");
            transaction.executeStatement(new DataStoreStatement() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    SmartStatement statement = MyTunesRssUtils.createStatement(connection, "removeTempPlaylistWithId");
                    statement.setString("id", sectionHash);
                    statement.execute();
                }
            });
            SaveTempPlaylistStatement statement = new SaveTempPlaylistStatement();
            statement.setId(sectionHash);
            statement.setName(sectionHash);
            statement.setTrackIds(Arrays.<String>asList(StringUtils.split(sectionIds, ',')));
            transaction.executeStatement(statement);
            return sectionHash;
        } catch (SQLException e) {
            LOGGER.error("Could not check for existing temporary playlist or could not insert missing temporary playlist.", e);
            return null;// do not use calculated section hash in case of an sql exception
        }
    }

    private static String sectionIdsToString(List<? extends Track> sectionTracks) {
        StringBuilder sectionIds = new StringBuilder();
        for (Iterator<? extends Track> iterator = sectionTracks.iterator(); iterator.hasNext();) {
            Track enhancedTrack = iterator.next();
            sectionIds.append(enhancedTrack.getId());
            if (iterator.hasNext()) {
                sectionIds.append(",");
            }
        }
        return sectionIds.toString();
    }

    /**
     * Get a list of track ids from a list of tracks.
     *
     * @param tracks A list of tracks.
     *
     * @return The list of track ids.
     */
    public static String[] getTrackIds(List<Track> tracks) {
        String[] trackIds = new String[tracks.size()];
        int i = 0;
        for (Track track : tracks) {
            trackIds[i++] = track.getId();
        }
        return trackIds;
    }

    public static class EnhancedTracks {
        private Collection<EnhancedTrack> myTracks;
        private boolean mySimpleResult;

        public boolean isSimpleResult() {
            return mySimpleResult;
        }

        public void setSimpleResult(boolean simpleResult) {
            mySimpleResult = simpleResult;
        }

        public Collection<EnhancedTrack> getTracks() {
            return myTracks;
        }

        public void setTracks(Collection<EnhancedTrack> tracks) {
            myTracks = tracks;
        }
    }

    public static class EnhancedTrack extends Track {
        private boolean myNewSection;
        private boolean myContinuation;
        private boolean mySimple;
        private String mySectionIds;
        private String mySectionPlaylistId;

        private EnhancedTrack(Track track) {
            super(track);
        }

        public boolean isNewSection() {
            return myNewSection;
        }

        public void setNewSection(boolean newSection) {
            myNewSection = newSection;
        }

        public boolean isContinuation() {
            return myContinuation;
        }

        public void setContinuation(boolean continuation) {
            myContinuation = continuation;
        }

        public boolean isSimple() {
            return mySimple;
        }

        public void setSimple(boolean simple) {
            mySimple = simple;
        }

        public String getSectionIds() {
            return mySectionIds;
        }

        public void setSectionIds(String sectionIds) {
            mySectionIds = sectionIds;
        }

        public String getSectionPlaylistId() {
            return mySectionPlaylistId;
        }

        public void setSectionPlaylistId(String sectionPlaylistId) {
            mySectionPlaylistId = sectionPlaylistId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnhancedTrack)) return false;
            if (!super.equals(o)) return false;

            EnhancedTrack that = (EnhancedTrack) o;

            if (myContinuation != that.myContinuation) return false;
            if (myNewSection != that.myNewSection) return false;
            if (mySimple != that.mySimple) return false;
            if (mySectionIds != null ? !mySectionIds.equals(that.mySectionIds) : that.mySectionIds != null)
                return false;
            return !(mySectionPlaylistId != null ? !mySectionPlaylistId.equals(that.mySectionPlaylistId) : that.mySectionPlaylistId != null);

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (myNewSection ? 1 : 0);
            result = 31 * result + (myContinuation ? 1 : 0);
            result = 31 * result + (mySimple ? 1 : 0);
            result = 31 * result + (mySectionIds != null ? mySectionIds.hashCode() : 0);
            result = 31 * result + (mySectionPlaylistId != null ? mySectionPlaylistId.hashCode() : 0);
            return result;
        }
    }

    public static class TvShowEpisode extends Track {
        private boolean myNewSeries;
        private boolean myNewSeason;
        private boolean myContinuation;
        private String mySeriesSectionIds;
        private String mySeriesSectionPlaylistId;
        private String mySeasonSectionIds;
        private String mySeasonSectionPlaylistId;

        private TvShowEpisode(Track track) {
            super(track);
        }

        public boolean isNewSeries() {
            return myNewSeries;
        }

        public void setNewSeries(boolean newSeries) {
            myNewSeries = newSeries;
        }

        public boolean isNewSeason() {
            return myNewSeason;
        }

        public void setNewSeason(boolean newSeason) {
            myNewSeason = newSeason;
        }

        public boolean isContinuation() {
            return myContinuation;
        }

        public void setContinuation(boolean continuation) {
            myContinuation = continuation;
        }

        public String getSeriesSectionIds() {
            return mySeriesSectionIds;
        }

        public void setSeriesSectionIds(String seriesSectionIds) {
            mySeriesSectionIds = seriesSectionIds;
        }

        public String getSeriesSectionPlaylistId() {
            return mySeriesSectionPlaylistId;
        }

        public void setSeriesSectionPlaylistId(String seriesSectionPlaylistId) {
            mySeriesSectionPlaylistId = seriesSectionPlaylistId;
        }

        public String getSeasonSectionIds() {
            return mySeasonSectionIds;
        }

        public void setSeasonSectionIds(String seasonSectionIds) {
            mySeasonSectionIds = seasonSectionIds;
        }

        public String getSeasonSectionPlaylistId() {
            return mySeasonSectionPlaylistId;
        }

        public void setSeasonSectionPlaylistId(String seasonSectionPlaylistId) {
            mySeasonSectionPlaylistId = seasonSectionPlaylistId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TvShowEpisode)) return false;
            if (!super.equals(o)) return false;

            TvShowEpisode that = (TvShowEpisode) o;

            if (myContinuation != that.myContinuation) return false;
            if (myNewSeason != that.myNewSeason) return false;
            if (myNewSeries != that.myNewSeries) return false;
            if (mySeasonSectionIds != null ? !mySeasonSectionIds.equals(that.mySeasonSectionIds) : that.mySeasonSectionIds != null)
                return false;
            if (mySeasonSectionPlaylistId != null ? !mySeasonSectionPlaylistId.equals(that.mySeasonSectionPlaylistId) : that.mySeasonSectionPlaylistId != null)
                return false;
            if (mySeriesSectionIds != null ? !mySeriesSectionIds.equals(that.mySeriesSectionIds) : that.mySeriesSectionIds != null)
                return false;
            return !(mySeriesSectionPlaylistId != null ? !mySeriesSectionPlaylistId.equals(that.mySeriesSectionPlaylistId) : that.mySeriesSectionPlaylistId != null);

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (myNewSeries ? 1 : 0);
            result = 31 * result + (myNewSeason ? 1 : 0);
            result = 31 * result + (myContinuation ? 1 : 0);
            result = 31 * result + (mySeriesSectionIds != null ? mySeriesSectionIds.hashCode() : 0);
            result = 31 * result + (mySeriesSectionPlaylistId != null ? mySeriesSectionPlaylistId.hashCode() : 0);
            result = 31 * result + (mySeasonSectionIds != null ? mySeasonSectionIds.hashCode() : 0);
            result = 31 * result + (mySeasonSectionPlaylistId != null ? mySeasonSectionPlaylistId.hashCode() : 0);
            return result;
        }
    }
}
