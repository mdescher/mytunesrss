package de.codewave.mytunesrss.datastore.filesystem;

import de.codewave.camel.mp3.Id3Tag;
import de.codewave.camel.mp3.Id3v1Tag;
import de.codewave.camel.mp3.Id3v2Tag;
import de.codewave.camel.mp3.Mp3Utils;
import de.codewave.camel.mp4.Mp4Atom;
import de.codewave.camel.mp4.Mp4Utils;
import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.meta.Image;
import de.codewave.mytunesrss.meta.MyTunesRssMp3Utils;
import de.codewave.mytunesrss.meta.TrackMetaData;
import de.codewave.mytunesrss.task.DatabaseBuilderTask;
import de.codewave.utils.io.FileProcessor;
import de.codewave.utils.io.IOUtils;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * de.codewave.mytunesrss.datastore.filesystem.MyTunesRssFileProcessor
 */
public class MyTunesRssFileProcessor implements FileProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRssFileProcessor.class);
    private static final String ATOM_ALBUM = "moov.udta.meta.ilst.\u00a9alb.data";
    private static final String ATOM_ARTIST = "moov.udta.meta.ilst.\u00a9ART.data";
    private static final String ATOM_TITLE = "moov.udta.meta.ilst.\u00a9nam.data";
    private static final String ATOM_TRACK_NUMBER = "moov.udta.meta.ilst.trkn.data";
    private static final String ATOM_GENRE = "moov.udta.meta.ilst.\u00a9gen.data";
    private static final String ATOM_STSD = "moov.trak.mdia.minf.stbl.stsd";
    private static final String ATOM_COVER = "moov.udta.meta.ilst.covr.data";

    private File myBaseDir;
    private long myLastUpdateTime;
    private DataStoreSession myStoreSession;
    private int myUpdatedCount;
    private Set<String> myExistingIds = new HashSet<String>();
    private Collection<String> myTrackIds;
    private long myScannedCount;
    private long myLastEventTime;
    private long myStartTime;

    public MyTunesRssFileProcessor(File baseDir, DataStoreSession storeSession, long lastUpdateTime, Collection<String> trackIds)
            throws SQLException {
        myBaseDir = baseDir;
        myStoreSession = storeSession;
        myLastUpdateTime = lastUpdateTime;
        myTrackIds = trackIds;
    }

    public Set<String> getExistingIds() {
        return myExistingIds;
    }

    public int getUpdatedCount() {
        return myUpdatedCount;
    }

    public void process(File file) {
        myScannedCount++;
        if (myLastEventTime == 0) {
            myLastEventTime = System.currentTimeMillis();
            myStartTime = myLastEventTime;
        } else if (System.currentTimeMillis() - myLastEventTime > 2500L) {
            MyTunesRssEvent event = MyTunesRssEvent.DATABASE_UPDATE_STATE_CHANGED;
            event.setMessageKey("settings.databaseUpdateRunningFolderWithCount");
            event.setMessageParams(myScannedCount, myScannedCount / ((System.currentTimeMillis() - myStartTime) / 1000L));
            MyTunesRssEventManager.getInstance().fireEvent(event);
            myLastEventTime = System.currentTimeMillis();
        }
        try {
            if (file.isFile() && FileSupportUtils.isSupported(file.getName())) {
                String fileId = "file_" + IOUtils.getFilenameHash(file);
                if (!myExistingIds.contains(fileId)) {
                    String canonicalFilePath = file.getCanonicalPath();
                    boolean existing = myTrackIds.contains(fileId);
                    if (existing) {
                        myExistingIds.add(fileId);
                    }
                    if ((file.lastModified() >= myLastUpdateTime || !existing)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Processing file \"" + file.getAbsolutePath() + "\".");
                        }
                        InsertOrUpdateTrackStatement statement;
                        if (!MyTunesRss.CONFIG.isIgnoreArtwork()) {
                            statement = existing ? new UpdateTrackAndImageStatement(TrackSource.FileSystem) : new InsertTrackAndImageStatement(TrackSource.FileSystem);
                        } else {
                            statement = existing ? new UpdateTrackStatement(TrackSource.FileSystem) : new InsertTrackStatement(TrackSource.FileSystem);
                        }
                        statement.clear();
                        statement.setId(fileId);
                        TrackMetaData meta = null;
                        if (FileSupportUtils.isMp3(file)) {
                            meta = parseMp3MetaData(file, statement, fileId);
                        } else if (FileSupportUtils.isMp4(file)) {
                            meta = parseMp4MetaData(file, statement, fileId);
                        } else {
                            setSimpleInfo(statement, file);
                        }
                        FileType type = MyTunesRss.CONFIG.getFileType(FileSupportUtils.getFileSuffix(file.getName()));
                        statement.setProtected(type.isProtected());
                        statement.setMediaType(type.getMediaType());
                        statement.setFileName(canonicalFilePath);
                        try {
                            myStoreSession.executeStatement(statement);
                            if (meta != null && meta.getImage() != null && !MyTunesRss.CONFIG.isIgnoreArtwork()) {
                                HandleTrackImagesStatement handleTrackImagesStatement = new HandleTrackImagesStatement(file, fileId, meta.getImage(), 0);
                                myStoreSession.executeStatement(handleTrackImagesStatement);
                            } else if (!MyTunesRss.CONFIG.isIgnoreArtwork()) {
                                HandleTrackImagesStatement handleTrackImagesStatement = new HandleTrackImagesStatement(file, fileId, 0);
                                myStoreSession.executeStatement(handleTrackImagesStatement);
                            }
                            myUpdatedCount++;
                            DatabaseBuilderTask.updateHelpTables(myStoreSession, myUpdatedCount);
                            myExistingIds.add(fileId);
                        } catch (SQLException e) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("Could not insert track \"" + canonicalFilePath + "\" into database", e);
                            }
                        }
                    }
                    DatabaseBuilderTask.doCheckpoint(myStoreSession, false);
                }
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not process file \"" + file.getAbsolutePath() + "\".", e);
            }
        }
        myTrackIds.removeAll(myExistingIds);
    }

    private TrackMetaData parseMp3MetaData(File file, InsertOrUpdateTrackStatement statement, String fileId) {
        TrackMetaData meta = new TrackMetaData();
        Id3Tag tag = null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Reading ID3 information from file \"" + file.getAbsolutePath() + "\".");
            }
            tag = Mp3Utils.readId3Tag(file);
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not get ID3 information from file \"" + file.getAbsolutePath() + "\".", e);
            }
        }
        if (tag == null) {
            setSimpleInfo(statement, file);
        } else {
            try {
                String album = tag.getAlbum();
                if (StringUtils.isEmpty(album)) {
                    album = getFallbackAlbumName(file);
                }
                statement.setAlbum(MyTunesRssUtils.normalize(album));
                String artist = tag.getArtist();
                if (StringUtils.isEmpty(artist)) {
                    artist = getFallbackArtistName(file);
                }
                statement.setArtist(MyTunesRssUtils.normalize(artist));
                String name = tag.getTitle();
                if (StringUtils.isEmpty(name)) {
                    name = FilenameUtils.getBaseName(file.getName());
                }
                statement.setName(MyTunesRssUtils.normalize(name));
                if (tag.isId3v2()) {
                    Id3v2Tag id3v2Tag = ((Id3v2Tag) tag);
                    statement.setTime(id3v2Tag.getTimeSeconds());
                    statement.setTrackNumber(id3v2Tag.getTrackNumber());
                    meta.setImage(MyTunesRssMp3Utils.getImage(id3v2Tag));
                    String pos = id3v2Tag.getFrameBodyToString("TPA", "TPOS");
                    try {
                        if (StringUtils.isNotEmpty(pos)) {
                            String[] posParts = pos.split("/");
                            if (posParts.length == 1) {
                                statement.setPos(Integer.parseInt(posParts[0].trim()), 0);
                            } else if (posParts.length == 2) {
                                statement.setPos(Integer.parseInt(posParts[0].trim()), Integer.parseInt(posParts[1].trim()));
                            }
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Illegal TPA/TPOS value \"" + pos + "\" in \"" + file + "\".");
                    }
                }
                String genre = tag.getGenreAsString();
                if (genre != null) {
                    statement.setGenre(StringUtils.trimToNull(genre));
                }
                statement.setComment(MyTunesRssUtils.normalize(StringUtils.trimToNull(createComment(tag))));
            } catch (Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Could not parse ID3 information from file \"" + file.getAbsolutePath() + "\".", e);
                }
                statement.clear();
                statement.setId(fileId);
                setSimpleInfo(statement, file);
            }
        }
        return meta;
    }

    private String createComment(Id3Tag tag) {
        try {
            if (tag.isId3v2()) {
                String comment = " " + MyTunesRss.CONFIG.getId3v2TrackComment() + " ";// make sure the comment does neither start nor end with a token
                if (StringUtils.isNotBlank(comment)) {
                    for (int s = comment.indexOf("${"); s > -1; s = comment.indexOf("${")) {
                        int e = comment.indexOf("}", s);
                        if (e != -1) {
                            String[] instructions = comment.substring(s + 2, e).split(";");
                            String[] tokens = instructions[0].split(",");
                            String tagData;
                            if (instructions.length > 2 && instructions[2].trim().toUpperCase().contains("M")) {
                                tagData = ((Id3v2Tag) tag).getFrameBodiesToString(tokens[0].trim(),
                                        tokens.length == 1 ? tokens[0].trim() : tokens[1].trim(),
                                        "\n");
                            } else {
                                tagData = ((Id3v2Tag) tag).getFrameBodyToString(tokens[0].trim(),
                                        tokens.length == 1 ? tokens[0].trim() : tokens[1].trim());
                            }
                            String value = StringUtils.trimToEmpty(tagData);
                            if (StringUtils.isEmpty(value) && instructions.length > 1) {
                                value = instructions[1];
                            }
                            comment = comment.substring(0, s) + value + comment.substring(e + 1);
                        }
                    }
                }
                if (StringUtils.isNotBlank(comment)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Created comment for ID3 tag: \"" + StringUtils.trimToEmpty(comment) + "\"");
                    }
                }
                return StringUtils.trimToNull(comment);
            }
            return ((Id3v1Tag) tag).getComment();
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Could not create comment for ID3 tag", e);
            }
        }
        return null;
    }

    private TrackMetaData parseMp4MetaData(File file, InsertOrUpdateTrackStatement statement, String fileId) {
        TrackMetaData meta = new TrackMetaData();
        Map<String, Mp4Atom> atoms = null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Reading ATOM information from file \"" + file.getAbsolutePath() + "\".");
            }
            atoms = Mp4Utils.getAtoms(file, Arrays.asList(ATOM_ALBUM, ATOM_ARTIST, ATOM_TITLE, ATOM_TRACK_NUMBER, ATOM_GENRE, ATOM_STSD, ATOM_COVER));
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not get ATOM information from file \"" + file.getAbsolutePath() + "\".", e);
            }
        }
        if (atoms == null || atoms.isEmpty()) {
            setSimpleInfo(statement, file);
        } else {
            try {
                Mp4Atom atom = atoms.get(ATOM_ALBUM);
                String album = atom != null ? atom.getDataAsString(8, "UTF-8") : null;
                if (StringUtils.isEmpty(album)) {
                    album = getFallbackAlbumName(file);
                }
                statement.setAlbum(MyTunesRssUtils.normalize(album));
                atom = atoms.get(ATOM_ARTIST);
                String artist = atom != null ? atom.getDataAsString(8, "UTF-8") : null;
                if (StringUtils.isEmpty(artist)) {
                    artist = getFallbackArtistName(file);
                }
                statement.setArtist(MyTunesRssUtils.normalize(artist));
                atom = atoms.get(ATOM_TITLE);
                String name = atom != null ? atom.getDataAsString(8, "UTF-8") : null;
                if (StringUtils.isEmpty(name)) {
                    name = FilenameUtils.getBaseName(file.getName());
                }
                statement.setName(MyTunesRssUtils.normalize(name));
                //statement.setTime(atoms.get(ATOM_TIME).getData()[11]);
                atom = atoms.get(ATOM_TRACK_NUMBER);
                if (atom != null) {
                    statement.setTrackNumber(atom.getData()[11]);
                }
                atom = atoms.get(ATOM_STSD);
                if (atom != null) {
                    statement.setMp4Codec(atom.getDataAsString(12, 4, "UTF-8"));
                }
                atom = atoms.get(ATOM_GENRE);
                String genre = atom != null ? atom.getDataAsString(8, "UTF-8") : null;
                if (genre != null) {
                    statement.setGenre(StringUtils.trimToNull(genre));
                }
            } catch (Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Could not parse ID3 information from file \"" + file.getAbsolutePath() + "\".", e);
                }
                statement.clear();
                statement.setId(fileId);
                setSimpleInfo(statement, file);
            }
        }
        Mp4Atom atom = atoms.get(ATOM_COVER);
        if (atom != null) {
            byte type = atom.getData()[3];
            meta.setImage(new Image(type == 0x0d ? "image/jpeg" : "image/png", ArrayUtils.subarray(atom.getData(), 8, atom.getData().length - 8)));
        }
        return meta;
    }

    private void setSimpleInfo(InsertOrUpdateTrackStatement statement, File file) {
        statement.setName(FilenameUtils.getBaseName(file.getName()));
        statement.setAlbum(getFallbackAlbumName(file));
        statement.setArtist(getFallbackArtistName(file));
    }

    private String getFallbackAlbumName(File file) {
        return getFallbackName(file, new String(MyTunesRss.CONFIG.getAlbumFallback()));
    }

    private String getFallbackName(File file, String pattern) {
        String name = new String(pattern);
        for (String token : StringUtils.substringsBetween(pattern, "[dir:", "]")) {
            String trimmedToken = StringUtils.trimToNull(token);
            if (StringUtils.isNumeric(trimmedToken)) {
                int number = Integer.parseInt(trimmedToken);
                File dir = file.getParentFile();
                while (dir != null && number > 0) {
                    dir = dir.getParentFile();
                    number--;
                }
                if (dir != null && dir.isDirectory()) {
                    name = name.replace("[dir:" + token + "]", dir.getName());
                }
            }
        }
        name = StringUtils.trimToNull(name);
        LOGGER.debug("Fallback name for \"" + file + "\" and pattern \"" + pattern + "\" is \"" + name + "\".");
        return name;
    }

    private String getFallbackArtistName(File file) {
        return getFallbackName(file, new String(MyTunesRss.CONFIG.getArtistFallback()));
    }
}