/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.log;

import com.caucho.util.FreeList;

import java.util.concurrent.Semaphore;

/**
 * Holds the HTTP buffers for keepalive reuse.  Because a request needs a
 * large number of buffers, but a keepalive doesn't need those buffers,
 * Resin can recycle the buffers during keepalives to keep the memory
 * consumption low.
 */
public final class LogBuffer
{
  private static final Semaphore _logSemaphore = new Semaphore(16 * 1024);
  private static final FreeList<LogBuffer> _freeList
    = new FreeList<LogBuffer>(256);
				    
  private final byte []_logBuffer = new byte[1024];
  private int _length;

  private LogBuffer _next;

  private LogBuffer()
  {
  }

  public static LogBuffer allocate()
  {
    try {
      Thread.interrupted();
      _logSemaphore.acquire();
    } catch (Exception e) {
    }
    
    LogBuffer buffer = _freeList.allocate();

    if (buffer == null)
      buffer = new LogBuffer();

    return buffer;
  }

  public final byte []getBuffer()
  {
    return _logBuffer;
  }

  public final void setLength(int length)
  {
    _length = length;
  }

  public final int getLength()
  {
    return _length;
  }

  public final LogBuffer getNext()
  {
    return _next;
  }

  public final void setNext(LogBuffer next)
  {
    _next = next;
  }

  public void free()
  {
    _logSemaphore.release();

    _next = null;

    _freeList.free(this);
  }
  
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
