/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import com.caucho.portal.generic.context.ConnectionContext;

import java.io.*;

import java.util.*;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;


/**
 * A PortletConnection is used to obtain {@link Action} and {@link Render}
 * objects.
 *
 * For implementations that support only one portlet for each connection,
 * the pattern of use is:
 *
 * <pre>
 * Window window = ...;
 * Portlet portlet = ...; 
 *
 * Action action = connection.getAction(window);
 *
 * if (action != null) {
 *   try {
 *      if (action.isTarget()) 
 *       action.processAction(portlet);
 *   } 
 *   finally { 
 *     action.finish(); 
 *   } 
 * }
 * 
 * Render render = connection.getRender(window);
 * 
 * if (render != null) {
 *   try {
 *     render.render(portlet);
 *   } 
 *   finally { 
 *     render.finish(); 
 *   } 
 * }
 *
 * </pre> 
 *
 * For implementations that support more than one portlet for each connection,
 * each portlet is identified with a namespace.  An optional Renderer is
 * specified, it is used to obtain a Writer or OutputStream when the portlet
 * requests it.
 *
 * <pre>
 * Window window = ...;
 * Portlet portlet = ...; 
 * String namespace = "..."; 
 *
 * Action action = connection.getAction(window, namespace);
 *
 * if (action != null) {
 *   try {
 *     if (action.isTarget()) 
 *       action.processAction(portlet);
 *   } 
 *   finally { 
 *     action.finish(); 
 *   } 
 * }
 * 
 * Render render = connection.getRender(window, namespace);
 * 
 * if (render != null) {
 *   try {
 *     render.render(portlet);
 *   } 
 *   finally { 
 *     render.finish(); 
 *   } 
 * }
 *
 * </pre> 
 *
 * @see PortletConnection#getAction
 * @see PortletConnection#getRender
 * @see Action
 * @see Render
 *
 */
abstract public class PortletConnection
{
  /**
   * Name of the request attribute that stores the connection
   */
  public static final String PORTLET_CONNECTION 
    = "com.caucho.portal.generic.PortletConnection";

  /**
   * Return the connection that corresponds to the PortletRequest
   */
  public static PortletConnection getConnection(PortletRequest portletRequest)
  {
    return (PortletConnection) portletRequest.getAttribute(PORTLET_CONNECTION);
  }

  /**
   * Return the Portal that corresponds to the PortletRequest, or null
   * if there is not a current Portal or Connection.
   */
  public static Portal getPortal(PortletRequest portletRequest)
  {
    PortletConnection connection = getConnection(portletRequest);

    if (connection == null)
      return null;
    else
      return connection.getPortal();
  }

  /**
   * Return the Action that corresponds to the PortletRequest, or null
   * if there is not a current Action or Connection.
   */
  public static Action getAction(PortletRequest portletRequest)
  {
    PortletConnection connection = getConnection(portletRequest);

    if (connection == null)
      return null;
    else
      return connection.getCurrentAction();
  }

  /**
   * Return the Render that corresponds to the PortletRequest, or null
   * if there is not a current Render or Connection.
   */
  public static Render getRender(PortletRequest portletRequest)
  {
    PortletConnection connection = getConnection(portletRequest);

    if (connection == null)
      return null;
    else
      return connection.getCurrentRender();
  }

  // --

  static final public Logger log = 
    Logger.getLogger(PortletConnection.class.getName());

  private static int _connectionCount = 10;

  private ConnectionContext _context;

  private String _connectionId;

  private Portal _portal;

  private boolean _connectionFailed;
  private Exception _connectionFailedCause;

  protected PortletConnection()
  {
    int id = _connectionCount++;
    _connectionId = Integer.toString(id, Character.MAX_RADIX);
    _context = new ConnectionContext(this);
  }

  /**
   * A unique identifier for this connection object, used for debugging
   */
  public String getId()
  {
    return _connectionId;
  }

  public void start(Portal portal, InvocationFactory invocationFactory)
  {
    if (_portal != null)
      throw new IllegalStateException("missing finish()?");

    _portal = portal;

    _context.start(invocationFactory);
  }

  public void finish()
  {
    _context.finish();
    _portal = null;
    _connectionFailedCause = null;
    _connectionFailed = false;
  }

  public Portal getPortal()
  {
    return _portal;
  }

  /**
   * Used to indicate that the connection has failed.  A connection fails if an
   * unrecoverable error occurs.
   */
  public void setConnectionFailed()
  {
    if (!_connectionFailed) {
      PortletException ex = new PortletException("connection failed");
      setConnectionFailed(ex);
    }
  }

  /**
   * Used to indicate that the connection has failed.  A connection fails if an
   * unrecoverable error occurs.
   */
  public void setConnectionFailed(Exception ex)
  {
    if (!_connectionFailed) {
      _connectionFailed = true;
      _connectionFailedCause = ex;

      log.log(Level.FINE, ex.toString(), ex);
    }
  }

  /**
   * A connection fails if an unrecoverable error occurs.
   */
  public boolean isConnectionFailed()
  {
    return _connectionFailed;
  }

  /**
   * Handle a constraint failure by sending some response to the client.
   *
   * @return false if the connection cannot handle the constraint failure. 
   */
  abstract public boolean handleConstraintFailure( Constraint constraint, 
                                                   int failureCode )
    throws IOException;

  /**
   * Handle an exception by sending some response to the client.
   *
   * @return false if the connection cannot handle the constraint failure. 
   */
  abstract public boolean handleException(Exception exception);

  /**
   * Return true if the connection can guarantee integrity 
   * (preventing data tampering in the communication process).
   */
  abstract public boolean canGuaranteeIntegrity();

  /**
   * Return true if the connection can guarantee confidentiality (preventing
   * reading while in transit).
   */
  abstract public boolean canGuaranteeConfidentiality();

  /**
   * Set an attribute for the current connection.  Attributes are name/value
   * pairs that are valid for the duration of one connection.
   */
  abstract public void setAttribute(String name, Object o);

  /**
   * Get an attribute for the current connection.  Attributes are name/value
   * pairs that are valid for the duration of one connection.
   */
  abstract public Object getAttribute(String name);

  /**
   * Remove an attribute for the current connection.  Attributes are name/value
   * pairs that are valid for the duration of one connection.
   */
  abstract public void removeAttribute(String name);

  /**
   * Get a list of all attributes for the current connection.  Attributes are
   * name/value pairs that are valid for the duration of one connection.
   *
   * @return an Enumeration of String 
   */
  abstract public Enumeration getAttributeNames();

  /**
   * Return a {@link PortletSession} for the current client, or null if one is
   * not available.
   *
   * A PortletSession once established will be consistently returned for a
   * client on subsequent requests.  Different clients will never have the same
   * PortletSession.
   *
   * @param create, if true create a new session if one does not already exist
   * for the client.
   */
  abstract public PortletSession getPortletSession(boolean create);

  /**
   * Return the scheme portion of the url that was used to make the request.
   *
   * @see javax.portlet.PortletRequest#getScheme
   */
  abstract public String getScheme();

  /**
   * Return the host name portion of the url that was used to make the request.
   *
   * @see javax.portlet.PortletRequest#getServerName
   */
  abstract public String getServerName();

  /**
   * Return the port portion of the url that was used to make the request.
   *
   * @see javax.portlet.PortletRequest#getServerPort
   */
  abstract public int getServerPort();

  /**
   * Return the path to the portal portion of the url that was used to make the
   * request.
   *
   * @see javax.portlet.PortletRequest#getContextPath
   */
  abstract public String getContextPath();

  /**
   * Return the authentication scheme used for the current request.
   * 
   * @return PortletRequest.BASIC_AUTH, PortletRequest.CLIENT_CERT_AUTH,
   * PortletRequest.DIGEST_AUTH, PortletRequest.FORM_AUTH, or a custom method.
   *
   * @see javax.portlet.PortletRequest#getAuthType
   */
  abstract public String getAuthType();

  /**
   * Return true if the connection for the current request is secure, for
   * example it uses HTTPS.
   *
   * @see javax.portlet.PortletRequest#isSecure
   */
  abstract public boolean isSecure();

  /**
   * Return the session id that was supplied by the client for the current
   * request.
   * 
   * @see javax.portlet.PortletRequest#getRequestedSessionId
   */
  abstract public String getRequestedSessionId();

  /**
   * Return true the session id that was supplied by the client for the current
   * request is valid.
   * 
   * @see javax.portlet.PortletRequest#isRequestedSessionIdValid
   */
  abstract public boolean isRequestedSessionIdValid();

  /**
   * Return the identity of the remote user, null if the identity has not been
   * established.
   *
   * @see javax.portlet.PortletRequest#getRemoteUser
   */
  abstract public String getRemoteUser();

  /**
   * Return a {@link java.security.Principal} that contains the identity of 
   * the remote user, null if the identity has not been established. 
   *
   * @see javax.portlet.PortletRequest#getUserPrincipal
   */
  abstract public Principal getUserPrincipal();

  /**
   * Return true if the identity of remote user has been established and the
   * user has been assigned the role.
   *
   * @see javax.portlet.PortletRequest#isUserInRole
   */
  abstract public boolean isUserInRole(String role);

  /**
   * Return the value of the specified connection property as a String, null
   * if the property was not provided by the request from the client.
   *
   * "properties" correspond to HTTP headers in the request for HTTP
   * connections.
   *
   * @see javax.portlet.PortletRequest#getProperty
   */
  abstract public String getProperty(String propertyName);

  /**
   * Return the values of the specified connection property as an array of
   * Strings, null if the property was not provided by the request from the
   * client.
   *
   * "properties" correspond to HTTP headers in the request for HTTP
   * connections.
   *
   * @return an Enumeration of String
   *
   * @see javax.portlet.PortletRequest#getProperties
   */
  abstract public Enumeration getProperties(String propertyName);

  /**
   * Return the names of available properties for the connection.
   *
   * "properties" correspond to HTTP headers in the request for HTTP
   * connections.
   *
   * @return an Enumeration of String
   *
   * @see javax.portlet.PortletRequest#getPropertyNames
   */
  abstract public Enumeration getPropertyNames();

  /**
   * Get the content types acceptable to the client.  The returned Set 
   * is ordered, the most preferrable content types appear before the least
   * preferred.
   *
   * A return of null or an empty Set indicates that the client content types
   * cannot be determiend, and is treated as an indication that any locale is
   * acceptable.
   */
  abstract public Set<String> getClientContentTypes();

  /**
   * Get the locales acceptable to the client.  The returned Set is ordered,
   * the most preferrable locale appears before the least preferred.  If the
   * client supports all locales, then a Locale("","","") will be present  in
   * the returned Set.
   *
   * A return of null or an empty Set indicates that the client locales cannot
   * be determiend, and is treated as an indication that any locale is
   * acceptable.
   */
  abstract public Set<Locale> getClientLocales();

  /**
   * Get the character encodings acceptable to the client.  The returned Set is
   * order, the most preferrable character encoding appears before the least
   * preferred.
   *
   * A return of null or an empty Set indicates that the client character
   * encodings cannot be determiend, and is treated as an indication that any
   * locale is
   * acceptable.
   */
  abstract public Set<String> getClientCharacterEncodings();

  /**
   * Return the MIME type of the data supplied as the "body" of the request,
   * null if not known. 
   *
   * @see javax.portlet.ActionRequest#getContentType
   */
  abstract public String getSubmitContentType();

  /**
   * Return the length of of the data supplied as the "body" of the request,
   * -1 if not known. 
   *
   * @see javax.portlet.ActionRequest#getContentLength
   */
  abstract public int getSubmitContentLength();

  /**
   * Return the binary body of the current request. 
   *
   * @throws IllegalStateException if getReader() has already been
   * called for this connection.
   *
   * @throws IOException
   *
   * @see javax.portlet.ActionRequest#getPortletInputStream
   */
  abstract public InputStream getSubmitInputStream()
    throws IOException, IllegalStateException;

  /**
   * Override the character encoding used by the Reader obtained
   * using {@link #getReader}. This method must be called prior to reading
   * input using {@link #getReader} or {@link #getPortletInputStream}. 
   *
   * @throws UnsupportedEncodingException
   *
   * @throws IllegalStateException if getReader() has already been called for
   * this connection.
   *
   * @see javax.portlet.ActionRequest#setCharacterEncoding
   */
  abstract public void setSubmitCharacterEncoding(String enc)
    throws UnsupportedEncodingException, IllegalStateException;

  /**
   * Return the name of the character encoding that will be used by the Reader
   * obtained using {@link #getReader}, null if none.
   *
   * @see javax.portlet.ActionRequest#getCharacterEncoding
   */
  abstract public String getSubmitCharacterEncoding();

  abstract public BufferedReader getSubmitReader()
    throws UnsupportedEncodingException, IOException;

  /**
   * Encode a url with any special encoding needed by the protocol,
   * for example by adding a sesison id.
   */
  abstract public String encodeURL(String path);

  /**
   * Resolve a url so that it makes a request to the portal
   */
  abstract public String resolveURL(String partialUrl);

  /**
   * Resolve a url so that it makes a request to the portal with 
   * the specified level of security.
   */
  abstract public String resolveURL(String partialUrl, boolean isSecure)
    throws PortletSecurityException;


  abstract public void sendRedirect(String location)
    throws IllegalStateException, IOException;

  /**
   * Set a property to be returned to the client.
   *
   * "properties" correspond to HTTP headers in the response for HTTP
   * connections.
   *
   * @see javax.portlet.PortletResponse#setProperty
   */ 
  abstract public void setProperty(String name, String value);

  /**
   * Add a value to a property to be returned to the client.
   *
   * "properties" correspond to HTTP headers in the response for HTTP
   * connections.
   *
   * @see javax.portlet.PortletResponse#addProperty
   */ 
  abstract public void addProperty(String name, String value);

  
  /**
   * Set the content type to use for the response.
   */
  abstract public void setContentType(String contentType);

  /** 
   * Return the content type established with setContentType(), or null if
   * setContentType() has not been called.
   */ 
  abstract public String getContentType();

  /**
   * Set the locale to use for the response.
   */
  abstract public void setLocale(Locale locale);

  /**
   * Return the Locale established with setLocale(), or null if setLocale()
   * has not been called.
   */
  abstract public Locale getLocale();

  abstract public void setBufferSize(int size);

  abstract public int getBufferSize();

  abstract public void flushBuffer() 
    throws IOException;

  abstract public void resetBuffer();

  abstract public void reset();

  abstract public boolean isCommitted();

  /** 
   * @throws IllegalStatementException if the content type has not been set
   * with setContentType.
   */
  abstract public OutputStream getOutputStream() 
    throws IOException;

  abstract public String getCharacterEncoding();

  abstract public void setCharacterEncoding(String enc) 
    throws UnsupportedEncodingException;

  /** 
    * @throws IllegalStatementException if the content type has not been set
    * with setContentType.
    */
  abstract public PrintWriter getWriter() 
    throws IOException;


  /**
   * Get an Action for a namespace.
   * Return null if the action stage for the request is complete
   * or there is some other reason that the window and it's children
   * should not proceed further in the action stage.
   *
   * @throws PortletException if the namespace has already been seen in the
   * actionstagephase of this connection
   */ 
  public Action getAction( Window window, String namespace )
    throws PortletException, IOException
  {
    return _context.getAction(window, namespace);
  }

  /**
   * Get the current Action object, established from a call to getRender().
   */
  public Action getCurrentAction()
  {
    return _context.getCurrentAction();
  }

  /**
   * Get a Render for a namespace.
   * Return null if there is some reason that the window and it's children
   * should not be rendered.
   *
   * @throws PortletException if the namespace has already been seen in the
   * render phase of this connection
   */ 
  public Render getRender( Window window, String namespace )
    throws PortletException, IOException
  {
    return _context.getRender(window, namespace);
  }

  /**
   * Get the current Render object, established from a call to getRender().
   */
  public Render getCurrentRender()
  {
    return _context.getCurrentRender();
  }


  /**
   * Throw an exception if an error was encountered when using this conneciton.
   */
  public void checkForFailure()
    throws PortletException
  {
    if (_connectionFailed) {
      if (_connectionFailedCause == null)
        throw new PortletException("connection failed");
      else
        throw new PortletException(
            "connection failed: " + _connectionFailedCause.toString(),
            _connectionFailedCause );
    }
  }

  /**
   * Used in derived classes during finish() to determine if 
   * the response is private
   */
  protected boolean isPrivate()
  {
    return _context.isConnectionPrivate();
  }

  /**
   * Used in derived classes during finish() to determine
   * the maximum expires time for the connection, derived classes use this
   * value to send an expires timeout to the client.
   * -1 means never expire, 0 means expire immediately, otherwise it is a
   *  number in seconds.
   */
  protected int getExpirationCache()
  {
    return _context.getConnectionExpirationCache();
  }

}

