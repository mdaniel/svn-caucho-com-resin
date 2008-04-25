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

import java.io.Serializable;
import java.util.*;

/**
 * Configuration for a service
 */
public class AbstractHmtpFilter implements HmtpStream
{
  private final HmtpStream _next;

  protected AbstractHmtpFilter(HmtpStream next)
  {
    _next = next;
  }

  protected HmtpStream getNext()
  {
    return _next;
  }
  
  /**
   * Sends a unidirectional message
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    _next.sendMessage(to, from, value);
  }
  
  /**
   * Sends a unidirectional message error
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    _next.sendMessageError(to, from, value, error);
  }
  
  public boolean sendQueryGet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    return _next.sendQueryGet(id, to, from, query);
  }
  
  public boolean sendQuerySet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    return _next.sendQuerySet(id, to, from, query);
  }
  
  public void sendQueryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    _next.sendQueryResult(id, to, from, value);
  }
  
  public void sendQueryError(long id,
			     String to,
			     String from,
			     Serializable query,
			     HmtpError error)
  {
    _next.sendQueryError(id, to, from, query, error);
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void sendPresence(String to,
			   String from,
			   Serializable []data)
  {
    _next.sendPresence(to, from, data);
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable []data)
  {
    _next.sendPresenceUnavailable(to, from, data);
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
    _next.sendPresenceSubscribe(to, from, data);
  }

  /**
   * A subscription response to a client
   */
  public void sendPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    _next.sendPresenceSubscribed(to, from, data);
  }

  /**
   * An unsubscription request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    _next.sendPresenceUnsubscribe(to, from, data);
  }

  /**
   * A unsubscription response to a client
   */
  public void sendPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    _next.sendPresenceUnsubscribed(to, from, data);
  }

  /**
   * An error response to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmtpError error)
  {
    _next.sendPresenceError(to, from, data, error);
  }
}
