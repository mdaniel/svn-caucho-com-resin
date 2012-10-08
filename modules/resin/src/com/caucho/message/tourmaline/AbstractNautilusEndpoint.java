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

package com.caucho.message.tourmaline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

/**
 * Abstract websocket receive endpoint.
 */
abstract class AbstractNautilusEndpoint extends AbstractWebSocketListener
{
  private static final Command []_dispatch;
  
  private WebSocketContext _wsContext;

  final WebSocketContext getContext()
  {
    return _wsContext;
  }

  @Override
  public void onStart(WebSocketContext context)
    throws IOException
  {
    _wsContext = context;
  }
  
  //
  // receive callback
  //

  @Override
  public void onReadBinary(WebSocketContext context, InputStream is)
    throws IOException
  {
    int code = is.read();
    
    _dispatch[code].onMessage(this, is);
  }

  @Override
  public void onClose(WebSocketContext context)
    throws IOException
  {
    onClose();
  }
  
  protected void publishStart(InputStream is)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected void receiveStart(InputStream is)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected void onSend(InputStream is)
    throws IOException
  {
  }
  
  private final void onFlow(InputStream is)
    throws IOException
  {
    int credit = readInt(is);
    long sequence = readLong(is);
    
    onFlow(credit, sequence);
  }
  
  protected void onFlow(int credit, long sequence)
  {
  }
  
  protected void onClose()
  {
  }
  
  protected final int readInt(InputStream is)
    throws IOException
  {
    return ((is.read() << 24)
        + (is.read() << 16)
        + (is.read() << 8)
        + (is.read()));
  }
  
  protected final long readLong(InputStream is)
    throws IOException
  {
    return (((long) is.read() << 56)
        + ((long) is.read() << 48)
        + ((long) is.read() << 40)
        + ((long) is.read() << 32)
        + ((long) is.read() << 24)
        + ((long) is.read() << 16)
        + ((long) is.read() << 8)
        + ((long) is.read()));
  }
  
  protected void writeInt(OutputStream os, int value)
    throws IOException
  {
    os.write(value >> 24);
    os.write(value >> 16);
    os.write(value >> 8);
    os.write(value);
  }
  
  protected void writeLong(OutputStream os, long value)
    throws IOException
  {
    os.write((int) (value >> 56));
    os.write((int) (value >> 48));
    os.write((int) (value >> 40));
    os.write((int) (value >> 32));
    os.write((int) (value >> 24));
    os.write((int) (value >> 16));
    os.write((int) (value >> 8));
    os.write((int) (value));
  }
  
  protected void write(OutputStream os, String msg)
    throws IOException
  {
    int len = msg.length();
    
    for (int i = 0; i < len; i++) {
      os.write(msg.charAt(i));
    }
  }

  public void close()
  {
    WebSocketContext cxt = _wsContext;
    _wsContext = null;
    
    // XXX: should be using clean shutdown with messages
    if (cxt != null) {
      cxt.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  private abstract static class Command {
    abstract void onMessage(AbstractNautilusEndpoint endpoint,
                            InputStream is)
      throws IOException;
  }
  
  private static class NullCommand extends Command {
    @Override
    void onMessage(AbstractNautilusEndpoint endpoint,
                   InputStream is)
      throws IOException
    {
    }
  }
  
  private static class PublishCommand extends Command {
    @Override
    void onMessage(AbstractNautilusEndpoint endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.publishStart(is);
    }
  }
  
  private static class ReceiveCommand extends Command {
    @Override
    void onMessage(AbstractNautilusEndpoint endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.receiveStart(is);
    }
  }
  
  private static class SendCommand extends Command {
    @Override
    void onMessage(AbstractNautilusEndpoint endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onSend(is);
    }
  }
  
  private static class FlowCommand extends Command {
    @Override
    void onMessage(AbstractNautilusEndpoint endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onFlow(is);
    }
  }

  static {
    _dispatch = new Command[NautilusCode.CLOSE.ordinal() + 1];
    _dispatch[NautilusCode.NULL.ordinal()] = new NullCommand();
    _dispatch[NautilusCode.PUBLISH.ordinal()] = new PublishCommand();
    _dispatch[NautilusCode.RECEIVE.ordinal()] = new ReceiveCommand();
    _dispatch[NautilusCode.SEND.ordinal()] = new SendCommand();
    _dispatch[NautilusCode.FLOW.ordinal()] = new FlowCommand();
  }
}
