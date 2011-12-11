package de.codewave.mytunesrss.datastore.filesystem;

import de.codewave.mytunesrss.FileSupportUtils;
import de.codewave.mytunesrss.ShutdownRequestedException;
import de.codewave.mytunesrss.WatchfolderDatasourceConfig;
import de.codewave.mytunesrss.datastore.updatequeue.DatabaseUpdateQueue;
import de.codewave.utils.io.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

/**
 * de.codewave.mytunesrss.datastore.filesystem.FileSystemLoaderr
 */
public class FileSystemLoader {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemLoader.class);

    public static void loadFromFileSystem(final Thread watchdogThread, final WatchfolderDatasourceConfig datasource, DatabaseUpdateQueue queue, long lastUpdateTime, Collection<String> trackIds, Collection<String> photoIds) throws IOException, SQLException {
        MyTunesRssFileProcessor fileProcessor = null;
        File baseDir = new File(datasource.getDefinition());
        if (baseDir != null && baseDir.isDirectory()) {
            fileProcessor = new MyTunesRssFileProcessor(datasource, queue, lastUpdateTime, trackIds, photoIds);
            if (LOG.isInfoEnabled()) {
                LOG.info("Processing files from: \"" + baseDir + "\".");
            }
            IOUtils.processFiles(baseDir, fileProcessor, new FileFilter() {
                public boolean accept(File file) {
                    if (watchdogThread.isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new ShutdownRequestedException();
                    }
                    return file.isDirectory() || (datasource.isIncluded(file) && FileSupportUtils.isSupported(file.getName()));
                }
            });
            PlaylistFileProcessor playlistFileProcessor = new PlaylistFileProcessor(queue, fileProcessor.getExistingIds());
            IOUtils.processFiles(baseDir, playlistFileProcessor, new FileFilter() {
                public boolean accept(File file) {
                    if (watchdogThread.isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new ShutdownRequestedException();
                    }
                    return file.isDirectory() || (datasource.isIncluded(file) && "m3u".equals(FilenameUtils.getExtension(file.getName().toLowerCase())));
                }
            });
            if (LOG.isInfoEnabled()) {
                LOG.info("Inserted/updated " + fileProcessor.getUpdatedCount() + " file system tracks.");
            }
        }
    }
}