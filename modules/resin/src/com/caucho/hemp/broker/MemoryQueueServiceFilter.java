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

import com.caucho.bam.BamError;
import com.caucho.bam.BamService;
import com.caucho.bam.BamStream;
import com.caucho.bam.BamBroker;
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
public class MemoryQueueServiceFilter implements BamService
{
  private static final Logger log
    = Logger.getLogger(MemoryQueueServiceFilter.class.getName());
  private static final L10N L = new L10N(MemoryQueueServiceFilter.class);
  
  private final BamService _service;
  private final BamStream _serviceStream;

  public MemoryQueueServiceFilter(BamService service,
				  BamBroker broker,
				  int threadMax)
  {
    if (service == null)
      throw new NullPointerException();

    if (broker == null)
      throw new NullPointerException();
    
    _service = service;

    _serviceStream
      = new HempMemoryQueue(null, broker, service.getAgentStream());
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

  public BamStream getAgentStream()
  {
    return _serviceStream;
  }
  
  /**
   * Requests that an agent with the given jid be started. 
   */
  public boolean startAgent(String jid)
  {
    return _service.startAgent(jid);
  }

  /**
   * Requests that an agent with the given jid be stopped. 
   */
  public boolean stopAgent(String jid)
  {
    return _service.stopAgent(jid);
  }

  /**
   * Called when an agent logs in
   */
  public void onAgentStart(String jid)
  {
    _service.onAgentStart(jid);
  }

  /**
   * Called when an agent logs out
   */
  public void onAgentStop(String jid)
  {
    _service.onAgentStop(jid);
  }
  
  /**
   * Returns a filter for outbound calls, i.e. filtering messages to the agent.
   */
  public BamStream getAgentFilter(BamStream stream)
  {
    return _service.getAgentFilter(stream);
  }

  /**
   * Returns a filter for inbound calls, i.e. filtering messages to the broker
   * from the agent.
   */
  public BamStream getBrokerFilter(BamStream stream)
  {
    return _service.getBrokerFilter(stream);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _service + "]";
  }
}
