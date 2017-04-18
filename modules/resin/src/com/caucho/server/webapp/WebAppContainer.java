/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.ConfigException;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployContainerApi;
import com.caucho.env.deploy.DeployGenerator;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.make.AlwaysModified;
import com.caucho.rewrite.DispatchRule;
import com.caucho.rewrite.RewriteFilter;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationBuilder;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.e_app.EarDeployController;
import com.caucho.server.e_app.EarDeployGenerator;
import com.caucho.server.e_app.EarSingleDeployGenerator;
import com.caucho.server.host.Host;
import com.caucho.server.log.AbstractAccessLog;
import com.caucho.server.log.AccessLog;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.session.SessionManager;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Resin's webApp implementation.
 */
public class WebAppContainer
  implements InvocationBuilder, ClassLoaderListener, EnvironmentListener
{
  static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebAppContainer.class.getName());

  private ServletService _server;
  private Host _host;

  // The context class loader
  private EnvironmentClassLoader _classLoader;
  
  private final Lifecycle _lifecycle;

  // The root directory.
  private Path _rootDir;

  // The document directory.
  private Path _docDir;

  // dispatch mapping
  private RewriteDispatch _rewriteDispatch;
  private WebApp _errorWebApp;

  // List of default ear webApp configurations
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<EarConfig>();

  private DeployContainer<EarDeployController> _earDeploy;
  
  private final DeployContainer<WebAppController> _appDeploySpi
    = new DeployContainer<WebAppController>(WebAppController.class);
  private final DeployContainerApi<WebAppController> _appDeploy
    = _appDeploySpi;
  
  private WebAppExpandDeployGenerator _warGenerator;

  private boolean _hasWarGenerator;

  private static final int URI_CACHE_SIZE = 8192;
  
  // LRU cache for the webApp lookup
  private LruCache<String,WebAppUriMap> _uriToAppCache
    = new LruCache<String,WebAppUriMap>(URI_CACHE_SIZE);

  // List of default webApp configurations
  private ArrayList<WebAppConfig> _webAppDefaultList
    = new ArrayList<WebAppConfig>();

  private AbstractAccessLog _accessLog;

  private long _startWaitTime = 10000L;

  private Throwable _configException;

  /**
   * Creates the webApp with its environment loader.
   */
  public WebAppContainer(ServletService server,
                         Host host,
                         Path rootDirectory,
                         EnvironmentClassLoader loader,
                         Lifecycle lifecycle)
  {
    _server = server;
    
    if (server == null)
      throw new NullPointerException();
    
    _host = host;
    
    if (host == null)
      throw new NullPointerException();
    
    _rootDir = rootDirectory;

    _classLoader = loader;
    
    if (lifecycle == null)
      throw new NullPointerException();

    _lifecycle = lifecycle;

    /*
    Environment.addEnvironmentListener(this, loader);
    Environment.addClassLoaderListener(this, loader);
    */

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(loader);

      // These need to be in the proper class loader so they can
      // register themselves with the environment
      _earDeploy = new DeployContainer<EarDeployController>(EarDeployController.class);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected ServletService getServer()
  {
    return _server;
  }

  public InvocationDecoder getInvocationDecoder()
  {
    return getServer().getInvocationDecoder();
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
  
  public String getStageTag()
  {
    return getServer().getStage();
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path path)
  {
    _rootDir = path;

    Vfs.setPwd(path, getClassLoader());
  }

  /**
   * Gets the document directory.
   */
  public Path getDocumentDirectory()
  {
    if (_docDir != null)
      return _docDir;
    else
      return _rootDir;
  }

  /**
   * Sets the document directory.
   */
  public void setDocumentDirectory(Path path)
  {
    _docDir = path;
  }

  /**
   * Sets the document directory.
   */
  public void setDocDir(Path path)
  {
    setDocumentDirectory(path);
  }

  /**
   * Sets the access log.
   */
  public AbstractAccessLog createAccessLog()
  {
    if (_accessLog == null)
      _accessLog = new AccessLog();

    return _accessLog;
  }

  /**
   * Sets the access log.
   */
  public void setAccessLog(AbstractAccessLog log)
  {
    _accessLog = log;

    Environment.setAttribute("caucho.server.access-log", log);
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
    return getErrorWebApp().getErrorPageManager();
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
  public DeployContainer<WebAppController> getWebAppGenerator()
  {
    return _appDeploySpi;
  }

  /**
   * Returns the container's session manager.
   */
  public SessionManager getSessionManager()
  {
    return null;
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(DispatchRule dispatchRule)
  {
    if (dispatchRule.isRequest()) {
      RewriteDispatch rewrite = createRewriteDispatch();

      rewrite.addRule(dispatchRule);
    }
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(RewriteFilter dispatchAction)
  {
    if (dispatchAction.isRequest()) {
      RewriteDispatch rewrite = createRewriteDispatch();

      rewrite.addAction(dispatchAction);
    }
  }

  /**
   * Adds rewrite-dispatch (backward compat).
   */
  public RewriteDispatch createRewriteDispatch()
  {
    if (_rewriteDispatch == null) {
      _rewriteDispatch = new RewriteDispatch(getServer());
    }

    return _rewriteDispatch;
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
      DeployGenerator<WebAppController> deploy
        = new WebAppRegexpDeployGenerator(_appDeploySpi, this, config);
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

    WebAppSingleDeployGenerator deployGenerator = createDeployGenerator(config);
      
    addWebApp(deployGenerator);
  }
  
  public WebAppSingleDeployGenerator createDeployGenerator(WebAppConfig config)
  {
    return new WebAppSingleDeployGenerator(_appDeploySpi, this, config);
  }
  
  public void addWebApp(WebAppSingleDeployGenerator deployGenerator)
  {
    deployGenerator.deploy();
    
    _appDeploy.add(deployGenerator);

    clearCache();
  }
  
  public void removeWebApp(WebAppSingleDeployGenerator deployGenerator)
  {
    _appDeploy.remove(deployGenerator);
    deployGenerator.destroy();
  }

  /**
   * Removes an webApp.
   */
  void removeWebApp(WebAppController entry)
  {
    _appDeploy.remove(entry.getContextPath());

    clearCache();
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
  public WebAppExpandDeployGenerator createWarDeploy()
  {
    String stage = getServer().getStage();
    String host = getHost().getIdTail();
    
    String id = stage + "/webapp/" + host;
    
    return new WebAppExpandDeployGenerator(id, _appDeploySpi, this);
  }

  /**
   * Sets the war-expansion
   */
  public WebAppExpandDeployGenerator createWebAppDeploy()
  {
    return createWarDeploy();
  }

  /**
   * Sets the war-expansion
   */
  public void addWebAppDeploy(WebAppExpandDeployGenerator deploy)
    throws ConfigException
  {
    addWarDeploy(deploy);
  }

  /**
   * Sets the war-expansion
   */
  public void addWarDeploy(WebAppExpandDeployGenerator webAppDeploy)
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
  public void addDeploy(DeployGenerator<WebAppController> deploy)
    throws ConfigException
  {
    if (deploy instanceof WebAppExpandDeployGenerator)
      addWebAppDeploy((WebAppExpandDeployGenerator) deploy);
    else
      _appDeploy.add(deploy);
  }

  /**
   * Removes a web-app-generator.
   */
  public void removeWebAppDeploy(DeployGenerator<WebAppController> deploy)
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

  /**
   * Adds an enterprise webApp.
   */
  public void addApplication(EarConfig config)
  {
    DeployGenerator<EarDeployController> deploy = new EarSingleDeployGenerator(_earDeploy, this, config);

    _earDeploy.add(deploy);
  }

  /**
   * Updates an ear deploy
   */
  public void updateEarDeploy(String name)
    throws Throwable
  {
    clearCache();
    
    _earDeploy.update();
    EarDeployController entry = _earDeploy.update(name);

    if (entry != null) {
      entry.start();

      Throwable configException = entry.getConfigException();

      if (configException != null)
        throw configException;
    }
    
    clearCache();
  }

  /**
   * Updates an ear deploy
   */
  public void expandEarDeploy(String name)
  {
    clearCache();
    
    _earDeploy.update();
    EarDeployController entry = _earDeploy.update(name);

    if (entry != null)
      entry.start();
    
    clearCache();
  }

  /**
   * Start an ear
   */
  public void startEarDeploy(String name)
  {
    clearCache();
    
    _earDeploy.update();
    EarDeployController entry = _earDeploy.update(name);

    if (entry != null)
      entry.start();
    
    clearCache();
  }

  /**
   * Adds an ear default
   */
  public void addEarDefault(EarConfig config)
  {
    _earDefaultList.add(config);
  }

  /**
   * Returns the list of ear defaults
   */
  public ArrayList<EarConfig> getEarDefaultList()
  {
    return _earDefaultList;
  }

  /**
   * Sets the ear-expansion
   */
  public EarDeployGenerator createEarDeploy()
    throws Exception
  {
    String id = getStageTag() + "/entapp/" + getHost().getIdTail();
    
    return new EarDeployGenerator(id, _earDeploy, this);
  }

  /**
   * Adds the ear-expansion
   */
  public void addEarDeploy(EarDeployGenerator earDeploy)
    throws Exception
  {
    _earDeploy.add(earDeploy);

    // server/26cc - _appDeploy must be added first, because the
    // _earDeploy addition will automaticall register itself
    _appDeploy.add(new WebAppEarDeployGenerator(_appDeploySpi, this, earDeploy));

    /*
    _earDeploy.add(earDeploy);
    */
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
  public void setWarDir(Path warDir)
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
  public Path getWarDir()
  {
    return getWarGenerator().getPath();
  }

  /**
   * Sets the war-expand-dir.
   */
  public void setWarExpandDir(Path warDir)
  {
    getWarGenerator().setExpandDirectory(warDir);
  }

  /**
   * Gets the war-expand-dir.
   */
  public Path getWarExpandDir()
  {
    return getWarGenerator().getExpandDirectory();
  }
  

  private WebAppExpandDeployGenerator getWarGenerator()
  {
    if (_warGenerator == null) {
      String id = getStageTag() + "/webapp/" + getHost().getIdTail();

      _warGenerator = new WebAppExpandDeployGenerator(id,
                                                      _appDeploySpi, 
                                                      this);
    }
    
    return _warGenerator;
  }

  /**
   * Starts the container.
   */
  public void start()
  {
    try {
      if (_accessLog != null) {
        _accessLog.start();
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    try {
      _appDeploy.start();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Clears the cache
   */
  public void clearCache()
  {
    _uriToAppCache = new LruCache<String,WebAppUriMap>(URI_CACHE_SIZE);
    
    _server.clearCache();
  }

  /**
   * Creates the invocation.
   */
  @Override
  public Invocation buildInvocation(Invocation invocation)
    throws ConfigException
  {
    if (_configException != null) {
      FilterChain chain = new ExceptionFilterChain(_configException);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());

      return invocation;
    }
    else if (! _lifecycle.waitForActive(_startWaitTime)) {
      log.fine(this + " container is not active");
      
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
      FilterChain chain = new ErrorFilterChain(code);
      invocation.setFilterChain(chain);

      invocation.setWebApp(getErrorWebApp());

      invocation.setDependency(AlwaysModified.create());

      return invocation ;
    }

    FilterChain chain;
    
    WebAppController controller = getWebAppController(invocation);
    
    WebApp webApp = getWebApp(invocation, controller, true);

    boolean isAlwaysModified;

    if (webApp != null) {
      boolean isTop = _rewriteDispatch == null;
      invocation = webApp.buildInvocation(invocation, isTop);
      chain = invocation.getFilterChain();
      isAlwaysModified = false;
    }
    else if (controller != null){
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
      chain = new ErrorFilterChain(code);
      ContextFilterChain contextChain = new ContextFilterChain(chain);
      contextChain.setErrorPageManager(getErrorPageManager());
      chain = contextChain;
      invocation.setFilterChain(contextChain);
      isAlwaysModified = true;
    }
    else {
      int code = HttpServletResponse.SC_NOT_FOUND;
      chain = new ErrorFilterChain(code);
      ContextFilterChain contextChain = new ContextFilterChain(chain);
      contextChain.setErrorPageManager(getErrorPageManager());
      chain = contextChain;
      invocation.setFilterChain(contextChain);
      isAlwaysModified = true;
    }

    if (_rewriteDispatch != null) {
      String uri = invocation.getURI();
      String queryString = invocation.getQueryString();

      FilterChain rewriteChain = _rewriteDispatch.map(DispatcherType.REQUEST,
                                                      uri,
                                                      queryString,
                                                      chain);
      if (rewriteChain != chain) {
        // server/13sf, server/1kq1, server/1krd
        // WebApp rootWebApp = findWebAppByURI("/");
        WebApp rootWebApp = findWebAppByURI(uri);
        
        if (rootWebApp == null) {
          // server/1u12
          rootWebApp = getErrorWebApp();
        }
        
        invocation.setWebApp(rootWebApp);
        isAlwaysModified = false;
      }
        
      // server/1k21 vs server/1kk7
      // if (rootWebApp != webApp)
      // server/02ex vs server/02ey - false
      webApp = invocation.getWebApp();
      rewriteChain = webApp.createWebAppFilterChain(rewriteChain, 
                                                    invocation,
                                                    true);

      invocation.setFilterChain(rewriteChain);
    }

    if (isAlwaysModified)
      invocation.setDependency(AlwaysModified.create());

    return invocation;
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

    Invocation includeInvocation = new Invocation();
    Invocation forwardInvocation = new Invocation();
    Invocation errorInvocation = new Invocation();
    Invocation dispatchInvocation = new Invocation();
    Invocation requestInvocation = dispatchInvocation;
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

      WebAppController controller = getWebAppController(includeInvocation);
      WebApp webApp = getWebApp(includeInvocation, controller, false);

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
  public void buildIncludeInvocation(Invocation invocation)
    throws ServletException
  {
    WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null)
      webApp.buildIncludeInvocation(invocation);
  }

  /**
   * Creates the invocation.
   */
  public void buildForwardInvocation(Invocation invocation)
    throws ServletException
  {
    WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null)
      webApp.buildForwardInvocation(invocation);
  }

  /**
   * Creates the error invocation.
   */
  public void buildErrorInvocation(Invocation invocation)
    throws ServletException
  {
    WebApp webApp = buildSubInvocation(invocation);

    if (webApp != null)
      webApp.buildErrorInvocation(invocation);
  }

  /**
   * Creates the invocation.
   */
  public void buildLoginInvocation(Invocation invocation)
    throws ServletException
  {
   WebApp app = buildSubInvocation(invocation);

    if (app != null)
      app.buildErrorInvocation(invocation);
  }

  /**
   * Creates the invocation for a rewrite-dispatch/dispatch.
   */
  public void buildDispatchInvocation(Invocation invocation)
    throws ServletException
  {
   WebApp app = buildSubInvocation(invocation);

    if (app != null)
      app.buildDispatchInvocation(invocation);
  }

  /**
   * Creates a sub invocation, handing unmapped URLs and stopped webApps.
   */
  private WebApp buildSubInvocation(Invocation invocation)
  {
    if (! _lifecycle.waitForActive(_startWaitTime)) {
      UnavailableException e;
      e = new UnavailableException(invocation.getURI());

      FilterChain chain = new ExceptionFilterChain(e);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());
      return null;
    }

    WebAppController appController = getWebAppController(invocation);

    if (appController == null) {
      String url = invocation.getURI();

      FileNotFoundException e = new FileNotFoundException(url);

      FilterChain chain = new ExceptionFilterChain(e);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());
      return null;
    }

    WebApp webApp = appController.subrequest();

    if (webApp == null) {
      UnavailableException e;
      e = new UnavailableException(invocation.getURI());

      FilterChain chain = new ExceptionFilterChain(e);
      invocation.setFilterChain(chain);
      invocation.setDependency(AlwaysModified.create());
      return null;
    }

    return webApp;
  }

  /**
   * Returns the webApp for the current request.
   */
  private WebApp getWebApp(Invocation invocation,
                           WebAppController controller,
                           boolean isTopRequest)
  {
    try {
      if (controller != null) {
        WebApp webApp;

        if (isTopRequest)
          webApp = controller.request();
        else
          webApp = controller.subrequest();

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

  /**
   * Returns the webApp controller for the current request.  Side effect
   * of filling in the invocation's context path and context uri.
   *
   * @param invocation the request's invocation
   *
   * @return the controller or null if none match the url.
   */
  protected WebAppController getWebAppController(Invocation invocation)
  {
    String invocationURI = invocation.getURI();
    
    WebAppUriMap entry = findEntryByURI(invocation.getURI());
    
    if (entry == null) {
      return null;
    }

    // server/1hb1
    WebAppController controller = entry.getController();
    String contextPath = controller.getContextPath(invocationURI);
    //String contextPath = entry.getContextPath();

    // server/108i
    //invocation.setContextPath(invocationURI.substring(0, contextPath.length()));
    invocation.setContextPath(contextPath);

    String contextUri = invocationURI.substring(contextPath.length());
    invocation.setContextURI(contextUri);

    return entry.getController();
  }

  /**
   * Creates the invocation.
   */
  public WebApp findWebAppByURI(String uri)
  {
    WebAppUriMap entry = findEntryByURI(uri);

    if (entry != null)
      return entry.getController().request();
    else
      return null;
  }

  /**
   * Creates the invocation.
   */
  public WebApp findSubWebAppByURI(String uri)
  {
    WebAppController controller = findByURI(uri);

    if (controller != null)
      return controller.subrequest();
    else
      return null;
  }

  /**
   * Finds the web-app matching the current entry.
   */
  public WebAppController findByURI(String uri)
  {
    WebAppUriMap entry = findEntryByURI(uri);
    
    if (entry != null)
      return entry.getController();
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

    if (entry != null)
      return entry;

    String cleanUri = uri;
    if (CauchoSystem.isCaseInsensitive())
      cleanUri = cleanUri.toLowerCase(Locale.ENGLISH);

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
      WebAppController controller = _appDeploy.findController(subURI);

      if (controller != null) {
        entry = new WebAppUriMap(subURI, controller);
        
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

  public DeployContainerApi<WebAppController> getWebAppDeployContainer()
  {
    return _appDeploy;
  }
  
  /**
   * Finds the web-app for the entry, not checking for sub-apps.
   * (used by LocalDeployServlet)
   */
  public WebAppController findController(String subURI)
  {
    return _appDeploy.findController(subURI);
  }

  /**
   * Returns a list of the webApps.
   */
  public WebAppController []getWebAppList()
  {
    return _appDeploy.getControllers();
  }

  public DeployContainerApi<EarDeployController> getEarDeployContainer()
  {
    return _earDeploy;
  }
  /**
   * Returns a list of the webApps.
   */
  public EarDeployController []getEntAppList()
  {
    return _earDeploy.getControllers();
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
  public boolean stop()
  {
    _earDeploy.stop();
    _appDeploy.stop();
    
    if (_errorWebApp != null) {
      _errorWebApp.stop();
    }

    return true;
  }

  /**
   * Closes the container.
   */
  public void destroy()
  {
    _earDeploy.destroy();
    _appDeploy.destroy();
    
    if (_errorWebApp != null) {
      _errorWebApp.destroy();
    }

    AbstractAccessLog accessLog = _accessLog;
    _accessLog = null;

    if (accessLog != null) {
      try {
        accessLog.destroy();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  /**
   * Returns the error webApp during startup.
   */
  public WebApp getErrorWebApp()
  {
    if (_errorWebApp == null) {
      WebApp defaultWebApp = findWebAppByURI("/");
      
      if (defaultWebApp != null)
        return defaultWebApp;
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

      Path errorRoot = new MemoryPath().lookup("/error-root");
      errorRoot.mkdirs();
      
      String id = "error/webapp/" + getHostName()+ "/error";

      UnknownWebAppController webAppController
        = new UnknownWebAppController(id, errorRoot, this);
      webAppController.init();
      webAppController.startOnInit();
        
      return webAppController.request();
    } catch (Exception e) {
      throw ConfigException.create(e);
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
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    destroy();
  }

  /**
   * Handles the environment config phase
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the environment bind phase
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _classLoader.getId() + "]";
  }
  
  static class WebAppUriMap {
    private final String _contextPath;
    private final WebAppController _webAppController;
    
    WebAppUriMap(String contextPath, WebAppController webAppController)
    {
      _contextPath = contextPath;
      _webAppController = webAppController;
    }
    
    String getContextPath()
    {
      return _contextPath;
    }
    
    WebAppController getController()
    {
      return _webAppController;
    }
  }
}
