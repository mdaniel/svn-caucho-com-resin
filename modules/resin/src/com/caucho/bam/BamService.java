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

import com.caucho.bam.BamAgentStream;
import com.caucho.bam.BamStream;

/**
 * BamService is a registered service.
 */
public interface BamService
{
  /**
   * Returns the service's jid.
   */
  public String getJid();

  /**
   * Returns the service's agent stream
   */
  public BamAgentStream getAgentStream();
  
  /**
   * Returns an agent, e.g. if the resource is room@domain, then
   * it might return a resource for room@domain/nick
   */
  public BamAgentStream findAgent(String jid);

  /**
   * Called when an agent logs in
   */
  public void onAgentStart(String jid);

  /**
   * Called when an agent logs out
   */
  public void onAgentStop(String jid);
  
  /**
   * Returns a filter for outbound calls, i.e. filtering messages to the agent.
   */
  public BamAgentStream getAgentFilter(BamAgentStream stream);

  /**
   * Returns a filter for inbound calls, i.e. filtering messages to the broker
   * from the agent.
   */
  public BamStream getBrokerFilter(BamStream stream);
}
