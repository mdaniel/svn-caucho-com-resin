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

package com.caucho.hemp.servlet;

import com.caucho.hmpp.HmppSession;
import com.caucho.hmpp.HmppBroker;
import com.caucho.hmpp.packet.PacketHandler;
import com.caucho.hmpp.packet.Packet;
import com.caucho.hmpp.HmppError;
import java.io.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.hessian.io.*;
import com.caucho.server.connection.*;
import com.caucho.vfs.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class ServerPacketHandler
  implements TcpConnectionHandler, PacketHandler
{
  private static final Logger log
    = Logger.getLogger(ServerPacketHandler.class.getName());
  
  private HmppBroker _manager;
  private HmppSession _session;

  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  private HmppServiceHandler _callbackHandler;

  ServerPacketHandler(HmppBroker manager, ReadStream rs, WriteStream ws)
  {
    _manager = manager;

    InputStream is = rs;
    OutputStream os = ws;

    if (log.isLoggable(Level.FINEST)) {
      os = new HessianDebugOutputStream(os, log, Level.FINEST);
      is = new HessianDebugInputStream(is, log, Level.FINEST);
    }
    
    _in = new Hessian2StreamingInput(is);
    _out = new Hessian2StreamingOutput(os);

    _callbackHandler = new HmppServiceHandler(this, _out);

    _session = _manager.createSession("anonymous@localhost", "test");
    _session.setMessageHandler(_callbackHandler);
    _session.setQueryHandler(_callbackHandler);
  }
  
  public boolean serviceRead(ReadStream is,
			     TcpConnectionController controller)
    throws IOException
  {
    Hessian2StreamingInput in = _in;

    if (in == null)
      return false;

    Object obj = in.readObject();
    
    Packet packet = (Packet) obj;

    if (packet == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " end of stream");
      
      controller.close();
      return false;
    }

    packet.dispatch(this);

    return true;
  }
  
  public boolean serviceWrite(WriteStream os,
			      TcpConnectionController controller)
    throws IOException
  {
    return false;
  }
  
  /**
   * Handles a message
   */
  public void onMessage(String to,
			String from,
			Serializable value)
  {
    _session.sendMessage(to, value);
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
    _session.queryGet(id, to, value);
    
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
    _session.querySet(id, to, value);
    
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
    _session.queryResult(id, to, from, value);
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
    _session.queryError(id, to, from, value, error);
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
    if (to != null)
      _session.presenceTo(to, data);
    else
      _session.presence(data);
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
    if (to != null)
      _session.presenceUnavailable(to, data);
    else
      _session.presenceUnavailable(data);
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

  public void close()
  {
    Hessian2StreamingInput in = _in;
    _in = null;
    
    Hessian2StreamingOutput out = _out;
    _out = null;

    if (in != null) {
      try { in.close(); } catch (IOException e) {}
    }

    if (out != null) {
      try { out.close(); } catch (IOException e) {}
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _session + "]";
  }
}
