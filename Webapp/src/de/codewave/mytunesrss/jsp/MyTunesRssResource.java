/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.jsp;

public enum MyTunesRssResource {
    Login("/login.jsp"),
    Portal("/portal.jsp");

    private String myValue;

    MyTunesRssResource(String value) {
        myValue = value;
    }

    public String getValue() {
        return myValue;
    }
}
