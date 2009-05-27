package de.codewave.mytunesrss.remote.service;

/**
 * de.codewave.mytunesrss.remote.service.RemoteControlService
 */
public interface RemoteController {
    void loadPlaylist(String playlistId) throws Exception;

    void loadAlbum(String albumName) throws Exception;

    void loadArtist(String artistName, boolean fullAlbums) throws Exception;

    void loadGenre(String genreName) throws Exception;

    void loadTrack(String trackId) throws Exception;

    void loadTracks(String[] trackIds) throws Exception;

    void clearPlaylist() throws Exception;

    /**
     * Play a certain track of the current playlist.
     *
     * @param index The 1-based index of the track to play. A value of 0 should start playback at the current position (used to resume from a pause command).
     * @throws Exception Any exception from the service.
     */
    void play(int index) throws Exception;

    void pause() throws Exception;

    void stop() throws Exception;

    void next() throws Exception;

    void prev() throws Exception;

    void jumpTo(int percentage) throws Exception;

    RemoteTrackInfo getCurrentTrackInfo() throws Exception;

    void setVolume(int percentage) throws Exception;

    void setFullscreen(boolean fullscreen) throws Exception;

    void shuffle() throws Exception;
}