/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */
package org.geomajas.gwt.server.mvc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geomajas.command.CommandDispatcher;
import org.geomajas.command.CommandResponse;
import org.geomajas.gwt.client.GeomajasService;
import org.geomajas.gwt.client.command.GwtCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.servlet.ModelAndView;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Geomajas GWT controller, implements communication between GWT face and back-end.
 * 
 * @author Jan De Moerloose
 * @since 1.0.0
 */
@Controller("/geomajasService")
public class GeomajasController extends RemoteServiceServlet implements GeomajasService, ServletConfigAware {

	private static final long serialVersionUID = 100L;

	@Autowired
	private CommandDispatcher commandDispatcher;

	@Autowired(required = false)
	private ServletContext servletContext;

	/**
	 * Implements Spring Controller interface method.
	 * 
	 * Call GWT's RemoteService doPost() method and return null.
	 * 
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return a ModelAndView to render, or null if handled directly
	 * @throws Exception in case of errors
	 */
	@RequestMapping("/geomajasService")
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doPost(request, response);
		return null;
	}

	/**
	 * Execute a GWT RPC command request, and return the response. These request come from the client, and the response
	 * is sent back to the client. We use a {@link CommandDispatcher} to actually execute the command.
	 */
	public CommandResponse execute(GwtCommand request) {
		if (request != null) {
			return commandDispatcher.execute(request.getCommandName(), request.getCommandRequest(),
					request.getUserToken(), request.getLocale());
		}
		return null;
	}

	public ServletContext getServletContext() {
		if (servletContext == null) {
			throw new RuntimeException("getServletContext() cannot be used outside web context");
		}
		return servletContext;
	}

	public void setServletConfig(ServletConfig servletConfig) {
		try {
			super.init(servletConfig);
		} catch (ServletException e) {
			throw new RuntimeException("init(servletConfig) failed", e);
		}

	}
}