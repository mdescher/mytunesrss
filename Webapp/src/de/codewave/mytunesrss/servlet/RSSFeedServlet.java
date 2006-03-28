/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.servlet;

import de.codewave.mytunesrss.itunes.*;
import de.codewave.mytunesrss.musicfile.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class RSSFeedServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doCommand(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doCommand(request, response);
    }

    private void doCommand(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ITunesLibrary library = ITunesLibraryContextListener.getLibrary(request);
        Collection<MusicFile> feedFiles = new ArrayList<MusicFile>();
        for (StringTokenizer tokenizer = new StringTokenizer(request.getPathInfo(), "/"); tokenizer.hasMoreTokens();) {
            String token = tokenizer.nextToken();
            if (token.startsWith("channel=")) {
                request.setAttribute("channel", token.substring("channel=".length()));
            } else {
                feedFiles.addAll(library.getMatchingFiles(new MusicFileIdSearch(token)));
            }
        }
        request.setAttribute("musicFiles", feedFiles);
        request.setAttribute("pubDate", new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US).format(new Date()));
        request.getRequestDispatcher("/rss.jsp").forward(request, response);
    }
}
