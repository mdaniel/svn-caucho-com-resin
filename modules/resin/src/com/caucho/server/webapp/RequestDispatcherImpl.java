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

  public void dispatchResume(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    dispatchResume((HttpServletRequest) request, (HttpServletResponse) response,
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
    CauchoResponse cauchoRes = null;

    if (res instanceof CauchoResponse)
      cauchoRes = (CauchoResponse) res;

    // jsp/15m8
    if (res.isCommitted()
        && method == null
        && ! _webApp.isAllowForwardAfterFlush()) {
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

    if (method == null)
      res.resetBuffer();

    HttpServletRequest parentReq = req;
    HttpServletRequestWrapper reqWrapper = null;

    if (req instanceof HttpServletRequestWrapper) {
      reqWrapper = (HttpServletRequestWrapper) req;
      parentReq = (HttpServletRequest) reqWrapper.getRequest();
    }

    HttpServletResponse parentRes = res;
    HttpServletResponseWrapper resWrapper = null;

    if (res instanceof HttpServletResponseWrapper) {
      resWrapper = (HttpServletResponseWrapper) res;
      parentRes = (HttpServletResponse) resWrapper.getResponse();
    }

    ForwardRequest subRequest;
    subRequest = new ForwardRequest(parentReq, parentRes, invocation);

    ForwardResponse subResponse = subRequest.getResponse();

    HttpServletRequest topRequest = subRequest;
    HttpServletResponse topResponse = subResponse;

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);
      topRequest = reqWrapper;
    }

    if (resWrapper != null) {
      resWrapper.setResponse(subResponse);
      topResponse = resWrapper;
    }
    
    boolean isValid = false;

    subRequest.startRequest();

    try {
      invocation.service(topRequest, topResponse);

      isValid = true;
    } finally {
      if (reqWrapper != null)
        reqWrapper.setRequest(parentReq);
      
      if (resWrapper != null)
        resWrapper.setResponse(parentRes);

      subRequest.finishRequest();
      
      // server/106r, ioc/0310
      if (isValid) {
        finishResponse(res);
      }
    }
  }

  private void finishResponse(HttpServletResponse res)
    throws ServletException, IOException
  {
    if (res instanceof CauchoResponse) {
      ((CauchoResponse) res).close();
    }
    else {
      try {
        OutputStream os = res.getOutputStream();
        os.close();
      } catch (Exception e) {
      }
      
      try {
        PrintWriter out = res.getWriter();
        out.close();
      } catch (Exception e) {
      }
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

    Invocation invocation = _includeInvocation;

    HttpServletRequest parentReq = req;
    HttpServletRequestWrapper reqWrapper = null;

    if (req instanceof HttpServletRequestWrapper) {
      reqWrapper = (HttpServletRequestWrapper)  req;
      parentReq = (HttpServletRequest) reqWrapper.getRequest();
    }

    HttpServletResponse parentRes = res;
    HttpServletResponseWrapper resWrapper = null;

    if (res instanceof HttpServletResponseWrapper) {
      resWrapper = (HttpServletResponseWrapper)  res;
      parentRes = (HttpServletResponse) resWrapper.getResponse();
    }
    
    IncludeRequest subRequest
      = new IncludeRequest(parentReq, parentRes, invocation);
    IncludeResponse subResponse = subRequest.getResponse();

    HttpServletRequest topRequest = subRequest;
    HttpServletResponse topResponse = subResponse;

    if (reqWrapper != null) {
      reqWrapper.setRequest(subRequest);
      topRequest = reqWrapper;
    }

    if (resWrapper != null) {
      resWrapper.setResponse(subResponse);
      topResponse = resWrapper;
    }
    
    // jsp/15lf, jsp/17eg - XXX: integrated with ResponseStream?
    // res.flushBuffer();

    subRequest.startRequest();

    try {
      invocation.service(topRequest, topResponse);
    } finally {
      if (reqWrapper != null)
        reqWrapper.setRequest(parentReq);
      
      if (resWrapper != null)
        resWrapper.setResponse(parentRes);
      
      subRequest.finishRequest();
    }
  }

  /**
   * Dispatch the async resume request to the servlet
   * named by the request dispatcher.
   *
   * @param req the servlet request.
   * @param res the servlet response.
   * @param invocation current invocation
   */
  public void dispatchResume(HttpServletRequest req, HttpServletResponse res,
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
    /* XXX:
    if (response != null) {
      oldRequest = response.getRequest();
      oldStream = response.getResponseStream();
    }
    */

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      /* XXX:
      if (response != null) {
        response.setRequest(subRequest);
        response.setResponseStream(response.getOriginalStream());
      }
      */

      invocation.service(topRequest, res);
    } finally {
      subRequest.finishRequest();

      thread.setContextClassLoader(oldLoader);

      /* XXX:
      if (response != null) {
        response.setRequest(oldRequest);
        response.setResponseStream(oldStream);
        //response.setWriter(oldWriter);
      }
      */

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

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _dispatchInvocation.getRawURI() + "]");
  }
}
