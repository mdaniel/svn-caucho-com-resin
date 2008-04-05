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

package com.caucho.hemp.client;

import com.caucho.hmpp.MessageHandler;
import com.caucho.hmpp.QueryHandler;
import com.caucho.hmpp.packet.PacketHandler;
import com.caucho.hmpp.packet.Packet;
import com.caucho.hmpp.HmppError;
import com.caucho.server.connection.*;
import com.caucho.server.port.*;
import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.hessian.io.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * HeMPP client protocol
 */
class ClientPacketHandler implements Runnable, PacketHandler {
  private static final Logger log
    = Logger.getLogger(ClientPacketHandler.class.getName());

  private static long _gId;
  
  private HempClient _client;
  private ClassLoader _loader;
  
  private boolean _isFinest;

  ClientPacketHandler(HempClient client)
  {
    _client = client;
    _loader = Thread.currentThread().getContextClassLoader();
  }

  private void close()
  {
    _client.close();
  }
    
  public void run()
  {
    _isFinest = log.isLoggable(Level.FINEST);

    Thread thread = Thread.currentThread();
    String oldName = thread.getName();
      
    try {
      thread.setName("hmpp-client-" + _gId++);
      thread.setContextClassLoader(_loader);
      
      while (! _client.isClosed()) {
	readPacket();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      close();

      thread.setName(oldName);
    }
  }

  private void readPacket()
    throws IOException
  {
    int tag;

    Hessian2StreamingInput in = _client.getStreamingInput();

    if (in == null)
      return;

    Packet packet = (Packet) in.readObject();

    if (packet == null) {
      close();
      return;
    }

    if (log.isLoggable(Level.FINER))
      log.finer(this + " receive " + packet);

    packet.dispatch(this);
  }
  
  /**
   * Handles a message
   */
  public void onMessage(String to,
			String from,
			Serializable value)
  {
    MessageHandler handler = _client.getMessageHandler();

    if (handler != null)
      handler.onMessage(to, from, value);
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean onQueryGet(long id,
		  	    String to,
			    String from,
			    Serializable value)
  {
    QueryHandler handler = _client.getQueryHandler();

    if (handler == null || ! handler.onQueryGet(id, to, from, value)) {
      _client.queryError(id, from, value,
			 new HmppError("unknown",
				       "no onQueryGet handling " + value.getClass().getName()));
    }
    
    return true;
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean onQuerySet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    QueryHandler handler = _client.getQueryHandler();

    if (handler == null || ! handler.onQuerySet(id, from, to, value)) {
      _client.queryError(id, from, value,
			 new HmppError("unknown",
				       "no onQuerySet handling " + value.getClass().getName()));
    }
    
    return true;
  }
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void onQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    _client.onQueryResult(id, to, from, value);
  }
  
  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  public void onQueryError(long id,
			   String to,
			   String from,
			   Serializable value,
			   HmppError error)
  {
    _client.onQueryError(id, to, from, value, error);
  }
  
  /**
   * Handles a presence availability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void onPresence(String to,
			 String from,
			 Serializable []data)
  {
  }
  
  /**
   * Handles a presence unavailability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void onPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void onPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void onPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void onPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void onPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void onPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void onPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmppError error)
  {
  }

  @Override
  public String toString()
  {
    // XXX: should have the connection
    return getClass().getSimpleName() + "[]";
  }
}
