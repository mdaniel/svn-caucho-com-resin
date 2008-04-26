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

package com.caucho.hmtp.client;

import com.caucho.hmtp.QueryStream;
import com.caucho.hmtp.PresenceStream;
import com.caucho.hmtp.MessageStream;
import com.caucho.hmtp.HmtpStream;
import com.caucho.hmtp.packet.Packet;
import com.caucho.hmtp.HmtpError;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * HMTP client protocol
 */
class ClientAgentStream implements Runnable, HmtpStream {
  private static final Logger log
    = Logger.getLogger(ClientAgentStream.class.getName());

  private static long _gId;
  
  private HmtpClient _client;
  private HmtpStream _clientStream;
  private ClassLoader _loader;
  
  private boolean _isFinest;

  ClientAgentStream(HmtpClient client)
  {
    _client = client;
    _clientStream = client.getBrokerStream();
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

    packet.dispatch(this, _clientStream);
  }
  
  /**
   * Handles a message
   */
  public void sendMessage(String to,
			String from,
			Serializable value)
  {
    MessageStream handler = _client.getMessageHandler();

    if (handler != null)
      handler.sendMessage(to, from, value);
  }
  
  /**
   * Handles a message
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    MessageStream handler = _client.getMessageHandler();

    if (handler != null)
      handler.sendMessageError(to, from, value, error);
    else {
      if (log.isLoggable(Level.FINER))
	log.finer(this + " sendMessageError to=" + to + " from=" + from
		  + " error=" + error);
    }
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean sendQueryGet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    QueryStream handler = _client.getQueryHandler();

    if (handler == null || ! handler.sendQueryGet(id, to, from, value)) {
      String msg = "no sendQueryGet handling " + value.getClass().getName();
      HmtpError error = new HmtpError("unknown", msg);
      
      _clientStream.sendQueryError(id, from, to, value, error);
    }
    
    return true;
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean sendQuerySet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    QueryStream handler = _client.getQueryHandler();

    if (handler == null || ! handler.sendQuerySet(id, to, from, value)) {
      String msg = "no sendQuerySet handling " + value.getClass().getName();
      HmtpError error = new HmtpError("unknown", msg);
      
      _clientStream.sendQueryError(id, from, to, value, error);
    }
    
    return true;
  }
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void sendQueryResult(long id,
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
  public void sendQueryError(long id,
			   String to,
			   String from,
			   Serializable value,
			   HmtpError error)
  {
    _client.onQueryError(id, to, from, value, error);
  }
  
  /**
   * Handles a presence availability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void sendPresence(String to,
			 String from,
			 Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresence(to, from, data);
  }
  
  /**
   * Handles a presence unavailability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void sendPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceUnavailable(to, from, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void sendPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceProbe(to, from, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void sendPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceSubscribe(to, from, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void sendPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceSubscribed(to, from, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceUnsubscribe(to, from, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceUnsubscribed(to, from, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmtpError error)
  {
    PresenceStream handler = _client.getPresenceHandler();

    if (handler != null)
      handler.sendPresenceError(to, from, data, error);
  }

  @Override
  public String toString()
  {
    // XXX: should have the connection
    return getClass().getSimpleName() + "[]";
  }
}
