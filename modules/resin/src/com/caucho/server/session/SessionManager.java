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

package com.caucho.server.session;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.JndiBuilder;
import com.caucho.config.types.Period;
import com.caucho.management.server.SessionManagerMXBean;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterObject;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.ObjectManager;
import com.caucho.server.cluster.Store;
import com.caucho.server.cluster.StoreManager;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.security.ServletAuthenticator;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

// import com.caucho.server.http.ServletServer;
// import com.caucho.server.http.VirtualHost;

/**
 * Manages sessions in a web-app.
 */
public final class SessionManager implements ObjectManager, AlarmListener
{
  static protected final L10N L = new L10N(SessionManager.class);
  static protected final Logger log
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
  
  private static final int DECODE[];
  
  private WebApp _webApp;
  private final SessionManagerAdmin _admin;

  // factory for creating sessions
  // private SessionFactory _sessionFactory;

  // active sessions
  private LruCache<String,SessionImpl> _sessions;
  // total sessions
  private int _totalSessions;
  
  // iterator to purge sessions (to reduce gc)
  private Iterator<SessionImpl> _sessionIter;
  // array list for session timeout
  private ArrayList<SessionImpl> _sessionList = new ArrayList<SessionImpl>();
  // generate cookies
  private boolean _enableSessionCookies = true;
  // allow session rewriting
  private boolean _enableSessionUrls = true;

  private boolean _isModuloSessionId = false;
  private boolean _isAppendServerIndex = false;
  private boolean _isTwoDigitSessionIndex = false;
  
  // invalidate the session after the listeners have been called
  private boolean _isInvalidateAfterListener;

  // maximum number of sessions
  private int _sessionMax = 4096;
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
  private long _cookieMaxAge;
  private boolean _cookieSecure;
  private int _isCookieHttpOnly;
  private String _cookiePort;
  private int _reuseSessionId = COOKIE;
  private int _cookieLength = 18;

  private int _sessionSaveMode = SAVE_AFTER_REQUEST;

  //private SessionStore sessionStore;
  private StoreManager _storeManager;

  // If true, serialization errors should not be logged
  private boolean _ignoreSerializationErrors = false;

  // List of the HttpSessionListeners from the configuration file
  private ArrayList<HttpSessionListener> _listeners;
  
  // List of the HttpSessionListeners from the configuration file
  private ArrayList<HttpSessionActivationListener> _activationListeners;
  
  // List of the HttpSessionAttributeListeners from the configuration file
  private ArrayList<HttpSessionAttributeListener> _attributeListeners;

  //
  // Compatibility fields
  //
  
  private boolean _isWebAppStore; // i.e. for old-style compatibility
  private Store _sessionStore;
  private int _alwaysLoadSession;
  private int _alwaysSaveSession;

  private boolean _distributedRing;
  private Path _persistentPath;

  private boolean _isClosed;

  private String _distributionId;
  private Cluster _cluster;
  private ClusterServer _selfServer;
  private ClusterServer []_srunGroup = new ClusterServer[0];

  private int _srunIndex;
  private int _srunLength;

  private Alarm _alarm = new Alarm(this);

  // statistics
  private Object _statisticsLock = new Object();
  private long _sessionCreateCount;
  private long _sessionTimeoutCount;
  private long _sessionInvalidateCount;

  /**
   * Creates and initializes a new session manager
   *
   * @param app the web-app webApp
   * @param registry the web-app configuration node
   */
  public SessionManager(WebApp app)
    throws Exception
  {
    _webApp = app;

    DispatchServer server = app.getDispatchServer();
    if (server != null) {
      InvocationDecoder decoder = server.getInvocationDecoder();

      _sessionSuffix = decoder.getSessionURLPrefix();
      _sessionPrefix = decoder.getAlternateSessionURLPrefix();

      _cookieName = decoder.getSessionCookie();
      _sslCookieName = decoder.getSSLSessionCookie();
    }
    
    // this.server = app.getVirtualHost().getServer();
    // this.srunIndex = server.getSrunIndex();

    String hostName = app.getHostName();
    String contextPath = app.getContextPath();
    
    if (hostName == null || hostName.equals(""))
      hostName = "default";

    String name = hostName + contextPath;

    _distributionId = name;

    _persistentPath = Vfs.lookup("WEB-INF/sessions");

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
   * Gets the cluster.
   */
  protected Cluster getCluster()
  {
    synchronized (this) {
      if (_cluster == null) {
	_cluster = Cluster.getLocal();
	ClusterServer selfServer = null;

	if (_cluster != null) {
	  _srunLength = _cluster.getServerList().length;
	  
	  selfServer = _cluster.getSelfServer();
	  _selfServer = selfServer;

	  if (selfServer != null) {
	    _srunGroup = _cluster.getServerList();
	    _srunIndex = selfServer.getIndex();
	  }
	}
      }
    }

    return _cluster;
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

  /**
   * Returns the SessionManager's authenticator
   */
  ServletAuthenticator getAuthenticator()
  {
    return _webApp.getAuthenticator();
  }

  /**
   * Sets the persistent store.
   */
  public void setPersistentStore(JndiBuilder store)
    throws javax.naming.NamingException, ConfigException
  {
    _storeManager = (StoreManager) store.getObject();

    if (_storeManager == null)
      throw new ConfigException(L.l("{0} is an unknown persistent store.",
				    store.getJndiName()));
  }

  /**
   * True if sessions should always be loadd.
   */
  boolean getAlwaysLoadSession()
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
    return _alwaysSaveSession == SET_TRUE;
  }

  /**
   * True if sessions should always be saved.
   */
  public void setAlwaysSaveSession(boolean save)
  {
    _alwaysSaveSession = save ? SET_TRUE : SET_FALSE;
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
      _sessionSaveMode = (SAVE_BEFORE_HEADERS|
			  SAVE_AFTER_REQUEST|
			  SAVE_ON_SHUTDOWN);
    }
    else if ("after-request".equals(mode)) {
      _sessionSaveMode = (SAVE_AFTER_REQUEST|
			  SAVE_ON_SHUTDOWN);
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
    else if (reuse.equalsIgnoreCase("true") ||
	     reuse.equalsIgnoreCase("yes") ||
	     reuse.equalsIgnoreCase("cookie"))
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
   * Returns the owning server.
   */
  ClusterServer getServer(int index)
  {
    Cluster cluster = getCluster();
    
    if (cluster != null)
      return cluster.getServer(index);
    else
      return null;
  }
  
  /**
   * Returns the index of this JVM in the ring.
   */
  public int getSrunIndex()
  {
    return _srunIndex;
  }
  
  /**
   * Returns the number of sruns in the cluster
   */
  public int getSrunLength()
  {
    return _srunLength;
  }

  /**
   * Returns true if the sessions are closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Sets the file store.
   */
  public StoreManager createFileStore()
    throws ConfigException
  {
    Cluster cluster = getCluster();

    if (cluster == null)
      throw new ConfigException(L.l("<file-store> needs a defined <cluster>."));
    
    if (cluster.getStore() != null)
      throw new ConfigException(L.l("<file-store> may not be used with a defined <persistent-store>.  Use <use-persistent-store> instead."));

    StoreManager fileStore = cluster.createPrivateFileStore();
    
    _storeManager = fileStore;

    _isWebAppStore = true;

    return fileStore;
  }

  /**
   * Sets the jdbc store.
   */
  public StoreManager createJdbcStore()
    throws ConfigException
  {
    Cluster cluster = getCluster();

    if (cluster == null)
      throw new ConfigException(L.l("<jdbc-store> needs a defined <cluster>."));
    
    if (cluster.getStore() != null)
      throw new ConfigException(L.l("<jdbc-store> may not be used with a defined <persistent-store>.  Use <use-persistent-store> instead."));
    
    _storeManager = cluster.createJdbcStore();

    _isWebAppStore = true;

    return _storeManager;
  }

  /**
   * Sets the tcp store.
   */
  public void setTcpStore(boolean isEnable)
    throws Exception
  {
    setClusterStore(isEnable);
  }

  /**
   * Sets the cluster store.
   */
  public void setClusterStore(boolean isEnable)
    throws Exception
  {
    if (! isEnable)
      return;
    
    Cluster cluster = getCluster();

    if (cluster == null)
      throw new ConfigException(L.l("<cluster-store> needs a defined <cluster>."));
    
    StoreManager store = cluster.getStore();

    if (store == null)
      throw new ConfigException(L.l("cluster-store in <session-config> requires a configured cluster-store in the <cluster>"));
    
    _storeManager = store;
  }

  /**
   * Sets the cluster store.
   */
  public void setUsePersistentStore(boolean enable)
    throws Exception
  {
    if (! enable)
      return;

    Cluster cluster = getCluster();

    if (cluster == null)
      throw new ConfigException(L.l("<use-persistent-store> needs a defined <cluster>."));
    
    StoreManager store = cluster.getStore();

    if (store == null) {
      try {
	Context ic = new InitialContext();
	store = (StoreManager) ic.lookup("java:comp/env/caucho/persistent-store");
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }

    if (store != null) {
    }
    else if (! Config.evalBoolean("${resin.isProfessional()}")) {
      throw new ConfigException(L.l("use-persistent-store in <session-config> requires Resin professional."));
    }
    else
      throw new ConfigException(L.l("use-persistent-store in <session-config> requires a configured <persistent-store> in the <server>"));
    
    if (_isWebAppStore)
      throw new ConfigException(L.l("use-persistent-store may not be used with <jdbc-store> or <file-store>."));
    
    _storeManager = store;
  }

  /**
   * Returns the session factory.
   */
  public void setPersistentPath(Path path)
  {
    _persistentPath = path;
  }

  public String getDistributionId()
  {
    return _distributionId;
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
    _sessionMax = max;
  }

  /**
   * Returns true if sessions use the cookie header.
   */
  public boolean enableSessionCookies()
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
   * Returns true if sessions can use the session rewriting.
   */
  public boolean enableSessionUrls()
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
  public boolean getCookieSecure()
  {
    if (_cookieSecure)
      return true;
    else
      return ! _cookieName.equals(_sslCookieName);
  }

  /**
   * Sets the secure of the session cookie.
   */
  public void setCookieSecure(boolean secure)
  {
    _cookieSecure = secure;
  }

  /**
   * Returns the http-only of the session cookie.
   */
  public boolean isCookieHttpOnly()
  {
    if (_isCookieHttpOnly == SET_TRUE)
      return true;
    else if (_isCookieHttpOnly == SET_FALSE)
      return true;
    else
      return getWebApp().getCookieHttpOnly();
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
    _isModuloSessionId = isModulo;
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
	&& (_alwaysSaveSession == SET_TRUE
	    || _alwaysLoadSession == SET_TRUE))
      throw new ConfigException(L.l("save-mode='on-shutdown' cannot be used with <always-save-session/> or <always-load-session/>"));
  }

  public void start()
    throws Exception
  {
    _sessions = new LruCache<String,SessionImpl>(_sessionMax);
    _sessionIter = _sessions.values();

    if (_cluster == null)
      getCluster();

    if (_isWebAppStore) {
      // for backward compatibility
      
      if (_alwaysLoadSession == SET_TRUE)
	_storeManager.setAlwaysLoad(true);
      else if (_alwaysLoadSession == SET_FALSE)
	_storeManager.setAlwaysLoad(false);
      
      if (_alwaysSaveSession == SET_TRUE)
	_sessionStore.setAlwaysSave(true);
      else if (_alwaysSaveSession == SET_FALSE)
	_sessionStore.setAlwaysSave(false);

      _storeManager.init();

      _storeManager.updateIdleCheckInterval(_sessionTimeout);
    }

    if (_storeManager != null) {
      _sessionStore = _storeManager.createStore(_distributionId, this);
      _sessionStore.setMaxIdleTime(_sessionTimeout);
      
      if (_alwaysLoadSession == SET_TRUE)
	_sessionStore.setAlwaysLoad(true);
      else if (_alwaysLoadSession == SET_FALSE)
	_sessionStore.setAlwaysLoad(false);
      
      if (_alwaysSaveSession == SET_TRUE)
	_sessionStore.setAlwaysSave(true);
      else if (_alwaysSaveSession == SET_FALSE)
	_sessionStore.setAlwaysSave(false);
    }

    _alarm.queue(60000);
  }

  /**
   * Returns the session store.
   */
  public Store getSessionStore()
  {
    return _sessionStore;
  }

  /**
   * Create a new session.
   *
   * @param oldId the id passed to the request.  Reuse if possible.
   * @param now the current date
   * @param sessionGroup the srun index for this machine
   */
  public SessionImpl createSession(String oldId, long now,
                                   HttpServletRequest request,
				   boolean fromCookie)
  {
    String id = oldId;

    if (id == null || id.length() < 4 ||
	! isInSessionGroup(id) || ! reuseSessionId(fromCookie)) {
      id = createSessionId(request, true);
    }

    SessionImpl session = create(id, now, true);

    if (session == null)
      return null;

    session.addUse();

    synchronized (_statisticsLock) {
      _sessionCreateCount++;
    }

    synchronized (session) {
      if (_sessionStore != null && id.equals(oldId))
        load(session, now);
      else
	session.create(now);
    }

    // after load so a reset doesn't clear any setting
    handleCreateListeners(session);
    
    return session;
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different webApps on the
   * same matchine should use the same cookie.
   *
   * @param sessionGroup possibly assigned by the web server
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
   * @param sessionGroup possibly assigned by the web server
   */
  public String createSessionId(HttpServletRequest request,
                                boolean create)
  {
    String id;

    do {
      id = createSessionIdImpl();
    } while (create && getSession(id, 0, create, true) != null);

    if (id == null || id.equals(""))
      throw new RuntimeException();

    return id;
  }

  public String createSessionIdImpl()
  {
    StringBuffer cb = new StringBuffer();
    // this section is the host specific session index
    // the most random bit is the high bit
    int index = _srunIndex;

    if (index < 0)
      index = 0;

    int length = _cookieLength;

    addBackup(cb, index);

    long random = RandomUtil.getRandomLong();

    for (int i = 0; i < 11 && length-- > 0; i++) {
      cb.append(convert(random));
      random = random >> 6;
    }

    if (length > 0) {
      long time = Alarm.getCurrentTime();
      for (int i = 0; i < 7 && length-- > 0; i++) {
        cb.append(convert(time));
        time = time >> 6;
      }
    }

    while (length > 0) {
      random = RandomUtil.getRandomLong();
      for (int i = 0; i < 11 && length-- > 0; i++) {
        cb.append(convert(random));
        random = random >> 6;
      }
    }

    if (_isAppendServerIndex) {
      cb.append('.');
      cb.append((index + 1));
    }

    return cb.toString();
  }

  /**
   * Adds the primary/backup/third digits to the session id.
   */
  private void addBackup(StringBuffer cb, int index)
  {
    long backupCode;

    if (_selfServer != null)
      backupCode = _selfServer.generateBackupCode();
    else
      backupCode = 0x000200010000L;
    
    addDigit(cb, (int) (backupCode & 0xffff));
    addDigit(cb, (int) ((backupCode >> 16) & 0xffff));
    addDigit(cb, (int) ((backupCode >> 32) & 0xffff));
  }

  private void addDigit(StringBuffer cb, int digit)
  {
    if (_srunLength <= 64 && ! _isTwoDigitSessionIndex)
      cb.append(convert(digit));
    else {
      cb.append(convert(digit / 64));
      cb.append(convert(digit));
    }
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

    if (_sessions == null)
      return null;

    session = _sessions.get(key);

    if (session != null && ! session.getId().equals(key))
      throw new IllegalStateException(key + " != " + session.getId());

    if (now <= 0) // just generating id
      return session;

    if (session != null && ! session.addUse()) {
      session = null;
    }
    
    if (session == null && _sessionStore != null) {
      if (! isInSessionGroup(key))
	return null;

      session = create(key, now, create);

      if (! session.addUse())
	session = null;
      isNew = true;
    }

    if (session == null)
      return null;
    
    if (isNew) {
      killSession = ! load(session, now);
      isNew = killSession;
    }
    else if (! session.load()) {
      // if the load failed, then the session died out from underneath
      session.reset(now);
      isNew = true;
    }

    if (killSession && (! create || ! reuseSessionId(fromCookie))) {
      // XXX: session.setClosed();
      session.endUse();
      session._isValid = false;
      _sessions.remove(key);
        
      return null;
    }
    else if (isNew)
      handleCreateListeners(session);
    else
      session.setAccess(now);
    
    return session;
  }

  public boolean isInSessionGroup(String id)
  {
    if (_srunLength == 0 || _srunGroup.length == 0)
      return true;

    int group = decode(id.charAt(0)) % _srunLength;

    for (int i = _srunGroup.length - 1; i >= 0; i--) {
      ClusterServer server = _srunGroup[i];
      
      if (server != null && group == server.getIndex())
        return true;
    }
    
    return false;
  }

  /**
   * Creates a session.  It's already been established that the
   * key does not currently have a session.
   */
  private SessionImpl create(String key, long now, boolean isCreate)
  {
    SessionImpl session = new SessionImpl(this, key, now);

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(key, session);

    if (! key.equals(session.getId()))
      throw new IllegalStateException(key + " != " + session.getId());

    Store sessionStore = _sessionStore;
    if (sessionStore != null) {
      ClusterObject clusterObject = sessionStore.createClusterObject(key);
      session.setClusterObject(clusterObject);
    }

    return session;
  }

  /**
   * Notification from the cluster.
   */
  public void notifyRemove(String id)
  {
    SessionImpl session = _sessions.remove(id);

    if (session != null)
      session.invalidateImpl(true);
  }

  /**
   * Notification from the cluster.
   */
  public void notifyUpdate(String id)
  {
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

  public static int decode(int code)
  {
    return DECODE[code & 0x7f];
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
  }

  /**
   * Loads the session from the backing store.  The caller must synchronize
   * the session.
   *
   * @param session the session to load.
   * @param now current time in milliseconds.
   */
  private boolean load(SessionImpl session, long now)
  {
    try {
      // XXX: session.setNeedsLoad(false);

      /*
      if (session.getUseCount() > 1) {
	// if used by more than just us, 
	return true;
      }
      else*/ if (now <= 0) {
        return false;
      }
      else if (session.load()) {
        session.setAccess(now);
        return true;
      }
      else {
        session.create(now);
      }
    } catch (Exception e) {
      e.printStackTrace();
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
   * Invalidates a session from the cache.
   */
  public void invalidateSession(SessionImpl session)
  {
    removeSession(session);

    synchronized (_statisticsLock) {
      _sessionInvalidateCount++;
    }

    if (_sessionStore != null) {
      try {
	_sessionStore.remove(session.getId());
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Removes a session from the cache.
   */
  public void removeSession(SessionImpl session)
  {
    _sessions.remove(session.getId());
  }

  /**
   * Loads the session.
   *
   * @param in the input stream containing the serialized session
   * @param obj the session object to be deserialized
   */
  public void load(ObjectInputStream in, Object obj)
    throws IOException
  {
    SessionImpl session = (SessionImpl) obj;

    session.load(in);
  }

  /**
   * Checks if the session is empty.
   */
  public boolean isEmpty(Object obj)
  {
    SessionImpl session = (SessionImpl) obj;

    return session.isEmpty();
  }

  /**
   * Saves the session.
   */
  public void store(ObjectOutputStream out, Object obj)
    throws IOException
  {
    SessionImpl session = (SessionImpl) obj;

    session.store(out);
  }

  /**
   * Timeout for reaping old sessions
   *
   * @return number of live sessions for stats
   */
  public void handleAlarm(Alarm alarm)
  {
    try {
      _sessionList.clear();

      int liveSessions = 0;

      if (_isClosed)
	return;

      long now = Alarm.getCurrentTime();
      long accessWindow = 0;

      if (_sessionStore != null)
	accessWindow = _sessionStore.getAccessWindowTime();
      
      synchronized (_sessions) {
	_sessionIter = _sessions.values(_sessionIter);
	while (_sessionIter.hasNext()) {
	  SessionImpl session = _sessionIter.next();

	  long maxIdleTime = session._maxInactiveInterval + accessWindow;

	  if (session.inUse())
	    liveSessions++;
	  else if (session._accessTime + maxIdleTime < now)
	    _sessionList.add(session);
	  else
	    liveSessions++;
	}
      }

      synchronized (_statisticsLock) {
	_sessionTimeoutCount += _sessionList.size();
      }

      for (int i = 0; i < _sessionList.size(); i++) {
	SessionImpl session = _sessionList.get(i);

	try {
	  long maxIdleTime = session._maxInactiveInterval;

	  if (_storeManager == null) {
	    // if no persistent store then invalidate
	    // XXX: server/12cg - single signon shouldn't logout
	    session.invalidate();
	  }
	  else if (session.getSrunIndex() != _srunIndex && _srunIndex >= 0) {
	    // if not the owner, then just remove
	    _sessions.remove(session.getId());
	  }
	  else {
	    session.invalidate();
	  }
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }
    } finally {
      if (! _isClosed)
	_alarm.queue(60000);
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

    _alarm.dequeue();
    
    _sessionList.clear();

    ArrayList<SessionImpl> list = new ArrayList<SessionImpl>();
    
    boolean isError = false;
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

    for (int i = list.size() - 1; i >= 0; i--) {
      SessionImpl session = list.get(i);
      
      if (log.isLoggable(Level.FINE))
        log.fine("close session " + session.getId());
      
      try {
        if (session.isValid()) {
          synchronized (session) {
	    // server/016i, server/018x
	    if (! session.isEmpty())
	      session.saveOnShutdown();
          }
        }

        _sessions.remove(session.getId());
      } catch (Exception e) {
        if (! isError)
          log.log(Level.WARNING, "Can't store session: " + e, e);
        isError = true;
      }
    }

    if (_admin != null)
      _admin.unregister();

    /*
    if (_clusterManager != null)
      _clusterManager.removeContext(_distributionId);
    */
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
