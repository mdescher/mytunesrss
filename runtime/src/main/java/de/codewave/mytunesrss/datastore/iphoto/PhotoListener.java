/*
 * Copyright (c) 2011. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.datastore.iphoto;

import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.ShutdownRequestedException;
import de.codewave.mytunesrss.TikaUtils;
import de.codewave.mytunesrss.config.CompiledReplacementRule;
import de.codewave.mytunesrss.config.MediaType;
import de.codewave.mytunesrss.config.PhotoDatasourceConfig;
import de.codewave.mytunesrss.config.ReplacementRule;
import de.codewave.mytunesrss.datastore.statement.InsertOrUpdatePhotoStatement;
import de.codewave.mytunesrss.datastore.statement.InsertPhotoStatement;
import de.codewave.mytunesrss.datastore.statement.UpdatePhotoStatement;
import de.codewave.mytunesrss.datastore.updatequeue.DataStoreStatementEvent;
import de.codewave.mytunesrss.datastore.updatequeue.DatabaseUpdateQueue;
import de.codewave.mytunesrss.meta.MyTunesRssExifUtils;
import de.codewave.utils.xml.PListHandlerListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * de.codewave.mytunesrss.datastore.itunes.TrackListenerr
 */
public abstract class PhotoListener implements PListHandlerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoListener.class);

    private DatabaseUpdateQueue myQueue;
    private LibraryListener myLibraryListener;
    private int myUpdatedCount;
    private Map<String, String> myPhotoIdToPersId;
    private Map<String, Long> myPhotoTsUpdate;
    private Map<String, String> myPhotoSourceId;
    private Thread myWatchdogThread;
    private Set<CompiledReplacementRule> myPathReplacements;
    private PhotoDatasourceConfig myDatasourceConfig;
    private long myXmlModDate;

    public PhotoListener(PhotoDatasourceConfig datasourceConfig, Thread watchdogThread, DatabaseUpdateQueue queue, LibraryListener libraryListener, Map<String, String> photoIdToPersId,
                         Map<String, Long> photoTsUpdate, Map<String, String> photoSourceId, long xmlModDate) {
        myDatasourceConfig = datasourceConfig;
        myWatchdogThread = watchdogThread;
        myQueue = queue;
        myLibraryListener = libraryListener;
        myPhotoIdToPersId = photoIdToPersId;
        myPhotoTsUpdate = photoTsUpdate;
        myPhotoSourceId = photoSourceId;
        myXmlModDate = xmlModDate;
        myPathReplacements = new HashSet<>();
        for (ReplacementRule pathReplacement : myDatasourceConfig.getPathReplacements()) {
            myPathReplacements.add(new CompiledReplacementRule(pathReplacement));
        }
    }

    public int getUpdatedCount() {
        return myUpdatedCount;
    }

    @Override
    public boolean beforeDictPut(Map dict, String key, Object value) {
        Map photo = (Map) value;
        String photoId = calculatePhotoId(key, photo);
        if (photoId != null) {
            try {
                Long tsUpdate = myPhotoTsUpdate.remove(photoId);
                if (processPhoto(key, photo, photoId, tsUpdate == null || myDatasourceConfig.getId().equals(myPhotoSourceId.get(photoId)) ? tsUpdate : Long.valueOf(0))) {
                    myUpdatedCount++;
                }
            } catch (RuntimeException e) {
                LOGGER.error("Could not process photo with ID " + photoId + ".", e);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private String calculatePhotoId(String key, Map photo) {
        if (myLibraryListener.getLibraryId() == null) {
            return null;
        }
        String photoId = myLibraryListener.getLibraryId() + "_";
        photoId += photo.get("GUID") != null ? MyTunesRssBase64Utils.encode((String) photo.get("GUID")) : "PhotoID" + key;
        return photoId;
    }

    @Override
    public boolean beforeArrayAdd(List array, Object value) {
        throw new UnsupportedOperationException("method beforeArrayAdd of iPhoto photo listener is not supported!");
    }

    private boolean processPhoto(String key, Map photo, String photoId, Long tsUpdated) throws InterruptedException {
        if (myWatchdogThread.isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new ShutdownRequestedException();
        }
        String name = (String) photo.get("Caption");
        String mediaType = (String) photo.get("MediaType");
        if ("Image".equals(mediaType)) {
            String filename = applyReplacements(getImagePath(photo));
            if (StringUtils.isNotBlank(filename)) {
                File file = MyTunesRssUtils.searchFile(filename);
                if (file.isFile() && (tsUpdated == null || myXmlModDate >= tsUpdated || file.lastModified() >= tsUpdated)) {
                    if (TikaUtils.getMediaType(new File(filename)) == MediaType.Image) {
                        InsertOrUpdatePhotoStatement statement = tsUpdated != null ? new UpdatePhotoStatement(myDatasourceConfig.getId()) : new InsertPhotoStatement(myDatasourceConfig.getId());
                        statement.clear();
                        statement.setId(photoId);
                        statement.setName(name.trim());
                        Double dateAsTimerInterval = (Double) photo.get("DateAsTimerInterval");
                        Double modDateAsTimerInterval = (Double) photo.get("ModDateAsTimerInterval");
                        Long createDate = null;
                        // preference order for date:
                        // 1) date from xml
                        // 2) exif date
                        // 3) modification date from xml
                        if (dateAsTimerInterval != null) {
                            createDate = (dateAsTimerInterval.longValue() * 1000L) + 978303600000L;
                        } else {
                            createDate = MyTunesRssExifUtils.getCreateDate(file);
                            if (createDate == null && modDateAsTimerInterval != null) {
                                createDate = (modDateAsTimerInterval.longValue() * 1000L) + 978303600000L;
                            }
                        }
                        statement.setDate(createDate != null ? createDate : 0);
                        statement.setFile(filename);
                        myQueue.offer(new DataStoreStatementEvent(statement, true, "Could not insert photo \"" + name + "\" into database"));
                        //HandlePhotoImagesStatement handlePhotoImagesStatement = new HandlePhotoImagesStatement(file, photoId, 0);
                        //myQueue.offer(new DataStoreStatementEvent(handlePhotoImagesStatement, false, "Could not insert photo \"" + name + "\" into database"));
                        myPhotoIdToPersId.put(key, photoId);
                        return true;
                    }
                } else if (tsUpdated != null) {
                    myPhotoIdToPersId.put(key, photoId);
                }
                return false;
            }
        }
        return false;
    }

    private String getImagePath(Map photo) {
        return (String) photo.get("ImagePath");
    }

    private String applyReplacements(String originalFileName) {
        for (CompiledReplacementRule pathReplacement : myPathReplacements) {
            if (pathReplacement.matches(originalFileName)) {
                return pathReplacement.replace(originalFileName);
            }
        }
        return originalFileName;
    }
}
