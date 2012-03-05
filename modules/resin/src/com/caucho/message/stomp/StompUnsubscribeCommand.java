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

package com.caucho.message.stomp;

import java.io.IOException;

import com.caucho.message.broker.SenderSettleHandler;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Unsubscribe from a stomp message destination.
 */
public class StompUnsubscribeCommand extends StompCommand
{
  @Override
  boolean doCommand(StompConnection conn, ReadStream is, WriteStream os)
    throws IOException
  {
    String id = conn.getId();
    SenderSettleHandler listener = conn.createReceiptCallback();
    long mid = 0;
                       
    if (id == null)
      throw new IOException("bad id");
                       
    if (! skipToEnd(is))
      return false;
    
    boolean isValid = conn.unsubscribe(id);
    
    if (listener != null) {
      if (isValid)
        listener.onAccepted(mid);
      else
        listener.onRejected(mid, "cannot unsubscribe from " + id);
    }
    
    return true;
  }
  
}
