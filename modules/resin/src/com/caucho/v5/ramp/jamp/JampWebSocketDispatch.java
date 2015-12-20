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

import io.baratine.core.ServiceException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.remote.ChannelServer;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.ramp.jamp.JampPodManager.PodContext;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.LruCache.Entry;
import com.caucho.v5.websocket.server.WebSocketServletDispatch;
import com.caucho.v5.websocket.server.WebSocketServletDispatch.WebSocketContextDispatch;

/**
 * Dispatching of a Jamp request to websocket.
 */
public class JampWebSocketDispatch
{
  private static final L10N L = new L10N(JampWebSocketDispatch.class);
  private static final Logger log 
    = Logger.getLogger(JampWebSocketDispatch.class.getName());
  
  private SessionContextJamp _sessionContext;
  
  private final LruCache<String,ChannelServer> _sessionMap
    = new LruCache<>(16 * 1024);
  
  private ConcurrentHashMap<String,WebSocketContextPod> _wsMap
    = new ConcurrentHashMap<>();
  
  private final Lifecycle _lifecycle = new Lifecycle();
  
  private long _alarmTimeout = 5000L;
  
  private Alarm _alarm = new Alarm(new ChannelTimeout());

  private JampPodManager _podManager;
  private WebSocketServletDispatch _webSocket;

  public JampWebSocketDispatch(JampPodManager podManager)
  {
    _podManager = podManager;
    
    _webSocket = new WebSocketServletDispatch();
    _webSocket.setContextFactory(x->getContext(x));
  }

  public static JampWebSocketDispatch createLocal(ServletContext ctx)
  {
    synchronized (ctx) {
      String id = JampWebSocketDispatch.class.getName();
      
      JampWebSocketDispatch wsDispatch
        = (JampWebSocketDispatch) ctx.getAttribute(id);
      
      if (wsDispatch != null) {
        return wsDispatch;
      }
      
      wsDispatch = new JampWebSocketDispatch(new JampPodManager());
      
      ctx.setAttribute(id, wsDispatch);
      
      return wsDispatch;
    }
  }
 
  public void init()
  {
    _lifecycle.toActive();
    
    // _failover = RampFailoverStatus.getCurrent();
    
    _alarm.queue(_alarmTimeout);
  }
  
  public void destroy()
  {
    _lifecycle.toDestroy();
    
    _alarm.dequeue();
  }
  
  public EndpointJampConfig getConfig()
  {
    return (EndpointJampConfig) getContext("").getConfig();
  }
  
  public WebSocketContextDispatch getContext(String pathInfo)
  {
    String podName = getPodName(pathInfo);

    Objects.requireNonNull(podName);
    
    WebSocketContextPod wsCxtPod = _wsMap.get(podName);
    
    if (wsCxtPod == null || wsCxtPod.isClosed()) {
      PodContext podContext = _podManager.getPodContextByName(podName);
      
      if (podContext.getAmpManager() == null) {
        throw new ServiceException(L.l("'{0}' is an inactive pod", podName));
      }
      
      EndpointJampConfigServer config;
      
      config = new EndpointJampConfigServer(podName,
                                            podContext.getAmpManager(),
                                            getChannelContext(),
                                            podContext.getWsRegistryFactory(),
                                            podContext.getJsonFactory());
      
      WebSocketContextDispatch wsCxt = new WebSocketContextDispatch(config);
      
      WebSocketContextPod wsCxtPodNew
        = new WebSocketContextPod(wsCxt, podContext.getAmpManager());

      if (wsCxtPod != null) {
        _wsMap.replace(podName, wsCxtPod, wsCxtPodNew);
      }
      else {
        _wsMap.putIfAbsent(podName, wsCxtPodNew);
      }
      
      wsCxtPod = _wsMap.get(podName);
    }

    return wsCxtPod.getContext();
  }
  
  private String getPodName(String pathInfo)
  {
    return "";
  }
  
  private SessionContextJamp getChannelContext()
  {
    if (_sessionContext == null) {
      _sessionContext = new SessionContextJamp();
    }
    
    return _sessionContext;
  }
  
  private class ChannelTimeout implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        ArrayList<String> brokerList = null;
        
        long now = CurrentTime.getCurrentTime();
        
        Iterator<Entry<String, ChannelServer>> iter = _sessionMap.iterator();
        
        while (iter.hasNext()) {
          Entry<String,ChannelServer> entry = iter.next();

          ChannelServerJampPull broker = (ChannelServerJampPull) entry.getValue();
          
          broker.timeoutConnection(now);
          
          if (broker.timeoutSession(now)) {
            if (brokerList == null) {
              brokerList = new ArrayList<>();
            }
            
            brokerList.add(entry.getKey());
          }
        }
        
        if (brokerList != null) {
          for (String key : brokerList) {
            ChannelServer channel = _sessionMap.remove(key);
            
            try {
              channel.shutdown(ShutdownModeAmp.GRACEFUL);
            } catch (Exception e) {
              log.log(Level.FINER, e.toString(), e);
            }
          }
        }
      } finally {
        if (_lifecycle.isActive()) {
          alarm.queue(_alarmTimeout);
        }
      }
    }
  }
  
  private static class WebSocketContextPod {
    private WebSocketContextDispatch _wsContext;
    private ServiceManagerAmp _ampManager;
    
    WebSocketContextPod(WebSocketContextDispatch wsContext, 
                        ServiceManagerAmp ampManager)
    {
      _wsContext = wsContext;
      _ampManager = ampManager;
    }
    
    public WebSocketContextDispatch getContext()
    {
      return _wsContext;
    }
    
    public boolean isClosed()
    {
      return _ampManager.isClosed();
    }
  }
}
