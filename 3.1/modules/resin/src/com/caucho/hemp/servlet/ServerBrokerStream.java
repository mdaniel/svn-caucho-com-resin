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

import com.caucho.hmtp.HmtpConnection;
import com.caucho.hmtp.HmtpStream;
import com.caucho.hmtp.packet.Packet;
import com.caucho.hmtp.HmtpError;
import java.io.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.hemp.*;
import com.caucho.hessian.io.*;
import com.caucho.hmtp.spi.HmtpBroker;
import com.caucho.server.connection.*;
import com.caucho.vfs.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class ServerBrokerStream
  implements TcpConnectionHandler, HmtpStream
{
  private static final Logger log
    = Logger.getLogger(ServerBrokerStream.class.getName());
  
  private HmtpBroker _broker;
  private HmtpConnection _conn;
  private HmtpStream _toBroker;

  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  private ServerAgentStream _callbackHandler;
  private AuthBrokerStream _authHandler;

  private String _jid;

  ServerBrokerStream(HmtpBroker broker, ReadStream rs, WriteStream ws)
  {
    _broker = broker;

    InputStream is = rs;
    OutputStream os = ws;

    if (log.isLoggable(Level.FINEST)) {
      os = new HessianDebugOutputStream(os, log, Level.FINEST);
      is = new HessianDebugInputStream(is, log, Level.FINEST);
    }
    
    _in = new Hessian2StreamingInput(is);
    _out = new Hessian2StreamingOutput(os);

    _callbackHandler = new ServerAgentStream(this, _out);
    _authHandler = new AuthBrokerStream(this, _callbackHandler);
  }

  protected String getJid()
  {
    return _jid;
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

    if (log.isLoggable(Level.FINER))
      log.finer(this + " receive " + packet);

    if (_conn != null)
      packet.dispatch(this, _toBroker);
    else
      packet.dispatch(_authHandler, _toBroker);

    return true;
  }
  
  public boolean serviceWrite(WriteStream os,
			      TcpConnectionController controller)
    throws IOException
  {
    return false;
  }

  String login(String uid, Serializable credentials, String resource)
  {
    String password = (String) credentials;
    
    _conn = _broker.getConnection(uid, password);
    _conn.setMessageHandler(_callbackHandler);
    _conn.setQueryHandler(_callbackHandler);
    _conn.setPresenceHandler(_callbackHandler);

    _jid = _conn.getJid();
    
    _toBroker = _conn.getBrokerStream();

    return _jid;
  }
  
  /**
   * Handles a message
   */
  public void sendMessage(String to,
			  String from,
			  Serializable value)
  {
    _toBroker.sendMessage(to, _jid, value);
  }
  
  /**
   * Handles a message
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    _toBroker.sendMessageError(to, _jid, value, error);
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
    _toBroker.sendQueryGet(id, to, _jid, value);
    
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
    _toBroker.sendQuerySet(id, to, _jid, value);
    
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
    _toBroker.sendQueryResult(id, to, _jid, value);
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
    _toBroker.sendQueryError(id, to, _jid, value, error);
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
    _toBroker.sendPresence(to, _jid, data);
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
    _toBroker.sendPresenceUnavailable(to, _jid, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void sendPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
    _toBroker.sendPresenceProbe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
    _toBroker.sendPresenceSubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
    _toBroker.sendPresenceSubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
    _toBroker.sendPresenceUnsubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
    _toBroker.sendPresenceUnsubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmtpError error)
  {
    _toBroker.sendPresenceError(to, _jid, data, error);
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
    return getClass().getSimpleName() + "[" + _conn + "]";
  }
}
