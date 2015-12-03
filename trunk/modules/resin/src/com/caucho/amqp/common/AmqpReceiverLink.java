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
  
  private long _transferCount;
  private long _transferCountSnapshot;
  private long _takeCount;
  
  private long _peerDeliveryCount;
  
  private long _deliveryLimit;
  
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
  // after attachment
  //

  /**
   * Called after the attach has been sent to the peer.
   */
  @Override
  public void afterAttach()
  {
    sendPrefetch(_prefetch);
  }
  
  protected int getPrefetchAvailable()
  {
    return _prefetch;
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
    updateTransfer();
  }
  
  protected final void updateTransfer()
  {
    _transferCount++;
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
    long takeCount = ++_takeCount;
    
    int prefetch = _prefetch;
    
    long limit = _deliveryLimit;
    long available = limit - _transferCount;
    
    if ((takeCount & 0x3f) == 0 || 2 * available < prefetch || prefetch < 8) {
      sendPrefetch(_prefetch);
    }
  }

  @Override
  public void setPeerDeliveryCount(long deliveryCount)
  {
    _peerDeliveryCount = deliveryCount;
    _transferCountSnapshot = _transferCount;
  }

  public final void setPrefetch(int prefetch)
  {
    _prefetch = prefetch;
  }

  public void updatePrefetch(int prefetch)
  {
    setPrefetch(prefetch);
    
    sendPrefetch(prefetch);
  }
  
  private void sendPrefetch(int prefetch)
  {
    long receiveCount = _transferCount;
    long receiveCountSnapshot = _transferCountSnapshot;
    
    int localQueueSize = (int) (receiveCount - _takeCount);
    
    long receiveDelta = receiveCount - receiveCountSnapshot;
    
    long peerDeliveryCount = _peerDeliveryCount + receiveDelta;
    
    _deliveryLimit = receiveCount + prefetch;

    getSession().flow(this, peerDeliveryCount, prefetch - localQueueSize);
  }
}
