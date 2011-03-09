/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.jsp.BundleError;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.ResultBuilder;
import de.codewave.utils.sql.SmartStatement;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * de.codewave.mytunesrss.command.BrowseTrackCommandHandler
 */
public class BrowseTvShowCommandHandler extends BrowseVideoCommandHandler {
    @Override
    protected List<? extends Track> getEnhancedTracks(List<Track> tracks) {
        List<TrackUtils.TvShowEpisode> episodes = TrackUtils.getTvShowEpisodes(getTransaction(), tracks);
        int seriesCount = getSeriesCount(episodes);
        int seasonCount = getSeasonCount(episodes);
        if (seriesCount > 1) {
            List<TrackUtils.TvShowEpisode> filteredEpisodes = new ArrayList<TrackUtils.TvShowEpisode>();
            for (TrackUtils.TvShowEpisode episode : episodes) {
                if (episode.isNewSeries()) {
                    episode.setId(null);
                    episode.setSeason(-1);
                    filteredEpisodes.add(episode);
                }
            }
            return filteredEpisodes;
        } else if (seasonCount > 1) {
            List<TrackUtils.TvShowEpisode> filteredEpisodes = new ArrayList<TrackUtils.TvShowEpisode>();
            for (TrackUtils.TvShowEpisode episode : episodes) {
                if (episode.isNewSeason()) {
                    episode.setId(null);
                    filteredEpisodes.add(episode);
                }
            }
            return filteredEpisodes;
        } else {
            return episodes;
        }
    }

    @Override
    protected DataStoreQuery<DataStoreQuery.QueryResult<Track>> getQuery() {
        String series = MyTunesRssBase64Utils.decodeToString(getRequestParameter("series", null));
        int season = getIntegerRequestParameter("season", -1);
        if (series != null && season > -1) {
            return FindTrackQuery.getTvShowSeriesSeasonEpisodes(getAuthUser(), series, season);
        } else if (series != null) {
            return FindTrackQuery.getTvShowSeriesEpisodes(getAuthUser(), series);
        } else {
            return FindTrackQuery.getTvShowEpisodes(getAuthUser());
        }
    }

    @Override
    protected MyTunesRssResource getResource(List<? extends Track> tracks) {
        return MyTunesRssResource.BrowseTvShow;
    }

    private int getSeriesCount(List<TrackUtils.TvShowEpisode> episodes) {
        int count = 0;
        for (TrackUtils.TvShowEpisode episode : episodes) {
            if (episode.isNewSeries()) {
                count++;
            }
        }
        return count;
    }

    private int getSeasonCount(List<TrackUtils.TvShowEpisode> episodes) {
        int count = 0;
        for (TrackUtils.TvShowEpisode episode : episodes) {
            if (episode.isNewSeason()) {
                count++;
            }
        }
        return count;
    }
}