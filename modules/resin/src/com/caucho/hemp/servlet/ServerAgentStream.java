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

import com.caucho.hmtp.QuerySet;
import com.caucho.hmtp.QueryResult;
import com.caucho.hmtp.QueryGet;
import com.caucho.hmtp.QueryError;
import com.caucho.hmtp.PresenceUnsubscribed;
import com.caucho.hmtp.PresenceUnsubscribe;
import com.caucho.hmtp.PresenceUnavailable;
import com.caucho.hmtp.PresenceSubscribed;
import com.caucho.hmtp.PresenceSubscribe;
import com.caucho.hmtp.PresenceProbe;
import com.caucho.hmtp.PresenceError;
import com.caucho.hmtp.Presence;
import com.caucho.hmtp.MessageError;
import com.caucho.hmtp.Message;
import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;
import com.caucho.bam.hmtp.HmtpPacketType;
import java.io.*;
import java.util.logging.*;

import com.caucho.hessian.io.*;

/**
 * Handles callbacks for a hmtp service
 */
public class ServerAgentStream implements BamStream
{
  private static final Logger log
    = Logger.getLogger(ServerAgentStream.class.getName());

  private ServerBrokerStream _packetHandler;
  private Hessian2Output _out;

  ServerAgentStream(ServerBrokerStream packetHandler,
		     Hessian2Output out)
  {
    _packetHandler = packetHandler;
    _out = out;
  }
  
  /**
   * Returns the jid of the target stream.
   */
  public String getJid()
  {
    return _packetHandler.getJid();
  }
  
  public void message(String to, String from, Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " message " + value
		  + " {to:" + to + ", from:" + from + "}");
      }

      _out.startPacket();
      _out.writeInt(HmtpPacketType.MESSAGE.ordinal());
      _out.writeString(to);
      _out.writeString(from);
      _out.writeObject(value);
      _out.endPacket();
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void messageError(String to,
			   String from,
			   Serializable value,
			   BamError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " messageError " + error + " " + value
		  + " {to:" + to + ", from:" + from + "}");
      }

      _out.startPacket();
      _out.writeInt(HmtpPacketType.MESSAGE_ERROR.ordinal());
      _out.writeString(to);
      _out.writeString(from);
      _out.writeObject(value);
      _out.writeObject(error);
      _out.endPacket();
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public boolean queryGet(long id,
		              String to,
		              String from,
		              Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " queryGet to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryGet(id, to, from, query));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
    
    return true;
  }
  
  public boolean querySet(long id,
		              String to,
		              String from,
		              Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " querySet to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QuerySet(id, to, from, query));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
    
    return true;
  }
  
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " queryResult " + value
		  + " {id:" + id + ", to:" + to + ", from:" + from + "}");
      }

      _out.startPacket();
      _out.writeInt(HmtpPacketType.QUERY_RESULT.ordinal());
      _out.writeString(to);
      _out.writeString(from);
      _out.writeLong(id);
      _out.writeObject(value);
      _out.endPacket();
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void queryError(long id,
			     String to,
			     String from,
			     Serializable value,
			     BamError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " queryError " + error + " " + value
		  + " {id:" + id + ", to:" + to + ", from:" + from + "}");
      }

      _out.startPacket();
      _out.writeInt(HmtpPacketType.QUERY_ERROR.ordinal());
      _out.writeString(to);
      _out.writeString(from);
      _out.writeLong(id);
      _out.writeObject(value);
      _out.writeObject(error);
      _out.endPacket();
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  /**
   * General presence, for clients announcing availability
   */
  public void presence(String to,
		           String from,
		           Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presence to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new Presence(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void presenceUnavailable(String to,
			  	      String from,
				      Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceUnavailable to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceUnavailable(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Presence probe from the server to a client
   */
  public void presenceProbe(String to,
			        String from,
			        Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceProbe to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceProbe(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * A subscription request from a client
   */
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceSubscribe to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceSubscribe(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * A subscription response to a client
   */
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceSubscribed to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceSubscribed(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * An unsubscription request from a client
   */
  public void presenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceUnsubscribe to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceUnsubscribe(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * A unsubscription response to a client
   */
  public void presenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceUnsubscribed to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceUnsubscribed(to, from, data));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * An error response to a client
   */
  public void presenceError(String to,
			        String from,
			        Serializable data,
			        BamError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceError to=" + to
		  + " from=" + from + " value=" + data);
      }
      
      _out.writeObject(new PresenceError(to, from, data, error));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _packetHandler + "]";
  }
}
