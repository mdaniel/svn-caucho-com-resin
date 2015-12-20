/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.ramp.jamp;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.remote.ChannelManagerService;
import com.caucho.v5.amp.remote.ChannelServerFactoryImpl;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.pod.NodePodAmp;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.util.L10N;

/**
 * JampServlet specialized for dispatching to the pod owner.
 */
//@WebServlet(value="/s/*",asyncSupported=true)
public class JampServletPodsImpl extends JampServletBase
{
  private static final L10N L = new L10N(JampServletPodsImpl.class);
  
  private final AtomicLong _roundRobin = new AtomicLong();
  
  @Override
  protected JampPodManager createPodManager()
  {
    return new JampPodManagerPods();
  }

  /**
   * pod://pod-name is the default authority for a request to /my-service.
   */
  @Override
  protected String getAuthority(String podName)
  {
    return "pod://" + podName;
  }
  
  @Override
  protected String getPodForward(String podName)
  {
    BartenderSystem bartender = BartenderSystem.getCurrent();
    
    PodBartender pod = bartender.findPod(podName);
    
    long roundRobin = _roundRobin.incrementAndGet();
    
    NodePodAmp node = pod.getNode((int) roundRobin);
    
    if (node == null) {
      return null;
    }
    
    for (int i = 0; i < node.getServerCount(); i++) {
      ServerBartender server = node.getServer(0);
      
      // String serverId = server.getServerId();
      
      if (server != null && server.isUp()) {
        return server.getId();
      }
    }
    
    return null;
  }

  @Override
  protected String getPodName(String pathInfo)
  {
    if (pathInfo == null) {
      return "";
    }
    
    int p = pathInfo.indexOf('/', 1);
    
    if (p > 0) {
      return pathInfo.substring(1, p);
    }
    else {
      return pathInfo.substring(1);
    }
  }
  
  @Override
  protected ChannelServerFactoryImpl
  createChannelFactory(Supplier<ServiceManagerAmp> ampManager,
                        String podName,
                        ChannelManagerService sessionManager)
  {
    String addressPod = "pod://" + podName;
    
    return new ChannelServerFactoryJampDispatch(ampManager,
                                                      sessionManager,
                                                      addressPod,
                                                      podName);
  }
}
