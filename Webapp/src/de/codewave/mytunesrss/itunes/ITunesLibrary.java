package de.codewave.mytunesrss.itunes;

import de.codewave.mytunesrss.musicfile.*;
import de.codewave.utils.xml.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.itunes.ITunesLibrary
 */
public class ITunesLibrary implements Serializable {
    private static final Log LOG = LogFactory.getLog(ITunesLibrary.class);

    private List<MusicFile> myTitles = new ArrayList<MusicFile>();
    private List<PlayList> myPlayLists = new ArrayList<PlayList>();

    public void load(URL iTunesLibraryXml, String fakeMp3Suffix, String fakeM4aSuffix)
            throws IOException, SAXException, ParserConfigurationException {
        Map plist = (Map)XmlUtils.parseApplePList(iTunesLibraryXml);
        Set<String> trackIds = createListOfMusicFiles(plist, fakeMp3Suffix, fakeM4aSuffix);
        createListOfPlayLists(plist, trackIds);
    }

    private void createListOfPlayLists(Map plist, Set<String> trackIds) {
        List<Map<String, Object>> playlists = (List<Map<String, Object>>)plist.get("Playlists");
        for (Iterator<Map<String, Object>> iterator = playlists.iterator(); iterator.hasNext();) {
            Map<String, Object> playlistMap = iterator.next();
            if (!Boolean.TRUE.equals(playlistMap.get("Master"))) { // ignore master list
                PlayList playlist = new PlayList((String)playlistMap.get("Playlist ID"), (String)playlistMap.get("Name"));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found playlist with name \"" + playlist.getName() + "\".");
                }
                List<Map<String, String>> items = (List<Map<String, String>>)playlistMap.get("Playlist Items");
                if (items != null && !items.isEmpty()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Playlist with name \"" + playlist.getName() + "\" is not empty.");
                    }
                    for (Iterator<Map<String, String>> itemIterator = items.iterator(); itemIterator.hasNext();) {
                        Map<String, String> item = itemIterator.next();
                        String trackId = item.get("Track ID");
                        if (trackIds.contains(trackId)) {
                            List<MusicFile> musicFiles = getMatchingFiles(new MusicFileIdSearch(trackId));
                            if (musicFiles.size() == 1) {
                                playlist.addMusicFile(musicFiles.get(0));
                            }
                        }
                    }
                    if (!playlist.getMusicFiles().isEmpty()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Added playlist with name \"" + playlist.getName() + "\".");
                        }
                        myPlayLists.add(playlist);
                    }
                }
            }
        }
        Collections.sort(myPlayLists, new PlayListComparator());
    }

    private Set<String> createListOfMusicFiles(Map plist, String fakeMp3Suffix, String fakeM4aSuffix) {
        Set<String> trackIds = new HashSet<String>();
        Map<String, Map<String, String>> tracks = (Map<String, Map<String, String>>)plist.get("Tracks");
        for (Iterator<Map<String, String>> trackIterator = tracks.values().iterator(); trackIterator.hasNext();) {
            Map<String, String> track = trackIterator.next();
            MusicFile musicFile = new MusicFile(fakeMp3Suffix, fakeM4aSuffix);
            musicFile.setAlbum(track.get("Album"));
            musicFile.setArtist(track.get("Artist"));
            musicFile.setId(track.get("Track ID"));
            musicFile.setName(track.get("Name"));
            String trackNumber = track.get("Track Number");
            musicFile.setTrackNumber(StringUtils.isNotEmpty(trackNumber) ? Integer.parseInt(trackNumber) : 0);
            String location = track.get("Location").substring("file://localhost".length());
            try {
                musicFile.setFile(new File(URLDecoder.decode(location, "UTF-8")));
            } catch (UnsupportedEncodingException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Could not create file for \"" + location + "\".", e);
                }
            }
            if (musicFile.isValid()) {
                myTitles.add(musicFile);
                trackIds.add(musicFile.getId());
            }
        }
        Collections.sort(myTitles, new MusicFileComparator());
        return trackIds;
    }

    public List<MusicFile> getMatchingFiles(MusicFileSearch... searches) {
        List<MusicFile> matchingFiles = new ArrayList<MusicFile>();
        if (searches != null && searches.length > 0) {
            for (Iterator<MusicFile> iterator = myTitles.iterator(); iterator.hasNext();) {
                MusicFile file = iterator.next();
                boolean match = true;
                for (int i = 0; i < searches.length; i++) {
                    if (!searches[i].matches(file)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    matchingFiles.add(file);
                }
            }
        }
        return matchingFiles;
    }

    public List<PlayList> getPlayLists() {
        return new ArrayList<PlayList>(myPlayLists);
    }

    public PlayList getPlayListWithId(String id) {
        if (StringUtils.isNotEmpty(id)) {
            for (Iterator<PlayList> iterator = myPlayLists.iterator(); iterator.hasNext();) {
                PlayList playList = iterator.next();
                if (id.equals(playList.getId())) {
                    return playList;
                }
            }
        }
        return null;
    }

    private static class MusicFileComparator implements Comparator<MusicFile> {
        public int compare(MusicFile m1, MusicFile m2) {
            int value = m1.getArtist().compareTo(m2.getArtist());
            if (value == 0) {
                value = m1.getAlbum().compareTo(m2.getAlbum());
                if (value == 0) {
                    value = m1.getTrackNumber() - m2.getTrackNumber();
                    if (value == 0) {
                        value = m1.getName().compareTo(m2.getName());
                    }
                }
            }
            return value;
        }
    }

    private static class PlayListComparator implements Comparator<PlayList> {
        public int compare(PlayList p1, PlayList p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }
}
