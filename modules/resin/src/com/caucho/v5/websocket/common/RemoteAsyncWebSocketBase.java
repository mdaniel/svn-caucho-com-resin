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
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import com.caucho.v5.inject.Module;
import com.caucho.v5.util.BasicFuture;
import com.caucho.v5.vfs.TempBuffer;

/**
 * stub/adapter for RemoteEndpoint.Async
 */
@Module
public class RemoteAsyncWebSocketBase 
  extends RemoteWebSocketBase
  implements RemoteEndpoint.Async
{
  private static final Logger log
    = Logger.getLogger(RemoteAsyncWebSocketBase.class.getName());
  
  private long _sendTimeout;

  @Override
  public void setSendTimeout(long timeout)
  {
    _sendTimeout = timeout;
  }

  @Override
  public long getSendTimeout()
  {
    return _sendTimeout;
  }

  @Override
  public Future<Void> sendBinary(ByteBuffer data)
  {
    BasicFuture<Void> future = new BasicFuture<>();
    
    try (OutputStream os = getSendStream()) {
      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.buffer();
      
      int len;
      
      while ((len = data.remaining()) > 0) {
        len = Math.min(len, buffer.length);
        
        data.get(buffer, 0, len);

        os.write(buffer, 0, len);
      }

      TempBuffer.free(tBuf);
      
      future.complete(null);
    } catch (IOException e) {
      future.fail(e);
    }
    
    return future;
  }

  @Override
  public void sendBinary(ByteBuffer data, SendHandler handler)
  {
    try (OutputStream os = getSendStream()) {
      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.buffer();
      
      int len;
      
      while ((len = data.remaining()) > 0) {
        len = Math.min(len, buffer.length);
        
        data.get(buffer, 0, len);

        os.write(buffer, 0, len);
      }

      TempBuffer.free(tBuf);
      
      handler.onResult(new SendResult());
    } catch (IOException e) {
      handler.onResult(new SendResult(e));
    }
  }

  @Override
  public Future<Void> sendObject(Object data)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void sendObject(Object data, SendHandler handler)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void sendText(String text, SendHandler handler)
  {
    try (Writer out = getSendWriter()) {
      out.append(text);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    handler.onResult(new SendResult());
  }

  @Override
  public Future<Void> sendText(String text)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected Writer getSendWriter() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected OutputStream getSendStream() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
