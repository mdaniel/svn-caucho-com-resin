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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.inject.Module;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.websocket.io.FrameListener;

/**
 * websocket client container
 */
@Module
public class SessionWebSocketBase implements Session, FrameListener {
  private static final L10N L = new L10N(SessionWebSocketBase.class);
  private static final Logger log
      = Logger.getLogger(SessionWebSocketBase.class.getName());
  
  private static HashMap<Class<?>,Encoder> _primEncoderMap = new HashMap<>();
  private static HashMap<Class<?>,Decoder> _primDecoderMap = new HashMap<>();
  
  private final ContainerWebSocketBase _container;
  private final String _uri;
  
  private Map<String,String[]> _paramMap;
  
  private LinkedHashSet<MessageHandler> _handlers
    = new LinkedHashSet<>();
  
  private ArrayList<EncoderItem<?,Encoder>> _encoderList
    = new ArrayList<>();
  
  private ConcurrentArrayList<Decoder> _decoderList
    = new ConcurrentArrayList<>(Decoder.class);
  
  private long _lastActiveTime;
  
  private String _queryString;
  private int _maxTextMessageBufferSize;
  private int _maxBinaryMessageBufferSize;
  
  protected SessionWebSocketBase(ContainerWebSocketBase container, String uri)
  {
    _container = container;
    
    int p = uri.indexOf('?');
    if (p > 0) {
      _queryString = uri.substring(p + 1);
      _uri = uri.substring(0, p);
    }
    else {
      _uri = uri;
    }
    
    _lastActiveTime = CurrentTime.getCurrentTime();
  }
  
  //
  // informational
  //

  @Override
  public String getId()
  {
    return null;
  }
  
  @Override
  public WebSocketContainer getContainer()
  {
    return _container;
  }

  @Override
  public URI getRequestURI()
  {
    try {
      return new URI(_uri);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public String getQueryString()
  {
    return _queryString;
  }

  protected EndpointConfig getEndpointConfig()
  {
    return null;
  }

  @Override
  public List<Extension> getNegotiatedExtensions()
  {
    return new ArrayList<>();
  }

  /* (non-Javadoc)
   * @see javax.websocket.Session#getNegotiatedSubprotocol()
   */
  @Override
  public String getNegotiatedSubprotocol()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<Session> getOpenSessions()
  {
    return _container.getOpenSessions();
  }

  @Override
  public Map<String, String> getPathParameters()
  {
    return new HashMap<>();
  }

  @Override
  public String getProtocolVersion()
  {
    return "13";
  }

  @Override
  public Map<String, List<String>> getRequestParameterMap()
  {
    return new HashMap<>();
  }

  @Override
  public Principal getUserPrincipal()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Object> getUserProperties()
  {
    return new HashMap<>();
  }

  @Override
  public boolean isSecure()
  {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public long getMaxIdleTimeout()
  {
    return 0;
  }

  @Override
  public void setMaxIdleTimeout(long timeout)
  {
  }

  @Override
  public void setMaxTextMessageBufferSize(int length)
  {
    _maxTextMessageBufferSize = length;
  }

  @Override
  public int getMaxTextMessageBufferSize()
  {
    return _maxTextMessageBufferSize;
  }
  
  @Override
  public int getMaxBinaryMessageBufferSize()
  {
    return _maxBinaryMessageBufferSize;
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int length)
  {
    _maxBinaryMessageBufferSize = length;
  }

  //
  // lifecycle
  //

  @Override
  public boolean isOpen()
  {
    return false;
  }

  /**
   * Request a close.
   * 
   * Called by any thread, either the writer or a control thread.
   */
  @Override
  public void close() throws IOException
  {
    close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "ok"));
  }

  /**
   * Request a close with a reason message.
   * 
   * Called by any thread, either the writer or a control thread.
   */
  @Override
  public void close(CloseReason reason) throws IOException
  {
    for (Decoder decoder : _decoderList) {
      decoder.destroy();
    }
    _decoderList.clear();
    
    for (EncoderItem<?,Encoder> encoder : _encoderList) {
      encoder.destroy();
    }
    _encoderList.clear();
  }

  public void error(Throwable exn)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Read callback when a peer sends a close message. Delivered on the
   * read thread.
   */
  //@Override
  public void onClose(CloseReason reason)
  {
    try {
      close(reason);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  //
  // message writing
  //

  /*
   * Basic writer methods.
   * 
   * Called by the writer thread.
   */
  @Override
  public Basic getBasicRemote()
  {
    return null;
  }

  /*
   * Async writer methods.
   * 
   * Called by the writer thread.
   */
  @Override
  public Async getAsyncRemote()
  {
    return null;
  }
  
  protected void testActivity()
  {
    long time = _lastActiveTime;
    long now = CurrentTime.getCurrentTime();
    
    long timeout = getMaxIdleTimeout();
    
    if (timeout > 0 && timeout < now - time) {
      try {
        close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "timeout"));
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
      
      throw new IllegalStateException(L.l("idle timeout: {0}", timeout));
    }
    
    _lastActiveTime = now;
  }
  
  protected void onActivity()
  {
    _lastActiveTime = CurrentTime.getCurrentTime();
  }
  
  //
  // message handlers
  //
  
  @Override
  public void addMessageHandler(MessageHandler handler)
  {
    if (handler instanceof MessageHandler.Whole) {
      Class<?> type = TypeUtil.getParameterType(handler.getClass(),
                                                MessageHandler.Whole.class);
      
      addBasicHandler((MessageHandler.Whole<?>) handler, type);
    }
    else if (handler instanceof MessageHandler.Partial) {
      Class<?> type = TypeUtil.getParameterType(handler.getClass(),
                                                MessageHandler.Partial.class);

      addAsyncHandler((MessageHandler.Partial<?>) handler, type);
    }
    else {
      throw new UnsupportedOperationException(String.valueOf(handler));
    }
    
    _handlers.add(handler);
  }
  
  public void addBasicHandler(MessageHandler.Whole<?> handler,
                                 Class<?> type)
  {
    if (_decoderList.size() > 0) {
      // Decoder decoder = _decoderList.get(0);
      
      addDecoder(handler, _decoderList);
      return;
    }
    
    Decoder decoder = _primDecoderMap.get(type);
    
    if (decoder != null) {
      ArrayList<Decoder> list = new ArrayList();
      list.add(decoder);
      
      addDecoder(handler, list);
      return;
    }
  
    throw new UnsupportedOperationException(handler + " " + getClass().getName() + " " + type.getName());
  }
  
  public void addPartialHandler(MessageHandler.Whole<?> handler,
                                Class<?> type)
  {
    if (_decoderList.size() > 0) {
      Decoder decoder = _decoderList.get(0);
      
      addDecoder(handler, _decoderList);
      return;
    }
    
    Decoder decoder = _primDecoderMap.get(type);
    
    if (decoder != null) {
      ArrayList<Decoder> list = new ArrayList();
      list.add(decoder);
      
      addDecoder(handler, list);
      return;
    }
  
    throw new UnsupportedOperationException(handler + " " + getClass().getName() + " " + type.getName());
  }
  
  private void addDecoder(MessageHandler.Whole<?> handler, 
                          List<Decoder> decoderList)
  {
    Decoder decoder = decoderList.get(0);
    
    if (decoder instanceof Decoder.BinaryStream<?>) {
      Decoder.BinaryStream<?> streamDecoder = (Decoder.BinaryStream<?>) decoder;
      
      addBasicHandler(new DecoderBinaryStreamHandler(handler, streamDecoder), 
                      InputStream.class);
    }
    else if (decoder instanceof Decoder.Binary<?>) {
      //Decoder.Binary<?> binaryDecoder = (Decoder.Binary<?>) decoder;
      
      addBasicHandler(new DecoderBinaryHandler(handler, decoderList), 
                      ByteBuffer.class);
    }
    else if (decoder instanceof Decoder.TextStream<?>) {
      Decoder.TextStream<?> textDecoder = (Decoder.TextStream<?>) decoder;
      
      addBasicHandler(new DecoderReaderHandler(handler, textDecoder), 
                      Reader.class);
    }
    else if (decoder instanceof Decoder.Text<?>) {
      //Decoder.Text<?> textDecoder = (Decoder.Text<?>) decoder;
      
      addBasicHandler(new DecoderTextHandler(handler, decoderList), 
                      String.class);
    }
    else {
      throw new UnsupportedOperationException(String.valueOf(decoder));
      
    }
  }
  
  protected void addAsyncHandler(MessageHandler.Partial<?> handler,
                                 Class<?> type)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected Class<?> getMessageType(MessageHandler handler, Class<?> api)
  {
    HashMap<String,Type> paramMap = new HashMap<>();
    
    Class<?> type = getMessageType(handler.getClass(), api, paramMap);

    if (type == null) {
      throw new ConfigException(L.l("{0} doesn't have a proper parameter",
                                    handler));
    }
    
    return type;
  }
  
  private Class<?> getMessageType(Type type,
                                  Class<?> api,
                                  HashMap<String,Type> paramMap)
  {
    if (type == null) {
      return null;
    }
    
    if (type instanceof Class<?>) {
      Class<?> cl = (Class<?>) type;
      
      if (cl.equals(api)) {
        TypeVariable<?> []vars = api.getTypeParameters();
        
        return (Class<?>) paramMap.get(vars[0].getName());
      }

      for (Type iface : cl.getGenericInterfaces()) {
        Class<?> messageType = getMessageType(iface, api, paramMap);
        
        if (messageType != null) {
          return messageType;
        }
      }
      
      return getMessageType(cl.getSuperclass(), api, paramMap);
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      
      HashMap<String,Type> newParamMap = new HashMap<>(paramMap);
      Class<?> rawType = (Class<?>) pType.getRawType();
      Type []actualTypes = pType.getActualTypeArguments();
      TypeVariable<?> []vars = rawType.getTypeParameters();
      
      for (int i = 0; i < vars.length; i++) {
        String var = vars[i].getName();
        
        newParamMap.put(var, actualTypes[i]);
      }
      
      return getMessageType(rawType, api, newParamMap);
    }
    
    return null;
  }

  @Override
  public Set<MessageHandler> getMessageHandlers()
  {
    return new HashSet<>(_handlers);
  }

  @Override
  public void removeMessageHandler(MessageHandler listener)
  {
    _handlers.remove(listener);
  }
  
  //
  // encoder/decoder
  //
  
  protected <T extends Encoder> void addEncoder(T encoder)
  {
    encoder.init(getEndpointConfig());

    if (encoder instanceof Encoder.Text) {
      _encoderList.add(new EncoderItem(encoder, Encoder.Text.class));
    }
    else if (encoder instanceof Encoder.TextStream) {
      _encoderList.add(new EncoderItem(encoder, Encoder.TextStream.class));
    }
    else if (encoder instanceof Encoder.Binary) {
      _encoderList.add(new EncoderItem(encoder, Encoder.Binary.class));
    }
    else if (encoder instanceof Encoder.BinaryStream) {
      _encoderList.add(new EncoderItem(encoder, Encoder.BinaryStream.class));
    }
    else {
      throw new UnsupportedOperationException(encoder.getClass().getName());
    }
  }
  
  public Encoder findEncoder(Class<?> type)
  {
    int bestCost = Integer.MAX_VALUE;
    Encoder bestEncoder = null;
    
    for (EncoderItem<?,Encoder> item : _encoderList) {
      int cost = item.getCost(type);
      
      if (cost < bestCost) {
        cost = bestCost;
        bestEncoder = item.getCoder();
      }
    }
    
    if (bestEncoder != null) {
      return bestEncoder;
    }
    
    Encoder encoder = _primEncoderMap.get(type);
    
    if (encoder != null) {
      return encoder;
    }
    
    if (ByteBuffer.class.isAssignableFrom(type)) {
      return _primEncoderMap.get(ByteBuffer.class);
    }
    
    return null;
  }
  
  public void setDecoders(List<Decoder> decoders)
  {
    _decoderList.clear();
    
    _decoderList.addAll(decoders);
    
    for (Decoder decoder : decoders) {
      decoder.init(getEndpointConfig());
    }
  }
  
  public Decoder []getDecoderArray()
  {
    return _decoderList.toArray();
  }
  
  //
  // read callbacks
  //

  /**
   * A 'ping' message is received from the peer.
   * 
   * Called on the read thread.
   */
  @Override
  public void onPing(byte[] buffer, int offset, int length)
  {
  }
  
  /**
   * A 'pong' message is received from the peer.
   * 
   * Called on the read thread.
   */
  @Override
  public void onPong(byte[] buffer, int offset, int length)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getRequestURI() + "]";
  }
  
  static class DecoderBinaryStreamHandler<T>
    implements MessageHandler.Whole<InputStream> {
    private final MessageHandler.Whole<T> _handler;
    private final Decoder.BinaryStream<T> _decoder;
    
    DecoderBinaryStreamHandler(MessageHandler.Whole<T> handler,
                               Decoder.BinaryStream<T> decoder)
    {
      _handler = handler;
      _decoder = decoder;
    }

    @Override
    public void onMessage(InputStream is)
    {
      try {
        _handler.onMessage(_decoder.decode(is));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  static class DecoderBinaryHandler<T>
    implements MessageHandler.Whole<ByteBuffer> {
    private final MessageHandler.Whole<T> _handler;
    private final Decoder.Binary<T> []_decoders;
    
    DecoderBinaryHandler(MessageHandler.Whole<T> handler,
                               List<Decoder> decoderList)
    {
      _handler = handler;
      _decoders = new Decoder.Binary[decoderList.size()];
      
      decoderList.toArray(_decoders);
    }

    @Override
    public void onMessage(ByteBuffer data)
    {
      for (Decoder.Binary<T> decoder : _decoders) {
        if (decoder.willDecode(data)) {
          _handler.onMessage(decoder.decode(data));
        }
      }
      
      log.finer("Undecoded data " + data + " " + _decoders);
    }
  }
  
  static class DecoderTextHandler<T>
    implements MessageHandler.Whole<String> {
    private final MessageHandler.Whole<T> _handler;
    private final Decoder.Text<T> []_decoders;
    
    DecoderTextHandler(MessageHandler.Whole<T> handler,
                       List<Decoder> decoderList)
    {
      _handler = handler;
      
      _decoders = new Decoder.Text[decoderList.size()];
      
      for (int i = 0; i < _decoders.length; i++) {
        _decoders[i] = (Decoder.Text) decoderList.get(i);
      }
    }
    
    @Override
    public void onMessage(String data)
    {
      for (Decoder.Text<T> decoder : _decoders) {
        if (decoder.willDecode(data)) {
          _handler.onMessage(decoder.decode(data));
          return;
        }
      }
      
      throw new UnsupportedOperationException(data);
    }
  }
  
  static class DecoderReaderHandler<T>
    implements MessageHandler.Whole<Reader> {
    private final MessageHandler.Whole<T> _handler;
    private final Decoder.TextStream<T> _decoder;
    
    DecoderReaderHandler(MessageHandler.Whole<T> handler,
                         Decoder.TextStream<T> decoder)
    {
      _handler = handler;
      _decoder = decoder;
    }

    @Override
    public void onMessage(Reader data)
    {
      try {
        _handler.onMessage(_decoder.decode(data));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  private static class PrimEncoderBase implements Encoder
  {
    @Override
    public void init(EndpointConfig config)
    {
    }

    @Override
    public void destroy()
    {
    }
  }
  
  private static class PrimEncoderBaseText<T> extends PrimEncoderBase
  implements Encoder.Text<T>
  {
    @Override
    public String encode(T object) throws EncodeException
    {
      return String.valueOf(object);
    }
  }
  
  private static class PrimEncoderBoolean extends PrimEncoderBaseText<Boolean>
  {
  }
  
  private static class PrimEncoderChar extends PrimEncoderBaseText<Character>
  {
  }
  
  private static class PrimEncoderByte extends PrimEncoderBaseText<Byte>
  {
  }
  
  private static class PrimEncoderShort extends PrimEncoderBaseText<Short>
  {
  }
  
  private static class PrimEncoderInteger extends PrimEncoderBaseText<Integer>
  {
  }
  
  private static class PrimEncoderLong extends PrimEncoderBaseText<Long>
  {
  }
  
  private static class PrimEncoderFloat extends PrimEncoderBaseText<Float>
  {
  }
  
  private static class PrimEncoderDouble extends PrimEncoderBaseText<Double>
  {
  }
  
  private static class PrimEncoderString extends PrimEncoderBaseText<String>
  {
  }
  
  private static class PrimEncoderByteArray extends PrimEncoderBase
    implements Encoder.BinaryStream<byte[]>
  {
    @Override
    public void encode(byte[] buffer, OutputStream os)
      throws IOException, EncodeException
    {
      os.write(buffer);
    }
  }
  
  private static class PrimEncoderByteBuffer extends PrimEncoderBase
    implements Encoder.Binary<ByteBuffer>
  {
    @Override
    public ByteBuffer encode(ByteBuffer value) throws EncodeException
    {
      return value;
    }
  }
  
  private abstract static class PrimDecoderBase<T> implements Decoder.Text<T>
  {
    @Override
    public void init(EndpointConfig config)
    {
    }

    @Override
    public void destroy()
    {

    }

    @Override
    abstract public T decode(String s) throws DecodeException;

    @Override
    public boolean willDecode(String s)
    {
      return true;
    }
  }
  
  private static class PrimDecoderBoolean extends PrimDecoderBase<Boolean>
  {
    @Override
    public Boolean decode(String s) throws DecodeException
    {
      return Boolean.valueOf(s);
    }
  }
  
  private static class PrimDecoderChar extends PrimDecoderBase<Character>
  {
    @Override
    public Character decode(String s) throws DecodeException
    {
      if (s == null || s.length() < 1) {
        return null;
      }
      
      return s.charAt(0);
    }
  }
  
  private static class PrimDecoderByte extends PrimDecoderBase<Byte>
  {
    @Override
    public Byte decode(String s) throws DecodeException
    {
      return Byte.valueOf(s);
    }
  }
  
  private static class ShortPrimDecoder extends PrimDecoderBase<Short>
  {
    @Override
    public Short decode(String s) throws DecodeException
    {
      return Short.valueOf(s);
    }
  }
  
  private static class IntegerPrimDecoder extends PrimDecoderBase<Integer>
  {
    @Override
    public Integer decode(String s) throws DecodeException
    {
      return Integer.valueOf(s);
    }
  }
  
  private static class PrimDecoderLong extends PrimDecoderBase<Long>
  {
    @Override
    public Long decode(String s) throws DecodeException
    {
      return Long.valueOf(s);
    }
  }
  
  private static class PrimDecoderFloat extends PrimDecoderBase<Float>
  {
    @Override
    public Float decode(String s) throws DecodeException
    {
      return Float.valueOf(s);
    }
  }
  
  private static class PrimDecoderDouble extends PrimDecoderBase<Double>
  {
    @Override
    public Double decode(String s) throws DecodeException
    {
      return Double.valueOf(s);
    }
  }
  
  static {
    _primDecoderMap.put(boolean.class, new PrimDecoderBoolean());
    _primDecoderMap.put(Boolean.class, new PrimDecoderBoolean());
    _primDecoderMap.put(char.class, new PrimDecoderChar());
    _primDecoderMap.put(Character.class, new PrimDecoderChar());
    
    _primDecoderMap.put(byte.class, new PrimDecoderByte());
    _primDecoderMap.put(Byte.class, new PrimDecoderByte());
    _primDecoderMap.put(short.class, new ShortPrimDecoder());
    _primDecoderMap.put(Short.class, new ShortPrimDecoder());
    _primDecoderMap.put(int.class, new IntegerPrimDecoder());
    _primDecoderMap.put(Integer.class, new IntegerPrimDecoder());
    _primDecoderMap.put(long.class, new PrimDecoderLong());
    _primDecoderMap.put(Long.class, new PrimDecoderLong());
    _primDecoderMap.put(float.class, new PrimDecoderFloat());
    _primDecoderMap.put(Float.class, new PrimDecoderFloat());
    _primDecoderMap.put(double.class, new PrimDecoderDouble());
    _primDecoderMap.put(Double.class, new PrimDecoderDouble());
    
    _primEncoderMap.put(boolean.class, new PrimEncoderBoolean());
    _primEncoderMap.put(Boolean.class, new PrimEncoderBoolean());
    _primEncoderMap.put(char.class, new PrimEncoderChar());
    _primEncoderMap.put(Character.class, new PrimEncoderChar());
    
    _primEncoderMap.put(byte.class, new PrimEncoderByte());
    _primEncoderMap.put(Byte.class, new PrimEncoderByte());
    _primEncoderMap.put(short.class, new PrimEncoderShort());
    _primEncoderMap.put(Short.class, new PrimEncoderShort());
    _primEncoderMap.put(int.class, new PrimEncoderInteger());
    _primEncoderMap.put(Integer.class, new PrimEncoderInteger());
    _primEncoderMap.put(long.class, new PrimEncoderLong());
    _primEncoderMap.put(Long.class, new PrimEncoderLong());
    _primEncoderMap.put(float.class, new PrimEncoderFloat());
    _primEncoderMap.put(Float.class, new PrimEncoderFloat());
    _primEncoderMap.put(double.class, new PrimEncoderDouble());
    _primEncoderMap.put(Double.class, new PrimEncoderDouble());
    
    _primEncoderMap.put(String.class, new PrimEncoderString());
    _primEncoderMap.put(byte[].class, new PrimEncoderByteArray());
    _primEncoderMap.put(ByteBuffer.class, new PrimEncoderByteBuffer());
  }

  /* (non-Javadoc)
   * @see com.caucho.v5.websocket.io.FrameListener#onClose(com.caucho.v5.websocket.common.CloseReason)
   */
  @Override
  public void onClose(com.caucho.v5.websocket.common.CloseReason closeReason)
  {
    // TODO Auto-generated method stub
    
  }
}
