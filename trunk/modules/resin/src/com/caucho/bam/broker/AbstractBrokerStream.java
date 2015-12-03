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

package com.caucho.bam.broker;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.MessageStream;

/**
 * The abstract implementation of an {@link com.caucho.bam.stream.MessageStream}
 * returns query errors for RPC packets, and ignores unknown packets
 * for messages and presence announcement.
 *
 * Most developers will use {@link com.caucho.bam.actor.SkeletonActorFilter}
 * or {@link com.caucho.bam.actor.SimpleActor} because those classes use
 * introspection with {@link com.caucho.bam.Message @Message} annotations
 * to simplify Actor development.
 */
abstract public class AbstractBrokerStream implements MessageStream
{
  private static final Logger log
    = Logger.getLogger(AbstractBrokerStream.class.getName());

  /**
   * Returns the actor stream for the given address.
   */
  abstract protected Mailbox getMailbox(String address);
  
  /**
   * Returns the address for the broker itself.
   */
  @Override
  public String getAddress()
  {
    return getClass().getSimpleName() + ".localhost";
  }

  //
  // Unidirectional messages
  //
  
  /**
   * Receives a unidirectional message.
   *
   * The abstract implementation ignores the message.
   * 
   * @param to the target actor's address
   * @param from the source actor's address
   * @param payload the message payload
   */
  @Override
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + ": message " + payload
                 + "\n  {to: " + to + ", from:" + from + "}");
    }
    
    MessageStream toStream = getMailbox(to);
    
    if (toStream != null) {
      toStream.message(to, from, payload);
      return;
    }

    String msg;
    msg = (this + ": message to unknown actor"
           + "\n  to: " + to
           + "\n  from: " + from
           + "\n  payload: " + payload);
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(msg);
    }

    BamError error = new BamError(BamError.TYPE_CANCEL,
                                      BamError.ITEM_NOT_FOUND,
                                      msg);

    MessageStream fromStream = getMailbox(from);

    if (fromStream != null)
      fromStream.messageError(from, to, payload, error);
  }
  
  /**
   * Receives a message error.
   *
   * The abstract implementation ignores the message.
   * 
   * @param to the target actor's address
   * @param from the source actor's address
   * @param payload the original message payload
   * @param error the message error
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + ": messageError " + error
                 + "\n  " + payload
                 + "\n  {to: " + to + ", from:" + from + "}");
    }
    
    MessageStream toStream = getMailbox(to);
    
    if (toStream != null) {
      toStream.messageError(to, from, payload, error);
      return;
    }

    String msg;
    msg = (this + ": messageError to unknown actor to:" + to
           + "\n  from:" + from
           + "\n  payload:" + payload);
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(msg);
    }
  }

  //
  // RPC query/response calls
  //
  
  /**
   * Receives a query call, acting as a service for
   * the query.
   *
   * The default implementation returns a feature-not-implemented QueryError
   * message to the client.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the service actor's address
   * @param from the client actor's address
   * @param payload the query payload
   *
   * @return true if this stream understand the query, false otherwise
   */
  @Override
  public void query(long id,
                    String to,
                    String from,
                    Serializable payload)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + ": query(" + id + ") " + payload
                 + "\n  {to: " + to + ", from:" + from + "}");
    }
    
    MessageStream toStream = getMailbox(to);
    
    if (toStream != null) {
      toStream.query(id, to, from, payload);
      return;
    }

    String msg;
    msg = (this + ": query(" + id + ") to unknown actor to:" + to
           + "\n  from:" + from
           + "\n  payload:" + payload);
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(msg);
    }

    BamError error = new BamError(BamError.TYPE_CANCEL,
                                      BamError.ITEM_NOT_FOUND,
                                      msg);

    MessageStream fromStream = getQueryErrorStream(from);

    if (fromStream != null) {
      fromStream.queryError(id, from, to, payload, error);
    }
  }
  
  protected MessageStream getQueryErrorStream(String from)
  {
    return getMailbox(from);
  }
  
  /**
   * Handles a query response from a service Actor.
   * The default implementation ignores the packet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's address
   * @param from the service actor's address
   * @param payload the result payload
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + ": queryResult(" + id + ") " + payload
                 + "\n  {to: " + to + ", from:" + from + "}");
    }
    
    MessageStream toStream = getMailbox(to);
    
    if (toStream != null) {
      toStream.queryResult(id, to, from, payload);
      return;
    }

    String msg;
    msg = (this + ": queryResult(qid=" + id + ") to unknown actor"
          + "\n  to: " + to
          + "\n  from: " + from
          + "\n  payload: " + payload);
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(msg);
    }
  }
  
  
  /**
   * Handles a query error from a service Actor.
   * The default implementation ignores the packet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's address
   * @param from the service actor's address
   * @param payload the result payload
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + ": queryError(" + id + ") " + error
                 + "\n  " + payload
                 + "\n  {to: " + to + ", from:" + from + "}");
    }
    
    MessageStream toStream = getMailbox(to);
    
    if (toStream != null) {
      toStream.queryError(id, to, from, payload, error);
      return;
    }

    String msg;
    msg = (this + ": queryError(" + id + ") to unknown actor to:" + to
           + "\n  from:" + from
           + "\n  payload:" + payload);
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(msg);
    }
  }
  
  /**
   * Tests if the stream is closed.
   */
  @Override
  public boolean isClosed()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
