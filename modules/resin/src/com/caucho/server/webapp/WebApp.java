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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Bean;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterRegistration;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.el.CandiElResolver;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SingletonBindingHandle;
import com.caucho.config.j2ee.PersistenceContextRefConfig;
import com.caucho.config.types.EjbLocalRef;
import com.caucho.config.types.EjbRef;
import com.caucho.config.types.InitParam;
import com.caucho.config.types.PathBuilder;
import com.caucho.config.types.Period;
import com.caucho.config.types.ResourceRef;
import com.caucho.config.types.Validator;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.manager.EjbModule;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployGenerator;
import com.caucho.env.deploy.DeployMode;
import com.caucho.env.deploy.EnvironmentDeployInstance;
import com.caucho.env.deploy.RepositoryDependency;
import com.caucho.env.thread.ThreadPool;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.java.WorkDir;
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
import com.caucho.loader.enhancer.AbstractScanClass;
import com.caucho.loader.enhancer.ScanClass;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.make.AlwaysModified;
import com.caucho.make.DependencyContainer;
import com.caucho.management.server.HostMXBean;
import com.caucho.naming.Jndi;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.rewrite.DispatchRule;
import com.caucho.rewrite.IfSecure;
import com.caucho.rewrite.Not;
import com.caucho.rewrite.RedirectSecure;
import com.caucho.rewrite.RewriteFilter;
import com.caucho.rewrite.WelcomeFile;
import com.caucho.security.Authenticator;
import com.caucho.security.BasicLogin;
import com.caucho.security.Login;
import com.caucho.security.RoleMapManager;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.FilterChainBuilder;
import com.caucho.server.dispatch.FilterConfigImpl;
import com.caucho.server.dispatch.FilterManager;
import com.caucho.server.dispatch.FilterMapper;
import com.caucho.server.dispatch.FilterMapping;
import com.caucho.server.dispatch.ForwardErrorFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationBuilder;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.RedirectFilterChain;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.dispatch.ServletManager;
import com.caucho.server.dispatch.ServletMapper;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.dispatch.ServletRegexp;
import com.caucho.server.dispatch.SubInvocation;
import com.caucho.server.dispatch.UrlMap;
import com.caucho.server.dispatch.VersionInvocation;
import com.caucho.server.host.Host;
import com.caucho.server.http.StubSessionContextRequest;
import com.caucho.server.httpcache.AbstractProxyCache;
import com.caucho.server.log.AbstractAccessLog;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.security.ConstraintManager;
import com.caucho.server.security.LoginConfig;
import com.caucho.server.security.PermitEmptyRolesConstraint;
import com.caucho.server.security.SecurityConstraint;
import com.caucho.server.security.TransportConstraint;
import com.caucho.server.security.WebResourceCollection;
import com.caucho.server.session.SessionManager;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webbeans.SessionContextContainer;
import com.caucho.util.BasicFuture;
import com.caucho.util.CharBuffer;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Resin's webApp implementation.
 */
@Configurable
@SuppressWarnings("serial")
public class WebApp extends ServletContextImpl
  implements Dependency, EnvironmentBean, SchemaBean, InvocationBuilder,
             EnvironmentDeployInstance, JspConfigDescriptor,
             java.io.Serializable
{
  private static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebApp.class.getName());

  private static final int JSP_1 = 1;
  private static final char []SERVLET_ANNOTATION
    = "javax.servlet.annotation.".toCharArray();

  private static EnvironmentLocal<AbstractAccessLog> _accessLogLocal
    = new EnvironmentLocal<AbstractAccessLog>("caucho.server.access-log");

  private static EnvironmentLocal<WebApp> _appLocal
    = new EnvironmentLocal<WebApp>("caucho.application");

  private static String []_classLoaderHackPackages;

  // The environment class loader
  private EnvironmentClassLoader _classLoader;

  private ServletService _server;
  private Host _host;
  // The parent
  private WebAppContainer _parent;

  private WebApp _oldWebApp;
  private long _oldWebAppExpireTime;

  // The webApp entry
  private WebAppController _controller;

  // The webbeans container
  private InjectManager _cdiManager;

  private InvocationDecoder _invocationDecoder;

  private String _moduleName = "default";
  
  // The context path
  private String _baseContextPath = "";
  private String _versionContextPath = "";

  // A description
  private String _description = "";

  private String _servletVersion;

  private boolean _isDynamicDeploy;
  private boolean _isDisableCrossContext;

  // true for jsp compilation from a command line
  private boolean _isCompileContext;

  // Any war-generators.
  private ArrayList<DeployGenerator<WebAppController>> _appGenerators
    = new ArrayList<DeployGenerator<WebAppController>>();

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

  private FilterChainBuilder _securityBuilder;

  // The session manager
  private SessionManager _sessionManager;
  // True if the session manager is inherited
  private boolean _isInheritSession;

  private String _characterEncoding;
  private int _formParameterMax = 10000;

  // The cache
  private AbstractProxyCache _proxyCache;

  private LruCache<String,FilterChainEntry> _filterChainCache
    = new LruCache<String,FilterChainEntry>(256);

  private UrlMap<CacheMapping> _cacheMappingMap = new UrlMap<CacheMapping>();

  private LruCache<String,RequestDispatcherImpl> _dispatcherCache;

  private Login _defaultLogin;
  private Login _login;
  private Authenticator _authenticator;

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
  
  private WelcomeFile _welcomeFile;

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
  private ArrayList<ListenerConfig> _listeners = new ArrayList<ListenerConfig>();

  // List of the ServletContextListeners from the configuration file
  private ArrayList<ServletContextListener> _webAppListeners
    = new ArrayList<ServletContextListener>();

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
  
  private ArrayList<String> _welcomeFileList
    = new ArrayList<String>();

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

  private ArrayList<JspTaglib> _taglibList;
  private JspApplicationContextImpl _jspApplicationContext;
  private HashMap<String,Object> _extensions = new HashMap<String,Object>();

  private MultipartForm _multipartForm;

  private ArrayList<String> _regexp;

  private boolean _isStatisticsEnabled;

  private long _shutdownWaitTime = 15000L;
  private long _activeWaitTime = 60000L;
  private String _activeWaitErrorPage;

  private long _idleTime = 2 * 3600 * 1000L;
  
  private boolean _isStartDisabled;
  private boolean _isEnabled = true;

  private final Lifecycle _lifecycle;

  private final AtomicInteger _requestCount = new AtomicInteger();
  private long _lastRequestTime = CurrentTime.getCurrentTime();
  private Pattern _cookieDomainPattern = null;

  //
  // statistics
  //

  private long _status500CountTotal;
  private long _status500LastTime;

  //servlet 3.0
  private boolean _isMetadataComplete = false;
  private List<String> _pendingClasses = new ArrayList<String>();
  private Ordering _absoluteOrdering;
  private List<WebAppFragmentConfig> _webFragments;
  private boolean _isApplyingWebFragments = false;
  private ClassHierarchyScanListener _classHierarchyScanListener;

  /**
   * Creates the webApp with its environment loader.
   */
  WebApp(WebAppController controller)
  {
    _controller = controller;
    _classLoader
      = EnvironmentClassLoader.create(controller.getParentClassLoader(),
                                      "web-app:" + getId());
    
    _server = controller.getWebManager();

    if (_server == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          ServletService.class.getSimpleName()));
    }
    
    _host = controller.getHost();
    
    if (_host == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          Host.class.getSimpleName()));
    }
    
    _invocationDecoder = _server.getInvocationDecoder();

    setVersionContextPath(controller.getContextPath());
    _baseContextPath = controller.getContextPath();
    
    _moduleName = _baseContextPath;
    
    if ("".equals(_moduleName))
      _moduleName = "ROOT";
    else if (_moduleName.startsWith("/"))
      _moduleName = _moduleName.substring(1);

    setParent(controller.getContainer());
    
    if (getId().startsWith("error/"))
      _lifecycle = new Lifecycle(log, toString(), Level.FINER);
    else
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
      
      Path rootDirectory = getRootDirectory();

      Vfs.setPwd(rootDirectory, _classLoader);
      WorkDir.setLocalWorkDir(rootDirectory.lookup("WEB-INF/work"),
                              _classLoader);
      
      EjbManager.setScanAll();
      
      EjbModule.replace(getModuleName(), _classLoader);
      EjbModule.setAppName(getModuleName(), _classLoader);

      _classLoader.addScanListener(new WebFragmentScanner());
      
      loadInitializers();

      // map.put("app", _appVar);

      _servletManager = new ServletManager();
      _servletMapper = new ServletMapper(this);
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

      _dispatchFilterMapper = new FilterMapper();
      _dispatchFilterMapper.setServletContext(this);
      _dispatchFilterMapper.setFilterManager(_filterManager);

      _errorFilterMapper = new FilterMapper();
      _errorFilterMapper.setServletContext(this);
      _errorFilterMapper.setFilterManager(_filterManager);

      _constraintManager = new ConstraintManager();
      
      // _errorPageManager = new ErrorPageManager(_server, this);
      
      // server/003a
      /*
      if (! getId().startsWith("error/")) {
        _errorPageManager.setParent(_host.getErrorPageManager());
      }
      */

      _invocationDependency = new DependencyContainer();
      _invocationDependency.add(this);

      if (_controller.getRepository() != null) {
        String tag = _controller.getId();
        String tagValue = _controller.getRepository().getTagContentHash(tag);

        _invocationDependency.add(new RepositoryDependency(tag, tagValue));
        
        if (_controller.getVersionDependency() != null)
          _invocationDependency.add(_controller.getVersionDependency());
      }
      
      _invocationDependency.add(_controller);

      _cdiManager = InjectManager.create(_classLoader);
      _cdiManager.addXmlPath(getRootDirectory().lookup("WEB-INF/beans.xml"));
      _cdiManager.addExtension(new WebAppInjectExtension(_cdiManager, this));

      _jspApplicationContext = new JspApplicationContextImpl(this);
      _jspApplicationContext.addELResolver(_cdiManager.getELResolver());

      // validation
      if (CauchoSystem.isTesting()) {
      }
      else if (rootDirectory.equals(CauchoSystem.getResinHome())) {
        throw new ConfigException(L.l("web-app root-directory '{0}' can not be the same as resin.home\n",
                                      rootDirectory.getURL()));
      }
      else if (_parent != null
               && rootDirectory.equals(_parent.getRootDirectory())) {
        throw new ConfigException(L.l("web-app root-directory '{0}' can not be the same as the host root-directory\n",
                                      rootDirectory.getURL()));
      }
    } catch (Throwable e) {
      setConfigException(e);
    }
  }

  /**
   * Initialization before configuration
   */
  @Override
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
   * Returns the owning host.
   */
  public Host getHost()
  {
    return _host;
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
  public ServletService getServer()
  {
    return _server;
  }
  
  public WebAppController getController()
  {
    return _controller;
  }
  
  public String getModuleName()
  {
    return _moduleName;
  }
  
  public void setModuleName(String moduleName)
  {
    _moduleName = moduleName;
    EjbModule.replace(getModuleName(), _classLoader);
  }

  public InvocationDecoder getInvocationDecoder()
  {
    if (_invocationDecoder != null)
      return _invocationDecoder;

    if (_server != null)
      _invocationDecoder = _server.getInvocationDecoder();

    if (_invocationDecoder == null && _server == null)
      _invocationDecoder = ServletService.getCurrent().getInvocationDecoder();

    return _invocationDecoder;
  }
  
  public InjectManager getBeanManager()
  {
    return _cdiManager;
  }

  /**
   * The id is the context path.
   */
  public void setId(String id)
  {
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
  public void setRedeployMode(DeployMode mode)
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

    // jsp/013f
    if (node.getNodeName().equals("web-app") && (ns == null || ns.equals(""))) {
      _jspState = JSP_1;
    }
  }

  /**
   * Gets the webApp directory.
   */
  public Path getRootDirectory()
  {
    return _controller.getRootDirectory();
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
  
  public String getWarName()
  {
    return _controller.getWarName();
  }

  /**
   * Gets the context path
   */
  public String getContextPath()
  {
    if (isVersionAlias())
      return _baseContextPath;
    else
      return _versionContextPath;
  }

  /**
   * Sets the context path
   */
  private void setVersionContextPath(String contextPath)
  {
    _versionContextPath = contextPath;

    if (getServletContextName() == null)
      setDisplayName(contextPath);
  }

  private String getVersionContextPath()
  {
    return _versionContextPath;
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
  
  public void setDisableStart(boolean isDisable)
  {
    _isStartDisabled = isDisable;
  }
  
  public void setEnabled(boolean isEnabled)
  {
    _isEnabled = isEnabled;
  }
  
  public boolean isEnabled()
  {
    return _isEnabled && _server.isEnabled();
  }

  public boolean isMetadataComplete()
  {
    return _isMetadataComplete;
  }

  public void setMetadataComplete(boolean metadataComplete)
  {
    _isMetadataComplete = metadataComplete;
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
      return _parent.getURL() + getContextPath();
    else
      return getContextPath();
  }

  /**
   * Gets the URL
   */
  public String getId()
  {
    return _controller.getId();
    /*
    if (_parent != null)
      return _parent.getId() + _versionContextPath;
    else
      return _versionContextPath;
      */
  }

  /**
   * Gets the URL
   */
  public String getHostName()
  {
    return _host.getHostName();
  }

  /**
   * Gets the URL
   */
  public HostMXBean getHostAdmin()
  {
    return _host.getAdmin();
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

  @Configurable
  public boolean isAllowForwardAfterFlush()
  {
    return _isAllowForwardAfterFlush;
  }

  public void setAllowForwardAfterFlush(boolean allowForwardAfterFlush)
  {
    _isAllowForwardAfterFlush = allowForwardAfterFlush;
  }

  /**
   * If true, disables getContext().
   */
  @Configurable
  public void setDisableCrossContext(boolean isDisable)
  {
    _isDisableCrossContext = isDisable;
  }

  public void setCompileContext(boolean isCompile)
  {
    _isCompileContext = isCompile;
  }

  public boolean isCompileContext()
  {
    return _isCompileContext;
  }

  public boolean isVersionAlias()
  {
    return _controller.isVersionAlias();
  }

  /**
   * Sets the old version web-app.
   */
  public void setOldWebApp(WebApp oldWebApp, long expireTime)
  {
    _oldWebApp = oldWebApp;
    _oldWebAppExpireTime = expireTime;
  }

  public Ordering createAbsoluteOrdering()
  {
    if (_absoluteOrdering == null)
      _absoluteOrdering = new Ordering();

    return _absoluteOrdering;
  }

  @Configurable
  public Ordering createOrdering() 
  {
    log.finer(L.l("'{0}' ordering tag should not be used inside web application descriptor.", this));

    return new Ordering();
  }

  /**
   * Adds a servlet configuration.
   */
  public ServletConfigImpl createServlet()
    throws ServletException
  {
    ServletConfigImpl config = new ServletConfigImpl();

    config.setWebApp(this);
    config.setServletContext(this);
    config.setServletMapper(_servletMapper);
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

    _servletManager.addServlet(config, _isApplyingWebFragments);
  }

  @Override
  public <T extends Servlet> T createServlet(Class<T> servletClass)
    throws ServletException
  {
    try {
      return _cdiManager.createTransientObject(servletClass);
    } catch (InjectionException e) {
      throw new ServletException(e);
    }
  }

  public void addServlet(WebServlet webServlet, String servletClassName)
    throws ServletException
  {
    ServletMapping mapping = createServletMapping();
    mapping.setServletClass(servletClassName);

    String name = webServlet.name();

    if (name == null || "".equals(name))
      name = servletClassName;

    mapping.setServletName(name);

    mapping.create(webServlet);

    addServletMapping(mapping);
  }

  @Override
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
    
    return addServlet(servletName, className, servletClass, null);
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Class<? extends Servlet> servletClass)
  {
    return addServlet(servletName, servletClass.getName(), servletClass, null);
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Servlet servlet)
  {
    Class cl = servlet.getClass();

    return addServlet(servletName, cl.getName(), cl, servlet);
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
    if (! isInitializing()) {
      throw new IllegalStateException(L.l("addServlet may only be called during initialization"));
    }

    try {
      ServletConfigImpl config
        = (ServletConfigImpl) getServletRegistration(servletName);

      if (config == null) {
        config = createServlet();

        config.setServletName(servletName);
        config.setServletClass(servletClassName);
        config.setServletClass(servletClass);
        config.setServlet(servlet);

        addServlet(config);
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

  @Override
  public ServletRegistration getServletRegistration(String servletName)
  {
    return _servletManager.getServlet(servletName);
  }

  @Override
  public Map<String, ServletRegistration> getServletRegistrations()
  {
    Map<String, ServletConfigImpl> configMap = _servletManager.getServlets();

    Map<String, ServletRegistration> result
      = new HashMap<String, ServletRegistration>(configMap);

    return Collections.unmodifiableMap(result);
  }

  private void addServletSecurity(Class<? extends Servlet> servletClass,
                                  ServletSecurity security)
  {
    ServletSecurityElement securityElement
      = new ServletSecurityElement(security);

    _servletManager.addSecurityElement(servletClass, securityElement);
  }

  private void initSecurityConstraints()
  {
    Map<String, ServletConfigImpl> servlets = _servletManager.getServlets();

    for (Map.Entry<String, ServletConfigImpl> entry : servlets.entrySet()) {
      ServletSecurityElement securityElement
        = entry.getValue().getSecurityElement();

      if (securityElement == null)
        continue;

      /*
      ServletSecurity.EmptyRoleSemantic rootRoleSemantic
        = securityElement.getEmptyRoleSemantic();
        */

      final Set<String> patterns = _servletMapper.getUrlPatterns(entry.getKey());
      final Collection<HttpMethodConstraintElement> constraints
        = securityElement.getHttpMethodConstraints();

      if (constraints != null) {
        for (HttpMethodConstraintElement httpMethodConstraintElement : securityElement
            .getHttpMethodConstraints()) {
          ServletSecurity.EmptyRoleSemantic emptyRoleSemantic =
            httpMethodConstraintElement.getEmptyRoleSemantic();

          ServletSecurity.TransportGuarantee transportGuarantee =
            httpMethodConstraintElement.getTransportGuarantee();

          String[] roles = httpMethodConstraintElement.getRolesAllowed();

          SecurityConstraint constraint = new SecurityConstraint();
          constraint.setFallthrough(false);

          if (emptyRoleSemantic == ServletSecurity.EmptyRoleSemantic.DENY) {
            constraint.addConstraint(new PermitEmptyRolesConstraint(false));
          } else if (roles.length == 0
                     && transportGuarantee == ServletSecurity.TransportGuarantee.NONE) {
            constraint.addConstraint(new PermitEmptyRolesConstraint(true));
          } else {
            for (String role : roles)
              constraint.addRoleName(role);

            if (transportGuarantee == ServletSecurity.TransportGuarantee.CONFIDENTIAL)
              constraint.addConstraint(new TransportConstraint("CONFIDENTIAL"));
          }

          WebResourceCollection resources = new WebResourceCollection();
          resources.addHttpMethod(httpMethodConstraintElement.getMethodName());

          for (String pattern : patterns) {
            resources.addURLPattern(pattern);
            constraint.addURLPattern(pattern);
          }

          constraint.addWebResourceCollection(resources);

          _constraintManager.addConstraint(constraint);
        }
      }

      ServletSecurity.EmptyRoleSemantic emptyRoleSemantic
        = securityElement.getEmptyRoleSemantic();

      ServletSecurity.TransportGuarantee transportGuarantee
        = securityElement.getTransportGuarantee();

      String []roles = securityElement.getRolesAllowed();

      SecurityConstraint constraint = new SecurityConstraint();

      if (emptyRoleSemantic == ServletSecurity.EmptyRoleSemantic.DENY) {
        constraint.addConstraint(new PermitEmptyRolesConstraint(false));
      } else if (roles.length == 0
        && transportGuarantee == ServletSecurity.TransportGuarantee.NONE) {
        constraint.addConstraint(new PermitEmptyRolesConstraint(true));
      } else {
        for (String role : roles)
          constraint.addRoleName(role);

        if (transportGuarantee
          == ServletSecurity.TransportGuarantee.CONFIDENTIAL)
          constraint.addConstraint(new TransportConstraint("CONFIDENTIAL"));
      }

      for (String pattern : patterns) {
        constraint.addURLPattern(pattern);
      }

      _constraintManager.addConstraint(constraint);
    }
  }

  @Override
  public <T extends Filter> T createFilter(Class<T> filterClass)
    throws ServletException
  {
    try {
      return _cdiManager.createTransientObject(filterClass);
    } catch (InjectionException e) {
      throw new ServletException(e);
    }
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
      for (String url : webFilter.value())
        urlPattern.addText(url);

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

    if (! isMapping) {
      throw new ConfigException(L.l("Annotation @WebFilter at '{0}' must specify either value, urlPatterns or servletNames", filterClassName));
    }

    for (WebInitParam initParam : webFilter.initParams())
      config.setInitParam(initParam.name(), initParam.value());

    for (DispatcherType dispatcher : webFilter.dispatcherTypes())
      config.addDispatcher(dispatcher);

    config.setAsyncSupported(webFilter.asyncSupported());

    addFilterMapping(config);
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName,
                                              String className)
  {
    return addFilter(filterName, className, null, null);
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName,
                                              Class<? extends Filter> filterClass)
  {
    return addFilter(filterName, filterClass.getName(), filterClass, null);
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
  {
    Class cl = filter.getClass();

    return addFilter(filterName, cl.getName(), cl, filter);
  }

  private FilterRegistration.Dynamic addFilter(String filterName,
                                               String className,
                                               Class<? extends Filter> filterClass,
                                               Filter filter)
  {
    if (! isInitializing())
      throw new IllegalStateException();

    try {
      FilterConfigImpl config = new FilterConfigImpl();

      config.setWebApp(this);
      config.setServletContext(this);

      config.setFilterName(filterName);

      if (filter != null)
        config.setFilter(filter);

      config.setFilterClass(className);

      if (filterClass != null)
        config.setFilterClass(filterClass);

      addFilter(config);

      return config;
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

    if (_isApplyingWebFragments)
      servletMapping.setInFragmentMode();

    servletMapping.setWebApp(this);
    servletMapping.setServletContext(this);
    servletMapping.setServletMapper(_servletMapper);
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
    // servletRegexp.initRegexp(this, _servletMapper, _regexp);

    ServletMapping mapping = new ServletMapping();
    
    mapping.addURLRegexp(servletRegexp.getURLRegexp());
    mapping.setServletName(servletRegexp.getServletName());
    if (servletRegexp.getServletClass() != null)
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

    config.setFilterManager(_filterManager);
    config.setWebApp(this);

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

  @Override
  public FilterRegistration getFilterRegistration(String filterName)
  {
    return _filterManager.getFilter(filterName);
  }

  /**
   * Returns filter registrations
   * @return
   */
  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations()
  {
    Map<String, FilterConfigImpl> configMap = _filterManager.getFilters();

    Map<String, FilterRegistration> result
      = new HashMap<String, FilterRegistration>(configMap);

    return Collections.unmodifiableMap(result);
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

    _welcomeFileList = new ArrayList<String>(fileList);
    
    //    _servletMapper.setWelcomeFileList(fileList);
  }
  
  public ArrayList<String> getWelcomeFileList()
  {
    return _welcomeFileList;
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

  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> modes)
  {
    if (modes == null) {
      _sessionManager.setEnableCookies(false);
      _sessionManager.setEnableUrlRewriting(false);
      
      return;
    }
    
    _sessionManager.setEnableCookies(modes.contains(SessionTrackingMode.COOKIE));
    _sessionManager.setEnableUrlRewriting(modes.contains(SessionTrackingMode.URL));
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

      setInitParam(key, value);
    }
  }

  /**
   * Adds an error page
   */
  @Configurable
  public void addErrorPage(ErrorPage errorPage)
  {
    getErrorPageManager().addErrorPage(errorPage);
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
   * Sets the maximum number of form parameters
   */
  @Configurable
  public void setFormParameterMax(int max)
  {
    _formParameterMax = max;
  }
  
  public int getFormParameterMax()
  {
    return _formParameterMax;
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
    _localeMapping.put(locale.toLowerCase(Locale.ENGLISH), encoding);
  }

  /**
   * Returns the locale encoding.
   */
  public String getLocaleEncoding(Locale locale)
  {
    String encoding;

    String key = locale.toString();
    encoding = _localeMapping.get(key.toLowerCase(Locale.ENGLISH));

    if (encoding != null)
      return encoding;

    if (locale.getVariant() != null) {
      key = locale.getLanguage() + '_' + locale.getCountry();
      encoding = _localeMapping.get(key.toLowerCase(Locale.ENGLISH));
      if (encoding != null)
        return encoding;
    }

    if (locale.getCountry() != null) {
      key = locale.getLanguage();
      encoding = _localeMapping.get(key.toLowerCase(Locale.ENGLISH));
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
    setLogin(loginConfig.getLogin());
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
    
    if (rule.isForward()) {
      if (_forwardRewriteDispatch == null)
        _forwardRewriteDispatch = new RewriteDispatch(this);

      _forwardRewriteDispatch.addRule(rule);
    }
    
    if (rule.isInclude()) {
      if (_includeRewriteDispatch == null)
        _includeRewriteDispatch = new RewriteDispatch(this);

      _includeRewriteDispatch.addRule(rule);
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
      _rewriteRealPath = new RewriteRealPath(getRootDirectory());

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
      RedirectSecure redirect = new RedirectSecure();
      // redirect.addURLPattern("/*");
      redirect.add(new Not(new IfSecure()));

      add(redirect);
    }
  }
  
  public boolean isSecure()
  {
    return _isSecure;
  }
  
  public Boolean isRequestSecure()
  {
    Host host = _host;
    
    if (host != null)
      return host.isRequestSecure();
    else
      return null;
  }

  @Override
  public <T extends EventListener> T createListener(Class<T> listenerClass)
    throws ServletException
  {
    try {
      return _cdiManager.createTransientObject(listenerClass);
    }
    catch (InjectionException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void addListener(String className)
  {
    try {
      Class listenerClass = Class.forName(className, false, getClassLoader());

      addListener(listenerClass);
    }
    catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public void addListener(Class<? extends EventListener> listenerClass)
  {
    addListener(_cdiManager.createTransientObject(listenerClass));
  }

  @Override
  public <T extends EventListener> void addListener(T listener)
  {
    addListenerObject(listener, true);
  }

  @Configurable
  public void addListener(ListenerConfig listener)
    throws Exception
  {
    if (! hasListener(listener.getListenerClass())) {
      _listeners.add(listener);
      
      // jsp/18n, server/12t7
      if (_lifecycle.isStarting() || _lifecycle.isActive()) {
        addListenerObject(listener.createListenerObject(), true);
      }
    }
  }

  /**
   * Returns true if a listener with the given type exists.
   */
  public boolean hasListener(Class<?> listenerClass)
  {
    for (int i = 0; i < _listeners.size(); i++) {
      ListenerConfig listener = _listeners.get(i);

      if (listenerClass.equals(listener.getListenerClass()))
        return true;
    }

    return false;
  }

  /**
   * Adds the listener object.
   */
  private void addListenerObject(Object listenerObj, boolean isStart)
  {
    if (listenerObj instanceof ServletContextListener) {
      ServletContextListener scListener = (ServletContextListener) listenerObj;
      
      if (! hasListener(_webAppListeners, listenerObj.getClass())) {
        _webAppListeners.add(scListener);
      
        if (isStart) {
          ServletContextEvent event = new ServletContextEvent(this);

          scListener.contextInitialized(event);
        }
      }
    }

    if (listenerObj instanceof ServletContextAttributeListener) {
      addAttributeListener((ServletContextAttributeListener) listenerObj);
    }

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

    if (listenerObj instanceof HttpSessionListener) {
      getSessionManager().addListener((HttpSessionListener) listenerObj);
    }

    if (listenerObj instanceof HttpSessionAttributeListener) {
      getSessionManager().addAttributeListener((HttpSessionAttributeListener) listenerObj);
    }

    if (listenerObj instanceof HttpSessionActivationListener) {
      getSessionManager().addActivationListener((HttpSessionActivationListener) listenerObj);
    }
  }

  /**
   * Returns true if a listener with the given type exists.
   */
  
  public boolean hasListener(ArrayList<?> listeners, Class<?> listenerClass)
  {
    for (int i = 0; i < listeners.size(); i++) {
      Object listener = listeners.get(i);

      if (listener.getClass().equals(listenerClass)) {
        return true;
      }
    }

    return false;
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
  public boolean isMultipartFormEnabled()
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
   * Returns the form upload max.
   */
  public long getFormParameterLengthMax()
  {
    if (_multipartForm != null)
      return _multipartForm.getParameterLengthMax();
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
      //_jsp.setDependencyCheckIntervalMillis(getEnvironmentClassLoader().getDependencyCheckInterval());
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

  public boolean isFacesServletConfigured()
  {
    return _servletManager.isFacesServletConfigured();
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
    if (_taglibList == null)
      _taglibList = new ArrayList<JspTaglib>();

    _taglibList.add(taglib);
  }

  /**
   * Returns the taglib configuration.
   */
  public ArrayList<JspTaglib> getTaglibList()
  {
    return _taglibList;
  }

  @Override
  public Collection<TaglibDescriptor> getTaglibs()
  {
    ArrayList<TaglibDescriptor> taglibs
      = new ArrayList<TaglibDescriptor>();

    for (int i = 0; _taglibList != null && i < _taglibList.size(); i++)
      taglibs.add(_taglibList.get(i));

    JspConfig jspConfig = (JspConfig) _extensions.get("jsp-config");

    if (jspConfig != null) {
      ArrayList<JspTaglib> taglibList = jspConfig.getTaglibList();
      for (int i = 0; i < taglibList.size(); i++)
        taglibs.add(taglibList.get(i));
    }

    return taglibs;
  }

  @Override
  public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups()
  {
    JspConfig jspConfig = (JspConfig) _extensions.get("jsp-config");

    Collection<JspPropertyGroupDescriptor> propertyGroups
      = new ArrayList<JspPropertyGroupDescriptor>();

    if (jspConfig != null) {
      ArrayList<JspPropertyGroup> groups = jspConfig.getJspPropertyGroupList();
      for (JspPropertyGroup group : groups) {
        propertyGroups.add(group);
      }
    }

    return propertyGroups;
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

  @Override
  public JspConfigDescriptor getJspConfigDescriptor()
  {
    return this;
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

    String appDir = config.getRootDirectory();

    if (appDir == null)
      appDir = "./" + prefix;

    Path root = PathBuilder.lookupPath(appDir, null, getRootDirectory());

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
   * Returns true if should ignore client disconnect.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return getServer().isIgnoreClientDisconnect();
  }

  /**
   * Sets the delay time waiting for requests to end.
   */
  @Configurable
  public void setShutdownWaitMax(Period wait)
  {
    _shutdownWaitTime = wait.getPeriod();

    Resin resin = Resin.getCurrent();
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

  public long getActiveWaitTime()
  {
    return _activeWaitTime;
  }

  /**
   * Sets the error page waiting for a restart
   */
  @Configurable
  public void setActiveWaitErrorPage(String location)
  {
    _activeWaitErrorPage = location;
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
  
  public boolean isSendfileEnabled()
  {
    return _server.isSendfileEnable();
  }
  
  public void addSendfileCount()
  {
    _server.addSendfileCount();
  }
  
  /**
   * Returns the minimum length for a caching sendfile
   */
  public long getSendfileMinLength()
  {
    return _server.getSendfileMinLength();
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
  @Override
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
    ProtocolConnection serverRequest = TcpSocketLink.getCurrentRequest();

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
    if (! _lifecycle.toInitializing()) {
      return;
    }

    try {
      _classLoader.setId("web-app:" + getId());

      _invocationDependency.setCheckInterval(getEnvironmentClassLoader().getDependencyCheckInterval());

      if (_tempDir == null) {
        _tempDir = (Path) Environment.getLevelAttribute("caucho.temp-dir");
      }

      try {
        if (_tempDir == null) {
          _tempDir = getRootDirectory().lookup("WEB-INF/tmp");

          if (getRootDirectory().lookup("WEB-INF").isDirectory()) {
            _tempDir.mkdirs();
          }
        }
        else {
          _tempDir.mkdirs();
        }
      } catch (IOException e) {
        log.warning(e.toString());
        
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, e.toString(), e);
      }

      setAttribute("javax.servlet.context.tempdir", new File(_tempDir.getNativePath()));

      _securityBuilder = _constraintManager.getFilterBuilder();
      
      /*
      if (_securityBuilder != null) {
        _filterMapper.addTopFilter(_securityBuilder);
      }
      */

      if (_server != null) {
        _proxyCache = _server.getProxyCache();
      }

      for (int i = 0; i < _appGenerators.size(); i++) {
        _parent.addDeploy(_appGenerators.get(i));
      }

      _classLoader.setId("web-app:" + getId());

      _cdiManager = InjectManager.getCurrent();
      
      // env/0e3a
      // _cdiManager.update();
      
      // server/1al4 vs server/1ak1, server/1la5
      /*
      SingleSignon singleSignon = AbstractSingleSignon.getCurrent();
      
      if (singleSignon == null && Server.getCurrent() != null) {
        if (getSessionManager().isUsePersistentStore()) {
          ClusterSingleSignon clusterSignon = new ClusterSingleSignon("web-app");
          singleSignon = clusterSignon;
        }
        else {
          MemorySingleSignon memorySignon = new MemorySingleSignon();
          memorySignon.init();
          singleSignon = memorySignon;
        }
          
        AbstractSingleSignon.setCurrent(singleSignon);
      }
      */

      // server/5030
      _cdiManager.addBeanDiscover(_cdiManager.createManagedBean(WebServiceContextProxy.class));

      /*
      _beanManager.addObserver(new WebBeansObserver(),
                            BeanManager.class,
                            new AnnotationLiteral<Initialized>() {});
      */

      if (! _isMetadataComplete) {
        initWebFragments();
      }

      WebAppController parent = null;
      if (_controller != null)
        parent = _controller.getParent();
      if (_isInheritSession && parent != null
          && _sessionManager != parent.getWebApp().getSessionManager()) {
        SessionManager sessionManager = _sessionManager;
        _sessionManager = parent.getWebApp().getSessionManager();

        if (sessionManager != null) {
          sessionManager.close();
        }
      }

      if (_server != null) {
        if (getSessionManager() != null) {
          getSessionManager().init();

          if (_sessionManager.getCookieDomainRegexp() != null) {
            _cookieDomainPattern
              = Pattern.compile(_sessionManager.getCookieDomainRegexp());
          }
          
          addListenerObject(new WebAppSessionListener(), true);
        }
      }

      _roleMapManager = RoleMapManager.create();

      _characterEncoding = CharacterEncoding.getLocalEncoding();
      
      WelcomeFile welcomeFile = new WelcomeFile(_welcomeFileList);
      
      _welcomeFile = welcomeFile;
      
      // add(welcomeFile);
      
      initCdiJsfContext();

      if (! _isCompileContext) {
        for (int i = 0; i < _resourceValidators.size(); i++) {
          Validator validator = _resourceValidators.get(i);

          validator.validate();
        }
      }
    } finally {
      _lifecycle.toInit();
    }
  }
  
  private void initCdiJsfContext()
  {
    try {
      String handler = "com.caucho.server.webbeans.ConversationJsfViewHandler";
      
      Class<?> cl = Class.forName(handler, false, getClassLoader());
      
      if (cl != null && cl.getMethods() != null) {
        getEnvironmentClassLoader().putResourceAlias("META-INF/faces-config.xml",
                                                     "META-INF/faces-config.xml.in");
        
        getEnvironmentClassLoader().putResourceAlias("META-INF/services/com.sun.faces.spi.injectionprovider",
                                                     "META-INF/services/com.sun.faces.spi.injectionprovider.in");
      }
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  public void initWebFragments()
  {
    loadWebFragments();

    List<WebAppFragmentConfig> fragments = sortWebFragments();

    _webFragments = fragments;

    _isApplyingWebFragments = true;

    for (WebAppConfig fragment : fragments) {
      fragment.getBuilderProgram().configure(this);
    }
    
    _isApplyingWebFragments = false;
  }

  public boolean isApplyingWebFragments()
  {
    return _isApplyingWebFragments;
  }

  public boolean isAllowInitParamOverride()
  {
    return _servletVersion == null || ! _servletVersion.startsWith("3");
  }

  private void loadWebFragments()
  {
    if (_webFragments == null)
      _webFragments = new ArrayList<WebAppFragmentConfig>();

    try {
      Enumeration<URL> fragments
        = _classLoader.getResources("META-INF/web-fragment.xml");

      Config config = new Config();
      config.setEL(_servletAllowEL);

      if (log.isLoggable(Level.FINER) && fragments.hasMoreElements())
        log.finer(L.l("{0} loading web-fragments", this));

      while (fragments.hasMoreElements()) {
        URL url = fragments.nextElement();

        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER,
            L.l("Loading web-fragment '{0}:{1}'.", this, url));
        }

        WebAppFragmentConfig fragmentConfig = new WebAppFragmentConfig();
        fragmentConfig.setRootPath(getRootPath(Vfs.lookup(url.toString()),
                                               "META-INF/web-fragment.xml"));
        config.configure(fragmentConfig, Vfs.lookup(url.toString()));

        _webFragments.add(fragmentConfig);
      }
    } catch (IOException e) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE,
          L.l("'{0}' error '{1}' loading fragments.", this, e), e);
    }
  }

  private List<WebAppFragmentConfig> sortWebFragments() 
  {
    Map<String, WebAppFragmentConfig> namedFragments
      = new HashMap<String, WebAppFragmentConfig>();

    List<WebAppFragmentConfig> anonFragments =
      new ArrayList<WebAppFragmentConfig>();

    for (WebAppFragmentConfig fragment : _webFragments) {
      if (fragment.getName() != null)
        namedFragments.put(fragment.getName(), fragment);
      else
        anonFragments.add(fragment);
    }

    if (_absoluteOrdering != null) {
      return getWebFragments(null,
                             _absoluteOrdering,
                             namedFragments,
                             anonFragments);
    } else {
      List<WebAppFragmentConfig> source = new ArrayList<WebAppFragmentConfig>();
      List<WebAppFragmentConfig> result = new ArrayList<WebAppFragmentConfig>();
      Set<String> names = getOrderingNames(_webFragments);
      
      source.addAll(_webFragments);
      
      sortFragments(names, source, result);
      
      return result;
    }
  }
  
  private Set<String> getOrderingNames(List<WebAppFragmentConfig> fragments)
  {
    Set<String> names = new HashSet<String>();
    
    for (WebAppFragmentConfig fragment : fragments) {
      Ordering ordering = fragment.getOrdering();
      
      if (ordering != null) {
        addOrderingNames(names, fragment.getName(), ordering.getBefore());
        addOrderingNames(names, fragment.getName(), ordering.getAfter());
      }
    }
    
    return names;
  }
  
  private void addOrderingNames(Set<String> names,
                                String selfName,
                                Ordering ordering)
  {
    if (ordering == null)
      return;
    
    for (Object order : ordering.getOrder()) {
      if (order instanceof String) {
        names.add((String) order);
      }
      else if (ordering.isOthers(order)) {
        if (selfName != null)
          names.add(selfName);
      }
    }
  }
  
  
  private void sortFragments(Set<String> names,
                             List<WebAppFragmentConfig> sourceList,
                             List<WebAppFragmentConfig> resultList)
  {
    while (sourceList.size() > 0) {
      int sourceSize = sourceList.size();
      
      for (int i = 0; i < sourceList.size(); i++) {
        WebAppFragmentConfig source = sourceList.get(i);

        if (isFragmentInsertable(source, names, sourceList, resultList)) {
          resultList.add(source);
          sourceList.remove(source);
        }
      }
      
      if (sourceList.size() == sourceSize) {
        throw new ConfigException(L.l("web-fragments at '{0}' appear to have circular dependency. Consider using <absolute-ordering> in web.xml.\n  {1}", 
                                      this,
                                      sourceList));
        
      }
    }
  }
  
  private boolean isFragmentInsertable(WebAppFragmentConfig source,
                                       Set<String> names,
                                       List<WebAppFragmentConfig> sourceList,
                                       List<WebAppFragmentConfig> resultList)
  {
    if (isBeforeOrderingPresent(source, names, sourceList)) {
      return false;
    }
    
    if (! isBeforeOrderingValid(source, names, sourceList, resultList)) {
      return false;
    }
    
    if (! isAfterOrderingValid(source, names, sourceList, resultList)) {
      return false;
    }

    return true;
  }
  
  private boolean isAllOrderingPresent(List<WebAppFragmentConfig> list,
                                       Ordering ordering)
  {
    if (ordering == null)
      return true;
    
    for (Object key : ordering.getOrder()) {
      if (key instanceof String) {
        String keyName = (String) key;
        
        if (! isFragmentInList(list, keyName)) {
          return false;
        }
      }
    }

    return true;
  }
  
  private boolean isBeforeOrderingValid(WebAppFragmentConfig source,
                                        Set<String> names,
                                        List<WebAppFragmentConfig> sourceList,
                                        List<WebAppFragmentConfig> resultList)
  {
    for (WebAppFragmentConfig fragment : resultList) {
      Ordering ordering = fragment.getOrdering();
      
      if (ordering != null) {
        Ordering before = ordering.getBefore();
        
        if (! isBeforeOrderingValid(before, source, names, 
                                    sourceList, resultList)) {
          return false;
        }
      }
    }
    
    return true;
  }
  
  private boolean isBeforeOrderingPresent(WebAppFragmentConfig source,
                                          Set<String> names,
                                          List<WebAppFragmentConfig> sourceList)
  {
    for (WebAppFragmentConfig fragment : sourceList) {
      if (fragment == source)
        continue;
      
      Ordering ordering = fragment.getOrdering();
      
      if (ordering != null) {
        Ordering before = ordering.getBefore();
        
        if (isBeforeOrderingPresent(before, source, names, sourceList)) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  private boolean isBeforeOrderingValid(Ordering before,
                                        WebAppFragmentConfig source,
                                        Set<String> names,
                                        List<WebAppFragmentConfig> sourceList,
                                        List<WebAppFragmentConfig> resultList)
  {
    if (before == null)
      return true;
    
    String name = source.getName();
    
    boolean isOthers = false;
    
    for (Object order : before.getOrder()) {
      if (name != null && name.equals(order)) {
        return ! isOthers;
      }
      
      if (before.isOthers(order)) {
        if (isOther(name, names)) {
          return true;
        }
        
        isOthers = isAnyOther(names, sourceList);
      }
      else if (order instanceof String) {
        String key = (String) order;
        
        if (! isFragmentInList(resultList, key))
          return false;
      }
    }
    
    return true;
  }
  
  private boolean isAfterOrderingValid(WebAppFragmentConfig source,
                                       Set<String> names,
                                       List<WebAppFragmentConfig> sourceList,
                                       List<WebAppFragmentConfig> resultList)
  {
    Ordering ordering = source.getOrdering();
    
    if (ordering == null)
      return true;
    
    Ordering after = ordering.getAfter();
    
    if (after == null)
      return true;
    
    for (Object order : after.getOrder()) {
      if (after.isOthers(order)) {
        if (isAnyOther(names, sourceList))
          return false;
      }
      else if (order instanceof String) {
        String key = (String) order;
        
        if (! isFragmentInList(resultList, key))
          return false;
      }
    }
    
    return true;
  }
  
  private boolean isFragmentInList(List<WebAppFragmentConfig> list,
                                   String keyName)
  {
    for (WebAppFragmentConfig config : list) {
      if (keyName.equals(config.getName()))
        return true;
    }
    
    return false;
  }
  
  private boolean isOther(String name, Set<String> names)
  {
    return name == null || ! names.contains(name);
  }
  
  private boolean isBeforeOrderingPresent(Ordering before,
                                          WebAppFragmentConfig source,
                                          Set<String> names,
                                          List<WebAppFragmentConfig> sourceList)
  {
    if (before == null)
      return false;
    
    String name = source.getName();
    
    for (Object subOrder : before.getOrder()) {
      if (name != null && name.equals(subOrder))
        return true;
      
      if (before.isOthers(subOrder)) {
        if (isOther(name, names))
          return true;
      }
    }
    
    return false;
  }
  
  private boolean isOrderingPresent2(Ordering order,
                                    String name,
                                    Set<String> names,
                                    List<WebAppFragmentConfig> sourceList)
  {
    for (Object subOrder : order.getOrder()) {
      if (name != null && name.equals(subOrder))
        return true;
      
      if (order.isOthers(subOrder)) {
        if (isOther(name, names))
          return true;
        
        // If an others fragment is still in the source list, add it.
        if (isAnyOther(names, sourceList)) {
          return false;
        }
      }
    }
    
    return false;
  }
  
  private boolean isAnyOther(Set<String> names,
                             List<WebAppFragmentConfig> sourceList)
  {
    for (WebAppFragmentConfig fragment : sourceList) {
      if (isOther(fragment.getName(), names)) {
        return true;
      }
    }
    
    return false;
  }
  
  private boolean isAnyOther(String selfName,
                             Set<String> names,
                             List<WebAppFragmentConfig> sourceList)
  {
    for (WebAppFragmentConfig fragment : sourceList) {
      if (selfName != null && selfName.equals(fragment.getName())) {
        // server/1r20
        continue;
      }
      
      if (isOther(fragment.getName(), names)) {
        return true;
      }
    }
    
    return false;
  }
  
  /*
      Map<WebAppFragmentConfig, Set<WebAppFragmentConfig>> parentsMap
        = new HashMap<WebAppFragmentConfig, Set<WebAppFragmentConfig>>();

      for (WebAppFragmentConfig config : namedFragments.values()) {
        if (config.getOrdering() == null)
          continue;

        List<WebAppFragmentConfig> children
          = getWebFragments(config,
                            config.getOrdering().getBefore(),
                            namedFragments,
                            anonFragments);

        List<WebAppFragmentConfig> parents
          = getWebFragments(config,
                            config.getOrdering().getAfter(),
                            namedFragments,
                            anonFragments);

        if (children != null && parents != null) {
          for (WebAppFragmentConfig fragmentConfig : children) {
            if (parents.contains(fragmentConfig))
              throw new ConfigException(L.l(
                "web-fragment '{0}' specifies conflicting ordering in its before and after tags. Consider using <absolute-ordering> in web.xml",
                config));
          }
        }


        if (children != null) {
          for (WebAppFragmentConfig child : children) {
            Set<WebAppFragmentConfig> parentSet = parentsMap.get(child);

            if (parentSet == null)
              parentSet = new HashSet<WebAppFragmentConfig>();

            parentSet.add(config);

            parentsMap.put(child, parentSet);
          }
        }

        if (parents != null) {
          Set<WebAppFragmentConfig> parentsSet = parentsMap.get(config);

          if (parentsSet == null)
            parentsSet = new HashSet<WebAppFragmentConfig>();

          parentsSet.addAll(parents);
          parentsMap.put(config, parentsSet);
        }
      }

      List<WebAppFragmentConfig> result = new ArrayList<WebAppFragmentConfig>();

      while (! parentsMap.isEmpty()) {
        int resultSize = 0;

        Set<WebAppFragmentConfig> removeSet
          = new HashSet<WebAppFragmentConfig>();

        for (
          Iterator<Map.Entry<WebAppFragmentConfig, Set<WebAppFragmentConfig>>>
            entries = parentsMap.entrySet().iterator(); entries.hasNext();) {
          Map.Entry<WebAppFragmentConfig, Set<WebAppFragmentConfig>> entry
            = entries.next();
          for (Iterator<WebAppFragmentConfig> iterator
            = entry.getValue().iterator(); iterator.hasNext();) {
            WebAppFragmentConfig config = iterator.next();

            if (result.contains(config))
              iterator.remove();
            else if (parentsMap.get(config) == null
              || parentsMap.get(config).isEmpty()) {
              result.add(config);
              removeSet.remove(config);
            }
          }

          if (entry.getValue().isEmpty())
            entries.remove();
        }

        for (WebAppFragmentConfig config : removeSet) {
          parentsMap.remove(config);
        }

        if (result.size() == resultSize)
          throw new ConfigException(L.l("web-fragments at '{0}' appear to have circular dependency. Consider using <absolute-ordering> in web.xml.", this));
      }

      for (WebAppFragmentConfig config : namedFragments.values()) {
        if (! result.contains(config))
          result.add(config);
      }

      if (anonFragments.size() > 0)
        result.addAll(anonFragments);

      return result;
    }
  }
  */

  private List<WebAppFragmentConfig> getWebFragments(final WebAppFragmentConfig config,
                                                     Ordering ordering,
                                                     Map<String, WebAppFragmentConfig> namedFragments,
                                                     List<WebAppFragmentConfig> anonFragments)
  {
    if (ordering == null)
      return null;

    Map<String, WebAppFragmentConfig> others
      = new HashMap<String, WebAppFragmentConfig>(namedFragments);

    if (config != null)
      others.remove(config.getName());

    ArrayList<WebAppFragmentConfig> result
      = new ArrayList<WebAppFragmentConfig>();

    int othersIdx = -1;

    for (Object key : ordering.getOrder()) {
      if (key instanceof String) {
        WebAppFragmentConfig fragmentConfig = others.remove(key);

        result.add(fragmentConfig);
      } else if (ordering.isOthers(key)) {
        othersIdx = result.size();
      }
    }

    if (othersIdx > -1) {
      result.ensureCapacity(result.size() + others.size());

      result.addAll(othersIdx, others.values());
      result.addAll(anonFragments);

      anonFragments.clear();
    }

    return result;
  }

  private void loadInitializers()
    throws Exception
  {
    Class<?> cl = ServletContainerInitializer.class;
    
    Enumeration<URL> e;
    e = getClassLoader().getResources("META-INF/services/" + cl.getName());
    
    if (e == null) {
      return;
    }
    
    while (e.hasMoreElements()) {
      URL url = e.nextElement();
      
      // might parse to check that the loader has a handles
      
      if (_classHierarchyScanListener == null) {
        _classHierarchyScanListener = new ClassHierarchyScanListener(getClassLoader());
      
        getEnvironmentClassLoader().addScanListener(_classHierarchyScanListener);
      }
    }
  }

  private void callInitializers()
    throws Exception
  {
    ArrayList<ListenerConfig> listeners = new ArrayList<ListenerConfig>(_listeners);
    ArrayList<ServletContextListener> webAppListeners
      = new ArrayList<ServletContextListener>(_webAppListeners);
    
    ArrayList<ServletContainerInitializer> initList
      = new ArrayList<ServletContainerInitializer>(_cdiManager.loadLocalServices(ServletContainerInitializer.class));
    
    Collections.sort(initList, new InitComparator());

    for (ServletContainerInitializer init : initList) {
      callInitializer(init);
    }
    
    _classHierarchyScanListener = null;
    
    for (ListenerConfig listener : listeners) {
      try {
        // server/10g0
        // addListenerObject(listener.createListenerObject(), false);
        addListenerObject(listener.createListenerObject(), true);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    ServletContextEvent event = new ServletContextEvent(this);

    for (ServletContextListener listener : webAppListeners) {
      listener.contextInitialized(event);
    }
  }
  
  private Path getRootPath(Path path, String name)
  {
    if (path instanceof JarPath) {
      return ((JarPath) path).getContainer();
    }
    else {
      String url = path.getURL();
      
      return path.lookup(url.substring(0, url.length() - name.length()));
    }
  }

  private class InitComparator
    implements Comparator<ServletContainerInitializer>
  {
    @Override
    public int compare(ServletContainerInitializer a,
                       ServletContainerInitializer b)
    {
      ClassLoader loader = getClassLoader();
      
      try {
        String aName = a.getClass().getName().replace('.', '/') + ".class";
        String bName = b.getClass().getName().replace('.', '/') + ".class";
        
        URL aUrl = loader.getResource(aName);
        URL bUrl = loader.getResource(bName);
        
        if (aUrl == null && bUrl == null) {
          return 0;
        }
        
        if (aUrl == null && bUrl != null) {
          return 1;
        }
        
        if (aUrl != null && bUrl == null) {
          return -1;
        }
        
        Path aPath = Vfs.lookup(aUrl.toString());
        Path bPath = Vfs.lookup(bUrl.toString());
        
        Path aRoot = getRootPath(aPath, aName);
        Path bRoot = getRootPath(bPath, bName);
        
        int aIndex = fragmentIndexOf(aRoot);
        int bIndex = fragmentIndexOf(bRoot);
        
        return Integer.signum(aIndex - bIndex);
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
        return 0;
      }
    }
    
    private int fragmentIndexOf(Path root)
    {
      for (int i = 0; i < _webFragments.size(); i++) {
        WebAppFragmentConfig fragment = _webFragments.get(i);

        if (root.equals(fragment.getRootPath())) {
          return i;
        }
      }
      
      return -1;
    }
  }

  private void callInitializer(ServletContainerInitializer init)
    throws ServletException
  {
    HandlesTypes handlesTypes
      = init.getClass().getAnnotation(HandlesTypes.class);

    if (handlesTypes == null) {
      if (log.isLoggable(Level.FINER)){
        log.finer("ServletContainerInitializer " + init + " {in " + this + "}");
      }
      
      init.onStartup(null, this);
      return;
    }
    
    if (_classHierarchyScanListener == null) {
      return;
    }
    
    HashSet<Class<?>> classes 
       = _classHierarchyScanListener.findClasses(handlesTypes.value());
    
    if (classes != null) {
      if (log.isLoggable(Level.FINER)){
        log.finer("ServletContainerInitializer " + init + "(" + classes + ") {in " + this + "}");
      }
      
      init.onStartup(classes, this);
    }
  }

  public void initAnnotated() throws Exception
  {
    List<Class<?>> listeners = new ArrayList<Class<?>>();

    List<Class<? extends Servlet>> servlets
      = new ArrayList<Class<? extends Servlet>>();

    List<Class<?>> filters = new ArrayList<Class<?>>();

    List<String> pendingClasses = new ArrayList<String>();
    
    // server/121e
    if (! _isMetadataComplete) {
      pendingClasses = new ArrayList<String>(_pendingClasses);
      _pendingClasses.clear();
    }

    for (String className : pendingClasses) {
      Class<?> cl = _classLoader.loadClass(className);
      
      if (ServletContextListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (ServletContextAttributeListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (ServletRequestListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (ServletRequestAttributeListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (HttpSessionListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (HttpSessionAttributeListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (Servlet.class.isAssignableFrom(cl))
        servlets.add((Class) cl);
      else if (Filter.class.isAssignableFrom(cl))
        filters.add(cl);
    }

    // server/12t7
    for (Class<?> listenerClass : listeners) {
      if (! isAttributeListener(listenerClass))
        continue;
      
      WebListener webListener
        = listenerClass.getAnnotation(WebListener.class);

      if (webListener != null) {
        ListenerConfig listener = new ListenerConfig();
        listener.setListenerClass(listenerClass);

        addListener(listener);
      }
    }

    for (Class<?> listenerClass : listeners) {
      if (isAttributeListener(listenerClass))
        continue;
      
      WebListener webListener
        = listenerClass.getAnnotation(WebListener.class);

      if (webListener != null) {
        ListenerConfig listener = new ListenerConfig();
        listener.setListenerClass(listenerClass);

        addListener(listener);
      }
    }

    Collections.sort(filters, new ClassComparator());
    for (Class<?> filterClass : filters) {
      WebFilter webFilter
        = filterClass.getAnnotation(WebFilter.class);

      if (webFilter != null)
        addFilter(webFilter, filterClass.getName());
    }

    for (Class<? extends Servlet> servletClass : servlets) {
      WebServlet webServlet
        = servletClass.getAnnotation(WebServlet.class);

      if (webServlet != null)
        addServlet(webServlet, servletClass.getName());

      ServletSecurity servletSecurity
        = (ServletSecurity) servletClass.getAnnotation(ServletSecurity.class);

      if (servletSecurity != null)
        addServletSecurity(servletClass, servletSecurity);
    }
  }
  
  private boolean isAttributeListener(Class<?> cl)
  {
    if (ServletContextAttributeListener.class.isAssignableFrom(cl))
      return true;
    else
      return false;
  }

  public WebAppAdmin getAdmin()
  {
    return _controller.getAdmin();
  }

  @Override
  public void start()
  {
    if (! _lifecycle.isAfterInit()) {
      throw new IllegalStateException(L.l("webApp must be initialized before starting.  Currently in state {0}.", _lifecycle.getStateName()));
    }
    
    synchronized (this) {
      if (_lifecycle.isActive() || _lifecycle.isAfterStopping()) {
        return;
      }
      
      StartupTask task = new StartupTask();
    
      ThreadPool.getCurrent().execute(task);

      task.waitFor(getActiveWaitTime());
    }
    // asdf: wait
  }
  
  private void startImpl(StartupTask task)
  {
    if (! _lifecycle.isAfterInit()) {
      throw new IllegalStateException(L.l("webApp must be initialized before starting.  Currently in state {0}.", _lifecycle.getStateName()));
    }
    
    if (! _lifecycle.toStarting()) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    boolean isOkay = true;

    try {
      thread.setContextClassLoader(_classLoader);

      isOkay = false;
      
      if (_accessLog == null) {
        _accessLog = _accessLogLocal.get();
      }
      
      if (_accessLog != null) {
        _accessLog.start();
      }

      long interval = _classLoader.getDependencyCheckInterval();
      _invocationDependency.setCheckInterval(interval);

      if (_parent != null) {
        _invocationDependency.add(_parent.getWebAppGenerator());
      }

      // Sets the last modified time so the app won't immediately restart
      _invocationDependency.clearModified();
      _classLoader.clearModified();

      String serverId = null;
      
      ServletService server = ServletService.getCurrent();
      
      if (server != null)
        serverId = server.getServerId();
      
      if (serverId != null)
        setAttribute("caucho.server-id", serverId);
      
      if (_isStartDisabled) {
        isOkay = true;
        return;
      }
      
      WebAppEnv env = new WebAppEnv();
      env.init();

      _classLoader.start();

      // configuration exceptions discovered by resources like
      // the persistence manager
      if (_configException == null) {
        _configException = Environment.getConfigException();
      }

      startAuthenticators();

      try {
        if (getSessionManager() != null)
          getSessionManager().start();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      _jspApplicationContext.addELResolver(new CandiElResolver());

      callInitializers();

      //jsp/18n7
      _servletManager.initializeJspServlets();

      //Servlet 3.0
      initAnnotated();

      try {
        _filterManager.init();
        _servletManager.init();

        initSecurityConstraints();
      } catch (Exception e) {
        setConfigException(e);

        // XXX: CDI TCK
        throw e;
      }

      _host.setConfigETag(null);

      _lifecycle.toActive();

      clearCache();
      
      if (! getRootDirectory().canRead()
          && ! getHost().getHostName().equals("admin.resin")) {
        log.warning(this + " cannot read root-directory " + getRootDirectory().getNativePath());
      }

      isOkay = true;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      if (! isOkay)
        _lifecycle.toError();

      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void startAuthenticators()
  {
    try {
      // server/1a36

      Authenticator auth = _cdiManager.getReference(Authenticator.class);

      setAttribute("caucho.authenticator", auth);
    } catch (Exception e) {
      log.finest(e.toString());
    }

    try {
      if (_login == null) {
        _login = _cdiManager.getReference(Login.class);
      }

      if (_login == null) {
        Bean<?> loginBean = _cdiManager.createManagedBean(BasicLogin.class);
        
        _cdiManager.addBeanDiscover(loginBean);
        // server/1aj0
        _defaultLogin = _cdiManager.getReference(Login.class);

        _authenticator = _cdiManager.getReference(Authenticator.class);
      }

      setAttribute("caucho.login", _login);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  /**
   * Returns true if the webApp has been modified.
   */
  @Override
  public boolean isModified()
  {
    // server/13l8
    // _configException test is needed so compilation failures will force
    // restart
    if (_lifecycle.isAfterStopping()) {
      return true;
    }
    else if (DeployMode.MANUAL.equals(_controller.getRedeployMode())) {
      return false;
    }
    else if (_classLoader.isModified()) {
      return true;
    }
    else
      return false;
  }

  /**
   * Returns true if the webApp has been modified.
   */
  @Override
  public boolean isModifiedNow()
  {
    // force check
    _classLoader.isModifiedNow();
    _invocationDependency.isModifiedNow();

    if (_lifecycle.isAfterStopping()) {
      return true;
    }
    else if (_classLoader.isModifiedNow()) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Log the reason for modification.
   */
  @Override
  public boolean logModified(Logger log)
  {
    if (_lifecycle.isAfterStopping()) {
      log.fine(this + " modified after stopping");
      
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
      return _lastRequestTime + _idleTime < CurrentTime.getCurrentTime();
  }

  /**
   * Returns the servlet context for the URI.
   */
  @Override
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
      else if (_host != null) {
        ServletContext subContext = _host.getWebAppContainer().findSubWebAppByURI(uri);
        
        if (subContext == null)
          return null;
        else if (getContextPath().equals(subContext.getContextPath()))
          return this;
        else
          return subContext;
      }
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
  @Override
  public Invocation buildInvocation(Invocation invocation)
  {
    return buildInvocation(invocation, true);
  }

    /**
     * Fills the servlet instance.  (Generalize?)
     */
  public Invocation buildInvocation(Invocation invocation, boolean isTop)
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
      else if (! isEnabled()) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " is disabled '" + invocation.getRawURI() + "'");
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        chain = new ErrorFilterChain(code);
        invocation.setFilterChain(chain);
        invocation.setDependency(AlwaysModified.create());

        return invocation;
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " returned 503 busy for '" + invocation.getRawURI() + "'");
        
        String errorPage = _activeWaitErrorPage;
        int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        
        if (errorPage != null) {
          WebApp subWebApp = getServer().getWebApp("", 0, "/");
          
          if (subWebApp != null) {
            RequestDispatcherImpl disp = subWebApp.getRequestDispatcher(errorPage);
            
            chain = new ForwardErrorFilterChain(disp, code);
            invocation.setFilterChain(chain);
            invocation.setDependency(AlwaysModified.create());
          
            return invocation;
          }
        }

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

          if (! entry.isAsyncSupported())
            invocation.clearAsyncSupported();

          invocation.setMultipartConfig(entry.getMultipartConfig());
        } else {
          chain = _servletMapper.mapServlet(invocation);
          
          // server/13s[o-r]
          _filterMapper.buildDispatchChain(invocation, chain);
          chain = invocation.getFilterChain();

          chain = applyWelcomeFile(DispatcherType.REQUEST, invocation, chain);

          if (_requestRewriteDispatch != null) {
            FilterChain newChain
              = _requestRewriteDispatch.map(DispatcherType.REQUEST,
                                            invocation.getContextURI(),
                                            invocation.getQueryString(),
                                            chain);

            chain = newChain;
          }

                    /*
          // server/13s[o-r]
          // _filterMapper.buildDispatchChain(invocation, chain);
          chain = invocation.getFilterChain();
           */
          
          entry = new FilterChainEntry(chain, invocation);
          chain = entry.getFilterChain();

          if (isCache)
            _filterChainCache.put(invocation.getContextURI(), entry);
        }
        
        chain = buildSecurity(chain, invocation);

        chain = createWebAppFilterChain(chain, invocation, isTop);

        invocation.setFilterChain(chain);
        invocation.setPathInfo(entry.getPathInfo());
        invocation.setServletPath(entry.getServletPath());
      }

      if (_oldWebApp != null
          && CurrentTime.getCurrentTime() < _oldWebAppExpireTime) {
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
      log.log(Level.WARNING, e.toString(), e);

      FilterChain chain = new ExceptionFilterChain(e);
      chain = new WebAppFilterChain(chain, this);
      invocation.setDependency(AlwaysModified.create());
      invocation.setFilterChain(chain);

      return invocation;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private FilterChain applyWelcomeFile(DispatcherType type,
                                       Invocation invocation, 
                                       FilterChain chain)
    throws ServletException
  {
    if ("".equals(invocation.getContextURI())) {
      // server/1u3l
      return new RedirectFilterChain(getContextPath() + "/");
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
                                      Invocation invocation,
                                      boolean isTop)
  {
    // the cache must be outside of the WebAppFilterChain because
    // the CacheListener in ServletInvocation needs the top to
    // be a CacheListener.  Otherwise, the cache won't get lru.
    
    if (getRequestListeners() != null && getRequestListeners().length > 0) {
      chain = new WebAppListenerFilterChain(chain, this, getRequestListeners());
    }
    
    // TCK: cache needs to be outside because the cache flush conflicts
    // with the request listener destroy callback
    // top-level filter elements
    // server/021h - cache not logging

    if (_proxyCache != null) {
      chain = _proxyCache.createFilterChain(chain, this);
    }
    
    WebAppFilterChain webAppChain = new WebAppFilterChain(chain, this);

    // webAppChain.setSecurityRoleMap(invocation.getSecurityRoleMap());
    chain = webAppChain;

    if (_isStatisticsEnabled)
      chain = new StatisticsFilterChain(chain, this);

    if (getAccessLog() != null && isTop) {
      chain = new AccessLogFilterChain(chain, this);
    }
    
    return chain;
  }

  public ServletMapper getServletMapper()
  {
    return _servletMapper;
  }

  /**
   * Clears all caches, including the invocation cache, the filter cache, and the proxy cache.
   */
  public void clearCache()
  {
    // server/1kg1
    synchronized (_filterChainCache) {
      _filterChainCache.clear();
      _dispatcherCache = null;
    }

    WebAppController controller = _controller;

    if (controller != null)
      controller.clearCache();
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
    // buildDispatchInvocation(invocation, _dispatchFilterMapper);
    buildDispatchInvocation(invocation, _filterMapper);
    
    buildSecurity(invocation);
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
      else if (! isEnabled()) {
        Exception exn = new UnavailableException(L.l("'{0}' is not currently available.",
                                                     getContextPath()));
        chain = new ExceptionFilterChain(exn);
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
        chain = filterMapper.buildDispatchChain(invocation, chain);
        
        if (filterMapper == _includeFilterMapper) {
          chain = applyWelcomeFile(DispatcherType.INCLUDE, invocation, chain);
          
          if (_includeRewriteDispatch != null) {
            chain = _includeRewriteDispatch.map(DispatcherType.INCLUDE,
                                                invocation.getContextURI(),
                                                invocation.getQueryString(),
                                                chain);
          }
        }
        else if (filterMapper == _forwardFilterMapper) {
          chain = applyWelcomeFile(DispatcherType.FORWARD, invocation, chain);
          
          if (_forwardRewriteDispatch != null) {
            chain = _forwardRewriteDispatch.map(DispatcherType.FORWARD,
                                                invocation.getContextURI(),
                                                invocation.getQueryString(),
                                                chain);
          }
        }

        /* server/10gw - #3111 */
        /*
        if (getRequestListeners().length > 0)
          chain = new DispatchFilterChain(chain, this); // invocation);
        */

        if (_proxyCache != null && filterMapper == _includeFilterMapper) {
          chain = _proxyCache.createFilterChain(chain, this);
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
  
  private void buildSecurity(Invocation invocation)
  {
    invocation.setFilterChain(buildSecurity(invocation.getFilterChain(),
                                            invocation));
  }
  
  private FilterChain buildSecurity(FilterChain chain, Invocation invocation)
  {
    if (_securityBuilder != null) {
      // server/1ksa
      // _dispatchFilterMapper.addTopFilter(_securityBuilder);
      return _securityBuilder.build(chain, invocation);
    }
    
    
    return chain;
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  @Override
  public RequestDispatcherImpl getRequestDispatcher(String url)
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
    Invocation requestInvocation = dispatchInvocation;
    
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = escapeURL(getContextPath() + url);

    try {
      decoder.splitQuery(includeInvocation, rawURI);
      decoder.splitQuery(forwardInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);
      decoder.splitQuery(dispatchInvocation, rawURI);

      // server/1h57
      boolean isSameWebApp = false;
      if (_parent != null) {
        WebAppController subController
          = _parent.getWebAppController(includeInvocation);

        // server/1233
        _parent.getWebAppController(forwardInvocation);
        _parent.getWebAppController(errorInvocation);
        _parent.getWebAppController(dispatchInvocation);

        if (subController != null
            && (_controller.getContextPath()
                .equals(subController.getContextPath()))) {
          isSameWebApp = true;
        }
      }
      
      if (_parent != null && ! isSameWebApp) {
        // jsp/15ll
        _parent.buildIncludeInvocation(includeInvocation);
        _parent.buildForwardInvocation(forwardInvocation);
        _parent.buildErrorInvocation(errorInvocation);
        _parent.buildDispatchInvocation(dispatchInvocation);
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
        throw new IllegalStateException(L.l("web-app '{0}' is restarting and is not yet ready to receive requests. state={1}",
                                            getVersionContextPath(), _lifecycle));
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
  
  private void buildCrossContextFilter(Invocation invocation)
  {
    FilterChain chain = invocation.getFilterChain();
    
    chain = new DispatchFilterChain(chain, invocation.getWebApp());
    
    invocation.setFilterChain(chain);
  }

  /**
   * Access logging for high-level errors
   */
  public void accessLog(HttpServletRequest req, HttpServletResponse res)
  {
    AbstractAccessLog accessLog = getAccessLog();

    if (accessLog != null) {
      try {
        accessLog.log(req, res, this);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
        /*
        log.warning("AccessLog: " + e);
        
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, "AccessLog: " + e.toString(), e);
        }
        */
      }
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
      throw new IllegalArgumentException(L.l("request dispatcher url '{0}' must be absolute", url));

    Invocation loginInvocation = new Invocation();
    Invocation errorInvocation = new Invocation();
    InvocationDecoder decoder = new InvocationDecoder();

    String rawURI = getContextPath() + url;

    try {
      decoder.splitQuery(loginInvocation, rawURI);
      decoder.splitQuery(errorInvocation, rawURI);

      if (! isEnabled()) {
        throw new IllegalStateException(L.l("'{0}' is disable and unavailable to receive requests",
                                            getVersionContextPath()));
      }
      else if (! _lifecycle.waitForActive(_activeWaitTime)) {
        throw new IllegalStateException(L.l("'{0}' is restarting and it not yet ready to receive requests",
                                            getVersionContextPath()));
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
  @Override
  public RequestDispatcher getNamedDispatcher(String servletName)
  {
    try {
      Invocation invocation = new Invocation();
      invocation.setWebApp(this);

      FilterChain chain
        = _servletManager.createServletChain(servletName, null, invocation);

      FilterChain includeChain
        = _includeFilterMapper.buildFilterChain(chain, servletName);
      FilterChain forwardChain
        = _forwardFilterMapper.buildFilterChain(chain, servletName);

      return new NamedDispatcherImpl(includeChain, forwardChain, invocation, 
                                     null, this);
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
    // server/10m7
    if (uri == null)
      return null;

    String realPath = _realPathCache.get(uri);

    if (realPath != null)
      return realPath;

    WebApp webApp = this;
    String tail = uri;
    
    if (isActive()) {
      String fullURI = getContextPath() + "/" + uri;

      try {
        fullURI = getInvocationDecoder().normalizeUri(fullURI);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      webApp = (WebApp) getContext(fullURI);

      if (webApp == null)
        webApp = this;

      String cp = webApp.getContextPath();
      tail = fullURI.substring(cp.length());
    }

    realPath = webApp.getRealPathImpl(tail);

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
      fullURI = getInvocationDecoder().normalizeUri(fullURI);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    WebApp webApp = (WebApp) getContext(fullURI);

    if (webApp == null)
      return null;

    int p = uri.lastIndexOf('.');

    if (p < 0)
      return null;
    else
      return webApp.getMimeTypeImpl(uri.substring(p));
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
    if (_login != null)
      return _login;
    else
      return _defaultLogin;
  }

  public Login getConfiguredLogin()
  {
    // server/1aj0
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
   * Gets the authenticator
   */
  public Authenticator getConfiguredAuthenticator()
  {
    Login login = getConfiguredLogin();

    if (login != null)
      return login.getAuthenticator();

    return _authenticator;
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig()
  {
    return getSessionManager();
  }

  /**
   * Gets the session manager.
   */
  public SessionManager getSessionManager()
  {
    if (_sessionManager == null) {
      if (_lifecycle.isAfterStopping())
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
    if (_errorPageManager == null) {
      _errorPageManager = new ErrorPageManager(_server, this);
      _errorPageManager.setParent(_host.getErrorPageManager());
    }
    
    return _errorPageManager;
  }

  /**
   * Called when a request starts the webApp.
   */
  final boolean enterWebApp()
  {
    _requestCount.incrementAndGet();
    _lastRequestTime = CurrentTime.getCurrentTime();

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
    CacheMapping map = _cacheMappingMap.map(uri);
    
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
    CacheMapping map = _cacheMappingMap.map(uri);

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
    return _proxyCache.getMaxEntrySize();
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

  public String generateCookieDomain(HttpServletRequest request)
  {
    String serverName = request.getServerName();

    if (_cookieDomainPattern == null)
      return _sessionManager.getCookieDomain();

    String domain;
    Matcher matcher = _cookieDomainPattern.matcher(serverName);

    // XXX: performance?
    if (matcher.find()) {
      domain = matcher.group();
    } else {
      domain = null;
    }

    return domain;
  }

  void updateStatistics(long time,
                        int readBytes,
                        int writeBytes,
                        boolean isClientDisconnect)
  {
    _controller.updateStatistics(time, readBytes, writeBytes, isClientDisconnect);
  }
  
  //
  // thread/cdi
  //
  
  /**
   * Runs a thread in a session context
   */
  public void runInSessionContext(String sessionId, Runnable task)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldClassLoader = thread.getContextClassLoader();
    ProtocolConnection serverRequest = TcpSocketLink.getCurrentRequest();
    StubSessionContextRequest stubRequest;
    
    try {
      stubRequest = new StubSessionContextRequest(this, sessionId);
      
      TcpSocketLink.setCurrentRequest(stubRequest);
      
      task.run();
    } finally {
      thread.setContextClassLoader(oldClassLoader);
      TcpSocketLink.setCurrentRequest(serverRequest);
    }
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

      if (! _lifecycle.toStopping()) {
        return;
      }
      
      long beginStop = CurrentTime.getCurrentTimeActual();

      clearCache();

      while (_requestCount.get() > 0
             && CurrentTime.getCurrentTimeActual() < beginStop + _shutdownWaitTime) {
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
          && (! _isInheritSession || _controller.getParent() == null)) {
        sessionManager.close();
      }

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
        ListenerConfig listener = _listeners.get(i);

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
      
      clearCache();
    }
  }

  /**
   * Closes the webApp.
   */
  @Override
  public void destroy()
  {
    try {
      stop();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    if (! _lifecycle.toDestroy()) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      for (int i = _appGenerators.size() - 1; i >= 0; i--) {
        try {
          DeployGenerator<WebAppController> deploy = _appGenerators.get(i);
          _parent.removeWebAppDeploy(deploy);
          deploy.destroy();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      AbstractAccessLog accessLog = _accessLog;
      _accessLog = null;

      if (accessLog != null) {
        try {
          accessLog.flush();
          accessLog.destroy();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      _classLoader.destroy();

      clearCache();
    }
  }

  //
  // statistics
  //

  public void addStatus500()
  {
    synchronized (this) {
      _status500CountTotal++;
      _status500LastTime = CurrentTime.getCurrentTime();
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
    return new SingletonBindingHandle(WebApp.class);
  }

  @Override
  public String toString()
  {
    if (_lifecycle == null)
      return "WebApp[" + getId() + "]";
    else if (_lifecycle.isActive())
      return "WebApp[" + getId() + "]";
    else
      return "WebApp[" + getId() + "," + _lifecycle.getState() + "]";
  }

  static class WebAppSessionListener implements HttpSessionListener {
    @Override
    public void sessionCreated(HttpSessionEvent se)
    {

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se)
    {
      HttpSession session = se.getSession();
      
      try {
        SessionContextContainer candiContainer
          = (SessionContextContainer) session.getAttribute("resin.candi.scope");
      
        if (candiContainer != null)
          candiContainer.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
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
    boolean _isAsyncSupported;
    MultipartConfigElement _multipartConfig;

    FilterChainEntry(FilterChain filterChain, Invocation invocation)
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

  class WebFragmentScanner implements ScanListener {
    public int getScanPriority()
    {
      return 2;
    }

    @Override
    public boolean isRootScannable(Path root, String packageRoot)
    {
      return true;
    }

    @Override
    public ScanClass scanClass(Path root, String packageRoot,
                               String name, int modifiers)
    {
      if (Modifier.isPublic(modifiers))
        return new WebScanClass(name);
      else
        return null;
    }

    @Override
    public boolean isScanMatchAnnotation(CharBuffer string)
    {
      if (string.startsWith("javax.servlet.annotation."))
        return true;

      return false;
    }

    @Override
    public void classMatchEvent(EnvironmentClassLoader loader,
                                Path root,
                                String className)
    {
      _pendingClasses.add(className);
    }
  }

  class WebScanClass extends AbstractScanClass {
    private String _className;
    private boolean _isValid;
    
    WebScanClass(String className)
    {
      _className = className;
    }
    
    @Override
    public void addClassAnnotation(char [] buffer, int offset, int length)
    {
      if (length < SERVLET_ANNOTATION.length)
        return;
      
      for (int i = SERVLET_ANNOTATION.length - 1; i >= 0; i--) {
        if (buffer[offset + i] != SERVLET_ANNOTATION[i])
          return;
      }
      
      _isValid = true;      
    }

    /**
     * Complete scan processing.
     */
    @Override
    public boolean finishScan()
    {
      if (_isValid) {
        _pendingClasses.add(_className);
        return true;
      }
      else
        return false;
    }
  }
  
  private class StartupTask implements Runnable {
    private BasicFuture<Boolean> _future
      = new BasicFuture<Boolean>();
    
    void waitFor(long timeout)
    {
      try {
        _future.get(timeout, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        log.log(Level.FINER, e.toString(), e);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    @Override
    public void run()
    {
      boolean isValid = false;
      
      try {
        startImpl(this);
        
        _future.complete(true);
        isValid = true;
      } catch (RuntimeException exn) {
        _future.complete(exn);
        isValid = true;
      } catch (Error exn) {
        log.log(Level.FINER, exn.toString(), exn);
        
        _future.complete(new IllegalStateException(exn));
        isValid = true;
      } finally {
        if (! isValid) {
          _future.complete(new IllegalStateException());
        }
      }
    }
  }
  
  static class ClassComparator implements Comparator<Class<?>> {
    @Override
    public int compare(Class<?> a, Class<?> b)
    {
      return a.getName().compareTo(b.getName());
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
