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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.DeliveryState;
import com.caucho.message.SettleMode;

/**
 * node for an unsettled transfer
 */
public class DeliveryNode
{
  private static final Logger log
    = Logger.getLogger(DeliveryNode.class.getName());
  
  private final long _deliveryId;
  private final AmqpLink _link;
  private final long _messageId;
  private final SettleMode _settleMode;

  DeliveryNode(long deliveryId, 
               AmqpLink link,
               long messageId,
               SettleMode settleMode)
  {
    _deliveryId = deliveryId;
    _link = link;
    _messageId = messageId;
    _settleMode = settleMode;
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

  public void onAccepted(long xid)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(_link + " onAccepted(" + _messageId + ")");
    }
    
    if (_settleMode == SettleMode.EXACTLY_ONCE) {
      _link.getSession().outgoingAccepted(_messageId);
    }
    
    _link.onAccepted(xid, _messageId);
  }

  public void onReleased(long xid)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(_link + " onReleased(" + _messageId + ")");
    }
    
    _link.onReleased(xid, _messageId);
  }

  public void onRejected(long xid, AmqpError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(_link + " onRejected(" + _messageId + "," + error + ")");
    }
    System.out.println("ONR " + _link + " " + error);
    _link.onRejected(xid, _messageId, error);
  }

  public void onModified(long xid, 
                         boolean isFailed, 
                         boolean isUndeliverableHere)
  {
    _link.onModified(xid, _messageId, isFailed, isUndeliverableHere);
  }

  public void onReceived(long xid)
  {
  }

  public void onXa(long xid, byte[] txnId, DeliveryState outcome)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _deliveryId + "]";
  }
}
