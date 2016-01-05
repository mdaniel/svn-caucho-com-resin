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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.TaglibDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.websocket.server.ServerContainer;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.queue.OutboxContext;
import com.caucho.v5.amp.spi.MessageAmp;
import com.caucho.v5.amp.spi.OutboxAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.config.types.Validator;
import com.caucho.v5.deploy.DeployInstanceEnvironment;
import com.caucho.v5.deploy.DeployMode;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.FilterMapper;
import com.caucho.v5.http.dispatch.InvocationDecoder;
import com.caucho.v5.http.dispatch.InvocationRouter;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.dispatch.UrlMap;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.log.AccessLogBase;
import com.caucho.v5.http.log.AccessLogServlet;
import com.caucho.v5.http.pod.PodManagerApp;
import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.RequestServletStubSession;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.http.security.Authenticator;
import com.caucho.v5.http.security.AuthenticatorRole;
import com.caucho.v5.http.security.BasicLogin;
import com.caucho.v5.http.security.ConstraintManager;
import com.caucho.v5.http.security.Login;
import com.caucho.v5.http.security.LoginConfig;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.i18n.CharacterEncoding;
import com.caucho.v5.io.AlwaysModified;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.javac.WorkDir;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.DependencyContainer;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.management.server.HostMXBean;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.network.port.ConnectionProtocol;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.util.BasicFuture;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.vfs.Encoding;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.websocket.server.ServerContainerImpl;

import io.baratine.inject.InjectManager;

/**
 * Resin's webApp implementation.
 */
@Configurable
@SuppressWarnings("serial")
@CauchoBean
public class WebApp extends ServletContextImpl
  implements Dependency, EnvironmentBean, 
             InvocationRouter<InvocationServlet>,
             DeployInstanceEnvironment,
             PodManagerApp,
             java.io.Serializable
{
  private static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebApp.class.getName());
  
  public static final String IMMEDIATE = "caucho.shutdown.immediate";

  private static EnvironmentLocal<AccessLogBase> _accessLogLocal
    = new EnvironmentLocal<AccessLogBase>("caucho.server.access-log");

  private static EnvironmentLocal<WebApp> _webAppLocal
    = new EnvironmentLocal<WebApp>("caucho.application");

  private static String []_classLoaderHackPackages;

  // The environment class loader
  private EnvironmentClassLoader _classLoader;

  private HttpContainerServlet _httpContainer;
  private Host _host;
  // The parent
  private WebAppContainer _container;

  // The webApp entry
  private WebAppController _controller;

  // The inject container
  private InjectManager _injectManager;

  private InvocationDecoder _invocationDecoder;

  private String _moduleName;
  
  // The context path
  private String _baseContextPath = "";
  private String _versionContextPath = "";

  private boolean _isDisableCrossContext;

  // true for jsp compilation from a command line
  private boolean _isCompileContext;

  // The session manager
  private SessionManager _sessionManager;
  // True if the session manager is inherited
  private boolean _isInheritSession;

  private String _characterEncoding;
  private int _formParameterMax = 10000;

  private UrlMap<CacheMapping> _cacheMappingMap = new UrlMap<>();

  private Login _defaultLogin;
  private Login _login;
  private AuthenticatorRole _authenticator;

  // private RoleMapManager _roleMapManager;

  // True for SSL secure.
  // private boolean _isSecure;

  // Error pages.
  private ErrorPageManager _errorPageManager;

  // Any configuration exception
  private Throwable _configException;
  
  // private WelcomeFile _welcomeFile;

  private LruCache<String,String> _realPathCache
    = new LruCache<>(1024);
  // real-path mapping
  //private RewriteRealPath _rewriteRealPath;

  // mime mapping
  private HashMap<String,String> _mimeMapping = new HashMap<>();
  // locale mapping
  private HashMap<String,String> _localeMapping
    = new HashMap<>();
    
  private WebAppListeners _listenerManager;

  private ArrayList<Validator> _resourceValidators
    = new ArrayList<>();

  private DependencyContainer _invocationDependency;

  private AccessLogBase _accessLog;
  private PathImpl _tempDir;

  private HashMap<String,Object> _extensions = new HashMap<String,Object>();

  private MultipartForm _multipartForm;

  private ArrayList<String> _regexp;

  private long _shutdownWaitTime = 15000L;
  private long _activeWaitTime = 60000L;

  private long _idleTime = -1; // 2 * 3600 * 1000L;
  
  private boolean _isStartDisabled;
  private boolean _isEnabled = true;
  boolean _isContextConfigUnsuppored = false;

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
  // private ServiceManagerAmp _ampManager;
  private int _podSize;
  private WebAppBuilder _builder;
  private WebAppDispatch _webAppDispatcher;

  private int _effectiveMajorVersion = 3;
  private int _effectiveMinorVersion = 1;
  private ServiceManagerAmp _ampManager;
  private boolean _isContainerInit;

  /**
   * Creates the webApp with its environment loader.
   */
  WebApp(WebAppBuilder builder)
  {
    Objects.requireNonNull(builder);
    
    _builder = builder;
    
    _controller = builder.getController();
    
    _classLoader = builder.getClassLoader();
    
    _webAppLocal.set(this, _classLoader);
    
    _httpContainer = builder.getHttpContainer();
    
    _host = _builder.getHost();
    
    _webAppDispatcher = createWebAppDispatch(builder);
    
    _invocationDecoder = _httpContainer.getInvocationDecoder();

    setVersionContextPath(_controller.getContextPath());
    _baseContextPath = _controller.getContextPath();
    
    _moduleName = _baseContextPath;
    
    if ("".equals(_moduleName)) {
      _moduleName = "ROOT";
    }
    else if (_moduleName.startsWith("/")) {
      _moduleName = _moduleName.substring(1);
    }

    setParent(_controller.getContainer());
    
    if (getId().startsWith("error/")) {
      _lifecycle = new Lifecycle(log, toString(), Level.FINER);
    }
    else {
      _lifecycle = new Lifecycle(log, toString(), Level.INFO);
    }
  }

  private void initVersion(WebAppBuilder builder) {
    String version = builder.getVersion();
    if (version == null)
      return;

    _effectiveMajorVersion = version.charAt(0) - '0';
    _effectiveMinorVersion = version.charAt(2) - '0';
  }

  protected void initConstructor()
  {
    try {
      _classLoader.addParentPriorityPackages(_classLoaderHackPackages);

      PathImpl rootDirectory = getRootDirectory();

      Vfs.setPwd(rootDirectory, _classLoader);
      WorkDir.setLocalWorkDir(rootDirectory.lookup("WEB-INF/work"),
                              _classLoader);
      
      _listenerManager = getBuilder().getListenerManager();

      getBuilder().getScanner().addScanListeners();
      
      getBuilder().getScanner().loadInitializers();

      _invocationDependency = new DependencyContainer();
      _invocationDependency.add(this);
      
      _invocationDependency.add(_controller);

      _injectManager = InjectManager.create(_classLoader);
      
      // validation
      if (CauchoUtil.isTesting()) {
      }
      /*
      else if (rootDirectory.equals(CauchoUtil.getResinHome())) {
        throw new ConfigException(L.l("web-app root-directory '{0}' can not be the same as resin.home\n",
                                      rootDirectory.getURL()));
      }
      */
      else if (_container != null
               && rootDirectory.equals(_container.getRootDirectory())) {
        throw new ConfigException(L.l("web-app root-directory '{0}' can not be the same as the host root-directory\n",
                                      rootDirectory.getURL()));
      }
    } catch (Throwable e) {
      setConfigException(e);
    }
  }

  protected WebAppDispatch createWebAppDispatch(WebAppBuilder builder)
  {
    return new WebAppDispatch(builder);
  }

  public WebAppDispatch getDispatcher()
  {
    return _webAppDispatcher;
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

    //_ampManager = Amp.newManagerBuilder().name(getId()).autoStart(false).build();
    //Amp.setContextManager(_ampManager);
    
    // _rampManager.start();
  }

  /**
   * Sets the parent container.
   */
  public void setParent(WebAppContainer parent)
  {
    _container = parent;

    if (parent == null)
      return;
  }

  /**
   * Gets the parent container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
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
    return _webAppLocal.get();
  }

  /**
   * Gets the dispatch server.
   */
  public HttpContainerServlet getServer()
  {
    return _httpContainer;
  }
  
  public WebAppController getController()
  {
    return _controller;
  }
  
  public String getModuleName()
  {
    return _moduleName;
  }

  public InvocationDecoder getInvocationDecoder()
  {
    if (_invocationDecoder != null)
      return _invocationDecoder;

    if (_httpContainer != null)
      _invocationDecoder = _httpContainer.getInvocationDecoder();

    if (_invocationDecoder == null && _httpContainer == null)
      _invocationDecoder = HttpContainer.current().getInvocationManager().getInvocationDecoder();

    return _invocationDecoder;
  }
  
  public InjectManager getInjectManager()
  {
    return _injectManager;
  }

  /**
   * Gets the environment class loader.
   */
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getClassLoader() from this context");

    return _classLoader;
  }

  /**
   * Gets the webApp directory.
   */
  public PathImpl getRootDirectory()
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
  @Override
  public String getContextPath()
  {
    if (isVersionAlias())
      return _baseContextPath;
    else
      return _versionContextPath;
  }
  
  /**
   * Returns the context ramp manager.
   */
  /*
  public ServiceManagerAmp getRampManager()
  {
    return _ampManager;
  }
  */

  /**
   * Sets the context path
   */
  private void setVersionContextPath(String contextPath)
  {
    _versionContextPath = contextPath;

    if (getServletContextName() == null)
      setDisplayName(contextPath);
  }

  String getVersionContextPath()
  {
    return _versionContextPath;
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
    return _isEnabled && _httpContainer.isEnabled();
  }

  /**
   * Gets the URL
   */
  public String getURL()
  {
    if (_container != null)
      return _container.getURL() + getContextPath();
    else
      return getContextPath();
  }

  /**
   * Gets the URL
   */
  public String getId()
  {
    return _controller.getId();
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

  public boolean isVersionAlias()
  {
    return _controller.isVersionAlias();
  }

  /**
   * Returns the character encoding.
   */
  public String getCharacterEncoding()
  {
    return _characterEncoding;
  }
  
  //
  // websocket
  //

  public void setCompileContext(boolean isCompile)
  {
    _isCompileContext = isCompile;
  }

  public boolean isCompileContext()
  {
    return _isCompileContext;
  }

  @Override
  public EnumSet<SessionTrackingMode> getDefaultSessionTrackingModes()
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getDefaultSessionTrackingModes() from this context");

    HashSet<SessionTrackingMode> modes = new HashSet<>();
    
    if (_sessionManager.isEnableSessionCookies()) {
      modes.add(SessionTrackingMode.COOKIE);
    }
    
    if (_sessionManager.isEnableSessionUrls()) {
      modes.add(SessionTrackingMode.URL);
    }
    
    return EnumSet.copyOf(modes);
  }

  @Override
  public EnumSet<SessionTrackingMode> getEffectiveSessionTrackingModes()
  {
    if (! isActive()) {
      throw new UnsupportedOperationException(L.l("{0} is not active", this));
    }
    
    HashSet<SessionTrackingMode> modes = new HashSet<>();
    
    if (_sessionManager.isEnableSessionCookies()) {
      modes.add(SessionTrackingMode.COOKIE);
    }
    
    if (_sessionManager.isEnableSessionUrls()) {
      modes.add(SessionTrackingMode.URL);
    }
    
    if (_sessionManager.isEnableSessionSsl()) {
      modes.add(SessionTrackingMode.SSL);
    }
    
    return EnumSet.copyOf(modes);
  }
  
  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> modes)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call setSessionTrackingModes() from this context");

    if (isActive()) {
      throw new IllegalStateException(L.l("Can't call setSessionTrackingModes while {0} is active",
                                          this));
    }
    
    if (modes == null) {
      _sessionManager.setEnableCookies(false);
      _sessionManager.setEnableUrlRewriting(false);
      
      return;
    }

/*
    if (modes.contains(SessionTrackingMode.SSL) && modes.size() != 1) {
      throw new IllegalArgumentException(L.l("SSL tracking mode must be by itself"));
    }
*/

    if (modes.contains(SessionTrackingMode.SSL))
      throw new IllegalArgumentException(L.l("SSL tracking mode is not supported"));

    _sessionManager.setEnableCookies(modes.contains(SessionTrackingMode.COOKIE));
    _sessionManager.setEnableUrlRewriting(modes.contains(SessionTrackingMode.URL));
    _sessionManager.setEnableSsl(modes.contains(SessionTrackingMode.SSL));
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
  public AccessLogServlet createAccessLog()
  {
    return new AccessLogServlet();
  }

  /**
   * Sets the access log.
   */
  @Configurable
  public void setAccessLog(AccessLogBase log)
  {
    add(log);
  }

  /**
   * Allow custom access log
   */
  @Configurable
  public void add(AccessLogBase log)
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
  
  public boolean isSecure()
  {
    return _builder.isSecure();
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
    if (! _listenerManager.isListenerClass(listenerClass)) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown listener class",
                                             listenerClass.getName()));
    }
    
    //try {
      T listener = _injectManager.lookup(listenerClass);
      
      return listener;
      /*
    }
    catch (InjectionException e) {
      throw new ServletException(e);
    }
    */
  }

  public boolean hasListener(Class<?> listenerClass)
  {
    return _listenerManager.hasListener(listenerClass);
  }

  @Override
  public void addListener(String className)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addListener() from this context");

    try {
      Class listenerClass = Class.forName(className, false, getClassLoader());

      addListener(listenerClass);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void addListener(Class<? extends EventListener> listenerClass)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addListener() from this context");

    if (! isContainerInit()
        && ! isActive()
        && ServletContextListener.class.isAssignableFrom(listenerClass))
      throw new IllegalArgumentException(L.l("ServletContextListener listener {0} is not allowed as a dynamic listener.", listenerClass.getName()));

    addListener(_injectManager.lookup(listenerClass));
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

  @Override
  public <T extends EventListener> void addListener(T listener)
  {
    if (isActive()) {
      throw new IllegalStateException(L.l("addListener may not be called after initialization"));
    }

    if (! isContainerInit() && listener instanceof ServletContextListener) {
      throw new IllegalArgumentException(L.l("{0} listener {1} is not allowed as a dynamic listener.",
                                             ServletContextListener.class.getSimpleName(),
                                             listener));
    }

    _listenerManager.addListenerObject(listener, true);
  }

  /**
   * Returns the request listeners.
   */
  public ServletRequestListener []getRequestListeners()
  {
    return _listenerManager.getRequestListeners();
  }

  /**
   * Returns the request attribute listeners.
   */
  public ServletRequestAttributeListener []getRequestAttributeListeners()
  {
    return _listenerManager.getRequestAttributeListeners();
  }

  @Override
  protected WebAppListeners getListenerManager()
  {
    return _listenerManager;
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
  public AccessLogBase getAccessLog()
  {
    return _accessLog;
  }

  /**
   * Sets the temporary directory
   */
  @Configurable
  public void setTempDir(PathImpl path)
  {
    _tempDir = path;
  }

  /**
   * Returns an extension.
   */
  public Object getExtension(String key)
  {
    return _extensions.get(key);
  }

  public Collection<TaglibDescriptor> getTaglibs()
  {
    return _builder.getTaglibs();
  }
  
  public ConstraintManager getConstraintManager()
  {
    return _builder.getConstraintManager();
  }

  /*
  @Override
  public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups()
  {
    // return _builder.getJspPropertyGroups();
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Sets the config exception.
   */
  @Override
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

    ServerBase server = ServerBase.current();
    if (server!= null &&
        server.getShutdownWaitMax() < _shutdownWaitTime) {
      log.warning(L.l("web-app shutdown-wait-max '{0}' is longer than server shutdown-wait-max '{1}'.",
                      _shutdownWaitTime,
                      server.getShutdownWaitMax()));
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
   * Sets the delay time waiting for requests to end.
   */
  @Configurable
  public void setIdleTime(Period idle)
  {
    _idleTime = idle.getPeriod();
  }
  
  public boolean isSendfileEnabled()
  {
    return _httpContainer.isSendfileEnable();
  }
  
  public void addSendfileCount()
  {
    _httpContainer.addSendfileCount();
  }
  
  /**
   * Returns the minimum length for a caching sendfile
   */
  public long getSendfileMinLength()
  {
    return _httpContainer.getSendfileMinLength();
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
   * Returns true if the webApp is starting.
   */
  public boolean isStarting()
  {
    return _lifecycle.isStarting();
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
    ConnectionProtocol serverRequest = ConnectionTcp.getCurrentRequest();

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

      _invocationDependency.setCheckInterval(getClassLoader().getDependencyCheckInterval());

      if (_tempDir == null) {
        _tempDir = (PathImpl) EnvLoader.getLevelAttribute("caucho.temp-dir");
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

      // _securityBuilder = _constraintManager.getFilterBuilder();
      
      /*
      if (_securityBuilder != null) {
        _filterMapper.addTopFilter(_securityBuilder);
      }
      */
      
      setAttribute(ServerContainer.class.getName(), 
                   new ServerContainerImpl());

      getBuilder().deployWebAppGenerators();

      _classLoader.setId("web-app:" + getId());

      _injectManager = InjectManager.current();
      
      // env/0e3a
      // _cdiManager.update();

      initVersion(getBuilder());
      // server/5030
      /*
      _cdiManager.addBeanDiscover(_cdiManager.createManagedBean(WebServiceContextProxy.class));

      CdiContextJampChannel channelContext = new CdiContextJampChannel();
      _cdiManager.addContext(channelContext);
      _cdiManager.addManagedBeanDiscover(_cdiManager.createManagedBean(ChannelContextCdiProducer.class));
      */

      /*
      ServiceManagerAmp ampManager = ServiceManagerAmp.current();
      
      if (ampManager == null) {
        ampManager = ServiceManagerAmp.newManager().name(getId()).build();
        Amp.setContextManager(ampManager);
      }
      
      _ampManager = ampManager;
      */

      initWebFragments();

      if (_httpContainer != null) {
        if (getSessionManager() != null) {
          getSessionManager().init();

          if (_sessionManager.getCookieDomainRegexp() != null) {
            _cookieDomainPattern
              = Pattern.compile(_sessionManager.getCookieDomainRegexp());
          }
          
          _listenerManager.addListenerObject(new WebAppSessionListener(), true);
        }
      }

      // XXX: _roleMapManager = RoleMapManager.create();

      _characterEncoding = CharacterEncoding.getLocalEncoding();
      
      //_webAppDispatcher.init();
      // add(welcomeFile);

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

  public boolean isAllowInitParamOverride()
  {
    String version = _builder.getVersion();
    
    return version == null || ! version.startsWith("3");
  }

  public WebAppAdmin getAdmin()
  {
    return _controller.getAdmin();
  }

  public String getPodName()
  {
    return _controller.getPodName();
  }

  @Override
  public void start()
  {
    if (! _lifecycle.isAfterInit()) {
      throw new IllegalStateException(L.l("webApp must be initialized before starting.  Currently in state {0}.", _lifecycle.getStateName()));
    }
    
    if (! _lifecycle.toStarting()) {
      return;
    }

//    _webAppDispatcher = new WebAppDispatch(_builder);
    
    StartupTask task = new StartupTask();

    startImpl(task);
    /*
    ThreadPool.getCurrent().execute(task);

    task.waitFor(getActiveWaitTime());
    // asdf: wait
     */
  }
  
  protected void startImpl(StartupTask task)
  {
    if (! _lifecycle.isAfterInit()) {
      throw new IllegalStateException(L.l("webApp must be initialized before starting.  Currently in state {0}.", _lifecycle.getStateName()));
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    boolean isOkay = true;
    
    OutboxAmp outbox = OutboxAmp.current();
    //OutboxContext<MessageAmp> context = outbox.getAndSetContext(_ampManager.getSystemInbox());
    OutboxContext<MessageAmp> context = outbox.getAndSetContext(null);

    try {
      thread.setContextClassLoader(_classLoader);

      isOkay = false;
      
      if (_accessLog == null) {
        _accessLog = _accessLogLocal.get();
      }

      long interval = _classLoader.getDependencyCheckInterval();
      _invocationDependency.setCheckInterval(interval);

      if (_container != null) {
        _invocationDependency.add(_container.getDependency());
      }

      // Sets the last modified time so the app won't immediately restart
      _invocationDependency.clearModified();
      _classLoader.clearModified();

      String serverId = null;
      
      HttpContainer server = HttpContainer.current();
      
      if (server != null)
        serverId = server.getServerDisplayName();
      
      if (serverId != null) {
        setAttribute("caucho.server-id", serverId);
      }
      
      if (_isStartDisabled) {
        isOkay = true;
        return;
      }
      
      initPod();

      _classLoader.start();
      
      // baratine/1931
      // _ampManager.start();

      // configuration exceptions discovered by resources like
      // the persistence manager
      if (_configException == null) {
        _configException = EnvLoader.getConfigException();
      }
      
      startAuthenticators();

      try {
        if (getSessionManager() != null) {
          getSessionManager().start();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      startInContext();

      callInitializers();

      //Servlet 3.0
      getBuilder().getScanner().initAnnotated();

      // ioc/0568 - before servlet for JSF
      _listenerManager.start(); // XXX: 
      
      try {
        _webAppDispatcher.init();
      } catch (Exception e) {
        setConfigException(e);

        // XXX: CDI TCK
        throw e;
      }

      _host.setConfigETag(null);
      
      _ampManager = ServiceManagerAmp.current();

      _lifecycle.toActive();
      
      clearCache();

      // cdiFireContextInitialized();
      
      if (! getRootDirectory().canRead()
          && ! getHost().getHostName().equals("admin.resin")) {
        log.warning(this + " cannot read root-directory " + getRootDirectory().getNativePath());
      }

      isOkay = true;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      if (! isOkay)
        _lifecycle.toError();

      thread.setContextClassLoader(oldLoader);
      
      outbox.getAndSetContext(context);
    }
  }
  
  protected void startInContext()
  {
  }

  protected boolean isContainerInit()
  {
    return _isContainerInit;
  }

  protected void callInitializers() throws Exception
  {
    _isContainerInit = true;
    
    /*
    try {
      ServletContainerInitBaratine baratine = new ServletContainerInitBaratine();
    
      baratine.onStartup(new HashSet<>(), this);
    } finally {
      _isContainerInit = false;
    }
    */
  }
  
  protected void initWebFragments()
  {
    if (! getBuilder().isMetadataComplete()) {
      getBuilder().initWebFragments();
    }
  }

  void setContextConfigUnsuppored(boolean value)
  {
    _isContextConfigUnsuppored = value;
  }

  @Override
  public int getEffectiveMajorVersion()
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getEffectiveMajorVersion() from this context");

    return _effectiveMajorVersion;
  }

  @Override
  public int getEffectiveMinorVersion()
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getEffectiveMinorVersion() from this context");

    return _effectiveMinorVersion;
  }

  @Override
  public boolean setInitParameter(String name, String value)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call setInitParameter() from this context");

    return super.setInitParameter(name, value);
  }

  @Override
  public void declareRoles(String... roleNames)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call declareRoles() from this context");

    super.declareRoles(roleNames);
  }

  private void initPod()
  {
    /*
    String podName = _builder.getPodName();

    if (podName != null) {
      BartenderSystem bartender = BartenderSystem.getCurrent();
      
      String id = _builder.getId();
      
      int p = id.lastIndexOf('/');
      
      String fullPodName = id.substring(p + 1);
      
      int podNode = 0; 
      
      p = fullPodName.indexOf('.');
      
      if (p > 0 && fullPodName.startsWith("n")) {
        podNode = Integer.parseInt(fullPodName.substring(1, p));
        
        PodBartender pod = bartender.getPodService().createPod(podName, _podSize);
        
        ShardPod node = pod.getNode(podNode);
      
        bartender.setLocalPod(pod);
        bartender.setLocalShard(node);
      }
    }
    */
  }

  /*
  private void cdiFireContextInitialized()
  {
    _cdiManager.fireEvent(this, WebAppInitializedLiteral.INSTANCE);
  }

  private void cdiFireContextDestroyed()
  {
    try {
      if (_cdiManager != null) {
        _cdiManager.fireEvent(this, WebAppDestroyedLiteral.INSTANCE);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  void cdiFireRequestInitialized(ServletRequest request)
  {
    _cdiManager.fireEvent(request, RequestInitializedLiteral.INSTANCE);
  }

  void cdiFireRequestDestroyed(ServletRequest request)
  {
    _cdiManager.fireEvent(request, RequestDestroyedLiteral.INSTANCE);
  }

  public void cdiFireSessionInitialized(HttpSession session)
  {
    _cdiManager.fireEvent(session, SessionInitializedLiteral.INSTANCE);
  }

  public void cdiFireSessionDestroyed(HttpSession session)
  {
    _cdiManager.fireEvent(session, SessionDestroyedLiteral.INSTANCE);
  }
  */

  private void startAuthenticators()
  {
    try {
      // server/1a36

      Authenticator auth = _injectManager.lookup(Authenticator.class);

      setAttribute("caucho.authenticator", auth);
    } catch (Exception e) {
      log.finest(e.toString());
    }

    try {
      if (_login == null) {
        _login = _injectManager.lookup(Login.class);
      }

      if (_login == null) {
        //Bean<?> loginBean = _cdiManager.createManagedBean(BasicLogin.class);
        
        //_cdiManager.addBeanDiscover(loginBean);
        // server/1aj0
        _defaultLogin = _injectManager.lookup(BasicLogin.class);

        _authenticator = _injectManager.lookup(AuthenticatorRole.class);
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
    if (_lifecycle.isAfterStopping())
      return true;
    else if (DeployMode.MANUAL.equals(_controller.getRedeployMode())) {
      return false;
    }
    else if (_classLoader.isModified())
      return true;
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

    if (_lifecycle.isAfterStopping())
      return true;
    else if (_classLoader.isModifiedNow())
      return true;
    else
      return false;
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
  @Override
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
    if (uri == null) {
      throw new IllegalArgumentException(L.l("getContext URI must not be null."));
    }
    else if (uri.startsWith("/")) {
    }
    else if (uri.equals("")) {
      uri = "/";
    }
    else
      throw new IllegalArgumentException(L.l("getContext URI '{0}' must be absolute.", uri));

    try {
      if (isDisableCrossContext()) {
        return uri.startsWith(getContextPath()) ? this : null;
      }
      else if (_host != null) {
        ServletContext subContext = _host.getWebAppContainer().findSubWebAppByURI(uri);
        
        if (subContext == null)
          return null;
        else if (getContextPath().equals(subContext.getContextPath()))
          return this;
        else
          return subContext;
      }
      else {
        return this;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }
  
  private boolean isDisableCrossContext()
  {
    return _builder.isDisableCrossContext();
  }

  public boolean waitForActive()
  {
    return _lifecycle.waitForActive(_activeWaitTime);
  }
  
  //
  // dispatcher
  //
  
  @Override
  public InvocationServlet routeInvocation(InvocationServlet invocation)
  {
    return _webAppDispatcher.buildInvocation(invocation);
  }

  /**
   * Fills the invocation for subrequests.
   */
  public void buildDispatchInvocation(InvocationServlet invocation,
                                      FilterMapper filterMapper)
    throws ServletException
  {
    _webAppDispatcher.buildDispatchInvocation(invocation, filterMapper);
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  @Override
  public RequestDispatcherImpl getRequestDispatcher(String url)
  {
    return _webAppDispatcher.getRequestDispatcher(url);
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  public RequestDispatcher getLoginDispatcher(String url)
  {
    return _webAppDispatcher.getLoginDispatcher(url);
  }

  /**
   * Returns a dispatcher for the named servlet.
   */
  @Override
  public RequestDispatcher getNamedDispatcher(String servletName)
  {
    return _webAppDispatcher.getNamedDispatcher(servletName);
  }

  /**
   * Clears all caches, including the invocation cache, the filter cache, and the proxy cache.
   */
  public void clearCache()
  {
    _webAppDispatcher.clearCache();

    WebAppController controller = _controller;

    if (controller != null)
      controller.clearCache();
  }
  
  //
  // servlet/filter dynamic registration
  //

  @Override
  public <T extends Servlet> T createServlet(Class<T> servletClass)
    throws ServletException
  {
    return _webAppDispatcher.createServlet(servletClass);
  }

  @Override
  public FilterRegistration getFilterRegistration(String filterName)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getFilterRegistration() from this context");

    return _webAppDispatcher.getFilterRegistration(filterName);
  }

  /**
   * Returns filter registrations
   * @return
   */
  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations()
  {
    return _webAppDispatcher.getFilterRegistrations();
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String servletName,
                                                String className)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addServlet() from this context");

    return _webAppDispatcher.addServlet(servletName, className);
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Class<? extends Servlet> servletClass)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addServlet() from this context");

    return _webAppDispatcher.addServlet(servletName, servletClass);
  }

  @Override
  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Servlet servlet)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addServlet() from this context");

    return _webAppDispatcher.addServlet(servletName, servlet);
  }

  @Override
  public ServletRegistration getServletRegistration(String servletName)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getServletRegistration() from this context");

    return _webAppDispatcher.getServletRegistration(servletName);
  }

  @Override
  public Map<String, ServletRegistration> getServletRegistrations()
  {
    return _webAppDispatcher.getServletRegistrations();
  }

  @Override
  public <T extends Filter> T createFilter(Class<T> filterClass)
    throws ServletException
  {
    return _webAppDispatcher.createFilter(filterClass);
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName,
                                              String className)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addFilter() from this context");

    return _webAppDispatcher.addFilter(filterName, className);
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName,
                                              Class<? extends Filter> filterClass)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addFilter() from this context");

    return _webAppDispatcher.addFilter(filterName, filterClass);
  }

  @Override
  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
  {
    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call addFilter() from this context");

    return _webAppDispatcher.addFilter(filterName, filter);
  }
  
  //
  // real path
  //

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
  private String getRealPathImpl(String uri)
  {
    return _builder.createRewriteRealPath().mapToRealPath(uri);
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
   * Access logging for high-level errors
   */
  public void accessLog(HttpServletRequest req, HttpServletResponse res)
  {
    AccessLogBase accessLog = getAccessLog();

    if (accessLog != null) {
      try {
        accessLog.log((RequestServlet) req);
      } catch (Exception e) {
        log.warning("AccessLog: " + e);
        
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, "AccessLog: " + e.toString(), e);
        }
      }
    }
  }

  /**
   * Error logging
   *
   * @param message message to log
   * @param e stack trace of the error
   */
  @Override
  public void log(String message, Throwable e)
  {
    if (e != null)
      log.log(Level.WARNING, message + " (" + this + ")", e);
    else
      log.info(message + " (" + this + ")");
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
  public AuthenticatorRole getAuthenticator()
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
  public AuthenticatorRole getConfiguredAuthenticator()
  {
    Login login = getConfiguredLogin();

    if (login != null)
      return login.getAuthenticator();

    return _authenticator;
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig()
  {
    /*
    if (! isActive()) {
      throw new UnsupportedOperationException(L.l("{0} is not active", this));
    }
    */

    if (_isContextConfigUnsuppored)
      throw new UnsupportedOperationException("Can't call getSessionCookieConfig() from this context");

    return getSessionManager();
  }
  
  @Override
  public ServiceManagerAmp getAmpManager()
  {
    return _ampManager;
  }

  /*
  public Iterable<ServiceRefAmp> getServiceList()
  {
    ArrayList<ServiceRefAmp> services = new ArrayList<>();
    
    ServiceManagerAmp ampManager = _ampManager;
    
    RegistryAmp registry = ampManager.getRegistry();
    
    for (ServiceRefAmp service : registry.getServices()) {
      if (! service.getAddress().startsWith("public:///")) {
        continue;
      }
      
      services.add(service);
    }
    // ampManager.getL..getS
    
    return services;
  }
  */

  public WebAppBuilder getBuilder()
  {
    return _builder;
  }

  /**
   * Gets the session manager.
   */
  public SessionManager getSessionManager()
  {
    if (_sessionManager == null) {
      if (_lifecycle.isAfterStopping()) {
        throw new IllegalStateException(L.l("Resin is shutting down."));
      }

      if (_isInheritSession && _container != null)
        _sessionManager = _container.getSessionManager();

      if (_sessionManager == null) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();

        try {
          thread.setContextClassLoader(getClassLoader());

          _sessionManager = new SessionManager(this);
        } catch (Throwable e) {
          throw ConfigException.wrap(e);
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
      _errorPageManager = new ErrorPageManager(_httpContainer, this);
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

  public boolean isAllowForwardAfterFlush()
  {
    return _webAppDispatcher.isAllowForwardAfterFlush();
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
    return getDispatcher().getMaxEntrySize();
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
    ConnectionProtocol serverRequest = ConnectionTcp.getCurrentRequest();
    RequestServletStubSession stubRequest;
    
    try {
      stubRequest = new RequestServletStubSession(this, sessionId);
      
      ConnectionTcp.setCurrentRequest(stubRequest);
      
      task.run();
    } finally {
      thread.setContextClassLoader(oldClassLoader);
      ConnectionTcp.setCurrentRequest(serverRequest);
    }
  }

  /**
   * Stops the webApp.
   */
  public void stop(ShutdownModeAmp mode)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStopping()) {
        return;
      }
      
      long beginStop = CurrentTime.getCurrentTime();
      
      if (mode == ShutdownModeAmp.IMMEDIATE) {
        setAttribute(IMMEDIATE, true);
      }

      clearCache();
      
      while (_requestCount.get() > 0
             && CurrentTime.getCurrentTime() < beginStop + _shutdownWaitTime
             && ! CurrentTime.isTest()) {
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

      SessionManager sessionManager = _sessionManager;
      _sessionManager = null;
      
      if (sessionManager != null) {
        sessionManager.close();
      }

      /*
      if (sessionManager != null
          && (! _isInheritSession || _controller.getParent() == null)) {
        sessionManager.close();
      }
      */

      _webAppDispatcher.destroy();

      _listenerManager.destroy();

      /*
      try {
        cdiFireContextDestroyed();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */
      
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
  public void shutdown(ShutdownModeAmp mode)
  {
    try {
      stop(mode);
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

      getBuilder().removeWebAppGenerators();

      AccessLogBase accessLog = _accessLog;
      _accessLog = null;

      if (accessLog != null) {
        try {
          accessLog.flush();
          accessLog.close();
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
  /*
  private Object writeReplace()
  {
    return new SingletonBindingHandle(WebApp.class);
  }
  */

  @Override
  public String toString()
  {
    if (_lifecycle == null)
      return getClass().getSimpleName() + "[" + getId() + "]";
    else if (_lifecycle.isActive())
      return getClass().getSimpleName() + "[" + getId() + "]";
    else
      return getClass().getSimpleName() + "[" + getId() + "," + _lifecycle.getState() + "]";
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
    
      /*
      try {
        SessionContextContainer candiContainer
          = (SessionContextContainer) session.getAttribute("resin.candi.scope");
      
        if (candiContainer != null)
          candiContainer.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
      */
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
      } catch (ExecutionException e) {
        throw ConfigException.wrap(e.getCause());
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
    
    @Override
    public void run()
    {
      try {
        startImpl(this);
        
        _future.complete(Boolean.TRUE);
      } catch (Throwable exn) {
        log.log(Level.FINER, exn.toString(), exn);

        _future.fail(exn);
      }
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
