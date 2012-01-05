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

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import javax.naming.InitialContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.types.*;

import com.caucho.java.WorkDir;

import com.caucho.jsp.JspServlet;
import com.caucho.jsp.cfg.JspConfig;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.jsp.cfg.JspTaglib;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;

import com.caucho.make.AlwaysModified;
import com.caucho.make.Dependency;
import com.caucho.make.DependencyContainer;

import com.caucho.management.server.*;

import com.caucho.server.cache.AbstractCache;

import com.caucho.server.cluster.Cluster;

import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.deploy.EnvironmentDeployInstance;

import com.caucho.server.dispatch.*;

import com.caucho.server.host.Host;

import com.caucho.server.log.AbstractAccessLog;
import com.caucho.server.log.AccessLog;

import com.caucho.server.resin.ResinServer;

import com.caucho.server.security.*;

import com.caucho.server.session.SessionManager;

import com.caucho.transaction.TransactionManagerImpl;

import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.LruCache;
import com.caucho.util.CauchoSystem;

import com.caucho.vfs.Encoding;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Resin's application implementation.
 */
public class Application extends ServletContextImpl
  implements Dependency, EnvironmentBean, SchemaBean, DispatchBuilder,
             EnvironmentDeployInstance {
  private static final String DEFAULT_VERSION = "2.4";

  private static final L10N L = new L10N(Application.class);
  private static final Logger log = Log.open(Application.class);

  private static final int JSP_NONE = 0;
  private static final int JSP_1 = 1;
  private static final int JSP_2 = 2;

  private static EnvironmentLocal<AbstractAccessLog> _accessLogLocal
    = new EnvironmentLocal<AbstractAccessLog>("caucho.server.access-log");

  private static EnvironmentLocal<Application> _appLocal
    = new EnvironmentLocal<Application>("caucho.application");

  static String []_classLoaderHackPackages;

  private ClassLoader _parentClassLoader;

  // The environment class loader
  private EnvironmentClassLoader _classLoader;

  // The parent
  private ApplicationContainer _parent;

  // The application entry
  private WebAppController _controller;

  // The context path
  private String _contextPath = "";

  // A description
  private String _description = "";

  private String _servletVersion;

  // The application directory.
  private Path _appDir;
  private boolean _isAppDirSet;
  private boolean _isDynamicDeploy;

  // Any war-generators.
  private ArrayList<DeployGenerator> _appGenerators
    = new ArrayList<DeployGenerator>();

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

  // The cache
  private AbstractCache _cache;

  private LruCache<String,FilterChainEntry> _filterChainCache
    = new LruCache<String,FilterChainEntry>(256);

  private UrlMap<CacheMapping> _cacheMappingMap = new UrlMap<CacheMapping>();

  private LruCache<String,RequestDispatcherImpl> _dispatcherCache;

  // The login manager
  private AbstractLogin _loginManager;

  // The security constraints
  private ConstraintManager _constraintManager;

  // True for SSL secure.
  private boolean _isSecure;

  // Error pages.
  private ErrorPageManager _errorPageManager;

  // Any configuration exception
  private Throwable _configException;

  // dispatch mapping
  private RewriteInvocation _rewriteInvocation;

  private LruCache<String,String> _realPathCache =
    new LruCache<String,String>(1024);
  // real-path mapping
  private RewriteRealPath _rewriteRealPath;

  // mime mapping
  private HashMap<String,String> _mimeMapping = new HashMap<String,String>();
  // locale mapping
  private HashMap<String,String> _localeMapping
    = new HashMap<String,String>();

  // List of all the listeners.
  private ArrayList<Object> _listeners = new ArrayList<Object>();

  // List of the ServletContextListeners from the configuration file
  private ArrayList<ServletContextListener> _applicationListeners
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
  private int _formParameterMax = 10000;

  // special
  private int _jspState;
  private JspPropertyGroup _jsp;
  private ArrayList<JspTaglib> _taglibList;
  private HashMap<String,Object> _extensions = new HashMap<String,Object>();
  private MultipartForm _multipartForm;

  private ArrayList<String> _regexp;

  private long _shutdownWaitTime = 15000L;
  private long _activeWaitTime = 15000L;

  private long _idleTime = 2 * 3600 * 1000L;

  private final Object _countLock = new Object();
  private final Lifecycle _lifecycle;

  private int _requestCount;
  private long _lastRequestTime = Alarm.getCurrentTime();

  /**
   * Creates the application with its environment loader.
   */
  public Application()
  {
    this(new WebAppController("/", null, null));
  }

  /**
   * Creates the application with its environment loader.
   */
  Application(WebAppController controller)
  {
    try {
      _classLoader = new EnvironmentClassLoader(controller.getParentClassLoader());

      // the JSP servlet needs to initialize the JspFactory
      JspServlet.initStatic();

      String contextPath = controller.getContextPath();
      setContextPathId(contextPath);

      _controller = controller;

      _classLoader.addParentPriorityPackages(_classLoaderHackPackages);

      _appLocal.set(this, _classLoader);

      // map.put("app", _appVar);

      _appDir = controller.getRootDirectory();

      if (_appDir.equals(CauchoSystem.getResinHome()))
        log.warning(L.l("web-app root directory should not be the same as resin.home\n{0}", _appDir));

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
      _errorPageManager = new ErrorPageManager();
      _errorPageManager.setApplication(this);

      setParent(controller.getContainer());

      _invocationDependency = new DependencyContainer();
      _invocationDependency.add(this);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      setConfigException(e);
    } finally {
      _lifecycle = new Lifecycle(log, toString(), Level.INFO);
    }
  }

  /**
   * Sets the parent container.
   */
  public void setParent(ApplicationContainer parent)
  {
    _parent = parent;

    if (parent == null)
      return;

    if (! _isAppDirSet) {
      setAppDir(parent.getDocumentDirectory());
      Vfs.setPwd(parent.getDocumentDirectory(), _classLoader);
      _isAppDirSet = false;
    }

    _errorPageManager.setParent(parent.getErrorPageManager());
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
  public ApplicationContainer getParent()
  {
    return _parent;
  }

  /**
   * Returns the local application.
   */
  public static Application getLocal()
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

    if (! _isAppDirSet && _parent != null) {
      setAppDir(_parent.getDocumentDirectory().lookup("./" + id));
      _isAppDirSet = false;
    }
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
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/webapp/resin-web-xml.rnc";
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
   * Gets the application directory.
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
    setAppDir(appDir);
  }

  /**
   * Sets the root directory (app-dir).
   */
  public void setRootDirectory(Path appDir)
  {
    setAppDir(appDir);
  }

  /**
   * Sets the application directory.
   */
  public void setAppDir(Path appDir)
  {
    _appDir = appDir;
    _isAppDirSet = true;

    WorkDir.setLocalWorkDir(appDir.lookup("WEB-INF/work"), getClassLoader());

    // XXX:
    // _classLoader.setAttribute("caucho.vfs.pwd", appDir);
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

    _classLoader.setId("web-app:" + getURL());
  }

  /**
   * Sets the servlet version.
   */
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
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the icon
   */
  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Sets the servlet init-param EL enabling.
   */
  public void setAllowServletEL(boolean allow)
  {
    _servletAllowEL = allow;
  }

  /**
   * Adds a servlet configuration.
   */
  public ServletConfigImpl createServlet()
    throws ServletException
  {
    ServletConfigImpl config = new ServletConfigImpl();

    config.setAllowEL(_servletAllowEL);

    return config;
  }

  /**
   * Adds a servlet configuration.
   */
  public void addServlet(ServletConfigImpl config)
    throws ServletException
  {
    config.setServletContext(this);

    _servletManager.addServlet(config);
  }

  /**
   * Set true if strict mapping.
   */
  public void setStrictMapping(boolean isStrict)
    throws ServletException
  {
    _isStrictMapping = isStrict;
  }

  /**
   * Lazy servlet validation.
   */
  public void setLazyServletValidate(boolean isLazy)
  {
    _servletManager.setLazyValidate(isLazy);
  }

  /**
   * Adds a servlet-mapping configuration.
   */
  public ServletMapping createServletMapping()
    throws ServletException
  {
    ServletMapping servletMapping = new ServletMapping();
    servletMapping.setStrictMapping(_isStrictMapping);

    return servletMapping;
  }

  /**
   * Adds a servlet-mapping configuration.
   */
  public void addServletMapping(ServletMapping servletMapping)
    throws ServletException
  {
    if (servletMapping.getURLRegexp() == null &&
        servletMapping.getServletClassName() != null) {
      if (servletMapping.getServletName() == null)
        servletMapping.setServletName(servletMapping.getServletClassName());

      addServlet(servletMapping);
    }

    _servletMapper.addServletMapping(servletMapping);
  }

  /**
   * Adds a web service.
   */
  public WebService createWebService()
    throws ServletException
  {
    WebService webService = new WebService();
    webService.setStrictMapping(_isStrictMapping);

    return webService;
  }

  /**
   * Adds a web service configuration.
   */
  public void addWebService(WebService webService)
    throws ServletException
  {
    addServletMapping(webService);
  }

  /**
   * Adds a servlet-regexp configuration.
   */
  public void addServletRegexp(ServletRegexp servletRegexp)
    throws ServletException, ClassNotFoundException
  {
    ServletMapping mapping = new ServletMapping();
    mapping.setURLRegexp(servletRegexp.getURLRegexp());
    mapping.setServletName(servletRegexp.getServletName());
    mapping.setServletClass(servletRegexp.getServletClass());
    mapping.setServletContext(this);
    mapping.setInit(new InitProgram(servletRegexp.getBuilderProgram()));

    _servletMapper.addServletRegexp(mapping);
  }

  /**
   * Adds a filter configuration.
   */
  public void addFilter(FilterConfigImpl config)
  {
    config.setServletContext(this);

    _filterManager.addFilter(config);
  }

  /**
   * Adds a filter-mapping configuration.
   */
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
   * Set true if includes wrap filters.
   */
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
   * Adds a welcome file list to the application.
   */
  public void addWelcomeFileList(WelcomeFileList list)
  {
    ArrayList<String> fileList = list.getWelcomeFileList();

    _servletMapper.setWelcomeFileList(fileList);
  }

  /**
   * Configures the locale encoding.
   */
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

  public void setFormParameterMax(int max)
  {
    _formParameterMax = max;
  }

  public int getFormParameterMax()
  {
    return _formParameterMax;
  }

  /**
   * Configures the session manager.
   */
  public SessionManager createSessionConfig()
    throws Exception
  {
    if (_isInheritSession)
      return new SessionManager(this);

    return getSessionManager();
  }

  /**
   * Adds the session manager.
   */
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
  public void setAccessLog(AbstractAccessLog log)
  {
    _accessLog = log;

    _accessLogLocal.set(log);
  }

  /**
   * Adds a mime-mapping
   */
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
    _localeMapping.put(locale, encoding);
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
   * Adds rewrite-dispatch.
   */
  public RewriteInvocation createRewriteDispatch()
  {
    if (_rewriteInvocation == null)
      _rewriteInvocation = new RewriteInvocation();

    return _rewriteInvocation;
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
   * Sets the login
   */
  public void setLoginConfig(LoginConfig loginConfig)
    throws Throwable
  {
    _loginManager = loginConfig.getLogin();
  }

  /**
   * Adds a security constraint
   */
  public void addSecurityConstraint(SecurityConstraint constraint)
  {
    _constraintManager.addConstraint(constraint);
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
  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;

    if (isSecure) {
      TransportConstraint transConstraint = new TransportConstraint("secure");

      SecurityConstraint constraint = new SecurityConstraint();
      constraint.setURLPattern("/*");
      constraint.addConstraint(transConstraint);

      _constraintManager.addConstraint(constraint);
    }
  }

  public void addListener(Listener listener)
    throws Exception
  {
    addListenerObject(listener.getListenerObject());
  }

  /**
   * Returns true if a listener with the given type exists.
   */
  public boolean hasListener(Class listenerClass)
  {
    for (int i = 0; i < _listeners.size(); i++) {
      Object obj = _listeners.get(i);

      if (listenerClass.equals(obj.getClass()))
        return true;
    }

    return false;
  }

  /**
   * Adds the listener object.
   */
  public void addListenerObject(Object listenerObj)
  {
    if (! _listeners.contains(listenerObj))
      _listeners.add(listenerObj);

    if (listenerObj instanceof ServletContextListener) {
      ServletContextListener scListener = (ServletContextListener) listenerObj;
      _applicationListeners.add(scListener);

      if (_lifecycle.isActive() || _lifecycle.isStarting()) {
        ServletContextEvent event = new ServletContextEvent(this);

        try {
          scListener.contextInitialized(event);
        } catch (Throwable e) {
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
   * Returns true for JSP 1.x
   */
  public boolean has23Config()
  {
    return _jspState == JSP_1;
  }

  /**
   * taglib configuration
   */
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
   * Sets the war-expansion
   */
  public WebAppExpandDeployGenerator createWebAppDeploy()
  {
    return _parent.createWebAppDeploy();
  }

  /**
   * Adds a war generator
   */
  public void addWebAppDeploy(WebAppExpandDeployGenerator deploy)
    throws Exception
  {
    String contextPath = getContextPath();
    String prefix = deploy.getURLPrefix();

    deploy.setURLPrefix(contextPath + prefix);
    deploy.setParent(_controller);
    deploy.setContainer(_parent);

    // _parent.addWebAppDeploy(gen);

    deploy.setParentClassLoader(getClassLoader());
    // deploy.deploy();
    // XXX: The parent is added in the init()
    // server/10t3
    // _parent.addWebAppDeploy(deploy);

    Environment.addEnvironmentListener(deploy, getClassLoader());

    _appGenerators.add(deploy);
  }

  /**
   * Adds a sub web-app
   */
  public void addWebApp(WebAppConfig config)
    throws Exception
  {
    String contextPath = getContextPath();
    String prefix = config.getId();

    if (prefix == null || prefix.equals("") || prefix.equals("/"))
      throw new ConfigException(L.l("'{0}' is an illegal sub web-app id.",
                                    prefix));

    ApplicationContainer container = _parent;
    DeployContainer<WebAppController> appGenerator;
    appGenerator = _parent.getApplicationGenerator();

    WebAppSingleDeployGenerator deploy = new WebAppSingleDeployGenerator(appGenerator,
                                                       container, config);

    deploy.setURLPrefix(contextPath + prefix);
    // deploy.setParent(_controller);

    // XXX: The parent is added in the init()
    // _parent.addWebAppDeploy(gen);

    deploy.setParentWebApp(_controller);
    deploy.setParentClassLoader(getClassLoader());
    deploy.setContainer(container);

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
    return Cluster.getCluster(getClassLoader());
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
  public void setShutdownWaitMax(Period wait)
  {
    _shutdownWaitTime = wait.getPeriod();

    ResinServer resinServer = ResinServer.getResinServer();
    if (resinServer != null &&
        resinServer.getShutdownWaitMax() < _shutdownWaitTime) {
      log.warning(L.l("web-app shutdown-wait-max '{0}' is longer than resin shutdown-wait-max '{1}'.",
                      _shutdownWaitTime,
                      resinServer.getShutdownWaitMax()));
    }
  }

  /**
   * Sets the delay time waiting for a restart
   */
  public void setActiveWaitTime(Period wait)
  {
    _activeWaitTime = wait.getPeriod();
  }

  /**
   * Sets the delay time waiting for requests to end.
   */
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
   * Returns true if the application is active.
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
   * Returns true if the application is active.
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

  /**
   * Initializes.
   */
  public void init()
    throws Exception
  {
    if (! _lifecycle.toInitializing())
      return;

    try {
      _classLoader.setId("web-app:" + getURL());

      _invocationDependency.setCheckInterval(getEnvironmentClassLoader().getDependencyCheckInterval());

      if (_tempDir == null)
        _tempDir = (Path) Environment.getLevelAttribute("caucho.temp-dir");

      if (_tempDir == null)
        _tempDir = getAppDir().lookup("WEB-INF/tmp");

      _tempDir.mkdirs();
      setAttribute("javax.servlet.context.tempdir", new File(_tempDir.getNativePath()));

      FilterChainBuilder securityBuilder = _constraintManager.getFilterBuilder();

      if (securityBuilder != null)
        _filterMapper.addTopFilter(securityBuilder);

      _cache = (AbstractCache) Environment.getAttribute("caucho.server.cache");

      for (int i = 0; i < _appGenerators.size(); i++)
        _parent.addDeploy(_appGenerators.get(i));

      _classLoader.setId("web-app:" + getURL());

      try {
        InitialContext ic = new InitialContext();
        ServletAuthenticator auth;
        auth = (ServletAuthenticator) ic.lookup("java:comp/env/caucho/auth");

        setAttribute("caucho.authenticator", auth);
      } catch (Exception e) {
        log.finest(e.toString());
      }

      WebAppController parent = null;
      if (_controller != null)
        parent = _controller.getParent();
      if (_isInheritSession && parent != null &&
          _sessionManager != parent.getApplication().getSessionManager()) {
        SessionManager sessionManager = _sessionManager;
        _sessionManager = parent.getApplication().getSessionManager();

        if (sessionManager != null)
          sessionManager.close();
      }

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
      throw new IllegalStateException(L.l("application must be initialized before starting.  Currently in state {0}.", _lifecycle.getStateName()));

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
        _invocationDependency.add(_parent.getApplicationGenerator());

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
        getSessionManager().start();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      ServletContextEvent event = new ServletContextEvent(this);

      for (int i = 0; i < _applicationListeners.size(); i++) {
        ServletContextListener listener = _applicationListeners.get(i);

        try {
          listener.contextInitialized(event);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      try {
        _filterManager.init();
        _servletManager.init();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
        setConfigException(e);
      }

      _lifecycle.toActive();

      if (_parent instanceof Host) {
        Host host = (Host) _parent;

        host.setConfigETag(null);
      }

      if (_parent != null)
        _parent.clearCache();

      isOkay = true;
    } finally {
      if (! isOkay)
        _lifecycle.toError();

      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns true if the application has been modified.
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
   * Returns true if the application has been modified.
   */
  public boolean isModifiedNow()
  {
    // force check
    _classLoader.isModifiedNow();
    _invocationDependency.isModifiedNow();

    return isModified();
  }

  /**
   * Returns true if the application deployed with an error.
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
      throw new IllegalArgumentException(L.l("getContext URI `{0}' must be absolute.", uri));

    try {
      if (_parent != null)
        return _parent.findSubApplicationByURI(uri);
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
  public void buildInvocation(Invocation invocation)
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
        return;
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        chain = new ErrorFilterChain(code);
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());
        return;
      }
      else {
        FilterChainEntry entry = null;

        // jsp/1910 - can't cache jsp_precompile
        String query = invocation.getQueryString();

        boolean isPrecompile = false;
        if (query != null && query.indexOf("jsp_precompile") >= 0)
          isPrecompile = true;

        if (! isPrecompile)
          entry = _filterChainCache.get(invocation.getContextURI());

        if (entry != null && ! entry.isModified()) {
          chain = entry.getFilterChain();
        } else {
          if (_rewriteInvocation != null) {
            chain = _rewriteInvocation.map(invocation.getContextURI(),
                                           invocation);
          }

          if (chain == null) {
            chain = _servletMapper.mapServlet(invocation);

            _filterMapper.buildDispatchChain(invocation, chain);

            chain = invocation.getFilterChain();
          }

          entry = new FilterChainEntry(chain, invocation);
          chain = entry.getFilterChain();

          if (! isPrecompile)
            _filterChainCache.put(invocation.getContextURI(), entry);
        }

        // the cache must be outside of the WebAppFilterChain because
        // the CacheListener in ServletInvocation needs the top to
        // be a CacheListener.  Otherwise, the cache won't get lru.

        // top-level filter elements
        if (_cache != null)
          chain = _cache.createFilterChain(chain, this);

        if (CauchoSystem.isDetailedStatistics())
          chain = new StatisticsFilterChain(chain, this);

        WebAppFilterChain webAppChain = new WebAppFilterChain(chain, this);

        webAppChain.setSecurityRoleMap(invocation.getSecurityRoleMap());

        invocation.setFilterChain(webAppChain);
        invocation.setPathInfo(entry.getPathInfo());
        invocation.setServletPath(entry.getServletPath());
      }
    } catch (Throwable e) {
      FilterChain chain = new ExceptionFilterChain(e);
      chain = new WebAppFilterChain(chain, this);
      invocation.setDependency(AlwaysModified.create());
      invocation.setFilterChain(chain);
    } finally {
      thread.setContextClassLoader(oldLoader);
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
   * Fills the invocation for subrequests.
   */
  public void buildDispatchInvocation(Invocation invocation,
                                      FilterMapper filterMapper)
    throws ServletException
  {
    invocation.setApplication(this);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    thread.setContextClassLoader(getClassLoader());
    try {
      FilterChain chain;

      /*
      if (! _isActive) {
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        chain = new ErrorFilterChain(code);
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());
        return;
      }
      */
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

        filterMapper.buildDispatchChain(invocation, chain);

        chain = invocation.getFilterChain();

        chain = new DispatchFilterChain(chain, this);

        if (_cache != null && filterMapper == _includeFilterMapper) {
          chain = _cache.createFilterChain(chain, this);
        }
      }

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
      throw new IllegalArgumentException(L.l("request dispatcher url `{0}' must be absolute", url));

    RequestDispatcherImpl disp = getDispatcherCache().get(url);

    if (disp != null && ! disp.isModified())
      return disp;

    Invocation includeInvocation = new SubInvocation();
    Invocation forwardInvocation = new SubInvocation();
    Invocation errorInvocation = new SubInvocation();
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = escapeURL(_contextPath + url);

    try {
      decoder.splitQuery(includeInvocation, rawURI);
      decoder.splitQuery(forwardInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);

      if (_parent != null) {
        _parent.buildIncludeInvocation(includeInvocation);
        _parent.buildForwardInvocation(forwardInvocation);
        _parent.buildErrorInvocation(errorInvocation);
      }
      else {
        FilterChain chain = _servletMapper.mapServlet(includeInvocation);
        _includeFilterMapper.buildDispatchChain(includeInvocation, chain);
        includeInvocation.setApplication(this);

        chain = _servletMapper.mapServlet(forwardInvocation);
        _forwardFilterMapper.buildDispatchChain(forwardInvocation, chain);
        forwardInvocation.setApplication(this);

        chain = _servletMapper.mapServlet(errorInvocation);
        _errorFilterMapper.buildDispatchChain(errorInvocation, chain);
        errorInvocation.setApplication(this);
      }

      disp = new RequestDispatcherImpl(includeInvocation,
                                       forwardInvocation,
                                       errorInvocation,
                                       this);

      getDispatcherCache().put(url, disp);

      return disp;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
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
      throw new IllegalArgumentException(L.l("request dispatcher url `{0}' must be absolute", url));

    Invocation loginInvocation = new Invocation();
    Invocation errorInvocation = new Invocation();
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = _contextPath + url;

    try {
      decoder.splitQuery(loginInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);

      if (_parent != null) {
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
    FilterChain chain;

    try {
      chain = _servletManager.createServletChain(servletName);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);

      return null;
    }

    return new NamedDispatcherImpl(chain, null, this);
  }

  /**
   * Maps from a URI to a real path.
   */
  public String getRealPath(String uri)
  {
    String realPath = _realPathCache.get(uri);

    if (realPath != null)
      return realPath;

    String fullURI = getContextPath() + "/" + uri;

    try {
      fullURI = InvocationDecoder.normalizeUri(fullURI);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    Application app = (Application) getContext(fullURI);

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

    Application app = (Application) getContext(fullURI);

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
      log.log(Level.WARNING, message, e);
    else
      log.info(message);
  }

  /**
   * Gets the login manager.
   */
  public AbstractLogin getLogin()
  {
    return _loginManager;
  }

  /**
   * Gets the authenticator
   */
  public ServletAuthenticator getAuthenticator()
  {
    AbstractLogin login = getLogin();

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
      if (_isInheritSession && _parent != null)
        _sessionManager = _parent.getSessionManager();

      if (_sessionManager == null) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();

        try {
          thread.setContextClassLoader(getClassLoader());

          _sessionManager = new SessionManager(this);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
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
   * Called when a request starts the application.
   */
  final boolean enterWebApp()
  {
    synchronized (_countLock) {
      _requestCount++;
      _lastRequestTime = Alarm.getCurrentTime();
    }

    return _lifecycle.isActive();
  }

  /**
   * Called when a request starts the application.
   */
  final void exitWebApp()
  {
    synchronized (_countLock) {
      _requestCount--;
    }
  }

  /**
   * Returns the request count.
   */
  public int getRequestCount()
  {
    return _requestCount;
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
    return 1000000L;
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
   * Stops the application.
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

      while (_requestCount > 0 &&
             Alarm.getCurrentTime() < beginStop + _shutdownWaitTime &&
             ! Alarm.isTest()) {
        try {
          Thread.interrupted();
          Thread.sleep(100);
        } catch (Throwable e) {
        }
      }

      if (_requestCount > 0) {
        log.warning(L.l("{0} closing with {1} active requests.",
                        toString(), _requestCount));
      }

      ServletContextEvent event = new ServletContextEvent(this);

      SessionManager sessionManager = _sessionManager;
      _sessionManager = null;

      if (sessionManager != null &&
          (! _isInheritSession || _controller.getParent() == null))
        sessionManager.close();

      _servletManager.destroy();
      _filterManager.destroy();

      // server/10g8 -- application listeners after session
      for (int i = _applicationListeners.size() - 1; i >= 0; i--) {
        ServletContextListener listener = _applicationListeners.get(i);

        try {
          listener.contextDestroyed(event);
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
   * Closes the application.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    if (_parent != null)
      _parent.clearCache();

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

  public String toString()
  {
    return "WebApp[" + getURL() + "]";
  }


  static class FilterChainEntry {
    FilterChain _filterChain;
    String _pathInfo;
    String _servletPath;
    HashMap<String,String> _securityRoleMap;
    final Dependency _dependency;

    FilterChainEntry(FilterChain filterChain, Invocation invocation)
    {
      _filterChain = filterChain;
      _pathInfo = invocation.getPathInfo();
      _servletPath = invocation.getServletPath();
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
