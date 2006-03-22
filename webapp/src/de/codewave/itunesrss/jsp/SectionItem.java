/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.itunesrss.jsp;

import de.codewave.itunesrss.musicfile.*;

import java.io.*;

/**
 * de.codewave.itunesrss.jsp.SectionItem
 */
public class SectionItem implements Serializable {
    private MusicFile myFile;
    private boolean mySelected;

    public SectionItem(MusicFile file, boolean selected) {
        myFile = file;
        mySelected = selected;
    }

    public MusicFile getFile() {
        return myFile;
    }

    public void setFile(MusicFile file) {
        myFile = file;
    }

    public boolean isSelected() {
        return mySelected;
    }

    public void setSelected(boolean selected) {
        mySelected = selected;
    }
}