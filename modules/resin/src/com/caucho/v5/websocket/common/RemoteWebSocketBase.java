/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.websocket.common;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.RemoteEndpoint;

import com.caucho.v5.util.ModulePrivate;

/**
 * Stub for RemoteEndpoint methods
 */
@ModulePrivate
public class RemoteWebSocketBase implements RemoteEndpoint
{
  private boolean _isBatching;
  
  @Override
  public boolean getBatchingAllowed()
  {
    return _isBatching;
  }

  @Override
  public void setBatchingAllowed(boolean isAllowed) throws IOException
  {
    _isBatching = isAllowed;
  }

  @Override
  public void flushBatch() throws IOException
  {
  }

  @Override
  public void sendPing(ByteBuffer applicationData) throws IOException
  {
  }

  @Override
  public void sendPong(ByteBuffer applicationData) throws IOException
  {
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

}
