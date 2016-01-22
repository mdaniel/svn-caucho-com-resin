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

package com.caucho.server.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.caucho.distcache.ByteStreamCache;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.json.Json;
import com.caucho.json.Transient;
import com.caucho.security.Login;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CacheListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.TempOutputStream;

/**
 * Implements a HTTP session.
 */
public class SessionImpl implements HttpSession, CacheListener {
  private static final Logger log
    = Logger.getLogger(SessionImpl.class.getName());
  private static final L10N L = new L10N(SessionImpl.class);

  // the session's identifier
  @Json(name = "SessionId")
  private String _id;

  // the owning session manager
  @Transient
  protected SessionManager _manager;
  // the session objectStore

  // Map containing the actual values.
  @Transient
  protected Map<String,Object> _values;

  // time the session was created
  @Json(name = "CreationTime")
  private long _creationTime;
  // time the session was last accessed
  @Json(name = "AccessTime")
  private long _accessTime;
  // maximum time the session may stay alive.
  @Json(name = "LastUseTime")
  private long _lastUseTime;
  @Json(name = "IdleTimeout")
  private long _idleTimeout;
  @Json(name = "IdleIsSet")
  private boolean _isIdleSet;

  // true if the session is new
  @Json(name = "New")
  private boolean _isNew = true;
  // true if the application has modified the data
  @Transient
  private boolean _isModified;
  // true if the session is still valid, i.e. not invalidated
  
  @Json(name = "Valid")
  private State _state = State.active;

  // the cache entry saved in the session
  @Transient
  private ExtCacheEntry _cacheEntry;

  // to protect for threading
  @Json(name = "UseCount")
  private final AtomicInteger _useCount = new AtomicInteger();

  @Json(name = "LastSaveLength")
  private int _lastSaveLength;
  /**
   * Create a new session object.
   *
   * @param manager the owning session manager.
   * @param id the session identifier.
   * @param creationTime the time in milliseconds when the session was created.
   */
  public SessionImpl(SessionManager manager, String id, long creationTime)
  {
    _manager = manager;
    
    // TCK requires exact time
    creationTime = CurrentTime.getExactTime();

    _creationTime = creationTime;
    setAccessTime(creationTime);
    _lastUseTime = _accessTime;
    _idleTimeout = manager.getSessionTimeout();

    _id = id;

    _values = createValueMap();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " new");
  }

  /**
   * Create the map used to objectStore values.
   */
  protected Map<String,Object> createValueMap()
  {
    return new TreeMap<String,Object>();
  }
  
  private boolean isClosed()
  {
    return _state.isClosed();
  }

  /**
   * Returns the time the session was created.
   */
  @Override
  public long getCreationTime()
  {
    // this test forced by TCK
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0}: can't call getCreationTime() when session is no longer valid.",
                                          this));
    }

    return _creationTime;
  }

  /**
   * Returns the session identifier.
   */
  @Override
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the last objectAccess time.
   */
  @Override
  public long getLastAccessedTime()
  {
    // this test forced by TCK
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0}: can't call getLastAccessedTime() when session is no longer valid.",
                                          this));
    }

    return _accessTime;
  }

  /**
   * Returns the time the session is allowed to be alive.
   *
   * @return time allowed to live in seconds
   */
  @Override
  public int getMaxInactiveInterval()
  {
    if (Long.MAX_VALUE / 2 <= _idleTimeout)
      return -1;
    else
      return (int) (_idleTimeout / 1000);
  }

  /**
   * Sets the maximum time a session is allowed to be alive.
   *
   * @param value time allowed to live in seconds
   */
  @Override
  public void setMaxInactiveInterval(int value)
  {
    if (value < 0)
      _idleTimeout = Long.MAX_VALUE / 2;
    else
      _idleTimeout = ((long) value) * 1000;

    _isIdleSet = true;
  }

  /**
   * Returns the session context.
   *
   * @deprecated
   */
  public HttpSessionContext getSessionContext()
  {
    return null;
  }

  /**
   * Returns the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _manager.getWebApp();
  }

  /**
   * Returns the session manager.
   */
  public SessionManager getManager()
  {
    return _manager;
  }

  /**
   * Returns true if the session is new.
   */
  public boolean isNew()
  {
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0} can't call isNew() when session is no longer valid.", this));
    }

    return _isNew;
  }

  /**
   * Returns true if the session is valid.
   */
  public boolean isValid()
  {
    return _state.isValid();
  }
  
  public boolean isTimeout()
  {
    return isTimeout(CurrentTime.getCurrentTime());
  }

  boolean isTimeout(long now)
  {
    long maxIdleTime = _idleTimeout;

    if (isInUse()) {
      return false;
    }
    else if (now < _lastUseTime + maxIdleTime) {
      return false;
    }
    else {
      long lastUseTime = getLastUseTime();
      
      return lastUseTime + maxIdleTime < now;
    }
  }
  
  private long getLastUseTime()
  {
    ByteStreamCache cache = _manager.getCache();

    if (cache == null)
      return _lastUseTime;

    ExtCacheEntry entry = cache.peekExtCacheEntry(_id);
    // server/01ke

    if (entry == null)
      return _lastUseTime;
    else
      return entry.getLastAccessedTime();
  }

  boolean isClosing()
  {
    return _state.isClosing();
  }

  /**
   * Returns true if the session is empty.
   */
  public boolean isEmpty()
  {
    return _values == null || _values.size() == 0;
  }

  /**
   * Returns true if the session is in use.
   */
  public boolean isInUse()
  {
    return _useCount.get() > 0;
  }
  
  protected int getUseCount()
  {
    return _useCount.get();
  }

  //
  // Attribute API
  //

  /**
   * Returns the named attribute from the session.
   */
  @Override
  public Object getAttribute(String name)
  {
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0}: can't call getAttribute() when session is no longer valid.",
                                          this));
    }

    synchronized (_values) {
      Object value = _values.get(name);

      return value;
    }
  }

  void setModified()
  {
    if (_values.size() > 0)
      _isModified = true;
  }

  /**
   * Sets a session attribute.  If the value is a listener, notify it
   * of the objectModified.  If the value has changed mark the session as changed
   * for persistent sessions.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   */
  @Override
  public void setAttribute(String name, Object value)
  {
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0}: can't call setAttribute(String, Object) when session is no longer valid.", this));
    }

    Object oldValue;
    
    if (value != null
        && ! (value instanceof Serializable)
        && log.isLoggable(Level.FINE)) {
      log.fine(L.l("{0} attribute '{1}' value is non-serializable type '{2}'",
                   this, name, value.getClass().getName()));
    }

    synchronized (_values) {
      if (value != null)
        oldValue = _values.put(name, value);
      else
        oldValue = _values.remove(name);
    }

    // server/017p
    _isModified = true;

    if (oldValue instanceof HttpSessionBindingListener) {
      HttpSessionBindingListener listener;
      listener = (HttpSessionBindingListener) oldValue;

      listener.valueUnbound(new HttpSessionBindingEvent(SessionImpl.this,
                                                        name, oldValue));
    }

    if (value instanceof HttpSessionBindingListener) {
      HttpSessionBindingListener listener;
      listener = (HttpSessionBindingListener) value;

      listener.valueBound(new HttpSessionBindingEvent(SessionImpl.this,
                                                      name, value));
    }

    // Notify the attribute listeners
    ArrayList listeners = _manager.getAttributeListeners();

    if (listeners != null && listeners.size() > 0) {
      HttpSessionBindingEvent event;

      if (oldValue != null)
        event = new HttpSessionBindingEvent(this, name, oldValue);
      else
        event = new HttpSessionBindingEvent(this, name, value);

      for (int i = 0; i < listeners.size(); i++) {
        HttpSessionAttributeListener listener;
        listener = (HttpSessionAttributeListener) listeners.get(i);

        if (oldValue != null)
          listener.attributeReplaced(event);
        else
          listener.attributeAdded(event);
      }
    }
  }

  /**
   * Remove a session attribute.  If the value is a listener, notify it
   * of the objectModified.
   *
   * @param name the name of the attribute to objectRemove
   */
  public void removeAttribute(String name)
  {
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0}: can't call removeAttribute(String) when session is no longer valid.", this));
    }

    Object oldValue;

    synchronized (_values) {
      oldValue = _values.remove(name);
    }

    if (oldValue != null)
      _isModified = true;

    notifyValueUnbound(name, oldValue);
  }

  /**
   * Return an enumeration of all the sessions' attribute names.
   *
   * @return enumeration of the attribute names.
   */
  public Enumeration getAttributeNames()
  {
    synchronized (_values) {
      if (isClosed()) {
        throw new IllegalStateException(L.l("{0} can't call getAttributeNames() when session is no longer valid.", this));
      }

      return Collections.enumeration(_values.keySet());
    }
  }

  /**
   * @deprecated
   */
  public Object getValue(String name)
  {
    return getAttribute(name);
  }

  /**
   * @deprecated
   */
  public void putValue(String name, Object value)
  {
    setAttribute(name, value);
  }

  /**
   * @deprecated
   */
  public void removeValue(String name)
  {
    removeAttribute(name);
  }

  /**
   * @deprecated
   */
  public String []getValueNames()
  {
    synchronized (_values) {
      if (isClosed()) {
        throw new IllegalStateException(L.l("{0} can't call getValueNames() when session is no longer valid.", this));
      }

      if (_values == null)
        return new String[0];

      String []s = new String[_values.size()];

      Enumeration e = getAttributeNames();
      int count = 0;
      while (e.hasMoreElements())
        s[count++] = (String) e.nextElement();

      return s;
    }
  }

  //
  // lifecycle: creation
  //

  /**
   * Creates a new session.
   */
  void create(long now, boolean isCreate)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " create session");
    }

    // e.g. server 'C' when 'A' and 'B' have no record of session
    if (! isClosed()) {
      unbind();
    }
    
    // TCK now cares about exact time
    now = CurrentTime.getExactTime();

    // _isValid = true;
    _isNew = true;
    setAccessTime(now);
    _creationTime = now;
    
    // server/01np
    _isModified = true;

    /*
    if (_clusterObject != null && isCreate)
      _clusterObject.objectCreate();
    */
  }

  /**
   * Set true if the session is in use.
   */
  boolean addUse()
  {
    _lastUseTime = CurrentTime.getCurrentTime();

    _useCount.incrementAndGet();

    /*
    synchronized (this) {
      if (_isClosing)
        return false;

      _useCount++;

      return true;
    }
    */

    return true;
  }

  /**
   * Set true if the session is in use.
   */
  void endUse()
  {
    _useCount.decrementAndGet();
    /*
    synchronized (this) {
      _useCount--;
    }
    */
  }

  /*
   * Set the current objectAccess time to now.
   */
  void setAccess(long now)
  {
    // server/01k0
    if (_useCount.get() > 1)
      return;

    _isNew = false;

    /*
    if (_clusterObject != null)
      _clusterObject.objectAccess();
    */
    // TCK now cares about exact time
    /*now = Alarm.getExactTime();

    _accessTime = now;*/
  }
  
  public void setAccessTime(long accessTime)
  {
    // server/0123 (vs TCK?)
    _accessTime = accessTime;
  }

  public int getLastSaveLength()
  {
    return _lastSaveLength;
  }

  /**
   * Cleaning up session stuff at the end of a request.
   *
   * <p>If the session data has changed and we have persistent sessions,
   * save the session.  However, if save-on-shutdown is true, only save
   * on a server shutdown.
   */
  public void finishRequest()
  {
    // server/0122
    // TCK cares about exact time
    setAccessTime(CurrentTime.getExactTime());
    _isNew = false;

    // update cache access?
    if (_useCount.decrementAndGet() > 0)
      return;

    saveAfterRequest();
  }

  //
  // persistent sessions and passivation
  //

  /**
   * Loads the session.
   *
   * @return true if the session was found in the persistent store
   */
  public boolean load(boolean isNew)
  {
    long now = CurrentTime.getCurrentTime();
    
    if (! _state.isValid()) {
      return false;
    }
    else if (_isIdleSet && _accessTime + _idleTimeout < now) {
      // server/01o2 (tck)
    
      return false;
    }

    // server/01k0
    if (_useCount.get() > 1) {
      return true;
    }

    try {
      ByteStreamCache cache = _manager.getCache();

      if (cache == null) {
        return ! isNew;
      }

      // server/015m
      if (! isNew && _manager.isSaveOnShutdown()) {
        return true;
      }

      ExtCacheEntry entry = cache.getExtCacheEntry(_id);
      ExtCacheEntry cacheEntry = _cacheEntry;

      if (entry != null && ! entry.isValueNull()) {
        // server/01a1, #4419

        _idleTimeout = entry.getAccessedExpireTimeout();

        long lastAccessTime = entry.getLastAccessedTime();

        if (lastAccessTime + _idleTimeout * 5 / 4 < now) {
          return false;
        }
        // _idleTimeout = entry.getIdleTimeout() * 4 / 5;
        //_isIdleSet = true;
      }

      //if (entry != null && cacheEntry != null
      //    && cacheEntry.getValueHash() == entry.getValueHash()) {
      
      if (entry != null && cacheEntry != null
          && (entry.getValueHash() == cacheEntry.getValueHash()
              || entry.getVersion() <= cacheEntry.getVersion())) {
        if (log.isLoggable(Level.FINE)) {
          log.fine(this + " session load-same valueHash="
              + (entry != null ? Long.toHexString(entry.getValueHash()) : null));
        }

        entry.updateAccessTime();

        _isModified = false;

        return true;
      }

      TempOutputStream os = new TempOutputStream();

      if (cache.get(_id, os)) {
        InputStream is = os.getInputStream();
        boolean isValid = false;

        try {
          HashChunkInputStream crcIs = new HashChunkInputStream(is);

          SessionDeserializer in = _manager.createSessionDeserializer(crcIs);

          if (log.isLoggable(Level.FINE)) {
            log.fine(this + " session load valueHash="
                + (entry != null ? Long.toHexString(entry.getValueHash()) : null));
          }

          isValid = load(in);

          in.close();
          crcIs.close();
        } finally {
          is.close();
        }
        
        if (isValid) {
          _cacheEntry = entry;
          _isModified = false;

          return true;
        }
        else {
          _cacheEntry = entry;
          _isModified = true;
          
          return false;
        }
      }
      else {
        _cacheEntry = null;

        if (log.isLoggable(Level.FINE)) {
          log.fine(this + " session remove");
        }

        if (cacheEntry == null) {
          return true;
        }
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return false;
  }

  /**
   * Loads the object from the input stream.
   */
  public boolean load(SessionDeserializer in)
    throws IOException
  {
    HttpSessionEvent event = null;
    ArrayList<HttpSessionActivationListener> listeners = null;

    String id = null;
    
    synchronized (this) {
      synchronized (_values) {
        // server/017u
        _values.clear();
        // unbind();

        try {
          id = (String) in.readObject();
          int size = in.readInt();

          // System.out.println("LOAD: " + size + " " + this + " " + _clusterObject + System.identityHashCode(this));

          for (int i = 0; i < size; i++) {
            String key = (String) in.readObject();
            Object value = in.readObject();

            if (value != null) {
              _values.put(key, value);

              if (value instanceof HttpSessionActivationListener) {
                HttpSessionActivationListener listener
                  = (HttpSessionActivationListener) value;

                if (event == null)
                  event = new HttpSessionEvent(this);

                if (listeners == null)
                  listeners = new ArrayList<HttpSessionActivationListener>();

                listeners.add(listener);
              }
            }
          }
        } catch (Exception e) {
          throw IOExceptionWrapper.create(e);
        }
      }
    }

    for (int i = 0; listeners != null && i < listeners.size(); i++) {
      HttpSessionActivationListener listener = listeners.get(i);

      if (event == null)
        event = new HttpSessionEvent(this);

      listener.sessionDidActivate(event);
    }

    listeners = _manager.getActivationListeners();
    for (int i = 0; listeners != null && i < listeners.size(); i++) {
      HttpSessionActivationListener listener = listeners.get(i);

      if (event == null)
        event = new HttpSessionEvent(this);

      listener.sessionDidActivate(event);
    }
    
    if (getId().equals(id)) {
      return true;
    }
    else {
      log.warning("Invalid session load id=" + getId() + ", but loaded id=" + id);
      
      synchronized (_values) {
        _values.clear();
      }
      
      return false;
    }
  }

  /**
   * Clears the session when reading a bad saved session.
   */
  void reset(long now)
  {
    if (log.isLoggable(Level.FINER))
      log.fine(this + " reset");

    now = CurrentTime.getCurrentTime();

    unbind();
    //_isValid = true;
    _isNew = true;
    setAccessTime(now);
    _creationTime = now;
  }

  /**
   * Save changes before any flush.
   */
  public final void saveBeforeFlush()
  {
    if (_manager == null || ! _manager.isSaveBeforeFlush()) {
      return;
    }

    save();
  }

  /**
   * Flush changes before the headers.
   */
  public final void saveBeforeHeaders()
  {
    if (_manager == null || ! _manager.isSaveBeforeHeaders()) {
      return;
    }

    save();
  }

  /**
   * Flush changes after a request completes.
   */
  public final void saveAfterRequest()
  {
    if (_manager == null || ! _manager.isSaveAfterRequest()) {
      return;
    }

    save();
  }

  /**
   * Saves changes to the session.
   */
  public final void save()
  {
    if (! isValid()) {
      return;
    }

    try {
      if (! _isModified && ! _manager.getAlwaysSaveSession()) {
        return;
      }

      if (! _manager.isPersistenceEnabled()) {
        return;
      }

      _isModified = false;

      TempOutputStream os = new TempOutputStream();
      HashChunkOutputStream crcOs = new HashChunkOutputStream(os);
      
      SessionSerializer out = _manager.createSessionSerializer(crcOs);

      store(out);
      
      out.close();
      crcOs.close();

      final int length = os.getLength();

      _manager.addSessionSaveSample(length);

      _lastSaveLength = length;
      
      // #5170
      long lastAccessTime = _accessTime;
      long lastModifiedTime = lastAccessTime;

      _cacheEntry = _manager.getCache().put(_id, os.getInputStream(),
                                            _idleTimeout,
                                            -1,
                                            lastAccessTime,
                                            lastModifiedTime);

      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " session save valueHash="
                 + (_cacheEntry != null ? Long.toHexString(_cacheEntry.getValueHash()) : null));
      }

      os.close();
    } catch (Exception e) {
      log.log(Level.WARNING, this + ": can't serialize session", e);
    }
  }

  /**
   * Store on shutdown.
   */
  void saveOnShutdown()
  {
    /*
        if (session.isValid()) {
          synchronized (session) {
            // server/016i, server/018x
            if (! session.isEmpty())
              session.saveOnShutdown();
          }
        }
    */

    save();
    /*
    try {
      ClusterObject clusterObject = _clusterObject;

      if (clusterObject != null) {
        clusterObject.objectModified();
        clusterObject.objectStore(this);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, this + ": can't serialize session", e);
    }
    */
  }

  /**
   * Passivates the session.
   */
  public void passivate()
  {
    unbind();
  }

  /**
   * Saves the object to the input stream.
   */
  public void store(SessionSerializer out)
    throws IOException
  {
    Set<Map.Entry<String,Object>> set = null;

    HttpSessionEvent event = null;
    ArrayList<HttpSessionActivationListener> listeners;
    
    out.writeObject(getId());

    synchronized (_values) {
      set = _values.entrySet();

      int size = set == null ? 0 : set.size();

      if (size == 0) {
        out.writeInt(0);
        return;
      }

      listeners = _manager.getActivationListeners();

      if (listeners != null && listeners.size() > 0) {
        if (event == null)
          event = new HttpSessionEvent(this);

        for (int i = 0; i < listeners.size(); i++) {
          HttpSessionActivationListener listener = listeners.get(i);

          listener.sessionWillPassivate(event);
        }
      }

      for (Map.Entry entry : set) {
        Object value = entry.getValue();

        if (value instanceof HttpSessionActivationListener) {
          HttpSessionActivationListener listener
            = (HttpSessionActivationListener) value;

          if (event == null)
            event = new HttpSessionEvent(this);

          listener.sessionWillPassivate(event);
        }
      }
    }

    synchronized (this) {
      synchronized (_values) {
        set = _values.entrySet();

        int size = set == null ? 0 : set.size();

        out.writeInt(size);

        if (size == 0) {
          return;
        }

        boolean ignoreNonSerializable
          = getManager().getIgnoreSerializationErrors();

        for (Map.Entry entry : set) {
          Object value = entry.getValue();

          out.writeObject(entry.getKey());

          if (ignoreNonSerializable && ! (value instanceof Serializable)) {
            out.writeObject(null);
            continue;
          }

          try {
            out.writeObject(value);
          } catch (NotSerializableException e) {
            log.warning(L.l("{0}: failed storing persistent session attribute '{1}'.  Persistent session values must extend java.io.Serializable.\n{2}",
                            this, entry.getKey(), String.valueOf(e)));
            throw e;
          }
        }
      }
    }
  }

  //
  // invalidation, lru, timeout
  //

  /**
   * Invalidates the session, called by user code.
   *
   * This should never be called by Resin code (for logging purposes)
   */
  public void invalidate()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " invalidate");

    _state = _state.toInvalidating();

    invalidate(Logout.INVALIDATE);
  }

  /**
   * Called by the session manager for a session timeout
   */
  public void timeout()
  {
    if (! isValid())
      return;

    if (! _manager.isPersistenceEnabled()) {
      // if no persistent store then invalidate
      // XXX: server/12cg - single signon shouldn't logout
      invalidateTimeout();
    }
    /*
      else if (session.getSrunIndex() != _selfIndex && _selfIndex >= 0) {
      if (log.isLoggable(Level.FINE))
      log.fine(session + " timeout (backup)");

      // if not the owner, then just remove
      _sessions.remove(session.getId());
    }
    */
    else {
      invalidateTimeout();
    }
  }

  /**
   * Callback when the session is removed from the session cache, generally
   * because the session cache is full.
   */
  @Override
  public void removeEvent()
  {
    synchronized (this) {
      if (_state.isInvalidating() || _useCount.get() <= 0) {
        _state = _state.toLru();
      }
    }

    if (! _state.isClosing()) {
      log.warning(L.l("{0} LRU while in use (use-count={1}).  Consider increasing session-count.",
                      this,
                      _useCount));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " remove");

    long now = CurrentTime.getCurrentTime();

    // server/015k, server/10g2
    if (_state.isInvalidating()
        || _manager.isDestroyOnLru()
        || _accessTime + getMaxInactiveInterval() < now) {
      notifyDestroy();
    }

    invalidateLocal();
  }

  private void notifyDestroy()
  {
    // server/01ni
    /* XXX:
    if (_clusterObject != null && ! _clusterObject.isPrimary())
      return;
    */

    ArrayList<HttpSessionListener> listeners = _manager.getListeners();

    if (listeners != null) {
      HttpSessionEvent event = new HttpSessionEvent(this);

      for (int i = listeners.size() - 1; i >= 0; i--) {
        HttpSessionListener listener;
        listener = (HttpSessionListener) listeners.get(i);

        listener.sessionDestroyed(event);
      }
    }
  }

  /**
   * Invalidates a session based on a logout.
   */
  public void invalidateLogout()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " logout");

    _state = _state.toInvalidating();
    invalidate(Logout.INVALIDATE);
  }

  /**
   * Invalidates a session based on a timeout
   */
  void invalidateTimeout()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " timeout");

    if (_manager.isOwner(_id)) {
      _state = _state.toInvalidating();
    }

    invalidate(Logout.TIMEOUT);
  }

  /**
   * Invalidates a session based on a LRU
   */
  void invalidateLru()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " lru");

    invalidateImpl(Logout.LRU);
  }

  /**
   * Invalidates the session, called by user code.
   *
   * This should never be called by Resin code (for logging purposes)
   */
  public void invalidateRemote()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " invalidate remote");

    _state = _state.toInvalidating();
    invalidate(Logout.INVALIDATE);
  }

  /**
   * Invalidates the session.
   */
  private void invalidate(Logout logout)
  {
    if (isClosed()) {
      throw new IllegalStateException(L.l("{0}: Can't call invalidate() when session is no longer valid.",
                                          this));
    }

    try {
      // server/017s
      // server/12i1, 12ch
      Login login = _manager.getWebApp().getLogin();

      if (login != null) {
        login.sessionInvalidate(this, logout == Logout.TIMEOUT);
      }

      _manager.removeSession(this);

      invalidateImpl(logout);
    } finally {
      _state = _state.toClosed();
    }
  }

  /**
   * Invalidate the session, removing it from the manager,
   * unbinding the values, and removing it from the objectStore.
   */
  private void invalidateImpl(Logout logout)
  {
    State state = _state;
    
    boolean invalidateAfterListener = _manager.isInvalidateAfterListener();
    if (! invalidateAfterListener) {
      _state = _state.toClosing();
    }

    try {
      if (state.isInvalidating() && _manager.getSessionStore() != null) {
        boolean isRemove = false;

        /*
        if (logout == Logout.TIMEOUT) {
          // server/016r
          ExtCacheEntry entry
            = _manager.getSessionStore().peekExtCacheEntry(_id);

          if (entry == null || ! entry.isValid()) {
            isRemove = true;
          }
        }
        else
          isRemove = true;
          */
        
        // server/018z
        isRemove = true;

        if (isRemove) {
          _manager.getSessionStore().remove(_id);
        }
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    invalidateLocal();
  }

  /**
   * unbinds the session and saves if necessary.
   */
  private void invalidateLocal()
  {
    if (! isClosed() && ! _state.isInvalidating()) {
      if (_manager.isSaveOnlyOnShutdown()) {
        save();
      }
    }

    unbind(); // we're invalidating, not passivating
  }

  /**
   * Cleans up the session.
   */
  public void unbind()
  {
    if (_values.size() == 0) {
      return;
    }

    // ClusterObject clusterObject = _clusterObject;

    ArrayList<String> names = new ArrayList<String>();
    ArrayList<Object> values = new ArrayList<Object>();

    synchronized (_values) {
      /*
      if (_useCount > 0)
        Thread.dumpStack();
      */

      for (Map.Entry<String,Object> entry : _values.entrySet()) {
        names.add(entry.getKey());
        values.add(entry.getValue());
      }

      _values.clear();
    }

    // server/015a
    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      Object value = values.get(i);

      notifyValueUnbound(name, value);
    }
  }

  /**
   * Notify any value unbound listeners.
   */
  private void notifyValueUnbound(String name, Object oldValue)
  {
    if (oldValue == null)
      return;

    if (oldValue instanceof HttpSessionBindingListener) {
      HttpSessionBindingListener listener;
      listener = (HttpSessionBindingListener) oldValue;

      listener.valueUnbound(new HttpSessionBindingEvent(this,
                                                        name,
                                                        oldValue));
    }

    // Notify the attributes listeners
    ArrayList listeners = _manager.getAttributeListeners();
    if (listeners != null) {
      HttpSessionBindingEvent event;

      event = new HttpSessionBindingEvent(this, name, oldValue);

      for (int i = 0; i < listeners.size(); i++) {
        HttpSessionAttributeListener listener;
        listener = (HttpSessionAttributeListener) listeners.get(i);

        listener.attributeRemoved(event);
      }
    }
  }

  @Override
  public String toString()
  {
    String contextPath = "";

    SessionManager manager = _manager;
    if (manager != null) {
      WebApp webApp = manager.getWebApp();

      if (webApp != null)
        contextPath = "," + webApp.getContextPath();
    }

    return getClass().getSimpleName() + "[" + getId() + contextPath + "]";
  }

  enum Logout {
    INVALIDATE,
    LRU,
    TIMEOUT
  };

  enum State {
    active {
      @Override
      boolean isValid() { return true; }
    },
    invalidating {
      @Override
      boolean isClosing() { return true; }
      @Override
      boolean isInvalidating() { return true; }
      @Override
      State toLru() { return this; }
    },
    lru {
      @Override
      boolean isClosing() { return true; }
    },
    closing {
      @Override
      boolean isClosing() { return true; }
      @Override
      State toInvalidating() { return this; }
      @Override
      State toLru() { return this; }
    },
    closed {
      @Override
      boolean isClosed() { return true; }
      @Override
      State toClosing() { return this; }
      @Override
      State toInvalidating() { return this; }
      @Override
      State toLru() { return this; }
    };
    
    boolean isValid()
    {
      return false;
    }
    
    boolean isClosing()
    {
      return false;
    }
    
    boolean isInvalidating()
    {
      return false;
    }
    
    boolean isClosed()
    {
      return false;
    }
    
    State toClosed()
    {
      return State.closed;
    }
    
    State toClosing()
    {
      return State.closing;
    }
    
    State toInvalidating()
    {
      return State.invalidating;
    }
    
    State toLru()
    {
      return State.lru;
    }
  }
}
