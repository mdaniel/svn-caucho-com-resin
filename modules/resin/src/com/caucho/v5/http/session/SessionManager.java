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

package com.caucho.v5.http.session;

import io.baratine.db.Cursor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.health.meter.AverageSensor;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.hessian.io.SerializerFactory;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.protocol.RequestCauchoBase;
import com.caucho.v5.http.security.AuthenticatorRole;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.json.io.JsonWriter;
import com.caucho.v5.management.server.SessionManagerMXBean;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.Crc64;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.RandomUtil;
import com.caucho.v5.util.WeakAlarm;
import com.caucho.v5.vfs.TempOutputStream;

/**
 * Manages sessions in a web-webApp.
 */
public final class SessionManager implements SessionCookieConfig, AlarmListener
{
  private static final L10N L = new L10N(SessionManager.class);
  private static final Logger log
    = Logger.getLogger(SessionManager.class.getName());

  private static final int FALSE = 0;
  private static final int COOKIE = 1;
  private static final int TRUE = 2;

  private static final int UNSET = 0;
  private static final int SET_TRUE = 1;
  private static final int SET_FALSE = 2;

  private static final int SAVE_BEFORE_HEADERS = 0x1;
  private static final int SAVE_BEFORE_FLUSH = 0x2;
  private static final int SAVE_AFTER_REQUEST = 0x4;
  private static final int SAVE_ON_SHUTDOWN = 0x8;
  
  private static final int []DECODE;

  private final WebApp _webApp;
  private final SessionManagerAdmin _admin;

  private final HttpContainerServlet _servletContainer;
  private final ServerBartender _selfServer;
  private final int _selfIndex;

  // XXX: private CacheImpl _sessionStore;

  // active sessions
  private LruCache<String,SessionImpl> _sessions;
  // iterator to purge sessions (to reduce gc)
  private Iterator<SessionImpl> _sessionIter;
  // array list for session timeout
  private ArrayList<SessionImpl> _sessionList = new ArrayList<SessionImpl>();
  // generate cookies
  private boolean _enableSessionCookies = true;
  // allow session rewriting
  private boolean _enableSessionUrls = true;

  private boolean _isAppendServerIndex = false;
  private boolean _isTwoDigitSessionIndex = false;

  // invalidate the session after the listeners have been called
  private boolean _isInvalidateAfterListener;

  // maximum number of sessions
  private int _sessionMax = 8192;
  // how long a session will be inactive before it times out
  private long _sessionTimeout = 30 * 60 * 1000;

  private String _cookieName = "JSESSIONID";
  private String _sslCookieName;

  // Rewriting strings.
  private String _sessionSuffix = ";jsessionid=";
  private String _sessionPrefix;

  // default cookie version
  private int _cookieVersion;
  private String _cookieDomain;
  private String _cookieDomainRegexp;
  private boolean _isCookieUseContextPath;
  private String _cookiePath;
  private long _cookieMaxAge;
  private int _isCookieHttpOnly;
  private String _cookieComment;
  private String _cookiePort;
  private int _reuseSessionId = COOKIE;
  private int _cookieLength = 18;
  private AtomicLong _sessionIdSequence = new AtomicLong();
  
  private HashSet<SessionTrackingMode> _trackingModes;
  //Servlet 3.0 plain | ssl session tracking cookies become secure when set to true
  private boolean _isSecure;

  // persistence configuration

  private int _sessionSaveMode = SAVE_AFTER_REQUEST;

  private boolean _isPersistenceEnabled = false;
  private boolean _isSaveTriplicate = true;
  private boolean _isSaveBackup = true;
  private boolean _isDestroyOnLru = true;

  // If true, serialization errors should not be logged
  // XXX: changed for JSF
  private boolean _ignoreSerializationErrors = true;
  private boolean _isHessianSerialization = false;
  private SerializerFactory _hessianFactory;
  private boolean _isSerializeCollectionType = true;

  // List of the HttpSessionListeners from the configuration file
  private ArrayList<HttpSessionListener> _listeners;

  // List of the HttpSessionListeners from the configuration file
  private ArrayList<HttpSessionActivationListener> _activationListeners;

  // List of the HttpSessionAttributeListeners from the configuration file
  private ArrayList<HttpSessionAttributeListener> _attributeListeners;
  
  private ArrayList<HttpSessionIdListener> _sessionIdListeners;

  //
  // Compatibility fields
  //

  // private Store _sessionStore;
  private int _alwaysLoadSession;
  private Boolean _alwaysSaveSession;

  private boolean _isClosed;

  private String _distributionId;

  private Alarm _alarm;

  // statistics
  private volatile long _sessionCreateCount;
  private volatile long _sessionTimeoutCount;
  private volatile long _sessionInvalidateCount;

  private final AverageSensor _sessionSaveSample;
  private final Charset UTF_8 = Charset.forName("UTF-8");
  
  private PodBartender _clusterPod;
  private TableSession _tableSession;
  private long _readTimeout = 250L;
  private boolean _enableSessionSsl;
  private boolean _isSaveWaitForAcknowledge;

  /**
   * Creates and initializes a new session manager
   *
   * @param webApp the web-webApp webApp
   */
  public SessionManager(WebApp webApp)
    throws Exception
  {
    _webApp = webApp;

    _servletContainer = webApp.getServer();

    if (_servletContainer == null) {
      throw new IllegalStateException(L.l("Server is not active in this context {0}",
                                          Thread.currentThread().getContextClassLoader()));
    }
    
    BartenderSystem system = BartenderSystem.getCurrent();
    
    _selfServer = system.getServerSelf();
    String clusterId = _selfServer.getClusterId();
    
    _clusterPod = system.findPod("cluster");
    
    if (_clusterPod == null) {
      throw new IllegalStateException(L.l("'{0}' is an unknown cluster", 
                                          clusterId));
    }
    
    _selfIndex = Math.max(_clusterPod.findServerIndex(_selfServer), 0);

    // copy defaults from store for backward compat
    /* XXX:
    PersistentStoreConfig cfg = _servletContainer.getPersistentStore();
    if (cfg != null) {
      setAlwaysSaveSession(cfg.isAlwaysSave());

      _isSaveBackup = cfg.isSaveBackup();
      _isSaveTriplicate = cfg.isSaveTriplicate();
    }
    */

    _sessionSuffix = _servletContainer.getSessionURLPrefix();
    _sessionPrefix = _servletContainer.getAlternateSessionURLPrefix();

    _cookieName = _servletContainer.getSessionCookie();
    _sslCookieName = _servletContainer.getSSLSessionCookie();
      
    /*
    if (_sslCookieName != null && ! _sslCookieName.equals(_cookieName))
      _isSecure = true;
      */

    String hostName = webApp.getHostName();
    String contextPath = webApp.getContextPath();
    
    long time = CurrentTime.getCurrentTime();
    // normalize for test.
    if (CurrentTime.isTest()) {
      time -= time % 1000;
    }
    
    _sessionIdSequence.set(time << 4);

    if (hostName == null || hostName.equals(""))
      hostName = "default";

    String name = hostName + contextPath;

    if (_distributionId == null)
      _distributionId = name;

    _alarm = new WeakAlarm(this);
    _sessionSaveSample
      = MeterService.createAverageMeter("Caucho|Http|Session Save", "Size");

    _admin = new SessionManagerAdmin(this);
  }

  /**
   * Returns the admin.
   */
  public SessionManagerMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the session prefix, ie.. ";jsessionid=".
   */
  public String getSessionPrefix()
  {
    return _sessionSuffix;
  }

  /**
   * Returns the alternate session prefix, before the URL for wap.
   */
  public String getAlternateSessionPrefix()
  {
    return _sessionPrefix;
  }

  /**
   * Returns the cookie version.
   */
  public int getCookieVersion()
  {
    return _cookieVersion;
  }

  /**
   * Sets the cookie version.
   */
  public void setCookieVersion(int cookieVersion)
  {
    _cookieVersion = cookieVersion;
  }

  /**
   * Sets the cookie ports.
   */
  public void setCookiePort(String port)
  {
    _cookiePort = port;
  }

  /**
   * Sets the cookie ports.
   */
  public void setCookieUseContextPath(boolean isCookieUseContextPath)
  {
    _isCookieUseContextPath = isCookieUseContextPath;
  }

  /**
   * Gets the cookie ports.
   */
  public String getCookiePort()
  {
    return _cookiePort;
  }

  /**
   * Returns the debug log
   */
  public Logger getDebug()
  {
    return log;
  }

  /**
   * Returns the SessionManager's webApp
   */
  WebApp getWebApp()
  {
    return _webApp;
  }
  
  ClassLoader getClassLoader()
  {
    return getWebApp().getClassLoader();
  }

  /**
   * Returns the SessionManager's authenticator
   */
  AuthenticatorRole getAuthenticator()
  {
    return _webApp.getAuthenticator();
  }

  /**
   * Returns the session cache
   */
  /* XXX:
  ByteStreamCache getCache()
  {
    if (_isPersistenceEnabled)
      return _sessionStore;
    else
      return null;
  }
  */

  /**
   * True if sessions should always be loadd.
   */
  boolean isAlwaysLoadSession()
  {
    return _alwaysLoadSession == SET_TRUE;
  }

  /**
   * True if sessions should always be loadd.
   */
  public void setAlwaysLoadSession(boolean load)
  {
    _alwaysLoadSession = load ? SET_TRUE : SET_FALSE;
  }

  /**
   * True if sessions should always be saved.
   */
  boolean getAlwaysSaveSession()
  {
    return ! Boolean.FALSE.equals(_alwaysSaveSession);
  }

  /**
   * True if sessions should always be saved.
   */
  public void setAlwaysSaveSession(boolean save)
  {
    _alwaysSaveSession = save;
  }

  /**
   * True if sessions should be saved on shutdown.
   */
  public boolean isSaveOnShutdown()
  {
    return (_sessionSaveMode & SAVE_ON_SHUTDOWN) != 0;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public boolean isSaveOnlyOnShutdown()
  {
    return (_sessionSaveMode & SAVE_ON_SHUTDOWN) == SAVE_ON_SHUTDOWN;
  }

  /**
   * True if sessions should be saved before the HTTP headers.
   */
  public boolean isSaveBeforeHeaders()
  {
    return (_sessionSaveMode & SAVE_BEFORE_HEADERS) != 0;
  }

  /**
   * True if sessions should be saved before each flush.
   */
  public boolean isSaveBeforeFlush()
  {
    return (_sessionSaveMode & SAVE_BEFORE_FLUSH) != 0;
  }

  /**
   * True if sessions should be saved after the request.
   */
  public boolean isSaveAfterRequest()
  {
    return (_sessionSaveMode & SAVE_AFTER_REQUEST) != 0;
  }
  
  public void setSaveWaitForAcknowledge(boolean isWait)
  {
    _isSaveWaitForAcknowledge = isWait;
  }

  public boolean isSaveWaitForAcknowledge()
  {
    return _isSaveWaitForAcknowledge;
  }

  /**
   * Determines how many digits are used to encode the server
   */
  boolean isTwoDigitSessionIndex()
  {
   return _isTwoDigitSessionIndex;
  }

  /**
   * Sets the save-mode: before-flush, before-headers, after-request,
   * on-shutdown
   */
  public void setSaveMode(String mode)
    throws ConfigException
  {
    /* XXX: probably don't want to implement this.
    if ("before-flush".equals(mode)) {
      _sessionSaveMode = (SAVE_BEFORE_FLUSH|
                          SAVE_BEFORE_HEADERS|
                          SAVE_AFTER_REQUEST|
                          SAVE_ON_SHUTDOWN);
    }
    else
    */

    if ("before-headers".equals(mode)) {
      _sessionSaveMode = (SAVE_BEFORE_HEADERS
                          | SAVE_ON_SHUTDOWN);
    }
    else if ("after-request".equals(mode)) {
      _sessionSaveMode = (SAVE_AFTER_REQUEST
                          | SAVE_ON_SHUTDOWN);
    }
    else if ("on-shutdown".equals(mode)) {
      _sessionSaveMode = (SAVE_ON_SHUTDOWN);
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown session save-mode.  Values are: before-headers, after-request, and on-shutdown.",
                                    mode));

  }

  /**
   * Returns the string value of the save-mode.
   */
  public String getSaveMode()
  {
    if (isSaveBeforeFlush())
      return "before-flush";
    else if (isSaveBeforeHeaders())
      return "before-headers";
    else if (isSaveAfterRequest())
      return "after-request";
    else if (isSaveOnShutdown())
      return "on-shutdown";
    else
      return "unknown";
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnlyOnShutdown(boolean save)
  {
    log.warning("<save-only-on-shutdown> is deprecated.  Use <save-mode>on-shutdown</save-mode> instead");

    if (save)
      _sessionSaveMode = SAVE_ON_SHUTDOWN;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnShutdown(boolean save)
  {
    log.warning("<save-on-shutdown> is deprecated.  Use <save-only-on-shutdown> instead");

    setSaveOnlyOnShutdown(save);
  }

  /**
   * Sets the serialization type.
   */
  public void setSerializationType(String type)
  {
    if ("hessian".equals(type))
      _isHessianSerialization = true;
    else if ("java".equals(type))
      _isHessianSerialization = false;
    else
      throw new ConfigException(L.l("'{0}' is an unknown valud for serialization-type.  The valid types are 'hessian' and 'java'.",
                                    type));
  }
  
  public void setSerializeCollectionType(boolean isEnable)
  {
    _isSerializeCollectionType = isEnable;
  }

  /**
   * Returns true for Hessian serialization.
   */
  public boolean isHessianSerialization()
  {
    return _isHessianSerialization;
  }

  /**
   * True if the session should be invalidated after the listener.
   */
  public void setInvalidateAfterListener(boolean inv)
  {
    _isInvalidateAfterListener = inv;
  }

  /**
   * True if the session should be invalidated after the listener.
   */
  public boolean isInvalidateAfterListener()
  {
    return _isInvalidateAfterListener;
  }

  /**
   * Returns the current number of active sessions.
   */
  public int getActiveSessionCount()
  {
    if (_sessions == null)
      return -1;
    else
      return _sessions.size();
  }

  /**
   * Returns the active sessions.
   */
  public int getSessionActiveCount()
  {
    return getActiveSessionCount();
  }

  /**
   * Returns the created sessions.
   */
  public long getSessionCreateCount()
  {
    return _sessionCreateCount;
  }

  /**
   * Returns the timeout sessions.
   */
  public long getSessionTimeoutCount()
  {
    return _sessionTimeoutCount;
  }

  /**
   * Returns the invalidate sessions.
   */
  public long getSessionInvalidateCount()
  {
    return _sessionInvalidateCount;
  }

  /**
   * Adds a new HttpSessionListener.
   */
  public void addListener(HttpSessionListener listener)
  {
    if (_listeners == null)
      _listeners = new ArrayList<HttpSessionListener>();

    _listeners.add(listener);
  }

  /**
   * Adds a new HttpSessionListener.
   */
  ArrayList<HttpSessionListener> getListeners()
  {
    return _listeners;
  }
  
  public void addSessionIdListener(HttpSessionIdListener listener)
  {
    if (_sessionIdListeners == null) {
      _sessionIdListeners = new ArrayList<>();
    }
    
    _sessionIdListeners.add(listener);
  }

  public ArrayList<HttpSessionIdListener> getSessionIdListeners()
  {
    return _sessionIdListeners;
  }

  /**
   * Adds a new HttpSessionActivationListener.
   */
  public void addActivationListener(HttpSessionActivationListener listener)
  {
    if (_activationListeners == null)
      _activationListeners = new ArrayList<HttpSessionActivationListener>();

    _activationListeners.add(listener);
  }

  /**
   * Returns the activation listeners.
   */
  ArrayList<HttpSessionActivationListener> getActivationListeners()
  {
    return _activationListeners;
  }

  /**
   * Adds a new HttpSessionAttributeListener.
   */
  public void addAttributeListener(HttpSessionAttributeListener listener)
  {
    if (_attributeListeners == null)
      _attributeListeners = new ArrayList<HttpSessionAttributeListener>();

    _attributeListeners.add(listener);
  }

  /**
   * Gets the HttpSessionAttributeListener.
   */
  ArrayList<HttpSessionAttributeListener> getAttributeListeners()
  {
    return _attributeListeners;
  }

  /**
   * True if serialization errors should just fail silently.
   */
  boolean getIgnoreSerializationErrors()
  {
    return _ignoreSerializationErrors;
  }

  /**
   * True if serialization errors should just fail silently.
   */
  public void setIgnoreSerializationErrors(boolean ignore)
  {
    _ignoreSerializationErrors = ignore;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public int getReuseSessionId()
  {
    return _reuseSessionId;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public boolean reuseSessionId(boolean fromCookie)
  {
    int reuseSessionId = _reuseSessionId;

    return reuseSessionId == TRUE || fromCookie && reuseSessionId == COOKIE;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public void setReuseSessionId(String reuse)
    throws ConfigException
  {
    if (reuse == null)
      _reuseSessionId = COOKIE;
    else if (reuse.equalsIgnoreCase("true")
             || reuse.equalsIgnoreCase("yes")
             || reuse.equalsIgnoreCase("cookie"))
      _reuseSessionId = COOKIE;
    else if (reuse.equalsIgnoreCase("false") || reuse.equalsIgnoreCase("no"))
      _reuseSessionId = FALSE;
    else if (reuse.equalsIgnoreCase("all"))
      _reuseSessionId = TRUE;
    else
      throw new ConfigException(L.l("'{0}' is an invalid value for reuse-session-id.  'true' or 'false' are the allowed values.",
                                    reuse));
  }

  /**
   * Returns true if the sessions are closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Sets the cluster store.
   */
  public void setUsePersistentStore(boolean enable)
    throws Exception
  {
    _isPersistenceEnabled = enable;
  }
  
  public boolean isUsePersistentStore()
  {
    return isPersistenceEnabled();
  }

  public boolean isPersistenceEnabled()
  {
    return _isPersistenceEnabled;
  }
  
  public void setDestroyOnLru(boolean isDestroy)
  {
    _isDestroyOnLru = isDestroy;
  }
  
  public boolean isDestroyOnLru()
  {
    return _isDestroyOnLru || ! isPersistenceEnabled();
  }

  public String getDistributionId()
  {
    return _distributionId;
  }

  public void setDistributionId(String distributionId)
  {
    _distributionId = distributionId;
  }

  /**
   * Returns the default session timeout in milliseconds.
   */
  public long getSessionTimeout()
  {
    return _sessionTimeout;
  }

  /**
   * Set the default session timeout in minutes
   */
  public void setSessionTimeout(long timeout)
  {
    if (timeout <= 0 || Integer.MAX_VALUE / 2 < timeout)
      _sessionTimeout = Long.MAX_VALUE / 2;
    else
      _sessionTimeout = 60000L * timeout;
  }

  public long getReadTimeout()
  {
    return _readTimeout ;
  }

  /**
   * Returns the idle time.
   */
  public long getMaxIdleTime()
  {
    return _sessionTimeout;
  }

  /**
   * Returns the maximum number of sessions.
   */
  public int getSessionMax()
  {
    return _sessionMax;
  }

  /**
   * Returns the maximum number of sessions.
   */
  public void setSessionMax(int max)
  {
    if (max < 1)
      throw new ConfigException(L.l("session-max '{0}' is too small.  session-max must be a positive number", max));

    _sessionMax = max;
  }

  /**
   * Returns true if sessions use the cookie header.
   */
  public boolean isEnableSessionCookies()
  {
    return _enableSessionCookies;
  }

  /**
   * Returns true if sessions use the cookie header.
   */
  public void setEnableCookies(boolean enableCookies)
  {
    _enableSessionCookies = enableCookies;
  }

  /**
   * Returns true if sessions use the SSL
   */
  public boolean isEnableSessionSsl()
  {
    return _enableSessionSsl;
  }

  /**
   * Returns true if sessions use SSL
   */
  public void setEnableSsl(boolean enableSsl)
  {
    _enableSessionSsl= enableSsl;
  }

  /**
   * Returns true if sessions can use the session rewriting.
   */
  public boolean isEnableSessionUrls()
  {
    return _enableSessionUrls;
  }

  /**
   * Returns true if sessions can use the session rewriting.
   */
  public void setEnableUrlRewriting(boolean enableUrls)
  {
    _enableSessionUrls = enableUrls;
  }

  //SessionCookieConfig implementation (Servlet 3.0)
  @Override
  public void setName(String name)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    setCookieName(name);
  }

  @Override
  public String getName()
  {
    return getCookieName();
  }

  @Override
  public void setDomain(String domain)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    setCookieDomain(domain);
  }

  @Override
  public String getDomain()
  {
    return getCookieDomain();
  }

  @Override
  public void setPath(String path)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _cookiePath = path;
  }

  @Override
  public String getPath()
  {
    return _cookiePath;
  }

  @Override
  public void setComment(String comment)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _cookieComment = comment;
  }

  @Override
  public String getComment()
  {
    return _cookieComment;
  }

  @Override
  public void setHttpOnly(boolean httpOnly)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    setCookieHttpOnly(httpOnly);
  }

  @Override
  public boolean isHttpOnly()
  {
    return isCookieHttpOnly();
  }

  @Override
  public void setSecure(boolean secure)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException(L.l("SessionCookieConfig must be set during initialization"));

    _isSecure = secure;
  }

  @Override
  public boolean isSecure()
  {
    return _isSecure;
  }

  @Override
  public void setMaxAge(int maxAge)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _cookieMaxAge = maxAge * 1000;
  }

  @Override
  public int getMaxAge()
  {
    return (int) (_cookieMaxAge / 1000);
  }
  
  
  @Configurable
  public SessionCookieConfig createCookieConfig()
  {
    return this;
  }

  public void setCookieName(String cookieName)
  {
    _cookieName = cookieName;
  }
  
  public void setTrackingMode(SessionTrackingMode mode)
  {
    if (_trackingModes == null) {
      _trackingModes = new HashSet<SessionTrackingMode>();
      
      setEnableCookies(false);
      setEnableUrlRewriting(false);
    }

    switch (mode) {
    case COOKIE:
      setEnableCookies(true);
      break;
      
    case URL:
      setEnableUrlRewriting(true);
      break;
    }
  }
  /**
   * Returns the default cookie name.
   */
  public String getCookieName()
  {
    return _cookieName;
  }

  /**
   * Returns the SSL cookie name.
   */
  public String getSSLCookieName()
  {
    if (_sslCookieName != null)
      return _sslCookieName;
    else
      return _cookieName;
  }

  /**
   * Returns the default session cookie domain.
   */
  public String getCookieDomain()
  {
    return _cookieDomain;
  }

  /**
   * Sets the default session cookie domain.
   */
  public void setCookieDomain(String domain)
  {
    _cookieDomain = domain;
  }

  public String getCookieDomainRegexp() {
    return _cookieDomainRegexp;
  }

  public void setCookieDomainRegexp(String regexp)
  {
    _cookieDomainRegexp = regexp;
  }

  /**
   * Sets the default session cookie domain.
   */
  public void setCookiePath(String path)
  {
    _cookiePath = path;
  }

  /**
   * Returns the max-age of the session cookie.
   */
  public long getCookieMaxAge()
  {
    return _cookieMaxAge;
  }

  /**
   * Sets the max-age of the session cookie.
   */
  public void setCookieMaxAge(Period maxAge)
  {
    _cookieMaxAge = maxAge.getPeriod();
  }

  /**
   * Returns the secure of the session cookie.
   */
  public boolean isCookieSecure()
  {
    if (_isSecure)
      return true;
    else
      return ! _cookieName.equals(_sslCookieName);
  }

  /**
   * Sets the secure of the session cookie.
   */
  public void setCookieSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  /**
   * Returns the http-only of the session cookie.
   */
  public boolean isCookieHttpOnly()
  {
    if (_isCookieHttpOnly == SET_TRUE) { 
      return true;
    }
    else if (_isCookieHttpOnly == SET_FALSE) {
      return false;
    }
    else {
      return getWebApp().getBuilder().getCookieHttpOnly();
    }
  }

  /**
   * Sets the http-only of the session cookie.
   */
  public void setCookieHttpOnly(boolean httpOnly)
  {
    _isCookieHttpOnly = httpOnly ? SET_TRUE : SET_FALSE;
  }

  /**
   * Sets the cookie length
   */
  public void setCookieLength(int cookieLength)
  {
    if (cookieLength < 7)
      cookieLength = 7;

    _cookieLength = cookieLength;
  }

  /**
   * Returns the cookie length.
   */
  public long getCookieLength()
  {
    return _cookieLength;
  }

  /**
   * Sets module session id generation.
   */
  public void setCookieModuloCluster(boolean isModulo)
  {
  }

  /**
   * Sets module session id generation.
   */
  public void setCookieAppendServerIndex(boolean isAppend)
  {
    _isAppendServerIndex = isAppend;
  }

  /**
   * Sets module session id generation.
   */
  public boolean isCookieAppendServerIndex()
  {
    return _isAppendServerIndex;
  }

  public void init()
  {
    if (_sessionSaveMode == SAVE_ON_SHUTDOWN
        && (Boolean.TRUE.equals(_alwaysSaveSession)
            || _alwaysLoadSession == SET_TRUE))
      throw new ConfigException(L.l("save-mode='on-shutdown' cannot be used with <always-save-session/> or <always-load-session/>"));
    _sessions = new LruCache<String,SessionImpl>(_sessionMax);
    _sessionIter = _sessions.values();

    if (_cookiePath != null) {
    }
    else if (_isCookieUseContextPath)
      _cookiePath = _webApp.getContextPath();

    if (_cookiePath == null || "".equals(_cookiePath))
      _cookiePath = "/";

    initPersistence();
  }
  
  private void initPersistence()
  {
    _tableSession = new TableSession(this, getWebApp().getId());
  }
  
  TableSession getTable()
  {
    return _tableSession;
  }
  
  /*
  private void initPersistence()
  {
    if (_isPersistenceEnabled) {
      AbstractCache cacheBuilder = new ClusterCache();

      cacheBuilder.setName("resin:session");
      if (_isSaveTriplicate)
        cacheBuilder.setScopeMode(Scope.CLUSTER);
      else if (_isSaveBackup)
        cacheBuilder.setScopeMode(Scope.CLUSTER);
      else
        cacheBuilder.setScopeMode(Scope.LOCAL);
        
      if (isAlwaysLoadSession())
        cacheBuilder.setLocalExpireTimeoutMillis(20);
      else
        cacheBuilder.setLocalExpireTimeoutMillis(1000);
      
      cacheBuilder.setAccessedExpireTimeoutMillis(_sessionTimeout);
      cacheBuilder.setLeaseExpireTimeoutMillis(5 * 60 * 1000);
      // server/0b12
      cacheBuilder.setLocalExpireTimeoutMillis(100);
      
      PersistentStoreConfig persistConfig = PersistentStoreConfig.getCurrent();
      
      if (persistConfig != null) {
        CacheBacking<?,?> backing = persistConfig.getBacking();
        
        if (backing != null) {
          cacheBuilder.setBacking(backing);
          cacheBuilder.setReadThroughExpireTimeoutMillis(500);
        }
      }

      _sessionStore = cacheBuilder.createIfAbsent();
    }
  }
  */

  public void start()
    throws Exception
  {
    _alarm.runAfter(60000);
  }

  /**
   * Returns the session store.
   */
  /*
  public ByteStreamCache getSessionStore()
  {
    return _sessionStore;
  }
  */

  public SerializerSession createSessionSerializer(OutputStream os)
    throws IOException
  {
    if (_isHessianSerialization) {
      if (_hessianFactory == null)
        _hessianFactory = new SerializerFactory(getClassLoader());
      
      SerializerSessionHessian ser;
      
      ser = new SerializerSessionHessian(os, _hessianFactory);
      
      ser.setSerializeCollectionType(_isSerializeCollectionType);
      
      return ser;
    }
    else
      return new SerializerSessionJava(os, getClassLoader());
  }

  public DeserializerSession createSessionDeserializer(InputStream is)
    throws IOException
  {
    if (_isHessianSerialization)
      return new DeserializerSessionHessian(is, getClassLoader());
    else
      return new DeserializerSessionJava(is, getClassLoader());
  }

  /**
   * Returns true if the session exists in this manager.
   */
  public boolean containsSession(String id)
  {
    return _sessions.get(id) != null;
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different webApps on the
   * same matchine should use the same cookie.
   *
   * @param request current request
   */
  public String createSessionId(HttpServletRequest request)
  {
    return createSessionId(request, false);
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different webApps on the
   * same machine should use the same cookie.
   *
   * @param request current request
   */
  public String createSessionId(HttpServletRequest request,
                                boolean create)
  {
    String id;

    do {
      id = createSessionIdImpl(request);
    } while (create && getSession(id, 0, create, true) != null);

    if (id == null || id.equals(""))
      throw new RuntimeException();

    return id;
  }

  public String createCookieValue()
  {
    return createCookieValue(null);
  }

  public String createSessionIdImpl(HttpServletRequest request)
  {
    // look at caucho.session-server-id for a hint of the owner
    Object owner = request.getAttribute("caucho.session-server-id");

    return createCookieValue(owner);
  }

  public boolean isOwner(String id)
  {
    // System.out.println("XXX: owner");
    
    // return id.startsWith(_selfServer.getClusterId());
    return false;
  }

  protected String createCookieValue(Object owner)
  {
    StringBuilder sb = new StringBuilder();
    // this section is the host specific session index
    // the most random bit is the high bit
    int index = _selfIndex;

    // ServerBartender server = _selfServer;

    /*
    if (owner == null) {
    }
    else if (owner instanceof Number) {
      index = ((Number) owner).intValue();

      int regionIndex = _selfServer.getRegionIndex();
      int podIndex = _selfServer.getPodIndex();

      server = _selfServer.getCluster().getServer(regionIndex, podIndex, index);

      if (server == null) {
        server = _selfServer;
      }
    }
    else if (owner instanceof String) {
      server = _selfServer.getRoot().getServer((String) owner);

      if (server == null) {
        server = _selfServer;
      }
    }
    */

    // index = server.getServerIndex();
    
    // ClusterServer clusterServer = server.getData(ClusterServer.class);

    // clusterServer.generateIdPrefix(sb);
    /*
    ServerNetwork.generateSessionAddress(sb, 
                                         server.getServerIndex(),
                                         server.getPodIndex());
                                         */
    // XXX: _cluster.generateBackup(sb, index);

    int length = _cookieLength;

    // length -= sb.length();

    long random = RandomUtil.getRandomLong();
    
    // first three chars encode the node in the cluster pod, which will map
    // back to the current server.
    int node = (int) (random & ((1 << 18) - 1));
    
    int nodeLength = _clusterPod.getNodeCount();
    nodeLength = 64; // XXX: forcing for old model
    node = (node - node % nodeLength) + index;
    sb.append(convert(node));
    sb.append(convert(node >> 6));
    sb.append(convert(node >> 12));
    
    length -= 3;
    
    random = random >> 18;

    for (int i = 18; i < 64 && length-- > 0; i += 6) {
      sb.append(convert(random));
      random = random >> 6;
    }

    if (length > 0) {
      long seq = _sessionIdSequence.incrementAndGet();

      /*
      // The QA needs to add a millisecond for each server start so the
      // clustering test will work, but all the session ids are generated
      // based on the timestamp.  So QA sessions don't have milliseconds
      if (CurrentTime.isTest())
        time -= time % 1000;
        */

      for (int i = 0; i < 7 && length-- > 0; i++) {
        sb.append(convert(seq));
        seq = seq >> 6;
      }
    }

    while (length > 0) {
      random = RandomUtil.getRandomLong();
      for (int i = 0; i < 64 && length-- > 0; i += 6) {
        sb.append(convert(random));
        random = random >> 6;
      }
    }

    if (_isAppendServerIndex) {
      sb.append('.');
      sb.append((index + 1));
    }

    return sb.toString();
  }

  public String changeSessionId(HttpServletRequest request,
                                SessionImpl session)
  {
    String sessionId = createSessionId(request, true);
    
    if (session != null) {
      String oldId = session.getId();
      
      session.changeSessionId(sessionId);
      
      _sessions.remove(oldId);
      _sessions.put(sessionId, session);

      session.finishChangeSessionId();
    }
    
    return sessionId;
  }

  /**
   * Finds a session in the session store, creating one if 'create' is true
   *
   * @param isCreate if the session doesn't exist, create it
   * @param request current request
   * @sessionId a desired sessionId or null
   * @param now the time in milliseconds
   * @param fromCookie true if the session id comes from a cookie
   *
   * @return the cached session.
   */
  public SessionImpl createSession(boolean isCreate,
                                   RequestCauchoBase request,
                                   String sessionId,
                                   long now,
                                   boolean fromCookie)
  {
    if (_sessions == null) {
      return null;
    }

    SessionImpl session = _sessions.get(sessionId);
    
    if (session != null && ! session.isValid()) {
      session = null;
    }

    boolean isNew = false;

    if (session == null
        && sessionId != null
        && isPersistenceEnabled()) {
      Cursor cursor = _tableSession.load(sessionId);

      if (cursor != null) {
        session = create(sessionId, now, isCreate);
        
        session.load(cursor);

        isNew = true;
      }
    }

    if (session != null) {
      if (session.isTimeout(now)) {
        session.invalidateTimeout();
        session = null;
      }
      else if (isPersistenceEnabled() && ! session.load(isNew)) {
        // if the load failed, then the session died out from underneath
        if (! isNew) {
          if (log.isLoggable(Level.FINER))
            log.fine("session load failed for " + session);

          // server/0174
          session.reset(0);
          /*
          session.setModified();

          // Return the existing session for timing reasons, e.g.
          // if a second request hits before the first has finished saving

          return session;
          */
        }
      }
      else {
        session.addUse();

        if (isCreate) {
          // TCK only set access on create
          session.setAccess(now);
        }

        return session;
      }
    }

    if (! isCreate) {
      return null;
    }

    if (sessionId == null
        || sessionId.length() <= 6
        || ! reuseSessionId(fromCookie)) {
      sessionId = createSessionId(request, true);
    }

    session = new SessionImpl(this, sessionId, now);

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(sessionId, session);

    if (! sessionId.equals(session.getId()))
      throw new IllegalStateException(sessionId + " != " + session.getId());

    if (! session.addUse())
      throw new IllegalStateException(L.l("Can't use session for unknown reason"));

    _sessionCreateCount++;

    session.create(now, true);
    
    request.setSession(session);

    handleCreateListeners(session);

    return session;
  }

  /**
   * Returns a session from the session store, returning null if there's
   * no cached session.
   *
   * @param key the session id
   * @param now the time in milliseconds
   *
   * @return the cached session.
   */
  public SessionImpl getSession(String key, long now,
                                boolean create, boolean fromCookie)
  {
    SessionImpl session;
    boolean isNew = false;
    boolean killSession = false;

    if (_sessions == null) {
      return null;
    }

    session = _sessions.get(key);
    
    if (session != null && ! session.getId().equals(key)) {
      throw new IllegalStateException(key + " != " + session.getId());
    }

    if (now <= 0) { // just generating id
      return session;
    }
    
    if (session != null) {
      if (! session.addUse()) {
        session = null;
      }
    }

    /* XXX:
    if (session == null && _sessionStore != null) {
      //if (! _objectManager.isInSessionGroup(key))
      //  return null;

      session = create(key, now, create);

      if (! session.addUse())
        session = null;

      isNew = true;
    }
    */

    if (session == null)
      return null;

    if (isNew) {
      killSession = ! load(session, now, create);
      isNew = killSession;
    }
    else if (! session.load(isNew)) {
      // if the load failed, then the session died out from underneath
      if (log.isLoggable(Level.FINER))
        log.fine(session + " load failed for existing session");

      session.setModified();

      isNew = true;
    }

    if (killSession && (! create || ! reuseSessionId(fromCookie))) {
      // XXX: session.setClosed();
      session.endUse();
      _sessions.remove(key);
      // XXX:
      // session._isValid = false;

      return null;
    }
    else if (isNew)
      handleCreateListeners(session);
    //else
      //session.setAccess(now);

    return session;
  }

  public SessionImpl getSession(String key)
  {
    if (_sessions == null)
      return null;

    return _sessions.get(key);
  }

  /**
   * Create a new session.
   *
   * @param oldId the id passed to the request.  Reuse if possible.
   * @param request - current HttpServletRequest
   * @param fromCookie
   */
  public SessionImpl createSession(String oldId, long now,
                                   HttpServletRequest request,
                                   boolean fromCookie)
  {
    if (_sessions == null) {
      log.fine(this + " createSession called when sessionManager closed");

      return null;
    }

    String id = oldId;

    if (id == null
        || id.length() < 4
        || ! reuseSessionId(fromCookie)) {
      // server/0175
      // || ! _objectManager.isInSessionGroup(id)

      id = createSessionId(request, true);
    }

    SessionImpl session = create(id, now, true);

    if (session == null)
      return null;

    session.addUse();

    _sessionCreateCount++;

    synchronized (session) {
      if (_isPersistenceEnabled && id.equals(oldId))
        load(session, now, true);
      else
        session.create(now, true);
    }

    // after load so a reset doesn't clear any setting
    handleCreateListeners(session);

    return session;
  }

  /**
   * Creates a session.  It's already been established that the
   * key does not currently have a session.
   */
  private SessionImpl create(String key, 
                             long now, 
                             boolean isCreate)
  {
    SessionImpl session = new SessionImpl(this, key, now);

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(key, session);

    if (! key.equals(session.getId()))
      throw new IllegalStateException(key + " != " + session.getId());

    return session;
  }

  /**
   * Notification from the cluster.
   */
  public void notifyRemove(String id)
  {
    SessionImpl session = _sessions.get(id);

    if (session != null)
      session.invalidateRemote();
  }

  private void handleCreateListeners(SessionImpl session)
  {
    if (_listeners != null) {
      HttpSessionEvent event = new HttpSessionEvent(session);

      for (int i = 0; i < _listeners.size(); i++) {
        HttpSessionListener listener = _listeners.get(i);

        listener.sessionCreated(event);
      }
    }

    // XXX: _webApp.cdiFireSessionInitialized(session);
  }

  /**
   * Loads the session from the backing store.  The caller must synchronize
   * the session.
   *
   * @param session the session to load.
   * @param now current time in milliseconds.
   */
  private boolean load(SessionImpl session, long now, boolean isCreate)
  {
    try {
      // XXX: session.setNeedsLoad(false);

      /*
      if (session.getUseCount() > 1) {
        // if used by more than just us,
        return true;
      }
      else*/

      if (now <= 0) {
        return false;
      }
      else if (session.load(true)) { // load for a newly created session
        session.setAccess(now);
        return true;
      }
      else {
        session.create(now, isCreate);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      session.reset(now);
    }

    return false;
  }

  /**
   * Adds a session from the cache.
   */
  void addSession(SessionImpl session)
  {
    _sessions.put(session.getId(), session);
  }

  /**
   * Removes a session from the cache.
   */
  void removeSession(SessionImpl session)
  {
    _sessions.remove(session.getId());
  }

  /**
   * Adds a new session save event
   */
  void addSessionSaveSample(long size)
  {
    _sessionSaveSample.add(size);
  }

  /**
   * Returns a debug string for the session
   */
  public String getSessionSerializationDebug(String id)
  {
    return null;
    /*
    ByteStreamCache cache = getCache();

    if (cache == null)
      return null;

    try {
      TempOutputStream os = new TempOutputStream();

      if (cache.get(id, os)) {
        InputStream is = os.getInputStream();

        StringWriter writer = new StringWriter();

        HessianDebugInputStream dis
          = new HessianDebugInputStream(is, new PrintWriter(writer));

        while (dis.read() >= 0) {
        }

        return writer.toString();
      }

      os.close();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }

    return null;
    */
  }

  public String getSessionAsJsonString(String id) {
    SessionImpl session = getSession(id);

    if (session == null)
      return null;

    TempOutputStream buffer = new TempOutputStream();

    PrintWriter out = new PrintWriter(new OutputStreamWriter(buffer, UTF_8));

    JsonWriter jsonOutput = new JsonWriter(out);

    try {
      jsonOutput.writeObject(session);

      jsonOutput.flush();

      jsonOutput.close();

      out.flush();

      return new String(buffer.toByteArray(), UTF_8);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("can't serialize session {0} due to {1}", session, e), e);
    }

    return null;
  }

  public String []sessionIdList()
  {
    ArrayList<String> sessionIds = new ArrayList<String>();
    
    synchronized (_sessions) {
      Iterator<LruCache.Entry<String, SessionImpl>> sessionsIterator
        = _sessions.iterator();

      while (sessionsIterator.hasNext()) {
        sessionIds.add(sessionsIterator.next().getKey());
      }
    }

    String []ids= new String[sessionIds.size()];

    sessionIds.toArray(ids);
    
    return ids;
  }
  
  public String getSessionsAsJsonString() {

    List<SessionImpl> sessionList;
    synchronized (_sessions) {

      sessionList = new ArrayList<SessionImpl>(_sessions.size());

      Iterator<LruCache.Entry<String, SessionImpl>> sessionsIterator
        = _sessions.iterator();

      while (sessionsIterator.hasNext()) {
        sessionList.add(sessionsIterator.next().getValue());
      }
    }

    SessionImpl []sessions = new SessionImpl[sessionList.size()];

    sessionList.toArray(sessions);

    TempOutputStream buffer = new TempOutputStream();

    PrintWriter out = new PrintWriter(new OutputStreamWriter(buffer, UTF_8));

    JsonWriter jsonOutput = new JsonWriter(out);

    try {
      jsonOutput.writeObject(sessions);

      jsonOutput.flush();

      jsonOutput.close();

      out.flush();

      return new String(buffer.toByteArray(), UTF_8);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("can't serialize sessions due to {0}", e), e);
    }

    return null;
  }

  public long getEstimatedMemorySize()
  {
    Iterator<SessionImpl> sessions = _sessions.values();
    long l = 0;

    while (sessions.hasNext()) {
      l += sessions.next().getLastSaveLength();
    }

    return l;
  }

  /**
   * Timeout for reaping old sessions
   *
   * @return number of live sessions for stats
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      _sessionList.clear();

      int liveSessions = 0;

      if (_isClosed)
        return;

      long now = CurrentTime.getCurrentTime();

      synchronized (_sessions) {
        _sessionIter = _sessions.values(_sessionIter);
        while (_sessionIter.hasNext()) {
          SessionImpl session = _sessionIter.next();

          if (session.isTimeout(now))
            _sessionList.add(session);
          else
            liveSessions++;
        }
      }

      _sessionTimeoutCount += _sessionList.size();

      for (int i = 0; i < _sessionList.size(); i++) {
        SessionImpl session = _sessionList.get(i);

        try {
          _sessions.remove(session.getId());
          
          session.timeout();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    } finally {
      if (! _isClosed)
        _alarm.runAfter(60000);
    }
  }

  /**
   * Cleans up the sessions when the WebApp shuts down gracefully.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
        return;
      _isClosed = true;
    }

    if (_sessions == null)
      return;

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    ArrayList<SessionImpl> list = new ArrayList<SessionImpl>();

    // XXX: messy way of dealing with saveOnlyOnShutdown
    synchronized (_sessions) {
      _sessionIter = _sessions.values(_sessionIter);
      while (_sessionIter.hasNext()) {
        SessionImpl session = _sessionIter.next();

        if (session.isValid())
          list.add(session);
      }

      // XXX: if cleared here, will remove the session
      // _sessions.clear();
    }

    boolean isError = false;
    for (int i = list.size() - 1; i >= 0; i--) {
      SessionImpl session = list.get(i);
      
      if (! session.isValid())
        continue;

      if (log.isLoggable(Level.FINE))
        log.fine("close session " + session.getId());

      try {
        session.saveOnShutdown();
      } catch (Exception e) {
        if (! isError)
          log.log(Level.WARNING, "Can't store session: " + e, e);
        
        isError = true;
      }

      try {
        if (session.isValid())
          _sessions.remove(session.getId());
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (_admin != null)
      _admin.unregister();

    _sessionList = new ArrayList<SessionImpl>();
  }

  /**
   * Converts an integer to a printable character
   */
  private static char convert(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }
  
  public static int getServerCode(String id, int count)
  {
    if (id == null)
      return -1;
    
    if (count == 0) {
      return decode(id.charAt(0));
    }
    
    long hash = Crc64.generate(id);
    
    for (int i = 0; i < count; i++) {
      hash >>= 6;
    }
    
    return (int) (hash & 0x3f);
  }

  static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _webApp.getContextPath() + "]";
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
