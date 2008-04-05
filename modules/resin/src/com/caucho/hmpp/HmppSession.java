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

package com.caucho.hmpp;

import java.io.Serializable;

import com.caucho.hemp.service.*;
import com.caucho.util.*;

/**
 * Manager
 */
public interface HmppSession {
  /**
   * Returns the session's jid
   */
  public String getJid();

  //
  // lifecycle
  //
  
  /**
   * Returns true if the session is closed
   */
  public boolean isClosed();

  /**
   * Closes the session
   */
  public void close();

  //
  // message handling
  //

  /**
   * Registers the message handler
   */
  public void setMessageHandler(MessageHandler handler);

  /**
   * Sends a message
   */
  public void sendMessage(String to, Serializable value);

  //
  // query handling
  //

  /**
   * Registers the query handler
   */
  public void setQueryHandler(QueryHandler handler);

  /**
   * Queries the service
   */
  public Serializable query(String to, Serializable query);

  /**
   * Queries the service
   */
  public void queryGet(long id, String to, Serializable query);

  /**
   * Queries the service
   */
  public void querySet(long id, String to, Serializable query);

  /**
   * Sends a query response
   */
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value);

  /**
   * Sends a query response
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 HmppError error);

  //
  // presence handling
  //

  /**
   * Sets the presence listener.  The handler will process presence
   * events sent by the server and other clients
   */
  public void setPresenceHandler(PresenceHandler handler);

  /**
   * Sends the basic presence notification to the server.
   *
   * The server will send presence messages to all resources which
   * are subscribed to this client.
   *
   * The server will send presence probes to all resources which this
   * client subscribes to.
   */
  public void presence(Serializable []data);

  /**
   * Basic presence
   */
  public void presenceTo(String to, Serializable []data);

  /**
   * Sends the basic presence unavailable notification to the server.
   */
  public void presenceUnavailable(Serializable []data);

  /**
   * Sends the basic presence unavailable notification to the server.
   */
  public void presenceUnavailable(String to, Serializable []data);
}
