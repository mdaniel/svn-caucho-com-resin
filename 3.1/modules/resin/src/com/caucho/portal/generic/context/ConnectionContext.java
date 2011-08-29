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



package com.caucho.portal.generic.context;

import com.caucho.portal.generic.*;

import javax.portlet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 * ConnectionContext tracks the state of a PortletConnection and provides
 * methods for all operations that affect the connection.
 *
 * InterfaceImpl classes implement interfaces and delegate all operations to
 * the ConnectionContext.
 *
 * <ul> 
 * <li>{@link com.caucho.portal.generic.context.ActionImpl} 
 * <li>{@link com.caucho.portal.generic.context.RenderImpl} 
 * <li>{@link com.caucho.portal.generic.context.PortalRequestImpl} 
 * <li>{@link com.caucho.portal.generic.context.PortalResponseImpl} 
 * </ul>
 * 
 * <ul> 
 * <li>{@link com.caucho.portal.generic.context.PortletRequestImpl} 
 * <li>{@link com.caucho.portal.generic.context.PortletResponseImpl} 
 * <li>{@link com.caucho.portal.generic.context.ActionRequestImpl} 
 * <li>{@link com.caucho.portal.generic.context.ActionResponseImpl} 
 * <li>{@link com.caucho.portal.generic.context.RenderRequestImpl} 
 * <li>{@link com.caucho.portal.generic.context.RenderResponseImpl} 
 * </ul> 
 *
 * Some of the state of the connection depends on the "current window".  The
 * ConnectionContext maintains a stack of {@link WindowContext}. 
 */ 
public class ConnectionContext {
  private static final Logger log = PortletConnection.log;

  private static final String EXPIRATION_CACHE 
    = "javax.portlet.expirationCache";

  private static final String RENDER_REQUEST
    = "javax.portlet.renderRequest";

  private static final String RENDER_RESPONSE
    = "javax.portlet.renderResponse";

  private static final String PORTLET_CONFIG
    = "javax.portlet.portletConfig";

  private static final Locale LOCALE_ANY = new Locale("", "", "");

  ActionImpl _action = new ActionImpl(this);
  RenderImpl _render = new RenderImpl(this);
  PortalRequestImpl _portalRequest = new PortalRequestImpl(this);
  PortalResponseImpl _portalResponse = new PortalResponseImpl(this);

  PortletRequestImpl _portletRequest = new PortletRequestImpl(this);
  PortletResponseImpl _portletResponse = new PortletResponseImpl(this);
  ActionRequestImpl _actionRequest = new ActionRequestImpl(this);
  ActionResponseImpl _actionResponse = new ActionResponseImpl(this);
  RenderRequestImpl _renderRequest = new RenderRequestImpl(this);
  RenderResponseImpl _renderResponse = new RenderResponseImpl(this);

  final static int STAGE_START = 1;
  final static int STAGE_ACTION = 2;
  final static int STAGE_DONEACTION = 3;
  final static int STAGE_RENDER = 4;
  final static int STAGE_DONE = 5;

  private PortletConnection _connection;

  private InvocationFactory _invocationFactory;
  private TopLevelResponseHandler _topLevelResponseHandler;

  int _connectionExpirationCache = -1;
  boolean _connectionIsPrivate = false;
  private boolean _forbidRedirect;

  private PreferencesStore _preferencesStore;
  private UserAttributeStore _userAttributeStore;

  private int _stage = STAGE_START;

  private Map<String, WindowContext> _windowContextMap 
    = new HashMap<String, WindowContext>();

  private ArrayList<WindowContext> _windowContextStack 
    = new ArrayList<WindowContext>();

  private WindowContext _windowContext; // current

  public ConnectionContext(PortletConnection connection)
  {
    _connection = connection;

    _topLevelResponseHandler = new TopLevelResponseHandler(connection, this);
  }

  public void start( InvocationFactory invocationFactory )
  {
    log(Level.FINER, "starting connection");

    if (_stage != STAGE_START || _invocationFactory != null)
      throw new IllegalStateException("missing finish()? " + _stage);

    _invocationFactory = invocationFactory;
  }

  public void finish()
  {
    log(Level.FINEST, "finishing connection");

    try {
      for (int i = _windowContextStack.size() - 1; i >= 0; i--) {
        try {
          WindowContext windowContext = _windowContextStack.get(i);

          if (windowContext != null)
            finishWindowContext(windowContext);
        }
        catch (IOException ex) {
          log(Level.WARNING, ex.toString(), ex);
        }
      }
    }
    finally {
      _windowContextStack.clear();
    }

    _windowContextMap.clear();
    _windowContext = null;
    _stage = STAGE_START;
    _forbidRedirect = false;
    _preferencesStore = null;
    _userAttributeStore = null;
    _connection = null;

    _invocationFactory = null;
  }

  /**
   * finish a WindowContext that is no longer needed for this connection
   */
  private void finishWindowContext(WindowContext windowContext)
    throws IOException
  {
    Map<String, String> userAttributeMap = windowContext.getUserAttributeMap();
    LinkingPortletPreferences pref = windowContext.getPreferences();

    windowContext.finish();

    if (pref != null) {
      Map<String, String[]> store = pref.getStore();
      pref.finish();
      getPreferencesStore().finish(store);
    }

    if (userAttributeMap != null) {
      getUserAttributeStore().finish(userAttributeMap);
    }
  }

  protected void log(Level level, String message)
  {
    if (!log.isLoggable(level))
      return;

    String namespace = _windowContext == null 
                        ? null 
                        : _windowContext.getNamespace();

    log(namespace, level, message, null);
  }

  protected void log(Level level, String message, Exception ex)
  {
    if (!log.isLoggable(level))
      return;

    String namespace = _windowContext == null 
                        ? null 
                        : _windowContext.getNamespace();

    log(namespace, level, message, ex);
  }

  protected void log(String namespace, Level level, String message)
  {
    if (!log.isLoggable(level))
      return;

    log(namespace, level, message, null);
  }

  protected void log(String namespace, Level level, String message, Exception ex)
  {
    if (log.isLoggable(level)) {
      StringBuffer sb = new StringBuffer(256);

      sb.append('[');

      sb.append(_connection.getId());

      if (namespace != null) {
        sb.append(' ');
        sb.append(namespace);
      }

      sb.append(']');

      if (_stage == STAGE_ACTION)
        sb.append('a');
      else if (_stage == STAGE_RENDER)
        sb.append('r');
      else
        sb.append(' ');

      sb.append(' ');
      sb.append(message);

      if (ex == null)
        log.log(level, sb.toString());
      else
        log.log(level, sb.toString(), ex);
    }
  }

  protected PortletConnection getConnection()
  {
    return _connection;
  }

  protected InvocationFactory getInvocationFactory()
  {
    return _invocationFactory;
  }

  protected void setConnectionFailed(Exception ex)
  {
    _connection.setConnectionFailed(ex);
  }

  protected void setConnectionFailed()
  {
    _connection.setConnectionFailed();
  }

  protected boolean isConnectionFailed()
  {
    return _connection.isConnectionFailed();
  }

  public int getConnectionExpirationCache()
  {
    return _connectionExpirationCache;
  }

  protected void updateConnectionExpirationCache(int expirationCache)
  {
    if (_connectionExpirationCache != 0 
        && expirationCache >= 0
        && _connectionExpirationCache < expirationCache) 
    {
      _connectionExpirationCache = expirationCache;
    }
  }

  public boolean isConnectionPrivate()
  {
    return _connectionIsPrivate;
  }

  protected void setConnectionPrivate()
  {
    _connectionIsPrivate = true;
  }

  protected void setForbidRedirect()
  {
    _forbidRedirect = true;
  }

  protected boolean isForbidRedirect()
  {
    return _forbidRedirect;
  }

  public PortalRequest getPortalRequest()
  {
    return _portalRequest;
  }

  public PortalResponse getPortalResponse()
  {
    return _portalResponse;
  }

  public Portal getPortal()
  {
    return _connection.getPortal();
  }

  public PortalContext getPortalContext()
  {
    return _connection.getPortal().getPortalContext();
  }

  protected PreferencesStore getPreferencesStore()
  {
    if (_preferencesStore == null)
      _preferencesStore = getPortal().getPreferencesStore();

    return _preferencesStore;
  }

  protected UserAttributeStore getUserAttributeStore()
  {
    if (_userAttributeStore == null)
      _userAttributeStore = getPortal().getUserAttributeStore();

    return _userAttributeStore;
  }

  private void pushStack(WindowContext windowContext)
  {
    _windowContextStack.add(_windowContext);

    _windowContext = windowContext;
  }

  private void popStack()
  {
    try {
      if (_windowContextStack.size() == 0)
        throw new IllegalStateException(
            "top of window stack reached, extra finish()?");

      WindowContext windowContext = _windowContext;

      _windowContext = null;

      if (_stage == STAGE_RENDER)
        finishWindowContext(windowContext);

      _windowContext = _windowContextStack.remove( _windowContextStack.size() - 1 );
    } 
    catch (Exception ex) {
      setConnectionFailed(ex);
    }
  }

  /**
   * See if the Window can handle the exception.  Return true
   * if the exception has been handled in some way.
   *
   * Side-effect: might set windows.isExcluded to true 
   *              might set window.setException to null if it is handled
   */
  protected void handleException()
    throws PortletException, IOException
  {
    final Exception exception = _windowContext.getException();

    if (exception == null)
      return;

    if (log.isLoggable(Level.FINER))
      log(Level.FINER, "handling exception: " + exception.getClass().getName());

    final WindowContext windowContext = _windowContext;

    ExceptionEvent event = new ExceptionEvent() {
      public Exception getException()
      {
        return exception;
      }

      public void setHandled(boolean hideWindow)
      {
        windowContext.setException(null);

        if (hideWindow)
          windowContext.setExcluded();
      }

      public boolean isHandled()
      {
        return !windowContext.isException();
      }

      public boolean isHideWindow()
      {
        return windowContext.isExcluded();
      }
    };

    Window window = getWindow();

    window.handleException( getRenderRequest(), 
                            getRenderResponse(), 
                            event );
  }


  /**
   * Check for constraint failures and if any are encountered
   * set the _windowContext appropriately.
   */
  protected void checkConstraints()
  {
    ArrayList<Constraint> constraints = getWindow().getConstraints();

    if (constraints == null)
      return;

    if (log.isLoggable(Level.FINER))
      log(Level.FINER, "checking constraints");

    int startIndex = _windowContext.getConstraintIndex();

    for (int i = startIndex; i < constraints.size(); i++) {
      _windowContext.setConstraintIndex(i + 1);

      Constraint constraint = constraints.get(i);

      int result = constraint.check(getPortletRequest(), getPortletResponse());

      if (result == Constraint.SC_PASS) {
      }
      else if (result == Constraint.SC_EXCLUDE) {
        _windowContext.setExcluded();
        if (log.isLoggable(Level.FINER))
          log(Level.FINE, "constraint excludes window");

        return;
      }
      else {
        _windowContext.setConstraintFailure(constraint, result);

        if (log.isLoggable(Level.FINER))
          log(Level.FINE, 
              "constraint failed: " + constraint.getClass().getName());

        return;
      }
    }
  }

  /**
   * See if the Window can handle the constraint.  Return true
   * if the constraint has been handled in some way.
   *
   * Side-effect: might set isExcluded to true 
   */
  protected boolean handleConstraintFailure()
    throws PortletException, IOException
  {
    final Constraint constraint 
      = _windowContext.getConstraintFailureConstraint();

    if (constraint == null)
      return true;

    if (log.isLoggable(Level.FINEST))
      log(Level.FINEST, 
          "handling constraint failure: " + constraint.getClass().getName());

    final WindowContext windowContext = _windowContext;

    final int code = _windowContext.getConstraintFailureCode();

    ConstraintFailureEvent event = new ConstraintFailureEvent() {
      public Constraint getConstraint()
      {
        return constraint;
      }

      public int getStatusCode()
      {
        return code;
      }

      public void setHandled(boolean hideWindow)
      {
        windowContext.setConstraintFailure(null, 0);

        if (hideWindow)
          windowContext.setExcluded();
      }

      public boolean isHandled()
      {
        return !windowContext.isConstraintFailure();
      }

      public boolean isHideWindow()
      {
        return windowContext.isExcluded();
      }
    };

    Window window = getWindow();

    window.handleConstraintFailure( getRenderRequest(), 
                                    getRenderResponse(), 
                                    event );

    return !windowContext.isConstraintFailure();
  }

  private boolean startActionOrRender( Window window,
                                       String namespace,
                                       boolean isActionStage )
    throws PortletException, IOException
  {
    if (log.isLoggable(Level.FINEST)) {
      log(Level.FINER, "portlet `" + 
          window.getPortletConfig().getPortletName() 
          + "' for namespace `" + namespace + "'");
    }

    if (_windowContext != null 
        &&
        (_windowContext.isExcluded()
         || _windowContext.getException() != null
         || _windowContext.isConstraintFailure()))
    {
      if (log.isLoggable(Level.FINER)) 
      {
        if (_windowContext.isExcluded())
          log(Level.FINER, 
              "child `" + namespace 
              + "' excluded because parent is excluded");
        else if (_windowContext.isException())
          log(Level.FINER,
              "child `" + namespace 
              + "' excluded because parent has exception");
        else if (_windowContext.isConstraintFailure())
          log(Level.FINER,
              "child `" + namespace 
              + "' excluded because parent has constraint failure");
      }

      return false;
    }

    boolean fail = true;

    try {
      boolean isTopLevel = _windowContextStack.size() == 0;

      // reuse a WindowContext prepared in a previous stage or create a new one

      WindowContext windowContext 
        = namespace == null ? null : _windowContextMap.get(namespace);

      if (windowContext != null) {

        if (isActionStage)
          throw new PortletException(
            "duplicate namespace `" + namespace  + "'");

        else if (windowContext.getWindow() != window)
          throw new PortletException(
                "cannot have different Window"
                + "  in render stage for namespace `" + namespace + "'");
  
        // check if windowContext was excluded in previous stage

        if (windowContext.isExcluded()) {
          if (log.isLoggable(Level.FINER)) 
          {
            if (_windowContext.isExcluded())
              log(Level.FINER, 
                  "child `" + namespace + "' excluded in previous stage");
          }

          fail = false;
          return false;
        }
      }
      else {
        windowContext = new WindowContext();

        windowContext.start(window, namespace);

        if (windowContext.getNamespace() != null) {
          _windowContextMap.put(windowContext.getNamespace(), windowContext);

          // prepare invocation

          Invocation invocation 
            = getInvocationFactory().getInvocation(namespace);

          windowContext.setInvocation(invocation);

          if (invocation.isActionTarget()) {
            log(Level.FINER, "action target");

            Map<String, String[]> actionMap 
              = invocation.releaseParameterMap();

            windowContext.setActionMap(actionMap);
          }
        }
      }

      // set the Context to work on the new namespace

      pushStack(windowContext);

      if (_windowContext.getNamespace() == null && log.isLoggable(Level.FINER))
        log(Level.FINER, "no invocation for null namespace");

      if (isActionStage) {
        checkConstraints();

        if (_windowContext.isConstraintFailure() || _windowContext.isExcluded())
        {
          popStack();
          fail = false;
          return false;
        }
      }

      fail = false;
    } 
    catch (PortletException ex) {
      setConnectionFailed(ex);
      throw ex;
    }
    catch (RuntimeException ex) {
      setConnectionFailed(ex);
      throw new PortletException(ex);
    }
    finally {
      if (fail) {
        setConnectionFailed();
        return false;
      }
    }

    return true;
  }

  public PortletRequest getPortletRequest()
  {
    return _portletRequest;
  }

  public PortletResponse getPortletResponse()
  {
    return _portletResponse;
  }

  public Action getAction( Window window,
                           String namespace )
    throws PortletException, IOException
  {
    if (isConnectionFailed())
      return null;

    if (_stage == STAGE_START) {
      if (log.isLoggable(Level.FINER))
        log(Level.FINER, "starting action stage");

      _stage = STAGE_ACTION;
    }
    else if (_stage != STAGE_ACTION) {
      IllegalStateException ex 
        = new IllegalStateException("missing finish()? " + _stage);

      setConnectionFailed(ex);

      throw ex;
    }

    if (!startActionOrRender(window, namespace, true))
      return null;

    return getCurrentAction();
  }

  public Action getCurrentAction()
  {
    return _stage == STAGE_ACTION ? _action : null;
  }

  public boolean isTarget()
  {
    return ( _stage == STAGE_ACTION  
             && _windowContext.getActionMap() != null 
             && !_windowContext.isExcluded() );

  }

  public ActionRequest getActionRequest()
  {
    if ( _stage != STAGE_ACTION || _windowContext.isExcluded() )
      return null;
    else
      return _actionRequest;
  }

  public ActionResponse getActionResponse()
  {
    if ( _stage != STAGE_ACTION || _windowContext.isExcluded() )
      return null;
    else
      return _actionResponse;
  }

  public void processAction(Portlet portlet)
  {
    if ( _stage != STAGE_ACTION )
      throw new IllegalStateException("not in action stage");

    if (log.isLoggable(Level.FINEST))
      log(Level.FINEST, "processAction()");

    try {
      portlet.processAction(_actionRequest, _actionResponse);
    }
    catch (Exception ex) {
      if (log.isLoggable(Level.FINE))
        log(Level.FINE, ex.toString(), ex);

      _windowContext.setException(ex);
    }
  }

  void finishAction()
    throws IOException, PortletException
  {
    boolean fail = true;

    try {
      if (_windowContext == null) {
        throw new IllegalStateException(
            "cannot finish action, at top of stack");
      }

      if (_stage != STAGE_ACTION && _stage != STAGE_DONEACTION) {
        throw new IllegalStateException(
            "cannot finish action for " + _windowContext.getNamespace() 
            + ", stage is " + _stage);
      }

      popStack();

      if (_windowContext == null) {
        _stage = STAGE_DONEACTION;

        if (log.isLoggable(Level.FINER))
          log(Level.FINER, "finishing action stage");
      }

      fail = false;
    } 
    catch (RuntimeException ex) {
      setConnectionFailed(ex);
      throw ex;
    }
    finally {
      if (fail)
        setConnectionFailed();
    }
  }

  // XXX: this is bogus, see comments in CacheKey.java
  // about how the cache key should be built up.
  // Basically, every WindowContext that has an expirationCache !=0
  // should have a CacheKey that get's filled with the invocation
  // for itself _and_ every child WindowContext during the action phase.

  private CacheKey getCacheKey()
  {
    CacheKey cacheKey = null;

    if (cacheKey == null)
      cacheKey = new CacheKey();
    else
      cacheKey.reset();

    WindowContext windowContext = _windowContext;

    if (windowContext.getNamespace() == null)
      return null;

    cacheKey.setNamespace(windowContext.getNamespace());
    cacheKey.setPortletMode(getPortletMode());
    cacheKey.setWindowState(getWindowState());
    cacheKey.setContentType(getResponseContentType());
    cacheKey.setLocale(getResponseLocale());
    cacheKey.setPrivate(windowContext.isPrivate());
    cacheKey.setRequestedSessionId(_connection.getRequestedSessionId());

    return null;  // XXX:
  }

  /**
   * Reset window specific attributes to null and return a map of the old
   * values
   */
  private Map<String, String> resetWindowRequestAttributes()
  {
    Map<String, String> attr = null;

    attr = getAttribute(attr, "javax.portlet.title", true);
    attr = getAttribute(attr, "javax.portlet.short-title", true);
    attr = getAttribute(attr, "javax.portlet.keywords", true);
    attr = getAttribute(attr, "javax.portlet.description", true);

    return attr;
  }

  /**
   * Return a map of window specific request attributes, attributes with null
   * values are not included, the return may be null.
   */
  private Map<String, String> getWindowRequestAttributes()
  {
    Map<String, String> attr = null;

    attr = getAttribute(attr, "javax.portlet.title", false);
    getAttribute(attr, "javax.portlet.short-title", false);
    getAttribute(attr, "javax.portlet.keywords", false);
    getAttribute(attr, "javax.portlet.description", false);

    return attr;
  }

  private Map<String, String> getAttribute( Map<String, String> map, 
                                            String name, 
                                            boolean isReset )
  {
    String value = (String) _connection.getAttribute(name);

    if (isReset || value != null) {
      if (map == null)
        map = new LinkedHashMap<String, String>();

      map.put(name, value);

      if (isReset)
        _connection.removeAttribute(name);
    }

    return map;
  }

  private void restoreWindowRequestAttributes(Map<String, String> map)
  {
    if (map != null && !map.isEmpty()) {
      Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();

      do {
        Map.Entry<String, String> entry = iter.next();
        _connection.setAttribute(entry.getKey(), entry.getValue());
      } while (iter.hasNext());
    }
  }

  public Render getRender( Window window,
                           String namespace )
    throws PortletException, IOException
  {
    if (isConnectionFailed())
      return null;

    if (_stage == STAGE_START || _stage == STAGE_DONEACTION) {
        if (log.isLoggable(Level.FINER))
          log(Level.FINER, "starting render stage");

      _stage = STAGE_RENDER;

      _connection.setAttribute("javax.portlet.renderRequest", _renderRequest);
      _connection.setAttribute("javax.portlet.renderResponse", _renderResponse);
    }
    else if (_stage != STAGE_RENDER) {
      IllegalStateException ex 
        = new IllegalStateException("missing finish()? " + _stage);

      setConnectionFailed(ex);

      throw ex;
    }

    checkAlwaysWriteOrStream();

    ResponseHandler parentResponseHandler 
      = _windowContext == null 
         ? _topLevelResponseHandler 
         : _windowContext.getResponseHandler();

    if (!startActionOrRender(window, namespace, false))
      return null;

    // discard preferences from action stage
     
    LinkingPortletPreferences pref = _windowContext.getPreferences();
    if (pref != null)
      pref.discard();


    // response handler

    _windowContext.setParentResponseHandler(parentResponseHandler);

    ResponseHandler responseHandler = parentResponseHandler;

    Renderer renderer = _windowContext.getWindow().getRenderer();

    if (renderer != null) {
      BufferFactory bufferFactory = getPortal().getBufferFactory();
      int rendererBufferSize = renderer.getBufferSize();

      if (bufferFactory != null && rendererBufferSize != 0) {

        responseHandler = new BufferedResponseHandler( responseHandler, 
                                                       bufferFactory,
                                                       rendererBufferSize );
      }

      responseHandler = new RendererResponseHandler( this,
                                                     responseHandler, 
                                                     renderer,
                                                     getRenderRequest(),
                                                     getRenderResponse(),
                                                     namespace );
    }

    int bufferSize = _windowContext.getWindow().getBufferSize();

    if (bufferSize != 0) {
      BufferFactory bufferFactory = getPortal().getBufferFactory();

      if (bufferFactory != null) {
        responseHandler = new BufferedResponseHandler( responseHandler, 
                                                       bufferFactory,
                                                       bufferSize );
      }
    }

    _windowContext.setResponseHandler(responseHandler);

    boolean isPrivate = _windowContext.getWindow().isPrivate();

    if (isPrivate) {
      _windowContext.setPrivate();
      setConnectionPrivate();
    }

    // set request attributes

    _connection.setAttribute( "javax.portlet.portletConfig", 
                              getWindow().getPortletConfig());

    Map<String, String> requestAttributes = resetWindowRequestAttributes();

    _windowContext.setWindowRequestAttributes(requestAttributes);

    // check for exception failures from previous stage

    if (_windowContext.isException()) {
      if (log.isLoggable(Level.FINE))
        log(Level.FINE, "previous exception");

      finishRender();
      return null;
    }

    // constraints 

    if (!_windowContext.isConstraintFailure())
      checkConstraints();

    if (_windowContext.isConstraintFailure())
    {
      if (!handleConstraintFailure()) {
        if (log.isLoggable(Level.FINE))
          log(Level.FINE, 
              "constraint failure unhandled, propagating to parent");
      }
      else {
        if (log.isLoggable(Level.FINER)) {
          if (_windowContext.isExcluded())
            log(Level.FINER, "constraint failure handled, excluding");
          else
            log(Level.FINER, "constraint failure handled by window");
        }
      }

      finishRender();
      return null;
    }

    // XXX: cache caching

    int expirationCache = _windowContext.getExpirationCache();


    if (expirationCache != 0 
        && (!isPrivate || getRequestedSessionId() != null)) 
    {
      _windowContext.setExpirationCache(expirationCache);
    }

    Cache cache = getPortal().getCache();
    CacheKey cacheKey = null;
        
    if (cache != null)
      cacheKey = getCacheKey();

    if (cacheKey != null) 
    {
      int result = cache.respondFromCache( cacheKey, 
                                           getRenderRequest(),
                                           getRenderResponse() );

      if (result != 0) 
      {
        updateConnectionExpirationCache(result);
        return null;
      }

      CachingResponseHandler cacheResponseHandler
        = new CachingResponseHandler( _windowContext.getResponseHandler(), 
                                      cache, 
                                      _windowContext.getNamespace(),
                                      _windowContext.getExpirationCache(),
                                      _windowContext.isPrivate() );

      _windowContext.setResponseHandler(cacheResponseHandler);
    }

    return getCurrentRender();
  }

  public RenderRequest getRenderRequest()
  {
    if (_stage != STAGE_RENDER || _windowContext.isExcluded() )
      return null;
    else
      return _renderRequest;
  }

  public RenderResponse getRenderResponse()
  {
    if (_stage != STAGE_RENDER || _windowContext.isExcluded() )
      return null;
    else
      return _renderResponse;
  }

  public void render(Portlet portlet)
    throws PortletException, IOException
  {
    if ( _stage != STAGE_RENDER )
      throw new IllegalStateException("not in render stage");

    if (log.isLoggable(Level.FINEST))
      log(Level.FINEST, "render()");

    if (_windowContext.getException() == null
        && !_windowContext.isExcluded()
        && !_windowContext.isConstraintFailure())
    {
      try {
        portlet.render(getRenderRequest(), getRenderResponse());
      }
      catch (Exception ex) {
        if (log.isLoggable(Level.FINE))
          log(Level.FINE, ex.toString(), ex);

        _windowContext.setException(ex);

      }
    }
  }

  public Render getCurrentRender()
  {
    return _stage == STAGE_RENDER ? _render : null;
  }

  void finishRender()
    throws IOException, PortletException
  {
    boolean fail = true;

    try {
      if (_windowContext == null) {
        throw new IllegalStateException(
            "cannot finish render, at top of stack");
      }

      Map<String, String> requestAttributes
        = _windowContext.getWindowRequestAttributes();

      if (_stage != STAGE_RENDER && _stage != STAGE_DONE) {
        throw new IllegalStateException(
            "cannot finish render for " + _windowContext.getNamespace()
            + ", stage is " + _stage);
      }

      if (_windowContext.isException()) {
        try {
          reset(false); // do not resetRenderer

          handleException();

          if (log.isLoggable(Level.FINE)) {
            if (_windowContext.isException())
                log(Level.FINE, "exception unhandled, propagating to parent");
            else if (_windowContext.isExcluded())
              log(Level.FINER, "exception handled, excluding");
            else
              log(Level.FINER, "exception handled by window");
          }
        }
        catch (Exception ex) {
          log.log(Level.WARNING, ex.toString(), ex);
        }
      }

      Exception exception = _windowContext.getException();

      boolean isException = exception != null;

      boolean isConstraintFailure = _windowContext.isConstraintFailure();

      Constraint constraintFailureConstraint
        = _windowContext.getConstraintFailureConstraint();

      int constraintFailureCode
        = _windowContext.getConstraintFailureCode();

      boolean killResponse = _windowContext.isExcluded() 
                             || isConstraintFailure
                             || isException;

      
      if (killResponse) {
        if (log.isLoggable(Level.FINEST)) 
        {
          if (isException)
            log(Level.FINEST, "killResponse due to exception");
          else if (_windowContext.isConstraintFailure())
            log(Level.FINEST, "killResponse due to constraintFailure");
          else
            log(Level.FINEST, "killResponse");
        }

        reset(true); // resetRenderer
      } 

      // unwind the write streams and finish() them

      ResponseHandler parentResponseHandler 
        = _windowContext.getParentResponseHandler();

      CachingResponseHandler cacheResponseHandler = null;

      ResponseHandler next = _windowContext.getResponseHandler();

      while ( next != null && next != parentResponseHandler ) {
        ResponseHandler responseHandler = next;
        next = next.getSuccessor();

        try {
          if (!killResponse && !isException) {
            responseHandler.flushBuffer();
          }

          if (responseHandler instanceof CachingResponseHandler)
            cacheResponseHandler = (CachingResponseHandler) responseHandler;
          else
            responseHandler.finish();
        }
        catch (Exception ex) {
          if (!isException) {
            isException = true;
            exception = ex;
          }
          else {
            if (log.isLoggable(Level.FINEST)) {
              log(Level.FINEST, "exception while finishing response handlers: " + ex.toString(), ex);
            }
          }
        }
      }

      String expirationCacheAttribute 
        = (String) _connection.getAttribute(EXPIRATION_CACHE); 

      _connection.removeAttribute(EXPIRATION_CACHE); 


      // caching

      if (cacheResponseHandler != null) {
        if (killResponse) {
          cacheResponseHandler.finish( 0, null, null );
        } 
        else  {
          try {
            int expirationCache = _windowContext.getExpirationCache();
            CacheKey cacheKey = null;

            if (expirationCache != 0 && expirationCacheAttribute != null)
              expirationCache = Integer.parseInt(expirationCacheAttribute);

            if (expirationCache != 0)
              cacheKey = getCacheKey();

            cacheResponseHandler.finish( expirationCache, 
                                         cacheKey,
                                         getWindowRequestAttributes() );
                                     
          }
          catch (Exception ex) {
            if (!isException) {
              isException = true;
              exception = ex;
            }
          }
        }
      }

      WindowContext windowContext = _windowContext;
      String namespace = _windowContext.getNamespace();

      try {
        popStack();
      }
      finally {
        windowContext.finish();
      }

      if (isConstraintFailure) {
        if (_windowContext != null) {
          if (log.isLoggable(Level.FINEST))
            log(namespace, Level.FINEST, "propagating constraint failure " 
                + constraintFailureConstraint.getClass().getName() 
                + " to parent");

          _windowContext.setConstraintFailure( constraintFailureConstraint,
                                               constraintFailureCode );
        }
        else {
          if (log.isLoggable(Level.FINEST))
            log(namespace, Level.FINEST, "propagating constraint failure  " 
                + constraintFailureConstraint.getClass().getName() 
                + " to connection");

          boolean handled = 
            _connection.handleConstraintFailure( constraintFailureConstraint,
                                                 constraintFailureCode );

          if (!handled && !isException) {
            isException = true;
            exception = new PortletException(
                "Constraint failure " 
                + constraintFailureConstraint.getClass().getName() 
                + "(" + constraintFailureCode + ")");
          }
        }
      }

      if (isException) {
        if (_windowContext != null) {
          if (log.isLoggable(Level.FINEST))
            log(namespace, Level.FINEST, "propagating exception " 
                + exception.getClass().getName() + " to parent");

          _windowContext.setException(exception);
        }
        else {
          if (log.isLoggable(Level.FINEST))
            log(namespace, Level.FINEST, "propagating exception " 
                + exception.getClass().getName() + " to connection");

          try {
            boolean handled = 
              _connection.handleException( exception );

            if (!handled)
              setConnectionFailed(exception);
          }
          catch (Exception ex) {
            setConnectionFailed(exception);
            log.log(Level.WARNING, ex.toString(), ex);
          }
        }
      }

      if (_windowContext == null) {
        if (log.isLoggable(Level.FINER))
          log(Level.FINER, "finishing render stage");

        _stage = STAGE_DONE;
        _connection.setAttribute( "javax.portlet.portletConfig", null );

      }
      else {
        _connection.setAttribute( "javax.portlet.portletConfig",
            getWindow().getPortletConfig());
      }

      restoreWindowRequestAttributes(requestAttributes);

      fail = false;
    } 
    catch (RuntimeException ex) {
      setConnectionFailed(ex);
      throw ex;
    }
    finally {
      if (fail)
        setConnectionFailed();
    }
  }

  /**
   * @throws  IllegalStateException if the window with the namespace
   * has already been processed as the target of an action, or already 
   * renderered.
   *
   * @throws  IllegalArgumentException if the window with the namespace
   * is not found
   */
  protected void checkWindowMutable(String namespace)
    throws IllegalStateException, IllegalArgumentException
  {
    WindowContext windowContext = _windowContextMap.get(namespace);

    if (windowContext == null)
      throw new IllegalArgumentException(
          "namespace `" + namespace + "' not known");

    if (windowContext.getActionMap() != null && !namespace.equals(getNamespace()))
      throw new IllegalStateException(
          "already did processAction() for namespace `" + namespace + "'");

    if (windowContext.getNamespace() == null)
      throw new IllegalStateException(
          "already did render() for namespace `" + namespace + "'");
  }

  protected Window getWindow()
  {
    return _windowContext.getWindow();
  }

  protected Window getWindow(String namespace)
  {
    WindowContext windowContext = _windowContextMap.get(namespace);
    
    return windowContext == null ? null : windowContext.getWindow();
  }

  protected String getNamespace()
  {
    return _windowContext.getNamespace();
  }

  protected Renderer getRenderer()
  {
    return _windowContext.getWindow().getRenderer();
  }

  protected Renderer getRenderer(String namespace)
  {
    WindowContext windowContext = _windowContextMap.get(namespace);
    
    return 
      windowContext == null 
      ? null 
      : windowContext.getWindow().getRenderer();
  }

  protected Invocation getInvocation()
  {
    Invocation invocation = _windowContext.getInvocation();

    if (invocation == null)
      throw new UnsupportedOperationException("operation requires a namespace");
    
    return invocation;
  }

  protected Invocation getInvocation(String namespace)
  {
    return _invocationFactory.getInvocation(namespace);
  }

  protected void forbidRedirectIfInActionStage()
  {
    if (_stage == STAGE_ACTION)
      setForbidRedirect();
  }

  // -- Invocation - WindowState/PortletMode

  public Set<WindowState> getWindowStatesUsed()
  {
    return getInvocationFactory().getWindowStatesUsed();
  }

  public Set<PortletMode> getPortletModesUsed()
  {
    return getInvocationFactory().getPortletModesUsed();
  }

  protected boolean isPortletModeAllowed( Window window,
                                          PortletMode portletMode)
  {
    PortletRequest portletRequest = getPortletRequest();


    boolean allowed = true;

    Renderer renderer = window.getRenderer();

    if (renderer != null)
      allowed = renderer.isPortletModeAllowed(portletRequest, portletMode);

    if (allowed && window != null)
      allowed = window.isPortletModeAllowed(portletRequest, portletMode);

    if (allowed)
      allowed = getPortal().isPortletModeAllowed(portletRequest, portletMode);

    return allowed;
  }

  protected void setPortletMode( Window window,
                                 String namespace,
                                 Invocation invocation,
                                 PortletMode portletMode)
    throws PortletModeException
  {
    if (!isPortletModeAllowed(window, portletMode))
      throw new PortletModeException(
          "PortletMode `" + portletMode 
          + "' not allowed for namespace `" + namespace + "'",
          portletMode);

    forbidRedirectIfInActionStage();

    invocation.setPortletMode(portletMode);
  }


  public boolean isPortletModeAllowed(PortletMode portletMode)
  {
    return isPortletModeAllowed( getWindow(), getPortletMode());
  }

  public boolean isPortletModeAllowed( String namespace, 
                                       PortletMode portletMode)
  {
    return isPortletModeAllowed( getWindow(namespace), 
                                 getPortletMode(namespace));
  }

  public PortletMode getPortletMode()
  {
    return getInvocation().getPortletMode();
  }

  public PortletMode getPortletMode(String namespace)
  {
    return getInvocation(namespace).getPortletMode();
  }

  public void setPortletMode(PortletMode portletMode)
    throws PortletModeException
  {
    setPortletMode( getWindow(),
                    getNamespace(),
                    getInvocation(),
                    portletMode);
  }

  public void setPortletMode(String namespace, PortletMode portletMode)
    throws PortletModeException
  {
    checkWindowMutable(namespace);

    setPortletMode( getWindow(namespace),
                    namespace,
                    getInvocation(namespace),
                    portletMode);
  }

  protected boolean isWindowStateAllowed( Window window,
                                          WindowState windowState)
  {
    PortletRequest portletRequest = getPortletRequest();

    boolean allowed = true;

    Renderer renderer = window.getRenderer();

    if (renderer != null)
      allowed = renderer.isWindowStateAllowed(portletRequest, windowState);

    if (allowed && window != null)
      allowed = window.isWindowStateAllowed(portletRequest, windowState);

    if (allowed)
      allowed = getPortal().isWindowStateAllowed(portletRequest, windowState);

    return allowed;
  }

  protected void setWindowState( Window window,
                                 String namespace,
                                 Invocation invocation,
                                 WindowState windowState )
    throws WindowStateException
  {
    if (!isWindowStateAllowed(window, windowState))
      throw new WindowStateException(
          "WindowState `" + windowState 
          + "' not allowed for namespace `" + namespace + "'",
          windowState);

    forbidRedirectIfInActionStage();

    invocation.setWindowState(windowState);
  }

  public boolean isWindowStateAllowed(WindowState windowState)
  {
    return isWindowStateAllowed( getWindow(), 
                                 windowState );
  }

  public boolean isWindowStateAllowed( String namespace, 
                                       WindowState windowState)
  {
    return isWindowStateAllowed( getWindow(namespace), 
                                 windowState );
  }

  public WindowState getWindowState()
  {
    return getInvocation().getWindowState();
  }

  public WindowState getWindowState(String namespace)
  {
    return getInvocation(namespace).getWindowState();
  }

  public void setWindowState(WindowState windowState)
    throws WindowStateException
  {
    setWindowState( getWindow(),
                    getNamespace(),
                    getInvocation(),
                    windowState );
  }

  public void setWindowState(String namespace, WindowState windowState)
    throws WindowStateException
  {
    checkWindowMutable(namespace);

    setWindowState( getWindow(namespace),
                    namespace,
                    getInvocation(namespace),
                    windowState );
  }


  // -- Invocation - parameters


  public Map<String, String[]> getActionParameterMap()
  {
    Map<String, String[]> actionMap = _windowContext.getActionMap();

    if (actionMap == null)
      throw new IllegalStateException(
          "namespace `" + getNamespace() + "' is not target of action");

    return actionMap;
  }

  public Map<String, String[]> getRenderParameterMap()
  {
    return getInvocation().getParameterMap();
  }

  public Map<String, String[]> getRenderParameterMap(String namespace)
  {
    return getInvocation(namespace).getParameterMap();
  }

  private void checkNullName(String name)
  {
    if (name == null)
      throw new IllegalArgumentException("parameter name cannot be null");
  }

  private void checkNullValue(Object value)
  {
    if (value == null)
      throw new IllegalArgumentException("parameter value cannot be null");
  }

  protected String getParameter(Map<String, String[]> map, String name)
  {
    checkNullName(name);

    String values[] = map.get(name);

    return values == null || values.length == 0 ? null : values[0];
  }

  protected String[] getParameterValues( Map<String, String[]> map, 
                                         String name)
  {
    checkNullName(name);
    return map.get(name);
  }

  protected Enumeration getParameterNames(Map<String, String[]> map)
  {
    return Collections.enumeration(map.keySet());
  }

  protected void setParameter( Map<String, String[]> map, 
                               String name, 
                               String value)
  {
    checkNullName(name);
    checkNullValue(value);

    map.put(name, new String[] { value });
  }

  protected void setParameters( Map<String, String[]> destMap, 
                                Map<String, String[]> srcMap)
  {
    checkNullValue(srcMap);
    destMap.clear();

    Iterator<Map.Entry<String, String[]>> iter 
      = srcMap.entrySet().iterator();

    while (iter.hasNext()) 
    {
      Map.Entry<String, String[]> entry = iter.next();

      setParameter(destMap, entry.getKey(), entry.getValue());
    }
  }

  protected void setParameter( Map<String, String[]> map, 
                               String name, 
                               String[] values)
  {
    checkNullName(name);
    checkNullValue(values);

    if (values.length == 0)
      map.remove(name);
    else
      map.put(name, values);
  }


  public String getActionParameter(String name)
  {
    return getParameter(getActionParameterMap(), name);
  }

  public String[] getActionParameterValues(String name)
  {
    return getParameterValues(getActionParameterMap(), name);
  }

  public Enumeration getActionParameterNames()
  {
    return getParameterNames(getActionParameterMap());
  }


  public String getRenderParameter(String name)
  {
    return getParameter(getRenderParameterMap(), name);
  }

  public String[] getRenderParameterValues(String name)
  {
    return getParameterValues(getRenderParameterMap(), name);
  }

  public Enumeration getRenderParameterNames()
  {
    return getParameterNames(getRenderParameterMap());
  }

  public void setRenderParameters(Map<String, String[]> srcMap)
  {
    forbidRedirectIfInActionStage();

    setParameters(getRenderParameterMap(), srcMap);
  }

  public void setRenderParameter(String name, String value)
  {
    forbidRedirectIfInActionStage();

    setParameter(getRenderParameterMap(), name, value);
  }

  public void setRenderParameter(String name, String[] values)
  {
    forbidRedirectIfInActionStage();

    setParameter(getRenderParameterMap(), name, values);
  }



  public String getRenderParameter(String namespace, String name)
  {
    return getParameter(getRenderParameterMap(namespace), name);
  }

  public String[] getRenderParameterValues(String namespace, String name)
  {
    return getParameterValues(getRenderParameterMap(namespace), name);
  }

  public Enumeration getRenderParameterNames(String namespace)
  {
    return getParameterNames(getRenderParameterMap(namespace));
  }

  public void setRenderParameters(String namespace, Map<String,String[]> srcMap)
  {
    checkWindowMutable(namespace);

    forbidRedirectIfInActionStage();

    setParameters(getRenderParameterMap(namespace), srcMap);
  }

  public void setRenderParameter(String namespace, String name, String value)
  {
    checkWindowMutable(namespace);

    forbidRedirectIfInActionStage();

    setParameter(getRenderParameterMap(namespace), name, value);
  }

  public void setRenderParameter(String namespace, String name, String[] values)
  {
    checkWindowMutable(namespace);

    forbidRedirectIfInActionStage();

    setParameter(getRenderParameterMap(namespace), name, values);
  }


  // -- Invocation - urls

  protected PortalURL createURL(InvocationURL url)
  {
    return new PortalURL(this, url);
  }

  protected PortalURL createRenderURL( Invocation invocation, 
                                       boolean keepParameters )
  {
    PortalURL url =  createURL(invocation.createRenderURL());

    if (keepParameters)
      url.setParameters(invocation.getParameterMap());

    return url;
  }

  protected PortalURL createActionURL( Invocation invocation,
                                       boolean keepParameters )
  {
    PortalURL url =  createURL(invocation.createActionURL());

    if (keepParameters)
      url.setParameters(invocation.getParameterMap());

    return url;
  }

  public PortalURL createRenderURL()
  {
    return createRenderURL(getInvocation(), false);
  }

  public PortalURL createActionURL()
  {
    return createActionURL(getInvocation(), false);
  }

  public PortalURL createRenderURL(String namespace, boolean keepParameters)
  {
    return createRenderURL(getInvocation(namespace), keepParameters);
  }

  public PortalURL createActionURL(String namespace, boolean keepParameters)
  {
    return createActionURL(getInvocation(namespace), keepParameters);
  }

  // -- Preferences

  public PortletPreferences getPreferences()
  {
    if (_windowContext.getPreferences() == null) {
      PreferencesStore store = getPreferencesStore();

      Window window = getWindow();

      PortletPreferences defaultPreferences
        = window == null 
          ? null 
          : window.getDefaultPreferences();

      ArrayList<PreferencesValidator> validators 
        = window == null 
          ? null 
          : window.getPreferencesValidators();

      try {
        Map<String, String[]> storeMap =
          store.getPreferencesMap(getPortletRequest(), getNamespace());

        // XXX: pool these
        LinkingPortletPreferences pref = new LinkingPortletPreferences();
        pref.start(defaultPreferences, validators, storeMap);

        _windowContext.setPreferences(pref);
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    return _windowContext.getPreferences();
  }

  // -- User Attributes

  public Map<String, String> getUserAttributeMap()
    throws IOException
  {
    if (_windowContext.getUserAttributeMap() == null) {
      UserAttributeStore store = getUserAttributeStore();

      Set<String> names = getPortal().getUserAttributeNames();

      Map<String, String> userAttributeMap
        = store.getUserAttributeMap(getPortletRequest(), names);

      _windowContext.setUserAttributeMap(userAttributeMap);
    }

    return _windowContext.getUserAttributeMap();
  }


  // Title

  public void setTitle(String title)
  {
    _connection.setAttribute("javax.portlet.title", title);
  }


  // -- Client information - character encoding, content type, locales

  // if the value has not been set for the connection,
  // the possibilities are defined by the client, and in the case
  // of locale and content type the capabilities defined by the
  // Window.
  //
  // Once the value is set for the conneciton, it is the only 
  // possibility.  


  /**
   * Get the locale already established or get the most preferred locale.
   */
  public Locale getResponseLocale()
  {
    // getResponseLocalesSet() updates the windowContext.getResponseLocale() with the
    // first entry

    getResponseLocalesSet();
    return _windowContext.getResponseLocale();
  }

  /**
   * True if the set is null or the set contains the locale, or the locale
   * without the variant, or the locale without the variant and country, or
   * LOCALE_ANY.
   */ 
  private boolean containsLocale(Set<Locale> set, Locale locale)
  {
    if (set == null || set.contains(locale))
      return true;
    
    String language = locale.getLanguage();
    String country = locale.getCountry();
    String variant = locale.getVariant();

    if (variant.length() > 0) {
      variant = "";
      Locale loc = new Locale(language, country, variant);
      if (set.contains(loc))
        return true;
    }

    if (country.length() > 0) {
      country = "";
      Locale loc = new Locale(language, country, variant);
      if (set.contains(loc))
        return true;
    }

    if (language.length() > 0) {
      Locale loc = LOCALE_ANY;
      if (set.contains(loc))
        return true;
    }

    return false;
  }

  /**
   * Get an ordered set containing all possible locales, the most preferred
   * Locale occurring first.  If the locale is already established a Set
   * containing only the established locale is returned, if the established
   * locale is not one of the supported locales specified in the configuration
   * an empty set is returned.
   */
  public Set<Locale> getResponseLocalesSet()
  {
    Set<Locale> responseLocales = _windowContext.getResponseLocales();

    Locale established = _windowContext.getResponseHandler().getLocale();

    // if it is established, make sure the set contains only
    // the established content type. Otherwise the set will
    // need to be rebuilt.
    
    if (established != null
        && responseLocales != null
        && (responseLocales.size() > 0)
        && !responseLocales.contains(established))

      responseLocales = null;

    if (responseLocales != null)
      return responseLocales;

    responseLocales = new LinkedHashSet<Locale>();

    Locale responseLocale = null; // first one added to Set

    Window window = getWindow();

    Set<Locale> configLocales = window == null 
                                ? null 
                                : window.getSupportedLocales();

    Set<Locale> clientLocales = _connection.getClientLocales();

    boolean configSupportsAll = configLocales == null 
                                || configLocales.isEmpty() 
                                || configLocales.contains(LOCALE_ANY);

    boolean clientSupportsAll = clientLocales == null 
                                || clientLocales.isEmpty() 
                                || clientLocales.contains(LOCALE_ANY);


    if (established != null)
    {
      boolean configSupports 
        = configSupportsAll || containsLocale( configLocales, established );

      boolean clientSupports 
        = clientSupportsAll || containsLocale(clientLocales, established);

      if (configSupports || clientSupports)
      {
        responseLocales.add(established);
        responseLocale = established;
      }
    }
    else if (configLocales == null)  {
      Iterator<Locale> iter = clientLocales.iterator();

      while (iter.hasNext()) {
        Locale clientLocale = iter.next();

        if (clientLocale.equals(LOCALE_ANY))
          continue;

        if (responseLocale == null)
          responseLocale = clientLocale;

        responseLocales.add(clientLocale);
      }
    }
    else {
      Iterator<Locale> iter = configLocales.iterator();

      while (iter.hasNext()) {
        Locale configLocale = iter.next();

        if (configLocale.equals(LOCALE_ANY))
          continue;

        if (clientSupportsAll || clientLocales.contains(configLocale)) {
          responseLocales.add(configLocale);

          if (responseLocale == null)
            responseLocale = configLocale;
        }
      }
    }

    // XXX: default currently is platform default

    if (responseLocale == null && established == null) {
      responseLocale = Locale.getDefault();
      responseLocales.add(responseLocale);
    }

    _windowContext.setResponseLocale(responseLocale);
    _windowContext.setResponseLocales(responseLocales);

    return responseLocales;
  }
  

  /**
   * Get the character encoding already established or get the most preferred
   * character encoding.
   */
  public String getResponseCharacterEncoding()
  {
    // getResponseCharacterEncodingsSet() updates the
    // windowContext.getResponseCharacterEncoding() with the first entry

    getResponseCharacterEncodingsSet();
    return _windowContext.getResponseCharacterEncoding();
  }

  /**
   * Get an ordered set containing all possible character encodings, the most
   * preferred Locale occurring first.  If the character encoding is already
   * established a Set containing only the established character encoding is
   * returned, if the established character encoding is not one of the
   * supported character encodings specified in the configuration an empty set
   * is returned.
   */
  public Set<String> getResponseCharacterEncodingsSet()
  {
    Set<String> responseEncodings 
      = _windowContext.getResponseCharacterEncodings();

    String establishedEncoding 
      = _windowContext.getResponseHandler().getCharacterEncoding();

    // if it is established, make sure the set contains only
    // the established character encoding. Otherwise the set will
    // need to be rebuilt.
    
    if (establishedEncoding != null
        && responseEncodings != null
        && (responseEncodings.size() > 0 )
        && !responseEncodings.contains(establishedEncoding))

      responseEncodings = null;


    if (responseEncodings != null)
      return responseEncodings;

    responseEncodings = new LinkedHashSet<String>();

    String responseEncoding = null; // first one added to Set

    Set<String> clientEncodings = _connection.getClientCharacterEncodings();

    boolean clientSupportsAll = clientEncodings == null 
                                || clientEncodings.isEmpty() 
                                || clientEncodings.contains("*");
  
  
    if (establishedEncoding != null)
    {
      if (clientSupportsAll || clientEncodings.contains(establishedEncoding)) 
      {
        responseEncodings.add(establishedEncoding);
        responseEncoding = establishedEncoding;
      }
    }
    else {
      Iterator<String> iter = clientEncodings.iterator();

      while (iter.hasNext()) {
        String clientEncoding = iter.next();

        responseEncodings.add(clientEncoding);

        if (responseEncoding == null)
          responseEncoding = clientEncoding;
      }

      if (responseEncoding == null) {
        // XXX: default is platform default
        responseEncoding = System.getProperty("file.encoding");
        responseEncodings.add(responseEncoding);
      }
    }

    _windowContext.setResponseCharacterEncoding(responseEncoding);
    _windowContext.setResponseCharacterEncodings(responseEncodings);

    return responseEncodings;
  }
  
  private String getWildcardContentType(String contentType)
  {
    // i.e "text/html" becomes "text/*", null if no wildcard possible
    // because there is no '/' 

    int i = contentType.indexOf('/');
    if (i < 0)
      return null;
    else 
        return contentType.substring(0,i + 1) + "*";
  }

  public String getResponseContentType()
  {
    // getResponseContentTypesSet() sets _windowContext.getResponseContentType
    // to the value of the first entry it add's to the set
    getResponseContentTypesSet();

    return _windowContext.getResponseContentType();
  }

  public Set<String> getResponseContentTypesSet()
  {
    Set<String> responseTypes = _windowContext.getResponseContentTypes();

    String establishedType = _windowContext.getResponseHandler().getContentType();

    // if it is established, make sure the set contains only
    // the established content type. Otherwise the set will
    // need to be rebuilt.
  
    if (establishedType != null
        && responseTypes != null
        && (responseTypes.size() > 0)
        && !responseTypes.contains(establishedType))

      responseTypes = null;


    if (responseTypes != null)
      return responseTypes;

    responseTypes = new LinkedHashSet<String>();

    String responseType = null; // first one added to Set

    Window window = getWindow();

    Set<String> configTypes = 
      window == null 
      ? null
      : window.getSupportedContentTypes(getPortletMode());
        
    boolean configSupportsAll 
      = configTypes == null || configTypes.contains("*/*");

    Set<String> clientTypes = _connection.getClientContentTypes();

    boolean clientSupportsAll = clientTypes == null 
                                || clientTypes.isEmpty() 
                                || clientTypes.contains("*/*");


    if (establishedType != null)
    {
      String wildcard = getWildcardContentType(establishedType);

      if ((configSupportsAll 
            || configTypes.contains(establishedType)
            || configTypes.contains(wildcard))
          &&
          (clientSupportsAll 
            || clientTypes.contains(establishedType)
            || clientTypes.contains(wildcard)))
      {
        responseTypes.add(establishedType);
        responseType = establishedType;
      }
    }
    else if (configSupportsAll)  {
      Iterator<String> iter = clientTypes.iterator();

      while (iter.hasNext()) {
        String clientType = iter.next();

        if (responseType == null)
          responseType = clientType;

        responseTypes.add(clientType);
      }
    }
    else {

      Iterator<String> iter = configTypes.iterator();

      // wildcards in the config are added a second time around, so that they
      // appear further down the list (they are less desirable as a return
      // value)

      boolean configHasWildcard = false;

      while (iter.hasNext()) {
        String configType = iter.next();

        boolean isUsableConfigType = false;

        if (configType.indexOf('*') > -1) {
          configHasWildcard = true;
        }
        else if (clientSupportsAll || clientTypes.contains(configType)) {
          isUsableConfigType = true;
        }
        else {
          String wildcardConfigType = getWildcardContentType(configType);

          if (wildcardConfigType != null 
              && clientTypes.contains(wildcardConfigType)) 
          {
            isUsableConfigType = true;
          }
        }

        if (isUsableConfigType) {
          responseTypes.add(configType);

          if (responseType == null)
            responseType = configType;
        }
      }

      if (configHasWildcard) {
      
        iter = clientTypes.iterator();

        while (iter.hasNext()) {
          String clientType = iter.next();

          boolean isUsableClientType = false;

          if (configSupportsAll)
            isUsableClientType = true;
          else {
            String wildcardClientType = clientType.indexOf('*') > -1
                                        ? clientType
                                        : getWildcardContentType(clientType);

            if (wildcardClientType != null 
              && configTypes.contains(wildcardClientType)) 
            {
              isUsableClientType = true;
            }
          }


          if (isUsableClientType) {
            responseTypes.add(clientType);

            if (responseType == null)
              responseType = clientType;
          }
        }
      }
    }

    _windowContext.setResponseContentType(responseType);
    _windowContext.setResponseContentTypes(responseTypes);

    return responseTypes;
  }

  // -- ResponseHandler

  public String getContentType()
  {
    return _windowContext.getResponseHandler().getContentType();
  }

  public void setContentType(String contentType)
  {
    if (contentType.equals(_windowContext.getResponseHandler().getContentType()))
       return;

    // make sure the content type is allowed before
    // allowing it to be set

    PortletMode currentMode = getPortletMode();

    Window window = _windowContext.getWindow();

    Set<String> configTypes  = null;

    if (window != null)
        configTypes = window.getSupportedContentTypes(currentMode);

    if (configTypes != null && !configTypes.contains(contentType))
      throw new IllegalArgumentException(
          "portlet with namespace `" + _windowContext.getNamespace ()
          + "' does not support content type `" + contentType + "'"
          + " when in mode `" + currentMode + "'");

    _windowContext.getResponseHandler().setContentType(contentType);
  }

  public Locale getLocale()
  {
    return _windowContext.getResponseHandler().getLocale();
  }

  public void addProperty(String key, String value)
  {
    _windowContext.getResponseHandler().addProperty(key, value);
  }

  public void setProperty(String key, String value)
  {
    _windowContext.getResponseHandler().setProperty(key,value);
  }

  public String getCharacterEncoding()
  {
    return _windowContext.getResponseHandler().getCharacterEncoding();
  }

  public PrintWriter getWriter() 
    throws IOException
  {
    return _windowContext.getResponseHandler().getWriter();
  }

  public void setBufferSize(int size)
  {
    _windowContext.getResponseHandler().setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _windowContext.getResponseHandler().getBufferSize();
  }

  /**
   * @param flushToClient true flush all buffers through to the client.
   * If false, flush only the buffers for the current window
   */
  public void flushBuffer(boolean flushToClient) 
    throws IOException
  {
    ResponseHandler parentResponseHandler 
      = _windowContext.getParentResponseHandler();

    ResponseHandler responseHandler 
      = _windowContext.getResponseHandler();

    while (responseHandler != null) {
      if (!flushToClient && responseHandler == parentResponseHandler)
        break;

      responseHandler.flushBuffer();

      responseHandler = responseHandler.getSuccessor();
    }
  }

  /**
   * @param resetRenderer if true, reset all of the buffers for the current
   * window.  If false, reset all of the buffers until the
   * RendererResponseHandler, reset that too but in such a way
   * that it will write the decorations again.
   */
  public void reset(boolean resetRenderer)
  {
    ResponseHandler parentResponseHandler 
      = _windowContext.getParentResponseHandler();

    ResponseHandler responseHandler = _windowContext.getResponseHandler();

    RendererResponseHandler renderer = null;
    boolean isWriter = false;
    boolean isOutputStream = false;

    while (responseHandler != null) {
      if (responseHandler == parentResponseHandler)
        break;

      if (!resetRenderer && (responseHandler instanceof RendererResponseHandler)) 
      {
        renderer = ( (RendererResponseHandler) responseHandler );
        isWriter = renderer.isWriter();
        isOutputStream = renderer.isOutputStream();
      }

      responseHandler.reset();

      responseHandler = responseHandler.getSuccessor();
    }

    if (renderer != null) {
      if (isWriter) {
        try {
          renderer.getWriter();
        }
        catch (IOException ex) {
          setConnectionFailed(ex);
        }
      }

      if (isOutputStream) {
        try {
          renderer.getOutputStream();
        }
        catch (IOException ex) {
          setConnectionFailed(ex);
        }
      }
    }
  }

  /**
   * If the current window has a renderer that is always-write or
   * always-stream, call getWriter() or getOutputStream()
   */
  protected void checkAlwaysWriteOrStream()
    throws IOException
  {
    if (_windowContext == null)
      return;

    ResponseHandler parentResponseHandler 
      = _windowContext.getParentResponseHandler();

    ResponseHandler responseHandler = _windowContext.getResponseHandler();
    ResponseHandler next = responseHandler;

    while (next != null && next != parentResponseHandler) {
      if (responseHandler instanceof RendererResponseHandler) 
      {
        RendererResponseHandler renderer 
          = ( (RendererResponseHandler) next );
        boolean isWriter = renderer.isWriter();
        boolean isOutputStream = renderer.isOutputStream();

        if (!isWriter && !isOutputStream) {

          if (renderer.isAlwaysWrite()) {
            if (getRenderResponse().getContentType() == null) {
              String contentType = renderer.getDefaultContentType();
              getRenderResponse().setContentType(contentType);
            }

            responseHandler.getWriter();
          }
          else if (renderer.isAlwaysStream()) {
            if (getRenderResponse().getContentType() == null) {
              String contentType = renderer.getDefaultContentType();
              getRenderResponse().setContentType(contentType);
            }

            responseHandler.getOutputStream();
          }
        }

        next = null;
      }
      else
        next = next.getSuccessor();
    }
  }

  public void resetBuffer(boolean resetRenderer)
  {
    ResponseHandler parentResponseHandler 
      = _windowContext.getParentResponseHandler();

    ResponseHandler responseHandler 
      = _windowContext.getResponseHandler();

    while (responseHandler != null) {

      if (responseHandler == parentResponseHandler)
        break;

      if (!resetRenderer && (responseHandler instanceof RendererResponseHandler))
        break;

      responseHandler.resetBuffer();

      responseHandler = responseHandler.getSuccessor();
    }
  }

  public boolean isCommitted()
  {
    return _windowContext.getResponseHandler().isCommitted();
  }

  public OutputStream getPortletOutputStream() 
    throws IOException
  {
    return _windowContext.getResponseHandler().getOutputStream();
  }

  // -- Security and user identity

  public boolean isUserInRole(String role)
  {
    Window window = getWindow();

    Map<String, String> roleRefMap 
      = window == null ? null : window.getRoleRefMap();

    if (roleRefMap != null) {
      String effectiveRole = roleRefMap.get(role);

      if (effectiveRole != null)
        role = effectiveRole;
    }

    return _connection.isUserInRole(role);
  }

  public String getRemoteUser()
  {
    return _connection.getRemoteUser();
  }

  public java.security.Principal getUserPrincipal()
  {
    return _connection.getUserPrincipal();
  }

  public String getAuthType()
  {
    return _connection.getAuthType();
  }

  public boolean isSecure()
  {
    return _connection.isSecure();
  }

  public String getRequestedSessionId()
  {
    return _connection.getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return _connection.isRequestedSessionIdValid();
  }

  public boolean canGuaranteeIntegrity()
  {
    return _connection.canGuaranteeIntegrity();
  }

  public boolean canGuaranteeConfidentiality()
  {
    return _connection.canGuaranteeConfidentiality();
  }

  // -- Connection - request info


  public String getContextPath()
  {
    return _connection.getContextPath();
  }

  public String getServerName()
  {
    return _connection.getServerName();
  }

  public int getServerPort()
  {
    return _connection.getServerPort();
  }

  public String getScheme()
  {
    return _connection.getScheme();
  }


  public String getProperty(String name)
  {
    return _connection.getProperty(name);
  }

  public Enumeration getProperties(String name)
  {
    return _connection.getProperties(name);
  }

  public Enumeration getPropertyNames()
  {
    return _connection.getPropertyNames();
  }



  // -- Connection - attributes


  public Enumeration getAttributeNames()
  {
    return _connection.getAttributeNames();
  }

  public Object getAttribute(String name)
  {
     if (name.equals(PortletRequest.USER_INFO)) {
       try {
         return getUserAttributeMap();
       }
       catch (IOException ex) {
         throw new RuntimeException(ex);
       }
     } else
       return _connection.getAttribute(name);
  }

  public void setAttribute(String name, Object o)
  {
    _connection.setAttribute(name, o);
  }

  public void removeAttribute(String name)
  {
    _connection.removeAttribute(name);
  }

  public PortletSession getPortletSession()
  {
    return getPortletSession(false);
  }

  public PortletSession getPortletSession(boolean create)
  {
    return _connection.getPortletSession(create);
  }

  // -- Connection - Submit - Reader/InputStream

  public InputStream getSubmitInputStream() 
    throws IOException
  {
    return _connection.getSubmitInputStream();
  }

  public void setSubmitCharacterEncoding(String enc) 
    throws UnsupportedEncodingException
  {
    _connection.setSubmitCharacterEncoding(enc);
  }

  public String getSubmitCharacterEncoding()
  {
    return _connection.getSubmitCharacterEncoding();
  }

  public BufferedReader getSubmitReader()
    throws UnsupportedEncodingException, IOException
  {
    return _connection.getSubmitReader();
  }

  public String getSubmitContentType()
  {
    return _connection.getSubmitContentType();
  }

  public int getSubmitContentLength()
  {
    return _connection.getSubmitContentLength();
  }

  // Connection - urls and redirect

  public String resolveURL(String path)
  {
    return _connection.resolveURL(path);
  }

  public String resolveURL(String path, boolean isSecure)
    throws PortletSecurityException
  {
    return _connection.resolveURL(path, isSecure);
  }

  public String encodeURL(String path)
  {
    return _connection.encodeURL(path);
  }

  public void sendRedirect(String location)
    throws IOException
  {
    if (isForbidRedirect())
      throw new IllegalStateException(
          "sendRedirect() forbidden, portlet mode, window state, "
          + " or render parameters were set in action");

    _connection.sendRedirect(location);
  }
}

