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

package com.caucho.v5.websocket.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Provider;
import javax.websocket.CloseReason;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.inject.InjectManagerAmp;
import com.caucho.v5.reflect.ReflectUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.websocket.common.SessionWebSocketBase;

@ModulePrivate
public class WebSocketEndpointSkeleton<T>
{
  private static final L10N L = new L10N(WebSocketEndpointSkeleton.class);
  private static final Logger log
    = Logger.getLogger(WebSocketEndpointSkeleton.class.getName());

  private static final Decoder []NULL_DECODERS = new Decoder[0];
  private static final Encoder []NULL_ENCODERS = new Encoder[0];

  private final Class<T> _beanClass;

  private final Provider<T> _beanSupplier;

  private ResolvingMethodHandle _openMethod;
  private ResolvingMethodHandle _closeMethod;
  private ResolvingMethodHandle _errorMethod;

  private MessageHandlerFactory<T> _messageHandlerFactoryText;
  private MessageHandlerFactory<T> _messageHandlerFactoryBinary;

  private Decoder []_decoders = NULL_DECODERS;
  private Encoder []_encoders = NULL_ENCODERS;
  private ServerEndpointConfig _config;
  private PathTemplateWebSocket _pathTemplate;

  public WebSocketEndpointSkeleton(Class<T> beanClass)
  {
    _beanClass = beanClass;

    _beanSupplier = InjectManagerAmp.create().supplierCreate(beanClass);

    introspect();
  }

  public WebSocketEndpointSkeleton(Class<T> beanClass,
                                   EndpointConfig config)
  {
    _beanClass = beanClass;

    if (config instanceof ServerEndpointConfig) {
      _config = (ServerEndpointConfig) config;
    }

    _beanSupplier = InjectManagerAmp.create().supplierCreate(beanClass);

    introspect();
  }

  public static <T> Endpoint wrap(T endpoint,
                                  EndpointConfig config,
                                  String []names)
  {
    if (endpoint instanceof Endpoint) {
      return (Endpoint) endpoint;
    }

    WebSocketEndpointSkeleton<T> skel
      = new WebSocketEndpointSkeleton<T>((Class<T>) endpoint.getClass(),
                                         config);

    return new EndpointImpl<T>(skel, endpoint, config, names);
  }

  public static Supplier<Endpoint> wrap(ServerEndpointConfig config)
  {
    Class<?> endpointClass = config.getEndpointClass();

    return createEndpointSupplier(endpointClass, config);
  }

  private static <T> Supplier<Endpoint>
  createEndpointSupplier(Class<T> endpointClass,
                         ServerEndpointConfig config)
  {
    InjectManagerAmp injectManager = InjectManagerAmp.create();

    Supplier<T> supplier = injectManager.supplierNew(endpointClass);

    return wrap(endpointClass, config, supplier);
  }

  public static <T> Supplier<Endpoint> wrap(Class<T> endpointClass,
                                            ServerEndpointConfig config,
                                            Supplier<T> supplier)
  {
    if (Endpoint.class.isAssignableFrom(endpointClass)) {
      return (Supplier<Endpoint>) supplier;
    }

    WebSocketEndpointSkeleton<T> skel
      = new WebSocketEndpointSkeleton<T>(endpointClass,
                                         config);

    return new EndpointSupplier<T>(skel, config, supplier);
  }

  public Class<T> getBeanClass()
  {
    return _beanClass;
  }

  public T get()
  {
    return _beanSupplier.get();
  }

  public void onOpen(T bean,
                     EndpointConfig config,
                     Session session,
                     String []pathNames)
  {
    if (_messageHandlerFactoryText != null) {
      _messageHandlerFactoryText.onOpen(session, bean);
    }
    
    if (_messageHandlerFactoryBinary != null) {
      _messageHandlerFactoryBinary.onOpen(session, bean);
    }

    try {
      if (_openMethod != null) {
        _openMethod.invoke(bean, config, session, null, null);
      }
    } catch (Exception e) {
      onError(bean, session, e);
    }
  }

  public void onClose(T bean, Session session, CloseReason reason)
  {
    try {
      if (_closeMethod != null) {
        _closeMethod.invoke(bean, _config, session, null, reason);
      }
      else {
        session.close();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public void onError(T bean, Session session, Throwable throwable)
  {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, throwable.getMessage(), throwable);
    }

    if (_errorMethod != null) {
      _errorMethod.invoke(bean, null, session, throwable, null);
    }
    else if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }
    else {
      throw new RuntimeException(throwable);
    }
  }

  private void introspect()
  {
    Class<?> cl = _beanClass;

    if (_config == null) {
      _config = introspectConfig();
    }

    if (_pathTemplate == null && _config != null)
      _pathTemplate = new PathTemplateWebSocket(_config.getPath());

    introspectOpenMethod(cl);

    introspectCloseMethod(cl);

    introspectErrorMethod(cl);

    try {
      addMessageHandler(cl);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ServerEndpointConfig introspectConfig()
  {
    Class<?> cl = _beanClass;

    ServerEndpoint endpoint = cl.getAnnotation(ServerEndpoint.class);

    if (endpoint == null) {
      return null;
    }

    String path = endpoint.value();

    if (! path.startsWith("/")) {
      path = "/" + path;
    }

    Class<? extends Decoder> []decoderClasses = endpoint.decoders();

    ArrayList<Class<? extends Decoder>> decoderList = new ArrayList<>();

    if (decoderClasses != null && decoderClasses.length > 0) {
      for (int i = 0; i < decoderClasses.length; i++) {
        decoderList.add(decoderClasses[i]);
      }
    }

    Class<? extends Encoder> []encoderClasses = endpoint.encoders();

    ArrayList<Class<? extends Encoder>> encoderList = new ArrayList<>();

    if (encoderClasses != null && encoderClasses.length > 0) {
      for (int i = 0; i < encoderClasses.length; i++) {
        encoderList.add(encoderClasses[i]);
      }
    }

    _pathTemplate = new PathTemplateWebSocket(path);

    ServerEndpointConfig.Builder builder;
    builder = ServerEndpointConfig.Builder.create(cl, path);

    builder.decoders(decoderList);
    builder.encoders(encoderList);

    ServerEndpointConfig config = builder.build();
    // XXX:
    /*
     = new WebSocketServerConfig(value, _pathTemplate.getPattern());
    
    // config.setDecoders(decoderList);
    
    ArrayList<Extension> extList = new ArrayList<>();
    // extList.add("mux");
    
    config.setExtensions(extList);
    */
    return config;
  }

  private void introspectOpenMethod(Class<?> cl)
  {
    Method method = findOpenMethod(cl);

    if (method == null)
      return;

    try {
      _openMethod = createMethodHandle(method);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  private Method findOpenMethod(Class<?> cl)
  {
    if (cl == null) {
      return null;
    }

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(OnOpen.class)) {
        return method;
      }
      else if (Endpoint.class.isAssignableFrom(cl)
               && method.getName().equals("onOpen")
               && method.getParameterTypes().length == 1
               && method.getParameterTypes()[0].equals(Session.class)) {
        return method;
      }
    }

    return findOpenMethod(cl.getSuperclass());
  }
  
  private void addMessageHandler(Class<?> cl)
    throws IllegalAccessException
  {
    if (cl == null) {
      return;
    }

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(OnMessage.class)) {
        addMessageHandlerImpl(method);
      }
    }

    addMessageHandler(cl.getSuperclass());
  }
  
  private void addMessageHandlerImpl(Method method)
    throws IllegalAccessException
  {
    if (isSimpleWhole(method)) {
      setMessageHandlerFactory(createSimpleWholeMessageHandler(method));
      return;
    }

    Class<?> []param = method.getParameterTypes();

    int []paramIndex = findOnMessageParams(method);
    Class<?> param0 = paramIndex[0] > -1 ? param[paramIndex[0]] : null;
    Class<?> param1 = paramIndex[1] > -1 ? param[paramIndex[1]] : null;

    method = ReflectUtil.findAccessibleMethod(method);
    MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);

    MessageHandlerFactoryBase handler = null;

    Resolver<?> []resolvers = createResolvers(method, paramIndex);
    
    long maxLength = -1;
    OnMessage onMessage = method.getAnnotation(OnMessage.class);
    
    if (onMessage != null) {
      maxLength = onMessage.maxMessageSize();
    }

    if (String.class.equals(param0)
        && boolean.class.equals(param1)) {
      handler = new AsyncTextMessageHandlerFactory(resolvers,
                                                   methodHandle);
    }
    else if (String.class.equals(param0)) {
      handler = new TextMessageHandlerFactory(resolvers,
                                              methodHandle,
                                              maxLength);
    }
    else if (Reader.class.equals(param0)) {
      handler = new ReaderMessageHandlerFactory(resolvers,
                                                methodHandle);
    }
    else if (ByteBuffer.class.equals(param0)
             && boolean.class.equals(param1)) {
      handler = new AsyncBinaryMessageHandlerFactory(resolvers,
                                                     methodHandle);
    }
    else if (ByteBuffer.class.equals(param0)) {
      handler = new BinaryMessageHandlerFactory(resolvers,
                                                methodHandle);
    }
    else if (InputStream.class.equals(param0)) {
      handler = new BinaryStreamMessageHandlerFactory(resolvers,
                                                      methodHandle);
    }
    else if (byte[].class.equals(param0)) {
      handler = new ByteArrayHandlerFactory(resolvers,
                                            methodHandle);
    }
    else if (param0 != null) {
      //Decoder.Text<?> decoder = findDecoder(param0);
      
      handler = new DecoderMessageHandlerFactory(resolvers,
                                                 methodHandle,
                                                 param0);
    }
    else {
      System.out.println("UNKNOWN METHOD:" + method);
    }

    setMessageHandlerFactory(handler);
  }

  private void setMessageHandlerFactory(MessageHandlerFactory<T> factory)
  {
    if (factory.isText()) {
      _messageHandlerFactoryText = factory;
    }
    else {
      _messageHandlerFactoryBinary = factory;
    }
  }
  
  private Decoder.Text<?> findDecoder(Class<?> cl)
  {
    return null;
  }
  
  /**
   * Creates a message handler optimized for the Whole message type with
   * an option session, but no @PathParam, decoders, or return types.
   */

  private MessageHandlerFactory<T> createSimpleWholeMessageHandler(Method method)
    throws IllegalAccessException
  {
    Class<?> []param = method.getParameterTypes();

    int []paramIndex = findOnMessageParams(method);
    Class<?> param0 = paramIndex[0] > -1 ? param[paramIndex[0]] : null;
    
    MethodHandle methodHandle = createSimpleWholeMethodHandle(method);
    
    if (String.class.equals(param0)) {
      return new SimpleWholeString(methodHandle);
    }
    else if (Reader.class.equals(param0)) {
      return new SimpleWholeReader(methodHandle);
    }
    else if (ByteBuffer.class.equals(param0)) {
      return new SimpleWholeByteBuffer(methodHandle);
    }
    else if (InputStream .class.equals(param0)) {
      return new SimpleWholeInputStream(methodHandle);
    }
    else {
      throw new IllegalStateException(L.l("Unknown message type {0}", param0));
    }
  }
  
  private MethodHandle createSimpleWholeMethodHandle(Method method)
    throws IllegalAccessException
  {
    method = ReflectUtil.findAccessibleMethod(method);
    
    MethodHandle handle = MethodHandles.lookup().unreflect(method);
    
    Class<?> []param = method.getParameterTypes();
    
    if (param.length == 1) {
      handle = MethodHandles.dropArguments(handle, 1, Session.class);
    }
    else if (param[0].equals(Session.class)) {
    }
    else {
      MethodType type = MethodType.methodType(void.class,
                                              method.getDeclaringClass(),
                                              Session.class,
                                              param[0]);
                                          
      handle = MethodHandles.permuteArguments(handle, type, 0, 2, 1);
    }
    
    MethodType type = MethodType.methodType(void.class,
                                            Object.class,
                                            Session.class,
                                            Object.class);
    
    return handle.asType(type);
  }

  private ResolvingMethodHandle<T> createMethodHandle(Method method)
    throws IllegalAccessException
  {
    method = ReflectUtil.findAccessibleMethod(method);
    method.setAccessible(true);

    Resolver<?> []resolvers = createResolvers(method, new int[]{-1, -1});

    MethodHandle mh = MethodHandles.lookup().unreflect(method);

    mh = mh.asSpreader(Object[].class, resolvers.length + 1);

    ResolvingMethodHandle<T> handle
      = new ResolvingMethodHandle<>(mh, resolvers);

    return handle;
  }
  
  private boolean isSimpleWhole(Method method)
  {
    if (! void.class.equals(method.getReturnType())) {
      return false;
    }
    
    OnMessage onMessage = method.getAnnotation(OnMessage.class);
    
    if (onMessage != null && onMessage.maxMessageSize() > 0) {
      return false;
    }
    
    Annotation [][]paramAnnotations = method.getParameterAnnotations();
    Class<?> []params = method.getParameterTypes();
    
    for (int i = 0; i < params.length; i++) {
      String name = findParamName(paramAnnotations[i]);
      
      if (name != null) {
        return false;
      }
      
      if (Session.class.equals(params[i])) {
      }
      else if (String.class.equals(params[i])) {
      }
      else if (InputStream.class.equals(params[i])) {
      }
      else if (Reader.class.equals(params[i])) {
      }
      else {
        return false;
      }
    }
    
    
    return true;
  }
  
  private Class<?> getMessageType(Method method)
  {
    Annotation [][]paramAnnotations = method.getParameterAnnotations();
    Class<?> []params = method.getParameterTypes();
    
    for (int i = 0; i < params.length; i++) {
      Class<?> param = params[i];
      
      String name = findParamName(paramAnnotations[i]);
      
      if (name != null) {
        continue;
      }
      
      if (Session.class.equals(param)) {
      }
      else {
        return param;
      }
    }
    
    return null;
  }

  private Resolver<?> []createResolvers(Method method, int []messageParam)
  {
    Class<?> []params = method.getParameterTypes();

    final int paramsLen = params.length;

    String []names = new String[paramsLen];

    Annotation [][]paramAnnotations = method.getParameterAnnotations();
    for (int i = 0; i < paramsLen; i++) {
      String name = findParamName(paramAnnotations[i]);
      names[i] = name;
    }

    Resolver<?> []resolvers = new Resolver<?>[paramsLen];

    for (int i = 0; i < paramsLen; i++) {
      if (i == messageParam[0]) {
        resolvers[i] = MessageResolver.RESOLVER;
      }
      else if (i == messageParam[1]) {
        resolvers[i] = IsLastResolver.RESOLVER;
      }
      else {
        resolvers[i] = createResolver(params[i], names[i]);
      }
    }

    return resolvers;
  }

  private Resolver createResolver(final Class type,
                                  final String name)
  {
    if (EndpointConfig.class == type) {
      return EndpointConfigResolver.RESOLVER;
    }
    else if (Session.class == type) {
      return SessionResolver.RESOLVER;
    }
    else if (Throwable.class == type) {
      return ThrowableResolver.RESOLVER;
    }
    else if (CloseReason.class == type) {
      return CloseReasonResolver.RESOLVER;
    }
    else if (name == null) {
      return new NullResolver();
    }
    else if (boolean.class == type || Boolean.class == type) {
      return new BooleanParamResolver(name);
    }
    else if (char.class == type || Character.class == type) {
      return new CharParamResolver(name);
    }
    else if (byte.class == type || Byte.class == type) {
      return new ByteParamResolver(name);
    }
    else if (short.class == type || Short.class == type) {
      return new ShortParamResolver(name);
    }
    else if (int.class == type || Integer.class == type) {
      return new IntParamResolver(name);
    }
    else if (long.class == type || Long.class == type) {
      return new LongParamResolver(name);
    }
    else if (float.class == type || Float.class == type) {
      return new FloatParamResolver(name);
    }
    else if (double.class == type || Double.class == type) {
      return new DoubleParamResolver(name);
    }
    else if (String.class == type) {
      return new StringParamResolver(name);
    }
    else {
      return new NullResolver();
    }
  }

  private String findParamName(Annotation []anns)
  {
    for (Annotation ann : anns) {
      if (ann instanceof PathParam) {
        PathParam paramAnn = (PathParam) ann;

        return paramAnn.value();
      }
    }

    return null;
  }

  private void introspectCloseMethod(Class<?> cl)
  {
    Method method = findCloseMethod(cl);

    if (method == null)
      return;

    try {
      _closeMethod = createMethodHandle(method);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  private Method findCloseMethod(Class<?> cl)
  {
    if (cl == null) {
      return null;
    }

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(OnClose.class)) {
        return method;
      }
      else if (Endpoint.class.isAssignableFrom(cl)
               && method.getName().equals("onClose")
               && method.getParameterTypes().length == 2
               && method.getParameterTypes()[0].equals(Session.class)
               && method.getParameterTypes()[1].equals(CloseReason.class)) {
        return method;
      }
    }

    return findCloseMethod(cl.getSuperclass());
  }

  private void introspectErrorMethod(Class<?> cl)
  {
    Method method = findErrorMethod(cl);

    if (method == null)
      return;

    try {
      _errorMethod = createMethodHandle(method);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  private Method findErrorMethod(Class<?> cl)
  {
    if (cl == null) {
      return null;
    }

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(OnError.class)) {
        return method;
      }
      else if (Endpoint.class.isAssignableFrom(cl)
               && method.getName().equals("onError")
               && method.getParameterTypes().length == 2
               && method.getParameterTypes()[0].equals(Session.class)
               && method.getParameterTypes()[1].equals(Throwable.class)) {
        return method;
      }
    }

    return findErrorMethod(cl.getSuperclass());
  }

  private int []findOnMessageParams(Method method)
  {
    int []indexes = new int[]{-1, -1};
    int index = 0;

    Class<?> []params = method.getParameterTypes();
    Annotation [][]annotations = method.getParameterAnnotations();

    for (int i = 0; i < params.length; i++) {
      Class<?> param = params[i];
      
      if (Session.class.equals(param)) {
        continue;
      }
      else if (getPathParam(annotations[i]) != null) {
        continue;
      }

      if (indexes.length <= index) {
        throw new ConfigException(L.l(
          "Method: '{0}' defines an unbound parameter '{1}'",
          method,
          params[i]));
      }

      indexes[index++] = i;
    }

    if (indexes[0] > -1 && indexes[1] > -1 
        && boolean.class.isAssignableFrom(params[indexes[0]])) {
      final int temp = indexes[0];
      indexes[0] = indexes[1];
      indexes[1] = temp;
    }

    return indexes;
  }

  private PathParam getPathParam(Annotation []annotations)
  {
    for (Annotation annotation : annotations) {
      if (annotation instanceof PathParam) {
        return (PathParam) annotation;
      }
    }

    return null;
  }

  private Method findMessageMethod(Class<?> cl)
  {
    if (cl == null) {
      return null;
    }

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(OnMessage.class)) {
        return method;
      }
    }

    return findMessageMethod(cl.getSuperclass());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass + "]";
  }

  static class EndpointImpl<T> extends Endpoint
  {
    private final WebSocketEndpointSkeleton<T> _skel;
    private final T _bean;
    private final EndpointConfig _config;
    private final String []_pathNames;

    EndpointImpl(WebSocketEndpointSkeleton<T> skel,
                 T bean,
                 EndpointConfig config,
                 String []pathNames)
    {
      _skel = skel;
      _bean = bean;
      _config = config;
      _pathNames = pathNames;
    }

    EndpointImpl(WebSocketEndpointSkeleton<T> skel,
                 T bean,
                 EndpointConfig config)
    {
      _skel = skel;
      _bean = bean;
      _config = config;
      _pathNames = skel._pathTemplate.getNames().toArray(new String[0]);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
      _skel.onOpen(_bean, config, session, _pathNames);
    }

    @Override
    public void onClose(Session session, CloseReason reason)
    {
      _skel.onClose(_bean, session, reason);
    }

    @Override
    public void onError(Session session, Throwable exn)
    {
      _skel.onError(_bean, session, exn);
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _bean + "]";
    }
  }

  interface MessageHandlerFactory<X>
  {
    MessageHandler create(Session session, X bean);

    boolean isText();

    void onOpen(Session session, X bean);
  }
  
  abstract class SimpleWholeBase<X>
    implements MessageHandler.Whole<X>, MessageHandlerFactory<T>
  
  {
    private final MethodHandle _method;
    
    private final Session _session;
    private final T _bean;

    protected SimpleWholeBase(MethodHandle method)
    {
      _method = method;
      _session = null;
      _bean = null;
    }

    protected SimpleWholeBase(MethodHandle method,
                              Session session,
                              T bean)
    {
      _method = method;
      _session = session;
      _bean = bean;
    }
    
    protected MethodHandle getMethodHandle()
    {
      return _method;
    }
    
    @Override
    public void onOpen(Session session, T bean)
    {
      session.addMessageHandler(create(session, bean));
    }

    @Override
    final public void onMessage(X message)
    {
      try {
        _method.invokeExact(_bean, _session, message);
      } catch (Throwable e) {
        onError(_bean, _session, e);
      }
    }
  }
  
  class SimpleWholeString extends SimpleWholeBase<String>
    implements MessageHandlerFactory<T>
  {
    SimpleWholeString(MethodHandle method)
    {
      super(method);
    }
    
    SimpleWholeString(MethodHandle method, Session session, T bean)
    {
      super(method, session, bean);
    }
    
    @Override
    public boolean isText()
    {
      return true;
    }
    
    @Override
    public MessageHandler create(Session session, T bean)
    {
      return new SimpleWholeString(getMethodHandle(), session, bean);
    }
  }
  
  class SimpleWholeInputStream extends SimpleWholeBase<InputStream>
    implements MessageHandlerFactory<T>
  {
    SimpleWholeInputStream(MethodHandle method)
    {
      super(method);
    }
    
    SimpleWholeInputStream(MethodHandle method, Session session, T bean)
    {
      super(method, session, bean);
    }
    
    @Override
    public boolean isText()
    {
      return false;
    }
    
    @Override
    public MessageHandler create(Session session, T bean)
    {
      return new SimpleWholeInputStream(getMethodHandle(), session, bean);
    }
  }
  
  class SimpleWholeReader extends SimpleWholeBase<Reader>
    implements MessageHandlerFactory<T>
  {
    SimpleWholeReader(MethodHandle method)
    {
      super(method);
    }
    
    SimpleWholeReader(MethodHandle method, Session session, T bean)
    {
      super(method, session, bean);
    }
    
    @Override
    public boolean isText()
    {
      return true;
    }
    
    @Override
    public MessageHandler create(Session session, T bean)
    {
      return new SimpleWholeReader(getMethodHandle(), session, bean);
    }
  }
  
  class SimpleWholeByteBuffer extends SimpleWholeBase<ByteBuffer>
    implements MessageHandlerFactory<T>
  {
    SimpleWholeByteBuffer(MethodHandle method)
    {
      super(method);
    }
    
    SimpleWholeByteBuffer(MethodHandle method, Session session, T bean)
    {
      super(method, session, bean);
    }
    
    @Override
    public boolean isText()
    {
      return false;
    }
    
    @Override
    public MessageHandler create(Session session, T bean)
    {
      return new SimpleWholeByteBuffer(getMethodHandle(), session, bean);
    }
  }
  
  abstract class MessageHandlerBase<X>
  {
    private Resolver<?> []_resolvers;
    private MethodHandle _method;
    private Session _session;
    private T _bean;

    protected MessageHandlerBase(Resolver<?>[] resolvers,
                                     MethodHandle method,
                                     Session session,
                                     T bean)
    {
      _resolvers = resolvers;
      _method = method;
      _session = session;
      _bean = bean;
    }
    
    protected Session getSession()
    {
      return _session;
    }

    final public void onMessageImpl(X obj,
                                    boolean isLast)
    {
      try {
        Resolver<?>[] resolvers = _resolvers;
        Object []args = new Object[resolvers.length];
        
        for (int i = 0; i < resolvers.length; i++) {
          Resolver<?> resolver = resolvers[i];

          args[i] = resolver.resolve(obj, null, _session, null, null, isLast);
        }

        Object result = _method.invokeExact(_bean, args);

        if (result != null) {
          _session.getBasicRemote().sendObject(result);
        }
      } catch (Throwable e) {
        e.printStackTrace();
        onError(_bean, _session, e);
      }
    }
  }

  class TextMessageHandler extends MessageHandlerBase
    implements MessageHandler.Whole<String>
  {
    TextMessageHandler(Resolver<?>[] resolvers,
                       MethodHandle method,
                       Session session,
                       Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(String message)
    {
      onMessageImpl(message, false);
    }
  }

  class TextMessageHandlerMaxLen extends MessageHandlerBase
    implements MessageHandler.Whole<String>
  {
    private long _maxLen;
    
    TextMessageHandlerMaxLen(Resolver<?>[] resolvers,
                       MethodHandle method,
                       Session session,
                       Object bean,
                       long maxLen)
    {
      super(resolvers, method, session, bean);
      
      _maxLen = maxLen;
    }

    @Override
    public void onMessage(String message)
    {
      if (message != null && _maxLen < message.length()) {
        try {
          getSession().close(new CloseReason(CloseReason.CloseCodes.TOO_BIG, "message too large"));
        } catch (IOException e) {
          log.log(Level.FINE, e.toString(), e);
        }
        
        return;
      }
      
      onMessageImpl(message, false);
    }
  }

  abstract class MessageHandlerFactoryBase implements MessageHandlerFactory<T>
  {
    final MethodHandle _method;
    final Resolver<?> []_resolvers;

    protected MessageHandlerFactoryBase(Resolver<?> []resolvers,
                                        MethodHandle method)
    {
      method = method.asSpreader(Object[].class, resolvers.length);
      
      MethodType type = MethodType.methodType(Object.class,
                                              Object.class,
                                              Object[].class);
      method = method.asType(type);
      
      _resolvers = resolvers;
      _method = method;
    }
    
    @Override
    public void onOpen(Session session, T bean)
    {
      session.addMessageHandler(create(session, bean));
    }

    MethodHandle bind(Object bean)
    {
      //return _method.bindTo(bean).asSpreader(Object[].class, _resolvers.length);
      return _method;
    }

    public abstract MessageHandler create(Session session, T bean);
  }

  class TextMessageHandlerFactory extends MessageHandlerFactoryBase
  {
    private long _maxLength;
    
    TextMessageHandlerFactory(Resolver<?> []resolvers,
                              MethodHandle method,
                              long maxLength)
    {
      super(resolvers, method);
      
      _maxLength = maxLength;
    }

    @Override
    public boolean isText()
    {
      return true;
    }
    
    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);

      if (_maxLength > 0) {
        return new TextMessageHandlerMaxLen(_resolvers, method, session, bean, _maxLength);
      }
      else {
        return new TextMessageHandler(_resolvers, method, session, bean);
      }
    }
  }

  class AsyncTextMessageHandlerFactory
    extends MessageHandlerFactoryBase
  {
    AsyncTextMessageHandlerFactory(Resolver<?> []resolvers,
                                   MethodHandle method)
    {
      super(resolvers, method);
    }

    @Override
    public boolean isText()
    {
      return true;
    }

    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);
      return new AsyncTextMessageHandler(_resolvers,
                                         method,
                                         session,
                                         bean);
    }
  }

  class AsyncTextMessageHandler extends MessageHandlerBase
    implements MessageHandler.Partial<String>
  {
    AsyncTextMessageHandler(Resolver<?> []resolvers,
                       MethodHandle method,
                       Session session,
                       Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(String partialMessage, boolean isLast)
    {
      onMessageImpl(partialMessage, isLast);
    }
  }

  class ReaderMessageHandlerFactory extends MessageHandlerFactoryBase
  {
    ReaderMessageHandlerFactory(Resolver<?> []resolvers,
                                MethodHandle method)
    {
      super(resolvers, method);
    }

    @Override
    public boolean isText()
    {
      return true;
    }

    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);

      return new ReaderMessageHandler(_resolvers, method, session, bean);
    }
  }

  class ReaderMessageHandler extends MessageHandlerBase
    implements MessageHandler.Whole<Reader>
  {

    ReaderMessageHandler(Resolver<?> []resolvers,
                         MethodHandle method,
                         Session session,
                         Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(Reader message)
    {
      onMessageImpl(message, false);
    }
  }

  class BinaryMessageHandlerFactory extends MessageHandlerFactoryBase
  {
    BinaryMessageHandlerFactory(Resolver<?> []resolvers,
                                MethodHandle method)
    {
      super(resolvers, method);
    }

    @Override
    public boolean isText()
    {
      return false;
    }

    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);

      return new BinaryMessageHandler(_resolvers,
                                      method,
                                      session,
                                      bean);
    }
  }

  class BinaryMessageHandler extends MessageHandlerBase
    implements MessageHandler.Whole<ByteBuffer>
  {
    BinaryMessageHandler(Resolver<?> []resolvers,
                         MethodHandle method,
                         Session session,
                         Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(ByteBuffer message)
    {
      onMessageImpl(message, false);
    }
  }

  class AsyncBinaryMessageHandlerFactory
    extends MessageHandlerFactoryBase
  {
    AsyncBinaryMessageHandlerFactory(Resolver<?> []resolvers,
                                     MethodHandle method)
    {
      super(resolvers, method);
    }

    @Override
    public boolean isText()
    {
      return false;
    }

    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);

      return new AsyncBinaryMessageHandler(_resolvers,
                                           method,
                                           session,
                                           bean);
    }
  }

  class AsyncBinaryMessageHandler extends MessageHandlerBase
    implements MessageHandler.Partial<ByteBuffer>
  {
    AsyncBinaryMessageHandler(Resolver []resolvers,
                              MethodHandle method,
                              Session session,
                              Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(ByteBuffer partialMessage, boolean isLast)
    {
      onMessageImpl(partialMessage, isLast);
    }
  }

  class BinaryStreamMessageHandlerFactory
    extends MessageHandlerFactoryBase
  {
    BinaryStreamMessageHandlerFactory(Resolver<?> []resolvers,
                                      MethodHandle method)
    {
      super(resolvers, method);
    }

    @Override
    public boolean isText()
    {
      return false;
    }

    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);

      return new BinaryStreamMessageHandler(_resolvers,
                                            method,
                                            session,
                                            bean);
    }
  }

  class BinaryStreamMessageHandler extends MessageHandlerBase
    implements MessageHandler.Whole<InputStream>

  {
    BinaryStreamMessageHandler(Resolver []resolvers,
                               MethodHandle method,
                               Session session,
                               Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(InputStream message)
    {
      onMessageImpl(message, false);
    }
  }

  class ByteArrayHandler extends MessageHandlerBase
    implements MessageHandler.Partial<ByteBuffer>

  {
    ByteArrayHandler(Resolver []resolvers,
                               MethodHandle method,
                               Session session,
                               Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(ByteBuffer is, boolean isLast)
    {
      byte []buffer = new byte[is.remaining()];
        
      is.get(buffer);

      onMessageImpl(buffer, isLast);
    }
  }

  class ByteArrayHandlerFactory extends MessageHandlerFactoryBase
  {
    ByteArrayHandlerFactory(Resolver<?> []resolvers,
                            MethodHandle method)
    {
      super(resolvers, method);
    }

    @Override
    public boolean isText()
    {
      return false;
    }

    @Override
    public MessageHandler create(Session session, T bean)
    {
      MethodHandle method = bind(bean);

      return new ByteArrayHandler(_resolvers,
                                            method,
                                            session,
                                            bean);
    }
  }

  class DecoderMessageHandlerFactory
    extends MessageHandlerFactoryBase
  {
    private Class<?> _type;
    
    DecoderMessageHandlerFactory(Resolver<?> []resolvers,
                                 MethodHandle method,
                                 Class<?> decoderType)
    {
      super(resolvers, method);
      
      _type = decoderType;
    }
    @Override
    public void onOpen(Session session, T bean)
    {
      SessionWebSocketBase sessionWs = (SessionWebSocketBase) session;
      
      sessionWs.addBasicHandler(create(session, bean), _type);
    }

    @Override
    public boolean isText()
    {
      return true;
    }

    @Override
    public MessageHandler.Whole<?> create(Session session, T bean)
    {
      MethodHandle method = bind(bean);
      
      return new DecoderMessageHandler(_resolvers,
                                       method,
                                       session,
                                       bean);
    }
  }

  class DecoderMessageHandler extends MessageHandlerBase
    implements MessageHandler.Whole<Object>
  {
    DecoderMessageHandler(Resolver []resolvers,
                          MethodHandle method,
                          Session session,
                          Object bean)
    {
      super(resolvers, method, session, bean);
    }

    @Override
    public void onMessage(Object message)
    {
      onMessageImpl(message, false);
    }
  }

  static class WrapEndpointFactory<T> implements Supplier<Endpoint> {
    private Supplier<T> _userFactory;
    private WebSocketEndpointSkeleton<T> _skel;
    private EndpointConfig _config;

    WrapEndpointFactory(Supplier<T> userFactory,
                        EndpointConfig config,
                        WebSocketEndpointSkeleton<T> skel)
    {
      _userFactory = userFactory;
      _config = config;
      _skel = skel;
    }

    // @Override
    public EndpointConfig getConfig()
    {
      return _config;
    }

    @Override
    public Endpoint get()
    {
      T userEndpoint = _userFactory.get();

      String []names = new String[0];

      return new EndpointImpl<T>(_skel, userEndpoint, getConfig(), names);
    }

    public Endpoint createEndpoint(String []names)
    {
      T userEndpoint = _userFactory.get();

      return new EndpointImpl<T>(_skel, userEndpoint, getConfig(), names);
    }
  }

  static class EndpointSupplier<T> implements Supplier<Endpoint>
  {
    private WebSocketEndpointSkeleton<T> _skel;
    private ServerEndpointConfig _config;
    private Supplier<T> _supplier;

    EndpointSupplier(WebSocketEndpointSkeleton<T> skel,
                     ServerEndpointConfig config,
                     Supplier<T> supplier)
    {
      _skel = skel;
      _config = config;
      _supplier = supplier;
    }

    @Override
    public Endpoint get()
    {
      return new EndpointImpl<T>(_skel, _supplier.get(), _config);
    }
  }

  interface Resolver<T>
  {
    T resolve(Object bean,
              EndpointConfig config,
              Session session,
              Throwable throwable,
              CloseReason closeReason,
              boolean isLast);
  }

  static class MessageResolver implements Resolver<Object>
  {
    static final MessageResolver RESOLVER = new MessageResolver();
    
    @Override
    public Object resolve(Object bean,
                                  EndpointConfig config,
                                  Session session,
                                  Throwable throwable,
                                  CloseReason closeReason,
                                  boolean isLast)
    {
      return bean;
    }
  }

  static class IsLastResolver implements Resolver<Boolean>
  {
    static final IsLastResolver RESOLVER = new IsLastResolver();
    
    @Override
    public Boolean resolve(Object bean,
                                  EndpointConfig config,
                                  Session session,
                                  Throwable throwable,
                                  CloseReason closeReason,
                                  boolean isLast)
    {
      return isLast;
    }
  }

  static class EndpointConfigResolver implements Resolver<EndpointConfig>
  {
    static final EndpointConfigResolver RESOLVER = new EndpointConfigResolver();
    
    @Override
    public EndpointConfig resolve(Object bean,
                                  EndpointConfig config,
                                  Session session,
                                  Throwable throwable,
                                  CloseReason closeReason,
                                  boolean isLast)
    {
      return config;
    }
  }

  static class SessionResolver implements Resolver<Session>
  {
    static final SessionResolver RESOLVER = new SessionResolver();
    
    @Override
    public Session resolve(Object bean,
                           EndpointConfig config,
                           Session session,
                           Throwable throwable,
                           CloseReason closeReason,
                           boolean isLast)
    {
      return session;
    }
  }

  static class ThrowableResolver implements Resolver<Throwable>
  {
    static final ThrowableResolver RESOLVER = new ThrowableResolver();
    
    @Override
    public Throwable resolve(Object bean,
                             EndpointConfig config,
                             Session session,
                             Throwable throwable,
                             CloseReason closeReason,
                             boolean isLast)
    {
      return throwable;
    }
  }

  static class CloseReasonResolver implements Resolver<CloseReason>
  {
    static final CloseReasonResolver RESOLVER = new CloseReasonResolver();
    
    @Override
    public CloseReason resolve(Object bean,
                               EndpointConfig config,
                               Session session,
                               Throwable throwable, 
                               CloseReason closeReason,
                               boolean isLast)
    {
      return closeReason;
    }
  }

  static abstract class ParamResolver<T> implements Resolver<T>
  {
    private String _name;

    protected ParamResolver(String name)
    {
      _name = name;
    }

    final protected String getParameter(Session session)
    {
      return session.getPathParameters().get(_name);
    }

    @Override
    public final T resolve(Object bean,
                           EndpointConfig config,
                           Session session,
                           Throwable throwable,
                           CloseReason closeReason,
                           boolean isLast)
    {
      try {
        return resolveImpl(config, session, throwable, closeReason);
      } catch (NumberFormatException e) {
        String message = L.l("Can't convert path parameter({0}: {1}", _name, e);

        throw new ResolveException(message, e);
      }
    }

    protected abstract T resolveImpl(EndpointConfig config,
                                   Session session,
                                   Throwable throwable,
                                   CloseReason closeReason);
  }

  static class StringParamResolver extends ParamResolver<String>
  {
    StringParamResolver(String name)
    {
      super(name);
    }

    @Override
    public String resolveImpl(EndpointConfig config,
                              Session session,
                              Throwable throwable,
                              CloseReason closeReason)
    {
      return getParameter(session);
    }
  }

  static class BooleanParamResolver extends ParamResolver<Boolean>
  {
    BooleanParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Boolean resolveImpl(EndpointConfig config,
                           Session session,
                           Throwable throwable,
                           CloseReason closeReason)
    {
      return Boolean.parseBoolean(getParameter(session));
    }
  }

  static class ByteParamResolver extends ParamResolver<Byte>
  {
    ByteParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Byte resolveImpl(EndpointConfig config,
                            Session session,
                            Throwable throwable,
                            CloseReason closeReason)
    {
      return Byte.parseByte(getParameter(session));
    }
  }

  static class ShortParamResolver extends ParamResolver<Short>
  {
    ShortParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Short resolveImpl(EndpointConfig config,
                             Session session,
                             Throwable throwable,
                             CloseReason closeReason)
    {
      return Short.parseShort(getParameter(session));
    }
  }

  static class LongParamResolver extends ParamResolver<Long>
  {
    LongParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Long resolveImpl(EndpointConfig config,
                            Session session,
                            Throwable throwable, CloseReason closeReason)
    {
      return Long.parseLong(getParameter(session));
    }
  }

  static class FloatParamResolver extends ParamResolver<Float>
  {
    FloatParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Float resolveImpl(EndpointConfig config,
                             Session session,
                             Throwable throwable, CloseReason closeReason)
    {
      return Float.parseFloat(getParameter(session));
    }
  }

  static class DoubleParamResolver extends ParamResolver<Double>
  {
    DoubleParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Double resolveImpl(EndpointConfig config,
                              Session session,
                              Throwable throwable, CloseReason closeReason)
    {
      return Double.parseDouble(getParameter(session));
    }
  }

  static class IntParamResolver extends ParamResolver<Integer>
  {
    IntParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Integer resolveImpl(EndpointConfig config,
                               Session session,
                               Throwable throwable, CloseReason closeReason)
    {
      return Integer.parseInt(getParameter(session));
    }
  }

  static class CharParamResolver extends ParamResolver<Character>
  {
    CharParamResolver(String name)
    {
      super(name);
    }

    @Override
    public Character resolveImpl(EndpointConfig config,
                               Session session,
                               Throwable throwable, CloseReason closeReason)
    {
      String p = getParameter(session);
      
      if (p == null || p.length() == 0) {
        return (char) 0;
      }
      else {
        return p.charAt(0);
      }
    }
  }

  static final class NullResolver<T> implements Resolver<T>
  {
    @Override
    public T resolve(Object bean,
                     EndpointConfig config,
                     Session session,
                     Throwable throwable, 
                     CloseReason closeReason,
                     boolean isLast)
    {
      return null;
    }
  }

  static class ResolveException extends RuntimeException
  {
    ResolveException(String message, Throwable cause)
    {
      super(message, cause);
    }
  }

  static class ResolvingMethodHandle<T>
  {
    MethodHandle _method;
    Resolver<?> []_resolvers;

    ResolvingMethodHandle(MethodHandle method,
                          Resolver<?> []resolvers)
    {
      _method = method;
      _resolvers = resolvers;
    }

    public void invoke(T bean,
                       EndpointConfig config,
                       Session session,
                       Throwable throwable,
                       CloseReason closeReason)
    {
      Object []args = new Object[_resolvers.length + 1];
      args[0] = bean;
      try {
        for (int i = 0; i < _resolvers.length; i++) {
          Resolver resolver = _resolvers[i];
          args[i + 1] = resolver.resolve(bean,
                                         config,
                                         session,
                                         throwable,
                                         closeReason,
                                         false);
        }

        _method.invoke(args);
      } catch (RuntimeException e) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, e.getMessage(), e);
        throw e;
      } catch (Throwable e) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }
  }
}
