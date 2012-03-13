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
import java.io.InputStream;
import java.util.ArrayList;

import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.DeliveryAccepted;
import com.caucho.amqp.io.DeliveryModified;
import com.caucho.amqp.io.DeliveryRejected;
import com.caucho.amqp.io.DeliveryReleased;
import com.caucho.amqp.io.DeliveryState;
import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.amqp.io.FrameTransfer;
import com.caucho.amqp.io.LinkSource;
import com.caucho.amqp.io.LinkTarget;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.message.SettleMode;

/**
 * channel session management
 */
public class AmqpSession
{
  private AmqpConnectionHandler _conn;
  
  private ArrayList<AmqpLink> _incomingLinks = new ArrayList<AmqpLink>();
  private ArrayList<AmqpLink> _outgoingLinks = new ArrayList<AmqpLink>();
  
  private TransferSettleManager<AmqpLink> _receiverSettle
    = new TransferSettleManager<AmqpLink>();
  
  private TransferSettleManager<AmqpLink> _senderSettle
    = new TransferSettleManager<AmqpLink>();
  
  AmqpSession(AmqpConnectionHandler conn)
  {
    _conn = conn;
  }
  
  public int getOutgoingIndex()
  {
    return 0;
  }
  
  //
  // link attachment
  //

  public boolean addSenderLink(AmqpSenderLink link)
  {
    addOutgoingLink(link);
    link.setSession(this);
    
    FrameAttach attach = new FrameAttach();
    attach.setName(link.getName());
    attach.setHandle(link.getOutgoingHandle());
    attach.setRole(Role.SENDER);
    
    /*
    switch (link.getSettleMode()) {
    
    }
    */
    System.out.println("SEND: " + link.getOutgoingHandle());
    
    LinkSource source = new LinkSource();
    attach.setSource(source);
    
    LinkTarget target = new LinkTarget();
    target.setAddress(link.getAddress());
    attach.setTarget(target);
    System.out.println("SENDER:");
    
    _conn.getWriter().sendFrame(attach);
      
    return true;
  }

  public boolean addReceiverLink(AmqpReceiverLink link)
  {
    addOutgoingLink(link);
    link.setSession(this);
    
    FrameAttach attach = new FrameAttach();
    attach.setName(link.getName());
    attach.setHandle(link.getOutgoingHandle());
    attach.setRole(Role.RECEIVER);
    
    LinkSource source = new LinkSource();
    source.setAddress(link.getAddress());
    attach.setSource(source);
    
    LinkTarget target = new LinkTarget();
    attach.setTarget(target);
    
    _conn.getWriter().sendFrame(attach);
      
    return true;
  }

  void onAttach(AmqpLink link)
  {
    link.setSession(this);
    
    addIncomingLink(link);
    addOutgoingLink(link);
    
    FrameAttach attach = new FrameAttach();
    
    attach.setName(link.getName());
    attach.setHandle(link.getOutgoingHandle());
    
    attach.setRole(link.getRole());
    System.out.println("ATT: " + link.getName() + " " + link.getRole() + " " + link.getIncomingHandle() + " " + link.getOutgoingHandle());
    
    _conn.getWriter().sendFrame(attach);
  }

  void addIncomingLink(AmqpLink link)
  {
    int handle = link.getIncomingHandle();
    
    while (_incomingLinks.size() <= handle) {
      _incomingLinks.add(null);
    }
    
    _incomingLinks.set(handle, link);
  }
  
  AmqpLink getIncomingLink(int handle)
  {
    return _incomingLinks.get(handle);
  }
  
  private void addOutgoingLink(AmqpLink link)
  {
    for (int i = 0; i < _outgoingLinks.size(); i++) {
      if (_outgoingLinks.get(i) == null) {
        _outgoingLinks.set(i, link);
        link.setOutgoingHandle(i);
        return;
      }
    }
    
    link.setOutgoingHandle(_outgoingLinks.size());
    _outgoingLinks.add(link);
    System.out.println("OUTGOING: " + link.getOutgoingHandle());
  }
  
  public AmqpLink detachOutgoingLink(int handle)
  {
    AmqpLink link = _outgoingLinks.get(handle);
    
    _outgoingLinks.set(handle, null);
    
    return link;
  }
  
  public AmqpLink getOutgoingLink(int handle)
  {
    return _outgoingLinks.get(handle);
  }
  
  AmqpLink findOutgoingLink(String name)
  {
    for (AmqpLink link : _outgoingLinks) {
      if (link != null && name.equals(link.getName())) {
        return link;
      }
    }
    
    return null;
  }
  
  //
  // message transfer
  //
  
  /**
   * Sends a message to the network.
   */
  public void transfer(AmqpSenderLink link,
                       long mid,
                       SettleMode settleMode,
                       InputStream is)
  {
    long deliveryId = addSenderSettle(link, mid, settleMode);
    
    System.out.println("XFER: " + deliveryId + " " + settleMode);
    
    _conn.getWriter().transfer(this, link,
                               deliveryId,
                               settleMode,
                               is);
  }

  private long addSenderSettle(AmqpLink link,
                               long messageId, 
                               SettleMode settleMode)
  {
    return _senderSettle.addDelivery(link, messageId, settleMode);
  }

  /**
   * Receive a message fragment from the network
   */
  void onTransfer(FrameTransfer transfer, AmqpReader ain)
    throws IOException
  {
    int handle = transfer.getHandle();
    
    AmqpLink link = getIncomingLink(handle);

    link.onTransfer(transfer, ain);
  }

  long addReceiverSettle(AmqpLink link,
                         long messageId, 
                         SettleMode settleMode)
  {
    return _receiverSettle.addDelivery(link, messageId, settleMode);
  }
   
  //
  // settle disposition
  //

  public void accepted(long deliveryId)
  {
    DeliveryState accepted = DeliveryAccepted.VALUE;
    
    boolean isSettled = false;
    _conn.getWriter().sendDisposition(this, deliveryId, accepted, isSettled);
  }

  public void outgoingAccepted(long deliveryId)
  {
    DeliveryState accepted = DeliveryAccepted.VALUE;
    
    boolean isSettled = true;
    _conn.getWriter().sendDisposition(this, deliveryId, accepted, isSettled);
  }

  /**
   * @param handle
   */
  public void rejected(long deliveryId, String errorMessage)
  {
    DeliveryRejected rejected = new DeliveryRejected();
      
    if (errorMessage != null) {
      AmqpError error = new AmqpError();

      error.setCondition("rejected");
      error.setDescription(errorMessage);
        
      rejected.setError(error);
    }
    
    boolean isSettled = true;
    _conn.getWriter().sendDisposition(this, deliveryId, rejected, isSettled);
  }

  /**
   * @param handle
   */
  public void modified(long deliveryId,
                       boolean isFailed,
                       boolean isUndeliverableHere)
  {
    DeliveryModified modified = new DeliveryModified();
      
    modified.setDeliveryFailed(isFailed);
    modified.setUndeliverableHere(isUndeliverableHere);
    
    boolean isSettled = false;
    _conn.getWriter().sendDisposition(this, deliveryId, modified, isSettled);
  }

  /**
   * @param handle
   */
  public void released(long deliveryId)
  {
    DeliveryReleased released = DeliveryReleased.VALUE;
    
    boolean isSettled = false;
    _conn.getWriter().sendDisposition(this, deliveryId, released, isSettled);
  }
 
  //
  // flow
  //
  
  void flow(AmqpLink link, long deliveryCount, int credit)
  {
    _conn.getWriter().sendFlow(this, link, deliveryCount, credit);
    
  }

  public void onFlow(FrameFlow flow)
  {
    int handle = flow.getHandle();
    
    AmqpLink link = getIncomingLink(handle);
    
    link.onFlow(flow);
  }

  public void onSenderDisposition(long xid,
                                    DeliveryState state, 
                                    long first, long last)
  {
    _receiverSettle.onDisposition(xid, state, first, last);
  }

  public void onReceiverDisposition(long xid,
                                    DeliveryState state, 
                                    long first, long last)
  {
    _senderSettle.onDisposition(xid, state, first, last);
  }

  /**
   * @param deliveryId
   */
  public void onAccepted(long deliveryId)
  {
    System.out.println("ON_ACCEPT: " + deliveryId);
    // TODO Auto-generated method stub
    
  }

  /**
   * @param deliveryId
   * @param msg
   */
  public void onRejected(long deliveryId, String msg)
  {
    // TODO Auto-generated method stub
    
  }
}
