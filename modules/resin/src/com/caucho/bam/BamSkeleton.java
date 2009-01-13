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

package com.caucho.bam;

import com.caucho.config.*;
import com.caucho.bam.annotation.*;
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
public class BamSkeleton<C>
{
  private static final L10N L = new L10N(BamSkeleton.class);
  private static final Logger log
    = Logger.getLogger(BamSkeleton.class.getName());

  private final static WeakHashMap<Class, BamSkeleton> _skeletons
    = new WeakHashMap<Class, BamSkeleton>();

  private Class _cl;
  
  private final HashMap<Class, Method> _messageHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _messageErrorHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _queryGetHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _querySetHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _queryResultHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _queryErrorHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceProbeHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceSubscribeHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceSubscribedHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceUnsubscribeHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceUnsubscribedHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceUnavailableHandlers
    = new HashMap<Class, Method>();
  private final HashMap<Class, Method> _presenceErrorHandlers
    = new HashMap<Class, Method>();

  private BamSkeleton(Class<C> cl)
  {
    _cl = cl;
    
    log.finest(L.l("{0} introspecting class {1}", this, cl.getName()));

    Method[] methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      
      Class messageType = getMessageType(Message.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @Message handler type={1} method={2}",
		       this, messageType.getName(), method));

	method.setAccessible(true);
	
        _messageHandlers.put(messageType, method);
        continue;
      }

      messageType = getMessageType(MessageError.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @MessageError handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);
	
        _messageErrorHandlers.put(messageType, method);
        continue;
      }

      messageType = getQueryMessageType(QueryGet.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @QueryGet handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);
	
        _queryGetHandlers.put(messageType, method);
        continue;
      }

      messageType = getQueryMessageType(QuerySet.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @QuerySet handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);

        _querySetHandlers.put(messageType, method);
        continue;
      }

      messageType = getQueryMessageType(QueryResult.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @QueryResult handler type={1} method={2}",
		       this, messageType.getName(), method));

	method.setAccessible(true);
	
        _queryResultHandlers.put(messageType, method);
        continue;
      }
      
      messageType = getQueryMessageType(QueryError.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @QueryError handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);
	
        _queryErrorHandlers.put(messageType, method);
        continue;
      }

      messageType = getMessageType(Presence.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @Presence handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);
	
        _presenceHandlers.put(messageType, method);
	
        continue;
      }

      messageType = getMessageType(PresenceProbe.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceProbe handler type={1} method={2}",
		       this, messageType.getName(), method));

	method.setAccessible(true);
	
        _presenceProbeHandlers.put(messageType, methods[i]);
	
        continue;
      }

      messageType = getMessageType(PresenceSubscribe.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceSubscribe handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);
	
        _presenceSubscribeHandlers.put(messageType, methods[i]);
        continue;
      }

      messageType = getMessageType(PresenceSubscribed.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceSubscribe handler type={1} method={2}",
		       this, messageType.getName(), method));

	method.setAccessible(true);
	
        _presenceSubscribedHandlers.put(messageType, methods[i]);
        continue;
      }

      messageType = getMessageType(PresenceUnsubscribe.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceUnsubscribe handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);

        _presenceUnsubscribeHandlers.put(messageType, methods[i]);
        continue;
      }

      messageType = getMessageType(PresenceUnsubscribed.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceUnsubscribed handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);

        _presenceUnsubscribedHandlers.put(messageType, method);
        continue;
      }

      messageType = getMessageType(PresenceUnavailable.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceUnavailable handler type={1} method={2}",
		       this, messageType.getName(), method));
	
	method.setAccessible(true);
	
        _presenceUnavailableHandlers.put(messageType, method);
        continue;
      }

      messageType = getMessageType(PresenceError.class, method);

      if (messageType != null) {
        log.finest(L.l("{0} found @PresenceError handler type={1} method={2}",
		       this, messageType.getName(), method));

	method.setAccessible(true);
	
        _presenceErrorHandlers.put(messageType, methods[i]);
        continue;
      }
    }
  }

  private Class getMessageType(Class annotationType, Method method)
  {
    Class []paramTypes = method.getParameterTypes();

    if (paramTypes.length < 3)
      return null;

    if (method.isAnnotationPresent(annotationType))
      return paramTypes[2];
    else
      return null;
  }

  private Class getQueryMessageType(Class annotationType, Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;
    
    Class []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 4
	|| ! long.class.equals(paramTypes[0])
	|| ! String.class.equals(paramTypes[1])
	|| ! String.class.equals(paramTypes[2])
	|| ! Serializable.class.isAssignableFrom(paramTypes[3])) {
      throw new BamException(method + " is an invalid @"
			     + annotationType.getSimpleName()
			     + " because queries require (long, String, String, MyValue)");
    }
    else if (! boolean.class.equals(method.getReturnType())) {
      throw new BamException(method + " is an invalid @"
			     + annotationType.getSimpleName()
			     + " because queries must return boolean");
    }

    return paramTypes[3];
  }
 
  public static BamSkeleton getBamSkeleton(Class cl)
  {
    synchronized(_skeletons) {
      BamSkeleton skeleton = _skeletons.get(cl);

      if (skeleton == null) {
        skeleton = new BamSkeleton(cl);
        _skeletons.put(cl, skeleton);
      }

      return skeleton;
    }
  }

  public void dispatchMessage(C target, String to, String from,
                              Serializable value)
  {
    Method messageHandler = _messageHandlers.get(value.getClass());

    if (messageHandler != null) {
      if (log.isLoggable(Level.FINER)) {
	log.finer(target + " message " + value + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	messageHandler.invoke(target, to, from, value);
      }
      catch (IllegalAccessException e) {
	// XXX: error
	log.log(Level.FINE, e.toString(), e);
      }
      catch (InvocationTargetException e) {
	// XXX: error
	log.log(Level.FINE, e.toString(), e);
      }
    }
    else {
      if (log.isLoggable(Level.FINE)) {
	log.fine(target + " unknown message " + value + " {from: " + from + " to: " + to + "}");
      }
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

    if (queryHandler != null) {
      if (log.isLoggable(Level.FINER)) {
	log.finer(target + " queryGet " + value + " {id:" + id + ", from:" + from + " to: " + to + "}");
      }
      
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
    else {
      return false;
    }
  }

  public boolean dispatchQuerySet(C target, long id, String to, String from,
                                  Serializable value)
  {
    Method queryHandler = _querySetHandlers.get(value.getClass());

    if (queryHandler == null)
      return false;
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(target + " querySet " + value + " {id:" + id + ", from:" + from + " to: " + to + "}");
    }
    
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

  public boolean dispatchQueryResult(C target, long id, String to, String from,
				     Serializable value)
  {
    if (value == null)
      return false;
    
    Method queryHandler = _queryResultHandlers.get(value.getClass());

    if (queryHandler == null)
      return false;
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(target + " queryResult " + value + " {id:" + id + ", from:" + from + " to: " + to + "}");
    }
      
    try {
      queryHandler.invoke(target, id, to, from, value);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return true;
  }

  public boolean dispatchQueryError(C target, long id, String to, String from,
				    Serializable value, BamError error)
  {
    Method queryErrorHandler = _queryErrorHandlers.get(value.getClass());

    if (queryErrorHandler == null)
      return false;
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(target + " queryError " + value + " {id:" + id + ", from:" + from + " to: " + to + "}");
    }
    
    try {
      queryErrorHandler.invoke(target, id, to, from, value, error);
    }
    catch (IllegalAccessException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (InvocationTargetException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return true;
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cl.getName() + "]";
  }
}
