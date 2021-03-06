/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.jsp.BundleError;
import de.codewave.mytunesrss.servlet.WebConfig;
import de.codewave.utils.sql.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * de.codewave.mytunesrss.command.CreatePlaylistCommandHandler
 */
public class CreatePlaylistCommandHandler extends CreatePlaylistBaseCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePlaylistCommandHandler.class);

    @Override
    public void executeAuthorized() throws SQLException, IOException, ServletException {
        boolean fpr = "1".equals(getRequestParameter("fpr", "0"));
        getRequest().setAttribute("filenames", fpr);
        if (getAuthUser().isPlaylist() || fpr) {
            TimeUnit timeUnit = TimeUnit.valueOf(getRequestParameter("timeunit", TimeUnit.SECONDS.name()));
            getRequest().setAttribute("timefactor", timeUnit.convert(1, TimeUnit.SECONDS));
            int imageSize = getSafeIntegerRequestParameter("imageSize", 0);
            getRequest().setAttribute("imgSizePathParam", imageSize > 0 ? "/size=" + imageSize : "");
            QueryResult<Track> tracks = getTracks();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found " + (tracks != null ? tracks.getResultSize() : 0) + " tracks for playlist.");
            }
            if (tracks != null && tracks.getResultSize() > 0) {
                getRequest().setAttribute("tracks", tracks.getResults());
                String playlistType = getRequestParameter("type", null);
                if (StringUtils.isEmpty(playlistType)) {
                    forward(getWebConfig().getPlaylistTemplateResource());
                } else {
                    forward(WebConfig.PlaylistType.valueOf(playlistType).getTemplateResource());
                }
            } else {
                addError(new BundleError("error.emptyFeed"));
                forward(MyTunesRssCommand.ShowPortal);// todo: redirect to backUrl
            }
        } else {
            getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

}
