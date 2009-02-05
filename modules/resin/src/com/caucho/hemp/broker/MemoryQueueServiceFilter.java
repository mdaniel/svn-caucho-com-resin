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

package com.caucho.hemp.broker;

import com.caucho.bam.ActorError;
import com.caucho.bam.Actor;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.config.ConfigException;
import com.caucho.hemp.packet.*;
import com.caucho.server.resin.*;
import com.caucho.server.util.*;
import com.caucho.util.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;

/**
 * Queue of hmtp packets
 */
public class MemoryQueueServiceFilter implements Actor
{
  private static final Logger log
    = Logger.getLogger(MemoryQueueServiceFilter.class.getName());
  private static final L10N L = new L10N(MemoryQueueServiceFilter.class);
  
  private final Actor _service;
  private final ActorStream _serviceStream;

  public MemoryQueueServiceFilter(Actor service,
				  Broker broker,
				  int threadMax)
  {
    if (service == null)
      throw new NullPointerException();

    if (broker == null)
      throw new NullPointerException();
    
    _service = service;

    _serviceStream
      = new HempMemoryQueue(null, broker, service.getActorStream());
  }
  
  /**
   * Returns the service's jid
   */
  public String getJid()
  {
    return _service.getJid();
  }
  
  /**
   * Returns the service's jid
   */
  public void setJid(String jid)
  {
  }

  public ActorStream getActorStream()
  {
    return _serviceStream;
  }
  
  /**
   * Requests that an child with the given jid be started. 
   */
  public boolean startChild(String jid)
  {
    return _service.startChild(jid);
  }

  /**
   * Requests that an child with the given jid be stopped. 
   */
  public boolean stopChild(String jid)
  {
    return _service.stopChild(jid);
  }

  /**
   * Called when an child logs in
   */
  public void onChildStart(String jid)
  {
    _service.onChildStart(jid);
  }

  /**
   * Called when an child logs out
   */
  public void onChildStop(String jid)
  {
    _service.onChildStop(jid);
  }
  
  /**
   * Returns a filter for outbound calls, i.e. filtering messages to the actor.
   */
  public ActorStream getActorFilter(ActorStream stream)
  {
    return _service.getActorFilter(stream);
  }

  /**
   * Returns a filter for inbound calls, i.e. filtering messages to the broker
   * from the actor.
   */
  public ActorStream getBrokerFilter(ActorStream stream)
  {
    return _service.getBrokerFilter(stream);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _service + "]";
  }
}
