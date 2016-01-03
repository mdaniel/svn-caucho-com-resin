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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.DeployContainerService;
import com.caucho.v5.deploy.DeployContainerServiceImpl;
import com.caucho.v5.deploy.DeployGenerator;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.FilterChainError;
import com.caucho.v5.http.dispatch.FilterChainException;
import com.caucho.v5.http.dispatch.InvocationDecoder;
import com.caucho.v5.http.dispatch.InvocationRouter;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.log.AccessLogBase;
import com.caucho.v5.http.log.AccessLogServlet;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.http.webapp.WebAppRouter.WebAppUriMap;
import com.caucho.v5.io.AlwaysModified;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvLoaderListener;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.vfs.MemoryPath;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;

/**
 * Resin's webApp implementation.
 */
public class WebAppContainer
  implements InvocationRouter<InvocationServlet>, EnvLoaderListener
{
  private static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebAppContainer.class.getName());

  private HttpContainerServlet _httpContainer;
  private Host _host;

  // The context class loader
  private EnvironmentClassLoader _classLoader;
  
  private final Lifecycle _lifecycle;

  // The root directory.
  private PathImpl _rootDir;

  // The document directory.
  private PathImpl _docDir;

  // dispatch mapping
  // private RewriteDispatch _rewriteDispatch;
  
  private WebApp _errorWebApp;

  // List of default service webApp configurations
  private ArrayList<WebAppConfig> _serviceDefaultList
    = new ArrayList<>();

  private DeployContainerService<WebApp,WebAppController> _appDeploy;
  
  private DeployGeneratorWebAppExpand _warGenerator;

  private boolean _hasWarGenerator;

  private static final int URI_CACHE_SIZE = 8192;
  
  // LRU cache for the webApp lookup
  private LruCache<String,WebAppUriMap> _uriToAppCache
    = new LruCache<>(URI_CACHE_SIZE);

  // List of default webApp configurations
  private ArrayList<WebAppConfig> _webAppDefaultList
    = new ArrayList<>();

  private AccessLogBase _accessLog;

  private long _startWaitTime = 10000L;

  private Throwable _configException;
  private Object _startCancel;
  private WebAppRouter _webAppRouter;

  /**
   * Creates the webApp with its environment loader.
   */
  public WebAppContainer(HttpContainerServlet http,
                         Host host,
                         PathImpl rootDirectory,
                         EnvironmentClassLoader loader,
                         Lifecycle lifecycle)
  {
    Objects.requireNonNull(http);
    Objects.requireNonNull(host);
    Objects.requireNonNull(lifecycle);

    _httpContainer = http;
    
    _host = host;
    
    _rootDir = rootDirectory;

    _classLoader = loader;
    
    _lifecycle = lifecycle;
    
    _webAppRouter = new WebAppRouter(http, host);
    
    DeployContainerServiceImpl<WebApp, WebAppController> deployServiceImpl
      = new DeployContainerServiceImpl<>(WebAppController.class);
    
    ServiceManagerAmp ampManager = AmpSystem.getCurrentManager();
    
    _appDeploy = ampManager.service(deployServiceImpl)
                          .start()
                          .as(DeployContainerService.class);
    
    //ServiceRefAmp onStartRef = ampManager.lookup("event:///" + OnWebAppStart.class.getName());
    
    //_startCancel = onStartRef.subscribe((OnWebAppStart) id->onWebAppStart(id));

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(loader);

      initConstructor();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /*
  private void onWebAppStart(String id)
  {
    _appDeploy.update();
    //this.updateWebAppDeploy(id);
  }
  */
  
  protected void initConstructor()
  {
    // _serviceDeploy = new DeployContainerImpl<>(PodAppController.class);
  }

  protected HttpContainerServlet getHttpContainer()
  {
    return _httpContainer;
  }

  public InvocationDecoder getInvocationDecoder()
  {
    return getHttpContainer().getInvocationDecoder();
  }

  /**
   * Gets the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * sets the class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _classLoader = loader;
  }

  /**
   * Returns the owning host.
   */
  public Host getHost()
  {
    return _host;
  }
  
  public String getClusterId()
  {
    return getHttpContainer().getClusterName();
  }

  /**
   * Gets the root directory.
   */
  public PathImpl getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(PathImpl path)
  {
    _rootDir = path;

    Vfs.setPwd(path, getClassLoader());
  }

  /**
   * Gets the document directory.
   */
  public PathImpl getDocumentDirectory()
  {
    if (_docDir != null)
      return _docDir;
    else
      return _rootDir;
  }

  /**
   * Sets the document directory.
   */
  public void setDocumentDirectory(PathImpl path)
  {
    _docDir = path;
  }

  /**
   * Sets the document directory.
   */
  public void setDocDir(PathImpl path)
  {
    setDocumentDirectory(path);
  }

  /**
   * Sets the access log.
   */
  public AccessLogBase createAccessLog()
  {
    if (_accessLog == null)
      _accessLog = new AccessLogServlet();

    return _accessLog;
  }

  /**
   * Sets the access log.
   */
  public void setAccessLog(AccessLogBase log)
  {
    _accessLog = log;

    EnvLoader.setAttribute("caucho.server.access-log", log);
  }

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    getErrorPageManager().addErrorPage(errorPage);
  }

  /**
   * Returns the error page manager
   */
  public ErrorPageManager getErrorPageManager()
  {
    WebApp errorWebApp = getErrorWebApp();
    
    if (errorWebApp != null) {
      return getErrorWebApp().getErrorPageManager();
    }
    else {
      return null;
    }
  }

  /**
   * Sets a configuration exception.
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
  }

  /**
   * Returns the webApp generator
   */
  /*
  public DeployContainerImpl<WebApp,WebAppController> getWebAppGenerator()
  {
    return _appDeploySpi;
  }
  */

  /**
   * Returns the container's session manager.
   */
  public SessionManager getSessionManager()
  {
    return null;
  }

  /**
   * Returns true if modified.
   */
  public boolean isModified()
  {
    return _lifecycle.isDestroyed() || _classLoader.isModified();
  }
  
  /**
   * For QA, returns true if the deployment is marked as modified.
   */
  public boolean isDeployModified()
  {
    return _appDeploy.isModified();
  }

  /**
   * Adds an webApp.
   */
  public void addWebApp(WebAppConfig config)
  {
    if (config.getURLRegexp() != null) {
      DeployGenerator<WebApp,WebAppController> deploy
        = new DeployGeneratorWebAppRegexp(_appDeploy, this, config);
      _appDeploy.add(deploy);
      
      clearCache();
      return;
    }

    // server/10f6
    /*
    WebAppController oldEntry
      = _appDeploy.findController(config.getContextPath());

    if (oldEntry != null && oldEntry.getSourceType().equals("single")) {
      throw new ConfigException(L.l("duplicate web-app '{0}' forbidden.",
                                    config.getId()));
    }
    */

    DeployGeneratorWebAppSingle deployGenerator = createDeployGenerator(config);
      
    addWebApp(deployGenerator);
  }
  
  public DeployGeneratorWebAppSingle createDeployGenerator(WebAppConfig config)
  {
    return new DeployGeneratorWebAppSingle(_appDeploy, this, config);
  }
  
  public void addWebApp(DeployGeneratorWebAppSingle deployGenerator)
  {
    deployGenerator.deploy();
    
    _appDeploy.add(deployGenerator);

    clearCache();
  }
  
  public void removeWebApp(DeployGeneratorWebAppSingle deployGenerator)
  {
    _appDeploy.remove(deployGenerator);
    deployGenerator.destroy();
  }

  /**
   * Adds a web-app default
   */
  public void addWebAppDefault(WebAppConfig init)
  {
    _webAppDefaultList.add(init);
  }

  /**
   * Returns the list of web-app defaults
   */
  public ArrayList<WebAppConfig> getWebAppDefaultList()
  {
    return _webAppDefaultList;
  }

  /**
   * Sets the war-expansion
   */
  public DeployGeneratorWebAppExpand createWarDeploy()
  {
    String cluster = getHttpContainer().getClusterName();
    String host = getHost().getIdTail();
    
    String id = "webapps/" + host;
    
    return new DeployGeneratorWebAppExpand(id, _appDeploy, this);
  }

  /**
   * Sets the war-expansion
   */
  public DeployGeneratorWebAppExpand createWebAppDeploy()
  {
    return createWarDeploy();
  }

  /**
   * Sets the war-expansion
   */
  public void addWebAppDeploy(DeployGeneratorWebAppExpand deploy)
    throws ConfigException
  {
    addWarDeploy(deploy);
  }

  /**
   * Sets the war-expansion
   */
  public void addWarDeploy(DeployGeneratorWebAppExpand webAppDeploy)
    throws ConfigException
  {
    assert webAppDeploy.getContainer() == this;

    if (! _hasWarGenerator) {
      _hasWarGenerator = true;
      _warGenerator = webAppDeploy;
    }

    _appDeploy.add(webAppDeploy);
  }

  /**
   * Sets the war-expansion
   */
  public void addDeploy(DeployGenerator<WebApp,WebAppController> deploy)
    throws ConfigException
  {
    if (deploy instanceof DeployGeneratorWebAppExpand)
      addWebAppDeploy((DeployGeneratorWebAppExpand) deploy);
    else
      _appDeploy.add(deploy);
  }

  /**
   * Removes a web-app-generator.
   */
  public void removeWebAppDeploy(DeployGenerator<WebApp,WebAppController> deploy)
  {
    _appDeploy.remove(deploy);
  }

  /**
   * Updates a WebApp deploy
   */
  public void updateWebAppDeploy(String name)
    throws Throwable
  {
    _appDeploy.update();
    WebAppController controller = _appDeploy.update(name);

    clearCache();

    if (controller != null) {
      Throwable configException = controller.getConfigException();

      if (configException != null)
        throw configException;
    }
  }
  
  //
  // service/baratine

  /**
   * Adds an ear default
   */
  public void addServiceDefault(WebAppConfig config)
  {
    _serviceDefaultList.add(config);
  }

  /**
   * Returns the list of ear defaults
   */
  public ArrayList<WebAppConfig> getServiceDefaultList()
  {
    return _serviceDefaultList;
  }
  
  /**
   * Returns the URL for the container.
   */
  public String getURL()
  {
    return _host.getURL();
  }

  /**
   * Returns the URL for the container.
   */
  public String getId()
  {
    return getURL();
  }

  /**
   * Returns the host name for the container.
   */
  public String getHostName()
  {
    return "";
  }

  // backwards compatibility

  /**
   * Sets the war-dir for backwards compatibility.
   */
  public void setWarDir(PathImpl warDir)
    throws ConfigException
  {
    getWarGenerator().setPath(warDir);

    if (! _hasWarGenerator) {
      _hasWarGenerator = true;
      addWebAppDeploy(getWarGenerator());
    }
  }

  /**
   * Gets the war-dir.
   */
  public PathImpl getWarDir()
  {
    return getWarGenerator().getPath();
  }

  /**
   * Sets the war-expand-dir.
   */
  public void setWarExpandDir(PathImpl warDir)
  {
    getWarGenerator().setExpandDirectory(warDir);
  }

  /**
   * Gets the war-expand-dir.
   */
  public PathImpl getWarExpandDir()
  {
    return getWarGenerator().getExpandDirectory();
  }
  

  private DeployGeneratorWebAppExpand getWarGenerator()
  {
    if (_warGenerator == null) {
      //String id = getClusterId() + "/webapp/" + getHost().getIdTail();
      String id = "webapps/" + getHost().getIdTail();

      _warGenerator = new DeployGeneratorWebAppExpand(id,
                                                      _appDeploy, 
                                                      this);
    }
    
    return _warGenerator;
  }

  /**
   * Starts the container.
   */
  public void start()
  {
    /*
    try {
      _serviceDeploy.start();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    */
    
    _webAppRouter.start();
    
    try {
      _appDeploy.start();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Returns the dependency for the container itself
   * @return
   */
  public Dependency getDependency()
  {
    return _classLoader;
  }

  /**
   * Clears the cache
   */
  public void clearCache()
  {
    _uriToAppCache = new LruCache<String,WebAppUriMap>(URI_CACHE_SIZE);
    
    _httpContainer.clearCache();
  }

  /**
   * Creates the invocation.
   */
  @Override
  public InvocationServlet routeInvocation(InvocationServlet invocation)
    throws ConfigException
  {
    if (_configException != null) {
      FilterChain chain = new FilterChainException(_configException);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());

      return invocation;
    }
    else if (! _lifecycle.waitForActive(_startWaitTime)) {
      log.fine(this + " container is not active");
      
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
      FilterChain chain = new FilterChainError(code);
      invocation.setFilterChain(chain);

      invocation.setWebApp(getErrorWebApp());

      invocation.setDependency(AlwaysModified.create());

      return invocation ;
    }

    FilterChain chain;
    
    //WebAppController controller = getWebAppController(invocation);
    DeployHandle<WebApp> webAppHandle = getWebAppHandle(invocation);
    
    WebApp webApp = getWebApp(invocation, webAppHandle, true);

    boolean isAlwaysModified;

    if (webApp != null) {
      invocation = webApp.routeInvocation(invocation);
      chain = invocation.getFilterChain();
      isAlwaysModified = false;
    }
    else if (webAppHandle != null){
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
      chain = new FilterChainError(code);
      FilterChainContext contextChain = new FilterChainContext(chain);
      contextChain.setErrorPageManager(getErrorPageManager());
      chain = contextChain;
      invocation.setFilterChain(contextChain);
      isAlwaysModified = true;
    }
    else {
      int code = HttpServletResponse.SC_NOT_FOUND;
      chain = new FilterChainError(code);
      FilterChainContext contextChain = new FilterChainContext(chain);
      contextChain.setErrorPageManager(getErrorPageManager());
      chain = contextChain;
      invocation.setFilterChain(contextChain);
      isAlwaysModified = true;
    }

    chain = buildFilterChain(invocation, chain);
    
    invocation.setFilterChain(chain);

    if (isAlwaysModified)
      invocation.setDependency(AlwaysModified.create());

    return invocation;
  }
  
  protected FilterChain buildFilterChain(InvocationServlet invocation,
                                         FilterChain chain)
  {
    return chain;
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getRequestDispatcher(String url)
  {
    // Currently no caching since this is only used for the error-page directive at the host level

    if (url == null)
      throw new IllegalArgumentException(L.l("request dispatcher url can't be null."));
    else if (! url.startsWith("/"))
      throw new IllegalArgumentException(L.l("request dispatcher url `{0}' must be absolute", url));

    InvocationServlet includeInvocation = new InvocationServlet();
    InvocationServlet forwardInvocation = new InvocationServlet();
    InvocationServlet errorInvocation = new InvocationServlet();
    InvocationServlet dispatchInvocation = new InvocationServlet();
    InvocationServlet requestInvocation = dispatchInvocation;
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = url;

    try {
      decoder.splitQuery(includeInvocation, rawURI);
      decoder.splitQuery(forwardInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);
      decoder.splitQuery(dispatchInvocation, rawURI);

      buildIncludeInvocation(includeInvocation);
      buildForwardInvocation(forwardInvocation);
      buildErrorInvocation(errorInvocation);
      buildDispatchInvocation(dispatchInvocation);

      //WebAppController controller = getWebAppController(includeInvocation);
      DeployHandle<WebApp> webAppHandle = getWebAppHandle(includeInvocation);
      WebApp webApp = getWebApp(includeInvocation, webAppHandle, false);

      RequestDispatcher disp
        = new RequestDispatcherImpl(includeInvocation,
                                    forwardInvocation,
                                    errorInvocation,
                                    dispatchInvocation,
                                    requestInvocation,
                                    webApp);

      return disp;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * Creates the invocation.
   */
  public void buildIncludeInvocation(InvocationServlet invocation)
    throws ServletException
  {
    WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null) {
      webApp.getDispatcher().buildIncludeInvocation(invocation);
    }
  }

  /**
   * Creates the invocation.
   */
  public void buildForwardInvocation(InvocationServlet invocation)
    throws ServletException
  {
    WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null) {
      webApp.getDispatcher().buildForwardInvocation(invocation);
    }
  }

  /**
   * Creates the error invocation.
   */
  public void buildErrorInvocation(InvocationServlet invocation)
    throws ServletException
  {
    WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null) {
      webApp.getDispatcher().buildErrorInvocation(invocation);
    }
  }

  /**
   * Creates the invocation.
   */
  public void buildLoginInvocation(InvocationServlet invocation)
    throws ServletException
  {
   WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null)
      webApp.getDispatcher().buildErrorInvocation(invocation);
  }

  /**
   * Creates the invocation for a rewrite-dispatch/dispatch.
   */
  public void buildDispatchInvocation(InvocationServlet invocation)
    throws ServletException
  {
   WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null) {
      webApp.getDispatcher().buildDispatchInvocation(invocation);
    }
  }

  /**
   * Creates a sub invocation, handing unmapped URLs and stopped webApps.
   */
  private WebApp buildSubInvocation(InvocationServlet invocation)
  {
    if (! _lifecycle.waitForActive(_startWaitTime)) {
      UnavailableException e;
      e = new UnavailableException(invocation.getURI());

      FilterChain chain = new FilterChainException(e);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());
      return null;
    }

    DeployHandle<WebApp> webAppHandle = getWebAppHandle(invocation);

    if (webAppHandle == null) {
      String url = invocation.getURI();

      FileNotFoundException e = new FileNotFoundException(url);

      FilterChain chain = new FilterChainException(e);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());
      return null;
    }

    WebApp webApp = webAppHandle.subrequest();

    if (webApp == null) {
      UnavailableException e;
      e = new UnavailableException(invocation.getURI());

      FilterChain chain = new FilterChainException(e);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());
      return null;
    }

    return webApp;
  }

  /**
   * Returns the webApp for the current request.
   */
  private WebApp getWebApp(InvocationServlet invocation,
                           DeployHandle<WebApp> handle,
                           boolean isTopRequest)
  {
    try {
      if (handle != null) {
        WebApp webApp;

        if (isTopRequest) {
          webApp = handle.request();
        }
        else {
          webApp = handle.subrequest();
        }

        if (webApp == null) {
          return null;
        }

        invocation.setWebApp(webApp);

        return webApp;
      }
      else {
        return null;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
  
  /*
  private WebApp getWebApp(Invocation invocation,
                           WebAppController controller,
                           boolean isTopRequest)
  {
    try {
      if (controller != null) {
        WebApp webApp;

        if (isTopRequest) {
          webApp = controller.request();
        }
        else {
          webApp = controller.subrequest();
        }

        if (webApp == null) {
          return null;
        }

        invocation.setWebApp(webApp);

        return webApp;
      }
      else {
        return null;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  */

  /**
   * Returns the webApp controller for the current request.  Side effect
   * of filling in the invocation's context path and context uri.
   *
   * @param invocation the request's invocation
   *
   * @return the controller or null if none match the url.
   */
  /*
  protected WebAppHandle getWebAppHandle(Invocation invocation)
  {
    WebAppUriMap entry = findEntryByURI(invocation.getURI());

    if (entry == null) {
      return null;
    }

    String invocationURI = invocation.getURI();

    // server/1hb1
    // String contextPath = controller.getContextPath(invocationURI);
    String contextPath = entry.getContextPath();

    // invocation.setContextPath(invocationURI.substring(0, contextPath.length()));
    invocation.setContextPath(contextPath);

    String uri = invocationURI.substring(contextPath.length());
    invocation.setContextURI(uri);

    //return entry.getController();
    return entry.getHandle();
  }
  */

  /**
   * Returns the webApp controller for the current request.  Side effect
   * of filling in the invocation's context path and context uri.
   *
   * @param invocation the request's invocation
   *
   * @return the controller or null if none match the url.
   */
  protected DeployHandle<WebApp> getWebAppHandle(InvocationServlet invocation)
  {
    //WebAppUriMap entry = findEntryByURI(invocation.getURI());
    
    WebAppUriMap entry = _webAppRouter.find(invocation.getURI());

    if (entry == null) {
      return null;
    }

    String invocationURI = invocation.getURI();

    // server/1hb1
    // String contextPath = controller.getContextPath(invocationURI);
    String contextPath = entry.getContextPath();

    // invocation.setContextPath(invocationURI.substring(0, contextPath.length()));
    invocation.setContextPath(contextPath);

    String uri = invocationURI.substring(contextPath.length());
    invocation.setContextURI(uri);

    return entry.getHandle();
  }

  public WebAppController createWebAppController(String id,
                                                 PathImpl rootDirectory,
                                                 String urlPrefix)
  {
    return new WebAppController(id, rootDirectory, this, urlPrefix);
  }

  /*
  protected DeployHandle<WebApp> createHandle(String id)
  {
    return _appDeploy.createHandle(id);
  }
  */

  /**
   * Creates the invocation.
   */
  public WebApp findWebAppByURI(String uri)
  {
    WebAppUriMap entry = findEntryByURI(uri);

    if (entry != null) {
      return entry.getHandle().request();
    }
    else {
      return null;
    }
  }

  /**
   * Creates the invocation.
   */
  public WebApp findSubWebAppByURI(String uri)
  {
    DeployHandle<WebApp> handle = findByURI(uri);

    if (handle != null) {
      return handle.subrequest();
    }
    else {
      return null;
    }
  }

  /**
   * Finds the web-app matching the current entry.
   */
  public DeployHandle<WebApp> findByURI(String uri)
  {
    WebAppUriMap entry = findEntryByURI(uri);
    
    if (entry != null)
      return entry.getHandle();
    else
      return null;
  }
  
  /**
   * Finds the web-app matching the current entry.
   */
  public WebAppUriMap findEntryByURI(String uri)
  {
    if (_appDeploy.isModified()) {
      _appDeploy.logModified(log);
      
      _uriToAppCache.clear();
    }

    WebAppUriMap entry = _uriToAppCache.get(uri);

    if (entry != null) {
      return entry;
    }

    String cleanUri = uri;
    if (CauchoUtil.isCaseInsensitive()) {
      cleanUri = cleanUri.toLowerCase(Locale.ENGLISH);
    }

    // server/105w
    try {
      cleanUri = getInvocationDecoder().normalizeUri(cleanUri);
    } catch (java.io.IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    entry = findByURIImpl(cleanUri);

    _uriToAppCache.put(uri, entry);

    return entry;
  }

  /**
   * Finds the web-app for the entry.
   */
  private WebAppUriMap findByURIImpl(String subURI)
  {
    WebAppUriMap entry = _uriToAppCache.get(subURI);

    if (entry != null) {
      return entry ;
    }

    int length = subURI.length();
    int p = subURI.lastIndexOf('/');

    if (p < 0 || p < length - 1) { // server/26cf
      //WebAppController controller = _appDeploy.findController(subURI);
      DeployHandle<WebApp> webAppHandle = _appDeploy.findHandle(subURI);
      
      if (webAppHandle != null) {
        entry = new WebAppUriMap(subURI, webAppHandle);
        
        _uriToAppCache.put(subURI, entry);

        return entry;
      }
    }

    if (p >= 0) {
      entry = findByURIImpl(subURI.substring(0, p));

      if (entry != null)
        _uriToAppCache.put(subURI, entry);
    }

    return entry;
  }

  public DeployContainerService<WebApp,WebAppController> getWebAppDeployContainer()
  {
    return _appDeploy;
  }
  
  /**
   * Finds the web-app for the entry, not checking for sub-apps.
   * (used by LocalDeployServlet)
   */
  /*
  public WebAppController findController(String subURI)
  {
    return _appDeploy.findController(subURI);
  }
  */

  /**
   * Returns a list of the webApps.
   */
  public WebAppController []getWebAppList()
  {
    return _appDeploy.getControllers();
  }

  /**
   * Returns a list of the webApps.
   */
  public DeployHandle<WebApp> []getWebAppHandles()
  {
    return _appDeploy.getHandles();
  }

  /**
   * Returns true if the webApp container has been closed.
   */
  public final boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Returns true if the webApp container is active
   */
  public final boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Closes the container.
   */
  public boolean stop(ShutdownModeAmp mode)
  {
    // XXX: _earDeploy.stop();
    _appDeploy.stop(mode);
    // _serviceDeploy.stop();
    _webAppRouter.shutdown(mode);

    return true;
  }

  /**
   * Closes the container.
   */
  public void destroy(ShutdownModeAmp mode)
  {
    // XXX: _earDeploy.destroy();
    _appDeploy.destroy(mode);
    // _serviceDeploy.destroy();
    
    WebApp errorWebApp = _errorWebApp;
    
    if (errorWebApp != null) {
      try {
        errorWebApp.shutdown(mode);
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    AccessLogBase accessLog = _accessLog;
    _accessLog = null;

    if (accessLog != null) {
      try {
        accessLog.close();
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    _webAppRouter.shutdown(mode);
  }

  /**
   * Returns the error webApp during startup.
   */
  public WebApp getErrorWebApp()
  {
    if (_errorWebApp == null) {
      WebApp defaultWebApp = findWebAppByURI("/");
      
      if (defaultWebApp != null) {
        return defaultWebApp;
      }
    }
    
    synchronized (this) {
      if (_errorWebApp == null) {
        _errorWebApp = createErrorWebApp();
      }
      
      return _errorWebApp;
    }
  }
  
  private WebApp createErrorWebApp()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      PathImpl errorRoot = new MemoryPath().lookup("/error-root");
      errorRoot.mkdirs();
      
      String id = "webapps/" + getHostName()+ "/error";

      WebAppController webAppController
        = createWebAppController(id, errorRoot, "/");
      
      DeployHandle<WebApp> handle = _appDeploy.createHandle(id);
      
      handle.getService().setController(webAppController);
      
      // XXX: webAppController.init();
      //webAppController.startOnInit();
      handle.startOnInit();
        
      //return webAppController.request();
      return handle.request();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  /**
   * Handles the case where a class loader has completed initialization
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  @Override
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    destroy(ShutdownModeAmp.GRACEFUL);
  }

  /**
   * Handles the case where the environment is stopping
   */
  @Override
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop(ShutdownModeAmp.GRACEFUL);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _classLoader.getId() + "]";
  }

  /*
  private static class WebAppUriMap {
    private final String _contextPath;
    //private final WebAppController _webAppController;
    private final DeployHandle<WebApp> _webAppHandle;
    
    WebAppUriMap(String contextPath, DeployHandle<WebApp> webAppHandle)
    {
      _contextPath = contextPath;
      //_webAppController = webAppController;
      _webAppHandle = webAppHandle;
    }
    
    String getContextPath()
    {
      return _contextPath;
    }
    
    DeployHandle<WebApp> getHandle()
    {
      return _webAppHandle;
    }
  }
  */
}
