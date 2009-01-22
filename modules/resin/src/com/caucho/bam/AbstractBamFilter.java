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
 * Abstrat implementation of a HtmpStream filter.  The default operation
 * of most methods is to forward the request to the next stream.
 */
public class AbstractBamFilter implements BamStream
{
  private final BamStream _next;

  protected AbstractBamFilter(BamStream next)
  {
    _next = next;
  }

  protected BamStream getNext()
  {
    return _next;
  }
  
  /**
   * Returns the jid of the final agent
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
   * @param value the message payload
   */
  public void message(String to, String from, Serializable value)
  {
    _next.message(to, from, value);
  }
  
  /**
   * Sends a unidirectional message error
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    _next.messageError(to, from, value, error);
  }
  
  public boolean queryGet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    return _next.queryGet(id, to, from, query);
  }
  
  public boolean querySet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    return _next.querySet(id, to, from, query);
  }
  
  public void queryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    _next.queryResult(id, to, from, value);
  }
  
  public void queryError(long id,
			     String to,
			     String from,
			     Serializable query,
			     BamError error)
  {
    _next.queryError(id, to, from, query, error);
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void presence(String to,
			   String from,
			   Serializable data)
  {
    _next.presence(to, from, data);
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void presenceUnavailable(String to,
				      String from,
				      Serializable data)
  {
    _next.presenceUnavailable(to, from, data);
  }

  /**
   * Presence probe from the server to a client
   */
  public void presenceProbe(String to,
			      String from,
			      Serializable data)
  {
  }

  /**
   * A subscription request from a client
   */
  public void presenceSubscribe(String to,
				  String from,
				  Serializable data)
  {
    _next.presenceSubscribe(to, from, data);
  }

  /**
   * A subscription response to a client
   */
  public void presenceSubscribed(String to,
				   String from,
				   Serializable data)
  {
    _next.presenceSubscribed(to, from, data);
  }

  /**
   * An unsubscription request from a client
   */
  public void presenceUnsubscribe(String to,
				    String from,
				    Serializable data)
  {
    _next.presenceUnsubscribe(to, from, data);
  }

  /**
   * A unsubscription response to a client
   */
  public void presenceUnsubscribed(String to,
				     String from,
				     Serializable data)
  {
    _next.presenceUnsubscribed(to, from, data);
  }

  /**
   * An error response to a client
   */
  public void presenceError(String to,
			      String from,
			      Serializable data,
			      BamError error)
  {
    _next.presenceError(to, from, data, error);
  }

  /**
   * Closes the filter, but not the child by default.
   */
  public void close()
  {
  }
}
