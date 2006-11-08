/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.*;
import de.codewave.mytunesrss.jsp.Error;
import de.codewave.mytunesrss.jsp.*;
import de.codewave.mytunesrss.servlet.*;
import de.codewave.utils.*;
import de.codewave.utils.servlet.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import javax.servlet.*;
import java.io.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.command.MyTunesRssCommandHandler
 */
public abstract class MyTunesRssCommandHandler extends CommandHandler {
    private static final Log LOG = LogFactory.getLog(MyTunesRssCommandHandler.class);

    protected MyTunesRssConfig getMyTunesRssConfig() {
        return (MyTunesRssConfig)getSession().getServletContext().getAttribute(MyTunesRssConfig.class.getName());
    }

    protected boolean isAuthorized(String userName, byte[] passwordHash) {
        MyTunesRssConfig config = getMyTunesRssConfig();
        User user = config.getUser(userName);
        return user != null && Arrays.equals(user.getPasswordHash(), passwordHash);
    }

    protected void authorize(String userName) {
        User user = getMyTunesRssConfig().getUser(userName);
        if (user != null) {
            getSession().setAttribute("auth", MyTunesRssBase64Utils.encode(user.getName()) + " " + MyTunesRssBase64Utils.encode(user.getPasswordHash()));
            getSession().setAttribute("authUser", getMyTunesRssConfig().getUser(userName));
        }
    }

    protected User getAuthUser() {
        return (User)getSession().getAttribute("authUser");
    }

    protected boolean needsAuthorization() {
        if (getSession().getAttribute("auth") != null) {
            return false;
        } else {
            if (StringUtils.isNotEmpty(getRequest().getParameter("auth"))) {
                try {
                    String auth = getRequestParameter("auth", "");
                    int i = auth.indexOf(" ");
                    if (i >= 1 && i < auth.length() - 1) {
                        byte[] requestAuthHash = MyTunesRssBase64Utils.decode(auth.substring(i + 1));
                        String userName = MyTunesRssBase64Utils.decodeToString(auth.substring(0, i));
                        if (isAuthorized(userName, requestAuthHash)) {
                            authorize(userName);
                            return false;
                        }
                    }
                } catch (NumberFormatException e) {
                    // intentionally left blank
                }
            }
            return true;
        }
    }

    protected void addError(Error error) {
        List<Error> errors = (List<Error>)getSession().getAttribute("errors");
        if (errors == null) {
            synchronized (getSession()) {
                errors = (List<Error>)getSession().getAttribute("errors");
                if (errors == null) {
                    errors = new ArrayList<Error>();
                    getSession().setAttribute("errors", errors);
                }
            }
        }
        errors.add(error);
    }

    protected MyTunesRssDataStore getDataStore() {
        return (MyTunesRssDataStore)getContext().getAttribute(MyTunesRssDataStore.class.getName());
    }

    protected void forward(MyTunesRssResource resource) throws IOException, ServletException {
        prepareRequestForResource();
        resource.beforeForward(getRequest(), getResponse());
        if (getRequest().getHeader("User-Agent").contains("PSP")) {
            getResponse().setHeader("Cache-Control", "no-cache");
            getResponse().setHeader("Pragma", "no-cache");
            getResponse().setDateHeader("Expires", 0);
        }
        forward(resource.getValue());
    }

    private void prepareRequestForResource() {
        getRequest().setAttribute("servletUrl", MyTunesRssWebUtils.getServletUrl(getRequest()));
        getRequest().setAttribute("appUrl", ServletUtils.getApplicationUrl(getRequest()));
        getRequest().setAttribute("mytunesrssVersion", MyTunesRss.VERSION);
        getWebConfig();// result not needed, method also fills the request attribute "config"
    }

    protected WebConfig getWebConfig() {
        WebConfig webConfig = (WebConfig)getRequest().getAttribute("config");
        if (webConfig == null) {
            webConfig = new WebConfig();
            webConfig.load(getRequest());
            getRequest().setAttribute("config", webConfig);
        }
        return webConfig;
    }

    protected void forward(MyTunesRssCommand command) throws IOException, ServletException {
        prepareRequestForResource();
        forward("/mytunesrss/" + command.getName());
    }

    protected void redirect(String url) throws IOException {
        getResponse().sendRedirect(url.replace("&amp;", "&"));
    }

    protected Map<String, Boolean> getStates() {
        Map<String, Boolean> states = (Map<String, Boolean>)getSession().getAttribute("states");
        if (states == null) {
            synchronized (getSession()) {
                states = (Map<String, Boolean>)getSession().getAttribute("states");
                if (states == null) {
                    states = new HashMap<String, Boolean>();
                    getSession().setAttribute("states", states);
                }
            }
        }
        return states;
    }

    public void execute() throws Exception {
        try {
            if (needsAuthorization() && getWebConfig().isLoginStored() && isAuthorized(getWebConfig().getUserName(),
                                                                                       getWebConfig().getPasswordHash())) {
                authorize(getWebConfig().getUserName());
                executeAuthorized();
            } else if (needsAuthorization()) {
                handleSingleUser();
                forward(MyTunesRssResource.Login);
            } else {
                executeAuthorized();
            }
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unhandled exception: ", e);
            }
            getSession().removeAttribute("errors");
            redirect(ServletUtils.getApplicationUrl(getRequest()) + "/mytunesrss" + "/" + MyTunesRssCommand.ShowFatalError.getName());
        }
    }

    public void executeAuthorized() throws Exception {
        // intentionally left blank
    }

    protected Pager createPager(int itemCount, int current) {
        int pageSize = getWebConfig().getEffectivePageSize();
        if (pageSize > 0) {
            List<Pager.Page> pages = new ArrayList<Pager.Page>();
            int page = 0;
            for (int index = 0; index < itemCount; index += pageSize) {
                pages.add(new Pager.Page(Integer.toString(page), Integer.toString(page + 1)));
                page++;
            }
            Pager pager = new Pager(pages, 10);
            pager.moveToPage(current);
            return pager;
        }
        return null;
    }

    protected String getBundleString(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle("de/codewave/mytunesrss/MyTunesRSSWeb", getRequest().getLocale());
        return bundle.getString(key);
    }

    protected void handleSingleUser() {
        Collection<User> users = getMyTunesRssConfig().getUsers();
        if (users != null && users.size() == 1) {
            getRequest().setAttribute("singleUserName", users.iterator().next().getName());
        }
    }
}