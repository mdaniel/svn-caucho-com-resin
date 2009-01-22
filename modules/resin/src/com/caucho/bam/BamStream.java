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

/**
 * Main agent callback to handle packet events.  Each method corresponds to
 * a packet class.
 */
public interface BamStream
{
  /**
   * Returns the jid of the agent at the end of the stream.  For brokers,
   * returns null.
   */
  public String getJid();
  
  //
  // messages
  //
  
  /**
   * Sends a message to an agent
   * 
   * @param to the target agent's JID
   * @param from the source agent's JID
   * @param value the message payload
   */
  public void message(String to, String from, Serializable value);
  
  /**
   * Sends a message error to an agent
   * 
   * @param to the target agent's JID
   * @param from the source agent's JID
   * @param value the message payload
   * @param error the message error
   */
  public void messageError(String to,
			   String from,
			   Serializable value,
			   BamError error);

  //
  // queries (iq)
  //
  
  /**
   * Sends a query information call (get), returning true if this
   * handler understands the query class, and false if it does not.
   *
   * If queryGet returns true, the handler MUST send a
   * <code>queryResult</code> or <code>queryError</code> to the sender,
   * using the same <code>id</code>.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param query the query payload
   *
   * @return true if this handler understand the query, false otherwise
   */
  public boolean queryGet(long id,
			  String to,
			  String from,
			  Serializable query);
  
  /**
   * Sends a query update request (set), returning true if this handler
   * understands the query class, and false if it does not.
   *
   * If querySet returns true, the handler MUST send a
   * <code>queryResult</code> or <code>queryError</code> to the sender,
   * using the same <code>id</code>.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param query the query payload
   *
   * @return true if this handler understand the query, false otherwise
   */
  public boolean querySet(long id,
			  String to,
			  String from,
			  Serializable query);

  /**
   * Handles the query response from a corresponding queryGet or querySet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param value the result payload
   */
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value);
  
  /**
   * Handles the query error from a corresponding queryGet or querySet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param query the query payload
   * @param error additional error information
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 BamError error);

  //
  // presence
  //

  /**
   * Announces an agent's presence, e.g. for an IM user logging on
   * or changing their status text.
   */
  public void presence(String to,
		       String from,
		       Serializable data);

  /**
   * Announces a logout of an agent.
   */
  public void presenceUnavailable(String to,
				  String from,
				  Serializable data);

  /**
   * Presence forwarding announcement from a server to a client.
   */
  public void presenceProbe(String to,
			    String from,
			    Serializable data);

  /**
   * A subscription request from a client
   */
  public void presenceSubscribe(String to,
				String from,
				Serializable data);

  /**
   * A subscription response to a client
   */
  public void presenceSubscribed(String to,
				 String from,
				 Serializable data);

  /**
   * An unsubscription request from a client
   */
  public void presenceUnsubscribe(String to,
				  String from,
				  Serializable data);

  /**
   * A unsubscription response to a client
   */
  public void presenceUnsubscribed(String to,
				   String from,
				   Serializable data);

  /**
   * An error response to a client
   */
  public void presenceError(String to,
			    String from,
			    Serializable data,
			    BamError error);

  /**
   * Closes the stream
   */
  public void close();
}
