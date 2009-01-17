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

package com.caucho.bam;

import java.io.Serializable;

import com.caucho.util.*;

/**
 * BamConnection is a client connection to an agent
 */
public interface BamConnection {
  /**
   * Returns the agent's jid
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
  // handlers
  //

  /**
   * Registers the stream to the agent
   */
  public void setAgentStream(BamStream handler);

  /**
   * Registers the stream to the agent
   */
  public BamStream getAgentStream();

  //
  // message handling
  //

  /**
   * Sends a message
   */
  public void message(String to, Serializable value);

  //
  // query handling
  //

  /**
   * Queries the service
   */
  public Serializable queryGet(String to, Serializable query);

  /**
   * Queries the service
   */
  public void queryGet(String to, Serializable query, BamQueryCallback callback);

  /**
   * Queries the service
   */
  public Serializable querySet(String to, Serializable query);

  /**
   * Queries the service
   */
  public void querySet(String to, Serializable query, BamQueryCallback callback);

  //
  // presence handling
  //

  /**
   * Sends the basic presence notification to the server.
   *
   * The server will send presence messages to all resources which
   * are subscribed to this client.
   *
   * The server will send presence probes to all resources which this
   * client subscribes to.
   */
  public void presence(Serializable data);

  /**
   * Basic presence
   */
  public void presence(String to, Serializable data);

  /**
   * Sends the basic presence unavailable notification to the server.
   */
  public void presenceUnavailable(Serializable data);

  /**
   * Basic presence on logout
   */
  public void presenceUnavailable(String to, Serializable data);

  /**
   * Presence callback on login
   */
  public void presenceProbe(String to, Serializable data);

  /**
   * Presence subscribe request
   */
  public void presenceSubscribe(String to, Serializable data);

  /**
   * Presence subscribed request
   */
  public void presenceSubscribed(String to, Serializable data);

  /**
   * Presence unsubscribe request
   */
  public void presenceUnsubscribe(String to, Serializable data);

  /**
   * Presence unsubscribed request
   */
  public void presenceUnsubscribed(String to, Serializable data);

  /**
   * Presence error
   */
  public void presenceError(String to,
			    Serializable data,
			    BamError error);
  
  /**
   * Returns the underlying, low-level stream to the broker
   */
  public BamStream getBrokerStream();
}
