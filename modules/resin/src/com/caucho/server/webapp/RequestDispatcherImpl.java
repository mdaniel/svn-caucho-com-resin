/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.AbstractResponseStream;
import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.connection.HttpBufferStore;
import com.caucho.server.dispatch.Invocation;
import com.caucho.util.L10N;
import com.caucho.jsf.context.JspResponseWrapper;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class RequestDispatcherImpl implements RequestDispatcher {
  private static final L10N L = new L10N(RequestDispatcherImpl.class);

  private static final String REQUEST_URI
    = "javax.servlet.include.request_uri";
  private static final String CONTEXT_PATH
    = "javax.servlet.include.context_path";
  private static final String SERVLET_PATH
    = "javax.servlet.include.servlet_path";
  private static final String PATH_INFO
    = "javax.servlet.include.path_info";
  private static final String QUERY_STRING
    = "javax.servlet.include.query_string";

  private static final String FWD_REQUEST_URI =
    "javax.servlet.forward.request_uri";
  private static final String FWD_CONTEXT_PATH =
    "javax.servlet.forward.context_path";
  private static final String FWD_SERVLET_PATH =
    "javax.servlet.forward.servlet_path";
  private static final String FWD_PATH_INFO =
    "javax.servlet.forward.path_info";
  private static final String FWD_QUERY_STRING =
    "javax.servlet.forward.query_string";

  // WebApp the request dispatcher was called from
  private WebApp _webApp;
  private Invocation _includeInvocation;
  private Invocation _forwardInvocation;
  private Invocation _errorInvocation;
  private Invocation _dispatchInvocation;
  private boolean _isLogin;

  RequestDispatcherImpl(Invocation includeInvocation,
                        Invocation forwardInvocation,
                        Invocation errorInvocation,
                        Invocation dispatchInvocation,
                        WebApp webApp)
  {
    _includeInvocation = includeInvocation;
    _forwardInvocation = forwardInvocation;
    _errorInvocation = errorInvocation;
    _dispatchInvocation = dispatchInvocation;

    _webApp = webApp;
  }

  public void setLogin(boolean isLogin)
  {
    _isLogin = isLogin;
  }

  public boolean isModified()
  {
    return _includeInvocation.isModified();
  }

  public void forward(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forward((HttpServletRequest) request, (HttpServletResponse) response,
	    null, _forwardInvocation);
  }

  public void forwardResume(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forwardResume((HttpServletRequest) request, (HttpServletResponse) response,
		  _forwardInvocation);
  }

  public void error(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forward((HttpServletRequest) request, (HttpServletResponse) response,
	    "error", _errorInvocation);
  }

  public void dispatch(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    forward((HttpServletRequest) request, (HttpServletResponse) response,
	    "error", _dispatchInvocation);
  }

  /**
   * Forwards the request to the servlet named by the request dispatcher.
   *
   * @param req the servlet request.
   * @param res the servlet response.
   * @param method special to tell if from error.
   */
  public void forward(HttpServletRequest req, HttpServletResponse res,
		      String method, Invocation invocation)
    throws ServletException, IOException
  {
    AbstractHttpResponse response = null;
    DispatchRequest subRequest;
    HttpSession session = null;

    CauchoResponse cauchoRes = null;

    if (res instanceof CauchoResponse)
      cauchoRes = (CauchoResponse) res;

    // jsp/15m8
    if (res.isCommitted() && method == null) {
      IllegalStateException exn;
      exn = new IllegalStateException("forward() not allowed after buffer has committed.");

      if (cauchoRes == null || ! cauchoRes.hasError()) {
	if (cauchoRes != null)
	  cauchoRes.setHasError(true);
        throw exn;
      }

      _webApp.log(exn.getMessage(), exn);

      return;
    }

    if (res instanceof CauchoResponse)
      response = ((CauchoResponse) res).getAbstractHttpResponse();

    ServletResponse resPtr = res;
    boolean isError = "error".equals(method);

    if (method == null || isError)
      method = req.getMethod();

    subRequest = DispatchRequest.createDispatch();

    HttpServletRequest parentRequest = req;
    HttpServletRequestWrapper reqWrapper = null;
    HttpServletRequest topRequest = subRequest;

    if (! (req instanceof CauchoRequest))
      topRequest = req;

    while (parentRequest instanceof HttpServletRequestWrapper
	   && ! (parentRequest instanceof CauchoRequest)) {
      reqWrapper = (HttpServletRequestWrapper) parentRequest;
      parentRequest = (HttpServletRequest) reqWrapper.getRequest();
    }

    String newQueryString = invocation.getQueryString();
    String reqQueryString = req.getQueryString();

    String queryString;

    /* Changed to match tomcat */
    // server/10y3
    if (_isLogin)
      queryString = newQueryString;
    else if (reqQueryString == null)
      queryString = newQueryString;
    else if (newQueryString == null)
      queryString = reqQueryString;
    else if (reqQueryString.equals(newQueryString)) {
      // server/1kn2
      queryString = newQueryString;
      newQueryString = null;
    }
    /*
    else
      queryString = newQueryString + '&' + reqQueryString;
    */
    else
      queryString = newQueryString;

    WebApp oldWebApp;

    if (req instanceof CauchoRequest)
      oldWebApp = ((CauchoRequest) req).getWebApp();
    else
      oldWebApp = (WebApp) _webApp.getContext(req.getContextPath());

    subRequest.init(invocation,
		    invocation.getWebApp(), oldWebApp,
                    parentRequest, res, method,
		    invocation.getURI(),
		    invocation.getServletPath(),
		    invocation.getPathInfo(),
		    queryString, newQueryString);

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);

      if (topRequest == parentRequest) // server/172o
	topRequest = reqWrapper;
    }

    Object oldUri = null;
    Object oldContextPath = null;
    Object oldServletPath = null;
    Object oldPathInfo = null;
    Object oldQueryString = null;
    Object oldJSPFile = null;
    Object oldForward = null;

    oldUri = req.getAttribute(REQUEST_URI);

    if (oldUri != null) {
      oldContextPath = req.getAttribute(CONTEXT_PATH);
      oldServletPath = req.getAttribute(SERVLET_PATH);
      oldPathInfo = req.getAttribute(PATH_INFO);
      oldQueryString = req.getAttribute(QUERY_STRING);

      req.removeAttribute(REQUEST_URI);
      req.removeAttribute(CONTEXT_PATH);
      req.removeAttribute(SERVLET_PATH);
      req.removeAttribute(PATH_INFO);
      req.removeAttribute(QUERY_STRING);
      req.removeAttribute("caucho.jsp.jsp-file");
    }

    // server/1ksc
    if (! isError && req.getAttribute(FWD_REQUEST_URI) == null) {
      subRequest.setAttribute(FWD_REQUEST_URI, req.getRequestURI());
      subRequest.setAttribute(FWD_CONTEXT_PATH, req.getContextPath());
      subRequest.setAttribute(FWD_SERVLET_PATH, req.getServletPath());
      subRequest.setAttribute(FWD_PATH_INFO, req.getPathInfo());
      subRequest.setAttribute(FWD_QUERY_STRING, req.getQueryString());
    }

    oldForward = req.getAttribute("caucho.forward");
    req.setAttribute("caucho.forward", "true");

    subRequest.setPageURI(subRequest.getRequestURI());
    subRequest.setPageContextPath(subRequest.getContextPath());
    subRequest.setPageServletPath(subRequest.getServletPath());
    subRequest.setPagePathInfo(subRequest.getPathInfo());
    subRequest.setPageQueryString(subRequest.getQueryString());

    CauchoRequest oldRequest = null;
    AbstractResponseStream oldStream = null;
    if (response != null) {
      oldRequest = response.getRequest();
      oldStream = response.getResponseStream();
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      if (response != null) {
	response.setRequest(subRequest);
	response.setResponseStream(response.getOriginalStream());
      }

      // server/1732 wants this commented out
      // jsp/15m9 (tck)
      ServletResponse ptr = res;
      while (ptr instanceof AbstractHttpResponse) {
	ptr = ((AbstractHttpResponse) ptr).getResponse();

	if (ptr != null)
	  ptr.resetBuffer();
      }

      res.resetBuffer();
      res.setContentLength(-1);

      invocation.service(topRequest, res);

      if (cauchoRes != null) {
	// server/1732 wants this commented out
	// jsp/15m9 (tck)
        ServletResponse closePtr = cauchoRes;
        while (closePtr instanceof CauchoResponse
	       && ! (closePtr instanceof JspResponseWrapper)) {
          ((CauchoResponse) closePtr).close();

          closePtr = ((CauchoResponse) closePtr).getResponse();
        }
      }
      else {
	// server/10ab
	try {
	  PrintWriter out = res.getWriter();
	  if (out != null)
	    out.close();
	} catch (IllegalStateException e1) {
	}

        try {
          OutputStream os = res.getOutputStream();
	  if (os != null)
	    os.close();
        } catch (IllegalStateException e) {
        }
      }
    } finally {
      subRequest.finishRequest();

      thread.setContextClassLoader(oldLoader);

      if (response != null) {
        response.setRequest(oldRequest);
        response.setResponseStream(oldStream);
        //response.setWriter(oldWriter);
      }

      DispatchRequest.free(subRequest);

      if (reqWrapper != null)
	reqWrapper.setRequest(parentRequest);

      // XXX: are these necessary?
      if (oldUri != null)
	req.setAttribute(REQUEST_URI, oldUri);

      if (oldContextPath != null)
	req.setAttribute(CONTEXT_PATH, oldContextPath);

      if (oldServletPath != null)
	req.setAttribute(SERVLET_PATH, oldServletPath);

      if (oldPathInfo != null)
	req.setAttribute(PATH_INFO, oldPathInfo);

      if (oldQueryString != null)
	req.setAttribute(QUERY_STRING, oldQueryString);

      if (oldForward == null)
	req.removeAttribute("caucho.forward");

      // server/1732 wants this commented out
      // jsp/15m9 (tck)
      /*
      ServletResponse ptr = res;
      while (ptr instanceof CauchoResponse) {
	ptr.close();
	
	ptr = ((CauchoResponse) ptr).getResponse();
      }
      */
    }
  }


  /**
   * Forwards the request to the servlet named by the request dispatcher.
   *
   * @param req the servlet request.
   * @param res the servlet response.
   * @param method special to tell if from error.
   */
  public void forwardResume(HttpServletRequest req, HttpServletResponse res,
			    Invocation invocation)
    throws ServletException, IOException
  {
    AbstractHttpResponse response = null;
    DispatchRequest subRequest;
    HttpSession session = null;

    CauchoResponse cauchoRes = null;

    if (res instanceof CauchoResponse)
      cauchoRes = (CauchoResponse) res;

    if (res instanceof AbstractHttpResponse)
      response = (AbstractHttpResponse) res;

    ServletResponse resPtr = res;

    String method = req.getMethod();

    subRequest = DispatchRequest.createDispatch();

    HttpServletRequest parentRequest = req;
    HttpServletRequest topRequest = subRequest;

    if (! (req instanceof CauchoRequest))
      topRequest = req;

    String newQueryString = invocation.getQueryString();
    String reqQueryString = req.getQueryString();

    String queryString;

    /* Changed to match tomcat */
    // server/10y3
    if (_isLogin)
      queryString = newQueryString;
    else if (reqQueryString == null)
      queryString = newQueryString;
    else if (newQueryString == null)
      queryString = reqQueryString;
    else if (reqQueryString.equals(newQueryString)) {
      // server/1kn2
      queryString = newQueryString;
      newQueryString = null;
    }
    /*
    else
      queryString = newQueryString + '&' + reqQueryString;
    */
    else
      queryString = newQueryString;

    WebApp oldWebApp;

    if (req instanceof CauchoRequest)
      oldWebApp = ((CauchoRequest) req).getWebApp();
    else
      oldWebApp = (WebApp) _webApp.getContext(req.getContextPath());

    subRequest.init(invocation,
		    invocation.getWebApp(), oldWebApp,
                    parentRequest, res, method,
		    invocation.getURI(),
		    invocation.getServletPath(),
		    invocation.getPathInfo(),
		    queryString, newQueryString);

    Object oldUri = null;
    Object oldContextPath = null;
    Object oldServletPath = null;
    Object oldPathInfo = null;
    Object oldQueryString = null;
    Object oldJSPFile = null;
    Object oldForward = null;

    oldUri = req.getAttribute(REQUEST_URI);

    if (oldUri != null) {
      oldContextPath = req.getAttribute(CONTEXT_PATH);
      oldServletPath = req.getAttribute(SERVLET_PATH);
      oldPathInfo = req.getAttribute(PATH_INFO);
      oldQueryString = req.getAttribute(QUERY_STRING);

      req.removeAttribute(REQUEST_URI);
      req.removeAttribute(CONTEXT_PATH);
      req.removeAttribute(SERVLET_PATH);
      req.removeAttribute(PATH_INFO);
      req.removeAttribute(QUERY_STRING);
      req.removeAttribute("caucho.jsp.jsp-file");
    }

    if (req.getAttribute(FWD_REQUEST_URI) == null) {
      subRequest.setAttribute(FWD_REQUEST_URI, req.getRequestURI());
      subRequest.setAttribute(FWD_CONTEXT_PATH, req.getContextPath());
      subRequest.setAttribute(FWD_SERVLET_PATH, req.getServletPath());
      subRequest.setAttribute(FWD_PATH_INFO, req.getPathInfo());
      subRequest.setAttribute(FWD_QUERY_STRING, req.getQueryString());
    }

    oldForward = req.getAttribute("caucho.forward");
    req.setAttribute("caucho.forward", "true");

    subRequest.setPageURI(subRequest.getRequestURI());
    subRequest.setPageContextPath(subRequest.getContextPath());
    subRequest.setPageServletPath(subRequest.getServletPath());
    subRequest.setPagePathInfo(subRequest.getPathInfo());
    subRequest.setPageQueryString(subRequest.getQueryString());

    CauchoRequest oldRequest = null;
    AbstractResponseStream oldStream = null;
    if (response != null) {
      oldRequest = response.getRequest();
      oldStream = response.getResponseStream();
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      if (response != null) {
	response.setRequest(subRequest);
	response.setResponseStream(response.getOriginalStream());
      }

      invocation.service(topRequest, res);
    } finally {
      subRequest.finishRequest();

      thread.setContextClassLoader(oldLoader);

      if (response != null) {
        response.setRequest(oldRequest);
        response.setResponseStream(oldStream);
        //response.setWriter(oldWriter);
      }

      // XXX: are these necessary?
      if (oldUri != null)
	req.setAttribute(REQUEST_URI, oldUri);

      if (oldContextPath != null)
	req.setAttribute(CONTEXT_PATH, oldContextPath);

      if (oldServletPath != null)
	req.setAttribute(SERVLET_PATH, oldServletPath);

      if (oldPathInfo != null)
	req.setAttribute(PATH_INFO, oldPathInfo);

      if (oldQueryString != null)
	req.setAttribute(QUERY_STRING, oldQueryString);

      if (oldForward == null)
	req.removeAttribute("caucho.forward");
    }
  }


  public void include(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    include(request, response, null);
  }

  /**
   * Include a request into the current page.
   */
  public void include(ServletRequest request, ServletResponse response,
		      String method)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    IncludeDispatchRequest subRequest;
    DispatchResponse subResponse;

    Invocation invocation = _includeInvocation;
    WebApp webApp = invocation.getWebApp();
    String queryString = invocation.getQueryString();

    if (method == null)
      method = req.getMethod();

    if (! "POST".equals(method))
      method = "GET";

    HttpServletRequest parentRequest = req;
    HttpServletRequestWrapper reqWrapper = null;

    HttpServletResponse parentResponse = res;
    HttpServletResponseWrapper resWrapper = null;

    if (! _webApp.getDispatchWrapsFilters()) {
      if (req instanceof HttpServletRequestWrapper
	  && ! (req instanceof CauchoRequest)) {
	reqWrapper = (HttpServletRequestWrapper) req;
	parentRequest = (HttpServletRequest) reqWrapper.getRequest();
      }

      if (res instanceof HttpServletResponseWrapper &&
	  ! (res instanceof CauchoResponse)) {
	resWrapper = (HttpServletResponseWrapper) res;
	parentResponse = (HttpServletResponse) resWrapper.getResponse();
      }
    }

    subRequest = IncludeDispatchRequest.createDispatch();

    WebApp oldWebApp;

    if (req instanceof CauchoRequest)
      oldWebApp = ((CauchoRequest) req).getWebApp();
    else
      oldWebApp = (WebApp) webApp.getContext(req.getContextPath());

    subRequest.init(invocation,
		    webApp, oldWebApp,
		    parentRequest, parentResponse,
		    method,
                    req.getRequestURI(), req.getServletPath(),
                    req.getPathInfo(), req.getQueryString(),
                    queryString);

    HttpServletRequest topRequest = subRequest;

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);
      topRequest = reqWrapper;
    }

    subResponse = DispatchResponse.createDispatch();
    subResponse.setNextResponse(res);

    AbstractResponseStream s = null;
    boolean oldDisableClose = false;
    HttpBufferStore httpBuffer = HttpBufferStore.allocate(webApp.getServer());

    subResponse.init(subRequest);
    subResponse.setNextResponse(parentResponse);
    subResponse.startRequest(httpBuffer);
    subResponse.setCharacterEncoding(res.getCharacterEncoding());

    if (_webApp.getDispatchWrapsFilters())
      subResponse.setCauchoResponseStream(true);

    CauchoResponse cauchoRes = null;
    if (res instanceof CauchoResponse) {
      cauchoRes = (CauchoResponse) res;
    }
    else if (! _webApp.getDispatchWrapsFilters()) {
      subResponse.killCache();
    }

    HttpServletResponse topResponse = subResponse;

    if (resWrapper != null) {
      resWrapper.setResponse(subResponse);
      topResponse = res;
    }

    Object oldUri = null;
    Object oldContextPath = null;
    Object oldServletPath = null;
    Object oldPathInfo = null;
    Object oldQueryString = null;

    oldUri = req.getAttribute(REQUEST_URI);
    if (oldUri != null) {
      oldContextPath = request.getAttribute(CONTEXT_PATH);
      oldServletPath = req.getAttribute(SERVLET_PATH);
      oldPathInfo = req.getAttribute(PATH_INFO);
      oldQueryString = req.getAttribute(QUERY_STRING);
    }

    subRequest.setPageURI(invocation.getURI());
    subRequest.setAttribute(REQUEST_URI, invocation.getURI());
    String contextPath;
    if (webApp != null)
      contextPath = webApp.getContextPath();
    else
      contextPath = null;

    subRequest.setPageContextPath(contextPath);
    subRequest.setAttribute(CONTEXT_PATH, contextPath);
    subRequest.setPageServletPath(invocation.getServletPath());
    subRequest.setAttribute(SERVLET_PATH, invocation.getServletPath());
    subRequest.setPagePathInfo(invocation.getPathInfo());
    subRequest.setAttribute(PATH_INFO, invocation.getPathInfo());
    subRequest.setPageQueryString(queryString);
    subRequest.setAttribute(QUERY_STRING, queryString);

    subRequest.removeAttribute("caucho.jsp.jsp-file");

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    boolean isOkay = false;
    try {
      invocation.service(topRequest, topResponse);

      int status = subResponse.getStatusCode();

      request.setAttribute("com.caucho.dispatch.response.statusCode",
                           new Integer(status));

      isOkay = true;
    } finally {
      thread.setContextClassLoader(oldLoader);
      // XXX: In many cases, able to use clear()?

      subRequest.finishRequest();

      /* XXX:
      if (s != null)
	s.setDisableClose(oldDisableClose);
      */
      if (oldUri != null)
        req.setAttribute(REQUEST_URI, oldUri);
      else
        req.removeAttribute(REQUEST_URI);

      if (oldContextPath != null)
        req.setAttribute(CONTEXT_PATH, oldContextPath);
      else
        req.removeAttribute(CONTEXT_PATH);

      if (oldServletPath != null)
        req.setAttribute(SERVLET_PATH, oldServletPath);
      else
        req.removeAttribute(SERVLET_PATH);

      if (oldPathInfo != null)
        req.setAttribute(PATH_INFO, oldPathInfo);
      else
        req.removeAttribute(PATH_INFO);

      if (oldQueryString != null)
        req.setAttribute(QUERY_STRING, oldQueryString);
      else
        req.removeAttribute(QUERY_STRING);

      if (! isOkay)
	subResponse.killCache();

      subResponse.close();

      /*
      if (! (res instanceof CauchoResponse)) {
	if (s != null)
	  s.close();
      }
      */

      IncludeDispatchRequest.free(subRequest);
      DispatchResponse.free(subResponse);

      if (reqWrapper != null)
	reqWrapper.setRequest(parentRequest);

      if (resWrapper != null)
	resWrapper.setResponse(parentResponse);

      HttpBufferStore.free(httpBuffer);
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _dispatchInvocation.getRawURI() + "]");
  }
}
