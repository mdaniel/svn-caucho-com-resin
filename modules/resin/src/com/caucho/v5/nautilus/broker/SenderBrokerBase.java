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

package com.caucho.v5.nautilus.broker;

import com.caucho.v5.io.TempBuffer;

/**
 * Custom serialization for the cache
 */
public class SenderBrokerBase implements SenderBroker
{
  @Override
  public String getId()
  {
    return "test";
  }
  
  @Override
  public int getPrefetch()
  {
    return 256;
  }
  
  public long nextMessageId()
  {
    return 0;
  }
  
  @Override
  public void message(long xid,
                      long mid,
                      boolean isDurable,
                      int priority,
                      long expireTime,
                      byte []buffer,
                      int offset,
                      int length,
                      TempBuffer tBuf,
                      SenderSettleHandler listener)
  {
    if (listener != null) {
      listener.onAccepted(mid);
    }
  }
  
  @Override
  public void disconnect()
  {
    close();
  }
  
  @Override
  public void close()
  {
    
  }
}
