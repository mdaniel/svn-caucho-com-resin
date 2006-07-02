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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.session;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.naming.*;

import com.caucho.log.Log;

import com.caucho.util.*;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;

import com.caucho.config.types.JndiBuilder;

import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.DispatchServer;

import com.caucho.server.cluster.FileStore;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterObject;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.ObjectManager;
import com.caucho.server.cluster.Store;
import com.caucho.server.cluster.StoreManager;

import com.caucho.server.session.SessionManager;

import com.caucho.server.webapp.Application;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.SessionArrayValue;

/**
 * Stripped down version of com.caucho.server.session.SessionManager,
 * customized to PHP instead of J2EE sessions.
 */
public class QuercusSessionManager implements ObjectManager, AlarmListener {
  static protected final L10N L = new L10N(QuercusSessionManager.class);
  static protected final Logger log = Log.open(QuercusSessionManager.class);

  private static int FALSE = 0;
  private static int COOKIE = 1;
  private static int TRUE = 2;

  private static int UNSET = 0;
  private static int SET_TRUE = 1;
  private static int SET_FALSE = 2;
  
  // active sessions
  private LruCache<String,SessionArrayValue> _sessions;
  // total sessions
  private int _totalSessions;

  // iterator to purge sessions (to reduce gc)
  private Iterator<SessionArrayValue> _sessionIter;
  // array list for session timeout
  private ArrayList<SessionArrayValue> _sessionList 
    = new ArrayList<SessionArrayValue>();

  // maximum number of sessions
  private int _sessionMax = 4096;

  private int _reuseSessionId = COOKIE;
  private int _cookieLength = 18;

  private StoreManager _storeManager;
  private Store _sessionStore;

  private int _alwaysLoadSession;
  private boolean _alwaysSaveSession;
  private boolean _saveOnlyOnShutdown;

  private boolean _isModuloSessionId = false;
  private boolean _isAppendServerIndex = false;
  private boolean _isTwoDigitSessionIndex = false;
  
  private boolean _isClosed;

  private Alarm _alarm = new Alarm(this);

  // statistics
  private Object _statisticsLock = new Object();
  private long _sessionCreateCount;
  private long _sessionTimeoutCount;

  private SessionManager _sessionManager;

  private Application _app;

  /**
   * Creates and initializes a new session manager.
   */
  public QuercusSessionManager()
  {
    _sessions = new LruCache<String,SessionArrayValue>(_sessionMax);
    _sessionIter = _sessions.values();
  }

  /**
   * Sets the SessionManager for this QuercusSessionManager.
   */
  public void setSessionManager(SessionManager sessionManager, Application app)
  {
    _sessionManager = sessionManager;

    synchronized(this) {
      if (_sessions.size() == 0) {
        _sessionMax = _sessionManager.getSessionMax();
        _sessions = new LruCache<String,SessionArrayValue>(_sessionMax);
      }
    }

    _app = app;

    _alarm.queue(60000);
  }

  private StoreManager getStoreManager()
  {
    if (_storeManager == null) {
      Cluster cluster = Cluster.getLocal();

      if (cluster != null)
        _storeManager = cluster.getStore();
    }

    return _storeManager;
  }

  private Store getSessionStore()
  {
    if (_sessionStore == null) {
      StoreManager manager = getStoreManager();

      if (manager != null) {
        String hostName = _app.getHostName();
        String contextPath = _app.getContextPath();

        if (hostName == null || hostName.equals(""))
          hostName = "default";

        String storeId = hostName + contextPath + "-PHP";

        _sessionStore = manager.createStore(storeId, this);
        _sessionStore.setMaxIdleTime(_sessionManager.getMaxIdleTime());
      }
    }

    return _sessionStore;
  }

  public SessionManager getSessionManager()
  {
    return _sessionManager;
  }

  /**
   * Create a new session.
   *
   * @param oldId the id passed to the request.  Reuse if possible.
   * @param now the current date
   *
   */
  public SessionArrayValue createSession(Env env, String oldId, long now)
  {
    String id = oldId;

    if (id == null || id.length() < 4 || ! _sessionManager.isInSessionGroup(id))
      id = createSessionId(env);

    SessionArrayValue session = create(env, id, now);

    if (session == null)
      return null;
    
    synchronized (_statisticsLock) {
      _sessionCreateCount++;
    }

    if (getSessionStore() != null && id.equals(oldId))
      load(env, session, now);
    
    return session;
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different applications on the
   * same matchine should use the same cookie.
   *
   */
  public String createSessionId(Env env)
  {
    String id;

    do {
      id = _sessionManager.createSessionIdImpl();
    } while (getSession(env, id, 0) != null);

    if (id == null || id.equals(""))
      throw new RuntimeException();

    return id;
  }

  /**
   * Returns a session from the session store, returning null if there's
   * no cached session.
   *
   * @param key the session id
   * @param now the time in milliseconds.  now == 0 implies
   * that we're just checking for the existence of such a session in
   * the cache and do not intend actually to load it if it is not.
   *
   * @return the cached session.
   * 
   */
  public SessionArrayValue getSession(Env env, String key, long now)
  {
    SessionArrayValue session;
    boolean isNew = false;
    boolean killSession = false;

    if (_sessions == null)
      return null;

    // Check the cache first
    session = _sessions.get(key);

    if (session != null && ! session.getId().equals(key))
      throw new IllegalStateException(key + " != " + session.getId());

    if (session != null) {
      if (session.inUse())
        return (SessionArrayValue)session.copy(env);
    }
    else if (now > 0 && getSessionStore() != null) {
      if (! _sessionManager.isInSessionGroup(key))
        return null;

      session = create(env, key, now);
      isNew = true;
    }

    if (session == null)
      return null;

    if (isNew) {
      killSession = ! load(env, session, now);
      isNew = killSession;
    }
    else if (! getSaveOnlyOnShutdown() && ! session.load()) {
      // if the load failed, then the session died out from underneath
      session.reset(now);
      isNew = true;
    }

    if (killSession) {
      session.setValid(false);
      _sessions.remove(key);

      return null;
    }
    else if (! isNew)
      session.setAccess(now);

    return (SessionArrayValue)session.copy(env);
  }

  public void saveSession(Env env, SessionArrayValue session)
  {
    session.finish();
    _sessions.put(session.getId(), (SessionArrayValue) session.copy(env));
  }

  /**
   * Creates a session.  It's already been established that the
   * key does not currently have a session.
   *
   */
  private SessionArrayValue create(Env env, String key, long now)
  {
    SessionArrayValue session = 
      new SessionArrayValue(key, now, _sessionManager.getSessionTimeout());

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(key, session);

    if (! key.equals(session.getId()))
      throw new IllegalStateException(key + " != " + session.getId());

    if (getSessionStore() != null) {
      ClusterObject clusterObject = getSessionStore().createClusterObject(key);

      clusterObject.setObjectManager(this);

      session.setClusterObject(clusterObject);
    }

    return (SessionArrayValue)session.copy(env);
  }

  /**
   * Loads the session from the backing store.  
   *
   * @param session the session to load.
   * @param now current time in milliseconds.  now == 0 implies
   * that we're just checking for the existence of such a session in
   * the cache and do not intend actually to load it if it is not.
   *
   */
  private boolean load(Env env, SessionArrayValue session, long now)
  {
    try {
      if (session.inUse()) {
        return true;
      }
      else if (now <= 0) {
        return false;
      }
      else if (session.load()) {
        session.setAccess(now);
        return true;
      }
      else {
        session.reset(now);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      session.reset(now);
    }

    return false;
  }

  public long getMaxIdleTime()
  {
    return _sessionManager.getMaxIdleTime();
  }

  /**
   * Timeout for reaping old sessions.
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

      if (getSessionStore() != null)
        accessWindow = getSessionStore().getAccessWindowTime();

      synchronized (_sessions) {
        _sessionIter = _sessions.values(_sessionIter);

        while (_sessionIter.hasNext()) {
          SessionArrayValue session = _sessionIter.next();

          long maxIdleTime = session.getMaxInactiveInterval() + accessWindow;

          if (session.inUse())
            liveSessions++;
          else if (session.getAccessTime() + maxIdleTime < now)
            _sessionList.add(session);
          else
            liveSessions++;
        }
      }

      synchronized (_statisticsLock) {
        _sessionTimeoutCount += _sessionList.size();
      }

      for (int i = 0; i < _sessionList.size(); i++) {
        SessionArrayValue session = _sessionList.get(i);

        try {
          long maxIdleTime = session.getMaxInactiveInterval();
          _sessions.remove(session.getId());

          int sessionSrunIndex = -1; 
          char ch = session.getId().charAt(0);

          if (_sessionManager.getSrunLength() > 0)
            sessionSrunIndex = 
              _sessionManager.decode(ch) % _sessionManager.getSrunLength();

          if (getStoreManager() == null ||
              (sessionSrunIndex == _sessionManager.getSrunIndex() && 
               _sessionManager.getSrunIndex() >= 0))
            session.invalidate();
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
   * Cleans up the sessions when the Application shuts down gracefully.
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

    ArrayList<SessionArrayValue> list = new ArrayList<SessionArrayValue>();

    boolean isError = false;

    synchronized (_sessions) {
      _sessionIter = _sessions.values(_sessionIter);

      while (_sessionIter.hasNext()) {
        SessionArrayValue session = _sessionIter.next();

        if (session.isValid())
          list.add(session);
      }
    }

    for (int i = list.size() - 1; i >= 0; i--) {
      SessionArrayValue session = list.get(i);

      try {
        if (session.isValid()) {
          synchronized (session) {
            if (! session.isEmpty())
              session.storeOnShutdown();
          }
        }

        _sessions.remove(session.getId());
      } catch (Exception e) {
        if (! isError)
          log.log(Level.WARNING, "Can't store session: " + e, e);
        isError = true;
      }
    }
  }

  /**
   * True if sessions should always be saved.
   */
  boolean getAlwaysSaveSession()
  {
    return _alwaysSaveSession;
  }

  /**
   * True if sessions should always be saved.
   */
  public void setAlwaysSaveSession(boolean save)
  {
    _alwaysSaveSession = save;
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
   * True if sessions should only be saved on shutdown.
   */
  public boolean getSaveOnlyOnShutdown()
  {
    return _saveOnlyOnShutdown;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnlyOnShutdown(boolean save)
  {
    _saveOnlyOnShutdown = save;
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
   * Sets the cookie length
   */
  public void setCookieLength(int cookieLength)
  {
    if (cookieLength < 7)
      cookieLength = 7;

    _cookieLength = cookieLength;
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
   * Returns true if the sessions are closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
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
   * Removes a session from the cache and deletes it from the backing store,
   * if applicable.
   */
  public void removeSession(String sessionId)
  {
    _sessions.remove(sessionId);

    if (getSessionStore() != null) {
      try {
        getSessionStore().remove(sessionId);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
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
    SessionArrayValue session = (SessionArrayValue) obj;

    session.load(null, in);
  }

  /**
   * Checks if the session is empty.
   */
  public boolean isEmpty(Object obj)
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    return session.isEmpty();
  }

  /**
   * Notification from the cluster.
   */
  public void notifyRemove(String id)
  {
    SessionArrayValue session = _sessions.remove(id);

    if (session != null)
      session.invalidate();
  }

  /**
   * Notification from the cluster.
   */
  public void notifyUpdate(String id)
  {
  }

  /**
   * Saves the session.
   */
  public void store(ObjectOutputStream out, Object obj)
    throws IOException
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    session.store(out);
  }

  /**
   * Sets module session id generation.
   */
  public void setCookieAppendServerIndex(boolean isAppend)
  {
    _isAppendServerIndex = isAppend;
  }
}
