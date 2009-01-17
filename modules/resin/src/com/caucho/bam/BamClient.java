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

package com.caucho.bam;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * BAM client
 */
public class BamClient implements BamConnection {
  private static final Logger log
    = Logger.getLogger(BamClient.class.getName());

  private static final WeakHashMap<ClassLoader,BamConnectionFactory>
    _factoryMap = new WeakHashMap<ClassLoader,BamConnectionFactory>();
  
  private boolean _isFinest = log.isLoggable(Level.FINEST);

  private BamConnection _conn;

  private boolean _isClosed;

  public BamClient()
  {
    _conn = openConnection(null, null);
  }

  public BamClient(String uid)
  {
    _conn = openConnection(uid, null);
  }

  public BamClient(String uid, String password)
  {
    _conn = openConnection(uid, password);
  }

  private BamConnection openConnection(String uid,
				       String password)
  {
    BamConnectionFactory factory = getFactory();

    return factory.getConnection(uid, password, "BamClient");
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _conn.getJid();
  }

  /**
   * Sets the message handler
   */
  public void setAgentStream(BamStream handler)
  {
    _conn.setAgentStream(handler);
  }

  /**
   * Gets the message listener
   */
  public BamStream getAgentStream()
  {
    return _conn.getAgentStream();
  }

  /**
   * Returns the client stream
   */
  public BamStream getBrokerStream()
  {
    return _conn.getBrokerStream();
  }

  /**
   * Sends a message to a given jid
   */
  public void message(String to, Serializable value)
  {
    _conn.message(to, value);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(Serializable data)
  {
    _conn.presence(data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(String to, Serializable data)
  {
    _conn.presence(to, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(Serializable data)
  {
    _conn.presenceUnavailable(data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(String to, Serializable data)
  {
    _conn.presenceUnavailable(to, data);
  }

  /**
   * Sends a presence probe packet to the server
   */
  public void presenceProbe(String to, Serializable data)
  {
    _conn.presenceProbe(to, data);
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceSubscribe(String to, Serializable data)
  {
    _conn.presenceSubscribe(to, data);
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceSubscribed(String to, Serializable data)
  {
    _conn.presenceSubscribed(to, data);
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceUnsubscribe(String to, Serializable data)
  {
    _conn.presenceUnsubscribe(to, data);
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceUnsubscribed(String to, Serializable data)
  {
    _conn.presenceUnsubscribed(to, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceError(String to, Serializable data, BamError error)
  {
    _conn.presenceError(to, data, error);
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable queryGet(String to,
			       Serializable query)
  {
    return _conn.queryGet(to, query);
  }

  /**
   * Sends a query-get packet to the server
   */
  public void queryGet(String to,
		       Serializable value,
		       BamQueryCallback callback)
  {
    _conn.queryGet(to, value, callback);
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable querySet(String to,
			       Serializable query)
  {
    return _conn.querySet(to, query);
  }

  /**
   * Sends a query-set packet to the server
   */
  public void querySet(String to,
		       Serializable value,
		       BamQueryCallback callback)
  {
    _conn.querySet(to, value, callback);
  }

  public boolean isClosed()
  {
    return _conn.isClosed();
  }

  public void close()
  {
    _conn.close();
  }

  private BamConnectionFactory getFactory()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    BamConnectionFactory factory = null;

    synchronized (_factoryMap) {
      factory = _factoryMap.get(loader);

      if (factory != null)
	return factory;
    }

    try {
      String name = readFactoryClassName();

      if (name != null) {
	Class cl = Class.forName(name, false, loader);

	factory = (BamConnectionFactory) cl.newInstance();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (factory == null)
      throw new IllegalStateException("Can't find a valid BamConnectionFactory");

    synchronized (_factoryMap) {
      _factoryMap.put(loader, factory);
    }
    
    return factory;
  }

  private String readFactoryClassName()
  {
    InputStream is = null;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      is = loader.getResourceAsStream("META-INF/services/com.caucho.bam.BamConnectionFactory");

      StringBuilder sb = new StringBuilder();
      int ch;

      while ((ch = is.read()) >= 0) {
	if (ch == '\r' || ch == '\n') {
	  String line = sb.toString();

	  int p = line.indexOf('#');
	  if (p > 0)
	    line = line.substring(0, p);

	  line = line.trim();

	  if (line.length() > 0)
	    return line;

	  sb = new StringBuilder();
	}
	else
	  sb.append((char) ch);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (IOException e) {
      }
    }

    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn.getJid() + "]";
  }

  @Override
  protected void finalize()
  {
    close();
  }
}
