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

import java.io.InputStream;

import com.caucho.amqp.io.FrameFlow;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.message.SettleMode;

/**
 * link management
 */
abstract public class AmqpSenderLink extends AmqpLink
{
  private long _deliveryCount;
  private long _deliveryLimit;
  
  private int _peerLinkCredit;
  
  protected AmqpSenderLink(String name, String address)
  {
    super(name, address);
  }

  @Override
  public final Role getRole()
  {
    return Role.SENDER;
  }
  
  public final long getDeliveryCount()
  {
    return _deliveryCount;
  }
  
  public final int getLinkCredit()
  {
    long linkCredit = _deliveryLimit - _deliveryCount;
    
    return Math.max((int) linkCredit, 0);
  }
  
  //
  // message transfer
  //

  public void transfer(long mid,
                       SettleMode settleMode,
                       InputStream is)
  {
    _deliveryCount++;
    
    getSession().transfer(this, mid, settleMode, is);
  }
  
  //
  // flow control
  //

  /**
   * When peer sends its link credit, update deliveryLimit. 
   */
  @Override
  public void onFlow(FrameFlow flow)
  {
    long peerDeliveryCount = flow.getDeliveryCount();
    int peerLinkCredit = flow.getLinkCredit();
    
    long deliveryCount = _deliveryCount;
    if (peerDeliveryCount < 0) {
      _deliveryLimit = deliveryCount + peerLinkCredit; 
    }
    else {
      long delta = Math.max(deliveryCount - peerDeliveryCount, 0);
      long linkCredit = Math.max(peerLinkCredit - delta, 0);
      
      _deliveryLimit = _deliveryCount + linkCredit;
    }
  }
}
