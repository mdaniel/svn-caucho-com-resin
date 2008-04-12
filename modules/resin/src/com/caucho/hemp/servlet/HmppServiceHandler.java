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

import com.caucho.hmpp.packet.*;
import com.caucho.hmpp.HmppError;
import java.io.*;
import java.util.logging.*;

import com.caucho.hessian.io.*;
import com.caucho.hmpp.*;
import com.caucho.hmpp.spi.*;

/**
 * Handles callbacks for a hmpp service
 */
public class HmppServiceHandler implements HmppServer, PacketHandler
{
  private static final Logger log
    = Logger.getLogger(HmppServiceHandler.class.getName());

  private ServerPacketHandler _packetHandler;
  private Hessian2StreamingOutput _out;

  HmppServiceHandler(ServerPacketHandler packetHandler,
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
  
  public void queryGet(long id,
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
  }
  
  public void querySet(long id,
		       String to,
		       String from,
		       Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " querySet to=" + to
		  + " from=" + from);
      }
      Thread.dumpStack();
      
      _out.writeObject(new QuerySet(id, to, from, query));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " queryResult to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryResult(id, to, from, value));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 HmppError error)
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
  public void presence(String to,
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
  public void presenceUnavailable(String to,
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
  public void presenceProbe(String to,
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
  public void presenceSubscribe(String to,
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
  public void presenceSubscribed(String to,
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
  public void presenceUnsubscribe(String to,
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
  public void presenceUnsubscribed(String to,
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
  public void presenceError(String to,
			    String from,
			    Serializable []data,
			    HmppError error)
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
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void onMessage(String to, String from, Serializable value)
  {
    sendMessage(to, from, value);
  }
  
  public boolean onQueryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    queryGet(id, to, from, query);
    
    return true;
  }
  
  public boolean onQuerySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    querySet(id, to, from, query);
    
    return true;
  }
  
  public void onQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    queryResult(id, to, from, value);
  }
  
  public void onQueryError(long id,
			   String to,
			   String from,
			   Serializable query,
			   HmppError error)
  {
    queryError(id, to, from, query, error);
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void onPresence(String to,
			 String from,
			 Serializable []data)
  {
    presence(to, from, data);
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void onPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
    presenceUnavailable(to, from, data);
  }

  /**
   * Presence probe from the server to a client
   */
  public void onPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
    presenceProbe(to, from, data);
  }

  /**
   * A subscription request from a client
   */
  public void onPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
    presenceSubscribe(to, from, data);
  }

  /**
   * A subscription response to a client
   */
  public void onPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    presenceSubscribed(to, from, data);
  }

  /**
   * An unsubscription request from a client
   */
  public void onPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    presenceUnsubscribe(to, from, data);
  }

  /**
   * A unsubscription response to a client
   */
  public void onPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    presenceUnsubscribed(to, from, data);
  }

  /**
   * An error response to a client
   */
  public void onPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmppError error)
  {
    presenceError(to, from, data, error);
  }
}

