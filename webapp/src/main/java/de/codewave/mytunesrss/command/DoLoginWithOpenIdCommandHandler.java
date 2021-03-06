/*
 * Copyright (c) 2011. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssCommandCallBuilder;
import de.codewave.mytunesrss.MyTunesRssWebUtils;
import de.codewave.mytunesrss.jsp.BundleError;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import de.codewave.utils.servlet.ServletUtils;
import org.apache.commons.lang3.StringUtils;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.util.HttpClientFactory;
import org.openid4java.util.ProxyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Command handler for submission of login form.
 */
public class DoLoginWithOpenIdCommandHandler extends DoLoginCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoLoginWithOpenIdCommandHandler.class);

    @Override
    public void execute() throws Exception {
        if (!MyTunesRss.CONFIG.isDisableWebLogin() && MyTunesRss.CONFIG.isOpenIdActive() && !isSessionAuthorized()) {
            String openId = getRequest().getParameter("openId");
            try {
                // TODO this should be set each time the proxy config changes but the http4 client is in the web app only at the moment
                if (MyTunesRss.CONFIG.isProxyServer()) {
                    ProxyProperties proxyProps = new ProxyProperties();
                    proxyProps.setProxyHostName(MyTunesRss.CONFIG.getProxyHost());
                    proxyProps.setProxyPort(MyTunesRss.CONFIG.getProxyPort());
                    HttpClientFactory.setProxyProperties(proxyProps);
                } else {
                    HttpClientFactory.setProxyProperties(null);
                }
                ConsumerManager manager = new ConsumerManager();
                manager.setConnectTimeout(30000);
                manager.setSocketTimeout(30000);
                List discoveries = manager.discover(openId);
                DiscoveryInformation discovered = manager.associate(discoveries);
                getSession().setAttribute("openIdConsumerManager", manager);
                getSession().setAttribute("openidDiscovered", discovered);
                MyTunesRssCommandCallBuilder callBuilder = new MyTunesRssCommandCallBuilder(MyTunesRssCommand.ValidateOpenId);
                callBuilder.addParam("lc", StringUtils.trimToNull(getRequest().getParameter("lc")));
                callBuilder.addParam("openId", StringUtils.trimToNull(getRequest().getParameter("openId")));
                callBuilder.addParam("rememberLogin", StringUtils.trimToNull(getRequest().getParameter("rememberLogin")));
                AuthRequest authReq = manager.authenticate(discovered, callBuilder.getCall(getRequest()), MyTunesRssWebUtils.getServletUrl(getRequest()));
                FetchRequest fetchRequest = FetchRequest.createFetchRequest();
                fetchRequest.addAttribute("email", "http://schema.openid.net/contact/email", true, 1);
                authReq.addExtension(fetchRequest);
                redirect(authReq.getDestinationUrl(true));
                return; // done
            } catch (DiscoveryException | IOException | ConsumerException | MessageException e) {
                LOGGER.debug("No open id login possible with username \"" + openId, e);
            }
            addError(new BundleError("error.openIdProviderFailure"));
            MyTunesRss.ADMIN_NOTIFY.notifyLoginFailure(StringUtils.trimToEmpty(getRequest().getParameter("openId")), ServletUtils.getBestRemoteAddress(getRequest()));
            removeLoginSessionAttributes();
        }
        redirect(MyTunesRssWebUtils.getResourceCommandCall(getRequest(), MyTunesRssResource.Login));
    }

    protected void removeLoginSessionAttributes() {
        getSession().removeAttribute("openIdConsumerManager");
        getSession().removeAttribute("openidDiscovered");
    }
}
