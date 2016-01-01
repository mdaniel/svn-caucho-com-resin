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

import com.caucho.v5.nautilus.broker.SenderBroker;
import com.caucho.v5.vfs.TempBuffer;

/**
 * xa item for a send
 */
class StompXaSend extends StompXaItem
{
  private SenderBroker _dest;
  
  private TempBuffer _tBuf;
  private int _length;
  
  StompXaSend(SenderBroker dest, TempBuffer tBuf, int length)
  {
    _dest = dest;
    _tBuf = tBuf;
    _length = length;
  }
  
  @Override
  boolean doCommand(StompConnection conn)
  {
    boolean isDurable= false;
    int priority = -1;
    long expireTime = 0;
    long mid = 0;
    
    _dest.message(conn.getXid(), mid, isDurable, priority, expireTime,
                  _tBuf.buffer(), 0, _length, _tBuf, null);
    
    return true;
  }
}
