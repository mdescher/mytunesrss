/*
 * Copyright (c) 2012. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.rest;

import de.codewave.mytunesrss.MyTunesRssWebUtils;
import de.codewave.mytunesrss.command.WebAppScope;
import de.codewave.mytunesrss.rest.resource.LibraryResource;
import de.codewave.mytunesrss.rest.resource.SessionResource;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

@Provider
@ServerInterceptor
public class AuthInterceptor implements PreProcessInterceptor, AcceptedByMethod {

    @Context
    private HttpServletRequest myRequest;

    public ServerResponse preProcess(HttpRequest request, ResourceMethod method) {
        if (MyTunesRssWebUtils.getAuthUser(myRequest) == null) {
            if (MyTunesRssWebUtils.isAuthorized(MyTunesRssWebUtils.getRememberedUsername(myRequest), MyTunesRssWebUtils.getRememberedPasswordHash(myRequest))) {
                MyTunesRssWebUtils.authorize(WebAppScope.Session, myRequest, MyTunesRssWebUtils.getRememberedUsername(myRequest));
                return null;
            }
            throw new MyTunesRssRestException(HttpServletResponse.SC_UNAUTHORIZED, "NO_VALID_USER_SESSION");
        }
        return null;
    }

    public boolean accept(Class declaring, Method method) {
        if (declaring.equals(LibraryResource.class) && "getLibrary".equals(method.getName())) {
            return false;
        }
        if (declaring.equals(SessionResource.class) && "loginOrPing".equals(method.getName())) {
            return false;
        }
        if ("handleCorsOptions".equals(method.getName())) {
            return false;
        }
        return true;
    }
}
