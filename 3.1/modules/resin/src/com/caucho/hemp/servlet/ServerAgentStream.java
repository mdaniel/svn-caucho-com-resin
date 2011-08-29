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
import com.caucho.hmtp.HmtpError;
import java.io.*;
import java.util.logging.*;

import com.caucho.hessian.io.*;

/**
 * Handles callbacks for a hmpp service
 */
public class ServerAgentStream implements HmtpStream
{
  private static final Logger log
    = Logger.getLogger(ServerAgentStream.class.getName());

  private ServerBrokerStream _packetHandler;
  private Hessian2StreamingOutput _out;

  ServerAgentStream(ServerBrokerStream packetHandler,
		     Hessian2StreamingOutput out)
  {
    _packetHandler = packetHandler;
    _out = out;
  }
  
  public void sendMessage(String to, String from, Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send message to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new Message(to, from, value));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send error message to=" + to
		  + " from=" + from + " error=" + error);
      }
      
      _out.writeObject(new MessageError(to, from, value, error));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public boolean sendQueryGet(long id,
		              String to,
		              String from,
		              Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " queryGet to=" + to
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
  
  public boolean sendQuerySet(long id,
		              String to,
		              String from,
		              Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " querySet to=" + to
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
  
  public void sendQueryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " queryResult id=" + id + " to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryResult(id, to, from, value));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void sendQueryError(long id,
			     String to,
			     String from,
			     Serializable query,
			     HmtpError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send " + error + " to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryError(id, to, from, query, error));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  /**
   * General presence, for clients announcing availability
   */
  public void sendPresence(String to,
		           String from,
		           Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presence to=" + to
		  + " from=" + from);
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
  public void sendPresenceUnavailable(String to,
			  	      String from,
				      Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceUnavailable to=" + to
		  + " from=" + from);
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
  public void sendPresenceProbe(String to,
			        String from,
			        Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceProbe to=" + to
		  + " from=" + from);
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
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceSubscribe to=" + to
		  + " from=" + from);
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
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceSubscribed to=" + to
		  + " from=" + from);
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
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceUnsubscribe to=" + to
		  + " from=" + from);
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
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceUnsubscribed to=" + to
		  + " from=" + from);
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
  public void sendPresenceError(String to,
			        String from,
			        Serializable []data,
			        HmtpError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send presenceError to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new PresenceError(to, from, data, error));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
}
