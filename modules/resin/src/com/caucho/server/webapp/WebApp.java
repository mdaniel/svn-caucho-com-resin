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

import com.caucho.amber.manager.AmberContainer;
import com.caucho.config.CauchoDeployment;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.inject.*;
import com.caucho.config.j2ee.PersistenceContextRefConfig;
import com.caucho.config.SchemaBean;
import com.caucho.config.el.WebBeansELResolver;
import com.caucho.config.types.*;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.jsp.JspServlet;
import com.caucho.jsp.cfg.JspConfig;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.jsp.cfg.JspTaglib;
import com.caucho.jsp.el.JspApplicationContextImpl;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.AlwaysModified;
import com.caucho.make.DependencyContainer;
import com.caucho.management.server.HostMXBean;
import com.caucho.naming.Jndi;
//import com.caucho.osgi.OsgiBundle;
//import com.caucho.osgi.OsgiManager;
import com.caucho.rewrite.RewriteFilter;
import com.caucho.rewrite.DispatchRule;
import com.caucho.security.Authenticator;
import com.caucho.security.Login;
import com.caucho.security.Deny;
import com.caucho.rewrite.IfSecure;
import com.caucho.rewrite.Not;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.deploy.EnvironmentDeployInstance;
import com.caucho.server.dispatch.*;
import com.caucho.server.host.Host;
import com.caucho.server.log.AbstractAccessLog;
import com.caucho.server.log.AccessLog;
import com.caucho.server.port.TcpConnection;
import com.caucho.server.port.ServerRequest;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.security.RoleMapManager;
import com.caucho.server.security.ConstraintManager;
import com.caucho.server.security.LoginConfig;
import com.caucho.server.security.SecurityConstraint;
import com.caucho.server.security.TransportConstraint;
import com.caucho.server.session.SessionManager;
import com.caucho.server.util.CauchoSystem;
//import com.caucho.soa.client.WebServiceClient;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.java.WorkDir;
import com.caucho.jsf.cfg.JsfPropertyGroup;

import javax.annotation.PostConstruct;
import javax.event.Observer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.inject.*;
import javax.inject.manager.Bean;
import javax.inject.manager.Initialized;
import javax.inject.manager.Manager;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.osgi.framework.BundleContext;

/**
 * Resin's webApp implementation.
 */
@Configurable
public class WebApp extends ServletContextImpl
  implements Dependency, EnvironmentBean, SchemaBean, DispatchBuilder,
             EnvironmentDeployInstance, java.io.Serializable
{
  private static final String DEFAULT_VERSION = "2.5";

  private static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebApp.class.getName());

  private static final int JSP_NONE = 0;
  private static final int JSP_1 = 1;
  private static final int JSP_2 = 2;

  private static EnvironmentLocal<AbstractAccessLog> _accessLogLocal
    = new EnvironmentLocal<AbstractAccessLog>("caucho.server.access-log");

  private static EnvironmentLocal<WebApp> _appLocal
    = new EnvironmentLocal<WebApp>("caucho.application");

  static String []_classLoaderHackPackages;

  private ClassLoader _parentClassLoader;

  // The environment class loader
  private EnvironmentClassLoader _classLoader;

  private Server _server;
  // The parent
  private WebAppContainer _parent;

  // web-app versioning
  private String _version;
  private WebApp _oldWebApp;
  private long _oldWebAppExpireTime;

  // The webApp entry
  private WebAppController _controller;

  // The webbeans container
  private InjectManager _webBeans;

  // The webApp directory.
  private final Path _appDir;

  // The context path
  private String _contextPath = "";

  // A description
  private String _description = "";

  private String _servletVersion;

  private boolean _isDynamicDeploy;
  private boolean _isDisableCrossContext;

  // Any war-generators.
  private ArrayList<DeployGenerator> _appGenerators
    = new ArrayList<DeployGenerator>();

  // Any web-app-default for children
  private ArrayList<WebAppConfig> _webAppDefaultList
    = new ArrayList<WebAppConfig>();

  // The servlet manager
  private ServletManager _servletManager;
  // The servlet mapper
  private ServletMapper _servletMapper;
  // True the mapper should be strict
  private boolean _isStrictMapping;
  // True if the servlet init-param is allowed to use EL
  private boolean _servletAllowEL = false;

  // The filter manager
  private FilterManager _filterManager;
  // The filter mapper
  private FilterMapper _filterMapper;
  // The filter mapper
  private FilterMapper _loginFilterMapper;
  // The include filter mapper
  private FilterMapper _includeFilterMapper;
  // The forward filter mapper
  private FilterMapper _forwardFilterMapper;
  // The error filter mapper
  private FilterMapper _errorFilterMapper;
  // True if includes are allowed to wrap a filter (forbidden by servlet spec)
  private boolean _dispatchWrapsFilters;

  // Transaction manager
  private TransactionManagerImpl _tm;

  // The session manager
  private SessionManager _sessionManager;
  // True if the session manager is inherited
  private boolean _isInheritSession;

  private String _characterEncoding;

  // The cache
  private AbstractCache _cache;

  private LruCache<String,FilterChainEntry> _filterChainCache
    = new LruCache<String,FilterChainEntry>(256);

  private UrlMap<CacheMapping> _cacheMappingMap = new UrlMap<CacheMapping>();

  private final Object _dispatcherCacheLock = new Object();
  private LruCache<String,RequestDispatcherImpl> _dispatcherCache;

  private Login _login;

  private RoleMapManager _roleMapManager;

  // The security constraints
  private ConstraintManager _constraintManager;

  // True for SSL secure.
  private boolean _isSecure;

  // Error pages.
  private ErrorPageManager _errorPageManager;

  // Any configuration exception
  private Throwable _configException;

  // dispatch mapping
  private RewriteDispatch _requestRewriteDispatch;
  private RewriteDispatch _includeRewriteDispatch;
  private RewriteDispatch _forwardRewriteDispatch;

  private LruCache<String,String> _realPathCache
    = new LruCache<String,String>(1024);
  // real-path mapping
  private RewriteRealPath _rewriteRealPath;

  // mime mapping
  private HashMap<String,String> _mimeMapping = new HashMap<String,String>();
  // locale mapping
  private HashMap<String,String> _localeMapping
    = new HashMap<String,String>();

  // List of all the listeners.
  private ArrayList<Listener> _listeners = new ArrayList<Listener>();

  // List of the ServletContextListeners from the configuration file
  private ArrayList<ServletContextListener> _webAppListeners
    = new ArrayList<ServletContextListener>();

  // List of the ServletContextAttributeListeners from the configuration file
  private ArrayList<ServletContextAttributeListener> _attributeListeners
    = new ArrayList<ServletContextAttributeListener>();

  // List of the ServletRequestListeners from the configuration file
  private ArrayList<ServletRequestListener> _requestListeners
    = new ArrayList<ServletRequestListener>();

  private ServletRequestListener []_requestListenerArray
    = new ServletRequestListener[0];

  // List of the ServletRequestAttributeListeners from the configuration file
  private ArrayList<ServletRequestAttributeListener> _requestAttributeListeners
    = new ArrayList<ServletRequestAttributeListener>();

  private ServletRequestAttributeListener []_requestAttributeListenerArray
    = new ServletRequestAttributeListener[0];

  private ArrayList<Validator> _resourceValidators
    = new ArrayList<Validator>();

  private DependencyContainer _invocationDependency;

  private AbstractAccessLog _accessLog;
  private Path _tempDir;

  private boolean _cookieHttpOnly;

  //  private OsgiBundle _osgiBundle;

  // special
  private int _jspState;
  private JspPropertyGroup _jsp;
  private JsfPropertyGroup _jsf;

  private ArrayList<JspTaglib> _taglibList;
  private JspApplicationContextImpl _jspApplicationContext;
  private HashMap<String,Object> _extensions = new HashMap<String,Object>();

  private MultipartForm _multipartForm;

  private ArrayList<String> _regexp;

  private boolean _isStatisticsEnabled;

  private long _shutdownWaitTime = 15000L;
  private long _activeWaitTime = 15000L;

  private long _idleTime = 2 * 3600 * 1000L;

  private final Object _countLock = new Object();
  private final Lifecycle _lifecycle;

  private final AtomicInteger _requestCount = new AtomicInteger();
  private long _lastRequestTime = Alarm.getCurrentTime();

  //
  // statistics
  //

  private long _status500CountTotal;
  private long _status500LastTime;

  /**
   * Creates the webApp with its environment loader.
   */
  public WebApp(Path rootDirectory)
  {
    this(new WebAppController("/", "/", rootDirectory, null));
  }

  /**
   * Creates the webApp with its environment loader.
   */
  WebApp(WebAppController controller)
  {
    _server = Server.getCurrent();
    
    String contextPath = controller.getContextPath();
    setContextPathId(contextPath);

    _controller = controller;
    _appDir = controller.getRootDirectory();

    setParent(controller.getContainer());

    _classLoader
      = EnvironmentClassLoader.create(controller.getParentClassLoader(),
				      "web-app:" + getId());
    
    _lifecycle = new Lifecycle(log, toString(), Level.INFO);

    initConstructor();
  }

  private void initConstructor()
  {
    try {
      // the JSP servlet needs to initialize the JspFactory
      JspServlet.initStatic();

      _classLoader.addParentPriorityPackages(_classLoaderHackPackages);

      // _classLoader.setId("web-app:" + getId());

      _appLocal.set(this, _classLoader);

      Vfs.setPwd(_appDir, _classLoader);
      WorkDir.setLocalWorkDir(_appDir.lookup("WEB-INF/work"), _classLoader);

      InjectManager inject = InjectManager.create(_classLoader);

      inject.addPath(_appDir.lookup("WEB-INF/beans.xml"));

      // map.put("app", _appVar);

      _servletManager = new ServletManager();
      _servletMapper = new ServletMapper();
      _servletMapper.setServletContext(this);
      _servletMapper.setServletManager(_servletManager);

      _filterManager = new FilterManager();
      _filterMapper = new FilterMapper();
      _filterMapper.setServletContext(this);
      _filterMapper.setFilterManager(_filterManager);

      _loginFilterMapper = new FilterMapper();
      _loginFilterMapper.setServletContext(this);
      _loginFilterMapper.setFilterManager(_filterManager);

      _includeFilterMapper = new FilterMapper();
      _includeFilterMapper.setServletContext(this);
      _includeFilterMapper.setFilterManager(_filterManager);

      _forwardFilterMapper = new FilterMapper();
      _forwardFilterMapper.setServletContext(this);
      _forwardFilterMapper.setFilterManager(_filterManager);

      _errorFilterMapper = new FilterMapper();
      _errorFilterMapper.setServletContext(this);
      _errorFilterMapper.setFilterManager(_filterManager);

      _constraintManager = new ConstraintManager();
      _errorPageManager = new ErrorPageManager(this);

      if (getParent() != null)
        _errorPageManager.setParent(getParent().getErrorPageManager());

      _invocationDependency = new DependencyContainer();
      _invocationDependency.add(this);

      _jspApplicationContext = new JspApplicationContextImpl(this);

      // validation
      if (CauchoSystem.isTesting()) {
      }
      else if (_appDir.equals(CauchoSystem.getResinHome())) {
        throw new ConfigException(L.l("web-app root-directory '{0}' can not be the same as resin.home\n", _appDir.getURL()));
      }
      else if (_parent != null
               && _appDir.equals(_parent.getRootDirectory())) {
        throw new ConfigException(L.l("web-app root-directory '{0}' can not be the same as the host root-directory\n", _appDir.getURL()));
      }
    } catch (Throwable e) {
      setConfigException(e);
    }
  }

  /**
   * Initialization before configuration
   */
  public void preConfigInit()
  {
    /*
    OsgiManager manager = _classLoader.createOsgiManager();

    _osgiBundle = new OsgiWebAppBundle(manager, this);

    InjectManager webBeans = InjectManager.create();

    webBeans.addSingleton(_osgiBundle.getBundleContext(),
			  CauchoDeployment.class, (String) null,
			  BundleContext.class);
    */
  }

  /**
   * Sets the parent container.
   */
  public void setParent(WebAppContainer parent)
  {
    _parent = parent;

    if (parent == null)
      return;
  }

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

  /**
   * Gets the parent container.
   */
  public WebAppContainer getParent()
  {
    return _parent;
  }

  /**
   * Returns the local webApp.
   */
  public static WebApp getLocal()
  {
    return getCurrent();
  }

  /**
   * Returns the local webApp.
   */
  public static WebApp getCurrent()
  {
    return _appLocal.get();
  }

  /**
   * Gets the dispatch server.
   */
  public DispatchServer getDispatchServer()
  {
    if (_parent != null)
      return _parent.getDispatchServer();
    else
      return null;
  }

  /**
   * Gets the dispatch server.
   */
  public Server getServer()
  {
    if (_parent != null && _parent.getDispatchServer() instanceof Server)
      return (Server) _parent.getDispatchServer();
    else
      return null;
  }

  /**
   * The id is the context path.
   */
  public void setId(String id)
  {
  }

  /**
   * The id is the context path.
   */
  private void setContextPathId(String id)
  {
    if (! id.equals("") && ! id.startsWith("/"))
      id = "/" + id;

    if (id.endsWith("/"))
      id = id.substring(0, id.length() - 1);

    setContextPath(id);
  }

  /**
   * Gets the environment class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Sets the environment class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    throw new IllegalStateException();
  }

  /**
   * Gets the environment class loader.
   */
  public EnvironmentClassLoader getEnvironmentClassLoader()
  {
    return _classLoader;
  }

  /**
   * Sets the redeploy-mode of the controller
   */
  @Configurable
  public void setRedeployMode(String mode)
  {
    if (_controller != null)
      _controller.setRedeployMode(mode);
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/webapp/resin-web-xml.rnc";
  }

  /**
   * Enables detailed statistics
   */
  @Configurable
  public void setStatisticsEnable(boolean isEnable)
  {
    _isStatisticsEnabled = isEnable;
  }

  /**
   * Sets the node for testing Servlet/JSP versions.
   */
  public void setConfigNode(org.w3c.dom.Node node)
  {
    String ns = node.getNamespaceURI();

    if (ns == null || ns.equals("")) {
      _jspState = JSP_1;
    }
  }

  /**
   * Gets the webApp directory.
   */
  public Path getAppDir()
  {
    return _appDir;
  }

  /**
   * Gets the dependency container
   */
  public DependencyContainer getInvocationDependency()
  {
    return _invocationDependency;
  }

  /**
   * Sets the regexp vars.
   */
  public void setRegexp(ArrayList<String> regexp)
  {
    _regexp = regexp;
  }

  /**
   * Gets the regexp vars.
   */
  public ArrayList<String> getRegexp()
  {
    return _regexp;
  }

  /**
   * Sets the document directory (app-dir).
   */
  public void setDocumentDirectory(Path appDir)
  {
    throw new ConfigException(L.l("Use <root-directory> instead of <document-directory>, because <document-directory> has been removed for Resin 4.0"));
  }

  /**
   * Sets the root directory (app-dir).
   */
  @Configurable
  public void setRootDirectory(Path appDir)
  {
  }

  /**
   * Sets the webApp directory.
   */
  public void setAppDir(Path appDir)
  {
    setRootDirectory(appDir);
  }

  /**
   * Returns the ObjectName.
   */
  public ObjectName getObjectName()
  {
    return _controller.getObjectName();
  }

  /**
   * Gets the context path
   */
  public String getContextPath()
  {
    return _contextPath;
  }

  /**
   * Sets the context path
   */
  private void setContextPath(String contextPath)
  {
    _contextPath = contextPath;

    if (getServletContextName() == null)
      setDisplayName(contextPath);
  }

  /**
   * Sets the servlet version.
   */
  @Configurable
  public void setVersion(String version)
  {
    _servletVersion = version;
  }

  /**
   * Returns the servlet version.
   */
  public String getVersion()
  {
    return _servletVersion;
  }

  /**
   * Sets the schema location.
   */
  public void setSchemaLocation(String location)
  {
  }

  @Configurable
  public void setDistributable(boolean isDistributable)
  {
  }

  /**
   * Gets the URL
   */
  public String getURL()
  {
    if (_parent != null)
      return _parent.getURL() + _contextPath;
    else
      return _contextPath;
  }

  /**
   * Gets the URL
   */
  public String getId()
  {
    if (_parent != null)
      return _parent.getId() + _contextPath;
    else
      return _contextPath;
  }

  /**
   * Gets the URL
   */
  public String getHostName()
  {
    if (_parent != null)
      return _parent.getHostName();
    else
      return null;
  }

  /**
   * Gets the URL
   */
  public HostMXBean getHostAdmin()
  {
    if (_parent != null && _parent.getHost() != null)
      return _parent.getHost().getAdmin();
    else
      return null;
  }

  /**
   * A user description of the web-app
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * A user description of the web-app
   */
  @Configurable
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the icon
   */
  @Configurable
  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Sets the servlet init-param EL enabling.
   */
  @Configurable
  public void setAllowServletEL(boolean allow)
  {
    _servletAllowEL = allow;
  }

  /**
   * If true, disables getContext().
   */
  @Configurable
  public void setDisableCrossContext(boolean isDisable)
  {
    _isDisableCrossContext = isDisable;
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
   * Adds a servlet configuration.
   */
  public ServletConfigImpl createServlet()
    throws ServletException
  {
    ServletConfigImpl config = new ServletConfigImpl();

    config.setServletContext(this);
    config.setAllowEL(_servletAllowEL);

    return config;
  }

  /**
   * Adds a servlet configuration.
   */
  @Configurable
  public void addServlet(ServletConfigImpl config)
    throws ServletException
  {
    config.setServletContext(this);

    _servletManager.addServlet(config);
  }

  /**
   * Returns the character encoding.
   */
  public String getCharacterEncoding()
  {
    return _characterEncoding;
  }

  /**
   * Set true if strict mapping.
   */
  @Configurable
  public void setStrictMapping(boolean isStrict)
    throws ServletException
  {
    _isStrictMapping = isStrict;
  }

  /**
   * Get the strict mapping setting.
   */
  public boolean getStrictMapping()
  {
    return _isStrictMapping;
  }

  /**
   * Lazy servlet validation.
   */
  @Configurable
  public void setLazyServletValidate(boolean isLazy)
  {
    _servletManager.setLazyValidate(isLazy);
  }

  public ServletMapping createServletMapping()
  {
    ServletMapping servletMapping = new ServletMapping();

    servletMapping.setServletContext(this);
    servletMapping.setStrictMapping(getStrictMapping());

    return servletMapping;
  }

  /**
   * Adds a servlet-mapping configuration.
   */
  @Configurable
  public void addServletMapping(ServletMapping servletMapping)
    throws ServletException
  {
    // log.fine("adding servlet mapping: " + servletMapping);
    servletMapping.setServletContext(this);

    servletMapping.init(_servletMapper);
  }

  /**
   * Adds a web service client.
   */
  /*
  public WebServiceClient createWebServiceClient()
  {
    return new WebServiceClient();
  }
  */

  /**
   * Adds a servlet-regexp configuration.
   */
  @Configurable
  public void addServletRegexp(ServletRegexp servletRegexp)
    throws ServletException, ClassNotFoundException
  {
    ServletMapping mapping = new ServletMapping();

    mapping.addURLRegexp(servletRegexp.getURLRegexp());
    mapping.setServletName(servletRegexp.getServletName());
    mapping.setServletClass(servletRegexp.getServletClass());
    mapping.setServletContext(this);
    servletRegexp.getBuilderProgram().configure(mapping);
    mapping.setStrictMapping(getStrictMapping());
    mapping.init(_servletMapper);

    //_servletMapper.addServletRegexp(mapping);
  }

  /**
   * Adds a filter configuration.
   */
  @Configurable
  public void addFilter(FilterConfigImpl config)
  {
    config.setServletContext(this);

    _filterManager.addFilter(config);
  }

  /**
   * Adds a filter-mapping configuration.
   */
  @Configurable
  public void addFilterMapping(FilterMapping filterMapping)
    throws ServletException
  {
    filterMapping.setServletContext(this);

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
   * Adds a persistence-context-ref configuration.
   */
  public void addPersistenceContextRef(PersistenceContextRefConfig persistenceContextRefConfig)
    throws ServletException
  {
    // XXX: TCK ejb30/persistence/ee/packaging/web/scope, needs a test case.

    log.fine("WebApp adding persistence context ref: " + persistenceContextRefConfig.getPersistenceContextRefName());

    String unitName = persistenceContextRefConfig.getPersistenceUnitName();

    log.fine("WebApp looking up entity manager: " + AmberContainer.getPersistenceContextJndiPrefix() + unitName);

    Object obj = Jndi.lookup(AmberContainer.getPersistenceContextJndiPrefix() + unitName);

    log.fine("WebApp found entity manager: " + obj);

    String contextRefName = persistenceContextRefConfig.getPersistenceContextRefName();

    try {
      Jndi.bindDeep("java:comp/env/" + contextRefName, obj);
    } catch (NamingException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Set true if includes wrap filters.
   */
  @Configurable
  public void setDispatchWrapsFilters(boolean wrap)
  {
    _dispatchWrapsFilters = wrap;
  }

  /**
   * Get true if includes wrap filters.
   */
  public boolean getDispatchWrapsFilters()
  {
    return _dispatchWrapsFilters;
  }

  /**
   * (compat) sets the directory servlet
   */
  public void setDirectoryServlet(String className)
    throws Exception
  {
    ServletConfigImpl config = new ServletConfigImpl();
    config.setServletName("directory");
    if (className.equals("none"))
      config.setServletClass("com.caucho.servlets.ErrorStatusServlet");
    else
      config.setServletClass(className);

    addServlet(config);
  }

  /**
   * Adds a welcome file list to the webApp.
   */
  @Configurable
  public void addWelcomeFileList(WelcomeFileList list)
  {
    ArrayList<String> fileList = list.getWelcomeFileList();

    _servletMapper.setWelcomeFileList(fileList);
  }

  /**
   * Configures the locale encoding.
   */
  @Configurable
  public LocaleEncodingMappingList createLocaleEncodingMappingList()
  {
    return new LocaleEncodingMappingList(this);
  }

  /**
   * Sets inherit session.
   */
  public void setInheritSession(boolean isInheritSession)
  {
    _isInheritSession = isInheritSession;
  }

  /**
   * Gets inherit session.
   */
  public boolean isInheritSession()
  {
    return _isInheritSession;
  }

  /**
   * Configures the session manager.
   */
  public SessionManager createSessionConfig()
    throws Exception
  {
    if (_isInheritSession)
      return new SessionManager(this);

    SessionManager manager = getSessionManager();

    return manager;
  }

  /**
   * Adds the session manager.
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
   * Sets an init-param
   */
  public InitParam createContextParam()
  {
    InitParam initParam = new InitParam();

    initParam.setAllowEL(_servletAllowEL);

    return initParam;
  }

  /**
   * Sets the context param
   */
  @Configurable
  public void addContextParam(InitParam initParam)
  {
    HashMap<String,String> map = initParam.getParameters();

    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      String value = map.get(key);

      setInitParameter(key, value);
    }
  }

  /**
   * Adds an error page
   */
  @Configurable
  public void addErrorPage(ErrorPage errorPage)
  {
    _errorPageManager.addErrorPage(errorPage);
  }

  /**
   * Sets the access log.
   */
  public AccessLog createAccessLog()
  {
    return new AccessLog();
  }

  /**
   * Sets the access log.
   */
  @Configurable
  public void setAccessLog(AbstractAccessLog log)
  {
    add(log);
  }

  /**
   * Allow custom access log
   */
  @Configurable
  public void add(AbstractAccessLog log)
  {
    _accessLog = log;

    _accessLogLocal.set(log);
  }

  /**
   * Adds a mime-mapping
   */
  @Configurable
  public void addMimeMapping(MimeMapping mimeMapping)
  {
    _mimeMapping.put(mimeMapping.getExtension(),
                     mimeMapping.getMimeType());
  }

  /**
   * Adds a locale-mapping
   */
  public void putLocaleEncoding(String locale, String encoding)
  {
    _localeMapping.put(locale.toLowerCase(), encoding);
  }

  /**
   * Returns the locale encoding.
   */
  public String getLocaleEncoding(Locale locale)
  {
    String encoding;

    String key = locale.toString();
    encoding = _localeMapping.get(key.toLowerCase());

    if (encoding != null)
      return encoding;

    if (locale.getVariant() != null) {
      key = locale.getLanguage() + '_' + locale.getCountry();
      encoding = _localeMapping.get(key.toLowerCase());
      if (encoding != null)
        return encoding;
    }

    if (locale.getCountry() != null) {
      key = locale.getLanguage();
      encoding = _localeMapping.get(key.toLowerCase());
      if (encoding != null)
        return encoding;
    }

    return Encoding.getMimeName(locale);
  }

  /**
   * Sets the login
   */
  public void setLoginConfig(LoginConfig loginConfig)
  {
    _login = loginConfig.getLogin();
  }

  /**
   * Sets the login
   */
  public void setLogin(Login login)
  {
    _login = login;
  }

  /**
   * Returns the RoleMapManager
   */
  public RoleMapManager getRoleMapManager()
  {
    return _roleMapManager;
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(RewriteFilter filter)
  {
    if (filter.isRequest()) {
      if (_requestRewriteDispatch == null)
	_requestRewriteDispatch = new RewriteDispatch(this);

      _requestRewriteDispatch.addAction(filter);
    }
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(DispatchRule rule)
  {
    if (rule.isRequest()) {
      if (_requestRewriteDispatch == null)
	_requestRewriteDispatch = new RewriteDispatch(this);

      _requestRewriteDispatch.addRule(rule);
    }
  }

  /**
   * Adds rewrite-dispatch (backwards compat).
   */
  public RewriteDispatch createRewriteDispatch()
  {
    return new RewriteDispatch(this);
  }

  /**
   * Adds rewrite-dispatch.
   */
  public void addRewriteDispatch(RewriteDispatch dispatch)
  {
    if (dispatch.isRequest())
      _requestRewriteDispatch = dispatch;
    
    if (dispatch.isInclude())
      _includeRewriteDispatch = dispatch;
    
    if (dispatch.isForward())
      _forwardRewriteDispatch = dispatch;
  }

  /**
   * Adds rewrite-real-path.
   */
  public RewriteRealPath createRewriteRealPath()
  {
    if (_rewriteRealPath == null)
      _rewriteRealPath = new RewriteRealPath(getAppDir());

    return _rewriteRealPath;
  }

  /**
   * Adds a path-mapping
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
   * Adds a security constraint
   */
  public void addSecurityConstraint(SecurityConstraint constraint)
  {
    _constraintManager.addConstraint(constraint);
  }

  public void add(SecurityConstraint constraint)
  {
    addSecurityConstraint(constraint);
  }

  /**
   * Adds a security role
   */
  public void addSecurityRole(SecurityRole role)
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
      Deny deny = new Deny();
      deny.addURLPattern("/*");
      deny.add(new Not(new IfSecure()));

      _constraintManager.addConstraint(deny);
    }
  }

  @Configurable
  public void addListener(Listener listener)
    throws Exception
  {
    if (! hasListener(listener.getListenerClass())) {
      _listeners.add(listener);

      if (_lifecycle.isStarting() || _lifecycle.isActive()) {
        addListenerObject(listener.createListenerObject(), true);
      }
    }
  }

  /**
   * Returns true if a listener with the given type exists.
   */
  public boolean hasListener(Class listenerClass)
  {
    for (int i = 0; i < _listeners.size(); i++) {
      Listener listener = _listeners.get(i);

      if (listenerClass.equals(listener.getListenerClass()))
        return true;
    }

    return false;
  }

  /**
   * Adds the listener object.
   */
  private void addListenerObject(Object listenerObj, boolean start)
  {
    if (listenerObj instanceof ServletContextListener) {
      ServletContextListener scListener = (ServletContextListener) listenerObj;
      _webAppListeners.add(scListener);

      if (start) {
        ServletContextEvent event = new ServletContextEvent(this);

        try {
          scListener.contextInitialized(event);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    if (listenerObj instanceof ServletContextAttributeListener)
      addAttributeListener((ServletContextAttributeListener) listenerObj);

    if (listenerObj instanceof ServletRequestListener) {
      _requestListeners.add((ServletRequestListener) listenerObj);

      _requestListenerArray = new ServletRequestListener[_requestListeners.size()];
      _requestListeners.toArray(_requestListenerArray);
    }

    if (listenerObj instanceof ServletRequestAttributeListener) {
      _requestAttributeListeners.add((ServletRequestAttributeListener) listenerObj);

      _requestAttributeListenerArray = new ServletRequestAttributeListener[_requestAttributeListeners.size()];
      _requestAttributeListeners.toArray(_requestAttributeListenerArray);
    }

    if (listenerObj instanceof HttpSessionListener)
      getSessionManager().addListener((HttpSessionListener) listenerObj);

    if (listenerObj instanceof HttpSessionAttributeListener)
      getSessionManager().addAttributeListener((HttpSessionAttributeListener) listenerObj);

    if (listenerObj instanceof HttpSessionActivationListener)
      getSessionManager().addActivationListener((HttpSessionActivationListener) listenerObj);
  }

  /**
   * Returns the request listeners.
   */
  public ServletRequestListener []getRequestListeners()
  {
    return _requestListenerArray;
  }

  /**
   * Returns the request attribute listeners.
   */
  public ServletRequestAttributeListener []getRequestAttributeListeners()
  {
    return _requestAttributeListenerArray;
  }

  /**
   * Adds a ResourceRef validator.
   */
  public void addResourceRef(ResourceRef ref)
  {
    _resourceValidators.add(ref);
  }

  // special config

  /**
   * Multipart form config.
   */
  @Configurable
  public MultipartForm createMultipartForm()
  {
    if (_multipartForm == null)
      _multipartForm = new MultipartForm();

    return _multipartForm;
  }

  /**
   * Returns true if multipart forms are enabled.
   */
  public boolean doMultipartForm()
  {
    return _multipartForm != null && _multipartForm.isEnable();
  }

  /**
   * Returns the form upload max.
   */
  public long getFormUploadMax()
  {
    if (_multipartForm != null)
      return _multipartForm.getUploadMax();
    else
      return -1;
  }

  /**
   * Returns the access log
   */
  public AbstractAccessLog getAccessLog()
  {
    return _accessLog;
  }

  /**
   * Sets the temporary directory
   */
  public void setTempDir(Path path)
  {
    _tempDir = path;
  }

  /**
   * jsp configuration
   */
  @Configurable
  public JspPropertyGroup createJsp()
  {
    if (_jsp == null) {
      _jsp = new JspPropertyGroup();
    }

    return _jsp;
  }

  /**
   * Returns the JSP configuration.
   */
  public JspPropertyGroup getJsp()
  {
    return _jsp;
  }

  /**
   * jsf configuration
   */
  public JsfPropertyGroup createJsf()
  {
    if (_jsf == null)
      _jsf = new JsfPropertyGroup();

    return _jsf;
  }

  /**
   * Returns the JSF configuration
   */
  @Configurable
  public JsfPropertyGroup getJsf() {
    return _jsf;
  }

  /**
   * Returns the JspApplicationContext for EL evaluation.
   */
  public JspApplicationContextImpl getJspApplicationContext()
  {
    return _jspApplicationContext;
  }

  /**
   * Returns true for JSP 1.x
   */
  public boolean hasPre23Config()
  {
    return _jspState == JSP_1;
  }

  /**
   * taglib configuration
   */
  @Configurable
  public void addTaglib(JspTaglib taglib)
  {
    if (_taglibList == null) {
      _taglibList = new ArrayList<JspTaglib>();
    }

    _taglibList.add(taglib);
  }

  /**
   * Returns the taglib configuration.
   */
  public ArrayList<JspTaglib> getTaglibList()
  {
    return _taglibList;
  }

  public JspConfig createJspConfig()
  {
    return new JspConfig(this);
  }

  /**
   * jsp-config configuration
   */
  public void addJspConfig(JspConfig config)
  {
    _extensions.put("jsp-config", config);
  }

  /**
   * Returns an extension.
   */
  public Object getExtension(String key)
  {
    return _extensions.get(key);
  }

  /**
   * ejb-ref configuration
   */
  public EjbRef createEjbRef()
  {
    if (_controller != null && _controller.getArchivePath() != null)
      return new EjbRef(_controller.getArchivePath());
    else
      return new EjbRef();
  }

  /**
   * ejb-local-ref configuration
   */
  public EjbLocalRef createEjbLocalRef()
  {
    if (_controller != null && _controller.getArchivePath() != null)
      return new EjbLocalRef(_controller.getArchivePath());
    else
      return new EjbLocalRef();
  }

  /**
   * Sets the war-expansion
   */
  public WebAppExpandDeployGenerator createWebAppDeploy()
  {
    return _parent.createWebAppDeploy();
  }

  /**
   * Adds a war generator
   */
  @Configurable
  public void addWebAppDeploy(WebAppExpandDeployGenerator deploy)
    throws Exception
  {
    String contextPath = getContextPath();
    String prefix = deploy.getURLPrefix();

    deploy.setURLPrefix(contextPath + prefix);
    deploy.setParent(_controller);

    // _parent.addWebAppDeploy(gen);

    deploy.setParentClassLoader(getClassLoader());
    // deploy.deploy();
    // XXX: The parent is added in the init()
    // server/10t3
    // _parent.addWebAppDeploy(deploy);

    for (WebAppConfig configDefault : _webAppDefaultList)
      deploy.addWebAppDefault(configDefault);

    Environment.addEnvironmentListener(deploy, getClassLoader());

    _appGenerators.add(deploy);
  }

  /**
   * Adds a web-app default
   */
  @Configurable
  public void addWebAppDefault(WebAppConfig config)
  {
    _webAppDefaultList.add(config);
  }

  /**
   * Adds a web-app default
   */
  public ArrayList<WebAppConfig> getWebAppDefaultList()
  {
    return _webAppDefaultList;
  }

  /**
   * Adds a sub web-app
   */
  @Configurable
  public void addWebApp(WebAppConfig config)
    throws Exception
  {
    String contextPath = getContextPath();
    String prefix = config.getId();

    if (prefix == null || prefix.equals("") || prefix.equals("/"))
      throw new ConfigException(L.l("'{0}' is an illegal sub web-app id.",
                                    prefix));

    WebAppContainer container = _parent;
    DeployContainer<WebAppController> appGenerator;
    appGenerator = _parent.getWebAppGenerator();

    WebAppSingleDeployGenerator deploy;
    deploy = new WebAppSingleDeployGenerator(appGenerator,
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

    String appDir = config.getDocumentDirectory();

    if (appDir == null)
      appDir = "./" + prefix;

    Path root = PathBuilder.lookupPath(appDir, null, getAppDir());

    deploy.setRootDirectory(root);

    deploy.init();

    _parent.addDeploy(deploy);

    //_appGenerators.add(deploy);

    //deploy.deploy();
  }

  /**
   * Sets the config exception.
   */
  public void setConfigException(Throwable e)
  {
    if (e != null) {
      Throwable e1 = e;
      for (;
           e1 != null
	     && ! (e1 instanceof ConfigException)
             && e1.getCause() != null
             && e1.getCause() != e1;
           e1 = e1.getCause()) {
      }
      
      if (e1 != null) {
	if (e1 instanceof ConfigException) {
	  if (log.isLoggable(Level.FINE))
	    log.log(Level.WARNING, e1.toString(), e1);
	  else
	    log.warning(e1.getMessage());
	}
	else {
	  log.log(Level.WARNING, e1.toString(), e1);
	}
      }
    }
    
    if (_configException == null)
      _configException = e;

    // server/13l8
    if (e != null) { // && _invocationDependency != null) {
      // _invocationDependency.add
      // _invocationDependency.clearModified();

      _classLoader.addDependency(AlwaysModified.create());
    }
  }

  /**
   * Gets the config exception.
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the current cluster.
   */
  public Cluster getCluster()
  {
    return getServer().getCluster();
  }

  /**
   * Returns true if should ignore client disconnect.
   */
  public boolean isIgnoreClientDisconnect()
  {
    DispatchServer server = getDispatchServer();

    if (server == null)
      return true;
    else
      return server.isIgnoreClientDisconnect();
  }

  /**
   * Sets the delay time waiting for requests to end.
   */
  @Configurable
  public void setShutdownWaitMax(Period wait)
  {
    _shutdownWaitTime = wait.getPeriod();

    Resin resin = Resin.getLocal();
    if (resin != null &&
        resin.getShutdownWaitMax() < _shutdownWaitTime) {
      log.warning(L.l("web-app shutdown-wait-max '{0}' is longer than resin shutdown-wait-max '{1}'.",
                      _shutdownWaitTime,
                      resin.getShutdownWaitMax()));
    }
  }

  /**
   * Sets the delay time waiting for a restart
   */
  @Configurable
  public void setActiveWaitTime(Period wait)
  {
    _activeWaitTime = wait.getPeriod();
  }

  /**
   * Sets the delay time waiting for requests to end.
   */
  @Configurable
  public void setIdleTime(Period idle)
  {
    _idleTime = idle.getPeriod();
  }

  /**
   * Backwards compatability for config-file.
   */
  public void addConfigFile(Path path)
    throws Exception
  {
    com.caucho.config.core.ResinImport rImport;
    rImport = new com.caucho.config.core.ResinImport();
    rImport.setPath(path);
    rImport.setOptional(true);
    rImport.setParent(this);
    rImport.init();

    log.config("<config-file> is deprecated.  Please use resin:import.");
  }

  /**
   * Returns true if the webApp is active.
   */
  public String getState()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Returns true if it's init.
   */
  public boolean isInit()
  {
    return _lifecycle.isInit() || _configException != null;
  }

  /**
   * Returns true if it's in the middle of initializing
   */
  public boolean isInitializing()
  {
    return _lifecycle.isBeforeActive();
  }

  /**
   * Returns true if the webApp is active.
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns true if it's closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isDestroyed();
  }

  public static ServletRequest getThreadRequest()
  {
    ServerRequest serverRequest = TcpConnection.getCurrentRequest();

    if (serverRequest instanceof ServletRequest)
      return (ServletRequest) serverRequest;
    else
      return null;
  }

  /**
   * Initializes.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (! _lifecycle.toInitializing())
      return;

    try {
      _classLoader.setId("web-app:" + getId());

      _invocationDependency.setCheckInterval(getEnvironmentClassLoader().getDependencyCheckInterval());

      if (_tempDir == null)
        _tempDir = (Path) Environment.getLevelAttribute("caucho.temp-dir");

      if (_tempDir == null) {
        _tempDir = getAppDir().lookup("WEB-INF/tmp");

        if (getAppDir().lookup("WEB-INF").isDirectory())
          _tempDir.mkdirs();
      }
      else
        _tempDir.mkdirs();

      setAttribute("javax.servlet.context.tempdir", new File(_tempDir.getNativePath()));

      FilterChainBuilder securityBuilder = _constraintManager.getFilterBuilder();

      if (securityBuilder != null)
        _filterMapper.addTopFilter(securityBuilder);

      _cache = (AbstractCache) Environment.getAttribute("caucho.server.cache");

      for (int i = 0; i < _appGenerators.size(); i++)
        _parent.addDeploy(_appGenerators.get(i));

      _classLoader.setId("web-app:" + getId());

      _webBeans = InjectManager.getCurrent();

      try {
	// server/1a36
	
	Authenticator auth = _webBeans.getInstanceByType(Authenticator.class);

	setAttribute("caucho.authenticator", auth);
      } catch (Exception e) {
	log.finest(e.toString());
      }

      try {
	_login = _webBeans.getInstanceByType(Login.class);

	setAttribute("caucho.login", _login);
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
      }

      _webBeans.addObserver(new WebBeansObserver(),
			    Manager.class,
			    new AnnotationLiteral<Initialized>() {});

      WebAppController parent = null;
      if (_controller != null)
        parent = _controller.getParent();
      if (_isInheritSession && parent != null
	  && _sessionManager != parent.getWebApp().getSessionManager()) {
        SessionManager sessionManager = _sessionManager;
        _sessionManager = parent.getWebApp().getSessionManager();

        if (sessionManager != null)
          sessionManager.close();
      }

      if (_server != null) {
	if (getSessionManager() != null)
	  getSessionManager().init();
      }
      
      _roleMapManager = RoleMapManager.create();

      _characterEncoding = CharacterEncoding.getLocalEncoding();

      for (int i = 0; i < _resourceValidators.size(); i++) {
        Validator validator = _resourceValidators.get(i);

        validator.validate();
      }
    } finally {
      _lifecycle.toInit();
    }
  }

  public WebAppAdmin getAdmin()
  {
    return _controller.getAdmin();
  }

  public void start()
  {
    if (! _lifecycle.isAfterInit())
      throw new IllegalStateException(L.l("webApp must be initialized before starting.  Currently in state {0}.", _lifecycle.getStateName()));

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    boolean isOkay = true;

    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStarting())
        return;

      isOkay = false;

      if (_accessLog == null)
        _accessLog = _accessLogLocal.get();

      long interval = _classLoader.getDependencyCheckInterval();
      _invocationDependency.setCheckInterval(interval);

      if (_parent != null)
        _invocationDependency.add(_parent.getWebAppGenerator());

      // Sets the last modified time so the app won't immediately restart
      _invocationDependency.clearModified();
      _classLoader.clearModified();

      String serverId = (String) new EnvironmentLocal("caucho.server-id").get();
      if (serverId != null)
        setAttribute("caucho.server-id", serverId);

      _classLoader.start();

      // configuration exceptions discovered by resources like
      // the persistence manager
      if (_configException == null)
        _configException = Environment.getConfigException();

      try {
	if (getSessionManager() != null)
	  getSessionManager().start();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      _jspApplicationContext.addELResolver(new WebBeansELResolver());
      
      ServletContextEvent event = new ServletContextEvent(this);

      for (Listener listener : _listeners) {
        try {
          addListenerObject(listener.createListenerObject(), false);
        } catch (Exception e) {
          throw ConfigException.create(e);
        }
      }

      for (int i = 0; i < _webAppListeners.size(); i++) {
        ServletContextListener listener = _webAppListeners.get(i);

        try {
          listener.contextInitialized(event);
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      try {
        _filterManager.init();
        _servletManager.init();
      } catch (Exception e) {
        setConfigException(e);
      }

      if (_parent instanceof Host) {
        Host host = (Host) _parent;

        host.setConfigETag(null);
      }

      _lifecycle.toActive();

      clearCache();

      isOkay = true;
    } finally {
      if (! isOkay)
        _lifecycle.toError();

      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns true if the webApp has been modified.
   */
  public boolean isModified()
  {
    // server/13l8

    // _configException test is needed so compilation failures will force
    // restart
    if (_lifecycle.isAfterActive())
      return true;
    else if (_classLoader.isModified())
      return true;
    else
      return false;
  }

  /**
   * Returns true if the webApp has been modified.
   */
  public boolean isModifiedNow()
  {
    // force check
    _classLoader.isModifiedNow();
    _invocationDependency.isModifiedNow();

    return isModified();
  }

  /**
   * Log the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    if (_lifecycle.isAfterActive()) {
      // closed web-apps don't modify (XXX: errors?)
      return true;
    }
    else
      return _classLoader.logModified(log);
  }

  /**
   * Returns true if the webApp deployed with an error.
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Returns true if the deployment is idle.
   */
  public boolean isDeployIdle()
  {
    if (_idleTime < 0)
      return false;
    else
      return _lastRequestTime + _idleTime < Alarm.getCurrentTime();
  }

  /**
   * Returns the servlet context for the URI.
   */
  public ServletContext getContext(String uri)
  {
    if (uri == null)
      throw new IllegalArgumentException(L.l("getContext URI must not be null."));
    else if (uri.startsWith("/")) {
    }

    else if (uri.equals(""))
      uri = "/";

    else
      throw new IllegalArgumentException(L.l("getContext URI '{0}' must be absolute.", uri));

    try {
      if (_isDisableCrossContext)
        return uri.startsWith(getContextPath()) ? this : null;
      else if (_parent != null)
        return _parent.findSubWebAppByURI(uri);
      else
        return this;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the best matching servlet pattern.
   */
  public String getServletPattern(String uri)
  {
    return _servletMapper.getServletPattern(uri);
  }

  /**
   * Returns the best matching servlet pattern.
   */
  public ArrayList<String> getServletMappingPatterns()
  {
    return _servletMapper.getURLPatterns();
  }

  /**
   * Returns the best matching servlet pattern.
   */
  public ArrayList<String> getServletIgnoreMappingPatterns()
  {
    return _servletMapper.getIgnorePatterns();
  }

  /**
   * Fills the servlet instance.  (Generalize?)
   */
  public Invocation buildInvocation(Invocation invocation)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    thread.setContextClassLoader(getClassLoader());
    try {
      FilterChain chain = null;

      if (_configException != null) {
        chain = new ExceptionFilterChain(_configException);
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());
	
        return invocation;
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " returned 503 busy for '" + invocation.getRawURI() + "'");
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        chain = new ErrorFilterChain(code);
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
	else if (_requestRewriteDispatch != null)
	  isCache = false;

        if (isCache)
          entry = _filterChainCache.get(invocation.getContextURI());

        if (entry != null && ! entry.isModified()) {
          chain = entry.getFilterChain();
	  invocation.setServletName(entry.getServletName());
        } else {
          chain = _servletMapper.mapServlet(invocation);

          if (_requestRewriteDispatch != null) {
	    FilterChain newChain
	      = _requestRewriteDispatch.map(invocation.getContextURI(),
					    invocation.getQueryString(),
					    chain);

	    chain = newChain;
          }

	  // server/13s[o-r]
	  _filterMapper.buildDispatchChain(invocation, chain);

          chain = invocation.getFilterChain();

          entry = new FilterChainEntry(chain, invocation);
          chain = entry.getFilterChain();

          if (isCache)
            _filterChainCache.put(invocation.getContextURI(), entry);
        }

        // the cache must be outside of the WebAppFilterChain because
        // the CacheListener in ServletInvocation needs the top to
        // be a CacheListener.  Otherwise, the cache won't get lru.

        if (_isStatisticsEnabled)
          chain = new StatisticsFilterChain(chain, this);

        WebAppFilterChain webAppChain = new WebAppFilterChain(chain, this);

        webAppChain.setSecurityRoleMap(invocation.getSecurityRoleMap());

	// TCK: cache needs to be outside because the cache flush conflicts
	// with the request listener destroy callback
        // top-level filter elements
        if (_cache != null)
          chain = _cache.createFilterChain(webAppChain, this);

        invocation.setFilterChain(chain);
        invocation.setPathInfo(entry.getPathInfo());
        invocation.setServletPath(entry.getServletPath());
      }

      if (_oldWebApp != null
	  && Alarm.getCurrentTime() < _oldWebAppExpireTime) {
	Invocation oldInvocation = new Invocation();
	oldInvocation.copyFrom(invocation);
	oldInvocation.setWebApp(_oldWebApp);

	_oldWebApp.buildInvocation(oldInvocation);
	
	invocation = new VersionInvocation(invocation, this,
					   oldInvocation,
					   oldInvocation.getWebApp(),
					   _oldWebAppExpireTime);
      }

      return invocation;
    } catch (Throwable e) {
      FilterChain chain = new ExceptionFilterChain(e);
      chain = new WebAppFilterChain(chain, this);
      invocation.setDependency(AlwaysModified.create());
      invocation.setFilterChain(chain);

      return invocation;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Clears all caches, including the invocation cache, the filter cache, and the proxy cache.
   */
  public void clearCache()
  {
    DispatchServer server = getDispatchServer();

    if (server != null)
      server.clearCache();
    
    WebAppContainer parent = _parent;

    if (parent != null)
      parent.clearCache();
    
    // server/1kg1
    synchronized (_filterChainCache) {
      _filterChainCache.clear();
      _dispatcherCache = null;
    }
  }
  /**
   * Fills the invocation for an include request.
   */
  public void buildIncludeInvocation(Invocation invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _includeFilterMapper);
  }

  /**
   * Fills the invocation for a forward request.
   */
  public void buildForwardInvocation(Invocation invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _forwardFilterMapper);
  }

  /**
   * Fills the invocation for an error request.
   */
  public void buildErrorInvocation(Invocation invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _errorFilterMapper);
  }

  /**
   * Fills the invocation for a login request.
   */
  public void buildLoginInvocation(Invocation invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _loginFilterMapper);
  }

  /**
   * Fills the invocation for a rewrite-dispatch/dispatch request.
   */
  public void buildDispatchInvocation(Invocation invocation)
    throws ServletException
  {
    buildDispatchInvocation(invocation, _filterMapper);
  }

  /**
   * Fills the invocation for subrequests.
   */
  public void buildDispatchInvocation(Invocation invocation,
                                      FilterMapper filterMapper)
    throws ServletException
  {
    invocation.setWebApp(this);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    thread.setContextClassLoader(getClassLoader());
    try {
      FilterChain chain;

      if (_configException != null) {
        chain = new ExceptionFilterChain(_configException);
        invocation.setDependency(AlwaysModified.create());
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
        Exception exn = new UnavailableException(L.l("'{0}' is not currently available.",
                                                     getContextPath()));
        chain = new ExceptionFilterChain(exn);
        invocation.setDependency(AlwaysModified.create());
      }
      else {
        chain = _servletMapper.mapServlet(invocation);

	if (_includeRewriteDispatch != null
	    && filterMapper == _includeFilterMapper) {
	  chain = _includeRewriteDispatch.map(invocation.getContextURI(),
					      invocation.getQueryString(),
					      chain);
	}
	else if (_forwardRewriteDispatch != null
		 && filterMapper == _forwardFilterMapper) {
	  chain = _forwardRewriteDispatch.map(invocation.getContextURI(),
					      invocation.getQueryString(),
					      chain);
	}

        filterMapper.buildDispatchChain(invocation, chain);

        chain = invocation.getFilterChain();

        chain = new DispatchFilterChain(chain, this, invocation);

        if (_cache != null && filterMapper == _includeFilterMapper) {
          chain = _cache.createFilterChain(chain, this);
        }
      }

      invocation.setFilterChain(chain);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      FilterChain chain = new ExceptionFilterChain(e);
      invocation.setFilterChain(chain);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getRequestDispatcher(String url)
  {
    if (url == null)
      throw new IllegalArgumentException(L.l("request dispatcher url can't be null."));
    else if (! url.startsWith("/"))
      throw new IllegalArgumentException(L.l("request dispatcher url '{0}' must be absolute", url));

    RequestDispatcherImpl disp = getDispatcherCache().get(url);

    if (disp != null && ! disp.isModified())
      return disp;

    Invocation includeInvocation = new SubInvocation();
    Invocation forwardInvocation = new SubInvocation();
    Invocation errorInvocation = new SubInvocation();
    Invocation dispatchInvocation = new SubInvocation();
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = escapeURL(_contextPath + url);

    try {
      decoder.splitQuery(includeInvocation, rawURI);
      decoder.splitQuery(forwardInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);
      decoder.splitQuery(dispatchInvocation, rawURI);

      if (_parent != null) {
        _parent.buildIncludeInvocation(includeInvocation);
        _parent.buildForwardInvocation(forwardInvocation);
        _parent.buildErrorInvocation(errorInvocation);
        _parent.buildDispatchInvocation(dispatchInvocation);
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
	throw new IllegalStateException(L.l("'{0}' is restarting and is not yet ready to receive requests",
					    _contextPath));
      }
      else {
        FilterChain chain = _servletMapper.mapServlet(includeInvocation);
        _includeFilterMapper.buildDispatchChain(includeInvocation, chain);
        includeInvocation.setWebApp(this);

        chain = _servletMapper.mapServlet(forwardInvocation);
        _forwardFilterMapper.buildDispatchChain(forwardInvocation, chain);
        forwardInvocation.setWebApp(this);

        chain = _servletMapper.mapServlet(errorInvocation);
        _errorFilterMapper.buildDispatchChain(errorInvocation, chain);
        errorInvocation.setWebApp(this);

        chain = _servletMapper.mapServlet(dispatchInvocation);
        _filterMapper.buildDispatchChain(dispatchInvocation, chain);
        dispatchInvocation.setWebApp(this);
      }

      disp = new RequestDispatcherImpl(includeInvocation,
                                       forwardInvocation,
                                       errorInvocation,
				       dispatchInvocation,
                                       this);

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
   * Access logging for high-level errors
   */
  public void accessLog(HttpServletRequest req, HttpServletResponse res)
    throws IOException
  {
    AbstractAccessLog log = getAccessLog();

    if (log != null)
      log.log(req, res, this);
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

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getLoginDispatcher(String url)
  {
    if (url == null)
      throw new IllegalArgumentException(L.l("request dispatcher url can't be null."));
    else if (! url.startsWith("/"))
      throw new IllegalArgumentException(L.l("request dispatcher url '{0}' must be absolute", url));

    Invocation loginInvocation = new Invocation();
    Invocation errorInvocation = new Invocation();
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = _contextPath + url;

    try {
      decoder.splitQuery(loginInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);

      if (! _lifecycle.waitForActive(_activeWaitTime)) {
	throw new IllegalStateException(L.l("'{0}' is restarting and it not yet ready to receive requests",
					    _contextPath));
      }
      else if (_parent != null) {
        _parent.buildInvocation(loginInvocation);
        _parent.buildErrorInvocation(errorInvocation);
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
                                       this);
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
      FilterChain chain = _servletManager.createServletChain(servletName);

      FilterChain includeChain
        = _includeFilterMapper.buildFilterChain(chain, servletName);
      FilterChain forwardChain
        = _forwardFilterMapper.buildFilterChain(chain, servletName);

      return new NamedDispatcherImpl(includeChain, forwardChain, null, this);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);

      return null;
    }
  }

  /**
   * Maps from a URI to a real path.
   */
  @Override
  public String getRealPath(String uri)
  {
    if (uri == null)
      throw new NullPointerException();
    
    String realPath = _realPathCache.get(uri);

    if (realPath != null)
      return realPath;

    String fullURI = getContextPath() + "/" + uri;

    try {
      fullURI = InvocationDecoder.normalizeUri(fullURI);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    WebApp app = (WebApp) getContext(fullURI);

    if (app == null)
      return null;

    String cp = app.getContextPath();
    String tail = fullURI.substring(cp.length());

    realPath = app.getRealPathImpl(tail);

    if (log.isLoggable(Level.FINEST))
      log.finest("real-path " + uri + " -> " + realPath);

    _realPathCache.put(uri, realPath);

    return realPath;
  }

  /**
   * Maps from a URI to a real path.
   */
  public String getRealPathImpl(String uri)
  {
    return createRewriteRealPath().mapToRealPath(uri);
  }

  /**
   * Returns the mime type for a uri
   */
  public String getMimeType(String uri)
  {
    if (uri == null)
      return null;

    String fullURI = getContextPath() + "/" + uri;

    try {
      fullURI = InvocationDecoder.normalizeUri(fullURI);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    WebApp app = (WebApp) getContext(fullURI);

    if (app == null)
      return null;

    int p = uri.lastIndexOf('.');

    if (p < 0)
      return null;
    else
      return app.getMimeTypeImpl(uri.substring(p));
  }

  /**
   * Maps from a URI to a real path.
   */
  public String getMimeTypeImpl(String ext)
  {
    return _mimeMapping.get(ext);
  }

  /**
   * Error logging
   *
   * @param message message to log
   * @param e stack trace of the error
   */
  public void log(String message, Throwable e)
  {
    if (e != null)
      log.log(Level.WARNING, this + " " + message, e);
    else
      log.info(this + " " + message);
  }

  /**
   * Gets the login manager.
   */
  public Login getLogin()
  {
    return _login;
  }

  /**
   * Gets the authenticator
   */
  public Authenticator getAuthenticator()
  {
    Login login = getLogin();

    if (login != null)
      return login.getAuthenticator();
    else
      return null;
  }

  /**
   * Gets the session manager.
   */
  public SessionManager getSessionManager()
  {
    if (_sessionManager == null) {
      if (_lifecycle.isStopped())
        throw new IllegalStateException(L.l("Resin is shutting down."));

      if (_isInheritSession && _parent != null)
        _sessionManager = _parent.getSessionManager();

      if (_sessionManager == null) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();

        try {
          thread.setContextClassLoader(getClassLoader());

          _sessionManager = new SessionManager(this);
        } catch (Throwable e) {
          throw ConfigException.create(e);
        } finally {
          thread.setContextClassLoader(oldLoader);
        }
      }
    }

    return _sessionManager;
  }

  /**
   * Gets the error page manager.
   */
  public ErrorPageManager getErrorPageManager()
  {
    return _errorPageManager;
  }

  /**
   * Called when a request starts the webApp.
   */
  final boolean enterWebApp()
  {
    _requestCount.incrementAndGet();
    _lastRequestTime = Alarm.getCurrentTime();

    return _lifecycle.isActive();
  }

  /**
   * Called when a request starts the webApp.
   */
  final void exitWebApp()
  {
    _requestCount.decrementAndGet();
  }

  /**
   * Returns the request count.
   */
  public int getRequestCount()
  {
    return _requestCount.get();
  }

  /**
   * Returns the maximum length for a cache.
   */
  public void addCacheMapping(CacheMapping mapping)
    throws Exception
  {
    if (mapping.getUrlRegexp() != null)
      _cacheMappingMap.addRegexp(mapping.getUrlRegexp(), mapping);
    else
      _cacheMappingMap.addMap(mapping.getUrlPattern(), mapping);
  }

  /**
   * Returns the time for a cache mapping.
   */
  public long getMaxAge(String uri)
  {
    CacheMapping map = (CacheMapping) _cacheMappingMap.map(uri);

    if (map != null)
      return map.getMaxAge();
    else
      return Long.MIN_VALUE;
  }

  /**
   * Returns the time for a cache mapping.
   */
  public long getSMaxAge(String uri)
  {
    CacheMapping map = (CacheMapping) _cacheMappingMap.map(uri);

    if (map != null)
      return map.getSMaxAge();
    else
      return Long.MIN_VALUE;
  }

  /**
   * Returns the maximum length for a cache.
   */
  public long getCacheMaxLength()
  {
    return _cache.getMaxEntrySize();
  }

  /**
   * Returns the classloader hack packages.
   */
  public String []getClassLoaderHackPackages()
  {
    return _classLoaderHackPackages;
  }

  /**
   * Returns the active session count.
   */
  public int getActiveSessionCount()
  {
    SessionManager manager = getSessionManager();

    if (manager != null)
      return manager.getActiveSessionCount();
    else
      return 0;
  }

  void updateStatistics(long time,
                        int readBytes,
                        int writeBytes,
                        boolean isClientDisconnect)
  {
    _controller.updateStatistics(time, readBytes, writeBytes, isClientDisconnect);
  }

  /**
   * Stops the webApp.
   */
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStopping())
        return;

      long beginStop = Alarm.getCurrentTime();

      while (_requestCount.get() > 0
	     && Alarm.getCurrentTime() < beginStop + _shutdownWaitTime
	     && ! Alarm.isTest()) {
        try {
          Thread.interrupted();
          Thread.sleep(100);
        } catch (Throwable e) {
        }
      }

      if (_requestCount.get() > 0) {
        log.warning(L.l("{0} closing with {1} active requests.",
                        toString(), _requestCount));
      }

      ServletContextEvent event = new ServletContextEvent(this);

      SessionManager sessionManager = _sessionManager;
      _sessionManager = null;

      if (sessionManager != null
	  && (! _isInheritSession || _controller.getParent() == null))
        sessionManager.close();

      if (_servletManager != null)
	_servletManager.destroy();
      if (_filterManager != null)
	_filterManager.destroy();

      // server/10g8 -- webApp listeners after session
      if (_webAppListeners != null) {
	for (int i = _webAppListeners.size() - 1; i >= 0; i--) {
	  ServletContextListener listener = _webAppListeners.get(i);

	  try {
	    listener.contextDestroyed(event);
	  } catch (Exception e) {
	    log.log(Level.WARNING, e.toString(), e);
	  }
	}
      }

      // server/10g8 -- webApp listeners after session
      for (int i = _listeners.size() - 1; i >= 0; i--) {
	Listener listener = _listeners.get(i);

	try {
	  listener.destroy();
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      try {
        _classLoader.stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      _lifecycle.toStop();
    }
  }

  /**
   * Closes the webApp.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    clearCache();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      for (int i = _appGenerators.size() - 1; i >= 0; i--) {
        try {
          DeployGenerator deploy = _appGenerators.get(i);
          _parent.removeWebAppDeploy(deploy);
          deploy.destroy();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      if (_accessLog != null) {
        _accessLog.flush();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      _classLoader.destroy();
    }
  }

  //
  // statistics
  //

  public void addStatus500()
  {
    synchronized (this) {
      _status500CountTotal++;
      _status500LastTime = Alarm.getExactTime();
    }
  }

  long getStatus500CountTotal()
  {
    return _status500CountTotal;
  }

  long getStatus500LastTime()
  {
    return _status500LastTime;
  }

  /**
   * Serialize to a handle
   */
  private Object writeReplace()
  {
    return new SingletonHandle(WebApp.class);
  }

  public String toString()
  {
    return "WebApp[" + getId() + "]";
  }

  /**
   * WebBeans callbacks
   */
  class WebBeansObserver implements Observer<Manager> {
    public void notify(Manager manager)
    {
      // search for servlets

      for (Bean bean : manager.resolveByType(Servlet.class, new CurrentLiteral())) {
	if (bean instanceof CauchoBean) {
	  CauchoBean cBean = (CauchoBean) bean;
	  
	  for (Annotation ann : cBean.getAnnotations()) {
	    if (ann.annotationType().equals(javax.servlet.http.annotation.Servlet.class)) {
	      //ServletConfigImpl config
	      //  = new ServletConfigImpl(this, ann, manager.getInstance(bean));

	      // _servletManager.addServlet(config);
	      
	    }
	  }
	}
      }
    }
  }

  static class FilterChainEntry {
    FilterChain _filterChain;
    String _pathInfo;
    String _servletPath;
    String _servletName;
    HashMap<String,String> _securityRoleMap;
    final Dependency _dependency;

    FilterChainEntry(FilterChain filterChain, Invocation invocation)
    {
      _filterChain = filterChain;
      _pathInfo = invocation.getPathInfo();
      _servletPath = invocation.getServletPath();
      _servletName = invocation.getServletName();
      _dependency = invocation.getDependency();
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
  }

  static {
    _classLoaderHackPackages = new String[] {
      "java.",
      "javax.servlet.",
      "javax.naming.",
      "javax.sql.",
      "javax.transaction.",
    };
  }
}
