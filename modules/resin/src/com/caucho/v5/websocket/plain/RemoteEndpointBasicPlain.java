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

package com.caucho.v5.websocket.plain;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;

import com.caucho.v5.inject.Module;
import com.caucho.v5.util.IoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ClientDisconnectException;
import com.caucho.v5.vfs.TempBuffer;
import com.caucho.v5.vfs.WriteStream;
import com.caucho.v5.websocket.common.RemoteBasicWebSocketBase;
import com.caucho.v5.websocket.io.WebSocketConstants;
import com.caucho.v5.websocket.io.OutWebSocket;
import com.caucho.v5.websocket.io.WebSocketWriter;

/**
 * websocket writing endpoint.
 */
@Module
public class RemoteEndpointBasicPlain extends RemoteBasicWebSocketBase
{
  private static final L10N L = new L10N(RemoteEndpointBasicPlain.class);
  private static final Logger log
    = Logger.getLogger(RemoteEndpointBasicPlain.class.getName());

  private HashMap<Class<?>,PlainEncoder<?>> _encoderMap = new HashMap<>();
  
  private SessionWebSocketPlain _session;
  private WriteStream _os;
  
  private OutWebSocket _binaryOut;
  private WebSocketWriter _textOut;
  
  private boolean _isBatching;
  private boolean _isMasked;

  RemoteEndpointBasicPlain(SessionWebSocketPlain session,
                           WriteStream os)
  {
    _session = session;
    _os = os;
    
    setMasked(session.isMasked());
    //System.err.println("PL: " + System.identityHashCode(os));
    //Thread.dumpStack();
  }

  @Override
  public void setBatchingAllowed(boolean isBatching)
  {
    _isBatching = isBatching;
    
    if (_textOut != null) {
      _textOut.setBatching(isBatching);
    }
    
    if (_binaryOut != null) {
      _binaryOut.setBatching(isBatching);
    }
  }

  public boolean isBatching()
  {
    return _isBatching;
  }
  
  public void setMasked(boolean isMasked)
  {
    _isMasked = isMasked;
  }
  
  @Override
  public void flushBatch()
    throws IOException
  {
    WriteStream os = _os;
    
    if (os != null) {
      os.flush();
    }
  }
  
  //
  // binary
  //

  @Override
  public OutputStream getSendStream()
      throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
    // return startBinaryMessage(true);
  }

  @Override
  public void sendBinary(ByteBuffer buffer)
      throws IOException
  {
    OutWebSocket out = startBinaryMessage(true);
    out.write(buffer);
    buffer.clear();
    out.close();
  }

  @Override
  public void sendBinary(ByteBuffer buffer, boolean isLast)
    throws IOException
  {
    OutWebSocket out = null;

    try {
      out = startBinaryMessage(false);
      out.write(buffer);
      
      if (! isLast) {
        out.flush(); // TCK:
      }
    } finally {
      if (isLast && out != null) {
        out.close();
      }
    }
  }

  public OutWebSocket startBinaryMessage(boolean isWhole)
    throws IOException
  {
    WriteStream out = _os;
    
    if (out == null) {
      throw new IllegalStateException(L.l("{0} is closed for writing.",
                                          this));
    }
    
    if (_binaryOut == null) {
      _binaryOut = new OutWebSocket(out,
                                             TempBuffer.allocate().getBuffer());
      _binaryOut.setMasked(_isMasked);
      
      _binaryOut.setBatching(_isBatching);
    }

    if (isWhole || _binaryOut.isIdle()) {
      _binaryOut.init();
    }
    
    return _binaryOut;
  }

  //
  // text
  //

  @Override
  public Writer getSendWriter()
      throws IOException
  {
    return startTextMessage(true);
  }

  @Override
  public void sendText(String text)
    throws IOException
  {
    try (Writer out = startTextMessage(true)) {
      out.write(text);
    }
  }

  @Override
  public void sendText(String text, boolean isLast)
    throws IOException
  {
    Writer out = null;

    try {
      out = startTextMessage(false);
      out.write(text);
      
      if (! isLast) {
        out.flush(); // TCK:
      }
    } finally {
      if (isLast && out != null) {
        IoUtil.close(out);
      }
    }
  }

  private WebSocketWriter startTextMessage(boolean isWhole)
    throws IOException
  {
    WriteStream out = _os;
    
    if (out == null) {
      throw new IllegalStateException(L.l("{0} is closed for writing.",
                                          this));
    }
    
    if (_textOut == null) {
      _textOut = new WebSocketWriter(out,
                                     TempBuffer.allocate().getBuffer());
      
      _textOut.setBatching(isBatching());
    }
    
    if (isWhole || _textOut.isIdle()) {
      _textOut.init();
    }
    
    return _textOut;
  }
  
  //
  // encoded objects
  //
  
  @Override
  public void sendObject(Object value)
    throws IOException
  {
    encodeObject(value);
  }
  
  private <T> void encodeObject(T value)
    throws IOException
  {
    getEncoder((Class<T>) value.getClass()).send(value);
  }
  
  private <T> PlainEncoder<T> getEncoder(Class<T> cl)
  {
    PlainEncoder<T> encoder = (PlainEncoder) _encoderMap.get(cl);
    
    if (encoder == null) {
      Encoder userEncoder = _session.findEncoder(cl);
      
      if (userEncoder instanceof Encoder.Text) {
        encoder = new PlainEncoderText((Encoder.Text) userEncoder);
      }
      else if (userEncoder instanceof Encoder.TextStream) {
        encoder = new PlainEncoderTextStream((Encoder.TextStream) userEncoder);
      }
      else if (userEncoder instanceof Encoder.Binary) {
        encoder = new PlainEncoderBinary((Encoder.Binary) userEncoder);
      }
      else if (userEncoder instanceof Encoder.BinaryStream) {
        encoder = new PlainEncoderBinaryStream((Encoder.BinaryStream) userEncoder);
      }
      else {
        throw new EncodeException(null,
                                  L.l("{0} is an unsupported encoder",
                                      cl.getName()));
      }
      
      _encoderMap.put(cl, encoder);
    }
    
    return encoder; 
  }
  
  
  
  //
  // ping/pong
  //
  
  @Override
  public void sendPing(ByteBuffer buf)
    throws IOException
  {
    int len = buf.remaining();
    
    WriteStream out = _os;
    
    out.write(0x80 | WebSocketConstants.OP_PING);
    out.write(len);
    out.write(buf, len);
    out.flush();
  }
  
  @Override
  public void sendPong(ByteBuffer buf)
    throws IOException
  {
    int len = buf.remaining();
    
    WriteStream out = _os;
    
    out.write(0x80 | WebSocketConstants.OP_PONG);
    out.write(len);
    out.write(buf, len);
    out.flush();
  }
  
  public void pong(byte []value)
    throws IOException
  {
    WriteStream out = _os;
    
    byte []bytes = value;
        
    out.write(0x8a);
    out.write(bytes.length);
    out.write(bytes);
    out.flush();
  }
  
  public boolean isClosed()
  {
    return _session.isWriteClosed();
  }
  
  public void close()
  {
    // close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "ok"));
  }
    
  void closeWrite(CloseReason reason)
  {
    int code = reason.getCloseCode().getCode();
    String message = reason.getReasonPhrase();
    
    WriteStream out = _os;
    _os = null;

    try {
      if (out != null && ! out.isClosed()) {
        if (code <= 0) {
          out.write(0x88);
          out.write(0x00);
        }
        else {
          byte []bytes = message.getBytes("utf-8");
        
          out.write(0x88);
          out.write(0x02 + bytes.length);
          out.write((code >> 8) & 0xff);
          out.write(code & 0xff);
          out.write(bytes);
        }

        out.flush();
        // out.close();
      }
    } catch (ClientDisconnectException e) {
      log.log(Level.FINEST, e.toString(), e);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  public void disconnect()
    // _isWriteClosed.set(true);
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  class PlainEncoder<T>
  {
    void send(T object)
      throws IOException
    {
      try (Writer out = startTextMessage(true)) {
        out.write(String.valueOf(object));
      }
    }
  }
  
  class PlainEncoderText<T> extends PlainEncoder<T>
  {
    private final Encoder.Text<T> _textEncoder;
    
    PlainEncoderText(Encoder.Text<T> textEncoder)
    {
      _textEncoder = textEncoder;
    }
    
    @Override
    void send(T object)
      throws IOException
    {
      sendText(_textEncoder.encode(object));
    }
  }
  
  class PlainEncoderTextStream<T> extends PlainEncoder<T>
  {
    private final Encoder.TextStream<T> _textEncoder;
    
    PlainEncoderTextStream(Encoder.TextStream<T> textEncoder)
    {
      _textEncoder = textEncoder;
    }
    
    @Override
    void send(T object)
      throws IOException
    {
      try (Writer out = getSendWriter()) {
        _textEncoder.encode(object, out);
      }
    }
  }
  
  class PlainEncoderBinary<T> extends PlainEncoder<T>
  {
    private final Encoder.Binary<T> _encoder;
    
    PlainEncoderBinary(Encoder.Binary<T> encoder)
    {
      _encoder = encoder;
    }
    
    @Override
    void send(T object)
      throws IOException
    {
      sendBinary(_encoder.encode(object));
    }
  }
  
  class PlainEncoderBinaryStream<T> extends PlainEncoder<T>
  {
    private final Encoder.BinaryStream<T> _encoder;
    
    PlainEncoderBinaryStream(Encoder.BinaryStream<T> encoder)
    {
      _encoder = encoder;
    }
    
    @Override
    void send(T object)
      throws IOException
    {
      try (OutputStream os = getSendStream()) {
        _encoder.encode(object, os);
      }
    }
  }
}
