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
import java.util.*;

/**
 * Abstract implementation of a BAM filter.  The default operation
 * of most methods is to forward the request to the next stream.
 */
public class AbstractFilter implements ActorStream
{
  private final ActorStream _next;

  protected AbstractFilter(ActorStream next)
  {
    _next = next;
  }

  protected ActorStream getNext()
  {
    return _next;
  }
  
  /**
   * Returns the jid of the final actor
   */
  public String getJid()
  {
    return _next.getJid();
  }
  
  /**
   * Sends a unidirectional message
   * 
   * @param to the target JID
   * @param from the source JID
   * @param payload the message payload
   */
  public void message(String to, String from, Serializable payload)
  {
    _next.message(to, from, payload);
  }
  
  /**
   * Sends a unidirectional message error
   * 
   * @param to the target JID
   * @param from the source JID
   * @param payload the message payload
   */
  public void messageError(String to,
			   String from,
			   Serializable payload,
			   ActorError error)
  {
    _next.messageError(to, from, payload, error);
  }
  
  public void queryGet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    _next.queryGet(id, to, from, payload);
  }
  
  public void querySet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    _next.querySet(id, to, from, payload);
  }
  
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable payload)
  {
    _next.queryResult(id, to, from, payload);
  }
  
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable payload,
			 ActorError error)
  {
    _next.queryError(id, to, from, payload, error);
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void presence(String to,
		       String from,
		       Serializable payload)
  {
    _next.presence(to, from, payload);
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void presenceUnavailable(String to,
				  String from,
				  Serializable payload)
  {
    _next.presenceUnavailable(to, from, payload);
  }

  /**
   * Presence probe from the server to a client
   */
  public void presenceProbe(String to,
			    String from,
			    Serializable payload)
  {
    _next.presenceProbe(to, from, payload);
  }

  /**
   * A subscription request from a client
   */
  public void presenceSubscribe(String to,
				String from,
				Serializable payload)
  {
    _next.presenceSubscribe(to, from, payload);
  }

  /**
   * A subscription response to a client
   */
  public void presenceSubscribed(String to,
				 String from,
				 Serializable payload)
  {
    _next.presenceSubscribed(to, from, payload);
  }

  /**
   * An unsubscription request from a client
   */
  public void presenceUnsubscribe(String to,
				  String from,
				  Serializable payload)
  {
    _next.presenceUnsubscribe(to, from, payload);
  }

  /**
   * A unsubscription response to a client
   */
  public void presenceUnsubscribed(String to,
				     String from,
				     Serializable payload)
  {
    _next.presenceUnsubscribed(to, from, payload);
  }

  /**
   * An error response to a client
   */
  public void presenceError(String to,
			    String from,
			    Serializable payload,
			    ActorError error)
  {
    _next.presenceError(to, from, payload, error);
  }

  /**
   * Closes the filter, but not the child by default.
   */
  public void close()
  {
  }
}
