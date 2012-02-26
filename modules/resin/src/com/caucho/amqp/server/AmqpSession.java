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

package com.caucho.amqp.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.amqp.io.FrameOpen;
import com.caucho.amqp.io.AmqpAbstractFrame;
import com.caucho.amqp.io.AmqpFrameReader;
import com.caucho.amqp.io.AmqpFrameWriter;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.AmqpFrameHandler;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.SaslMechanisms;
import com.caucho.amqp.io.SaslOutcome;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * channel session management
 */
public class AmqpSession
{
  private long _deliveryId = 1;
  
  private ArrayList<AmqpLink> _links = new ArrayList<AmqpLink>();
  
  private DeliveryNode _head;
  private DeliveryNode _tail;
  
  public void addLink(AmqpLink link)
  {
    int handle = link.getHandle();
    
    while (_links.size() <= handle) {
      _links.add(null);
    }
    
    _links.set(handle, link);
    System.out.println("SET: " + handle);
  }
  
  public AmqpLink getLink(int handle)
  {
    return _links.get(handle);
  }
  
  void onFlow(FrameFlow flow)
  {
    int handle = flow.getHandle();
    
    AmqpLink link = getLink(handle);
    
    link.onFlow(flow);
  }
  
  long addDelivery(AmqpLink link, long messageId)
  {
    long deliveryId = _deliveryId++;
    
    DeliveryNode node = new DeliveryNode(deliveryId, link, messageId);
    
    if (_tail != null) {
      _tail.setNext(node);
    }
    else {
      _head = node;
    }
    
    return deliveryId;
  }

  void accept(long xid)
  {
    DeliveryNode node = _head;
    
    if (node != null) {
      _head = node.getNext();
      
      if (_head == null)
        _tail = null;
      
      AmqpLink link = node.getLink();
      
      link.accept(xid, node.getMessageId());
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
