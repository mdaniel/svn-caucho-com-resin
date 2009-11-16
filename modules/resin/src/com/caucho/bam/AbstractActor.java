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
 * Abstract implementation of a BAM actor.
 */
abstract public class AbstractActor implements Actor
{
  private static final Logger log
    = Logger.getLogger(AbstractActor.class.getName());

  private ActorStream _actorStream;
  private ActorStream _brokerStream;

  private String _jid;

  /**
   * The jid to the {@link com.caucho.bam.Broker} for addressing
   * from other Actors.
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  /**
   * The jid to the {@link com.caucho.bam.Broker} for addressing
   * from other Actors.
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * The stream to the Broker is used by the Actor to send messages to
   * all other Actors in the system.
   */
  public ActorStream getLinkStream()
  {
    return _brokerStream;
  }

  public ActorStream getBrokerStream()
  {
    return getLinkStream();
  }

  /**
   * The stream to the Broker is used by the Actor to send messages to
   * all other Actors in the system.
   */
  public void setBrokerStream(ActorStream linkStream)
  {
    _brokerStream = linkStream;
  }

  /**
   * Returns the stream to the Actor from the {@link com.caucho.bam.Broker} so
   * messages from other Actors can be delivered.
   */
  public ActorStream getActorStream()
  {
    return _actorStream;
  }

  /**
   * The stream to the Actor from the {@link com.caucho.bam.Broker} so
   * messages from other Actors can be delivered.
   */
  public void setActorStream(ActorStream actorStream)
  {
    _actorStream = actorStream;
  }

  //
  // Actor children
  //

  /**
   * Requests that a client actor with the given jid be started.
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   */
  public boolean startChild(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " startChild(" + jid + ")");

    return false;
  }

  /**
   * Requests that a client actor with the given jid be stopped.
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @jid the jid of the child actor logging in.
   */
  public boolean stopChild(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " stopChild(" + jid + ")");

    return false;
  }

  /**
   * Called when a a child actor logs in
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @jid the jid of the child actor logging in.
   */
  public void onChildStart(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onChildStart(" + jid + ")");
  }

  /**
   * Called when a child actor logs out
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @jid the jid of the child actor logging in.
   */
  public void onChildStop(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onChildStop(" + jid + ")");
  }

  //
  // Filter API
  //

  /**
   * Creates an outbound filter.
   */
  public ActorStream getActorFilter(ActorStream stream)
  {
    return stream;
  }

  /**
   * Creates an inbound filter
   */
  public ActorStream getBrokerFilter(ActorStream stream)
  {
    return stream;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
