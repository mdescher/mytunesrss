package de.codewave.mytunesrss.transcoder;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.config.TranscoderConfig;
import de.codewave.mytunesrss.datastore.statement.FindTrackImageQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.meta.Image;
import de.codewave.utils.io.LogStreamCopyThread;
import de.codewave.utils.io.StreamCopyThread;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.transcoder.TranscoderStream
 */
public class TranscoderStream extends InputStream {
    private static final Logger LOG = LoggerFactory.getLogger(TranscoderStream.class);

    private Process myProcess;
    private TranscoderConfig myTranscoderConfig;
    private File myImageFile;

    TranscoderStream(TranscoderConfig transcoderConfig, Track track, final InputStream inputStream) throws IOException {
        myTranscoderConfig = transcoderConfig;
        final String[] transcoderCommand = new String[getArguments().split(" ").length + 1];
        transcoderCommand[0] = transcoderConfig.getBinary();
        int i = 1;
        for (String part : getArguments().split(" ")) {
            transcoderCommand[i++] = part;
        }
        replaceTokens(transcoderCommand, track);
        if (LOG.isDebugEnabled()) {
            LOG.debug("executing " + getName() + " command \"" + StringUtils.join(transcoderCommand, " ") + "\".");
        }
        myProcess = Runtime.getRuntime().exec(transcoderCommand);
        new StreamCopyThread(inputStream, true, myProcess.getOutputStream(), true).start();
        new LogStreamCopyThread(myProcess.getErrorStream(), false, LoggerFactory.getLogger(getClass()), LogStreamCopyThread.LogLevel.Debug).start();
    }

    public int read() throws IOException {
        return myProcess.getInputStream().read();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return myProcess.getInputStream().read(bytes);
    }

    @Override
    public int read(byte[] bytes, int start, int length) throws IOException {
        return myProcess.getInputStream().read(bytes, start, length);
    }

    @Override
    public long skip(long l) throws IOException {
        return myProcess.getInputStream().skip(l);
    }

    @Override
    public int available() throws IOException {
        return myProcess.getInputStream().available();
    }

    @Override
    public void mark(int i) {
        myProcess.getInputStream().mark(i);
    }

    @Override
    public void reset() throws IOException {
        myProcess.getInputStream().reset();
    }

    @Override
    public boolean markSupported() {
        return myProcess.getInputStream().markSupported();
    }

    @Override
    public void close() throws IOException {
        if (myImageFile != null) {
            myImageFile.delete();
        }
        if (myProcess != null) {
            myProcess.destroy();
        }
    }

    protected String getName() {
        return myTranscoderConfig.getName();
    }

    protected String getArguments() {
        return myTranscoderConfig.getOptions();
    }

    public void replaceTokens(String[] command, Track track) {
        for (int i = 0; i < command.length; i++) {
            if ("{info.album}".equals(command[i])) {
                command[i] = track.getAlbum();
            } else if ("{info.artist}".equals(command[i])) {
                command[i] = track.getOriginalArtist();
            } else if ("{info.track}".equals(command[i])) {
                command[i] = track.getName();
            } else if ("{info.genre}".equals(command[i])) {
                command[i] = track.getGenre();
            } else if ("{info.comment}".equals(command[i])) {
                command[i] = track.getComment();
            } else if ("{info.pos.number}".equals(command[i])) {
                command[i] = Integer.toString(track.getPosNumber());
            } else if ("{info.pos.size}".equals(command[i])) {
                command[i] = Integer.toString(track.getPosSize());
            } else if ("{info.time}".equals(command[i])) {
                command[i] = Integer.toString(track.getTime());
            } else if ("{info.track.number}".equals(command[i])) {
                command[i] = Integer.toString(track.getTrackNumber());
            } else if ("{info.image.file}".equals(command[i])) {
                replaceImageToken(track, command, i);
            }
        }
    }

    private void replaceImageToken(Track track, String[] command, int i) {
        try {
            myImageFile = MyTunesRssUtils.createTempFile("jpg");
            byte[] data = new byte[0];
            DataStoreSession transaction = MyTunesRss.STORE.getTransaction();
            try {
                data = transaction.executeQuery(new FindTrackImageQuery(track.getId(), -1));
                if (data != null && data.length > 0) {
                    Image image = new Image("image/jpeg", data);
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(myImageFile);
                        fos.write(image.getData());
                    } catch (IOException e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Could not create image file.", e);
                        }
                        myImageFile.delete();
                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                }
            } catch (SQLException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Could not query image data.", e);
                }
                myImageFile.delete();
            } finally {
                transaction.rollback();
            }
            try {
                command[i] = myImageFile.getCanonicalPath();
            } catch (IOException e) {
                command[i] = myImageFile.getAbsolutePath();
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not create temp file for image.", e);
            }
        }
    }
}