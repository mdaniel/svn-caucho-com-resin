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

import com.caucho.bam.*;

import com.caucho.util.*;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Manager
 */
public class HempConnectionImpl extends AbstractBamConnection
{
  private static final Logger log
    = Logger.getLogger(HempConnectionImpl.class.getName());
  
  private static final L10N L = new L10N(HempConnectionImpl.class);
  
  private final HempBroker _broker;
  private HempConnectionAgentStream _handler;
  
  private final String _jid;
  
  private BamStream _brokerFilter;
  // private HmtpAgentStream _agentFilter;
  
  private BamStream _brokerStream;
  // private HmtpAgentStream _agentStream;
  
  private BamService _resource;

  HempConnectionImpl(HempBroker broker,
		     String jid,
		     BamStream agentStream)
  {
    _broker = broker;
    _jid = jid;

    //_handler = new HempConnectionAgentStream(this);
    //setAgentStream(_handler);

    _brokerStream = broker.getBrokerStream();
    // _agentStream = _handler;
    
    if (agentStream == null)
      agentStream = new SimpleBamClientStream();
    
    setAgentStream(agentStream);
    
    String uid = jid;
    int p = uid.indexOf('/');
    if (p > 0)
      uid = uid.substring(0, p);

    _resource = broker.findService(uid);

    if (_resource != null) {
      _brokerFilter = _resource.getBrokerFilter(_broker);
      _brokerStream = _brokerFilter;
      
      // _agentFilter = _resource.getAgentFilter(_handler);
    }
  }

  /**
   * Returns the session's jid
   */
  public String getJid()
  {
    return _jid;
  }

  HempConnectionAgentStream getAgentStreamHandler()
  {
    return _handler;
  }
  
  public BamStream getBrokerStream()
  {
    return _brokerStream;
  }

  @Override
  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    close();
  }
}
