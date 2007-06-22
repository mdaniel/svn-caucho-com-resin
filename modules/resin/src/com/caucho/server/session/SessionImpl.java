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

import com.caucho.log.Log;
import com.caucho.hessian.io.*;
import com.caucho.server.cluster.ClusterObject;
import com.caucho.server.cluster.Store;
import com.caucho.server.security.AbstractAuthenticator;
import com.caucho.server.security.ServletAuthenticator;
import com.caucho.util.Alarm;
import com.caucho.util.CacheListener;
import com.caucho.util.L10N;
import com.caucho.vfs.IOExceptionWrapper;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a HTTP session.
 */
public class SessionImpl implements HttpSession, CacheListener {
  static protected final Logger log = Log.open(SessionImpl.class);
  static final L10N L = new L10N(SessionImpl.class);

  static final String LOGIN = "caucho.login";

  // the session's identifier
  private String _id;

  // the owning session manager
  protected SessionManager _manager;
  // the session store

  // Map containing the actual values.
  protected Map<String,Object> _values;

  // time the session was created
  private long _creationTime;
  // time the session was last accessed
  long _accessTime;
  // maximum time the session may stay alive.
  long _maxInactiveInterval;
  // true if the session is new
  private boolean _isNew = true;
  // true if the session is still valid, i.e. not invalidated
  boolean _isValid = true;
  // true if the session is closing
  boolean _isClosing = false;
  // true if the session is being closed from an invalidation
  boolean _isInvalidating = false;
  //To protect for threading
  private int _useCount;

  private ClusterObject _clusterObject;
  // The logged-in user
  private Principal _user;

  // index of the owning srun
  private int _srunIndex = -1;

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

    _creationTime = creationTime;
    _accessTime = creationTime;
    _maxInactiveInterval = manager.getSessionTimeout();

    _id = id;

    // Finds the owning JVM from the session encoding
    char ch = id.charAt(0);
    int length = manager.getSrunLength();

    if (length > 0)
      _srunIndex = SessionManager.decode(ch) % length;
    else
      _srunIndex = 0;

    _values = createValueMap();

    if (log.isLoggable(Level.FINE))
      log.fine("create session " + id);
  }

  /**
   * Returns the time the session was created.
   */
  public long getCreationTime()
  {
    // this test forced by TCK
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call getCreationTime() when session is no longer valid."));

    return _creationTime;
  }

  /**
   * Returns the session identifier.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the index of the owning srun for distributed sessions.
   */
  int getSrunIndex()
  {
    return _srunIndex;
  }

  /**
   * Sets the cluster object.
   */
  void setClusterObject(ClusterObject clusterObject)
  {
    _clusterObject = clusterObject;
    if (clusterObject != null)
      clusterObject.update();
  }

  /**
   * Returns the last access time.
   */
  public long getLastAccessedTime()
  {
    // this test forced by TCK
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call getLastAccessedTime() when session is no longer valid."));

    return _accessTime;
  }

  /**
   * Returns the time the session is allowed to be alive.
   *
   * @return time allowed to live in seconds
   */
  public int getMaxInactiveInterval()
  {
    if (Long.MAX_VALUE / 2 <= _maxInactiveInterval)
      return -1;
    else
      return (int) (_maxInactiveInterval / 1000);
  }

  /**
   * Sets the maximum time a session is allowed to be alive.
   *
   * @param value time allowed to live in seconds
   */
  public void setMaxInactiveInterval(int value)
  {
    if (value < 0)
      _maxInactiveInterval = Long.MAX_VALUE / 2;
    else
      _maxInactiveInterval = ((long) value) * 1000;

    if (_clusterObject != null)
      _clusterObject.setExpireInterval(_maxInactiveInterval);
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
   * Returns the authenticator
   */
  public ServletAuthenticator getAuthenticator()
  {
    return _manager.getWebApp().getAuthenticator();
  }

  /**
   * Returns the user
   */
  public Principal getUser()
  {
    if (_user != null)
      return _user;

    if (_isValid) {
      _user = (Principal) getAttribute(LOGIN);
    }

    return _user;
  }

  /**
   * Sets the user
   */
  public void setUser(Principal user)
  {
    _user = user;

    setAttribute(LOGIN, user);
  }

  /**
   * Returns the named attribute from the session.
   */
  public Object getAttribute(String name)
  {
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call getAttribute() when session is no longer valid."));

    synchronized (_values) {
      return _values.get(name);
    }
  }

  /**
   * Sets a session attribute.  If the value is a listener, notify it
   * of the change.  If the value has changed mark the session as changed
   * for persistent sessions.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   */
  public void setAttribute(String name, Object value)
  {
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call setAttribute(String, Object) when session is no longer valid."));

    Object oldValue;

    if (value != null &&
	! (value instanceof Serializable) &&
	log.isLoggable(Level.FINE)) {
      log.fine(L.l("session attribute '{0}' value is non-serializable type '{1}'",
		   name, value.getClass().getName()));
    }

    synchronized (_values) {
      if (value != null)
        oldValue = _values.put(name, value);
      else
        oldValue = _values.remove(name);
    }

    // server/017p
    if (_clusterObject != null) // && value != oldValue)
      _clusterObject.change();

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
    if (listeners != null) {
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
   * Create the map used to store values.
   */
  protected Map<String,Object> createValueMap()
  {
    return new Hashtable<String,Object>(8);
  }

  /**
   * Remove a session attribute.  If the value is a listener, notify it
   * of the change.
   *
   * @param name the name of the attribute to remove
   */
  public void removeAttribute(String name)
  {
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call removeAttribute(String) when session is no longer valid."));

    Object oldValue = _values.remove(name);

    if (_clusterObject != null && oldValue != null)
      _clusterObject.change();

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
      if (! _isValid)
	throw new IllegalStateException(L.l("Can't call getAttributeNames() when session is no longer valid."));

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
      if (! _isValid)
	throw new IllegalStateException(L.l("Can't call getValueNames() when session is no longer valid."));

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

  /**
   * Returns true if the session is new.
   */
  public boolean isNew()
  {
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call isNew() when session is no longer valid."));

    return _isNew;
  }

  /**
   * Returns true if the session is valid.
   */
  public boolean isValid()
  {
    return _isValid;
  }

  /**
   * Set the session valid or invalid.
   */
  void setValid(boolean isValid)
  {
    _isValid = isValid;
  }

  boolean isClosing()
  {
    return _isClosing;
  }

  /**
   * Callback when the session is removed from the session cache, generally
   * because the session cache is full.
   */
  public void removeEvent()
  {
    synchronized (this) {
      if (_isInvalidating || _useCount <= 0)
	_isClosing = true;
    }

    if (! _isClosing) {
      log.warning(L.l("session {0} LRU while in use.  Consider increasing session-count.",
		     _id));
    }
    
    boolean isValid = _isValid;

    if (log.isLoggable(Level.FINE))
      log.fine("remove session " + _id);

    long now = Alarm.getCurrentTime();

    Store store = _manager.getSessionStore();

    // server/015k
    if (_isInvalidating ||
	store == null || _accessTime + getMaxInactiveInterval() < now)
      notifyDestroy();

    invalidateLocal();
  }

  private void notifyDestroy()
  {
    ArrayList listeners = _manager.getListeners();

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
   * Invalidates the session.
   */
  public void invalidate()
  {
    _isInvalidating = true;

    invalidate(Logout.INVALIDATE);
  }

  /**
   * Invalidates the session.
   */
  public void invalidate(Logout logout)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine("invalidate session " + _id);
    }

    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call invalidate() when session is no longer valid."));

    ServletAuthenticator auth = getAuthenticator();
    if (! (auth instanceof AbstractAuthenticator)
	|| logout == Logout.INVALIDATE
	|| (logout == Logout.TIMEOUT
	    && ((AbstractAuthenticator) auth).getLogoutOnSessionTimeout())) {
      // server/12i1, 12ch
      logout(logout == Logout.TIMEOUT ? this : null);
    }

    /*
    boolean invalidateAfterListener = _manager.isInvalidateAfterListener();
    if (! invalidateAfterListener)
      _isValid = false;
    */

    try {
      // server/017s
      /*
      if (_clusterObject != null) {
	_clusterObject.remove();
	notifyDestroy();
      }
      */

      _manager.removeSession(this);

      invalidateImpl(logout);

      _isValid = false;
    } finally {
      _isValid = false;
    }
  }

  /**
   * Logs out the user
   */
  public void logout()
  {
    // server/12bw
    logout(null);
  }

  /**
   * Logs out the user
   *
   * @param session the session in case of timeout and single-signon
   */
  public void logout(SessionImpl timeoutSession)
  {
    if (_user != null) {
      if (_isValid)
        removeAttribute(LOGIN);
      Principal user = _user;
      _user = null;

      try {
	ServletAuthenticator auth = getAuthenticator();

	if (auth != null)
	  auth.logout(_manager.getWebApp(), timeoutSession, _id, user);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Invalidate the session, removing it from the manager,
   * unbinding the values, and removing it from the store.
   */
  void invalidateImpl(Logout logout)
  {
    boolean invalidateAfterListener = _manager.isInvalidateAfterListener();
    if (! invalidateAfterListener)
      _isValid = false;

    try {
      ClusterObject clusterObject = _clusterObject;
      // _clusterObject = null;

      if (clusterObject != null && _isInvalidating)
	clusterObject.remove();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    invalidateLocal();
  }

  /**
   * unbinds the session and saves if necessary.
   */
  private void invalidateLocal()
  {
    ClusterObject clusterObject = _clusterObject;
    if (_isValid && ! _isInvalidating && clusterObject != null) {
      if (_manager.isSaveOnlyOnShutdown()) {
	clusterObject.update();

	try {
	  clusterObject.store(this);
	} catch (Throwable e) {
	  log.log(Level.WARNING, "Can't serialize session", e);
	}
      }
    }

    unbind(); // we're invalidating, not passivating
  }

  /**
   * Creates a new session.
   */
  void create(long now)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine("create session " + _id);
    }

    // e.g. server 'C' when 'A' and 'B' have no record of session
    if (_isValid)
      unbind();

    _isValid = true;
    _isNew = true;
    _accessTime = now;
    _creationTime = now;

    if (_clusterObject != null)
      _clusterObject.setValid();
  }

  /**
   * Returns true if the session is in use.
   */
  public boolean inUse()
  {
    return _useCount > 0;
  }

  /**
   * Set true if the session is in use.
   */
  boolean addUse()
  {
    synchronized (this) {
      if (_isClosing)
	return false;

      _useCount++;

      return true;
    }
  }

  /**
   * Set true if the session is in use.
   */
  void endUse()
  {
    synchronized (this) {
      _useCount--;
    }
  }

  /**
   * Clears the session when reading a bad saved session.
   */
  void reset(long now)
  {
    if (log.isLoggable(Level.FINE))
      log.fine("reset session " + _id);

    unbind();
    _isValid = true;
    _isNew = true;
    _accessTime = now;
    _creationTime = now;
  }

  /**
   * Loads the session.
   */
  public boolean load()
  {
    boolean isValid;

    // server/01k0
    if (_useCount > 1)
      return true;

    ClusterObject clusterObject = _clusterObject;
    if (clusterObject != null)
      isValid = clusterObject.load(this);
    else
      isValid = true;

    return isValid;
  }

  /**
   * Passivates the session.
   */
  public void passivate()
  {
    unbind();
  }

  /**
   * Cleans up the session.
   */
  public void unbind()
  {
    if (_values.size() == 0)
      return;

    ClusterObject clusterObject = _clusterObject;

    ArrayList<String> names = new ArrayList<String>();
    ArrayList<Object> values = new ArrayList<Object>();
    
    synchronized (this) {
      //System.out.println("UNBIND: " + this + " " + clusterObject + " " + System.identityHashCode(this));

      /*
      if (_useCount > 0)
	Thread.dumpStack();
      */

      Iterator<Map.Entry<String,Object>> iter = _values.entrySet().iterator();
      while (iter.hasNext()) {
	Map.Entry<String,Object> entry = iter.next();

	names.add(entry.getKey());
	values.add(entry.getValue());
      }
      
      _values.clear();

      if (clusterObject != null)
	clusterObject.update();
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

  /*
   * Set the current access time to now.
   */
  void setAccess(long now)
  {
    // server/01k0
    if (_useCount > 1)
      return;
    
    _isNew = false;

    if (_clusterObject != null)
      _clusterObject.access();

    _accessTime = now;
  }

  /**
   * Cleaning up session stuff at the end of a request.
   *
   * <p>If the session data has changed and we have persistent sessions,
   * save the session.  However, if save-on-shutdown is true, only save
   * on a server shutdown.
   */
  public void finish()
  {
    int count;

    _accessTime = Alarm.getCurrentTime();

    synchronized (this) {
      if (_useCount > 1) {
	_useCount--;
	return;
      }
    }

    try {
      saveAfterRequest();
    } finally {
      synchronized (this) {
	_useCount--;
      }
    }
  }

  /**
   * Save changes before any flush.
   */
  public final void saveBeforeFlush()
  {
    if (_manager == null || ! _manager.isSaveBeforeFlush())
      return;

    save();
  }

  /**
   * Flush changes before the headers.
   */
  public final void saveBeforeHeaders()
  {
    if (_manager == null || ! _manager.isSaveBeforeHeaders())
      return;

    save();
  }

  /**
   * Flush changes after a request completes.
   */
  public final void saveAfterRequest()
  {
    if (_manager == null || ! _manager.isSaveAfterRequest())
      return;

    save();
  }

  /**
   * Saves changes to the session.
   */
  public final void save()
  {
    try {
      ClusterObject clusterObject = _clusterObject;
      if (clusterObject != null) {
	clusterObject.store(this);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, "Can't serialize session", e);
    }
  }

  /**
   * Store on shutdown.
   */
  void saveOnShutdown()
  {
    try {
      ClusterObject clusterObject = _clusterObject;

      if (clusterObject != null) {
	clusterObject.change();
	clusterObject.store(this);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, "Can't serialize session", e);
    }
  }

  /**
   * Loads the object from the input stream.
   */
  public void load(Hessian2Input in)
    throws IOException
  {
    HttpSessionEvent event = null;
    
    synchronized (_values) {
      unbind();

      try {
	int size = in.readInt();

	//System.out.println("LOAD: " + size + " " + this + " " + _clusterObject + System.identityHashCode(this));

	for (int i = 0; i < size; i++) {
	  String key = in.readString();
	  Object value = in.readObject();

	  if (value != null) {
	    synchronized (_values) {
	      _values.put(key, value);
	    }
	      
	    if (value instanceof HttpSessionActivationListener) {
	      HttpSessionActivationListener listener
		= (HttpSessionActivationListener) value;

	      if (event == null)
		event = new HttpSessionEvent(this);

	      listener.sessionDidActivate(event);
	    }
	  }
	}
      } catch (Exception e) {
	throw IOExceptionWrapper.create(e);
      }

      ArrayList<HttpSessionActivationListener> listeners;
      listeners = _manager.getActivationListeners();
      for (int i = 0; listeners != null && i < listeners.size(); i++) {
	HttpSessionActivationListener listener = listeners.get(i);

	if (event == null)
	  event = new HttpSessionEvent(this);
	
	listener.sessionDidActivate(event);
      }
    }
  }

  /**
   * Returns true if the session is empty.
   */
  public boolean isEmpty()
  {
    return _values == null || _values.size() == 0;
  }

  /**
   * Saves the object to the input stream.
   */
  public void store(Hessian2Output out)
    throws IOException
  {
    HttpSessionEvent event = null;
    
    synchronized (_values) {
      Set set = getEntrySet();

      int size = set == null ? 0 : set.size();

      out.writeInt(size);

      if (size == 0)
	return;

      ArrayList<HttpSessionActivationListener> listeners;
      listeners = _manager.getActivationListeners();
      for (int i = 0; listeners != null && i < listeners.size(); i++) {
	HttpSessionActivationListener listener = listeners.get(i);

	if (event == null)
	  event = new HttpSessionEvent(this);
	
	listener.sessionWillPassivate(event);
      }

      boolean ignoreNonSerializable =
	getManager().getIgnoreSerializationErrors();

      Map.Entry []entries = new Map.Entry[set.size()];
      
      Iterator iter = set.iterator();
      int i = 0;
      while (iter.hasNext()) {
	entries[i++] = (Map.Entry) iter.next();
      }

      Arrays.sort(entries, KEY_COMPARATOR);

      for (i = 0; i < entries.length; i++) {
	Map.Entry entry = entries[i];
	Object value = entry.getValue();

	out.writeString((String) entry.getKey());
	if (ignoreNonSerializable && ! (value instanceof Serializable)) {
	  out.writeObject(null);
	  continue;
	}
	      
	if (value instanceof HttpSessionActivationListener) {
	  HttpSessionActivationListener listener
	    = (HttpSessionActivationListener) value;

	  if (event == null)
	    event = new HttpSessionEvent(this);

	  listener.sessionWillPassivate(event);
	}

	try {
	  out.writeObject(value);
	} catch (NotSerializableException e) {
	  log.warning(L.l("Failed storing persistent session attribute `{0}'.  Persistent session values must extend java.io.Serializable.\n{1}", entry.getKey(), String.valueOf(e)));
	  throw e;
	}
      }
    }
  }

  /**
   * Returns the set of values in the session
   */
  Set getEntrySet()
  {
    synchronized (_values) {
      if (! _isValid)
	throw new IllegalStateException(L.l("Can't call getEntrySet() when session is no longer valid."));

      return _values.entrySet();
    }
  }

  public boolean canLog()
  {
    return log.isLoggable(Level.FINE);
  }

  public void log(String value)
  {
    log.fine(value);
  }

  public String toString()
  {
    return "SessionImpl[" + getId() + "]";
  }

  enum Logout {
    INVALIDATE,
    LRU,
    TIMEOUT
  };

  private static Comparator KEY_COMPARATOR = new Comparator() {
      public int compare(Object aObj, Object bObj)
      {
	Map.Entry a = (Map.Entry) aObj;
	Map.Entry b = (Map.Entry) bObj;

	String aStr = (String) a.getKey();
	String bStr = (String) b.getKey();

	return aStr.compareTo(bStr);
      }
    };
}
