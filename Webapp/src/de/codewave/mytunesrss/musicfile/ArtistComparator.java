/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.musicfile;

import java.util.*;

/**
 * de.codewave.mytunesrss.musicfile.ArtistComparator
 */
public class ArtistComparator implements Comparator<MusicFile> {
    public int compare(MusicFile o1, MusicFile o2) {
        int value = o1.getArtist().compareTo(o2.getArtist());
        if (value == 0) {
            value = o1.getAlbum().compareTo(o2.getAlbum());
            if (value == 0) {
                value = o1.getTrackNumber() - o2.getTrackNumber();
            }
        }
        return value;
    }
}