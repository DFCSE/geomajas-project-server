/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2014 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */
package org.geomajas.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * Special resource servlet for efficiently resolving and rendering static resources from within a JAR file. The cache
 * header is set to a date in the far future to improve caching. For development, you can specify a "files-location"
 * servlet init parameter. Any resources are searched at that location first. When found, the cache header is not set.
 * This assures the jar does not need building for testing.
 *
 * @author Jan De Moerloose
 * @deprecated use DispatcherServlet which use the ResourceController (but changes base URL,
 * probably from js to data/resource). Not deleted to allow 1.6.0 configurations to work.
 */
@Deprecated
public class ResourceServlet extends HttpServlet {

	private static final long serialVersionUID = 190L;

	private static final String HTTP_CONTENT_LENGTH_HEADER = "Content-Length";
	private static final String HTTP_LAST_MODIFIED_HEADER = "Last-Modified";
	private static final String HTTP_EXPIRES_HEADER = "Expires";
	private static final String HTTP_CACHE_CONTROL_HEADER = "Cache-Control";
	private static final String INIT_PARAM_LOCATION = "files-location";

	private final Logger log = LoggerFactory.getLogger(ResourceServlet.class);
	private static final String PROTECTED_PATH = "/?WEB-INF/.*";
	private final String[] allowedResourcePaths = new String[]{
			"/**/*.css", "/**/*.gif", "/**/*.ico", "/**/*.jpeg",
			"/**/*.jpg", "/**/*.js", "/**/*.html", "/**/*.png",
			"META-INF/**/*.css", "META-INF/**/*.gif", "META-INF/**/*.ico", "META-INF/**/*.jpeg",
			"META-INF/**/*.jpg", "META-INF/**/*.js", "META-INF/**/*.html", "META-INF/**/*.png",
	};

	private File fileLocation;

	private static final Map<String, String> DEFAULT_MIME_TYPES = new HashMap<String, String>();
	private static final Set<String> COMPRESSED_MIME_TYPES = new HashSet<String>();

	static
	{
		DEFAULT_MIME_TYPES.put(".css", "text/css");
		DEFAULT_MIME_TYPES.put(".gif", "image/gif");
		DEFAULT_MIME_TYPES.put(".ico", "image/vnd.microsoft.icon");
		DEFAULT_MIME_TYPES.put(".jpeg", "image/jpeg");
		DEFAULT_MIME_TYPES.put(".jpg", "image/jpeg");
		DEFAULT_MIME_TYPES.put(".js", "text/javascript");
		DEFAULT_MIME_TYPES.put(".png", "image/png");

		COMPRESSED_MIME_TYPES.add("text/css");
		COMPRESSED_MIME_TYPES.add("text/javascript");
		COMPRESSED_MIME_TYPES.add("application/javascript");
		COMPRESSED_MIME_TYPES.add("application/x-javascript");
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String fileLocationString = getInitParameter(INIT_PARAM_LOCATION);
		if (log.isDebugEnabled()) {
			log.debug("init-param {}=|{}|", INIT_PARAM_LOCATION, fileLocationString);
			Enumeration en = getInitParameterNames();
			while (en.hasMoreElements()) {
				log.debug("init-params |{}|", en.nextElement());
			}
		}
		if (null != fileLocationString) {
			File location = new File(fileLocationString);
			if (location.exists() && location.isDirectory()) {
				fileLocation = location;
			}
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String rawResourcePath = request.getPathInfo();

		log.debug("Attempting to GET resource: {}", rawResourcePath);

		URL[] resources = getRequestResourceUrls(request);

		if (resources == null || resources.length == 0) {
			log.debug("Resource not found: {}", rawResourcePath);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		prepareResponse(response, resources, rawResourcePath);

		OutputStream out = selectOutputStream(request, response);

		try {
			for (URL resource : resources) {
				URLConnection resourceConn = resource.openConnection();
				InputStream in = resourceConn.getInputStream();
				try {
					byte[] buffer = new byte[1024];
					while (in.available() > 0) {
						int len = in.read(buffer);
						out.write(buffer, 0, len);
					}
				} finally {
					in.close();
					try {
						resourceConn.getOutputStream().close();
					} catch (IOException e) {
						/*ignore, just trying to free resources*/
					}
				}
			}
		} finally {
			out.close();
		}
	}

	private OutputStream selectOutputStream(HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String acceptEncoding = request.getHeader("Accept-Encoding");
		String mimeType = response.getContentType();

		if (StringUtils.hasText(acceptEncoding) && acceptEncoding.contains("gzip")
				&& COMPRESSED_MIME_TYPES.contains(mimeType)) {
			log.debug("Enabling GZIP compression for the current response.");
			return new GzipResponseStream(response);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No compression for the current response.");
				log.debug("Accept-Encoding : " + acceptEncoding);
				log.debug("Content-type : " + mimeType);
			}

			return response.getOutputStream();
		}
	}

	private void prepareResponse(HttpServletResponse response, URL[] resources, String rawResourcePath)
			throws IOException {
		long lastModified = -1;
		int contentLength = 0;
		boolean isFile = false;
		String mimeType = null;
		for (URL resource : resources) {
			if ("file".equals(resource.getProtocol())) {
				isFile = true;
			}
			URLConnection resourceConn = resource.openConnection();
			if (resourceConn.getLastModified() > lastModified) {
				lastModified = resourceConn.getLastModified();
			}

			String currentMimeType = getServletContext().getMimeType(resource.getPath());
			if (currentMimeType == null) {
				String extension = resource.getPath().substring(resource.getPath().lastIndexOf('.'));
				currentMimeType = DEFAULT_MIME_TYPES.get(extension);
			}
			if (mimeType == null) {
				mimeType = currentMimeType;
			} else if (!mimeType.equals(currentMimeType)) {
				throw new MalformedURLException("Combined resource path: " + rawResourcePath
						+ " is invalid. All resources in a combined resource path must be of the same mime type.");
			}
			contentLength += resourceConn.getContentLength();
			try {
				resourceConn.getInputStream().close();
			} catch (IOException e) {
				/*ignore, just trying to free resources*/
			}
			try {
				resourceConn.getOutputStream().close();
			} catch (IOException e) {
				/*ignore, just trying to free resources*/
			}
		}

		response.setContentType(mimeType);
		response.setHeader(HTTP_CONTENT_LENGTH_HEADER, Long.toString(contentLength));
		response.setDateHeader(HTTP_LAST_MODIFIED_HEADER, lastModified);
		configureCaching(response, isFile ? 0 : 31556926);
	}

	protected long getLastModified(HttpServletRequest request) {
		log.debug("Checking last modified of resource: {}", request.getPathInfo());
		URL[] resources;
		try {
			resources = getRequestResourceUrls(request);
		} catch (MalformedURLException e) {
			return -1;
		}

		if (resources == null || resources.length == 0) {
			return -1;
		}

		long lastModified = -1;

		for (URL resource : resources) {
			URLConnection resourceConn;
			try {
				resourceConn = resource.openConnection();
			} catch (IOException e) {
				return -1;
			}
			if (resourceConn.getLastModified() > lastModified) {
				lastModified = resourceConn.getLastModified();
			}
			try {
				resourceConn.getInputStream().close();
			} catch (IOException e) {
				/*ignore, just trying to free resources*/
			}
			try {
				resourceConn.getOutputStream().close();
			} catch (IOException e) {
				/*ignore, just trying to free resources*/
			}
		}
		return lastModified;
	}

	private URL[] getRequestResourceUrls(HttpServletRequest request) throws MalformedURLException {

		String rawResourcePath = request.getPathInfo();
		String appendedPaths = request.getParameter("appended");
		if (StringUtils.hasText(appendedPaths)) {
			rawResourcePath = rawResourcePath + "," + appendedPaths;
		}
		String[] localResourcePaths = StringUtils.delimitedListToStringArray(rawResourcePath, ",");
		URL[] resources = new URL[localResourcePaths.length];
		for (int i = 0; i < localResourcePaths.length; i++) {
			String localResourcePath = localResourcePaths[i];
			if (!isAllowed(localResourcePath)) {
				if (log.isWarnEnabled()) {
					log.warn("An attempt to access a protected resource at " + localResourcePath + " was disallowed.");
				}
				return null;
			}

			URL resource = null;

			// try direct file access first (development mode)
			if (null != fileLocation) {
				File file = new File(fileLocation, localResourcePath);
				log.debug("trying to find {} ({})", file.getAbsolutePath(), file.exists());
				if (file.exists()) {
					log.debug("found {} ({})", file.getAbsolutePath(), file.exists());
					resource = file.toURI().toURL();
				}
			}

			if (resource == null) {
				resource = getServletContext().getResource(localResourcePath);
			}
			if (resource == null) {
				if (!isAllowed(localResourcePath)) {
					if (log.isWarnEnabled()) {
						log.warn("An attempt to access a protected resource at " + localResourcePath
								+ " was disallowed.");
					}
					return null;
				}
				log.debug("Searching classpath for resource: {}", localResourcePath);
				resource = ClassUtils.getDefaultClassLoader().getResource(localResourcePath);
				if (resource == null) {
					log.debug("Searching classpath for resource: {}", localResourcePath.substring(1));
					resource = ClassUtils.getDefaultClassLoader().getResource(localResourcePath.substring(1));
				}
			}
			if (resource == null) {
				if (resources.length > 1) {
					log.debug("Combined resource not found: {}", localResourcePath);
				}
				return null;
			} else {
				log.debug(resource.toExternalForm());
				resources[i] = resource;
			}
		}
		return resources;
	}

	private boolean isAllowed(String resourcePath) {
		if (resourcePath.matches(PROTECTED_PATH)) {
			return false;
		}
		PathMatcher pathMatcher = new AntPathMatcher();
		for (String pattern : allowedResourcePaths) {
			if (pathMatcher.match(pattern, resourcePath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set HTTP headers to allow caching for the given number of seconds.
	 *
	 * @param response where to set the caching settings
	 * @param seconds number of seconds into the future that the response should be cacheable for
	 */
	private void configureCaching(HttpServletResponse response, int seconds) {
		// HTTP 1.0 header
		response.setDateHeader(HTTP_EXPIRES_HEADER, System.currentTimeMillis() + seconds * 1000L);
		if (seconds > 0) {
			// HTTP 1.1 header
			response.setHeader(HTTP_CACHE_CONTROL_HEADER, "max-age=" + seconds);
		} else {
			// HTTP 1.1 header
			response.setHeader(HTTP_CACHE_CONTROL_HEADER, "no-cache");

		}
	}
}
