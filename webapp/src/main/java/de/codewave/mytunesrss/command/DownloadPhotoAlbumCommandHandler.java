/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.MyTunesRssSendCounter;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.datastore.statement.FindPhotoQuery;
import de.codewave.mytunesrss.datastore.statement.Photo;
import de.codewave.utils.servlet.SessionManager;
import de.codewave.utils.sql.QueryResult;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * de.codewave.mytunesrss.command.GetZipArchiveCommandHandler
 */
public class DownloadPhotoAlbumCommandHandler extends BandwidthThrottlingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadPhotoAlbumCommandHandler.class);

    @Override
    public void executeAuthorized() throws Exception {
        User user = getAuthUser();
        if (isRequestAuthorized() && user.isDownloadPhotoAlbum()) {
            String photoAlbumId = MyTunesRssBase64Utils.decodeToString(getRequestParameter("photoalbumid", null));
            int photoSize = Math.max(Math.min(getIntegerRequestParameter("size", 100), 100), 1);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Getting photos for album with ID \"" + photoAlbumId + "\" with size \"" + photoSize + "\".");
            }
            QueryResult<Photo> photoResult = getTransaction().executeQuery(FindPhotoQuery.getForAlbum(getAuthUser(), photoAlbumId));
            getResponse().setContentType("application/zip");
            getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + MyTunesRssUtils.getLegalFileName(MyTunesRssBase64Utils.decodeToString(getRequestParameter("photoalbum", "cGhvdG9z")) + ".zip") + "\""); // cGhvdG9z => photos
            createZipArchive(getResponse().getOutputStream(), photoResult.getResults(), new MyTunesRssSendCounter(user, SessionManager.getSessionInfo(getRequest())), photoSize);
        } else {
            getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void createZipArchive(OutputStream outputStream, List<Photo> results, MyTunesRssSendCounter sendCounter, int photoSize) throws IOException {
        try (ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(outputStream)) {
            zipStream.setLevel(ZipArchiveOutputStream.STORED);
            zipStream.setComment("MyTunesRSS v" + MyTunesRss.VERSION + " (http://www.codewave.de)");
            for (Photo photo : results) {
                File photoFile = new File(photo.getFile());
                if (photoFile.isFile()) {
                    FileInputStream inputStream = null;
                    try {
                        ZipArchiveEntry archiveEntry = new ZipArchiveEntry(photoFile.getName());
                        archiveEntry.setTime(photoFile.lastModified());
                        zipStream.putArchiveEntry(archiveEntry);
                        if (photoSize == 100) {
                            archiveEntry.setSize(photoFile.length());
                            inputStream = new FileInputStream(photoFile);
                            IOUtils.copy(inputStream, zipStream);
                        } else {
                            File tempFile = MyTunesRssUtils.createTempFile("jpg");
                            try {
                                MyTunesRssUtils.resizeImageWithMaxSize(photoFile, tempFile, (photoSize * MyTunesRssUtils.getImageSize(photo).getMaxSize()) / 100, (float) MyTunesRss.CONFIG.getJpegQuality(), "photo=" + photoFile.getAbsolutePath());
                                try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                                    IOUtils.copyLarge(fileInputStream, zipStream);
                                }
                                archiveEntry.setSize(tempFile.length());
                            } finally {
                                if (!tempFile.delete()) {
                                    LOGGER.debug("Could not delete file \"" + tempFile.getAbsolutePath() + "\".");
                                }
                            }
                        }
                        zipStream.closeArchiveEntry();
                        if (sendCounter != null) {
                            sendCounter.add((int) archiveEntry.getCompressedSize());
                        }
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                } else {
                    byte[] textFile = "Image file was not found!".getBytes("UTF-8");
                    ZipArchiveEntry archiveEntry = new ZipArchiveEntry(photoFile.getName() + ".txt");
                    archiveEntry.setSize(textFile.length);
                    archiveEntry.setTime(System.currentTimeMillis());
                    zipStream.putArchiveEntry(archiveEntry);
                    zipStream.write(textFile);
                    zipStream.closeArchiveEntry();
                }
            }
        }
    }
}
