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

import com.caucho.bam.ActorStream;
import com.caucho.bam.ProtocolException;
import com.caucho.bam.ActorError;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
public class HmtpWriter implements ActorStream
{
  private static final Logger log
    = Logger.getLogger(HmtpWriter.class.getName());

  private String _jid;
  
  private OutputStream _os;
  private Hessian2Output _out;

  public HmtpWriter()
  {
  }

  public HmtpWriter(OutputStream os)
  {
    init(os);
  }

  public void init(OutputStream os)
  {
    _os = os;

    if (log.isLoggable(Level.FINEST)) {
      _os = new HessianDebugOutputStream(_os, log, Level.FINEST);
    }
      
    _out = new Hessian2Output(_os);
  }
  
  /**
   * The jid of the stream
   */
  public String getJid()
  {
    return _jid;
  }
  
  /**
   * The jid of the stream
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  //
  // message
  //

  /**
   * Sends a message to a given jid
   */
  public void message(String to, String from, Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " message " + value
		    + " {to:" + to + ", from:" + from + "}");
	}
	
	out.startPacket();
	out.writeInt(HmtpPacketType.MESSAGE.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a message error to a given jid
   */
  public void messageError(String to,
			   String from,
			   Serializable value,
			   ActorError error)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " messageError " + value
		    + " {to:" + to + ", from:" + from + "}");
	}
	
	out.startPacket();
	out.writeInt(HmtpPacketType.MESSAGE_ERROR.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.writeObject(error);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  //
  // query
  //

  /**
   * Low-level query get
   */
  public void queryGet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " queryGet " + value
		    + " {id: " + id + ", to:" + to + ", from:" + from + "}");
	}

	out.startPacket();
	out.writeInt(HmtpPacketType.QUERY_GET.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeLong(id);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Low-level query set
   */
  public void querySet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " querySet " + value
		    + " {id: " + id + ", to:" + to + ", from:" + from + "}");
	}
	
	out.startPacket();
	out.writeInt(HmtpPacketType.QUERY_SET.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeLong(id);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Low-level query response
   */
  public void queryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " queryResult " + value
		    + " {id: " + id + ", to:" + to + ", from:" + from + "}");
	}
	
	out.startPacket();
	out.writeInt(HmtpPacketType.QUERY_RESULT.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeLong(id);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Low-level query error
   */
  public void queryError(long id,
			 String to,
			 String from, 
			 Serializable value,
			 ActorError error)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " queryError " + error + " " + value
		    + " {id: " + id + ", to:" + to + ", from:" + from + "}");
	}
	
	out.startPacket();
	out.writeInt(HmtpPacketType.QUERY_ERROR.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeLong(id);
	out.writeObject(value);
	out.writeObject(error);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new RuntimeException(e);
    }
  }

  //
  // presence
  //

  /**
   * Sends a presence packet to the server
   */
  public void presence(String to,
		       String from,
		       Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(String to,
				  String from,
				  Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_UNAVAILABLE.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence probe packet to the server
   */
  public void presenceProbe(String to,
			    String from,
			    Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_PROBE.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceSubscribe(String to,
				String from,
				Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_SUBSCRIBE.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceSubscribed(String to,
				 String from,
				 Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_SUBSCRIBED.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceUnsubscribe(String to,
				  String from,
				  Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_UNSUBSCRIBE.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceUnsubscribed(String to,
				   String from,
				   Serializable value)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_UNSUBSCRIBED.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  /**
   * Sends a presence error packet to the server
   */
  public void presenceError(String to,
			    String from,
			    Serializable value,
			    ActorError error)
  {
    try {
      Hessian2Output out = _out;

      if (out != null) {
	out.startPacket();
	out.writeInt(HmtpPacketType.PRESENCE_ERROR.ordinal());
	out.writeString(to);
	out.writeString(from);
	out.writeObject(value);
	out.writeObject(error);
	out.endPacket();
	out.flush();
      }
    } catch (IOException e) {
      close();
      
      throw new ProtocolException(e);
    }
  }

  public void flush()
    throws IOException
  {
    Hessian2Output out = _out;

    if (out != null) {
      out.flush();
    }
  }

  public boolean isClosed()
  {
    return _out == null;
  }

  public void close()
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " close");
    
    try {
      Hessian2Output out = _out;
      _out = null;

      if (out != null)
	out.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
