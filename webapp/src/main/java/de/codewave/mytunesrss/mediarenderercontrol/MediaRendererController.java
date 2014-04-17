package de.codewave.mytunesrss.mediarenderercontrol;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.command.MyTunesRssCommand;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.config.transcoder.TranscoderConfig;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.servlet.TransactionFilter;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.QueryResult;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.avtransport.callback.*;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MediaRendererController {

    private static final class TrackWithUser {
        private final Track myTrack;
        private final User myUser;

        private TrackWithUser(Track track, User user) {
            myTrack = track;
            myUser = user;
        }

        public Track getTrack() {
            return myTrack;
        }

        public User getUser() {
            return myUser;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaRendererController.class);
    private static final MediaRendererController INSTANCE = new MediaRendererController();
    private static final long TIMEOUT_MILLIS = 2000;

    public static MediaRendererController getInstance() {
        return INSTANCE;
    }

    private volatile RemoteDevice myMediaRenderer;
    private volatile long myMaxVolume = 1;
    private volatile SubscriptionCallback mySubscriptionCallback;
    private volatile List<TrackWithUser> myTracks = new ArrayList<>();
    private AtomicInteger myCurrentTrack = new AtomicInteger(0);
    private AtomicBoolean myPlaying = new AtomicBoolean(false);
    private AtomicLong myTimeExplicitlyStopped = new AtomicLong(0);

    private MediaRendererController() {
        // only singleton instance above can be created
    }

    public synchronized String getMediaRendererName() {
        return myMediaRenderer != null ? myMediaRenderer.getDetails().getFriendlyName() : "";
    }

    public synchronized void loadPlaylist(User user, String playlistId) throws SQLException {
        DataStoreQuery<QueryResult<Track>> query = new FindPlaylistTracksQuery(user, playlistId, SortOrder.KeepOrder);
        loadItems(user, query);
    }

    private void loadItems(User user, DataStoreQuery<QueryResult<Track>> query) throws SQLException {
        List<Track> tracks = TransactionFilter.getTransaction().executeQuery(query).getResults();
        setTracks(user, tracks);
    }

    public synchronized void setTracks(User user, List<Track> tracks) {
        stop(false);
        myTracks.clear();
        for (Track track : tracks) {
            myTracks.add(new TrackWithUser(track, user));
        }
    }

    private void addItems(User user, DataStoreQuery<QueryResult<Track>> query, boolean startPlaybackIfStopped) throws SQLException {
        List<Track> tracks = TransactionFilter.getTransaction().executeQuery(query).getResults();
        int oldSize = myTracks.size();
        for (Track track : tracks) {
            myTracks.add(new TrackWithUser(track, user));
        }
        if (!getCurrentTrackInfo().isPlaying() && startPlaybackIfStopped) {
            // start playback with first new track
            play(oldSize, true);
        }
    }

    public synchronized void loadAlbum(User user, String albumName, String albumArtistName) throws SQLException {
        DataStoreQuery<QueryResult<Track>> query = FindTrackQuery.getForAlbum(user, new String[]{albumName}, StringUtils.isNotBlank(albumArtistName) ? new String[]{albumArtistName} : new String[0], SortOrder.Album);
        loadItems(user, query);
    }

    public synchronized void loadArtist(User user, String artistName, boolean fullAlbums) throws SQLException {
        DataStoreQuery<QueryResult<Track>> query = FindTrackQuery.getForArtist(user, new String[]{artistName}, SortOrder.Album);
        loadItems(user, query);
    }

    public synchronized void loadGenre(User user, String genreName) throws SQLException {
        DataStoreQuery<QueryResult<Track>> query = FindTrackQuery.getForGenre(user, new String[]{genreName}, SortOrder.Album);
        loadItems(user, query);
    }

    public synchronized void loadTracks(User user, String[] trackIds) throws SQLException {
        DataStoreQuery<QueryResult<Track>> query = FindTrackQuery.getForIds(trackIds);
        loadItems(user, query);
    }

    public synchronized void addTracks(User user, String[] trackIds, boolean startPlaybackIfStopped) throws SQLException {
        DataStoreQuery<QueryResult<Track>> query = FindTrackQuery.getForIds(trackIds);
        addItems(user, query, startPlaybackIfStopped);
    }

    public synchronized void clearPlaylist() {
        stop(false);
        LOGGER.debug("Clearing playlist.");
        myTracks.clear();
    }

    public synchronized void play(final int index, boolean async) {
        final RemoteService service = getAvTransport();
        if (service != null) {
            if (index == -1) {
                sendPlay(service);
            } else {
                final Track track = myTracks.get(index).getTrack();
                String hostAddress = service.getDevice().getIdentity().getDiscoveredOnLocalAddress().getHostAddress();
                final String playbackUrl = createPlaybackUrl(createBaseUrl(hostAddress, myTracks.get(index).getUser(), MyTunesRssCommand.PlayTrack.getName()), myTracks.get(index).getTrack());
                LOGGER.debug("Setting playback URL to \"" + playbackUrl + "\".");
                Future setUriFuture = MyTunesRss.UPNP_SERVICE.execute(new SetAVTransportURI(service, playbackUrl, track.getName()) {
                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        LOGGER.warn("Could not set playback URL \"" + playbackUrl + "\" at media renderer \"" + service.getDevice().getDisplayString() + "\".");
                    }

                    @Override
                    public void success(ActionInvocation invocation) {
                        myCurrentTrack.set(index);
                        sendPlay(service);
                    }
                });
                if (!async) {
                    try {
                        setUriFuture.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | TimeoutException | ExecutionException ignored) {
                        LOGGER.info("Problem while waiting for set URI response.");
                    }
                }
            }
        }
    }

    private void sendPlay(final RemoteService service) {
        LOGGER.debug("Starting playback.");
        MyTunesRss.UPNP_SERVICE.execute(new Play(service) {
            @Override
            public void success(ActionInvocation invocation) {
                myPlaying.set(true);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                LOGGER.warn("Could not start playback on media renderer \"" + service.getDevice().getDetails().getFriendlyName() + "\".");
            }
        });
    }

    public synchronized void pause() {
        final RemoteService service = getAvTransport();
        if (service != null) {
            LOGGER.debug("Pausing playback.");
            MyTunesRss.UPNP_SERVICE.execute(new Pause(service) {
                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    LOGGER.warn("Could not pause playback on media renderer \"" + service.getDevice().getDetails().getFriendlyName() + "\".");
                }
            });
        }
    }

    public synchronized void stop(boolean async) {
        final RemoteService service = getAvTransport();
        if (service != null) {
            myTimeExplicitlyStopped.set(System.currentTimeMillis());
            LOGGER.debug("Stopping playback.");
            Future stopFuture = MyTunesRss.UPNP_SERVICE.execute(new Stop(service) {
                @Override
                public void success(ActionInvocation invocation) {
                    myPlaying.set(false);
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    LOGGER.warn("Could not stop playback on media renderer \"" + service.getDevice().getDetails().getFriendlyName() + "\".");
                }
            });
            if (!async) {
                try {
                    stopFuture.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException | ExecutionException ignored) {
                    LOGGER.info("Problem while waiting for stop playback response.");
                }
            }
        }
    }

    public synchronized void next() {
        stop(false);
        if (myCurrentTrack.get() + 1 < myTracks.size()) {
            LOGGER.debug("Playing next track.");
            play(myCurrentTrack.incrementAndGet(), true);
        }
    }

    public synchronized void prev() {
        stop(false);
        if (myCurrentTrack.get() > 0) {
            LOGGER.debug("Playing previous track.");
            play(myCurrentTrack.decrementAndGet(), true);
        }
    }

    public synchronized void seek(final int percentage) {
        final RemoteService avTransport = getAvTransport();
        if (avTransport != null) {
            MyTunesRss.UPNP_SERVICE.execute(new GetPositionInfo(avTransport) {
                @Override
                public void received(ActionInvocation invocation, PositionInfo positionInfo) {
                    int duration = (int) positionInfo.getTrackDurationSeconds();
                    int position = (duration * percentage) / 100;
                    int hours = position / 3600;
                    int minutes = (position / 60) % 60;
                    int second = position % 60;
                    final String target = Integer.toString(hours) + ":" + StringUtils.leftPad(Integer.toString(minutes), 2, '0') + ":" + StringUtils.leftPad(Integer.toString(second), 2, '0');
                    LOGGER.debug("Seeking to \"" + target + "\".");
                    MyTunesRss.UPNP_SERVICE.execute(new Seek(avTransport, SeekMode.REL_TIME, target) {
                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            LOGGER.warn("Could not seek to \"" + target + "\" on media renderer \"" + avTransport.getDevice().getDetails().getFriendlyName() + "\".");
                        }
                    });
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    LOGGER.warn("Could not get track duration seconds from media renderer \"" + avTransport.getDevice().getDetails().getFriendlyName() + "\".");
                }
            });
        }
    }

    public synchronized MediaRendererTrackInfo getCurrentTrackInfo() {
        final MediaRendererTrackInfo trackInfo = new MediaRendererTrackInfo();
        trackInfo.setCurrentTrack(myCurrentTrack.get() + 1);
        trackInfo.setPlaying(myPlaying.get());

        final RemoteService avTransport = getAvTransport();
        final RemoteService renderingControl = getRenderingControl();

        if (avTransport != null && renderingControl != null) {
            Future getPositionInfoFuture = MyTunesRss.UPNP_SERVICE.execute(new GetPositionInfo(avTransport) {
                @Override
                public void received(ActionInvocation invocation, PositionInfo positionInfo) {
                    trackInfo.setCurrentTime((int) positionInfo.getTrackElapsedSeconds());
                    trackInfo.setLength((int) positionInfo.getTrackDurationSeconds());
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    LOGGER.warn("Could not get position info from media renderer \"" + avTransport.getDevice().getDetails().getFriendlyName() + "\".");
                }
            });
            Future getVolumeFuture = MyTunesRss.UPNP_SERVICE.execute(new GetVolume(renderingControl) {
                @Override
                public void received(ActionInvocation actionInvocation, int currentVolume) {
                    Long maxVolume = Math.max(myMaxVolume, 1);
                    trackInfo.setVolume((int) (((long) currentVolume * 100L) / maxVolume.longValue()));
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    LOGGER.warn("Could not get volume from media renderer \"" + renderingControl.getDevice().getDetails().getFriendlyName() + "\".");
                }
            });
            try {
                long timeout = System.currentTimeMillis() + 1000;
                for (Future future : new Future[]{getPositionInfoFuture, getVolumeFuture}) {
                    future.get(Math.max(timeout - System.currentTimeMillis(), 1), TimeUnit.MILLISECONDS);
                }
            } catch (ExecutionException | TimeoutException | InterruptedException e) {
                LOGGER.debug("Exception while waiting for future.", e);
            }
        }

        return trackInfo;
    }

    public synchronized void setVolume(final int percentage) {
        final RemoteService service = getRenderingControl();
        if (service != null) {
            LOGGER.debug("Setting volume to " + percentage + "% of maximum volume.");
            MyTunesRss.UPNP_SERVICE.execute(new SetVolume(service, ((long) percentage * myMaxVolume) / 100L) {
                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    LOGGER.warn("Could not set volume to \"" + percentage + "\" on media renderer \"" + service.getDevice().getDetails().getFriendlyName() + "\".");
                }
            });
        }
    }

    public synchronized boolean setFullScreen(boolean fullScreen) {
        // TOOD set fullscreen
        LOGGER.debug("Setting fullscreen (NOT YET IMPLEMENTED).");
        return fullScreen;
    }

    public synchronized void shuffle() {
        stop(false);
        LOGGER.debug("Shuffling tracks.");
        Collections.shuffle(myTracks);
        myCurrentTrack.set(0);
    }

    public synchronized List<Track> getPlaylist() {
        List<Track> playlist = new ArrayList<>();
        for (TrackWithUser track : myTracks) {
            playlist.add(track.getTrack());
        }
        return playlist;
    }

    public synchronized Track getTrack(int index) throws Exception {
        if (index < 0 || index >= myTracks.size()) {
            return null;
        }
        return myTracks.get(index).getTrack();
    }

    public synchronized void setMediaRenderer(RemoteDevice mediaRenderer) {
        if (mySubscriptionCallback != null) {
            LOGGER.debug("Ending subscription callback.");
            mySubscriptionCallback.end();
            mySubscriptionCallback = null;
        }
        stop(true);
        if (mediaRenderer != null) {
            LOGGER.debug("Setting media renderer \"" + mediaRenderer.getDetails().getFriendlyName() + "\".");
        } else {
            LOGGER.debug("Clearing media renderer.");
        }
        myMediaRenderer = mediaRenderer;
        if (mediaRenderer != null) {
            mySubscriptionCallback = new AvTransportLastChangeSubscriptionCallback(getAvTransport()) {
                @Override
                void handleTransportStateChange(TransportState previousTransportState, TransportState currentTransportState) {
                    MediaRendererController.this.handleTransportStateChange(previousTransportState, currentTransportState);
                }
            };
            LOGGER.debug("Starting subscription callback.");
            myTimeExplicitlyStopped.set(System.currentTimeMillis());
            MyTunesRss.UPNP_SERVICE.execute(mySubscriptionCallback);
            myMaxVolume = getRenderingControl().getStateVariable("Volume").getTypeDetails().getAllowedValueRange().getMaximum();
            LOGGER.debug("Maximum volume for media renderer \"" + mediaRenderer.getDetails().getFriendlyName() + "\" is " + myMaxVolume + ".");
            play(myCurrentTrack.get(), true);
        }
    }

    private synchronized void handleTransportStateChange(TransportState previousTransportState, TransportState currentTransportState) {
        LOGGER.debug("Media renderer transport state changed from " + previousTransportState.name() + " to " + currentTransportState.name() + ".");
        myPlaying.set(currentTransportState != TransportState.STOPPED && currentTransportState != TransportState.PAUSED_PLAYBACK);
        long timeDelta = System.currentTimeMillis() - myTimeExplicitlyStopped.get();
        if (currentTransportState == TransportState.STOPPED && (timeDelta > 1000) && myCurrentTrack.get() + 1 < myTracks.size()) {
            // advance if playback has stopped automatically (myUserStopped == false)
            LOGGER.debug("Automatically advancing to next track (last explicitly stopped " + timeDelta + " milliseconds ago).");
            next();
        }
    }

    private RemoteService getAvTransport() {
        return myMediaRenderer != null ? myMediaRenderer.findService(new UDAServiceId("AVTransport")) : null;
    }

    private RemoteService getRenderingControl() {
        return myMediaRenderer != null ? myMediaRenderer.findService(new UDAServiceId("RenderingControl")) : null;
    }

    private String createPlaybackUrl(String baseUrl, Track track) {
        StringBuilder pathInfo = new StringBuilder("track=");
        pathInfo.append(MiscUtils.getUtf8UrlEncoded(track.getId()));
        TranscoderConfig transcoder = null;
        // TODO transcoders
        /*for (TranscoderConfig config : .................) {
            transcoder = MyTunesRssUtils.getTranscoder(config.getName(), track);
            if (transcoder != null) {
                pathInfo.append("/tc=").append(transcoder.getName());
            }
        }*/
        StringBuilder builder = new StringBuilder(StringUtils.stripEnd(baseUrl, "/"));
        builder.append("/").
                append(MyTunesRssUtils.encryptPathInfo(pathInfo.toString()));
        builder.append("/").
                append(MiscUtils.getUtf8UrlEncoded(MyTunesRssUtils.virtualTrackName(track))).
                append(".").
                append(MiscUtils.getUtf8UrlEncoded(transcoder != null ? transcoder.getTargetSuffix() : FilenameUtils.getExtension(track.getFilename())));
        return builder.toString();
    }

    private String createBaseUrl(String hostAddress, User user, String command) {
        StringBuilder builder = new StringBuilder("http://");
        builder.append(hostAddress).
                append(":").append(MyTunesRss.CONFIG.getPort());
        String context = StringUtils.trimToEmpty(MyTunesRss.CONFIG.getWebappContext());
        if (!context.startsWith("/")) {
            builder.append("/");
        }
        builder.append(context);
        if (context.length() > 0 && !context.endsWith("/")) {
            builder.append("/");
        }
        builder.append("mytunesrss/").
                append(command).
                append("/").
                append(MyTunesRssUtils.createAuthToken(user));
        return builder.toString();
    }
}