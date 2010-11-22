/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class FallbackActorStream implements ActorStream
{
  private static final Logger log
    = Logger.getLogger(FallbackActorStream.class.getName());

  private Actor _actor;
  
  public FallbackActorStream(Actor actor)
  {
    _actor = actor;
    
    if (actor == null)
      throw new IllegalArgumentException();
  }

  /**
   * Returns the Actor's jid so the {@link com.caucho.bam.Broker} can
   * register it.
   */
  @Override
  public String getJid()
  {
    return _actor.getJid();
  }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
  public ActorStream getLinkStream()
  {
    return _actor.getLinkStream();
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " message ignored " + payload
               + " {from: " + from + " to: " + to + "}");
    }
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " messageError ignored " + error + " " + payload
               + " {from: " + from + " to: " + to + "}");
    }
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  @Override
  public void queryGet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet not implemented for " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }

    String msg;
    msg = (this + ": queryGet is not implemented for this payload:\n"
           + "  " + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);

    getLinkStream().queryError(id, from, to, payload, error);
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  @Override
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " querySet not implemented for " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }

    String msg;
    msg = (this + ": querySet is not implemented for this payload:\n"
           + "  " + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);

    getLinkStream().queryError(id, from, to, payload, error);
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryResult not implemented for " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryError ignored " + error + " " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }
  }
  
  @Override
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Close the stream
   */
  @Override
  public void close()
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "," + _actor.getClass().getSimpleName() + "]";
  }
}
