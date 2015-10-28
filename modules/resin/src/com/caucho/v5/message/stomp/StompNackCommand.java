/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.message.stomp;

import java.io.IOException;

import com.caucho.v5.nautilus.broker.SenderSettleHandler;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.WriteStream;

/**
 * Unsubscribe from a stomp message destination.
 */
public class StompNackCommand extends StompCommand
{
  @Override
  boolean doCommand(StompConnection conn, ReadStream is, WriteStream os)
    throws IOException
  {
    String subscription = conn.getSubscription();
    long mid = conn.getMessageId();
    SenderSettleHandler listener = conn.createReceiptCallback();
                       
    if (subscription == null)
      throw new IOException("bad id");
    
    if (mid <= 0)
      throw new IOException("bad mid");
                       
    if (! skipToEnd(is))
      return false;
    
    boolean isValid = conn.nack(subscription, mid);
    
    if (listener != null) {
      if (isValid)
        listener.onAccepted(mid);
      else
        listener.onRejected(mid, "cannot ack from " + subscription);
    }
    
    return true;
  }
  
}
