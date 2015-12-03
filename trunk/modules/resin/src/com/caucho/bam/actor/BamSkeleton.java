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

package com.caucho.bam.actor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.L10N;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
public class BamSkeleton<S>
{
  private static final L10N L = new L10N(BamSkeleton.class);
  private static final Logger log
    = Logger.getLogger(BamSkeleton.class.getName());

  private final static WeakHashMap<Class<?>, SoftReference<BamSkeleton<?>>> _skeletonRefMap
    = new WeakHashMap<Class<?>, SoftReference<BamSkeleton<?>>>();

  private Class<?> _cl;

  private final HashMap<Class<?>, Method> _messageHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _messageErrorHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, QueryInvoker> _queryHandlers
    = new HashMap<Class<?>, QueryInvoker>();
  private final HashMap<Class<?>, Method> _queryResultHandlers
    = new HashMap<Class<?>, Method>();
  private final HashMap<Class<?>, Method> _queryErrorHandlers
    = new HashMap<Class<?>, Method>();
  
  private BamSkeleton(Class<S> cl)
  {
    _cl = cl;

    log.finest(L.l("{0} introspecting class {1}", this, cl.getName()));

    introspect(cl);
  }

  @SuppressWarnings("unchecked")
  public static <T> BamSkeleton<T>
  getSkeleton(Class<T> cl)
  {
    synchronized(_skeletonRefMap) {
      SoftReference<BamSkeleton<?>> skeletonRef = _skeletonRefMap.get(cl);

      BamSkeleton<?> skeleton = null;

      if (skeletonRef != null)
        skeleton = skeletonRef.get();

      if (skeleton == null) {
        skeleton = new BamSkeleton(cl);
        _skeletonRefMap.put(cl, new SoftReference<BamSkeleton<?>>(skeleton));
      }

      return (BamSkeleton<T>) skeleton;
    }
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
    Method handler;

    if (payload != null)
      handler = _messageHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " message " + payload
                   + " {from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, to, from, payload);
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
    QueryInvoker handler;

    if (payload != null)
      handler = _queryHandlers.get(payload.getClass());
    else {
      handler = null;
    }

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(actor + " query " + payload
                   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }

      try {
        handler.invoke(actor, broker, id, to, from, payload);
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
    if (cl == null)
      return;

    introspect(cl.getSuperclass());

    Method[] methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      Class<?> payloadType = getPayloadType(Message.class, method);

      if (payloadType != null) {
        log.log(Level.ALL, L.l("{0} introspect @Message {1} method={2}",
                    this, payloadType.getName(), method));

        method.setAccessible(true);

        _messageHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getPayloadType(MessageError.class, method);

      if (payloadType != null) {
        log.log(Level.ALL, L.l("{0} introspect @MessageError {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _messageErrorHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(Query.class, method);

      if (payloadType != null) {
        log.log(Level.ALL, L.l("{0} @Query {1} method={2}",
                               this, payloadType.getName(), method));

        method.setAccessible(true);
        
        if (method.getParameterTypes().length == 1)
          _queryHandlers.put(payloadType, new QueryShortMethodInvoker(method));
        else if (method.getParameterTypes().length == 4)
          _queryHandlers.put(payloadType, new QueryMethodInvoker(method));
        else 
          throw new IllegalStateException(String.valueOf(method));
        
        continue;
      }

      payloadType = getQueryPayloadType(QueryResult.class, method);

      if (payloadType != null) {
        log.log(Level.ALL, L.l("{0} @QueryResult {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _queryResultHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryErrorPayloadType(QueryError.class, method);

      if (payloadType != null) {
        log.log(Level.ALL, L.l("{0} @QueryError {1} method={2}",
                       this, payloadType.getName(), method));

        method.setAccessible(true);

        _queryErrorHandlers.put(payloadType, method);
        continue;
      }
    }
  }

  private Class<?> getPayloadType(Class<? extends Annotation> annotationType, 
                                  Method method)
  {
    Class<?> []paramTypes = method.getParameterTypes();

    if (paramTypes.length < 3)
      return null;

    if (method.isAnnotationPresent(annotationType))
      return paramTypes[2];
    else
      return null;
  }

  private Class<?> getQueryPayloadType(Class<? extends Annotation> annotationType, 
                                       Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;

    Class<?> []paramTypes = method.getParameterTypes();

    if (paramTypes.length == 1
        && Serializable.class.isAssignableFrom(paramTypes[0]))
      return paramTypes[0];
    else if (paramTypes.length == 4
             && long.class.equals(paramTypes[0])
             && String.class.equals(paramTypes[1])
             && String.class.equals(paramTypes[2])
             && Serializable.class.isAssignableFrom(paramTypes[3])) {
      return paramTypes[3];
    }
    else {
      throw new BamException(method + " is an invalid "
                             + " @" + annotationType.getSimpleName()
                             + " because queries require (long, String, String, MyPayload)");
    }
  }

  private Class<?> getQueryErrorPayloadType(Class<? extends Annotation> annotationType, Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;

    Class<?> []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 5
        || ! long.class.equals(paramTypes[0])
        || ! String.class.equals(paramTypes[1])
        || ! String.class.equals(paramTypes[2])
        || ! Serializable.class.isAssignableFrom(paramTypes[3])
        || ! BamError.class.isAssignableFrom(paramTypes[4])) {
      throw new BamException(method + " is an invalid "
                             + " @" + annotationType.getSimpleName()
                             + " because queries require (long, String, String, MyPayload, ActorError)");
    }
    /*
    else if (! void.class.equals(method.getReturnType())) {
      throw new ActorException(method + " is an invalid @"
                             + annotationType.getSimpleName()
                             + " because queries must return void");
    }
    */

    return paramTypes[3];
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
                                Serializable payload)
    throws IllegalAccessException, InvocationTargetException;
  }
  
  static class QueryMethodInvoker extends QueryInvoker {
    private final Method _method;
    
    QueryMethodInvoker(Method method)
    {
      _method = method;
    }

    @Override
    public void invoke(Object actor,
                       Broker broker,
                       long id, 
                       String to, 
                       String from,
                       Serializable payload)
      throws IllegalAccessException, InvocationTargetException
    {
      _method.invoke(actor, id, to, from, payload);
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
                       Serializable payload)
      throws IllegalAccessException, InvocationTargetException
    {
      Object result = _method.invoke(actor, payload);
      
      broker.queryResult(id, from, to, (Serializable) result);
    }
  }
}
