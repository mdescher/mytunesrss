/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.servlet;

import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.servlet.WebConfig
 */
public class WebConfig {
    private static final Log LOG = LogFactory.getLog(WebConfig.class);

    private static final String CONFIG_COOKIE_NAME = "MyTunesRSSConfig";
    private static final String CFG_LOGIN_COOKIE = "rememberLogin";
    private static final String CFG_FEED_TYPES = "feedTypes";
    private static final String CFG_SUFFIX = "suffix.";
    private static final String CFG_RSS_LIMIT = "rssLimit";
    private static final String CFG_PAGE_SIZE = "pageSize";
    private static Map<String, String> FEED_FILE_SUFFIXES = new HashMap<String, String>();

    static {
        FEED_FILE_SUFFIXES.put("rss", "xml");
        FEED_FILE_SUFFIXES.put("m3u", "m3u");
    }

    Map<String, String> myConfigValues = new HashMap<String, String>();

    public void clear() {
        myConfigValues.clear();
    }

    private void initWithDefaults() {
        myConfigValues.put(CFG_FEED_TYPES, "rss,m3u");
        myConfigValues.put(CFG_RSS_LIMIT, "0");
        myConfigValues.put(CFG_LOGIN_COOKIE, "false");
        myConfigValues.put(CFG_PAGE_SIZE, "0");
    }

    public void load(HttpServletRequest request) {
        clear();
        initWithDefaults();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (CONFIG_COOKIE_NAME.equals(cookie.getName())) {
                    String cookieValue = decode(cookie.getValue());
                    for (String keyValueToken : StringUtils.split(cookieValue, ';')) {
                        String[] keyValuePair = StringUtils.split(keyValueToken, '=');
                        if (keyValuePair.length > 0) {
                            myConfigValues.put(keyValuePair[0], keyValuePair.length > 1 ? keyValuePair[1] : "");
                        } else {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Illegal configuration token found in cookie: \"" + keyValueToken + "\".");
                            }
                        }
                    }
                }
            }
        }
    }

    public void save(HttpServletResponse response) {
        StringBuffer value = new StringBuffer();
        for (Map.Entry<String, String> entry : myConfigValues.entrySet()) {
            value.append(";").append(entry.getKey()).append("=").append(entry.getValue());
        }
        Cookie cookie = new Cookie(CONFIG_COOKIE_NAME, encode(value.substring(1)));
        cookie.setComment("MyTunesRSS settings cookie");
        cookie.setMaxAge(3600 * 24 * 365); // one year
        response.addCookie(cookie);
    }

    private String encode(String text) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            buffer.append(Integer.toHexString(text.charAt(i)));
        }
        return buffer.toString().toUpperCase();
    }

    private String decode(String text) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < text.length(); i+= 2) {
            buffer.append((char)Integer.parseInt(text.substring(i, i + 2), 16));
        }
        return buffer.toString();
    }

    public String getSuffix(File file) {
        String name = file.getName();
        int suffixSeparatorIndex = name.lastIndexOf(".");
        if (suffixSeparatorIndex > -1 && suffixSeparatorIndex + 1 < name.length()) {
            String suffix = name.substring(suffixSeparatorIndex + 1);
            String pseudoSuffix = myConfigValues.get(CFG_SUFFIX + suffix.toLowerCase());
            return StringUtils.isNotEmpty(pseudoSuffix) ? pseudoSuffix : suffix;
        }
        return null;
    }

    public void addFileSuffix(String originalSuffix, String fakeSuffix) {
        myConfigValues.put(CFG_SUFFIX + originalSuffix.toLowerCase(), fakeSuffix.toLowerCase());
    }

    public boolean isRememberLogin() {
        String rememberLogin = myConfigValues.get(CFG_LOGIN_COOKIE);
        return Boolean.valueOf(rememberLogin);
    }

    public void setRememberLogin(boolean rememberLogin) {
        myConfigValues.put(CFG_LOGIN_COOKIE, Boolean.toString(rememberLogin));
    }

    public String[] getFeedTypes() {
        String feedTypes = myConfigValues.get(CFG_FEED_TYPES);
        return StringUtils.split(feedTypes, ',');
    }

    public void addFeedType(String feedType) {
        String feedTypes = myConfigValues.get(CFG_FEED_TYPES);
        myConfigValues.put(CFG_FEED_TYPES, feedTypes != null ? feedTypes + "," + feedType : feedType);
    }

    public int getRssFeedLimit() {
        String rssLimit = myConfigValues.get(CFG_RSS_LIMIT);
        if (StringUtils.isNotEmpty(rssLimit)) {
            return Integer.parseInt(rssLimit);
        }
        return 0;
    }

    public void setRssFeedLimit(int rssFeedLimit) {
        myConfigValues.put(CFG_RSS_LIMIT, Integer.toString(rssFeedLimit));
    }

    public Map<String, String> getFeedFileSuffix() {
        return FEED_FILE_SUFFIXES;
    }

    public int getPageSize() {
        return Integer.parseInt(myConfigValues.get(CFG_PAGE_SIZE));
    }

    public void setPageSize(int pageSize) {
        myConfigValues.put(CFG_PAGE_SIZE, Integer.toString(pageSize));
    }
}