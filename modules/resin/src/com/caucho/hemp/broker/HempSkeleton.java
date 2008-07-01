/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.hemp.broker;

import com.caucho.config.*;
import com.caucho.hemp.annotation.*;
import com.caucho.bam.BamError;
import com.caucho.util.*;

import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 */
public class HempSkeleton<C>
{
  private static final L10N L = new L10N(HempSkeleton.class);
  private static final Logger log
    = Logger.getLogger(HempSkeleton.class.getName());

  private final static HashMap<Class, HempSkeleton> _skeletons = 
    new HashMap<Class, HempSkeleton>();
  
  private final HashMap<Class, Method> _messageHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _messageErrorHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _queryGetHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _querySetHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _queryResultHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _queryErrorHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceProbeHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceSubscribeHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceSubscribedHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceUnsubscribeHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceUnsubscribedHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceUnavailableHandlers = 
    new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceErrorHandlers = 
    new HashMap<Class, Method>();

  private HempSkeleton(Class<C> cl)
  {
    log.log(Level.FINEST, L.l("Introspecting class {0}", cl.getName()));

    Method[] methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Class observed = null;

      observed = getMessageObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found message handler: {0}", methods[i]));
        _messageHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getMessageErrorObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found message error handler: {0}", 
                                  methods[i]));
        _messageErrorHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getQueryGetObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found queryGet handler: {0}", methods[i]));
        _queryGetHandlers.put(observed, methods[i]);
        continue;
      }
      

      observed = getQuerySetObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found querySet handler: {0}", methods[i]));
        _querySetHandlers.put(observed, methods[i]);
        continue;
      }

      
      observed = getQueryResultObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found queryResult handler: {0}", 
                                  methods[i]));
        _queryResultHandlers.put(observed, methods[i]);
        continue;
      }
      

      observed = getQueryErrorObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found query error handler: {0}", 
                                  methods[i]));
        _queryErrorHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence handler: {0}", methods[i]));
        _presenceHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceProbeObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence probe handler: {0}", 
                                  methods[i]));
        _presenceProbeHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceSubscribeObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence subscribe handler: {0}", 
                                  methods[i]));
        _presenceSubscribeHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceSubscribedObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence subscribed handler: {0}", 
                                  methods[i]));
        _presenceSubscribedHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceUnsubscribeObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence unsubscribe handler: {0}", 
                                  methods[i]));
        _presenceUnsubscribeHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceUnsubscribedObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence unsubscribed handler: {0}", 
                                  methods[i]));
        _presenceUnsubscribedHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceUnavailableObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence unavailable handler: {0}", 
                                  methods[i]));
        _presenceUnavailableHandlers.put(observed, methods[i]);
        continue;
      }


      observed = getPresenceErrorObserved(methods[i]);

      if (observed != null) {
        log.log(Level.FINEST, L.l("Found presence error handler: {0}", 
                                  methods[i]));
        _presenceErrorHandlers.put(observed, methods[i]);
        continue;
      }
    }
  }
 
  public static HempSkeleton getHempSkeleton(Class cl)
  {
    HempSkeleton skeleton = _skeletons.get(cl);

    if (skeleton == null) {
      skeleton = new HempSkeleton(cl);
      _skeletons.put(cl, skeleton);
    }

    return skeleton;
  }

  public void dispatchMessage(C target, String to, String from,
                              Serializable value)
  {
    Method messageHandler = _messageHandlers.get(value.getClass());

    if (messageHandler == null)
      return;
    
    try {
      messageHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchMessageError(C target, String to, String from,
                                   Serializable value, BamError error)
  {
    Method messageErrorHandler = _messageErrorHandlers.get(value.getClass());

    if (messageErrorHandler == null)
      return;
    
    try {
      messageErrorHandler.invoke(target, to, from, value, error);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public boolean dispatchQueryGet(C target, long id, String to, String from,
                                  Serializable value)
  {
    Method queryHandler = _queryGetHandlers.get(value.getClass());

    if (queryHandler == null)
      return false;
    
    try {
      return (Boolean) queryHandler.invoke(target, id, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  public boolean dispatchQuerySet(C target, long id, String to, String from,
                                  Serializable value)
  {
    Method queryHandler = _querySetHandlers.get(value.getClass());

    if (queryHandler == null)
      return false;
    
    try {
      return (Boolean) queryHandler.invoke(target, id, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  public void dispatchQueryResult(C target, long id, String to, String from,
                                  Serializable value)
  {
    Method queryHandler = _queryResultHandlers.get(value.getClass());

    if (queryHandler == null)
      return;
    
    try {
      queryHandler.invoke(target, id, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchQueryError(C target, long id, String to, String from,
                                 Serializable value, BamError error)
  {
    Method queryErrorHandler = _queryErrorHandlers.get(value.getClass());

    if (queryErrorHandler == null)
      return;
    
    try {
      queryErrorHandler.invoke(target, id, to, from, value, error);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresence(C target, String to, String from,
                               Serializable value)
  {
    Method presenceHandler = _presenceHandlers.get(value.getClass());

    if (presenceHandler == null)
      return;
    
    try {
      presenceHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceProbe(C target, String to, String from,
                                    Serializable value)
  {
    Method presenceProbeHandler = _presenceProbeHandlers.get(value.getClass());

    if (presenceProbeHandler == null)
      return;
    
    try {
      presenceProbeHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceSubscribe(C target, String to, String from,
                                    Serializable value)
  {
    Method presenceSubscribeHandler = 
      _presenceSubscribeHandlers.get(value.getClass());

    if (presenceSubscribeHandler == null)
      return;
    
    try {
      presenceSubscribeHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceSubscribed(C target, String to, String from,
                                    Serializable value)
  {
    Method presenceSubscribedHandler = 
      _presenceSubscribedHandlers.get(value.getClass());

    if (presenceSubscribedHandler == null)
      return;
    
    try {
      presenceSubscribedHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceUnsubscribe(C target, String to, String from,
                                    Serializable value)
  {
    Method presenceUnsubscribeHandler = 
      _presenceUnsubscribeHandlers.get(value.getClass());

    if (presenceUnsubscribeHandler == null)
      return;
    
    try {
      presenceUnsubscribeHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceUnsubscribed(C target, String to, String from,
                                    Serializable value)
  {
    Method presenceUnsubscribedHandler = 
      _presenceUnsubscribedHandlers.get(value.getClass());

    if (presenceUnsubscribedHandler == null)
      return;
    
    try {
      presenceUnsubscribedHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceUnavailable(C target, String to, String from,
                                    Serializable value)
  {
    Method presenceUnavailableHandler = 
      _presenceUnavailableHandlers.get(value.getClass());

    if (presenceUnavailableHandler == null)
      return;
    
    try {
      presenceUnavailableHandler.invoke(target, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void dispatchPresenceError(C target, String to, String from,
                                    Serializable value, BamError error)
  {
    Method presenceErrorHandler = _presenceErrorHandlers.get(value.getClass());

    if (presenceErrorHandler == null)
      return;
    
    try {
      presenceErrorHandler.invoke(target, to, from, value, error);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Returns the class of the message that a method observes, if it is
   * a message-observing method.  A message-observing method looks like
   *
   *   void foo(String to, String from, @Message Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getMessageObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(Message.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the message that a method observes, if it is
   * a message error-observing method.  A message error-observing method 
   * looks like
   *
   *   void foo(String to, String from, @MessageError Bar bar, BamError error)
   *
   * where Bar implements Serializable.
   **/
  private Class getMessageErrorObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 4)
      return null;

    // check for to and from and error parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]) ||
        ! BamError.class.equals(parameterTypes[3]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(MessageError.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the query that a method observes, if it is
   * a query-observing method.  A query-observing method looks like
   *
   *   boolean foo(long id, String to, String from, @QueryGet Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getQueryGetObserved(Method method)
  {
    if (! boolean.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 4)
      return null;

    // check for id, to, and from parameters
    if (! long.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]) ||
        ! String.class.equals(parameterTypes[2]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[3]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(QueryGet.class, parameterAnnotations[3]))
      return null;

    return parameterTypes[3];
  }

  /**
   * Returns the class of the query that a method observes, if it is
   * a query-observing method.  A query-observing method looks like
   *
   *   boolean foo(long id, String to, String from, @QuerySet Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getQuerySetObserved(Method method)
  {
    if (! boolean.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 4)
      return null;

    // check for id, to, and from parameters
    if (! long.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]) ||
        ! String.class.equals(parameterTypes[2]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[3]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(QuerySet.class, parameterAnnotations[3]))
      return null;

    return parameterTypes[3];
  }

  /**
   * Returns the class of the query that a method observes, if it is
   * a query-observing method.  A query-observing method looks like
   *
   *   boolean foo(long id, String to, String from, @QueryResult Bar bar)
   *
   * or
   *
   *   void foo(long id, String to, String from, @QueryResult Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getQueryResultObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()) &&
        ! boolean.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 4)
      return null;

    // check for id, to, and from parameters
    if (! long.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]) ||
        ! String.class.equals(parameterTypes[2]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[3]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(QueryResult.class, parameterAnnotations[3]))
      return null;

    return parameterTypes[3];
  }

  /**
   * Returns the class of the query that a method observes, if it is
   * a query error-observing method.  A query error-observing method 
   * looks like
   *
   *   void foo(long id, String to, String from, @QueryError Bar bar, 
   *            BamError error)
   *
   * where Bar implements Serializable.
   **/
  private Class getQueryErrorObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 5)
      return null;

    // check for id, to, from, and error parameters
    if (! long.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]) ||
        ! String.class.equals(parameterTypes[2]) ||
        ! BamError.class.equals(parameterTypes[4]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[3]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(QueryError.class, parameterAnnotations[3]))
      return null;

    return parameterTypes[3];
  }

  /**
   * Returns the class of the presence that a method observes, if it is
   * a presence-observing method.  A presence-observing method looks like
   *
   *   void foo(String to, String from, @Presence Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(Presence.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence probe that a method observes, if it is
   * a presence probe-observing method.  A presence probe-observing method 
   * looks like
   *
   *   void foo(String to, String from, @PresenceProbe Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceProbeObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceProbe.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence subscribe that a method observes, 
   * if it is a presence subscribe-observing method.  A presence 
   * subscribe-observing method looks like
   *
   *   void foo(String to, String from, @PresenceSubscribe Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceSubscribeObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceSubscribe.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence subscribed that a method observes, 
   * if it is a presence subscribed-observing method.  A presence 
   * subscribed-observing method looks like
   *
   *   void foo(String to, String from, @PresenceSubscribed Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceSubscribedObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceSubscribed.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence unsubscribe that a method observes, 
   * if it is a presence unsubscribe-observing method.  A presence 
   * unsubscribe-observing method looks like
   *
   *   void foo(String to, String from, @PresenceUnsubscribe Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceUnsubscribeObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceUnsubscribe.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence unsubscribed that a method observes, 
   * if it is a presence unsubscribed-observing method.  A presence 
   * unsubscribed-observing method looks like
   *
   *   void foo(String to, String from, @PresenceUnsubscribed Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceUnsubscribedObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceUnsubscribed.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence unavailabled that a method observes, 
   * if it is a presence unavailable-observing method.  A presence 
   * unavailable-observing method looks like
   *
   *   void foo(String to, String from, @PresenceUnavailable Bar bar)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceUnavailableObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 3)
      return null;

    // check for to and from parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceUnavailable.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  /**
   * Returns the class of the presence that a method observes, if it is
   * a presence error-observing method.  A presence error-observing method 
   * looks like
   *
   *   void foo(String to, String from, @PresenceError Bar bar, BamError error)
   *
   * where Bar implements Serializable.
   **/
  private Class getPresenceErrorObserved(Method method)
  {
    if (! void.class.equals(method.getReturnType()))
      return null;

    Class[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length != 4)
      return null;

    // check for to and from and error parameters
    if (! String.class.equals(parameterTypes[0]) ||
        ! String.class.equals(parameterTypes[1]) ||
        ! BamError.class.equals(parameterTypes[3]))
      return null;

    if (! Serializable.class.isAssignableFrom(parameterTypes[2]))
      return null;

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (! contains(PresenceError.class, parameterAnnotations[2]))
      return null;

    return parameterTypes[2];
  }

  private boolean contains(Class annotation, Annotation[] annotations)
  {
    for (int i = 0; i < annotations.length; i++) {
      if (annotation.isInstance(annotations[i]))
        return true;
    }

    return false;
  }
}
