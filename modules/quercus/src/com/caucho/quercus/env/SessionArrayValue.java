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

package com.caucho.quercus.env;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;

import java.util.logging.*;

import javax.servlet.http.HttpServletRequest;

import com.caucho.log.Log;

import com.caucho.util.Alarm;
import com.caucho.util.CacheListener;

import com.caucho.vfs.WriteStream;

import com.caucho.server.cluster.ClusterObject;

import com.caucho.quercus.lib.UnserializeReader;

import com.caucho.quercus.QuercusModuleException;

/**
 * Represents the $_SESSION
 */
public class SessionArrayValue extends ArrayValueWrapper 
  implements CacheListener {
  static protected final Logger log = Log.open(SessionArrayValue.class);

  private String _id;
  private transient Env _env;

  private ClusterObject _clusterObject;
  
  private int _useCount;

  private long _accessTime;
  private long _maxInactiveInterval;

  private boolean _isValid;

  public SessionArrayValue(Env env, String id, long now, 
                           long maxInactiveInterval)
  {
    this(env, id, now, maxInactiveInterval, new ArrayValueImpl());
  }
  
  public SessionArrayValue(Env env, String id, long now,
                           long maxInactiveInterval, ArrayValue array)
  {
    super(array);
    
    _env = env;
    _id = id;
    _accessTime = now;
    _maxInactiveInterval = maxInactiveInterval;
  }

  /**
   * Returns the session id.
   */
  public String getId()
  {
    return _id;
  }
  
  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    long accessTime = _accessTime;

    SessionArrayValue theCopy = 
      new SessionArrayValue(env, _id, accessTime, _maxInactiveInterval,
                            (ArrayValue) getArray().copy(env, map));

    theCopy.setClusterObject(_clusterObject);

    return theCopy;
  }

  /**
   * Encoding for serialization.
   */
  public String encode()
  {
    StringBuilder sb = new StringBuilder();
    ArrayValue array = getArray();

    synchronized (array) {
      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        sb.append(entry.getKey().toString());
        sb.append("|");
        entry.getValue().serialize(sb);
      }
    }

    return sb.toString();
  }

  /**
   * Decodes encoded values, adding them to this object.
   */
  public boolean decode(Env env, String encoded)
  {
    ArrayValue array = getArray();

    try {
      UnserializeReader is = new UnserializeReader(encoded);

      StringBuilder sb = new StringBuilder();

      synchronized (array) {
        while (true) {
          int ch;

          sb.setLength(0);

          while ((ch = is.read()) > 0 && ch != '|') {
            sb.append((char) ch);
          }

          if (sb.length() == 0)
            return true;

          String key = sb.toString();

          array.put(new StringValueImpl(key), is.unserialize(env));
        }
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public synchronized boolean inUse()
  {
    return _useCount > 0;
  }

  public synchronized void addUse()
  {
    _useCount++;
  }

  public boolean load()
  {
    if (_clusterObject != null)
      return _clusterObject.load(this);
    else
      return true;
  }

  /**
   * Saves the object to the output stream.
   */
  public void store(ObjectOutputStream out)
    throws IOException
  {
    out.writeObject(encode());
  }

  public void load(ObjectInputStream in)
    throws IOException
  {
    try {
      String encoded = in.readObject().toString();

      decode(_env, encoded);
    } catch (ClassNotFoundException e) {
      log.log(Level.WARNING, "Can't deserialize session", e);
    }
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

    synchronized (this) {
      count = --_useCount;
    }

    if (count > 0)
      return;

    if (count < 0)
      throw new IllegalStateException();

    try {
      ClusterObject clusterObject = _clusterObject;

      if (clusterObject != null) {
        // make sure the object always saves - PHP references can make changes
        // without directly calling on the session object
        clusterObject.change(); 

        clusterObject.store(this);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, "Can't serialize session", e);
    }
  }

  /**
   * Store on shutdown.
   */
  public void storeOnShutdown()
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

  public long getMaxInactiveInterval()
  {
    return _maxInactiveInterval;
  }

  public void setClusterObject(ClusterObject clusterObject)
  {
    _clusterObject = clusterObject;
  }

  public void reset(long now)
  {
    setValid(true);
    setAccess(now);
    clear();
  }

  public synchronized long getAccessTime()
  {
    return _accessTime;
  }
  
  public synchronized void setAccess(long now)
  {
    _accessTime = now;
  }

  public synchronized boolean isValid()
  {
    return _isValid;
  }

  public synchronized void setValid(boolean isValid)
  {
    _isValid = isValid;
  }

  /**
   * Invalidates the session.
   */
  public void invalidate()
  {
    if (! _isValid)
      throw new IllegalStateException(L.l("Can't call invalidate() when session is no longer valid."));

    try {
      ClusterObject clusterObject = _clusterObject;
      _clusterObject = null;

      if (clusterObject != null)
        clusterObject.remove();

      clear();
 
      _isValid = false;
    } finally {
      _isValid = false;
    }
  }
  
  public boolean isEmpty()
  {
    return getSize() == 0;
  }

  /**
   * Callback when the session is removed from the session cache, generally
   * because the session cache is full.
   */
  public void removeEvent()
  {
    boolean isValid = _isValid;

    if (log.isLoggable(Level.FINE))
      log.fine("remove session " + _id);

    long now = Alarm.getCurrentTime();

    ClusterObject clusterObject = _clusterObject;

    if (_isValid && clusterObject != null) {
      try {
        clusterObject.update();
        clusterObject.store(this);
      } catch (Throwable e) {
        log.log(Level.WARNING, "Can't serialize session", e);
      }
    }
  }
}
