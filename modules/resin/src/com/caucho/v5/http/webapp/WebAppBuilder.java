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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletException;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.descriptor.TaglibDescriptor;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.types.Icon;
import com.caucho.v5.config.types.InitParam;
import com.caucho.v5.deploy.DeployGenerator;
import com.caucho.v5.deploy.DeployInstanceBuilder;
import com.caucho.v5.deploy2.DeployMode;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.FilterChainBuilder;
import com.caucho.v5.http.dispatch.FilterConfigImpl;
import com.caucho.v5.http.dispatch.FilterManager;
import com.caucho.v5.http.dispatch.FilterMapper;
import com.caucho.v5.http.dispatch.FilterMapping;
import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.http.dispatch.ServletDefaultMapper;
import com.caucho.v5.http.dispatch.ServletManager;
import com.caucho.v5.http.dispatch.ServletMapper;
import com.caucho.v5.http.dispatch.ServletMapping;
import com.caucho.v5.http.dispatch.ServletRegexp;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.rewrite.DispatchRuleBase;
import com.caucho.v5.http.rewrite.IfSecure;
import com.caucho.v5.http.rewrite.Not;
import com.caucho.v5.http.rewrite.RedirectSecure;
import com.caucho.v5.http.rewrite.WelcomeFile;
import com.caucho.v5.http.security.ConstraintManager;
import com.caucho.v5.http.security.SecurityConstraint;
import com.caucho.v5.http.security.WebResourceCollection;
import com.caucho.v5.http.servlets.ErrorStatusServlet;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;

/**
 * Builder for the webapp to encapsulate the configuration process.
 */
public class WebAppBuilder<T extends WebApp>
  implements DeployInstanceBuilder<WebApp> // , XmlSchemaBean
{
  private static final L10N L = new L10N(WebAppBuilder.class);

  private static final Logger log
    = Logger.getLogger(WebAppBuilder.class.getName());

  private final WebAppController _controller;
  private EnvironmentClassLoader _classLoader;
  private HttpContainerServlet _httpContainer;
  private Host _host;

  // The servlet manager
  private ServletManager _servletManager;
  // The servlet mapper
  private ServletMapper _servletMapper;
  // True the mapper should be strict
  private boolean _isStrictMapping;
  // True if the servlet init-param is allowed to use EL
  private boolean _servletAllowEL = false;
  // True if requestDispatcher forward is allowed after buffers flush
  private boolean _isAllowForwardAfterFlush = false;

  private Throwable _configException;

  // XXX: transitional copy of the web-app during building
  private T _webApp;

  // Any war-generators.
  private ArrayList<DeployGenerator<WebApp,WebAppController>> _appGenerators
    = new ArrayList<>();

  private String _moduleName;

  private String _description;

  private boolean _isDisableCrossContext;

  private boolean _isCompileContext;

  private String _servletVersion;

  private boolean _isApplyingWebFragments;

  private boolean _isDynamicDeploy;

  // Any web-app-default for children
  private ArrayList<WebAppConfig> _webAppDefaultList
    = new ArrayList<>();

  private boolean _isMetadataComplete;

  private boolean _isStatisticsEnabled;

  private boolean _isPre23Config;

  private WebAppBuilderFragment _builderFragment;

  private int _podSize;

  private boolean _isInheritSession;

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

  // The security constraints
  private ConstraintManager _constraintManager;

  private FilterChainBuilder _securityBuilder;
  // real-path mapping
  private RewriteRealPath _rewriteRealPath;

  private ArrayList<String> _welcomeFileList = new ArrayList<>();

  private boolean _cookieHttpOnly;

  private WelcomeFile _welcomeFile;

  private WebAppBuilderScan _scanner;

  private WebAppListeners _listenerManager;

  private boolean _isSecure;

  private boolean _isFragment;

  /**
   * Builder Creates the webApp with its environment loader.
   */
  public WebAppBuilder(WebAppController controller)
  {
    Objects.requireNonNull(controller);

    _controller = controller;
    
    _classLoader
      = EnvironmentClassLoader.create(controller.getParentClassLoader(),
                                      "web-app:" + getId());

    _httpContainer = controller.getHttpContainer();

    if (_httpContainer == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          HttpContainerServlet.class.getSimpleName()));
    }

    _host = controller.getHost();

    if (_host == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          Host.class.getSimpleName()));
    }

    _builderFragment = new WebAppBuilderFragment(this);

    _servletManager = createServletManager();
    _servletMapper = new ServletMapper(null);
    _servletMapper.setServletManager(_servletManager);

    _filterManager = new FilterManager();
    _filterMapper = new FilterMapper();

    _filterMapper.setFilterManager(_filterManager);

    _loginFilterMapper = new FilterMapper();
    _loginFilterMapper.setFilterManager(_filterManager);

    _includeFilterMapper = new FilterMapper();
    _includeFilterMapper.setFilterManager(_filterManager);

    _forwardFilterMapper = new FilterMapper();
    _forwardFilterMapper.setFilterManager(_filterManager);

    _dispatchFilterMapper = new FilterMapper();
    _dispatchFilterMapper.setFilterManager(_filterManager);

    _errorFilterMapper = new FilterMapper();
    _errorFilterMapper.setFilterManager(_filterManager);

    _constraintManager = new ConstraintManager();

    _scanner = new WebAppBuilderScan(this);
    _listenerManager = new WebAppListeners(this);
    
    WebApp webApp = getWebApp();
    webApp.initConstructor();

    _servletMapper.setServletContext(webApp);
    _filterMapper.setServletContext(webApp);
    _loginFilterMapper.setServletContext(webApp);
    _includeFilterMapper.setServletContext(webApp);
    _forwardFilterMapper.setServletContext(webApp);
    _dispatchFilterMapper.setServletContext(webApp);
    _errorFilterMapper.setServletContext(webApp);
  }
  
  protected ServletManager createServletManager()
  {
    return new ServletManager();
  }
  
  @Override
  public WebApp getInstance()
  {
    return getWebApp();
  }

  WebAppBuilderScan getScanner()
  {
    return _scanner;
  }
  
  boolean isSecure()
  {
    return _isSecure;
  }

  FilterManager getFilterManager()
  {
    return _filterManager;
  }

  FilterMapper getFilterMapper()
  {
    return _filterMapper;
  }

  FilterMapper getFilterMapperLogin()
  {
    return _loginFilterMapper;
  }

  FilterMapper getFilterMapperInclude()
  {
    return _includeFilterMapper;
  }

  FilterMapper getFilterMapperForward()
  {
    return _forwardFilterMapper;
  }

  FilterMapper getFilterMapperDispatch()
  {
    return _dispatchFilterMapper;
  }

  FilterMapper getFilterMapperError()
  {
    return _errorFilterMapper;
  }

  FilterChainBuilder getSecurityBuilder()
  {
    if (_constraintManager != null) {
      return _constraintManager.getFilterBuilder();
    }
    else {
      return null;
    }
  }

  //
  // special auto-config methods
  //

  //
  // configuration
  //

  /**
   * app-dir (compat)
   */
  public void setAppDir(PathImpl appDir)
  {
    setRootDirectory(appDir);
  }

  /**
   * allow-servlet-el enables EL expressions in the the servlet init-param
   */
  @Configurable
  public void setAllowServletEL(boolean allow)
  {
    _servletAllowEL = allow;
  }

  /**
   * allow-forward-after-flush
   */
  @Configurable
  public void setAllowForwardAfterFlush(boolean isEnable)
  {
    getWebApp().getDispatcher().setAllowForwardAfterFlush(isEnable);
  }

  /*
  public boolean isAllowForwardAfterFlush()
  {
    return _isAllowForwardAfterFlush;
  }
  */

  /**
   * Sets the cookie-http-only
   */
  @Configurable
  public void setCookieHttpOnly(boolean isHttpOnly)
  {
    _cookieHttpOnly = isHttpOnly;
  }

  /**
   * Sets the cookie-http-only
   */
  public boolean getCookieHttpOnly()
  {
    return _cookieHttpOnly;
  }

  /**
   * context-param configuration
   */
  public InitParam createContextParam()
  {
    InitParam initParam = new InitParam();

    initParam.setAllowEL(_servletAllowEL);

    return initParam;
  }

  /**
   * context-param configuration
   */
  @Configurable
  public void addContextParam(InitParam initParam)
  {
    HashMap<String,String> map = initParam.getParameters();

    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      String value = map.get(key);

      getWebApp().setInitParam(key, value);
    }
  }

  @Configurable
  public void setDenyUncoveredHttpMethods(boolean isDeny)
  {
    _constraintManager.setDenyUncoveredHttpMethods(isDeny);
  }

  /**
   * document-directory (compat)
   */
  @Configurable
  public void setDocumentDirectory(PathImpl appDir)
  {
    throw new ConfigException(L.l("Use <root-directory> instead of <document-directory>, because <document-directory> has been removed for Resin 4.0"));
  }

  /**
   * description - a user description of the web-app
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * description - a user description of the web-app
   */
  @Configurable
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * disable-cross-context - if true, disables getContext().
   */
  @Configurable
  public void setDisableCrossContext(boolean isDisable)
  {
    _isDisableCrossContext = isDisable;
  }

  public boolean isDisableCrossContext()
  {
    return _isDisableCrossContext;
  }

  /**
   * dispatch-wraps-filters is true if includes wrap filters.
   */
  @Configurable
  public void setDispatchWrapsFilters(boolean wrap)
  {
    _dispatchWrapsFilters = wrap;
  }

  public boolean getDispatchWrapsFilters()
  {
    return _dispatchWrapsFilters;
  }

  /**
   * directory-servlet configuration
   */
  @Configurable
  public void setDirectoryServlet(String className)
    throws Exception
  {
    ServletBuilder config = new ServletBuilder();
    config.setServletName("directory");
    
    if (className.equals("none")) {
      config.setServletClass(ErrorStatusServlet.class.getName());
    }
    else {
      config.setServletClass(className);
    }

    addServlet(config);
  }

  /**
   * distributable configuration
   */
  @Configurable
  public void setDistributable(boolean isDistributable)
  {
  }

  /**
   * Adds a filter configuration.
   */
  @Configurable
  public void addFilter(FilterConfigImpl config)
  {
    config.setServletContext(getWebApp());

    config.setFilterManager(_filterManager);
    config.setWebApp(getWebApp());

    _filterManager.addFilter(config);
  }
  
  FilterConfigImpl getFilter(String filterName)
  {
    return _filterManager.getFilter(filterName);
  }

  /**
   * Adds a filter-mapping configuration.
   */
  @Configurable
  public void addFilterMapping(FilterMapping filterMapping)
    throws ServletException
  {
    filterMapping.setServletContext(getWebApp());

    _filterManager.addFilterMapping(filterMapping);

    if (filterMapping.isRequest()) {
      _filterMapper.addFilterMapping(filterMapping);
      _loginFilterMapper.addFilterMapping(filterMapping);
    }

    if (filterMapping.isInclude())
      _includeFilterMapper.addFilterMapping(filterMapping);

    if (filterMapping.isForward())
      _forwardFilterMapper.addFilterMapping(filterMapping);

    if (filterMapping.isError())
      _errorFilterMapper.addFilterMapping(filterMapping);
  }
  
  /**
   * isFragment is true if currently configuring a fragment.
   */
  public boolean isFragment()
  {
    return _isFragment;
  }
  
  /**
   * isFragment is true if currently configuring a fragment.
   */
  public void setFragment(boolean isFragment)
  {
    _isFragment = isFragment;
  }

  /**
   * icon configuration
   */
  @Configurable
  public void setIcon(Icon icon)
  {
  }

  /**
   * inherit-session configuration
   */
  @Configurable
  public void setInheritSession(boolean isInheritSession)
  {
    _isInheritSession = isInheritSession;
  }

  public boolean isInheritSession()
  {
    return _isInheritSession;
  }

  /**
   * lazy-servlet-validate configuration to only validate when servlet used.
   */
  @Configurable
  public void setLazyServletValidate(boolean isLazy)
  {
    _servletManager.setLazyValidate(isLazy);
  }

  /**
   * listener configuration
   */
  @Configurable
  public void addListener(ListenerConfig<?> listener)
    throws Exception
  {
    _listenerManager.addListener(listener);
  }

  /**
   * locale-encoding-mapping-list configuration
   */
  @Configurable
  public LocaleEncodingMappingList createLocaleEncodingMappingList()
  {
    return new LocaleEncodingMappingList(getWebApp());
  }

  /**
   * metadata-complete configuration
   */
  @Configurable
  public void setMetadataComplete(boolean metadataComplete)
  {
    _isMetadataComplete = metadataComplete;
  }


  public boolean isMetadataComplete()
  {
    return _isMetadataComplete;
  }

  /**
   * ordering configuration
   */
  @Configurable
  public Ordering createOrdering()
  {
    log.finer(L.l("'{0}' ordering tag should not be used inside web application descriptor.", this));

    return new Ordering();
  }

  /**
   * path-mapping config
   */
  public void addPathMapping(PathMapping pathMapping)
    throws Exception
  {
    String urlPattern = pathMapping.getUrlPattern();
    String urlRegexp = pathMapping.getUrlRegexp();
    String realPath = pathMapping.getRealPath();

    if (urlPattern != null)
      createRewriteRealPath().addPathPattern(urlPattern, realPath);
    else if (urlRegexp != null)
      createRewriteRealPath().addPathRegexp(urlRegexp, realPath);
    else
      throw new NullPointerException();
  }

  /**
   * pod-size configuration for baratine pods
   */
  @Configurable
  public void setPodSize(int podSize)
  {
    _podSize = podSize;
  }

  /**
   * redeploy-mode configuration
   */
  @Configurable
  public void setRedeployMode(DeployMode mode)
  {
    if (_controller != null) {
      _controller.setRedeployMode(mode);
    }
  }

  /**
   * rewrite-real-path config.
   */
  public RewriteRealPath createRewriteRealPath()
  {
    if (_rewriteRealPath == null) {
      _rewriteRealPath = new RewriteRealPath(getWebApp().getRootDirectory());
    }

    return _rewriteRealPath;
  }

  /**
   * root-directory is the web-app root
   */
  @Configurable
  public void setRootDirectory(PathImpl appDir)
  {
  }

  /**
   * xsi:schema-location
   */
  @Configurable
  public void setSchemaLocation(String location)
  {
  }

  /**
   * Sets the secure requirement.
   */
  @Configurable
  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;

    if (isSecure) {
      RedirectSecure redirect = new RedirectSecure();
      // redirect.addURLPattern("/*");
      redirect.add(new Not(new IfSecure()));

      add(redirect);
    }
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(DispatchRuleBase rule)
  {
  }

  /**
   * security constraint configuration
   */
  public void addSecurityConstraint(SecurityConstraint constraint)
  {
    _constraintManager.addConstraint(constraint);
  }

  /**
   * security-role configuration
   */
  @Configurable
  public void addSecurityRole(SecurityRole role)
  {
  }

  /**
   * servlet configuration.
   */
  public ServletBuilder createServlet()
    throws ServletException
  {
    ServletBuilder config = newServletBuilder();

    config.setWebApp(getWebApp());
    config.setServletContext(getWebApp());
    config.setServletMapper(_servletMapper);
    config.setAllowEL(_servletAllowEL);

    return config;
  }
  
  protected ServletBuilder newServletBuilder()
  {
    return new ServletBuilder();
  }

  /**
   * servlet configuration
   */
  @Configurable
  public void addServlet(ServletBuilder config)
    throws ServletException
  {
    config.setServletContext(getWebApp());

    // _servletManager.addServlet(config, _isApplyingWebFragments);
    _servletManager.addServlet(config, true);
  }

  /**
   * servlet-mapping configuration
   */
  public ServletMapping createServletMapping()
  {
    ServletMapping servletMapping = newServletMapping();
    
    if (_isApplyingWebFragments)
      servletMapping.setInFragmentMode();

    servletMapping.setWebApp(getWebApp());
    servletMapping.setServletContext(getWebApp());
    servletMapping.setServletMapper(_servletMapper);
    servletMapping.setStrictMapping(getStrictMapping());

    return servletMapping;
  }
  
  protected ServletMapping newServletMapping()
  {
    return new ServletMapping();
  }

  /**
   * servlet-mapping configuration.
   */
  @Configurable
  public void addServletMapping(ServletMapping servletMapping)
    throws ServletException
  {
    // log.fine("adding servlet mapping: " + servletMapping);
    servletMapping.setServletContext(getWebApp());

    if ((isFragment() || servletMapping.isAnnotation())
        && _servletMapper.containsServlet(servletMapping.getServletName())) {
      return;
    }

    servletMapping.init(_servletMapper);
  }

  /**
   * servlet-regexp configuration.
   */
  @Configurable
  public void addServletRegexp(ServletRegexp servletRegexp)
    throws ServletException, ClassNotFoundException
  {
    // servletRegexp.initRegexp(this, _servletMapper, _regexp);

    ServletMapping mapping = new ServletMapping();

    mapping.addURLRegexp(servletRegexp.getURLRegexp());
    mapping.setServletName(servletRegexp.getServletName());

    if (servletRegexp.getServletClass() != null) {
      mapping.setServletClass(servletRegexp.getServletClass());
    }

    mapping.setServletContext(getWebApp());
    servletRegexp.getBuilderProgram().configure(mapping);
    mapping.setStrictMapping(getStrictMapping());
    mapping.init(_servletMapper);

    //_servletMapper.addServletRegexp(mapping);
  }

  /**
   * session-config
   */
  public SessionManager createSessionConfig()
    throws Exception
  {
    if (_isInheritSession) {
      return new SessionManager(getWebApp());
    }

    SessionManager manager = getWebApp().getSessionManager();

    return manager;
  }

  /**
   * session-config
   */
  @Configurable
  public void addSessionConfig(SessionManager manager)
    throws ConfigException
  {
    if (_isInheritSession) {
      manager.close();
    }
  }

  /**
   * statistics-enable enables detailed statistics
   */
  @Configurable
  public void setStatisticsEnable(boolean isEnable)
  {
    _isStatisticsEnabled = isEnable;
  }

  public boolean isStatisticsEnabled()
  {
    return _isStatisticsEnabled;
  }

  /**
   * strict-mapping configuration
   */
  @Configurable
  public void setStrictMapping(boolean isStrict)
    throws ServletException
  {
    _isStrictMapping = isStrict;
  }

  /**
   * strict-mapping
   */
  public boolean getStrictMapping()
  {
    return _isStrictMapping;
  }

  /**
   * version configuration (spec)
   */
  @Configurable
  public void setVersion(String version)
  {    
    _servletVersion = version;
  }

  /**
   * version from the web.xml configuration
   */
  public String getVersion()
  {
    return _servletVersion;
  }

  /**
   * web-app-default for child web-apps
   */
  @Configurable
  public void addWebAppDefault(WebAppConfig config)
  {
    _webAppDefaultList.add(config);
  }

  /**
   * web-app-default for child web-apps
   */
  public ArrayList<WebAppConfig> getWebAppDefaultList()
  {
    return _webAppDefaultList;
  }

  /**
   * web-app child configuration
   */
  /*
  @Configurable
  public void addWebApp(WebAppConfig config)
    throws Exception
  {
    String contextPath = getWebApp().getContextPath();
    String prefix = config.getId();

    if (prefix == null || prefix.equals("") || prefix.equals("/"))
      throw new ConfigException(L.l("'{0}' is an illegal sub web-app id.",
                                    prefix));

    WebAppContainer container = getController().getContainer();
    DeployContainerImpl<WebApp,WebAppController> appGenerator;
    appGenerator = container.getWebAppGenerator();

    DeployGeneratorWebAppSingle deploy;
    deploy = new DeployGeneratorWebAppSingle(appGenerator,
                                             container, config);

    deploy.setURLPrefix(contextPath + prefix);
    // deploy.setParent(_controller);

    // XXX: The parent is added in the init()
    // _parent.addWebAppDeploy(gen);

    deploy.setParentWebApp(_controller);
    deploy.setParentClassLoader(getClassLoader());
    deploy.setContainer(container);

    for (WebAppConfig configDefault : _webAppDefaultList)
      deploy.addWebAppDefault(configDefault);

    String appDir = config.getRootDirectory();

    if (appDir == null)
      appDir = "./" + prefix;

    Path root = PathBuilder.lookupPath(appDir, null,
                                       getController().getRootDirectory());

    deploy.setRootDirectory(root);

    deploy.init();

    container.addDeploy(deploy);

    //_appGenerators.add(deploy);

    //deploy.deploy();
  }
  */

  /**
   * web-app-deploy for child web-apps
   */
  /*
  public DeployGeneratorWebAppExpand createWebAppDeploy()
  {
    return getController().getContainer().createWebAppDeploy();
  }
  */

  /**
   * web-app-deploy configuration for child web-apps
   */
  /*
  @Configurable
  public void addWebAppDeploy(DeployGeneratorWebAppExpand deploy)
    throws Exception
  {
    String contextPath = getController().getContextPath();
    String prefix = deploy.getURLPrefix();

    deploy.setURLPrefix(contextPath + prefix);
    deploy.setParent(_controller);

    // _parent.addWebAppDeploy(gen);

    deploy.setParentClassLoader(getClassLoader());
    // deploy.deploy();
    // XXX: The parent is added in the init()
    // server/10t3
    // _parent.addWebAppDeploy(deploy);

    for (WebAppConfig configDefault : _webAppDefaultList) {
      deploy.addWebAppDefault(configDefault);
    }

    Environment.addEnvironmentListener(deploy, getClassLoader());

    _appGenerators.add(deploy);
  }
  */

  /**
   * welcome-file-list configuration
   */
  @Configurable
  public void addWelcomeFileList(WelcomeFileList list)
  {
    ArrayList<String> fileList = list.getWelcomeFileList();

    _welcomeFileList = new ArrayList<String>(fileList);

    // _servletMapper.setWelcomeFileList(fileList);
  }

  public ArrayList<String> getWelcomeFileList()
  {
    return _welcomeFileList;
  }

  //
  // config utilities
  //

  public void add(SecurityConstraint constraint)
  {
    addSecurityConstraint(constraint);
  }

  public WelcomeFile getWelcomeFile()
  {
    // XXX: timing: needs to be in an init
    if (_welcomeFile == null) {
      _welcomeFile = new WelcomeFile(_welcomeFileList);
    }

    return _welcomeFile;
  }

  public void addServletDefaultMapper(ServletDefaultMapper map)
  {
    _servletMapper.addDefaultMapper(map);
  }

  T getWebApp()
  {
    if (_webApp == null) {
      _webApp = createWebApp();
    }

    return _webApp;
  }

  protected T createWebApp()
  {
    return (T) new WebApp(this);
  }

  public Collection<TaglibDescriptor> getTaglibs()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public void addFilter(WebFilter webFilter, String filterClassName)
    throws Exception
  {
    FilterMapping config = new FilterMapping();
    config.setFilterManager(_filterManager);
    config.setFilterClass(filterClassName);

    String filterName = webFilter.filterName();
    if ("".equals(filterName))
      filterName = filterClassName;

    config.setFilterName(filterName);

    boolean isMapping = false;

    if (webFilter.value().length > 0) {
      FilterMapping.URLPattern urlPattern = config.createUrlPattern();
      for (String url : webFilter.value()) { 
        urlPattern.addText(url);
      }

      urlPattern.init();

      isMapping = true;
    }

    if (webFilter.urlPatterns().length > 0) {
      FilterMapping.URLPattern urlPattern = config.createUrlPattern();

      for (String url : webFilter.urlPatterns()) {
        urlPattern.addText(url);
      }

      urlPattern.init();

      isMapping = true;
    }

    if (webFilter.servletNames().length > 0) {
      for (String servletName : webFilter.servletNames())
        config.addServletName(servletName);

      isMapping = true;
    }

    /* XXX: tck
    if (! isMapping) {
      throw new ConfigException(L.l("Annotation @WebFilter at '{0}' must specify either value, urlPatterns or servletNames", filterClassName));
    }
    */

    for (WebInitParam initParam : webFilter.initParams())
      config.setInitParam(initParam.name(), initParam.value());

    for (DispatcherType dispatcher : webFilter.dispatcherTypes())
      config.addDispatcher(dispatcher);

    config.setAsyncSupported(webFilter.asyncSupported());

    addFilterMapping(config);
  }

  private void initSecurityConstraints()
  {
    if (true) {
      return;
    }
    
    Map<String, ServletBuilder> servlets = _servletManager.getServlets();

    for (Map.Entry<String, ServletBuilder> entry : servlets.entrySet()) {
      ServletSecurityElement securityElement = entry.getValue().getSecurityElement();
      ServletBuilder servletBuilder = entry.getValue();
      
      if (securityElement == null) {
        continue;
      }

      /*
      ServletSecurity.EmptyRoleSemantic rootRoleSemantic
        = securityElement.getEmptyRoleSemantic();
        */

      final Set<String> patterns = _servletMapper.getUrlPatterns(entry.getKey());
      final Collection<HttpMethodConstraintElement> constraints
        = securityElement.getHttpMethodConstraints();

      if (constraints != null) {
        for (HttpMethodConstraintElement elt
             : securityElement.getHttpMethodConstraints()) {
          SecurityConstraint constraint = ConstraintManager.buildConstraint(elt);

          WebResourceCollection resources = new WebResourceCollection();
          resources.addHttpMethod(elt.getMethodName());

          if (patterns != null) {
            for (String pattern : patterns) {
              resources.addURLPattern(pattern);
              constraint.addURLPattern(pattern);
            }
          }

          constraint.addWebResourceCollection(resources);

          _constraintManager.addConstraint(servletBuilder.getName(), constraint);
        }
      }

      SecurityConstraint constraint = ConstraintManager.buildConstraint(securityElement);

      if (patterns != null) {
        for (String pattern : patterns) {
          constraint.addURLPattern(pattern);
        }
      }

      _constraintManager.addConstraint(servletBuilder.getName(), constraint);
    }
  }

  public ServletManager getServletManager()
  {
    return _servletManager;
  }

  public void addContentProgram(ConfigProgram program)
  {
    program.inject(getWebApp());
  }

  @Override
  public WebApp build()
  {
    WebApp webApp = getWebApp();
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);
      
      if (_classLoader instanceof DynamicClassLoader) {
        ((DynamicClassLoader) _classLoader).make();
      }
      
      initConstructor();

      //_filterManager.init();
      //_servletManager.init();

      initSecurityConstraints();

      webApp.init();
    } catch (Exception e) {
      webApp.setConfigException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return webApp;
  }

  protected void initConstructor()
  {
  }

  public ServletMapper getServletMapper()
  {
    return _servletMapper;
  }

  String getId()
  {
    return _controller.getId();
  }

  WebAppController getController()
  {
    return _controller;
  }

  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }

  @Override
  public void setConfigException(Throwable exn)
  {
    if (exn != null) {
      //exn.printStackTrace();

      getWebApp().setConfigException(exn);
    }
  }

  @Override
  public Throwable getConfigException()
  {
    return _configException;
  }

  HttpContainerServlet getHttpContainer()
  {
    return _httpContainer;
  }

  Host getHost()
  {
    return _host;
  }

  @Override
  public void preConfigInit()
  {
    initPermissions();
    getWebApp().preConfigInit();
  }
  
  private void initPermissions()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    try {
      PathImpl path = Vfs.lookup("META-INF/permissions.xml");
      
      initPermissions(path);
      
      Enumeration<URL> e = loader.getResources("/META-INF/permissions.xml");
      
      if (e == null) {
        return;
      }
      
      while (e.hasMoreElements()) {
        URL url = e.nextElement();
      
        path = Vfs.lookup(url);
        
        initPermissions(path);
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  private void initPermissions(PathImpl path)
    throws IOException
  {
    if (! path.canRead()) {
      return;
    }

    PermissionsConfig perm = new PermissionsConfig();
    
    // new ConfigXml().configure(perm, path);
  }

  void setRegexp(ArrayList<String> regexpValues)
  {
    getWebApp().setRegexp(regexpValues);
  }

  /*
  void setDynamicDeploy(boolean isDynamicDeploy)
  {
    getWebApp().setDynamicDeploy(isDynamicDeploy);
  }
  */

  /**
   * Set true for a dynamically deployed server.
   */
  public void setDynamicDeploy(boolean isDynamicDeploy)
  {
    _isDynamicDeploy = isDynamicDeploy;
  }

  /**
   * Set true for a dynamically deployed server.
   */
  public boolean isDynamicDeploy()
  {
    return _isDynamicDeploy;
  }

  public void setModuleName(String moduleName)
  {
    _moduleName = moduleName;
  }

  public String getModuleName()
  {
    return _moduleName;
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/http/webapp/resin-web-xml.rnc";
  }

  public boolean isServletAllowEL()
  {
    return _servletAllowEL;
  }

  ArrayList<DeployGenerator<WebApp,WebAppController>> getWebAppGenerators()
  {
    return _appGenerators;
  }

  public void deployWebAppGenerators()
  {
    WebAppContainer container = getController().getContainer();

    for (DeployGenerator<WebApp,WebAppController> gen : _appGenerators) {
      container.addDeploy(gen);
    }
  }

  public void removeWebAppGenerators()
  {
    WebAppContainer container = getController().getContainer();

    for (int i = _appGenerators.size() - 1; i >= 0; i--) {
      try {
        DeployGenerator<WebApp,WebAppController> deploy = _appGenerators.get(i);

        container.removeWebAppDeploy(deploy);
        deploy.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
  
  public Ordering createAbsoluteOrdering()
  {
    return _builderFragment.createAbsoluteOrdering();
  }

  public void initWebFragments()
  {
    _builderFragment.initWebFragments();
  }
  
  public WebAppBuilderFragment getBuilderFragment()
  {
    return _builderFragment;
  }
  
  public void setPre23Config(boolean isPre)
  {
    _isPre23Config = isPre;
  }

  public boolean hasPre23Config()
  {    
    return _isPre23Config;
  }

  public WebAppListeners getListenerManager()
  {
    return _listenerManager;
  }

  public ConstraintManager getConstraintManager()
  {
    return _constraintManager;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
