/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.jms.amq;

import java.io.*;
import java.util.logging.*;

import com.caucho.util.*;

/**
 * AMQ channel.
 */
public class AmqChannel implements AmqConstants {
  private static final Logger log
    = Logger.getLogger(AmqChannel.class.getName());
  
  protected AmqConnection _conn;
  private int _id;

  protected boolean _isOpen;
  protected boolean _isClosed;

  protected long _bodySize;
  protected ChunkItem _contentHead;
  protected ChunkItem _contentTail;
  
  AmqChannel(AmqConnection conn)
  {
    _conn = conn;
  }

  void setId(int id)
  {
    _id = id;
  }

  int getId()
  {
    return _id;
  }
  
  boolean doOpenOk(InputStream is)
    throws IOException
  {
    return fatal("doOpenOk() should not be called");
  }
  
  boolean doQueueDeclare(InputStream is)
    throws IOException
  {
    return fatal("doQueueDeclare() should not be called");
  }
  
  boolean doQueueDeclareOk(InputStream is)
    throws IOException
  {
    return fatal("doQueueDeclare() should not be called");
  }
  
  boolean doBasicPublish(InputStream is)
    throws IOException
  {
    return fatal("doBasicPublish() should not be called");
  }
  
  boolean doHeader(int classId, int weight, int bodySize, InputStream is)
    throws IOException
  {
    System.out.println("HEADER: " + classId + " " + bodySize);

    _bodySize = bodySize;
    _contentHead = _contentTail = null;
    
    return true;
  }
  
  void addChunk(Chunk chunk, int offset, int length)
    throws IOException
  {
    _bodySize -= length;

    ChunkItem item = new ChunkItem(chunk, offset, length);

    if (_contentTail != null) {
      _contentTail.setNext(item);
      _contentTail = item;
    }
    else {
      _contentHead = _contentTail = item;
    }
  }

  void endContentFrame()
    throws IOException
  {
    System.out.println("END_FRAME:" + _bodySize);
    if (_bodySize <= 0) {
      ChunkInputStream is = new ChunkInputStream(_contentHead);
      _contentHead = _contentTail = null;

      doContentEnd(is);
    }
  }

  void doContentEnd(InputStream is)
    throws IOException
  {
    // the channel will handle the appropriate action by scheduling
    // a thread from ThreadPool depending on the action.
    throw new IllegalStateException();
  }

  boolean fatal(String msg)
  {
    log.fine(msg);
    
    System.out.println(msg);

    close();

    return false;
  }
  
  public void close()
  {
    synchronized (this) {
      boolean isClosed = _isClosed;
      _isClosed = true;
      _isOpen = false;

      if (isClosed)
	return;
    }
  }

  static class ChunkItem {
    private final Chunk _chunk;
    private final int _offset;
    private final int _length;

    private ChunkItem _next;

    ChunkItem(Chunk chunk, int offset, int length)
    {
      _chunk = chunk;
      _offset = offset;
      _length = length;
    }

    void setNext(ChunkItem next)
    {
      _next = next;
    }
  }

  static class ChunkInputStream extends InputStream {
    private ChunkItem _head;
    private int _offset;

    ChunkInputStream(ChunkItem head)
    {
      _head = head;
    }

    public int read()
    {
      return -1;
    }
  }
}
