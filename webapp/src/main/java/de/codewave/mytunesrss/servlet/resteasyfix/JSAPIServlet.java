package de.codewave.mytunesrss.servlet.resteasyfix;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssWebUtils;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.jsapi.ServiceRegistry;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class JSAPIServlet extends HttpServlet
{

	private static final long serialVersionUID = -1985015444704126795L;

	private final static Logger logger = Logger.getLogger(JSAPIServlet.class);
    
	private transient ServiceRegistry service;

	private transient JSAPIWriter apiWriter;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		if (logger.isDebugEnabled())
			logger.info("Loading JSAPI Servlet");

		scanResources();

		if (logger.isDebugEnabled())
			logger.debug("JSAPIServlet loaded");

		// make it possible to get to us for rescanning
		ServletContext servletContext = config .getServletContext();
		servletContext.setAttribute(getClass().getName(), this);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String pathInfo = req.getPathInfo();
		String uri = req.getRequestURL().toString();
		uri = uri.substring(0, uri.length() - req.getServletPath().length());
		if (logger.isDebugEnabled())
		{
			logger.debug("Serving " + pathInfo);
			logger.debug("Query " + req.getQueryString());
		}
        resp.setHeader("Cache-Control", MyTunesRssWebUtils.createCacheControlValue(MyTunesRss.CONFIG.getRestApiJsExpirationMillis() / 1000));
        resp.setDateHeader("Expires", System.currentTimeMillis() + MyTunesRss.CONFIG.getRestApiJsExpirationMillis());
        PrintWriter printWriter = resp.getWriter();
        this.apiWriter.writeJavaScript(uri, printWriter, service);
	}

	public void scanResources(){

		ServletConfig config = getServletConfig();
		ServletContext servletContext = config .getServletContext();
		ResourceMethodRegistry registry = (ResourceMethodRegistry) servletContext
		.getAttribute(Registry.class.getName());
		ResteasyProviderFactory providerFactory =
			(ResteasyProviderFactory) servletContext.getAttribute(ResteasyProviderFactory.class.getName());

		String restPath = servletContext
		.getInitParameter("resteasy.servlet.mapping.prefix");

		service = new ServiceRegistry(null, registry, providerFactory, null);
		apiWriter = new JSAPIWriter(restPath);
	}
}
