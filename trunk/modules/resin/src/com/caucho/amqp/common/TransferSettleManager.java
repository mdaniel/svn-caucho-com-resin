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

import com.caucho.amqp.io.DeliveryState;
import com.caucho.message.SettleMode;

/**
 * manages transfers and settlements.
 */
public class TransferSettleManager<L extends AmqpLink>
{
  private long _deliveryId = 1;

  private DeliveryNode []_unsettledList = new DeliveryNode[128];
  
  private int _unsettledCount;
  private int _unsettledHead;
  private int _unsettledTail;
  
  public long addDelivery(L link,
                          long messageId, 
                          SettleMode settleMode)
  {
    long deliveryId = _deliveryId++;
    
    if (settleMode == SettleMode.ALWAYS){
      return deliveryId;
    }
    
    DeliveryNode node = new DeliveryNode(deliveryId, link, messageId, settleMode);
    
    // XXX: extend
    _unsettledList[_unsettledHead++] = node;
    
    return deliveryId;
  }

  public void onDisposition(long xid, 
                            DeliveryState deliveryState,
                            long first, long last)
  {
    DeliveryNode []unsettledList = _unsettledList;
    int head = _unsettledHead;
    
    for (int i = _unsettledTail; i < head; i++) {
      DeliveryNode node = unsettledList[i];
      
      if (node == null) {
        continue;
      }
      
      long deliveryId = node.getDeliveryId();
      
      if (last < deliveryId) {
        return;
      }
      
      if (deliveryId < first) {
        continue;
      }
      
      unsettledList[i] = null;
        
      deliveryState.update(xid, node);
    }
  }
}
