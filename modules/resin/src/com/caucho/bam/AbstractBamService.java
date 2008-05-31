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

import com.caucho.bam.BamService;
import com.caucho.bam.BamAgentStream;
import com.caucho.bam.BamStream;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * Configuration for a service
 */
abstract public class AbstractBamService implements BamService
{
  private static final Logger log
    = Logger.getLogger(AbstractBamService.class.getName());
  
  private String _jid;

  protected void setJid(String jid)
  {
    _jid = jid;
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Looks up a sub-resource
   */
  public BamAgentStream findAgent(String jid)
  {
    return null;
  }

  /**
   * Returns the stream to the service's agent
   */
  abstract public BamAgentStream getAgentStream();

  /**
   * Creates an outbound filter
   */
  public BamAgentStream getAgentFilter(BamAgentStream stream)
  {
    return stream;
  }

  /**
   * Creates an inbound filter
   */
  public BamStream getBrokerFilter(BamStream stream)
  {
    return stream;
  }

  /**
   * Called when an agent logs in
   * 
   * @jid the jid of the agent logging in.
   */
  public void onAgentStart(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onLogin(" + jid + ")");
  }

  /**
   * Called when an agent logs out
   */
  public void onAgentStop(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onLogout(" + jid + ")");
  }

  protected String logValue(Serializable value)
  {
    if (value == null)
      return "";
    else
      return " value=" + value.getClass().getSimpleName();
  }

  protected String logData(Serializable []data)
  {
    if (data == null)
      return "";

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    for (int i = 0; i < data.length; i++) {
      if (i != 0)
	sb.append(", ");

      if (data[i] != null)
	sb.append(data[i].getClass().getSimpleName());
      else
	sb.append("null");
    }

    sb.append("]");

    return sb.toString();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
