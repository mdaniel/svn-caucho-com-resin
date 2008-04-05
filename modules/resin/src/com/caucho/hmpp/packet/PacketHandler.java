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

package com.caucho.hmpp.packet;

import java.io.Serializable;

import com.caucho.hmpp.HmppError;

/**
 * Low-level callback to handle packet events.  Each interface corresponds to
 * a packet class.
 */
public interface PacketHandler {
  /**
   * Handles a message
   */
  public void onMessage(String from,
			String to,
			Serializable value);
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  public void onQueryGet(String id,
			 String from,
			 String to,
			 Serializable value);
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public void onQuerySet(String id,
			 String from,
			 String to,
			 Serializable value);
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void onQueryResult(String id,
			    String from,
			    String to,
			    Serializable value);
  
  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  public void onQueryError(String id,
			   String from,
			   String to,
			   Serializable value,
			   HmppError error);
  
  /**
   * Handles a presence availability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void onPresence(String from,
			 String to,
			 Serializable []data);
  
  /**
   * Handles a presence unavailability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void onPresenceUnavailable(String from,
				    String to,
				    Serializable []data);
  
  /**
   * Handles a presence probe from another server
   */
  public void onPresenceProbe(String from,
			      String to,
			      Serializable []data);
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void onPresenceSubscribe(String from,
				  String to,
				  Serializable []data);
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void onPresenceSubscribed(String from,
				   String to,
				   Serializable []data);
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void onPresenceUnsubscribe(String from,
				    String to,
				    Serializable []data);
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void onPresenceUnsubscribed(String from,
				     String to,
				     Serializable []data);
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void onPresenceError(String from,
			      String to,
			      Serializable []data,
			      HmppError error);
}
