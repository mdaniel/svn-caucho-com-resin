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
import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.io.WriteBuffer;
import com.caucho.v5.nautilus.broker.SenderBroker;
import com.caucho.v5.nautilus.broker.SenderSettleHandler;

/**
 * Custom serialization for the cache
 */
public class StompSendCommand extends StompCommand
{
  @Override
  boolean doCommand(StompConnection conn, ReadBuffer is, WriteBuffer os)
    throws IOException
  {
    SenderBroker dest = conn.getDestination();
    
    long contentLength = conn.getContentLength();
    String contentType = conn.getContentType();
    String xa = conn.getTransaction();
    SenderSettleHandler receipt = conn.createReceiptCallback();
    
    StompXaSend xaSend = null;
    
    int ch;
    long mid = 0;
    
    if (contentLength >= 0) {
      int offset = 0;
      
      while (offset < contentLength) {
        int sublen = (int) (contentLength - offset);
        
        TempBuffer tBuf = TempBuffer.allocate();
        byte []buffer = tBuf.buffer();
        
        if (buffer.length < sublen)
          sublen = buffer.length;
        
        sublen = is.readAll(buffer, 0, sublen);
        
        if (sublen < 0) {
          throw new IOException("eof protocol");
        }
    
        if (xa == null) {
          boolean isFinal = (offset + sublen == contentLength);
          boolean isDurable = false;
          int priority = -1;
          long expireTime = 0;
          
          dest.message(conn.getXid(), mid, isDurable, priority, expireTime,
                       tBuf.buffer(), 0, sublen, tBuf, receipt);
        }
        else {
          xaSend = new StompXaSend(dest, tBuf, sublen);
        }
        
        offset += sublen;
      }
      
      ch = is.read();
    }
    else {
      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.buffer();
      
      boolean isDurable = false;
      int priority = -1;
      long expireTime = 0;
      
      int offset = 0;
      
      for (ch = is.read(); ch > 0; ch = is.read()) {
        buffer[offset++] = (byte) ch;
        
        if (offset == buffer.length) {
          dest.message(conn.getXid(), mid, isDurable, priority, expireTime,
                       tBuf.buffer(), 0, offset, tBuf, receipt);

          tBuf = TempBuffer.allocate();
          offset = 0;
        }
      }
      
      if (xa == null) {
        dest.message(conn.getXid(), mid, isDurable, priority, expireTime,
                     tBuf.buffer(), 0, offset, tBuf, receipt);
      }
      else {
        xaSend = new StompXaSend(dest, tBuf, offset);
      }
    }
    
    if (ch != 0)
      throw new IOException("protocol");
    
    if (xaSend != null) {
      conn.addXaItem(xaSend);
      
      if (receipt != null)
        receipt.onAccepted(mid);
    }
    
    return true;
  }
  
}
