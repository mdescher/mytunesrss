/*
 * Copyright (c) 2014. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.mediaserver;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.config.MediaType;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.config.transcoder.TranscoderConfig;
import de.codewave.mytunesrss.datastore.statement.Album;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.model.*;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.item.Movie;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MyTunesRssDIDL extends DIDLContent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRssDIDL.class);

    final void initDirectChildren(String oidParams, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws SQLException {
        DataStoreSession tx = MyTunesRss.STORE.getTransaction();
        try {
            createDirectChildren(MyTunesRss.CONFIG.getUser("cling"), tx, oidParams, filter, firstResult, maxResults, orderby);
        } finally {
            tx.rollback();
        }
    }

    final void initMetaData(String oidParams) throws SQLException {
        DataStoreSession tx = MyTunesRss.STORE.getTransaction();
        try {
            createMetaData(MyTunesRss.CONFIG.getUser("cling"), tx, oidParams);
        } finally {
            tx.rollback();
        }
    }

    abstract void createDirectChildren(User user, DataStoreSession tx, String oidParams, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws SQLException;

    protected Res createTrackResource(Track track, User user) {
        StringBuilder builder = createWebAppCall(user, "playTrack"); // TODO hard-coded command name is not nice
        StringBuilder pathInfo = new StringBuilder("track=");
        pathInfo.append(MiscUtils.getUtf8UrlEncoded(track.getId()));
        TranscoderConfig transcoder = null;
        if (track.getMediaType() == MediaType.Audio) {
            transcoder = MyTunesRssUtils.getTranscoder(TranscoderConfig.MEDIA_SERVER_MP3_128.getName(), track);
            if (transcoder != null) {
                pathInfo.append("/tc=").
                         append(transcoder.getName());
            }
        }
        builder.append("/").
                append(MyTunesRssUtils.encryptPathInfo(pathInfo.toString()));
        builder.append("/").
                append(MiscUtils.getUtf8UrlEncoded(MyTunesRssUtils.virtualTrackName(track))).
                append(".").
                append(MiscUtils.getUtf8UrlEncoded(transcoder != null ? transcoder.getTargetSuffix() : FilenameUtils.getExtension(track.getFilename())));
        Res res = new Res();
        MimeType mimeType = MimeType.valueOf(transcoder != null ? transcoder.getTargetContentType() : track.getContentType());
        res.setProtocolInfo(new ProtocolInfo(mimeType));
        if (transcoder == null) {
            res.setSize(track.getContentLength());
        }
        res.setDuration(toHumanReadableTime(track.getTime()));
        res.setValue(builder.toString());
        LOGGER.debug("Resource value is \"" + res.getValue() + "\".");
        return res;
    }

    private StringBuilder createWebAppCall(User user, String command) {
        StringBuilder builder = new StringBuilder("http://");
        String hostAddress = StringUtils.defaultIfBlank(MyTunesRss.CONFIG.getHost(), AbstractContentDirectoryService.REMOTE_CLIENT_INFO.get().getLocalAddress().getHostAddress());
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
        return builder;
    }

    protected URI getImageUri(User user, int size, String imageHash) {
        if (StringUtils.isNotBlank(imageHash)) {
            StringBuilder builder = createWebAppCall(user, "showImage"); // TODO hard-coded command name is not nice
            builder.append("/").
                    append(MyTunesRssUtils.encryptPathInfo("hash=" + MiscUtils.getUtf8UrlEncoded(imageHash) + "/size=" + Integer.toString(size)));
            try {
                return new URI(builder.toString());
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not create URI for image.", e);
            }
        }
        return null;
    }

    abstract void createMetaData(User user, DataStoreSession tx, String oidParams) throws SQLException;

    abstract long getTotalMatches();

    String encode(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : strings) {
            stringBuilder.append(MyTunesRssBase64Utils.encode(s)).append(";");
        }
        return stringBuilder.length() > 0 ? stringBuilder.substring(0, stringBuilder.length() - 1) : stringBuilder.toString();
    }

    List<String> decode(String s) {
        List<String> decoded = new ArrayList<>();
        for (String encoded : StringUtils.split(s, ";")) {
            decoded.add(MyTunesRssBase64Utils.decodeToString(encoded));
        }
        return decoded;
    }

    protected String toHumanReadableTime(int time) {
        int seconds = time % 60;
        int minutes = (time / 60) % 60;
        int hours = time / 3600;
        StringBuilder builder = new StringBuilder();
        builder.append(hours).append(":");
        builder.append(StringUtils.leftPad(Integer.toString(minutes), 2, '0')).append(":");
        builder.append(StringUtils.leftPad(Integer.toString(seconds), 2, '0')).append(".000");
        LOGGER.debug("Human readable of \"" + time + "\" is \"" + builder.toString() + "\".");
        return builder.toString();
    }

    protected MusicTrack createMusicTrack(User user, Track track, String objectId, String parentId) {
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.setId(objectId);
        musicTrack.setParentID(parentId);
        musicTrack.setTitle(track.getName());
        musicTrack.setArtists(new PersonWithRole[]{new PersonWithRole(track.getArtist(), "Performer")});
        musicTrack.setAlbum(track.getAlbum());
        musicTrack.setCreator("MyTunesRSS");
        musicTrack.setDescription(track.getName());
        musicTrack.setOriginalTrackNumber(track.getTrackNumber());
        musicTrack.setGenres(new String[]{track.getGenre()});
        List<Res> resources = new ArrayList<>();
        resources.add(createTrackResource(track, user));
        musicTrack.setResources(resources);
        addTrackImageUris(user, track.getImageHash(), musicTrack);
        return musicTrack;
    }

    protected Movie createMovieTrack(User user, Track track, String objectId, String parentId) {
        Movie movie = new Movie();
        movie.setId(objectId);
        movie.setParentID(parentId);
        movie.setTitle(track.getName());
        movie.setCreator("MyTunesRSS");
        movie.setResources(Collections.singletonList(createTrackResource(track, user)));
        addTrackImageUris(user, track.getImageHash(), movie);
        return movie;
    }

    private void addTrackImageUris(User user, String imageHash, DIDLObject movie) {
        URI thumbnailImage = getImageUri(user, 128, imageHash);
        if (thumbnailImage != null) {
            DIDLObject.Property<DIDLAttribute> profileId = new DIDLObject.Property.DLNA.PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_TN"));
            List<DIDLObject.Property<DIDLAttribute>> props = Collections.singletonList(profileId);
            movie.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(thumbnailImage, props));
        }
        URI smallImage = getImageUri(user, 256, imageHash);
        if (smallImage != null) {
            DIDLObject.Property<DIDLAttribute> profileId = new DIDLObject.Property.DLNA.PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_SM"));
            List<DIDLObject.Property<DIDLAttribute>> props = Collections.singletonList(profileId);
            movie.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(smallImage, props));
        }
        int maxSize = MyTunesRssUtils.getMaxSizedImageSize(imageHash);
        if (maxSize > 256 && maxSize < 768 && "image/jpeg".equalsIgnoreCase(MyTunesRssUtils.guessContentType(MyTunesRssUtils.getMaxSizedImage(imageHash)))) {
            URI mediumImage = getImageUri(user, maxSize, imageHash);
            if (mediumImage != null) {
                DIDLObject.Property<DIDLAttribute> profileId = new DIDLObject.Property.DLNA.PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_MED"));
                List<DIDLObject.Property<DIDLAttribute>> props = Collections.singletonList(profileId);
                movie.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(mediumImage, props));
            }
        }
    }

    protected MusicAlbum createMusicAlbum(User user, Album album, String objectId, String parentId) {
        MusicAlbum musicAlbum = new MusicAlbum(objectId, parentId, album.getName(), album.getArtist(), album.getTrackCount());
        addTrackImageUris(user, album.getImageHash(), musicAlbum);
        return musicAlbum;
    }
}
