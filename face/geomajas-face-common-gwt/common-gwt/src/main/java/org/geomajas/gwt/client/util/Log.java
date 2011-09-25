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

package org.geomajas.gwt.client.util;

import org.geomajas.annotation.Api;
import org.geomajas.command.dto.LogRequest;
import org.geomajas.gwt.client.command.GwtCommand;
import org.geomajas.gwt.client.command.GwtCommandDispatcher;

import java.util.logging.Logger;

/**
 * SmartClient-based logger functionality.
 * 
 * @author Jan De Moerloose
 * @author Joachim Van der Auwera
 * @since 1.0.0
 */
@Api(allMethods = true)
public final class Log {

	private static final Logger LOG = Logger.getLogger("Log");

	private static final int STACK_TRACE_LINE_LIMIT = 500; // max # of lines in a stack trace, to prevent OOME
	private static final int STACK_TRACE_CAUSE_LIMIT = 12; // max # of causes in a stack trace, to prevent OOME

	public static final int LEVEL_DEBUG = LogRequest.LEVEL_DEBUG;
	public static final int LEVEL_INFO = LogRequest.LEVEL_INFO;
	public static final int LEVEL_WARN = LogRequest.LEVEL_WARN;
	public static final int LEVEL_ERROR = LogRequest.LEVEL_ERROR;

	private static final String SEP = ", ";

	private Log() {
		// do not allow instantiation.
	}

	public static void logDebug(String message) {
		LOG.fine(message);
	}

	public static void logInfo(String message) {
		LOG.info(message);
	}

	public static void logWarn(String message) {
		LOG.warning(message);
		logServer(LEVEL_WARN, message, null);
	}

	public static void logError(String message) {
		LOG.severe(message);
		logServer(LEVEL_ERROR, message, null);
	}

	public static void logDebug(String message, Throwable t) {
		logDebug(message + SEP + getMessage(t));
	}

	public static void logInfo(String message, Throwable t) {
		logInfo(message + SEP + getMessage(t));
	}

	public static void logWarn(String message, Throwable t) {
		logWarn(message + SEP + getMessage(t));
	}

	public static void logError(String message, Throwable t) {
		logError(message + SEP + getMessage(t));
	}

	private static String getMessage(Throwable throwable) {
		StringBuilder sb = new StringBuilder();
		if (null != throwable) {
			addMessageAndStackTrace(sb, throwable);
			Throwable cause = throwable.getCause();
			int count = 0;
			while (null != cause && ++count <= STACK_TRACE_CAUSE_LIMIT) {
				sb.append("\ncaused by ");
				addMessageAndStackTrace(sb, cause);
			}
		}
		return sb.toString();
	}

	private static void addMessageAndStackTrace(StringBuilder sb, Throwable throwable) {
		sb.append(throwable.getClass().getName());
		sb.append(": ");
		sb.append(throwable.getMessage());
		int line = 0;
		for (StackTraceElement ste : throwable.getStackTrace()) {
			sb.append("\n   ");
			sb.append(ste.toString());
			if (++line > STACK_TRACE_LINE_LIMIT) {
				break;
			}
		}
	}

	/**
	 * Log a message in the server log.
	 *
	 * @param logLevel log level
	 * @param message message to log
	 */
	public static void logServer(int logLevel, String message) {
		logServer(logLevel, message, null);
	}

	/**
	 * Log a message in the server log.
	 *
	 * @param logLevel log level
	 * @param message message to log
	 * @param throwable exception to include in message
	 */
	public static void logServer(int logLevel, String message, Throwable throwable) {
		String logMessage  = message;
		if (null == logMessage) {
			logMessage = "";
		}
		if (null != throwable) {
			logMessage += "\n" + getMessage(throwable);
		}
		LogRequest logRequest = new LogRequest();
		logRequest.setLevel(logLevel);
		logRequest.setStatement(message);
		GwtCommand command = new GwtCommand(LogRequest.COMMAND);
		command.setCommandRequest(logRequest);
		GwtCommandDispatcher.getInstance().execute(command);
	}

}
