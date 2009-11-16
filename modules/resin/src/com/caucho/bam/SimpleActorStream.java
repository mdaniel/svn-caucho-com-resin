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

import java.util.logging.*;
import java.util.concurrent.atomic.*;

import java.io.Serializable;

/**
 * Base ActorStream implementation using introspection and
 * {@link com.caucho.bam.Message @Message} annotations to simplify
 * Actor development.
 *
 * <h2>Message Handline</h2>
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
public class SimpleActorStream implements ActorStream
{
  private static final Logger log
    = Logger.getLogger(SimpleActorStream.class.getName());

  private final Skeleton _skeleton;

  private String _jid;
  private ActorStream _brokerStream;
  private ActorClient _client;

  private Broker _broker;

  public SimpleActorStream()
  {
    _skeleton = Skeleton.getSkeleton(getClass());
  }

  /**
   * Returns the Actor's jid so the {@link com.caucho.bam.Broker} can
   * register it.
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Sets the Actor's jid so the {@link com.caucho.bam.Broker} can
   * register it.
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  /**
   * Returns the ActorClient for convenient message calls.
   */
  public ActorClient getActorClient()
  {
    return _client;
  }

  /**
   * Returns the ActorClient for convenient message calls.
   */
  public void setActorClient(ActorClient client)
  {
    _client = client;
  }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
  public ActorStream getBrokerStream()
  {
    return _brokerStream;
  }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
  public ActorStream getLinkStream()
  {
    return _brokerStream;
  }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
  public void setBrokerStream(ActorStream brokerStream)
  {
    _brokerStream = brokerStream;
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
   * @param to the SimpleActorStream's JID
   * @param from the sending actor's JID
   * @param payload the message payload
   */
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    _skeleton.message(this, to, from, payload);
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
   * @param to the SimpleActorStream's JID
   * @param from the sending actor's JID
   * @param payload the message payload
   * @param error the message error
   */
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    _skeleton.messageError(this, to, from, payload, error);
  }

  //
  // RPC query
  //

  /**
   * Dispatches a queryGet to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryGet invokes a method
   * annotated by {@link com.caucho.bam.QueryGet @QueryGet} with
   * a payload class matching the queryGet payload.
   *
   * The {@link com.caucho.bam.QueryGet @QueryGet} method MUST
   * send either a queryResult or queryError as a response.
   *
   * If no method is found, queryGet sends a queryError response with
   * a feature-not-implemented error.
   *
   * @param id a correlation id to match the result or error
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   */
  public void queryGet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    _skeleton.queryGet(this, id, to, from, payload);
  }

  /**
   * Dispatches a querySet to a matching method on
   * the SimpleActorStream.
   *
   * By default, querySet invokes a method
   * annotated by {@link com.caucho.bam.QuerySet @QuerySet} with
   * a payload class matching the querySet payload.
   *
   * The {@link com.caucho.bam.QuerySet @QuerySet} method MUST
   * send either a queryResult or queryError as a response.
   *
   * If no method is found, querySet sends a queryError response with
   * a feature-not-implemented error.
   *
   * @param id a correlation id to match the result or error
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   */
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    _skeleton.querySet(this, id, to, from, payload);
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
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   */
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    ActorClient client = _client;

    if (client != null && client.onQueryResult(id, to, from, payload)) {
      return;
    }

    _skeleton.queryResult(this, id, to, from, payload);
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
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   * @param error the error information
   */
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    ActorClient client = _client;

    if (client != null && client.onQueryError(id, to, from, payload, error)) {
      return;
    }

    _skeleton.queryError(this, id, to, from, payload, error);
  }

  //
  // presence
  //

  /**
   * Dispatches a presence notification to a matching method on
   * the SimpleActorStream.  Presence is a notification from
   * a subscriber to a publisher that it has logged in or changed
   * its state.
   *
   * By default, presence invokes a method
   * annotated by {@link com.caucho.bam.Presence @Presence} with
   * a payload class matching the presence payload.
   *
   * If no method is found, presence ignores the packet.
   *
   * @param to the SimpleActorStream's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presence(String to,
                       String from,
                       Serializable presence)
  {
    _skeleton.presence(this, to, from, presence);
  }

  /**
   * Dispatches a presenceUnavailable notification to a matching method on
   * the SimpleActorStream.  PresenceUnavailable is a notification from
   * a subscriber to a publisher that it has logged out.
   *
   * By default, presenceUnavailable invokes a method
   * annotated by {@link com.caucho.bam.PresenceUnavailable
   * @PresenceUnavailable} with
   * a payload class matching the presenceUnavailable payload.
   *
   * If no method is found, presenceUnavailable ignores the packet.
   *
   * @param to the SimpleActorStream's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceUnavailable(String to,
                                  String from,
                                  Serializable payload)
  {
    _skeleton.presenceUnavailable(this, to, from, payload);
  }

  /**
   * Dispatches a presenceProbe notification to a matching method on
   * the SimpleActorStream.  PresenceProbe is a request from
   * a publisher to a subscriber, asking for client capabilities, like
   * image or audio support..
   *
   * By default, presenceProbe invokes a method
   * annotated by {@link com.caucho.bam.PresenceProbe
   * @PresenceProbe} with
   * a payload class matching the presenceProbe payload.
   *
   * If no method is found, presenceProbe ignores the packet.
   *
   * @param to the SimpleActorStream's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceProbe(String to,
                            String from,
                            Serializable payload)
  {
    _skeleton.presenceProbe(this, to, from, payload);
  }

  /**
   * Dispatches a presenceSubscribe request to a matching method on
   * the SimpleActorStream.  PresenceSubscribe is a request from
   * a subscriber to publisher asking to subscribe for messages.
   *
   * By default, presenceSubscribe invokes a method
   * annotated by {@link com.caucho.bam.PresenceSubscribe
   * @PresenceSubscribe} with
   * a payload class matching the presenceSubscribe payload.
   *
   * If no method is found, presenceSubscribe sends a presenceError message
   * with a feature-not-implemented error.
   *
   * @param to the SimpleActorStream's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribe(String to,
                                String from,
                                Serializable payload)
  {
    _skeleton.presenceSubscribe(this, to, from, payload);
  }

  /**
   * Dispatches a presenceSubscribed request to a matching method on
   * the SimpleActorStream.  PresenceSubscribed is a notification from
   * a publisher to a subscriber that it has been subscribed to receive
   * messagse.
   *
   * By default, presenceSubscribed invokes a method
   * annotated by {@link com.caucho.bam.PresenceSubscribed
   * @PresenceSubscribed} with
   * a payload class matching the presenceSubscribed payload.
   *
   * If no method is found, presenceSubscribed ignores the packet.
   *
   * @param to the SimpleActorStream's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribed(String to,
                                 String from,
                                 Serializable payload)
  {
    _skeleton.presenceSubscribed(this, to, from, payload);
  }

  /**
   * Dispatches a presenceUnsubscribe request to a matching method on
   * the SimpleActorStream.  PresenceUnsubscribe is a request from
   * a subscriber to publisher asking to unsubscribe from message
   * sending.
   *
   * By default, presenceUnsubscribe invokes a method
   * annotated by {@link com.caucho.bam.PresenceUnsubscribe
   * @PresenceUnsubscribe} with
   * a payload class matching the presenceUnsubscribe payload.
   *
   * If no method is found, presenceUnsubscribe sends a presenceError message
   * with a feature-not-implemented error.
   *
   * @param to the SimpleActorStream's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribe(String to,
                                  String from,
                                  Serializable payload)
  {
    _skeleton.presenceUnsubscribe(this, to, from, payload);
  }

  /**
   * Dispatches a presenceUnsubscribed request to a matching method on
   * the SimpleActorStream.  PresenceUnsubscribed is a notification from
   * a publisher to a subscriber that it has been unsubscribed from receive
   * messagse.
   *
   * By default, presenceUnsubscribed invokes a method
   * annotated by {@link com.caucho.bam.PresenceUnsubscribed
   * @PresenceUnsubscribed} with
   * a payload class matching the presenceUnsubscribed payload.
   *
   * If no method is found, presenceUnsubscribed ignores the packet.
   *
   * @param to the SimpleActorStream's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribed(String to,
                                   String from,
                                   Serializable payload)
  {
    _skeleton.presenceUnsubscribed(this, to, from, payload);
  }

  /**
   * Dispatches a presenceError request to a matching method on
   * the SimpleActorStream.  PresenceError is a notification of an
   * error in a presence request.
   *
   * By default, presenceError invokes a method
   * annotated by {@link com.caucho.bam.PresenceError
   * @PresenceError} with
   * a payload class matching the presenceError payload.
   *
   * If no method is found, presenceError ignores the packet.
   *
   * @param to the SimpleActorStream's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   * @param error the error information
   */
  public void presenceError(String to,
                            String from,
                            Serializable payload,
                            ActorError error)
  {
    _skeleton.presenceError(this, to, from, payload, error);
  }

  //
  // skeleton callbacks
  //

  protected Skeleton getSkeleton()
  {
    return _skeleton;
  }

  /**
   * Close the stream
   */
  public void close()
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
