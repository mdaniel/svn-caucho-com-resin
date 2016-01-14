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

package com.caucho.v5.nautilus.protocol;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * Custom serialization for the cache
 */
class EndpointReceiverClientNautilus<T> extends EndpointNautilusBase
{
  private final ReceiverClientNautilus<T> _receiver;
  
  EndpointReceiverClientNautilus(ReceiverClientNautilus<T> receiver)
  {
    _receiver = receiver;
  }
  
  @Override
  protected void receiveStart(InputStream is)
    throws IOException
  {
    ReadStream in = VfsOld.openRead(is);
    
    String line;
    
    while ((line = in.readLine()) != null) {
      int p = line.indexOf(':');
      
      if (p < 0) {
        continue;
      }
      
      String key = line.substring(0, p).trim();
      String value = line.substring(p + 1).trim();
      
      addReceiveProperty(key, value);
    }
  }
  
  private void addReceiveProperty(String key, String value)
  {
    if ("id".equals(key)) {
      _receiver.setSessionId(value);
    }
  }
  

  @Override
  protected void onSend(InputStream is)
    throws IOException
  {
    T value = _receiver.getDecoder().decode(is);
    
    _receiver.receiveEntry(value);
  }

  @Override
  protected void onAcceptAck(long mid)
  {
    _receiver.acceptedAck(mid);
  }

  @Override
  protected void onDisconnect()
  {
    _receiver.onDisconnect();
  }
}
