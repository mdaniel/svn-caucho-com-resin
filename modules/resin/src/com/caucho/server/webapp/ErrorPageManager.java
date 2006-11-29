/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.Version;
import com.caucho.config.LineConfigRuntimeException;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.java.LineMap;
import com.caucho.java.LineMapException;
import com.caucho.java.ScriptStackTrace;
import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.dispatch.BadRequestException;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.CompileException;
import com.caucho.util.HTTPUtil;
import com.caucho.util.L10N;
import com.caucho.util.LineCompileException;
import com.caucho.util.Log;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.Encoding;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the final servlet in a filter chain.
 */
public class ErrorPageManager {
  private final static L10N L = new L10N(ErrorPageManager.class);
  private final static Logger log = Log.open(ErrorPageManager.class);
  
  public static final char []MSIE_PADDING;
  
  public static String REQUEST_URI = "javax.servlet.include.request_uri";
  public static String CONTEXT_PATH = "javax.servlet.include.context_path";
  public static String SERVLET_PATH = "javax.servlet.include.servlet_path";
  public static String PATH_INFO = "javax.servlet.include.path_info";
  public static String QUERY_STRING = "javax.servlet.include.query_string";
  
  public static String STATUS_CODE = "javax.servlet.error.status_code";
  public static String EXCEPTION_TYPE = "javax.servlet.error.exception_type";
  public static String MESSAGE = "javax.servlet.error.message";
  public static String EXCEPTION = "javax.servlet.error.exception";
  public static String ERROR_URI = "javax.servlet.error.request_uri";
  public static String SERVLET_NAME = "javax.servlet.error.servlet_name";
  
  public static String JSP_EXCEPTION = "javax.servlet.jsp.jspException";
  
  public static String SHUTDOWN = "com.caucho.shutdown";
  
  private WebApp _app;
  private WebAppContainer _appContainer;
  private HashMap<Object,String> _errorPageMap = new HashMap<Object,String>();
  private String _defaultLocation;

  private ErrorPageManager _parent;

  /**
   * Create error page manager.
   */
  public ErrorPageManager()
  {
  }

  /**
   * Sets the manager parent.
   */
  public void setParent(ErrorPageManager parent)
  {
    _parent = parent;
  }

  /**
   * Gets the manager parent.
   */
  public ErrorPageManager getParent()
  {
    return _parent;
  }

  /**
   * Adds an error page.
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    if (errorPage.getExceptionType() != null) {
      _errorPageMap.put(errorPage.getExceptionType(),
                        errorPage.getLocation());
    }
    else if (errorPage.getErrorCode() < 0) {
      _defaultLocation = errorPage.getLocation();
    }
    else
      _errorPageMap.put(new Integer(errorPage.getErrorCode()),
                        errorPage.getLocation());
  }

  /**
   * Sets the webApp.
   */
  public void setWebApp(WebApp app)
  {
    _app = app;
  }

  /**
   * Sets the webApp container.
   */
  public void setWebAppContainer(WebAppContainer appContainer)
  {
    _appContainer = appContainer;
  }
  
  /**
   * Displays a parse error.
   */
  public void sendServletError(Throwable e,
                               ServletRequest req,
                               ServletResponse res)
    throws IOException
  {
    HttpServletResponse response = (HttpServletResponse) res;
    HttpServletRequest request = (HttpServletRequest) req;
    Throwable rootExn = e;
    Throwable errorPageExn = null;
    LineMap lineMap = null;

    if (response instanceof AbstractHttpResponse) {
      ((AbstractHttpResponse) response).killCache();
      ((AbstractHttpResponse) response).setNoCache(true);
    }

    try {
      response.reset();
    } catch (IllegalStateException e1) {
    }

    if (rootExn instanceof ClientDisconnectException)
      throw (ClientDisconnectException) rootExn;

    String location = null;

    String title = "500 Servlet Exception";
    boolean badRequest = false;
    boolean doStackTrace = true;
    boolean isCompileException = false;
    boolean isLineCompileException = false;
    boolean isServletException = false;
    Throwable compileException = null;
    String lineMessage = null;

    boolean lookupErrorPage = true;
    while (true) {
      if (rootExn instanceof LineMapException)
        lineMap = ((LineMapException) rootExn).getLineMap();

      if (lookupErrorPage) {
	errorPageExn = rootExn;
      }

      if (rootExn instanceof CompileException) {
        doStackTrace = false;
        isCompileException = true;

        // use outer exception because it might have added more location info
        if (rootExn instanceof LineCompileException) {
	  compileException = rootExn;
	  
          isLineCompileException = true;
        }
	else if (compileException == null) // ! isLineCompileException)
          compileException = rootExn;
      }
      else if (rootExn instanceof LineConfigRuntimeException) {
	if (lineMessage == null)
	  lineMessage = rootExn.getMessage();
      }

      if (rootExn instanceof BadRequestException)
        badRequest = true;

      if (location != null || ! lookupErrorPage) {
      }
      else if (rootExn instanceof LineMapException &&
	       rootExn instanceof ServletException &&
	       ! (rootExn instanceof LineCompileException) &&
	       rootExn.getCause() != null) {
	// hack to deal with JSP wrapping
      }
      else if (! isServletException) {
	// SRV.9.9.2 Servlet 2.4
        //location = getErrorPage(rootExn, ServletException.class);
        location = getErrorPage(rootExn);
	isServletException = true;
      }
      else {
        location = getErrorPage(rootExn);
	lookupErrorPage = false;
      }

      if (location != null)
	lookupErrorPage = false;

      Throwable cause = null;
      if (rootExn instanceof ServletException &&
	  ! (rootExn instanceof LineCompileException))
        cause = ((ServletException) rootExn).getRootCause();
      else {
	lookupErrorPage = false;
        cause = rootExn.getCause();
      }

      if (cause != null)
        rootExn = cause;
      else {
        break;
      }
    }

    if (location == null && lookupErrorPage) {
      location = getErrorPage(rootExn);
    }

    if (location == null)
      location = getErrorPage(500);

    if (location == null && _defaultLocation == null && _parent != null) {
      _parent.sendServletError(e, req, res);
      return;
    }

    if (isCompileException)
      log.warning(compileException.getMessage());
    else if (! doStackTrace)
      log.warning(rootExn.toString());
    else
      log.log(Level.WARNING, e.toString(), e);

    if (badRequest) {
      title = rootExn.getMessage();
      doStackTrace = false;
      badRequest = true;

      if (request instanceof CauchoRequest)
	((CauchoRequest) request).killKeepalive();
      
      response.resetBuffer();
      
      response.setStatus(response.SC_BAD_REQUEST, rootExn.getMessage());

    }
    else if (rootExn instanceof UnavailableException) {
      UnavailableException unAvail = (UnavailableException) rootExn;

      if (unAvail.isPermanent()) {
	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	title = "404 Not Found";

	if (location == null)
	  location = getErrorPage(response.SC_NOT_FOUND);
      }
      else {
	response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
	title = "503 Unavailable";

	if (unAvail.getUnavailableSeconds() > 0)
	  response.setIntHeader("Retry-After",
				unAvail.getUnavailableSeconds());
	
	if (location == null)
	  location = getErrorPage(response.SC_SERVICE_UNAVAILABLE);
      }
    }
    /*
    else if (_app != null && app.getServer().isClosed()) {
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      title = "503 Unavailable";
    }
    */
    else
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    
    if (location == null)
      location = _defaultLocation;
    
    if (location != null) {
      if (errorPageExn == null)
	errorPageExn = rootExn;
      
      request.setAttribute(JSP_EXCEPTION, errorPageExn);
      request.setAttribute(EXCEPTION, errorPageExn);
      request.setAttribute(EXCEPTION_TYPE, errorPageExn.getClass());
      if (request instanceof HttpServletRequest)
        request.setAttribute(ERROR_URI,
                             ((HttpServletRequest) request).getRequestURI());
      if (request instanceof AbstractHttpRequest)
        request.setAttribute(AbstractHttpRequest.SERVLET_NAME,
                             ((AbstractHttpRequest) request).getServletName());

      request.setAttribute(STATUS_CODE, new Integer(500));
      request.setAttribute(MESSAGE, errorPageExn.getMessage());
      /*
        if (_app != null && _app.getServer().isClosed())
        setAttribute(SHUTDOWN, "shutdown");
      */

      try {
        RequestDispatcher disp = null;
        // can't use filters because of error pages due to filters
        // or security.

	if (_app != null)
	  disp = _app.getRequestDispatcher(location);
	else if (_appContainer != null)
	  disp = _appContainer.getRequestDispatcher(location);
	  
        if (disp != null) {
          ((RequestDispatcherImpl) disp).error(request, response);
          return;
        }
      } catch (Throwable e1) {
        log.log(Level.INFO, e1.toString(), e1);
        rootExn = e1;
      }
    }

    response.setContentType("text/html");

    String encoding = CharacterEncoding.getLocalEncoding();

    if (encoding != null)
      response.setCharacterEncoding(encoding);
    else {
      Locale locale = Locale.getDefault();
      if (! "ISO-8859-1".equals(Encoding.getMimeName(locale)))
	response.setLocale(Locale.getDefault());
    }

    PrintWriter out = response.getWriter();

    out.println("<html>");
    if (! response.isCommitted())
      out.println("<head><title>" + title + "</title></head>");
    out.println("<body>");
    out.println("<h1>" + title + "</h1>");

    out.println("<code><pre>");
    
    /*
    if (app != null && app.getServer().isClosed()) {
      pw.println("Server is temporarily unavailable");
      doStackTrace = false;
    }
    else
    */

    if (compileException != null)
      out.println(escapeHtml(compileException.getMessage()));
    else if (! doStackTrace)
      out.println(escapeHtml(rootExn.toString()));

    if (doStackTrace || log.isLoggable(Level.FINE)) {
      printStackTrace(out, lineMessage, rootExn, lineMap);
    }

    out.println("</pre></code>");

    if (! CauchoSystem.isTesting()) {
      out.println("<hr /><small>");
      out.println(Version.FULL_VERSION);
      out.println("</small>");
    }
    out.println("</body></html>");

    String userAgent = request.getHeader("User-Agent");
      
    if (userAgent != null && userAgent.indexOf("MSIE") >= 0) {
      out.print(MSIE_PADDING);
    }

    out.close();
  }

  /**
   * Sends an HTTP error to the browser.
   *
   * @param code the HTTP error code
   * @param value a string message
   */
  public void sendError(CauchoRequest request,
                        CauchoResponse response,
                        int code, String message)
    throws IOException
  {
    response.resetBuffer();
    
    /* XXX: if we've already got an error, won't this just mask it?
    if (responseStream.isCommitted())
      throw new IllegalStateException("response can't sendError() after commit");
    */

    response.setStatus(code, message);
    
    try {
      if (handleErrorStatus(request, response, code, message) ||
          code == HttpServletResponse.SC_NOT_MODIFIED)
        return;

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      out.println("<html>");
      if (! response.isCommitted()) {
        out.print("<head><title>");
        out.print(code);
        out.print(" ");
        out.print(message);
        out.println("</title></head>");
      }

      out.println("<body>");
      out.print("<h1>");
      out.print(code);
      out.print(" ");
      out.print(message);
      out.println("</h1>");

      if (code == HttpServletResponse.SC_NOT_FOUND) {
        out.println(L.l("{0} was not found on this server.",
                        HTTPUtil.encodeString(request.getPageURI())));
      }

      if (! CauchoSystem.isTesting()) {
	out.println("<p /><hr />");
	out.println("<small>");
	out.println(com.caucho.Version.FULL_VERSION);
	out.println("</small>");
      }
      out.println("</body></html>");

      String userAgent = request.getHeader("User-Agent");
      
      if (userAgent != null && userAgent.indexOf("MSIE") >= 0) {
	out.write(MSIE_PADDING, 0, MSIE_PADDING.length);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles an error status code.
   *
   * @return true if we've forwarded to an error page.
   */
  private boolean handleErrorStatus(CauchoRequest request,
                                    CauchoResponse response,
                                    int code, String message)
    throws ServletException, IOException
  {
    if (code == HttpServletResponse.SC_OK ||
        code == HttpServletResponse.SC_MOVED_TEMPORARILY ||
        code == HttpServletResponse.SC_NOT_MODIFIED)
      return false;

    if (request.getRequestDepth(0) > 16)
      return false;

    else if (request.getAttribute(AbstractHttpRequest.ERROR_URI) != null)
      return false;

    response.killCache();
    
    String location = getErrorPage(code);
      
    if (location == null)
      location = _defaultLocation;

    if (location == null && _parent != null)
      return _parent.handleErrorStatus(request, response, code, message);

    if (_app == null && _appContainer == null)
      return false;
    
    if (location != null && ! location.equals(request.getRequestURI())) {
      // XXX: _responseStream.clearHeaders();

      request.setAttribute(AbstractHttpRequest.STATUS_CODE,
                            new Integer(code));
      request.setAttribute(AbstractHttpRequest.MESSAGE,
                           message);
      request.setAttribute(AbstractHttpRequest.ERROR_URI,
                           request.getRequestURI());
      if (request instanceof AbstractHttpRequest)
        request.setAttribute(AbstractHttpRequest.SERVLET_NAME,
                             ((AbstractHttpRequest) request).getServletName());

      try {
        RequestDispatcher disp = null;
        // can't use filters because of error pages due to filters
        // or security.
	if (_app != null)
	  disp = _app.getRequestDispatcher(location);
	else if (_appContainer != null)
	  disp = _appContainer.getRequestDispatcher(location);

        //disp.forward(request, this, "GET", false);

	if (disp != null)
	  ((RequestDispatcherImpl) disp).error(request, response);
	else
	  return false;
      } catch (Throwable e) {
        sendServletError(e, request, response);
      }
          
      return true;
    }

    return false;
  }

  /**
   * Returns the URL of an error page for the given exception.
   */
  String getErrorPage(Throwable e)
  {
    return getErrorPage(e, Throwable.class);
  }
  
  /**
   * Returns the URL of an error page for the given exception.
   */
  String getErrorPage(Throwable e, Class limit)
  {
    Class cl = e.getClass();
    for (; cl != null; cl = cl.getSuperclass()) {
      String location = (String) _errorPageMap.get(cl.getName());
      if (location != null)
        return location;

      if (cl == limit)
        break;
    }
    
    for (cl = e.getClass(); cl != null; cl = cl.getSuperclass()) {
      String name = cl.getName();
      int p = name.lastIndexOf('.');

      if (p > 0) {
        name = name.substring(p + 1);

        String location =  (String) _errorPageMap.get(name);
        if (location != null)
          return location;
      }
      
      if (cl == limit)
        break;
    }

    return null;
  }
  
  /**
   * Returns the URL of an error page for the given exception.
   */
  String getErrorPage(int code)
  {
    Integer key = new Integer(code);

    String location = (String) _errorPageMap.get(key);
    if (location != null)
      return location;

    return (String) _errorPageMap.get(new Integer(0));
  }

  /**
   * Escapes HTML symbols in a stack trace.
   */
  private void printStackTrace(PrintWriter out,
			       String lineMessage,
			       Throwable e,
                               LineMap lineMap)
  {
    CharArrayWriter writer = new CharArrayWriter();
    PrintWriter pw = new PrintWriter(writer);

    if (lineMessage != null)
      pw.println(lineMessage);

    if (lineMap != null)
      lineMap.printStackTrace(e, pw);
    else
      ScriptStackTrace.printStackTrace(e, pw);

    pw.close();

    char []array = writer.toCharArray();
    out.print(escapeHtml(new String(array)));
  }

  /**
   * Escapes special symbols in a string.  For example '<' becomes '&lt;'
   */
  private String escapeHtml(String s)
  {
    if (s == null)
      return null;

    CharBuffer cb = new CharBuffer();
    int lineCharacter = 0;
    boolean startsWithSpace = false;
    
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);

      lineCharacter++;
      
      if (ch == '<')
	cb.append("&lt;");
      else if (ch == '&')
	cb.append("&amp;");
      else if (ch == '\n' || ch == '\r') {
        lineCharacter = 0;
	cb.append(ch);
        startsWithSpace = false;
      }
      else if (lineCharacter > 70 && ch == ' ' && ! startsWithSpace) {
        lineCharacter = 0;
        cb.append('\n');
        for (; i + 1 < s.length() && s.charAt(i + 1) == ' '; i++) {
        }
      }
      else if (lineCharacter == 1 && (ch == ' ' || ch == '\t')) {
        cb.append((char) ch);
        startsWithSpace = true;
      }
      else
	cb.append(ch);
    }

    return cb.toString();
  }

  static {
    MSIE_PADDING = ("\n\n\n\n" +
                    "<!--\n" +
                    "   - Unfortunately, Microsoft has added a clever new\n" +
                    "   - \"feature\" to Internet Explorer.  If the text in\n" +
                    "   - an error's message is \"too small\", specifically\n" +
                    "   - less than 512 bytes, Internet Explorer returns\n" +
                    "   - its own error message.  Yes, you can turn that\n" +
                    "   - off, but *surprise* it's pretty tricky to find\n" +
                    "   - buried as a switch called \"smart error\n" +
                    "   - messages\"  That means, of course, that many of\n" +
                    "   - Resin's error messages are censored by default.\n" +
                    "   - And, of course, you'll be shocked to learn that\n" +
                    "   - IIS always returns error messages that are long\n" +
                    "   - enough to make Internet Explorer happy.  The\n" +
                    "   - workaround is pretty simple: pad the error\n" +
                    "   - message with a big comment to push it over the\n" +
                    "   - five hundred and twelve byte minimum.  Of course,\n" +
                    "   - that's exactly what you're reading right now.\n" +
                    "   -->\n").toCharArray();
  }
}
