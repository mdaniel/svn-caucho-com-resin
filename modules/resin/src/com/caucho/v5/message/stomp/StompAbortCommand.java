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

import com.caucho.v5.io.ReadBuffer;
import com.caucho.v5.io.WriteBuffer;
import com.caucho.v5.nautilus.broker.SenderSettleHandler;

/**
 * begins a transaction.
 */
public class StompAbortCommand extends StompCommand
{
  @Override
  boolean doCommand(StompConnection conn, ReadBuffer is, WriteBuffer os)
    throws IOException
  {
    String transaction = conn.getTransaction();
    SenderSettleHandler listener = conn.createReceiptCallback();
    long mid = 0;
                       
    if (transaction == null)
      throw new IOException("bad transaction");
                       
    if (! skipToEnd(is))
      return false;
    
    boolean isValid = conn.abort(transaction);
    
    if (listener != null) {
      if (isValid)
        listener.onAccepted(mid);
      else
        listener.onRejected(mid, "cannot commit " + transaction);
    }
    
    return true;
  }
  
}
