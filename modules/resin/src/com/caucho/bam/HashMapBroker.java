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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class HashMapBroker extends AbstractBrokerStream
{
  private final String _jid;
  
  private final ConcurrentHashMap<String,ActorStream> _actorMap
    = new ConcurrentHashMap<String,ActorStream>();

  public HashMapBroker(String jid)
  {
    _jid = jid;
  }
  
  /**
   * Returns the jid for the broker itself.
   */
  @Override
  public String getJid()
  {
    return _jid;
  }
  
  /**
   * Returns the actor stream for the given jid.
   */
  @Override
  protected ActorStream getActorStream(String jid)
  {
    return _actorMap.get(jid);
  }
  
  /**
   * Adds a new actor to the broker.
   */
  public void addActor(ActorStream actor)
  {
    String jid = actor.getJid();
    
    if (jid == null)
      throw new NullPointerException(String.valueOf(actor) + " has a null jid");
    
    _actorMap.put(jid, actor);
  }

  /**
   * Removes an actor from the broker.
   */
  public void removeActor(ActorStream actor)
  {
    String jid = actor.getJid();
    
    _actorMap.remove(jid);
  }
}
