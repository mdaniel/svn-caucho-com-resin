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

import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.FrameTransfer;
import com.caucho.amqp.io.FrameAttach.Role;

/**
 * link management
 */
abstract public class AmqpReceiverLink extends AmqpLink
{
  private int _prefetch;
  
  private long _outgoingDeliveryCount;
  private long _deliveryLimit;
  private long _takeCount;
  
  protected AmqpReceiverLink(String name, String address)
  {
    super(name, address);
  }

  @Override
  public final Role getRole()
  {
    return Role.RECEIVER;
  }
  
  //
  // message transfer
  //

  /**
   * Message receivers implement this method to receive a
   * message fragment from the network.
   */
  @Override
  protected void onTransfer(FrameTransfer transfer, AmqpReader ain)
    throws IOException
  {
    addDeliveryCount();
  }
  
  //
  // message settle disposition
  //
  
  @Override
  public void accepted(long mid)
  {
    getSession().accepted(mid);
  }
  
  @Override
  public void rejected(long mid, String message)
  {
    getSession().rejected(mid, message);
  }
  
  @Override
  public void released(long mid)
  {
    getSession().released(mid);
  }
  
  @Override
  public void modified(long mid, 
                       boolean isFailure, 
                       boolean isUndeliverableHere)
  {
    getSession().modified(mid, isFailure, isUndeliverableHere);
  }
  
  //
  // message flow
  //
  
  public void updateTake()
  {
    _takeCount++;
    
    long limit = _deliveryLimit;
    long available = limit - _takeCount;
    
    if (2 * available < _prefetch || _prefetch < 8) {
      setPrefetch(_prefetch);
    }
  }
  
  public void setIncomingDeliveryCount(int count)
  {
    super.setIncomingDeliveryCount(count);
    
    // XXX: sync
   // _outgoingDeliveryCount = count;
  }

  public void setPrefetch(int prefetch)
  {
    _prefetch = prefetch;
    
    long receiveCount = getTransferCount();
    int delta = (int) (receiveCount - _takeCount);
    
    long deliveryCount = getIncomingDeliveryCount();
    
    _deliveryLimit = _takeCount + prefetch;

    getSession().flow(this, deliveryCount, prefetch - delta);
  }
}
