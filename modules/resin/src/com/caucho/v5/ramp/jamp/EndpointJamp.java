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

import io.baratine.core.ResultStream;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.remote.ChannelAmp;
import com.caucho.v5.amp.remote.ChannelClient;
import com.caucho.v5.amp.remote.OutAmp;
import com.caucho.v5.amp.remote.OutAmpManager;
import com.caucho.v5.amp.spi.HeadersAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.pod.PodRef;

/**
 * JampWebSocketEndpoint responds to JAMP websocket messages.
 */
public class EndpointJamp extends Endpoint
  implements OutAmp, OutAmpManager
{
  private static final Logger log
    = Logger.getLogger(EndpointJamp.class.getName());
  
  private ServiceManagerAmp _rampManager;
  private ChannelAmp _channelRegistry;
  private ChannelContextJampImpl _channelEnv;
  private SessionContextJamp _channelContext;
  
  private Session _session;
  private InAmpWebSocket _ampReader;
  private OutAmpWebSocket _ampWriter;
  
  
  public EndpointJamp()
  {
  }
  
  /*
  private void init(RampManager rampManager,
                    ChannelServerFactory brokerFactory)
  {
    _rampManager = rampManager;
    _channelBroker = brokerFactory.create(this);
    
    _channelBroker.onLogin("auto");
    
    _jampReader = new JampReader(_rampManager, _channelBroker);
    _jampWriter = new JampWriter();
  }
  */
  
  @Override
  public void onOpen(Session session, EndpointConfig endpointConfig)
  {
    _session = session;
    
    String id = JampWebSocketDispatch.class.getName();
    JampWebSocketDispatch wsDispatch
      = (JampWebSocketDispatch) session.getUserProperties().get(id);
    
    if (endpointConfig instanceof EndpointJampConfig) {
      EndpointJampConfig config = (EndpointJampConfig) endpointConfig;
      
      init(session.getNegotiatedSubprotocol(), config);
    }
    else if (wsDispatch != null) {
      EndpointJampConfig config = wsDispatch.getConfig();
      
      init(session.getNegotiatedSubprotocol(), config);
    }
    else {
      log.warning("Unexpected JAMP endpoint context");

      try {
        session.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    try {
      if (session.getBasicRemote() != null) {
        session.getBasicRemote().setBatchingAllowed(true);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private void init(String subprotocol, EndpointJampConfig config)
  {
    if (subprotocol == null) {
      log.fine("Missing WebSocket subprotocol");
      
      subprotocol = "jamp";
      
      //close();
      //return;
    }
    
    _rampManager = config.getRampManager();
    _channelRegistry = config.createRegistry(this);
    _channelEnv = new ChannelContextJampImpl(_channelRegistry);
    
    _channelContext = config.getChannelContext();
    
    // _channelBroker.onLogin("auto");
    
    switch (subprotocol) {
    case "jamp": {
      InJampWebSocket jampReader;
      jampReader = new InJampWebSocket(_rampManager, _channelRegistry,
                                       _channelContext, // _channelEnv,
                                       config.getJsonFactory());
      
      _session.addMessageHandler(jampReader);
      _ampWriter = new OutJampWebSocket(config.getJsonFactory());
      return;
    }
      
    case "hamp": {
      InHampWebSocket hampReader;
      hampReader = new InHampWebSocket(_rampManager, _channelRegistry,
                                       _channelContext); // , _channelEnv); 
      
      _session.addMessageHandler(hampReader);
      
      _ampReader = hampReader; 
      _ampWriter = new OutHampWebSocket();
      return;
    }

    default:
      log.warning("Unknown WebSocket subprotocol: either 'jamp' or 'hamp' must be specified."
                  + " The default WebSocket subprotocol is not supported.");
      
      close();
    }
  }

  @Override
  public void onClose(Session session, CloseReason closeReason)
  {
    // baratine/2204
    ChannelAmp broker = _channelRegistry;
    _channelRegistry = null;
        
    if (broker != null) {
      broker.shutdown(ShutdownModeAmp.GRACEFUL);
    }

    try {
      session.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
  @Override
  public void onMessage(Reader is)
  {
    try {
      _ampReader.readMessage(is);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINEST)) {
        log.log(Level.FINEST, e.toString(), e);
      }
      else {
        log.fine(e.toString());
      }
    }
  }

  @Override
  public void onMessage(InputStream is)
  {
    try {
      _ampReader.readMessage(is);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINEST)) {
        log.log(Level.FINEST, e.toString(), e);
      }
      else {
        log.fine(e.toString());
      }
    }
  }
  */

  @Override
  public boolean isUp()
  {
    return true;
  }

  @Override
  public void send(HeadersAmp headers, 
                   String address, String methodName, PodRef podCaller,
                   Object[] args)
  {
    _channelContext.start(_channelEnv);
    
    try {
      _ampWriter.send(_session, headers, 
                      address, methodName, podCaller, 
                      args);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      _channelContext.start(null);
    }
  }

  @Override
  public void query(HeadersAmp headers, 
                    String fromAddress, long qid,
                    String address, String methodName, PodRef podCaller, 
                    Object[] args)
  {
    try {
      _ampWriter.query(_session,
                       headers,
                       fromAddress, qid,
                       address, methodName, podCaller,
                       args);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stream(HeadersAmp headers,
                     String fromAddress, long qid,
                     String address, String methodName, PodRef podCaller,
                     ResultStream<?> result,
                     Object[] args)
  {
    try {
      _ampWriter.query(_session,
                       headers,
                       fromAddress,
                       qid,
                       address, methodName, podCaller,
                       args);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void reply(HeadersAmp headers, String address, long qid, Object result)
  {
    try {
      _ampWriter.reply(_session, headers, address, qid, result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void queryError(HeadersAmp headers, String address, long qid,
                         Throwable exn)
  {
    try {
      _ampWriter.queryError(_session, headers, address, qid, exn);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void streamReply(HeadersAmp headers,
                          String address, 
                          long qid,
                          int sequence,
                          List<Object> results,
                          Throwable exn,
                          boolean isComplete)
  {
    try {
      _ampWriter.streamReply(_session, headers, address, qid, sequence, results, exn, isComplete);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void streamCancel(HeadersAmp headers,
                           String address,
                           String addressFrom,
                          long qid)
  {
    System.out.println("STR_CANCEL: " + this);
    /*
    try {
      _ampWriter.reply(_session, headers, address, qid, result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    */
  }

  @Override
  public OutAmp getCurrentOut()
  {
    return this;
  }

  @Override
  public OutAmp getOut(ChannelClient broker)
  {
    return this;
  }
  
  @Override
  public void flush()
  {
    try {
      Session session = _session;
      
      if (session != null && session.isOpen()) {
        Basic remote = session.getBasicRemote();

        if (remote != null) {
          remote.flushBatch();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public void close()
  {
    try {
      _session.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _rampManager + "]";
  }
}
