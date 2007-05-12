package de.codewave.mytunesrss.servlet;

import de.codewave.mytunesrss.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import javax.crypto.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

/**
 * de.codewave.mytunesrss.servlet.EncodingFilter
 */
public class PathInfoDecryptionFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(PathInfoDecryptionFilter.class);

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if ("true".equalsIgnoreCase(System.getProperty("encryptPathInfo")) && MyTunesRss.CONFIG.getPathInfoKey() != null) {
            servletRequest = new RequestWrapper((HttpServletRequest)servletRequest);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // intentionally left blank
    }

    public void destroy() {
        // intentionally left blank
    }

    private static class RequestWrapper extends HttpServletRequestWrapper {
        public RequestWrapper(HttpServletRequest servletRequest) {
            super(servletRequest);
        }

        @Override
        public String getPathInfo() {
            SecretKey key = MyTunesRss.CONFIG.getPathInfoKey();
            String pathInfo = ((HttpServletRequest)getRequest()).getPathInfo();
            if (StringUtils.isNotEmpty(pathInfo)) {
                String[] splitted = StringUtils.split(pathInfo, "/");
                if (splitted != null && splitted.length > 1) {
                    try {
                        Cipher cipher = Cipher.getInstance("DES");
                        cipher.init(Cipher.DECRYPT_MODE, key);
                        splitted[1] = new String(cipher.doFinal(MyTunesRssBase64Utils.decode(splitted[1])), "UTF-8");
                        String newPathInfo = "/" + StringUtils.join(splitted, "/");
                        return URLDecoder.decode(newPathInfo, "UTF-8");
                    } catch (Exception e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Could not descrypt path info.", e);
                        }
                    }
                }
            }
            return pathInfo;
        }
    }
}