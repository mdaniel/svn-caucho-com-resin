/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.amqp.common;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.io.AmqpConnectionReader;
import com.caucho.amqp.io.AmqpConnectionWriter;
import com.caucho.amqp.io.AmqpFrameHandler;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.DeliveryState;
import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameAttach.ReceiverSettleMode;
import com.caucho.amqp.io.FrameAttach.SenderSettleMode;
import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.FrameClose;
import com.caucho.amqp.io.FrameDetach;
import com.caucho.amqp.io.FrameDisposition;
import com.caucho.amqp.io.FrameEnd;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.amqp.io.FrameTransfer;
import com.caucho.amqp.io.LinkTarget;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.amqp.io.LinkSource;
import com.caucho.message.DistributionMode;
import com.caucho.message.SettleMode;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Amqp server connection
 */
public class AmqpConnectionHandler
  implements AmqpFrameHandler
{
  private static final Logger log
    = Logger.getLogger(AmqpConnectionHandler.class.getName());
  
  private final AmqpLinkFactory _linkFactory;
  
  private AmqpConnectionReader _in;
  private AmqpConnectionWriter _out;
  
  private AmqpSession []_sessions = new AmqpSession[1];
  
  public AmqpConnectionHandler(AmqpLinkFactory linkFactory,
                               ReadStream is, 
                               WriteStream os)
  {
    _linkFactory = linkFactory;
    
    _out = new AmqpConnectionWriter(os);
    _in = new AmqpConnectionReader(is, this);
  }
  
  public AmqpConnectionReader getReader()
  {
    return _in;
  }
  
  public AmqpConnectionWriter getWriter()
  {
    return _out;
  }
  
  //
  // session management
  //

  public AmqpSession beginSession()
  {
    _sessions[0] = new AmqpSession(this);
    
    getWriter().writeBegin();
    
    return _sessions[0];
  }

  @Override
  public void onBegin(FrameBegin beginRequest)
    throws IOException
  {
    if (_sessions[0] == null) {
      _sessions[0] = new AmqpSession(this);
    
      FrameBegin beginResponse = new FrameBegin();
    
      _out.sendFrame(beginResponse);
    }
  }
  
  private void endSession(int channel)
  {
    FrameEnd end = new FrameEnd();
    
    _out.sendFrame(end);
  }

  @Override
  public void onEnd(FrameEnd clientEnd)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    _sessions[0] = null;

    if (session != null) {
      FrameEnd sessionEnd = new FrameEnd();
    
      _out.sendFrame(sessionEnd);
    }
  }
  
  //
  // link attachment
  //

  @Override
  public void onAttach(FrameAttach attach)
  {
    AmqpSession session = _sessions[0];
    
    String name = attach.getName();
    
    AmqpLink link = session.findOutgoingLink(name);
    
    if (link != null) {
      link.setIncomingHandle(attach.getHandle());
      session.addIncomingLink(link);
      
      onAttachAck(link, attach);
      return;
    }

    String address;
    SettleMode settleMode;
    
    if (attach.getSenderSettleMode() == SenderSettleMode.SETTLED
        || attach.getSenderSettleMode() == SenderSettleMode.MIXED) {
      settleMode = SettleMode.ALWAYS;
    }
    else if (attach.getReceiverSettleMode() == ReceiverSettleMode.FIRST) {
      settleMode = SettleMode.EXACTLY_ONCE;
    }
    else {
      settleMode = SettleMode.AT_LEAST_ONCE;
    }
   
    if (attach.getRole() == Role.SENDER) {
      LinkTarget target = attach.getTarget();
      address = target.getAddress();
      
      Map<String,Object> targetProperties;
      targetProperties = target.getDynamicNodeProperties();

      link = _linkFactory.createReceiverLink(name, address, targetProperties);
    }
    else {
      LinkSource source = attach.getSource();
      address = source.getAddress();
      DistributionMode distMode = source.getDistributionMode();
      
      Map<String,Object> sourceProperties;
      sourceProperties = source.getDynamicNodeProperties();

      link = _linkFactory.createSenderLink(name, address, distMode, 
                                           settleMode,
                                           sourceProperties);
    }
    
    link.setIncomingHandle(attach.getHandle());
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(link + " onAttach(" + address + "," + settleMode + ")");
    }
    
    session.onAttach(link);
  }

  private void onAttachAck(AmqpLink link, FrameAttach attach)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(link + " onAttachAck()");
    }
    
    if (link.getRole() == Role.RECEIVER) {
      AmqpReceiverLink receiver = (AmqpReceiverLink) link;
      
      receiver.setPeerDeliveryCount(attach.getInitialDeliveryCount());
    }
  }

  public void closeSender(AmqpLink link)
  {
    AmqpSession session = _sessions[0];
    
    int handle = link.getOutgoingHandle();
    
    session.detachOutgoingLink(handle);
    link.setOutgoingHandle(-1);
    
    if (link != null) {
      FrameDetach detach = new FrameDetach();
      detach.setHandle(handle);
      
      _out.sendFrame(detach);
    }
  }

  @Override
  public void onDetach(FrameDetach clientDetach)
    throws IOException
  {
    FrameDetach serverDetach = new FrameDetach();
    
    serverDetach.setHandle(clientDetach.getHandle());
    
    _out.sendFrame(serverDetach);
  }
  
  //
  // message transfer
  //

  @Override
  public void onTransfer(AmqpReader ain, FrameTransfer transfer)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    session.onTransfer(transfer, ain);
  }
  
  //
  // message dispositon
  //
  
  @Override
  public void onDisposition(FrameDisposition disposition)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    DeliveryState state = disposition.getState();
    long xid = 0;
    long first = disposition.getFirst();
    long last = disposition.getLast();
    
    if (disposition.getRole() == Role.SENDER)
      session.onReceiverDisposition(xid, state, first, last);
    else
      session.onReceiverDisposition(xid, state, first, last);
  }
  
  //
  // message flow
  //

  /**
   * @param handle
   */
  public void flow(AmqpLink link, long deliveryCount, int credit)
  {
    FrameFlow flow = new FrameFlow();
    flow.setHandle(link.getOutgoingHandle());
    flow.setDeliveryCount(deliveryCount);
    flow.setLinkCredit(credit);
      
    getWriter().sendFrame(flow);
  }

  @Override
  public void onFlow(FrameFlow flow)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    session.onFlow(flow);
  }
  
  @Override
  public void onClose(FrameClose clientClose)
    throws IOException
  {
    try {
      FrameClose serverClose = new FrameClose();
    
      _out.sendFrame(serverClose);
    } finally {
      disconnect();
    }
  }
  
  public void closeSessions()
  {
    for (int i = 0; i < _sessions.length; i++) {
      AmqpSession session = _sessions[i];
      _sessions[i] = null;
      
      if (session != null) {
        endSession(i);
      }
    }
  }
   
  public void closeConnection()
  {
    FrameClose close = new FrameClose();
    
    _out.sendFrame(close);
  }

  /**
   * 
   */
  private void disconnect()
  {
    // TODO Auto-generated method stub
    
  }
 }
