package de.codewave.mytunesrss.remote.service;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.command.MyTunesRssCommand;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.jsp.MyTunesFunctions;
import de.codewave.mytunesrss.remote.MyTunesRssRemoteEnv;
import de.codewave.mytunesrss.servlet.TransactionFilter;
import de.codewave.mytunesrss.servlet.WebConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

/**
 * de.codewave.mytunesrss.remote.service.VideoLanClientService
 */
public class VideoLanClientRemoteController implements RemoteController {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoLanClientRemoteController.class);

    private VideoLanClient getVideoLanClient() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = new VideoLanClient();
        videoLanClient.connect(MyTunesRss.CONFIG.getVideoLanClientHost(), MyTunesRss.CONFIG.getVideoLanClientPort());
        return videoLanClient;
    }

    public void loadPlaylist(String playlistId) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("playlist=" + playlistId);
    }

    private void loadItem(String pathInfo) throws IllegalAccessException, IOException, InterruptedException {
        String url = MyTunesRssRemoteEnv.getServerCall(MyTunesRssCommand.CreatePlaylist, pathInfo + "/type=" + WebConfig.PlaylistType.M3u) + "/mytunesrss.m3u";
        loadUrl(url);
    }

    private void loadUrl(String url) throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("clear", "add " + url, "stop");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void loadAlbum(String albumName) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("album=" + MyTunesRssBase64Utils.encode(albumName));
    }

    public void loadArtist(String artistName, boolean fullAlbums) throws IllegalAccessException, IOException, InterruptedException {
        if (fullAlbums) {
            loadItem("fullAlbums=true/artist=" + MyTunesRssBase64Utils.encode(artistName));
        } else {
            loadItem("artist=" + MyTunesRssBase64Utils.encode(artistName));
        }
    }

    public void loadGenre(String genreName) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("genre=" + MyTunesRssBase64Utils.encode(genreName));
    }

    public void loadTrack(String trackId) throws IllegalAccessException, IOException, InterruptedException, SQLException {
        Collection<Track> tracks = TransactionFilter.getTransaction().executeQuery(FindTrackQuery.getForId(new String[]{trackId})).getResults();
        loadUrl(MyTunesFunctions.playbackUrl(MyTunesRssRemoteEnv.getRequest(), tracks.iterator().next(), null));
    }

    public void loadTracks(String[] trackIds) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("tracklist=" + StringUtils.join(trackIds, ","));
    }

    public void clearPlaylist() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("clear");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void play(int index) throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            if (index == 0) {
                if (!StringUtils.contains(videoLanClient.sendCommands("status"), "play state")) {
                    videoLanClient.sendCommands("pause");
                }
            } else {
                videoLanClient.sendCommands("goto " + (index - 1));
            }
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void pause() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            if (StringUtils.contains(videoLanClient.sendCommands("status"), "play state")) {
                videoLanClient.sendCommands("pause");
            }
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void stop() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            if (StringUtils.contains(videoLanClient.sendCommands("status"), "play state")) {
                videoLanClient.sendCommands("seek 0", "pause");
            } else {
                videoLanClient.sendCommands("pause", "seek 0", "pause");
            }
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void next() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("next");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void prev() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("prev", "f on");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void jumpTo(int percentage) throws Exception {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            int max = Integer.parseInt(videoLanClient.sendCommands("get_length"));
            videoLanClient.sendCommands("seek " + (max * percentage) / 100);
        } finally {
            videoLanClient.disconnect();
        }
    }

    public RemoteTrackInfo getCurrentTrackInfo() throws Exception {
        VideoLanClient videoLanClient = getVideoLanClient();
        RemoteTrackInfo info = new RemoteTrackInfo();
        try {
            String currentTrack = StringUtils.trim(StringUtils.substringBefore(StringUtils.substringAfter(videoLanClient.sendCommands("status"), "new input:"), ")"));
            if (StringUtils.isNotBlank(currentTrack)) {
                info.setPlaying(true);
                int i = 0;
                String[] playlist = StringUtils.split(videoLanClient.sendCommands("playlist"), "|");
                for (String entry : playlist) {
                    if (StringUtils.isNotBlank(entry)) {
                        if (StringUtils.contains(entry, currentTrack)) {
                            info.setCurrentTrack((i / 2) + 1);
                            break;
                        }
                        i++;
                    }
                }
                info.setLength(Integer.parseInt(videoLanClient.sendCommands("get_length")));
                info.setCurrentTime(Integer.parseInt(videoLanClient.sendCommands("get_time")));
            }
            String volume = StringUtils.trim(StringUtils.substringBefore(StringUtils.substringAfter(videoLanClient.sendCommands("volume"), "audio volume:"), ")"));
            try {
                info.setVolume((int) (((Float.parseFloat(volume) * 100.0) / 1024.0)));
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not get volume information.", e);
                info.setVolume(-1);
            }
        } finally {
            videoLanClient.disconnect();
        }
        return info;
    }

    public void setVolume(int percentage) throws Exception {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            int normalizedPercentage = Math.min(Math.max(0, percentage), 100);
            videoLanClient.sendCommands("volume " + (int) (((1024.0 * (float) normalizedPercentage) / 100.0)));
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void setFullscreen(boolean fullscreen) throws Exception {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("f " + (fullscreen ? "on" : "off"));
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void shuffle() {
        // todo: implement method
        throw new UnsupportedOperationException("method shuffle of class VideoLanClientRemoteController is not yet implemented!");
    }

    public String sendCommand(String command) throws IOException, IllegalAccessException, InterruptedException {
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            return videoLanClient.sendCommands(command);
        } finally {
            videoLanClient.disconnect();
        }
    }
}