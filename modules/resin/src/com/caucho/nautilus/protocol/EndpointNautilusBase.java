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

package com.caucho.nautilus.protocol;

import java.io.IOException;
import java.io.InputStream;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.ServiceManagerAmp;

/**
 * Abstract websocket receive endpoint.
 */
class EndpointNautilusBase extends Endpoint
  implements MessageHandler.Whole<InputStream>
{
  private static final Command []_dispatch;
  
  private Session _session;

  private WriterNautilus _writer;

  final Session getSession()
  {
    return _session;
  }

  @Override
  public void onOpen(Session session, EndpointConfig config)
  {
    _session = session;
    
    session.addMessageHandler(this);
    
    WriterNautilusImpl writerImpl = new WriterNautilusImpl(session.getBasicRemote());
    
    ServiceManagerAmp ampManager = Amp.newManager();
    
    _writer = ampManager.service(writerImpl).as(WriterNautilus.class);
  }

  @Override
  public void onClose(Session session, CloseReason closeReason)
  {
    onGracefulClose();
    onDisconnect();
  }
  
  public void onError(Throwable t, Session s)
  {
    onDisconnect();
  }

  /**
   * @return
   */
  public WriterNautilus getWriter()
  {
    return _writer;
  }

  //
  // receive callback
  //

  @Override
  public void onMessage(InputStream is)
  {
    try {
      int code = is.read();

      _dispatch[code].onMessage(this, is);
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    long credit = readLong(is);
    
    onFlow(credit);
  }
  
  protected void onFlow(long credit)
  {
  }
  
  private final void onAccept(InputStream is)
    throws IOException
  {
    long messageId = readLong(is);
    
    onAccept(messageId);
  }
  
  protected void onAccept(long messageId)
  {
  }
  
  private final void onAcceptAck(InputStream is)
    throws IOException
  {
    long messageId = readLong(is);
    
    onAcceptAck(messageId);
  }
  
  protected void onAcceptAck(long messageId)
  {
  }
  
  private final void onClose(InputStream is)
    throws IOException
  {
    onGracefulClose();
  }
  
  protected void onGracefulClose()
  {
  }
  
  protected void onDisconnect()
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

  public void close()
  {
    try {
      _session.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  private abstract static class Command {
    abstract void onMessage(EndpointNautilusBase endpoint,
                            InputStream is)
      throws IOException;
  }
  
  private static class NullCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
    }
  }
  
  private static class PublishCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.publishStart(is);
    }
  }
  
  private static class ReceiveCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.receiveStart(is);
    }
  }
  
  private static class SendCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onSend(is);
    }
  }
  
  private static class FlowCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onFlow(is);
    }
  }
  
  private static class AcceptCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onAccept(is);
    }
  }
  
  private static class AcceptAckCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onAcceptAck(is);
    }
  }
  
  private static class CloseCommand extends Command {
    @Override
    void onMessage(EndpointNautilusBase endpoint,
                   InputStream is)
      throws IOException
    {
      endpoint.onClose(is);
    }
  }

  static {
    _dispatch = new Command[OpcodeNautilus.CLOSE.ordinal() + 1];
    _dispatch[OpcodeNautilus.NULL.ordinal()] = new NullCommand();
    
    _dispatch[OpcodeNautilus.SENDER.ordinal()] = new PublishCommand();
    _dispatch[OpcodeNautilus.RECEIVER.ordinal()] = new ReceiveCommand();
    
    _dispatch[OpcodeNautilus.SEND.ordinal()] = new SendCommand();
    
    _dispatch[OpcodeNautilus.FLOW.ordinal()] = new FlowCommand();
    
    _dispatch[OpcodeNautilus.ACCEPT.ordinal()] = new AcceptCommand();
    _dispatch[OpcodeNautilus.ACCEPT_ACK.ordinal()] = new AcceptAckCommand();
    
    _dispatch[OpcodeNautilus.CLOSE.ordinal()] = new CloseCommand();
  }
}
