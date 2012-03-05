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

import java.util.ArrayList;

import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.DeliveryAccepted;
import com.caucho.amqp.io.DeliveryModified;
import com.caucho.amqp.io.DeliveryRejected;
import com.caucho.amqp.io.DeliveryReleased;
import com.caucho.amqp.io.DeliveryState;
import com.caucho.amqp.io.FrameFlow;

/**
 * channel session management
 */
public class AmqpSession<L extends AmqpLink>
{
  private long _deliveryId = 1;
  
  private ArrayList<L> _incomingLinks = new ArrayList<L>();
  private ArrayList<L> _outgoingLinks = new ArrayList<L>();
  
  private DeliveryNode _head;
  private DeliveryNode _tail;
  
  public void addIncomingLink(int handle, L link)
  {
    while (_incomingLinks.size() <= handle) {
      _incomingLinks.add(null);
    }
    
    _incomingLinks.set(handle, link);
  }
  
  public L getIncomingLink(int handle)
  {
    return _incomingLinks.get(handle);
  }
  
  public int nextHandle()
  {
    for (int i = 0; i < _outgoingLinks.size(); i++) {
      if (_outgoingLinks.get(i) == null) {
        return i;
      }
    }
    
    return _outgoingLinks.size();
  }
  
  public void addOutgoingLink(int handle, L link)
  {
    while (_outgoingLinks.size() <= handle) {
      _outgoingLinks.add(null);
    }
    
    _outgoingLinks.set(handle, link);
  }
  
  public L detachOutgoingLink(int handle)
  {
    L link = _outgoingLinks.get(handle);
    
    _outgoingLinks.set(handle, null);
    
    return link;
  }
  
  public L getOutgoingLink(int handle)
  {
    return _outgoingLinks.get(handle);
  }
  
  public L findOutgoingLink(String name)
  {
    for (L link : _outgoingLinks) {
      if (link != null && name.equals(link.getName())) {
        return link;
      }
    }
    
    return null;
  }

  public void onFlow(FrameFlow flow)
  {
    int handle = flow.getHandle();
    
    AmqpLink link = getIncomingLink(handle);
    
    link.onFlow(flow);
  }
  
  public long addDelivery(L link, long messageId, boolean isSettled)
  {
    long deliveryId = _deliveryId++;
    
    if (! isSettled) {
      DeliveryNode node = new DeliveryNode(deliveryId, link, messageId);
    
      if (_tail != null) {
        _tail.setNext(node);
      }
      else {
        _head = node;
      }
    }
    
    return deliveryId;
  }

  public void onAccept(long xid)
  {
    System.out.println(this + " accept " + xid);
    DeliveryNode node = _head;
    
    if (node != null) {
      _head = node.getNext();
      
      if (_head == null)
        _tail = null;
      
      AmqpLink link = node.getLink();
      
      link.onAccept(xid, node.getMessageId());
    }
  }

  public void reject(long xid, long first, long last, String message)
  {
    DeliveryNode node = _head;
    
    if (node != null) {
      _head = node.getNext();
      
      if (_head == null)
        _tail = null;
      
      AmqpLink link = node.getLink();
      
      link.reject(xid, node.getMessageId(), message);
    }
  }

  public void release(long xid, long first, long last)
  {
    DeliveryNode node = _head;
    
    if (node != null) {
      _head = node.getNext();
      
      if (_head == null)
        _tail = null;
      
      AmqpLink link = node.getLink();
      
      link.release(xid, node.getMessageId());
    }
  }

  public void modified(long xid,
                       long first, long last,
                       boolean isFailed,
                       boolean isUndeliverableHere)
  {
    DeliveryNode node = _head;
    
    if (node != null) {
      _head = node.getNext();
      
      if (_head == null)
        _tail = null;
      
      AmqpLink link = node.getLink();
      
      link.modified(xid, node.getMessageId(), isFailed, isUndeliverableHere);
    }
  }
  
  public void onDisposition(long first, long last, DeliveryState state)
  {
    long xid = 0;
    
    if (state instanceof DeliveryAccepted) {
      onAccept(xid);
    }
    else if (state instanceof DeliveryRejected) {
      DeliveryRejected rejected = (DeliveryRejected) state;
      
      AmqpError error = rejected.getError();
      
      String message = null;
      
      if (error != null) {
        message = error.getCondition() + ": " + error.getDescription();
      }
      
      reject(xid, first, last, message);
    }
    else if (state instanceof DeliveryModified) {
      DeliveryModified modified = (DeliveryModified) state;
      
      modified(xid, first, last,
               modified.isDeliveryFailed(),
               modified.isUndeliverableHere());
    }
    else if (state instanceof DeliveryReleased) {
      release(xid, first, last);
    }
    else {
      System.out.println("UNKNOWN");
    }
  }

  private static class DeliveryNode {
    private final long _deliveryId;
    private final AmqpLink _link;
    private final long _messageId;
    
    private DeliveryNode _next;
    
    DeliveryNode(long deliveryId, 
                 AmqpLink link,
                 long messageId)
    {
      _deliveryId = deliveryId;
      _link = link;
      _messageId = messageId;
    }
    
    public long getDeliveryId()
    {
      return _deliveryId;
    }
    
    public long getMessageId()
    {
      return _messageId;
    }
    
    public AmqpLink getLink()
    {
      return _link;
    }
    
    public void setNext(DeliveryNode next)
    {
      _next = next;
    }
    
    public DeliveryNode getNext()
    {
      return _next;
    }
  }
}
