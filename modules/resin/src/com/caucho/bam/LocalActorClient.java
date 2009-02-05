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
public class LocalActorClient implements ActorClient {
  private static final Logger log
    = Logger.getLogger(LocalActorClient.class.getName());

  private static final WeakHashMap<ClassLoader,ClientActorFactory>
    _factoryMap = new WeakHashMap<ClassLoader,ClientActorFactory>();
  
  private boolean _isFinest = log.isLoggable(Level.FINEST);

  private ActorClient _client;

  private boolean _isClosed;

  public LocalActorClient()
  {
    this(null, null);
  }

  public LocalActorClient(String uid)
  {
    this(uid, null);
  }

  public LocalActorClient(String uid, String password)
  {
    _client = getFactory().getConnection(uid, getClass().getSimpleName());
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _client.getJid();
  }

  /**
   * Sets the message handler
   */
  public void setActorStream(ActorStream stream)
  {
    _client.setActorStream(stream);
  }

  /**
   * Gets the message listener
   */
  public ActorStream getActorStream()
  {
    return _client.getActorStream();
  }

  /**
   * Returns the client stream
   */
  public ActorStream getBrokerStream()
  {
    return _client.getBrokerStream();
  }

  public void message(String to,
		      Serializable payload)
  {
    _client.message(to, payload);
  }

  //
  // RPC
  //

  public Serializable queryGet(String to,
			       Serializable payload)
  {
    return _client.queryGet(to, payload);
  }

  public void queryGet(String to,
		       Serializable payload,
		       QueryCallback callback)
  {
    _client.queryGet(to, payload, callback);
  }

  public Serializable querySet(String to,
			       Serializable payload)
  {
    return _client.querySet(to, payload);
  }

  public void querySet(String to,
		       Serializable payload,
		       QueryCallback callback)
  {
    _client.querySet(to, payload, callback);
  }

  public void presence(String to, Serializable payload)
  {
    _client.presence(to, payload);
  }

  public void presenceUnavailable(String to, Serializable payload)
  {
    _client.presenceUnavailable(to, payload);
  }

  public void presenceProbe(String to, Serializable payload)
  {
    _client.presenceProbe(to, payload);
  }

  public void presenceSubscribe(String to, Serializable payload)
  {
    _client.presenceSubscribe(to, payload);
  }

  public void presenceSubscribed(String to, Serializable payload)
  {
    _client.presenceSubscribed(to, payload);
  }

  public void presenceUnsubscribe(String to, Serializable payload)
  {
    _client.presenceUnsubscribe(to, payload);
  }

  public void presenceUnsubscribed(String to, Serializable payload)
  {
    _client.presenceUnsubscribed(to, payload);
  }

  public void presenceError(String to,
			    Serializable payload,
			    ActorError error)
  {
    _client.presenceError(to, payload, error);
  }

  public boolean isClosed()
  {
    return _client.isClosed();
  }

  public void close()
  {
    _client.close();
  }
  
  public final boolean onQueryResult(long id,
				     String to,
				     String from,
				     Serializable payload)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public final boolean onQueryError(long id,
				    String to,
				    String from,
				    Serializable payload,
				    ActorError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private ClientActorFactory getFactory()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    ClientActorFactory factory = null;

    synchronized (_factoryMap) {
      factory = _factoryMap.get(loader);

      if (factory != null)
	return factory;
    }

    try {
      String name = readFactoryClassName();

      if (name != null) {
	Class cl = Class.forName(name, false, loader);

	factory = (ClientActorFactory) cl.newInstance();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (factory == null)
      throw new IllegalStateException("Can't find a valid ActorClient");

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
      
      is = loader.getResourceAsStream("META-INF/services/com.caucho.bam.ClientActorFactory");

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
    return getClass().getSimpleName() + "[" + _client.getJid() + "]";
  }

  @Override
  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    close();
  }
}
