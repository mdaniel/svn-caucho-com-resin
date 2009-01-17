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

import com.caucho.bam.BamConnection;
import com.caucho.bam.BamStream;
import com.caucho.hmtp.Packet;
import com.caucho.hmtp.HmtpPacketType;
import com.caucho.bam.BamError;
import java.io.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.hemp.*;
import com.caucho.hessian.io.*;
import com.caucho.bam.BamBroker;
import com.caucho.server.connection.*;
import com.caucho.vfs.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class ServerBrokerStream
  implements TcpDuplexHandler, BamStream
{
  private static final Logger log
    = Logger.getLogger(ServerBrokerStream.class.getName());
  
  private BamBroker _broker;
  private BamConnection _conn;
  private BamStream _toBroker;

  private Hessian2StreamingInput _in;
  private Hessian2Output _out;

  private ServerAgentStream _agentStream;
  private LinkService _linkService;
  private AuthBrokerStream _authHandler;

  private String _jid;

  public ServerBrokerStream(BamBroker broker, ReadStream rs, WriteStream ws)
  {
    _broker = broker;

    InputStream is = rs;
    OutputStream os = ws;

    if (log.isLoggable(Level.FINEST)) {
      os = new HessianDebugOutputStream(os, log, Level.FINEST);
      is = new HessianDebugInputStream(is, log, Level.FINEST);
    }
    
    _in = new Hessian2StreamingInput(is);
    _out = new Hessian2Output(os);

    _agentStream = new ServerAgentStream(this, _out);
    _authHandler = new AuthBrokerStream(this, _agentStream);
    _linkService = new LinkService(this, _agentStream);
  }

  public String getJid()
  {
    return _jid;
  }
  
  public boolean serviceRead(ReadStream is,
			     TcpDuplexController controller)
    throws IOException
  {
    Hessian2StreamingInput in = _in;

    if (in == null)
      return false;

    Hessian2Input hIn = in.startPacket();

    if (hIn == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " end of stream");
      
      controller.close();
      return false;
    }

    int type = hIn.readInt();
    String to = hIn.readString();
    String from = hIn.readString();

    BamStream stream;
    if (to == null)
      stream = _linkService.getAgentStream();
    else if (_conn != null)
      stream = _toBroker;
    else
      stream = _authHandler;

    switch (HmtpPacketType.TYPES[type]) {
    case MESSAGE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	stream.message(to, from, value);

	break;
      }
      
    case MESSAGE_ERROR:
      {
	Serializable value = (Serializable) hIn.readObject();
	BamError error = (BamError) hIn.readObject();
	in.endPacket();

	stream.messageError(to, from, value, error);

	break;
      }
      
    case QUERY_GET:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	stream.queryGet(id, to, from, value);

	break;
      }
      
    case QUERY_SET:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	stream.querySet(id, to, from, value);

	break;
      }
      
    case QUERY_RESULT:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	stream.queryResult(id, to, from, value);

	break;
      }
      
    case QUERY_ERROR:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	BamError error = (BamError) hIn.readObject();
	in.endPacket();

	stream.queryError(id, to, from, value, error);

	break;
      }
    }

    return true;
  }
  
  public boolean serviceWrite(WriteStream os,
			      TcpDuplexController controller)
    throws IOException
  {
    return false;
  }

  String login(String uid, Serializable credentials, String resource)
  {
    String password = (String) credentials;
    
    _conn = _broker.getConnection(uid, password);
    _conn.setStreamHandler(_agentStream);

    _jid = _conn.getJid();
    
    _toBroker = _conn.getBrokerStream();

    return _jid;
  }
  
  /**
   * Handles a message
   */
  public void message(String to,
			  String from,
			  Serializable value)
  {
    _toBroker.message(to, _jid, value);
  }
  
  /**
   * Handles a message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    _toBroker.messageError(to, _jid, value, error);
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
    _toBroker.queryGet(id, to, _jid, value);
    
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
    _toBroker.querySet(id, to, _jid, value);
    
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
    _toBroker.queryResult(id, to, _jid, value);
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
    _toBroker.queryError(id, to, _jid, value, error);
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
    _toBroker.presence(to, _jid, data);
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
    _toBroker.presenceUnavailable(to, _jid, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void presenceProbe(String to,
			      String from,
			      Serializable data)
  {
    _toBroker.presenceProbe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    _toBroker.presenceSubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    _toBroker.presenceSubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void presenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    _toBroker.presenceUnsubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void presenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    _toBroker.presenceUnsubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void presenceError(String to,
			      String from,
			      Serializable data,
			      BamError error)
  {
    _toBroker.presenceError(to, _jid, data, error);
  }

  public void close()
  {
    Hessian2StreamingInput in = _in;
    _in = null;
    
    Hessian2Output out = _out;
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
