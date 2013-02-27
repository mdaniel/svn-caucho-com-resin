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
 * @author Emil Ong
 */

package com.caucho.bam.proxy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.bam.Message;
import com.caucho.bam.MessageError;
import com.caucho.bam.Query;
import com.caucho.bam.QueryError;
import com.caucho.bam.QueryResult;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.actor.SkeletonActorFilter;
import com.caucho.bam.actor.SkeletonInvocationException;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.Hex;
import com.caucho.util.L10N;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
public class ProxySkeleton<S>
{
  private static final L10N L = new L10N(ProxySkeleton.class);
  private static final Logger log
    = Logger.getLogger(ProxySkeleton.class.getName());

  private Class<?> _cl;

  private final HashMap<String, Method> _messageHandlers
    = new HashMap<String, Method>();
  private final HashMap<Class<?>, Method> _messageErrorHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<String, QueryInvoker> _queryHandlers
    = new HashMap<String, QueryInvoker>();
  private final HashMap<Class<?>, Method> _queryResultHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _queryErrorHandlers
    = new HashMap<Class<?>, Method>();
  
  private ProxySkeleton(Class<S> cl)
  {
    _cl = cl;

    log.finest(L.l("{0} introspecting class {1}", this, cl.getName()));

    introspect(cl);
  }

  @SuppressWarnings("unchecked")
  public static <T> ProxySkeleton<T>
  getSkeleton(Class<T> cl)
  {
    return new ProxySkeleton(cl);
    /*
    synchronized(_skeletonRefMap) {
      SoftReference<ProxySkeleton<?>> skeletonRef = _skeletonRefMap.get(cl);

      ProxySkeleton<?> skeleton = null;

      if (skeletonRef != null)
        skeleton = skeletonRef.get();

      if (skeleton == null) {
        skeleton = new ProxySkeleton(cl);
        _skeletonRefMap.put(cl, new SoftReference<ProxySkeleton<?>>(skeleton));
      }

      return (ProxySkeleton<T>) skeleton;
    }
    */
  }

  /**
   * Dispatches a message to the actorStream.
   */
  public void message(S actor,
                      MessageStream fallback,
                      String to,
                      String from,
                      Serializable payload)
  {
    if (! (payload instanceof CallPayload)) {
      if (log.isLoggable(Level.FINER)) {
        log.finer(actor + " message " + payload + " is unsupported");
      }
      
      return;
    }
    
    CallPayload call = (CallPayload) payload;
    
    Method handler = _messageHandlers.get(call.getName());

    if (handler != null) {
      if (log.isLoggable(Level.FINER)) {
        logCall(-1, to, from, call);
      }
      
      Object []args = call.getArgs();
      Class<?> []paramTypes = handler.getParameterTypes();
      if (args != null && args.length != paramTypes.length) {
        String msg = (L.l("'{0}.{1}' has an incorrect number of arguments (received {2} but expected {3})\n  {4}",
                          actor.getClass().getSimpleName(),
                          handler.getName(),
                          args != null ? args.length : 0,
                          paramTypes.length,
                          handler));
        
        log.warning(msg);
        
        throw new IllegalArgumentException(msg);
      }

      try {
        handler.invoke(actor, call.getArgs());
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      fallback.message(to, from, payload);
    }
  }

  public void messageError(S actor,
                           MessageStream fallback,
                           String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    Method handler;

    if (payload != null)
      handler = _messageErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " messageError " + error + " " + payload
                   + " {from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, to, from, payload, error);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      fallback.messageError(to, from, payload, error);
    }
  }

  public void query(S actor,
                    MessageStream fallback,
                    Broker broker,
                    long id,
                    String to,
                    String from,
                    Serializable payload)
  {
    if (! (payload instanceof CallPayload)) {
      if (log.isLoggable(Level.FINER)) {
        log.finer(actor + " query " + payload + " is unsupported");
      }
      
      broker.queryError(id, from, to, payload,
                        new BamError(BamError.TYPE_CANCEL,
                                     BamError.FEATURE_NOT_IMPLEMENTED,
                                     actor + " query " + payload + " is unsupported"));
      
      return;
    }
    
    CallPayload call = (CallPayload) payload;

    QueryInvoker handler = _queryHandlers.get(call.getName());

    if (handler != null) {
      if (log.isLoggable(Level.FINER)) {
        logCall(id, to, from, call);
      }

      try {
        handler.invoke(actor, broker, id, to, from, call.getName(), call.getArgs());
      }
      catch (RuntimeException e) {
        // broker.queryError(id, from, to, payload, ActorError.create(e));
        
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        // broker.queryError(id, from, to, payload, ActorError.create(cause));
        
        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        // broker.queryError(id, from, to, payload, ActorError.create(e));
        
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      fallback.query(id, to, from, payload);
    }
  }
  
  private void logCall(long id, String to, String from, CallPayload call)
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append("BAM ");
    sb.append(call.getName());
    sb.append(" (");
    
    Object []args = call.getArgs();
    for (int i = 0; args != null && i < args.length; i++) {
      if (i != 0)
        sb.append(", ");
      
      Object arg = args[i];
      
      if (arg instanceof byte []) {
        sb.append("byte[")
          .append(Hex.toHex((byte []) args[i], 0, 4))
          .append("...]");
      }
      else {
        sb.append(args[i]);
      }
    }
    
    sb.append(")");
    
    
    sb.append("\n  {");
    
    if (id >= 0)
      sb.append("id: " + id + ", ");

    sb.append("from:" + from + ", to:" + to + "}");
    
    log.finer(sb.toString());
  }

  public void queryResult(S actor,
                          MessageStream fallback,
                          long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _queryResultHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " queryResult " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, id, to, from, payload);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      fallback.queryResult(id, to, from, payload);
    }
  }

  public void queryError(S actor,
                         MessageStream fallback,
                         long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    Method handler;

    if (payload != null)
      handler = _queryErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " queryError " + error + " " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, id, to, from, payload, error);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        throw SkeletonInvocationException.createRuntimeException(cause);
      }
      catch (Exception e) {
        throw SkeletonInvocationException.createRuntimeException(e);
      }
    }
    else {
      fallback.queryError(id, to, from, payload, error);
    }
  }

  protected void introspect(Class<?> cl)
  {
    if (cl == null || cl == Object.class)
      return;

    introspect(cl.getSuperclass());

    Method[] methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      
      if (! Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      
      if (Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      
      Class<?> []paramTypes = method.getParameterTypes();
      
      if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1].isAssignableFrom(ReplyCallback.class)) {
        method.setAccessible(true);

        _queryHandlers.put(method.getName(), new QueryShortReplyMethodInvoker(method));
        continue;
      }
      else if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1].isAssignableFrom(QueryCallback.class)) {
        method.setAccessible(true);

        _queryHandlers.put(method.getName(), new QueryShortQueryMethodInvoker(method));
        continue;
      }
      else if (void.class.equals(method.getReturnType())) {
        method.setAccessible(true);

        _messageHandlers.put(method.getName(), method);
        continue;
      }
      else {
        method.setAccessible(true);

        _queryHandlers.put(method.getName(), new QueryMethodInvoker(method));
        _messageHandlers.put(method.getName(), method);
        continue;
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cl.getName() + "]";
  }
  
  abstract static class QueryInvoker {
    abstract public void invoke(Object actor,
                                Broker broker,
                                long id, 
                                String to, 
                                String from,
                                String name,
                                Object []args)
    throws IllegalAccessException, InvocationTargetException;
  }
  
  static class QueryMethodInvoker extends QueryInvoker {
    private final Method _method;
    private final Class<?> []_paramTypes;
    
    QueryMethodInvoker(Method method)
    {
      _method = method;
      _paramTypes = method.getParameterTypes();
    }

    @Override
    public void invoke(Object actor,
                       Broker broker,
                       long id, 
                       String to, 
                       String from,
                       String name,
                       Object []args)
      throws IllegalAccessException, InvocationTargetException
    {
      if (args != null && args.length != _paramTypes.length) {
        throw new IllegalArgumentException(L.l("'{0}.{1}' has an incorrect number of arguments (received {2} but expected {3})\n  {4}",
                                               actor.getClass().getSimpleName(),
                                               name,
                                               args != null ? args.length : 0,
                                               _paramTypes.length,
                                               _method));
      }
      
      Object result = _method.invoke(actor, args);
      
      broker.queryResult(id, from, to, new ReplyPayload(result));
    }
  }
  
  static class QueryShortMethodInvoker extends QueryInvoker {
    private final Method _method;
    
    QueryShortMethodInvoker(Method method)
    {
      _method = method;
    }

    @Override
    public void invoke(Object actor,
                       Broker broker,
                       long id, 
                       String to, 
                       String from,
                       String methodName,
                       Object []args)
      throws IllegalAccessException, InvocationTargetException
    {
      Object result = _method.invoke(actor, args);
      
      broker.queryResult(id, from, to, new ReplyPayload(result));
    }
  }
  
  static class QueryShortReplyMethodInvoker extends QueryInvoker {
    private final Method _method;
    
    QueryShortReplyMethodInvoker(Method method)
    {
      _method = method;
    }

    @Override
    public void invoke(Object actor,
                       Broker broker,
                       long id, 
                       String to, 
                       String from,
                       String methodName,
                       Object []args)
      throws IllegalAccessException, InvocationTargetException
    {
      Object []param = new Object[args.length + 1];
      System.arraycopy(args, 0, param, 0, args.length);
      
      param[args.length] = new QueryReplyCallback(id, to, from, broker);
      
      _method.invoke(actor, param);
    }
  }
  
  static class QueryReplyCallback implements ReplyCallback<Object> {
    private final long _id;
    private final String _to;
    private final String _from;
    private final Broker _broker;
    
    QueryReplyCallback(long id, String to, String from, Broker broker)
    {
      _id = id;
      _to = to;
      _from = from;
      _broker = broker;
    }

    @Override
    public void onReply(Object result)
    {
      ReplyPayload reply = new ReplyPayload(result);
      
      _broker.queryResult(_id, _from, _to, reply);
    }
    
    @Override
    public void onError(BamError error)
    {
      _broker.queryError(_id, _from, _to, null, error);
    }
  }
  
  static class QueryShortQueryMethodInvoker extends QueryInvoker {
    private final Method _method;
    
    QueryShortQueryMethodInvoker(Method method)
    {
      _method = method;
    }

    @Override
    public void invoke(Object actor,
                       Broker broker,
                       long id, 
                       String to, 
                       String from,
                       String methodName,
                       Object []args)
      throws IllegalAccessException, InvocationTargetException
    {
      Object []param = new Object[args.length + 1];
      System.arraycopy(args, 0, param, 0, args.length);
      
      param[args.length] = new QueryQueryCallback(id, to, from, broker);
      
      _method.invoke(actor, param);
    }
  }
  
  static class QueryQueryCallback implements QueryCallback {
    private final long _id;
    private final String _to;
    private final String _from;
    private final Broker _broker;
    
    QueryQueryCallback(long id, String to, String from, Broker broker)
    {
      _id = id;
      _to = to;
      _from = from;
      _broker = broker;
    }

    @Override
    public void onQueryResult(String to, String from, Serializable payload)
    {
      _broker.queryResult(_id, _from, _to, payload);
    }
    
    @Override
    public void onQueryError(String to, String from, Serializable payload,
                             BamError error)
    {
      _broker.queryError(_id, _from, _to, payload, error);
    }
  }
}
