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

package com.caucho.server.spdy;

import java.io.IOException;

import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;
import com.caucho.env.actor.ActorQueue;
import com.caucho.util.RingItemFactory;
import com.caucho.vfs.WriteStream;

/**
 * SPDY connection
 */
class SpdyWriteActor extends AbstractActorProcessor<SpdyWriteItem>
  implements RingItemFactory<SpdyWriteItem>
{
  private static final int VERSION = 3;
  
  private final SpdyConnection _conn;
  private final ActorQueue<SpdyWriteItem> _writeQueue;
  
  SpdyWriteActor(SpdyConnection conn)
  {
    _conn = conn;
    
    _writeQueue = new ActorQueue<SpdyWriteItem>(256, this, this);
  }

  @Override
  public String getThreadName()
  {
    return _conn.getClass().getSimpleName() + '-' + Thread.currentThread().getId();
  }

  @Override
  public void process(SpdyWriteItem item) throws Exception
  {
    int type = item.getType();
    
    switch (type) {
    case SpdyConnection.SYN_REPLY:
      writeReply(item.getStream());
      break;
      
    default:
      _conn.close();
      break;
    }
    
    item.clear();
  }
  
  private void writeReply(SpdyStream stream)
    throws IOException
  {
    WriteStream os = _conn.getWriteStream();
    
    os.write(0x80);
    os.write(VERSION);
    os.write(0x00);
    os.write(SpdyConnection.SYN_REPLY);
    
    int offset = os.getBufferOffset();
    
    int len = 8;
    writeInt(os, len);

    writeInt(os, stream.getClientId());
    writeInt(os, 0);
  }
  
  private void writeInt(WriteStream os, int value)
    throws IOException
  {
    os.write(value >> 24);
    os.write(value >> 16);
    os.write(value >> 8);
    os.write(value);
  }

  @Override
  public void onProcessComplete() throws Exception
  {
    _conn.getWriteStream().flush();
  }

  @Override
  public SpdyWriteItem createItem(int index)
  {
    return new SpdyWriteItem(index);
  }

  /**
   * @param stream
   */
  public void writeStreamReply(SpdyStream stream)
  {
    SpdyWriteItem item = _writeQueue.startOffer(true);
    
    item.setType(SpdyConnection.SYN_REPLY);
    item.setStream(stream);
    
    _writeQueue.finishOffer(item);
  }
} 
