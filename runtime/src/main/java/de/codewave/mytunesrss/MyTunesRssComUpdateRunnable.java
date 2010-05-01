/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * de.codewave.mytunesrss.DatabaseWatchdogTask
 */
public class MyTunesRssComUpdateRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MyTunesRssComUpdateRunnable.class);
    private static final String MYTUNESRSSCOM_URL = MyTunesRss.MYTUNESRSSCOM_TOOLS_URL + "/save_ip.php";
    public static MyTunesRssEvent LAST_UPDATE_EVENT;

    public void run() {
        if (MyTunesRss.CONFIG.isMyTunesRssComActive()) {
            String username = MyTunesRss.CONFIG.getMyTunesRssComUser();
            String base64Hash = MyTunesRssUtils.getUtf8String(Base64.encodeBase64(MyTunesRss.CONFIG.getMyTunesRssComPasswordHash()));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating mytunesrss.com: user=\"" + username + "\", password=\"" + base64Hash + "\", port=\"" + MyTunesRss.CONFIG.getPort() +
                        "\".");
            }
            PostMethod postMethod = new PostMethod(System.getProperty("MyTunesRSS.mytunesrsscomUrl", MYTUNESRSSCOM_URL));
            postMethod.addParameter("user", username);
            postMethod.addParameter("pass", base64Hash);
            if (MyTunesRss.CONFIG.isMyTunesRssComSsl()) {
                if (!"http".equals(MyTunesRss.CONFIG.getTomcatSslProxyScheme())) {
                    postMethod.addParameter("https", "true");
                }
                if (StringUtils.isNotBlank(MyTunesRss.CONFIG.getTomcatSslProxyHost())) {
                    postMethod.addParameter("host", MyTunesRss.CONFIG.getTomcatSslProxyHost());
                }
                if (MyTunesRss.CONFIG.getTomcatSslProxyPort() > 0 && MyTunesRss.CONFIG.getTomcatSslProxyPort() < 65536) {
                    postMethod.addParameter("port", Integer.toString(MyTunesRss.CONFIG.getTomcatSslProxyPort()));
                } else {
                    postMethod.addParameter("port", Integer.toString(MyTunesRss.CONFIG.getSslPort()));
                }
            } else {
                if ("https".equals(MyTunesRss.CONFIG.getTomcatProxyScheme())) {
                    postMethod.addParameter("https", "true");
                }
                if (StringUtils.isNotBlank(MyTunesRss.CONFIG.getTomcatProxyHost())) {
                    postMethod.addParameter("host", MyTunesRss.CONFIG.getTomcatProxyHost());
                }
                if (MyTunesRss.CONFIG.getTomcatProxyPort() > 0 && MyTunesRss.CONFIG.getTomcatProxyPort() < 65536) {
                    postMethod.addParameter("port", Integer.toString(MyTunesRss.CONFIG.getTomcatProxyPort()));
                } else {
                    postMethod.addParameter("port", Integer.toString(MyTunesRss.CONFIG.getPort()));
                }
            }
            postMethod.addParameter("context", MyTunesRss.CONFIG.getWebappContext());
            HttpClient client = MyTunesRssUtils.createHttpClient();
            try {
                MyTunesRssEvent event = MyTunesRssEvent.create(MyTunesRssEvent.EventType.MYTUNESRSS_COM_UPDATED);
                LAST_UPDATE_EVENT = event;
                int responseCode = client.executeMethod(postMethod);
                if (responseCode != 200) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Could not update mytunesrss.com (Status code " + responseCode + ").");
                    }
                    event.setMessageKey("mytunesrsscom.invalidLogin");
                } else {
                    event.setMessageKey("mytunesrsscom.updateOk");
                    event.setMessageParams(new SimpleDateFormat(MyTunesRssUtils.getBundleString(Locale.getDefault(), "settings.lastMyTunesRssComUpdateDateFormat")).format(new Date()));
                }
                MyTunesRssEventManager.getInstance().fireEvent(event);
            } catch (IOException e) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Could not update mytunesrss.com", e);
                }
            } finally {
                postMethod.releaseConnection();
            }
        }
    }
}