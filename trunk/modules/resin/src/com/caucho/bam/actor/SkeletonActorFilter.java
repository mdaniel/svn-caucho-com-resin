/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.bam.actor;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.broker.Broker;

/**
 * Base ActorStream implementation using introspection and
 * {@link com.caucho.bam.Message @Message} annotations to simplify
 * Actor development.
 *
 * <h2>Message Handling</h2>
 *
 * To handle a message, create a method with the proper signature for
 * the expected payload type and
 * annotate it with {@link com.caucho.bam.Message @Message}.  To send
 * a response message or query, use <code>getBrokerStream()</code> or
 * <code>getClient()</code>.
 *
 * <code><pre>
 * @Message
 * public void myMessage(String to, String from, MyPayload payload);
 * </pre></code>
 */
public class SkeletonActorFilter<T> implements Actor
{
  private final BamSkeleton<T> _skeleton;
  private final T _actor;
  
  private final Actor _next;

  public SkeletonActorFilter(Actor next, T actor)
  {
    if (next == null)
      throw new IllegalStateException("next is a required argument");
    
    if (actor == null)
      throw new IllegalStateException("actor is a required argument");
    
    _next = next;
    _actor = actor;
    
    _skeleton = createSkeleton(actor);
  }
  
  @SuppressWarnings("unchecked")
  protected BamSkeleton<T> createSkeleton(T actor)
  {
    return BamSkeleton.getSkeleton((Class<T>) actor.getClass());
  }
  
  /**
   * Returns the Actor's address so the {@link com.caucho.bam.broker.Broker} can
   * register it.
   */
  @Override
  public String getAddress()
  {
    return _next.getAddress();
  }

   @Override
   public boolean isClosed()
   {
     return _next.isClosed();
   }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
   @Override
  public Broker getBroker()
  {
    return _next.getBroker();
  }

  //
  // message
  //

  /**
   * Dispatches a unidirectional message to a matching method on
   * the SimpleActorStream.
   *
   * By default, message invokes a method
   * annotated by {@link com.caucho.bam.Message @Message} with
   * a payload class matching the message payload.
   *
   * If no method is found, the message is ignored.
   *
   * @param to the SimpleActorStream's address
   * @param from the sending actor's address
   * @param payload the message payload
   */
  @Override
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    _skeleton.message(_actor, _next, to, from, payload);
  }

  /**
   * Dispatches a messageError to a matching method on
   * the SimpleActorStream.
   *
   * By default, messageError invokes a method
   * annotated by {@link com.caucho.bam.MessageError @MessageError} with
   * a payload class matching the messageError payload.
   *
   * If no method is found, the messageError is ignored.
   *
   * @param to the SimpleActorStream's address
   * @param from the sending actor's address
   * @param payload the message payload
   * @param error the message error
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    _skeleton.messageError(_actor, _next, to, from, payload, error);
  }

  //
  // RPC query
  //

  /**
   * Dispatches a queryGet to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryGet invokes a method
   * annotated by {@link com.caucho.bam.Query @QueryGet} with
   * a payload class matching the queryGet payload.
   *
   * The {@link com.caucho.bam.Query @QueryGet} method MUST
   * send either a queryResult or queryError as a response.
   *
   * If no method is found, queryGet sends a queryError response with
   * a feature-not-implemented error.
   *
   * @param id a correlation id to match the result or error
   * @param to the SimpleActorStream's address
   * @param from the client actor's address
   * @param payload the query payload
   */
  @Override
  public void query(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    _skeleton.query(_actor, _next, getBroker(), id, to, from, payload);
  }

  /**
   * Dispatches a queryResult to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryResult invokes a method
   * annotated by {@link com.caucho.bam.QueryResult @QueryResult} with
   * a payload class matching the queryResult payload.
   *
   * If no method is found, queryResult ignores the packet.
   *
   * @param id the correlation id from the original query
   * @param to the SimpleActorStream's address
   * @param from the client actor's address
   * @param payload the query payload
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    _skeleton.queryResult(_actor, _next, id, to, from, payload);
  }

  /**
   * Dispatches a queryError to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryError invokes a method
   * annotated by {@link com.caucho.bam.QueryError @QueryError} with
   * a payload class matching the queryError payload.
   *
   * If no method is found, queryError ignores the packet.
   *
   * @param id the correlation id from the original query
   * @param to the SimpleActorStream's address
   * @param from the client actor's address
   * @param payload the query payload
   * @param error the error information
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    _skeleton.queryError(_actor, _next, id, to, from, payload, error);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "," + _actor.getClass().getName() + "]";
  }
}
