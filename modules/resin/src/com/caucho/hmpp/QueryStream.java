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

/**
 * Handling query packets
 */
public interface QueryStream {
  /**
   * Handles a query information request (get), returning true if this
   * handler understands the query class, and false if it does not.
   *
   * If sendQueryGet returns true, the handler MUST send a
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
  public boolean sendQueryGet(long id,
			      String to,
			      String from,
			      Serializable query);
  
  /**
   * Handles a query update request (set), returning true if this handler
   * understands the query class, and false if it does not.
   *
   * If sendQuerySet returns true, the handler MUST send a
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
  public boolean sendQuerySet(long id,
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
  public void sendQueryResult(long id,
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
  public void sendQueryError(long id,
			     String to,
			     String from,
			     Serializable query,
			     HmppError error);
}
