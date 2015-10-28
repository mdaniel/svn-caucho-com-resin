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
import java.io.OutputStream;
import java.util.Map;

import javax.websocket.RemoteEndpoint;

import com.caucho.nautilus.EncoderMessage;
import com.caucho.nautilus.MessagePropertiesFactory;
import com.caucho.v5.vfs.TempBuffer;

/**
 * nautilus websocket stream writer.
 */
public class WriterNautilusImpl implements WriterNautilus
{
  private RemoteEndpoint.Basic _remote;
  
  private TempBuffer _tBuf = TempBuffer.allocate();
  
  WriterNautilusImpl(RemoteEndpoint.Basic remote)
  {
    _remote = remote;
  }
  
  @Override
  public void sendReceiver(String queue, Map<String,String> props)
    throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.RECEIVER.ordinal());
        
      write(os, "name:");
      write(os, queue);
      write(os, "\n");
      
      for (Map.Entry<String,String> entry : props.entrySet()) {
        write(os, entry.getKey());
        write(os, ": ");
        write(os, entry.getValue());
        write(os, "\n");
      }
    }
  }
  
  @Override
  public void sendReceiverAck(String id)
    throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.RECEIVER.ordinal());
        
      write(os, "id:");
      write(os, id);
      write(os, "\n");
    }
  }
  
  public void sendSender(String queue)
    throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.SENDER.ordinal());
      
      write(os, "name:");
      write(os, queue);
      write(os, "\n");
    }
  }

  @Override
  public void sendStreamMessage(long messageId, 
                                InputStream is,
                                long contentLength)
    throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.SEND.ordinal());
    
      byte []buffer = _tBuf.getBuffer();
      int len;

      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        os.write(buffer, 0, len);
      }
    }
  }
  
  @Override
  public <T> void sendValueMessage(MessagePropertiesFactory<T> factory,
                                   T value,
                                   EncoderMessage<T> encoder,
                                   long timeoutMicros)
    throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.SEND.ordinal());

      encoder.encode(os, value);
    }
  }

  @Override
  public void sendFlow(long credit)
      throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.FLOW.ordinal());
      
      writeLong(os, credit);
    }
  }

  @Override
  public void sendAck(long messageId)
      throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.ACCEPT.ordinal());
      
      writeLong(os, messageId);
    }
  }

  @Override
  public void sendAckAck(long messageId)
      throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.ACCEPT_ACK.ordinal());
      
      writeLong(os, messageId);
    }
  }

  @Override
  public void sendClose()
      throws IOException
  {
    try (OutputStream os = _remote.getSendStream()) {
      os.write(OpcodeNautilus.CLOSE.ordinal());
    }
  }
  
  public void close()
  {
    
  }
  /*
  private void writeInt(OutputStream os, int value)
    throws IOException
  {
    os.write(value >> 24);
    os.write(value >> 16);
    os.write(value >> 8);
    os.write(value);
  }
  */
  private void writeLong(OutputStream os, long value)
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
  
  private void write(OutputStream os, String msg)
    throws IOException
  {
    int len = msg.length();
    
    for (int i = 0; i < len; i++) {
      os.write(msg.charAt(i));
    }
  }
}
