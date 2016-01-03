/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.cache.HttpCacheBase;
import com.caucho.v5.http.dispatch.FilterChainBuilder;
import com.caucho.v5.http.dispatch.FilterChainError;
import com.caucho.v5.http.dispatch.FilterChainException;
import com.caucho.v5.http.dispatch.FilterChainRedirect;
import com.caucho.v5.http.dispatch.FilterConfigImpl;
import com.caucho.v5.http.dispatch.FilterManager;
import com.caucho.v5.http.dispatch.FilterMapper;
import com.caucho.v5.http.dispatch.InvocationDecoder;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.http.dispatch.ServletManager;
import com.caucho.v5.http.dispatch.ServletMapper;
import com.caucho.v5.http.dispatch.ServletMapping;
import com.caucho.v5.http.dispatch.SubInvocation;
import com.caucho.v5.http.rewrite.WelcomeFile;
import com.caucho.v5.inject.InjectManager;
import com.caucho.v5.io.AlwaysModified;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;

/**
 * servlet/filter dispatching.
 */
public class WebAppDispatch
{
  private static final Logger log
    = Logger.getLogger(WebAppDispatch.class.getName());
  private static final L10N L = new L10N(WebAppDispatch.class);
  
  private final WebAppBuilder _builder;

  // The servlet manager
  private ServletManager _servletManager;
  // The servlet mapper
  private ServletMapper _servletMapper;
  // True if requestDispatcher forward is allowed after buffers flush
  private boolean _isAllowForwardAfterFlush = false;

  // The filter manager
  private FilterManager _filterManager;
  // The filter mapper
  private FilterMapper _filterMapper;
  // The filter mapper
  private FilterMapper _loginFilterMapper;
  // The dispatch filter mapper
  private FilterMapper _dispatchFilterMapper;
  // The include filter mapper
  private FilterMapper _includeFilterMapper;
  // The forward filter mapper
  private FilterMapper _forwardFilterMapper;
  // The error filter mapper
  private FilterMapper _errorFilterMapper;
  // True if includes are allowed to wrap a filter (forbidden by servlet spec)
  private boolean _dispatchWrapsFilters;

  // The cache
  private HttpCacheBase _httpCache;

  private FilterChainBuilder _securityBuilder;

  private WebApp _oldWebApp;
  private long _oldWebAppExpireTime;

  private LruCache<String,RequestDispatcherImpl> _dispatcherCache;

  private LruCache<String,FilterChainEntry> _filterChainCache
    = new LruCache<>(256);
  private WelcomeFile _welcomeFile;

  public WebAppDispatch(WebAppBuilder builder)
  {
    Objects.requireNonNull(builder);
    
    _builder = builder;
    
    _servletManager = builder.getServletManager();
    _servletMapper = builder.getServletMapper();
    
    _filterManager = builder.getFilterManager();
    _filterMapper = builder.getFilterMapper();
    _loginFilterMapper = builder.getFilterMapperLogin();
    _includeFilterMapper = builder.getFilterMapperInclude();
    _forwardFilterMapper = builder.getFilterMapperForward();
    _dispatchFilterMapper = builder.getFilterMapperDispatch();
    _errorFilterMapper = builder.getFilterMapperError();

    _httpCache = builder.getHttpContainer().getHttpCache();
    
    _securityBuilder = builder.getSecurityBuilder();
  }
  
  private boolean isEnabled()
  {
    return getWebApp().isEnabled();
  }
  
  private WebApp getWebApp()
  {
    return _builder.getWebApp();
  }
  
  private WebAppBuilder getBuilder()
  {
    return _builder;
  }
  
  private String getContextPath()
  {
    return getWebApp().getContextPath();
  }

  private WebAppController getController()
  {
    return getWebApp().getController();
  }

  private WebAppContainer getContainer()
  {
    return getWebApp().getContainer();
  }
  
  private ClassLoader getClassLoader()
  {
    return getWebApp().getClassLoader();
  }
  
  private Throwable getConfigException()
  {
    return getWebApp().getConfigException();
  }

  /**
   * Sets the old version web-app.
   */
  public void setOldWebApp(WebApp oldWebApp, long expireTime)
  {
    _oldWebApp = oldWebApp;
    _oldWebAppExpireTime = expireTime;
  }

  /**
   * Returns the best matching servlet pattern.
   */
  public ArrayList<String> getServletMappingPatterns()
  {
    return _servletMapper.getURLPatterns();
  }

  public ServletMapper getServletMapper()
  {
    return _servletMapper;
  }
  
  public ServletManager getServletManager()
  {
    return _servletManager;
  }

  /**
   * Returns the best matching servlet pattern.
   */
  public ArrayList<String> getServletIgnoreMappingPatterns()
  {
    return _servletMapper.getIgnorePatterns();
  }
  
  public void init()
    throws ServletException
  {
    _welcomeFile = _builder.getWelcomeFile();

    _servletManager.init();
    
    _filterManager.init();
  }

  /**
   * creates the servlet/filter dispatch chain.
   */
  public InvocationServlet buildInvocation(InvocationServlet invocation)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    thread.setContextClassLoader(getClassLoader());
    try {
      FilterChain chain = null;

      if (getConfigException() != null) {
        chain = new FilterChainException(getConfigException());
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());

        return invocation;
      }
      else if (! getWebApp().isEnabled()) {
        if (log.isLoggable(Level.FINE))
          log.fine("disabled '" + invocation.getRawURI() + "' (" + getWebApp() + ")");
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        chain = new FilterChainError(code);
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());

        return invocation;
      }
      else if (! getWebApp().waitForActive()) {
        if (log.isLoggable(Level.FINE))
          log.fine("status 503 busy waiting for active for '" + invocation.getRawURI() + "'"
                   + " (" + getWebApp() + ")");
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        chain = new FilterChainError(code);
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());

        return invocation;
      }
      else {
        FilterChainEntry entry = null;

        // jsp/1910 - can't cache jsp_precompile
        String query = invocation.getQueryString();

        boolean isCache = true;
        if (query != null && query.indexOf("jsp_precompile") >= 0)
          isCache = false;
        /* XXX:
        else if (_requestRewriteDispatch != null)
          isCache = false;
          */

        if (isCache) {
          entry = _filterChainCache.get(invocation.getContextURI());
        }

        if (entry != null && ! entry.isModified()) {
          chain = entry.getFilterChain();
          invocation.setServletName(entry.getServletName());

          if (! entry.isAsyncSupported())
            invocation.clearAsyncSupported();

          invocation.setMultipartConfig(entry.getMultipartConfig());
        } 
        else {
          chain = _servletMapper.mapServlet(invocation);
          
          // server/13s[o-r]
          _filterMapper.buildDispatchChain(invocation, chain);
          chain = invocation.getFilterChain();

          chain = applyWelcomeFile(DispatcherType.REQUEST, invocation, chain);

          chain = applyRewrite(DispatcherType.REQUEST, invocation, chain);

                    /*
          // server/13s[o-r]
          // _filterMapper.buildDispatchChain(invocation, chain);
          chain = invocation.getFilterChain();
           */
          
          entry = new FilterChainEntry(chain, invocation);
          chain = entry.getFilterChain();

          if (isCache) {
            _filterChainCache.put(invocation.getContextURI(), entry);
          }
        }
        
        chain = buildSecurity(chain, invocation);

        chain = createWebAppFilterChain(chain, invocation, true);

        invocation.setFilterChain(chain);
        invocation.setPathInfo(entry.getPathInfo());
        invocation.setServletPath(entry.getServletPath());
      }

      return invocation;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      FilterChain chain = new FilterChainException(e);
      chain = new FilterChainWebApp(chain, getWebApp());
      invocation.setDependency(AlwaysModified.create());
      invocation.setFilterChain(chain);

      return invocation;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Fills the invocation for subrequests.
   */
  public void buildDispatchInvocation(InvocationServlet invocation,
                                      FilterMapper filterMapper)
    throws ServletException
  {
    invocation.setWebApp(getWebApp());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    thread.setContextClassLoader(getClassLoader());
    try {
      FilterChain chain;

      if (getConfigException() != null) {
        chain = new FilterChainException(getConfigException());
        invocation.setDependency(AlwaysModified.create());
      }
      else if (! getWebApp().isEnabled()) {
        Exception exn = new UnavailableException(L.l("'{0}' is not currently available.",
                                                     getContextPath()));
        chain = new FilterChainException(exn);
        invocation.setDependency(AlwaysModified.create());
      }
      else if (! getWebApp().waitForActive()) {
        Exception exn = new UnavailableException(L.l("'{0}' is not currently available.",
                                                     getContextPath()));
        chain = new FilterChainException(exn);
        invocation.setDependency(AlwaysModified.create());
      }
      else {
        chain = _servletMapper.mapServlet(invocation);
        chain = filterMapper.buildDispatchChain(invocation, chain);
        
        if (filterMapper == _includeFilterMapper) {
          chain = applyWelcomeFile(DispatcherType.INCLUDE, invocation, chain);
          
          chain = applyRewrite(DispatcherType.INCLUDE, invocation, chain);
        }
        else if (filterMapper == _forwardFilterMapper) {
          chain = applyWelcomeFile(DispatcherType.FORWARD, invocation, chain);
          
          chain = applyRewrite(DispatcherType.FORWARD,
                               invocation,
                               chain);
        }

        /* server/10gw - #3111 */
        /*
        if (getRequestListeners().length > 0)
          chain = new DispatchFilterChain(chain, this); // invocation);
        */

        if (_httpCache != null && filterMapper == _includeFilterMapper) {
          chain = _httpCache.createFilterChain(chain, getWebApp());
        }
      }

      invocation.setFilterChain(chain);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      FilterChain chain = new FilterChainException(e);
      invocation.setFilterChain(chain);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected FilterChain buildRequestFilterChain(InvocationServlet invocation,
                                                FilterChain chain)
  {
    return chain;
  }
  
  private FilterChain applyWelcomeFile(DispatcherType type,
                                       InvocationServlet invocation, 
                                       FilterChain chain)
    throws ServletException
  {
    if ("".equals(invocation.getContextURI())) {
      // server/1u3l
      return new FilterChainRedirect(getContextPath() + "/");
    }

    if (_welcomeFile != null) {
      chain = _welcomeFile.map(type,
                               invocation.getContextURI(),
                               invocation.getQueryString(),
                               chain, chain);
    }
    
    return chain;
  }
  
  FilterChain createWebAppFilterChain(FilterChain chain,
                                      InvocationServlet invocation,
                                      boolean isTop)
  {
    // the cache must be outside of the WebAppFilterChain because
    // the CacheListener in ServletInvocation needs the top to
    // be a CacheListener.  Otherwise, the cache won't get lru.

    boolean hasListeners = getWebApp().getRequestListeners() != null
                           && getWebApp().getRequestListeners().length > 0;

    if (! hasListeners) {
      /*
      EventManager manager = getWebApp().getBeanManager().getEventManager();

      hasListeners = manager.resolveObserverMethods(
        HttpServletRequest.class,
        RequestInitializedLiteral.INSTANCE).size() > 0;

      hasListeners = hasListeners || manager.resolveObserverMethods(
        HttpServletRequest.class,
        RequestDestroyedLiteral.INSTANCE).size() > 0;
        */
    }

    if (hasListeners) {
      chain = new FilterChainWebAppListener(chain, 
                                            getWebApp(), 
                                            getWebApp().getRequestListeners());
    }

    // TCK: cache needs to be outside because the cache flush conflicts
    // with the request listener destroy callback
    // top-level filter elements
    // server/021h - cache not logging

    if (_httpCache != null) {
      chain = _httpCache.createFilterChain(chain, getWebApp());
    }
    
    FilterChainWebApp webAppChain = new FilterChainWebApp(chain, getWebApp());

    // webAppChain.setSecurityRoleMap(invocation.getSecurityRoleMap());
    chain = webAppChain;

    if (getBuilder().isStatisticsEnabled()) {
      chain = new FilterChainStatistics(chain, getWebApp());
    }

    if (getWebApp().getAccessLog() != null && isTop)
      chain = new AccessLogFilterChain(chain, getWebApp());
    
    return chain;
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcherImpl getRequestDispatcher(String url)
  {
    if (url == null)
      throw new IllegalArgumentException(L.l("request dispatcher url can't be null."));
    else if (! url.startsWith("/"))
      throw new IllegalArgumentException(L.l("request dispatcher url '{0}' must be absolute", url));

    RequestDispatcherImpl disp = getDispatcherCache().get(url);

    if (disp != null && ! disp.isModified())
      return disp;

    InvocationServlet includeInvocation = new SubInvocation();
    InvocationServlet forwardInvocation = new SubInvocation();
    InvocationServlet errorInvocation = new SubInvocation();
    InvocationServlet dispatchInvocation = new SubInvocation();
    InvocationServlet requestInvocation = dispatchInvocation;
    
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = escapeURL(getContextPath() + url);

    try {
      decoder.splitQuery(includeInvocation, rawURI);
      decoder.splitQuery(forwardInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);
      decoder.splitQuery(dispatchInvocation, rawURI);

      WebAppContainer container = getContainer();
      
      // server/1h57
      boolean isSameWebApp = false;
      if (container != null) {
        DeployHandle<WebApp> subController
          = container.getWebAppHandle(includeInvocation);

        // server/1233
        container.getWebAppHandle(forwardInvocation);
        container.getWebAppHandle(errorInvocation);
        container.getWebAppHandle(dispatchInvocation);

        /*
        if (subController != null
            && (getController().getContextPath()
                .equals(subController.getContextPath()))) {
          isSameWebApp = true;
        }
        */
        if (subController != null
            && (getController().getId()
                .equals(subController.getId()))) {
          isSameWebApp = true;
        }
      }

      if (container != null && ! isSameWebApp) {
        // jsp/15ll
        container.buildIncludeInvocation(includeInvocation);
        container.buildForwardInvocation(forwardInvocation);
        container.buildErrorInvocation(errorInvocation);
        container.buildDispatchInvocation(dispatchInvocation);
      }
      else if (! getWebApp().waitForActive()) {
        throw new IllegalStateException(L.l("web-app '{0}' is restarting and is not yet ready to receive requests",
                                            getWebApp().getVersionContextPath()));
      }
      else {
        buildIncludeInvocation(includeInvocation);
        buildForwardInvocation(forwardInvocation);
        buildErrorInvocation(errorInvocation);
        buildDispatchInvocation(dispatchInvocation);
      }
      
      // jsp/15ln
      buildCrossContextFilter(includeInvocation);
      buildCrossContextFilter(forwardInvocation);
      buildCrossContextFilter(errorInvocation);
      buildCrossContextFilter(dispatchInvocation);
      
      disp = new RequestDispatcherImpl(includeInvocation,
                                       forwardInvocation,
                                       errorInvocation,
                                       dispatchInvocation,
                                       requestInvocation,
                                       getWebApp());

      getDispatcherCache().put(url, disp);

      return disp;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }
  
  /**
   * Fills the invocation for an include request.
   */
  public void buildIncludeInvocation(InvocationServlet invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _includeFilterMapper);
  }

  /**
   * Fills the invocation for a forward request.
   */
  public void buildForwardInvocation(InvocationServlet invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _forwardFilterMapper);
  }

  /**
   * Fills the invocation for an error request.
   */
  public void buildErrorInvocation(InvocationServlet invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _errorFilterMapper);
  }

  /**
   * Fills the invocation for a login request.
   */
  public void buildLoginInvocation(InvocationServlet invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _loginFilterMapper);
  }

  /**
   * Fills the invocation for a rewrite-dispatch/dispatch request.
   */
  public void buildDispatchInvocation(InvocationServlet invocation)
    throws ServletException
  {
    // buildDispatchInvocation(invocation, _dispatchFilterMapper);
    buildDispatchInvocation(invocation, _filterMapper);
    
    buildSecurity(invocation);
  }
  
  protected FilterChain applyRewrite(DispatcherType type,
                                     InvocationServlet invocation,
                                     FilterChain chain)
  {
    return chain;
  }
  
  private void buildSecurity(InvocationServlet invocation)
  {
    invocation.setFilterChain(buildSecurity(invocation.getFilterChain(), invocation));
  }
  
  private FilterChain buildSecurity(FilterChain chain, InvocationServlet invocation)
  {
    if (_securityBuilder != null) {
      // server/1ksa
      // _dispatchFilterMapper.addTopFilter(_securityBuilder);
      return _securityBuilder.build(chain, invocation);
    }
    
    
    return chain;
  }

  private void buildCrossContextFilter(InvocationServlet invocation)
  {
    FilterChain chain = invocation.getFilterChain();
    
    chain = new FilterChainDispatch(chain, invocation.getWebApp());
    
    invocation.setFilterChain(chain);
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getLoginDispatcher(String url)
  {
    if (url == null)
      throw new IllegalArgumentException(L.l("request dispatcher url can't be null."));
    else if (! url.startsWith("/"))
      throw new IllegalArgumentException(L.l("request dispatcher url '{0}' must be absolute", url));

    InvocationServlet loginInvocation = new InvocationServlet();
    InvocationServlet errorInvocation = new InvocationServlet();
    InvocationDecoder decoder = new InvocationDecoder();
    
    WebAppContainer container = getContainer();

    String rawURI = getContextPath() + url;

    try {
      decoder.splitQuery(loginInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);

      if (! isEnabled()) {
        throw new IllegalStateException(L.l("'{0}' is disable and unavailable to receive requests",
                                            getWebApp().getVersionContextPath()));
      }
      else if (! getWebApp().waitForActive()) {
        throw new IllegalStateException(L.l("'{0}' is restarting and it not yet ready to receive requests",
                                            getWebApp().getVersionContextPath()));
      }
      else if (container != null) {
        container.routeInvocation(loginInvocation);
        container.buildErrorInvocation(errorInvocation);
      }
      else {
        FilterChain chain = _servletMapper.mapServlet(loginInvocation);
        _filterMapper.buildDispatchChain(loginInvocation, chain);

        chain = _servletMapper.mapServlet(errorInvocation);
        _errorFilterMapper.buildDispatchChain(errorInvocation, chain);
      }

      RequestDispatcherImpl disp;
      disp = new RequestDispatcherImpl(loginInvocation,
                                       loginInvocation,
                                       errorInvocation,
                                       loginInvocation,
                                       loginInvocation,
                                       getWebApp());
      disp.setLogin(true);

      return disp;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getNamedDispatcher(String servletName)
  {
    try {
      InvocationServlet invocation = new InvocationServlet();
      invocation.setWebApp(getWebApp());

      FilterChain chain
        = _servletManager.createServletChain(servletName, null, invocation);

      FilterChain includeChain
        = _includeFilterMapper.buildFilterChain(chain, servletName);
      FilterChain forwardChain
        = _forwardFilterMapper.buildFilterChain(chain, servletName);

      return new NamedDispatcherImpl(includeChain, forwardChain,
                                     invocation,
                                     null, getWebApp());
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);

      return null;
    }
  }

  public long getMaxEntrySize()
  {
    return _httpCache.getMaxEntrySize();
  }

  void setAllowForwardAfterFlush(boolean isEnable)
  {
    _isAllowForwardAfterFlush = isEnable;
  }

  public boolean isAllowForwardAfterFlush()
  {
    return _isAllowForwardAfterFlush;
  }

  private LruCache<String,RequestDispatcherImpl> getDispatcherCache()
  {
    LruCache<String,RequestDispatcherImpl> cache = _dispatcherCache;

    if (cache != null)
      return cache;

    synchronized (this) {
      cache = new LruCache<String,RequestDispatcherImpl>(1024);
      _dispatcherCache = cache;
      return cache;
    }
  }

  private String escapeURL(String url)
  {
    return url;

    /* jsp/15dx
       CharBuffer cb = CharBuffer.allocate();

       int length = url.length();
       for (int i = 0; i < length; i++) {
       char ch = url.charAt(i);

       if (ch < 0x80)
       cb.append(ch);
       else if (ch < 0x800) {
       cb.append((char) (0xc0 | (ch >> 6)));
       cb.append((char) (0x80 | (ch & 0x3f)));
       }
       else {
       cb.append((char) (0xe0 | (ch >> 12)));
       cb.append((char) (0x80 | ((ch >> 6) & 0x3f)));
       cb.append((char) (0x80 | (ch & 0x3f)));
       }
       }

       return cb.close();
    */
  }

  //
  // dynamic server/filter
  //

  public ServletRegistration.Dynamic addServlet(String servletName,
                                                String className)
  {
    Class<? extends Servlet> servletClass;
    
    try {
      servletClass = (Class) Class.forName(className, false, getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown class in {1}",
                                             className, this),
                                         e);
    }
    
    return addServletApi(servletName, className, servletClass, null);
  }

  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Class<? extends Servlet> servletClass)
  {
    return addServletApi(servletName, servletClass.getName(), servletClass, null);
  }

  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Servlet servlet)
  {
    Class cl = servlet.getClass();

    return addServletApi(servletName, cl.getName(), cl, servlet);
  }
  
  private ServletRegistration.Dynamic addServletApi(String servletName,
                                                    String servletClassName,
                                                    Class<? extends Servlet> servletClass,
                                                    Servlet servlet)
  {
    if (getServletRegistration(servletName) == null) {
      return addServlet(servletName, servletClassName, servletClass, servlet);
    }
    else {
      return null;
    }
  }

  /**
   * Adds a new or augments existing registration
   *
   * @since 3.0
   */
  private ServletRegistration.Dynamic addServlet(String servletName,
                                                 String servletClassName,
                                                 Class<? extends Servlet> servletClass,
                                                 Servlet servlet)
  {
    if (! getWebApp().isInitializing()) {
      throw new IllegalStateException(L.l("addServlet may only be called during initialization"));
    }

    try {
      ServletBuilder config
      = (ServletBuilder) getServletRegistration(servletName);

      if (config == null) {
        config = getWebApp().getBuilder().createServlet();

        config.setServletName(servletName);
        config.setServletClass(servletClassName);
        config.setServletClass(servletClass);
        config.setServlet(servlet);

        getWebApp().getBuilder().addServlet(config);
      } else {
        if (config.getClassName() == null)
          config.setServletClass(servletClassName);

        if (config.getServletClass() == null)
          config.setServletClass(servletClass);

        if (config.getServlet() == null)
          config.setServlet(servlet);
      }
      
      if (log.isLoggable(Level.FINE)) {
        log.fine(L.l("dynamic servlet added [name: '{0}', class: '{1}'] (in {2})",
                     servletName, servletClassName, this));
      }

      return config;
    }
    catch (ServletException e) {
      //spec declares no throws so far
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public <T extends Servlet> T createServlet(Class<T> servletClass)
    throws ServletException
  {
    //try {
      // return getWebApp().getBeanManager().createTransientObject(servletClass);
      InjectManager inject = InjectManager.current();
      
      return inject.lookup(servletClass);
      /*
    } catch (InjectionException e) {
      throw new ServletException(e);
    }
    */
  }

  public void addServlet(WebServlet webServlet, String servletClassName)
    throws ServletException
  {
    ServletMapping mapping = getWebApp().getBuilder().createServletMapping();
    mapping.setAnnotation(true);
    mapping.setServletClass(servletClassName);

    String name = webServlet.name();

    if (name == null || "".equals(name))
      name = servletClassName;

    mapping.setServletName(name);

    if (webServlet.value().length > 0 && webServlet.urlPatterns().length == 0) {
      for (String url : webServlet.value())
        mapping.addURLPattern(url); // XXX: support addURLRegexp?
    }
    else if (webServlet.urlPatterns().length > 0 &&
             webServlet.value().length == 0) {
      for (String url : webServlet.urlPatterns()) {
        mapping.addURLPattern(url); // XXX: support addURLRegexp?
      }
    }
    /* servlet/1176 (tck)
    else {
      throw new ConfigException(L.l("Annotation @WebServlet at '{0}' must specify either value or urlPatterns", servletClassName));
    }
    */

    for (WebInitParam initParam : webServlet.initParams())
      mapping.setInitParam(initParam.name(), initParam.value()); //omit description

    mapping.setLoadOnStartup(webServlet.loadOnStartup());
    mapping.setAsyncSupported(webServlet.asyncSupported());

    getWebApp().getBuilder().addServletMapping(mapping);
  }

  /**
   * Returns filter registrations
   */
  Map<String, ? extends FilterRegistration> getFilterRegistrations()
  {
    Map<String, FilterConfigImpl> configMap = _filterManager.getFilters();

    Map<String, FilterRegistration> result
      = new HashMap<String, FilterRegistration>(configMap);

    return Collections.unmodifiableMap(result);
  }

  public FilterRegistration getFilterRegistration(String filterName)
  {
    return _filterManager.getFilter(filterName);
  }

  public ServletRegistration getServletRegistration(String servletName)
  {
    return _servletManager.getServlet(servletName);
  }

  public Map<String, ServletRegistration> getServletRegistrations()
  {
    Map<String, ServletBuilder> configMap = _servletManager.getServlets();

    Map<String, ServletRegistration> result
      = new HashMap<String, ServletRegistration>(configMap);

    return Collections.unmodifiableMap(result);
  }

  void addServletSecurity(Class<? extends Servlet> servletClass,
                                  ServletSecurity security)
  {
    ServletSecurityElement securityElement
      = new ServletSecurityElement(security);

    _servletManager.addSecurityElement(servletClass, securityElement);
  }

  public <T extends Filter> T createFilter(Class<T> filterClass)
    throws ServletException
  {
    // return getWebApp().getBeanManager().createTransientObject(filterClass);
    InjectManager inject = InjectManager.current();
      
    return inject.lookup(filterClass);
  }

  public FilterRegistration.Dynamic addFilter(String filterName,
                                              String className)
  {
    return addFilter(filterName, className, null, null);
  }

  public FilterRegistration.Dynamic addFilter(String filterName,
                                              Class<? extends Filter> filterClass)
  {
    return addFilter(filterName, filterClass.getName(), filterClass, null);
  }

  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
  {
    Class<? extends Filter> cl = filter.getClass();

    return addFilter(filterName, cl.getName(), cl, filter);
  }

  private FilterRegistration.Dynamic addFilter(String filterName,
                                               String className,
                                               Class<? extends Filter> filterClass,
                                               Filter filter)
  {
    if (! getWebApp().isInitializing()) {
      throw new IllegalStateException();
    }

    try {
      FilterConfigImpl config = new FilterConfigImpl();

      config.setWebApp(getWebApp());
      config.setServletContext(getWebApp());

      config.setFilterName(filterName);
      config.setFilterClass(className);

      if (filterClass != null)
        config.setFilterClass(filterClass);

      if (filter != null)
        config.setFilter(filter);

      if (getWebApp().getBuilder().getFilter(filterName) == null) {
        getWebApp().getBuilder().addFilter(config);
        
        return config;
      }
      else {
        return null;
      }
    }
    catch (ClassNotFoundException e) {
      e.printStackTrace();
      //spec declares no throws so far.
      throw new RuntimeException(e.getMessage(), e);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public void clearCache()
  {
    // server/1kg1
    synchronized (_filterChainCache) {
      _filterChainCache.clear();
      _dispatcherCache = null;
    }
  }

  public void destroy()
  {
    if (_servletManager != null) {
      _servletManager.destroy();
    }
    
    if (_filterManager != null) {
      _filterManager.destroy();
    }
  }
  
  static class FilterChainEntry {
    FilterChain _filterChain;
    String _pathInfo;
    String _servletPath;
    String _servletName;
    HashMap<String,String> _securityRoleMap;
    final Dependency _dependency;
    boolean _isAsyncSupported;
    MultipartConfigElement _multipartConfig;

    FilterChainEntry(FilterChain filterChain, InvocationServlet invocation)
    {
      _filterChain = filterChain;
      _pathInfo = invocation.getPathInfo();
      _servletPath = invocation.getServletPath();
      _servletName = invocation.getServletName();
      _dependency = invocation.getDependency();
      _isAsyncSupported = invocation.isAsyncSupported();
      _multipartConfig = invocation.getMultipartConfig();
    }

    boolean isModified()
    {
      return _dependency != null && _dependency.isModified();
    }

    FilterChain getFilterChain()
    {
      return _filterChain;
    }

    HashMap<String,String> getSecurityRoleMap()
    {
      return _securityRoleMap;
    }

    void setSecurityRoleMap(HashMap<String,String> roleMap)
    {
      _securityRoleMap = roleMap;
    }

    String getPathInfo()
    {
      return _pathInfo;
    }

    String getServletPath()
    {
      return _servletPath;
    }

    String getServletName()
    {
      return _servletName;
    }

    boolean isAsyncSupported() {
      return _isAsyncSupported;
    }

    public MultipartConfigElement getMultipartConfig()
    {
      return _multipartConfig;
    }
  }
}
