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

package com.caucho.hmtp;

import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * HMTP client protocol
 */
class ClientAgentStream implements Runnable, BamStream {
  private static final Logger log
    = Logger.getLogger(ClientAgentStream.class.getName());

  private static long _gId;
  
  private HmtpClient _client;
  private BamStream _clientStream;
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

    Hessian2Input hIn = in.startPacket();

    if (hIn == null) {
      close();
      return;
    }

    int type = hIn.readInt();
    String to = hIn.readString();
    String from = hIn.readString();

    System.out.println("CL: " + HmtpPacketType.TYPES[type] + " " + to + " " + from);

    switch (HmtpPacketType.TYPES[type]) {
    case QUERY_RESULT:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	_clientStream.queryResult(id, to, from, value);
	break;
      }
      
    case QUERY_ERROR:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	BamError error = (BamError) hIn.readObject();
	in.endPacket();

	_clientStream.queryError(id, to, from, value, error);
	break;
      }
    }
  }
  
  /**
   * Returns the jid of the client
   */
  public String getJid()
  {
    return _client.getJid();
  }
  
  /**
   * Handles a message
   */
  public void message(String to,
			String from,
			Serializable value)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.message(to, from, value);
  }
  
  /**
   * Handles a message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.messageError(to, from, value, error);
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
  public boolean queryGet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler == null || ! handler.queryGet(id, to, from, value)) {
      String msg = "no queryGet handling " + value.getClass().getName();
      BamError error = new BamError("unknown", msg);
      
      _clientStream.queryError(id, from, to, value, error);
    }
    
    return true;
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean querySet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler == null || ! handler.querySet(id, to, from, value)) {
      String msg = "no querySet handling " + value.getClass().getName();
      BamError error = new BamError("unknown", msg);
      
      _clientStream.queryError(id, from, to, value, error);
    }
    
    return true;
  }
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void queryResult(long id,
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
  public void queryError(long id,
			   String to,
			   String from,
			   Serializable value,
			   BamError error)
  {
    _client.onQueryError(id, to, from, value, error);
  }
  
  /**
   * Handles a presence availability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void presence(String to,
			 String from,
			 Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presence(to, from, data);
  }
  
  /**
   * Handles a presence unavailability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void presenceUnavailable(String to,
				    String from,
				    Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceUnavailable(to, from, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void presenceProbe(String to,
			      String from,
			      Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceProbe(to, from, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void presenceSubscribe(String to,
				  String from,
				  Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceSubscribe(to, from, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void presenceSubscribed(String to,
				   String from,
				   Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceSubscribed(to, from, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void presenceUnsubscribe(String to,
				    String from,
				    Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceUnsubscribe(to, from, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void presenceUnsubscribed(String to,
				     String from,
				     Serializable data)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceUnsubscribed(to, from, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void presenceError(String to,
			      String from,
			      Serializable data,
			      BamError error)
  {
    BamStream handler = _client.getStreamHandler();

    if (handler != null)
      handler.presenceError(to, from, data, error);
  }

  @Override
  public String toString()
  {
    // XXX: should have the connection
    return getClass().getSimpleName() + "[]";
  }
}
