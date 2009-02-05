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
import java.util.logging.*;

/**
 * The abstract implementation of an {@link com.caucho.bam.ActorStream}
 * returns query errors for RPC packets, and ignores unknown packets
 * for messages and presence announcement.
 *
 * Most developers will use {@link com.caucho.bam.SimpleActorStream}
 * or {@link com.caucho.bam.SimpleActor} because those classes use
 * introspection with {@link com.caucho.bam.Message @Message} annotations
 * to simplify Actor development.
 */
abstract public class AbstractActorStream implements ActorStream
{
  private static final Logger log
    = Logger.getLogger(AbstractActorStream.class.getName());
  
  /**
   * Returns the jid at the end of the stream.
   */
  abstract public String getJid();

  /**
   * Returns the stream to the broker.
   */
  abstract public ActorStream getBrokerStream();

  //
  // Unidirectional messages
  //
  
  /**
   * Receives a unidirectional message.
   *
   * The abstract implementation ignores the message.
   * 
   * @param to the target actor's JID
   * @param from the source actor's JID
   * @param payload the message payload
   */
  public void message(String to,
		      String from,
		      Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " message ignored " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }
  
  /**
   * Receives a message error.
   *
   * The abstract implementation ignores the message.
   * 
   * @param to the target actor's JID
   * @param from the source actor's JID
   * @param payload the original message payload
   * @param error the message error
   */
  public void messageError(String to,
			   String from,
			   Serializable payload,
			   ActorError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " messageError ignored " + error + " " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }

  //
  // RPC query/response calls
  //
  
  /**
   * Receives a query information call (get), acting as a service for
   * the query.
   *
   * The default implementation returns a feature-not-implemented QueryError
   * message to the client.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the service actor's JID
   * @param from the client actor's JID
   * @param payload the query payload
   *
   * @return true if this stream understand the query, false otherwise
   */
  public void queryGet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " queryGet not implemented " + payload
		+ " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }

    String msg;
    msg = (this + ": queryGet is not implemented by this actor.\n"
	   + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
				      ActorError.FEATURE_NOT_IMPLEMENTED,
				      msg);

    getBrokerStream().queryError(id, from, to, payload, error);
  }
  
  /**
   * Receives a query update call (set), acting as a service for
   * the query.
   *
   * The default implementation returns a feature-not-implemented QueryError
   * message to the client.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the service actor's JID
   * @param from the client actor's JID
   * @param payload the query payload
   *
   * @return true if this stream understand the query, false otherwise
   */
  public void querySet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " querySet not implemented " + payload
		+ " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }

    String msg;
    msg = (this + ": querySet is not implemented for this payload:\n"
	   + "  " + payload
	   + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
				      ActorError.FEATURE_NOT_IMPLEMENTED,
				      msg);

    getBrokerStream().queryError(id, from, to, payload, error);
  }
  
  /**
   * Handles a query response from a service Actor.
   * The default implementation ignores the packet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's JID
   * @param from the service actor's JID
   * @param payload the result payload
   */
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " queryResult ignored " + payload
		+ " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }
  }
  
  
  /**
   * Handles a query error from a service Actor.
   * The default implementation ignores the packet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's JID
   * @param from the service actor's JID
   * @param payload the result payload
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable payload,
			 ActorError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " queryError ignored " + error + " " + payload
		+ " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }
  }

  //
  // presence - publish/subscribe messages
  //
  
  /**
   * Handles a subscriber Actor's presence/login announcement.
   * The default implementation ignores the packet.
   *
   * @param to the publisher actor's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presence(String to,
		       String from,
		       Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presence ignored " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }
  
  /**
   * Handles a subscriber Actor's logout announcement.
   * The default implementation ignores the packet.
   *
   * @param to the publisher actor's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceUnavailable(String to,
				      String from,
				      Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceUnavailable ignored " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }

  /**
   * Handles a publisher's probing packet, used to query
   * subscriber capabilities.  The default implementation ignores the
   * packet.
   *
   * @param to the subscriber actor's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceProbe(String to,
			    String from,
			    Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceProbe ignored " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }

  /**
   * Handles a subscription request from a subscriber.
   * The default implementation returns a feature-not-implemented
   * error.
   *
   * @param to the publisher actor's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribe(String to,
				String from,
				Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceSubscribe rejected " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }

    String msg
      = (this + ": presenceSubscribe is not implemented by this actor.\n"
	   + payload + " {from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
				      ActorError.FEATURE_NOT_IMPLEMENTED,
				      msg);

    getBrokerStream().presenceError(from, to, payload, error);
  }

  /**
   * Handles a subscription acceptance from a publisher.  The default
   * implementation ignores the request.
   *
   * @param to the subscriber actor's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribed(String to,
				 String from,
				 Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceSubscribed ignored " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }

  /**
   * Handles a unsubscription request from a subscribing Actor.
   * The default implementation returns a feature-not-implemented
   * error.
   *
   * @param to the publisher actor's JID
   * @param from the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribe(String to,
				  String from,
				  Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceUnsubscribe default accept " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }

    String msg;
    msg = (this + ": presenceSubscribe is not implemented by this actor.\n"
	   + payload + " {from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
				      ActorError.FEATURE_NOT_IMPLEMENTED,
				      msg);

    getBrokerStream().presenceError(from, to, payload, error);
  }

  /**
   * Handles a unsubscribed notification from a publishing Actor.
   * The default implementation ignores the packet.
   *
   * @param to the subscriber actor's JID
   * @param from the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribed(String to,
				   String from,
				   Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceUnsubscribed ignored " + payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }


  /**
   * Handles a presence error from a publishing Actor.
   * The default implementation ignores the packet.
   *
   * @param to the target actor's JID
   * @param from the source actor's JID
   * @param payload the presence payload of the original request
   * @param error description of the error
   */
  public void presenceError(String to,
			    String from,
			    Serializable payload,
			    ActorError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceError ignored " + error + " "+ payload
		+ " {from:" + from + ", to:" + to + "}");
    }
  }

  /**
   * Closes the stream
   */
  public void close()
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
