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

package com.caucho.hmpp.spi;

import com.caucho.hmpp.*;
import com.caucho.hmpp.spi.HmppResource;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * Configuration for a service
 */
public class AbstractHmppResource implements HmppResource
{
  private static final Logger log
    = Logger.getLogger(AbstractHmppResource.class.getName());
  
  private String _jid;

  protected void setJid(String jid)
  {
    _jid = jid;
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Looks up a sub-resource
   */
  public HmppResource lookupResource(String jid)
  {
    return null;
  }

  /**
   * Creates an outbound filter
   */
  public HmppStream getOutboundFilter(HmppStream stream)
  {
    return stream;
  }

  /**
   * Creates an inbound filter
   */
  public HmppStream getInboundFilter(HmppStream stream)
  {
    return stream;
  }

  /**
   * Called when an instance logs in
   */
  public void onLogin(String jid)
  {
  }

  /**
   * Called when an instance logs out
   */
  public void onLogout(String jid)
  {
  }
  
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendMessage to=" + to + " from=" + from
		+ logValue(value));
    }
  }
  
  public boolean sendQueryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendQuerySet id=" + id + " to=" + to
		+ " from=" + from + logValue(query));
    }
    
    return false;
  }
  
  public boolean sendQuerySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendQuerySet id=" + id + " to=" + to
		+ " from=" + from + logValue(query));
    }
    
    return false;
  }
  
  public void sendQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
  }
  
  public void sendQueryError(long id,
			   String to,
			   String from,
			   Serializable query,
			   HmppError error)
  {
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void sendPresence(String to,
			 String from,
			 Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresence to=" + to + " from=" + from
		+ logData(data));
    }
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void sendPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
  }

  /**
   * Presence probe from the server to a client
   */
  public void sendPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
  }

  /**
   * A subscription request from a client
   */
  public void sendPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
  }

  /**
   * A subscription response to a client
   */
  public void sendPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
  }

  /**
   * An unsubscription request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
  }

  /**
   * A unsubscription response to a client
   */
  public void sendPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
  }

  /**
   * An error response to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmppError error)
  {
  }

  private String logValue(Serializable value)
  {
    if (value == null)
      return "";
    else
      return " value=" + value.getClass().getSimpleName();
  }

  private String logData(Serializable []data)
  {
    if (data == null)
      return "";

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    for (int i = 0; i < data.length; i++) {
      if (i != 0)
	sb.append(", ");

      if (data[i] != null)
	sb.append(data[i].getClass().getSimpleName());
      else
	sb.append("null");
    }

    sb.append("]");

    return sb.toString();
  }

  //
  // client presence
  //

  /**
   * Client request for presence
   */
  public void onClientPresenceSubscribe(String to,
					String from,
					Serializable []data)
  {
    log.fine(this + " onClientPresenceSubscribe to=" + to + " from=" + from);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jid + "]";
  }
}
