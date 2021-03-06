package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.config.FlashPlayerConfig;
import de.codewave.mytunesrss.config.PlaylistFileType;
import de.codewave.mytunesrss.servlet.WebConfig;
import de.codewave.utils.MiscUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * de.codewave.mytunesrss.command.ShowJukeboxCommandHandler
 */
public class ShowJukeboxCommandHandler extends MyTunesRssCommandHandler {
    @Override
    public void executeAuthorized() throws Exception {
        String playerId = StringUtils.defaultIfEmpty(getRequestParameter("playerId", getWebConfig().getFlashplayer()), FlashPlayerConfig.ABSOLUTE_DEFAULT.getId());
        FlashPlayerConfig flashPlayerConfig = MyTunesRss.CONFIG.getFlashPlayer(playerId);
        if (flashPlayerConfig == null) {
            flashPlayerConfig = FlashPlayerConfig.getDefault();
        }
        redirect(MyTunesRssWebUtils.getApplicationUrl(getRequest()) + "/flashplayer/" + playerId + "/?url=" + MiscUtils.getUtf8UrlEncoded(getPlaylistUrl(flashPlayerConfig.getPlaylistFileType(), flashPlayerConfig.getTimeUnit(), flashPlayerConfig.getImageSize())));
    }

    private String getPlaylistUrl(PlaylistFileType playlistFileType, TimeUnit timeUnit, int imageSize) {
        MyTunesRssCommandCallBuilder builder = new MyTunesRssCommandCallBuilder(MyTunesRssCommand.CreatePlaylist);
        builder.addParam("fpr", "1");
        builder.addParam("timeunit", timeUnit.name());
        builder.addParam("type", convertPlaylistType(playlistFileType).name());
        builder.addParam("imageSize", Integer.toString(imageSize));
        builder.addPathInfoSegment(MyTunesRssBase64Utils.decodeToString(getRequestParameter("playlistParams", null)));
        return builder.getCall(getRequest());
    }

    private WebConfig.PlaylistType convertPlaylistType(PlaylistFileType playlistFileType) {
        switch (playlistFileType) {
            case Xspf:
                return WebConfig.PlaylistType.Xspf;
            case M3u:
                return WebConfig.PlaylistType.M3u;
            case Json:
                return WebConfig.PlaylistType.Json;
            case JwMediaRss:
                return WebConfig.PlaylistType.JwMediaRss;
            default:
                throw new IllegalArgumentException("Illegal playlist file type \"" + playlistFileType.name() + "\".");
        }
    }
}
