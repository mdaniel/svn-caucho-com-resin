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

import com.caucho.hmtp.packet.QuerySet;
import com.caucho.hmtp.packet.QueryResult;
import com.caucho.hmtp.packet.QueryGet;
import com.caucho.hmtp.packet.QueryError;
import com.caucho.hmtp.packet.PresenceUnsubscribed;
import com.caucho.hmtp.packet.PresenceUnsubscribe;
import com.caucho.hmtp.packet.PresenceUnavailable;
import com.caucho.hmtp.packet.PresenceSubscribed;
import com.caucho.hmtp.packet.PresenceSubscribe;
import com.caucho.hmtp.packet.PresenceProbe;
import com.caucho.hmtp.packet.PresenceError;
import com.caucho.hmtp.packet.Presence;
import com.caucho.hmtp.packet.MessageError;
import com.caucho.hmtp.packet.Message;
import com.caucho.hmtp.HmtpStream;
import com.caucho.hmtp.HmtpProtocolException;
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
public class ClientBrokerStream implements HmtpStream {
  private static final Logger log
    = Logger.getLogger(ClientBrokerStream.class.getName());

  private InputStream _is;
  private OutputStream _os;

  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  private boolean _isFinest;

  public ClientBrokerStream(InputStream is, OutputStream os)
  {
    _is = is;
    _os = os;

    _isFinest = log.isLoggable(Level.FINEST);

    if (log.isLoggable(Level.FINEST)) {
      _os = new HessianDebugOutputStream(_os, log, Level.FINEST);
      _is = new HessianDebugInputStream(_is, log, Level.FINEST);
    }
      
    _out = new Hessian2StreamingOutput(_os);
    _in = new Hessian2StreamingInput(_is);
  }

  Hessian2StreamingInput getStreamingInput()
  {
    return _in;
  }

  //
  // message
  //

  /**
   * Sends a message to a given jid
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new Message(to, from, value));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a message error to a given jid
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new MessageError(to, from, value, error));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  //
  // query
  //

  /**
   * Low-level query get
   */
  public boolean sendQueryGet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new QueryGet(id, to, from, value));
	out.flush();
      }

      return true;
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Low-level query set
   */
  public boolean sendQuerySet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new QuerySet(id, to, from, value));
	out.flush();
      }

      return true;
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Low-level query response
   */
  public void sendQueryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new QueryResult(id, to, from, value));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Low-level query error
   */
  public void sendQueryError(long id,
			     String to,
			     String from, 
			     Serializable value,
			     HmtpError error)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new QueryError(id, to, from, value, error));
	out.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  //
  // presence
  //

  /**
   * Sends a presence packet to the server
   */
  public void sendPresence(String to,
			   String from,
			   Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new Presence(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence packet to the server
   */
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new PresenceUnavailable(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence probe packet to the server
   */
  public void sendPresenceProbe(String to,
				String from,
				Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new PresenceProbe(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new PresenceSubscribe(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new PresenceSubscribed(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new PresenceUnsubscribe(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
    try {
      Hessian2StreamingOutput out = _out;
    
      if (out != null) {
	out.writeObject(new PresenceUnsubscribed(to, from, data));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  /**
   * Sends a presence error packet to the server
   */
  public void sendPresenceError(String to,
				String from,
				Serializable []data,
				HmtpError error)
  {
    try {
      Hessian2StreamingOutput out = _out;
    
      if (out != null) {
	out.writeObject(new PresenceError(to, from, data, error));
	out.flush();
      }
    } catch (IOException e) {
      throw new HmtpProtocolException(e);
    }
  }

  public void flush()
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.flush();
    }
  }

  public boolean isClosed()
  {
    return _is == null;
  }

  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");
    
    try {
      InputStream is;
      OutputStream os;
      
      synchronized (this) {
	is = _is;
	_is = null;

	_in = null;
	
	os = _os;
	_os = null;

	_out = null;
      }

      if (os != null) {
	try { os.close(); } catch (IOException e) {}
      }

      if (is != null) {
	is.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
