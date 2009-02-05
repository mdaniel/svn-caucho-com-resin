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
import com.caucho.util.*;

import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.SimpleActor}
 * or {@link com.caucho.bam.SimpleActorStream}.
 */
class Skeleton<S extends SimpleActorStream>
{
  private static final L10N L = new L10N(Skeleton.class);
  private static final Logger log
    = Logger.getLogger(Skeleton.class.getName());

  private final static WeakHashMap<Class, Skeleton> _skeletons
    = new WeakHashMap<Class, Skeleton>();

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

  private Skeleton(Class<S> cl)
  {
    _cl = cl;
    
    log.finest(L.l("{0} introspecting class {1}", this, cl.getName()));

    introspect(cl);
  }

  /**
   * Dispatches a message to the actorStream.
   */
  public void message(S actorStream,
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
	log.finest(actorStream + " message " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " message ignored " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void messageError(S actorStream,
			   String to,
			   String from,
			   Serializable payload,
			   ActorError error)
  {
    Method handler;

    if (payload != null)
      handler = _messageErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " messageError " + error + " " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload, error);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " messageError ignored " + error + " " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void queryGet(S actorStream,
		       long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _queryGetHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " queryGet " + payload
		   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, id, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " queryGet not implemented for " + payload
		 + " {id: " + id + ", from: " + from + " to: " + to + "}");
      }
      
      String msg;
      msg = (actorStream + ": queryGet is not implemented for this payload:\n"
	     + "  " + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

      ActorError error = new ActorError(ActorError.TYPE_CANCEL,
					ActorError.FEATURE_NOT_IMPLEMENTED,
					msg);

      actorStream.getBrokerStream().queryError(id, from, to, payload, error);
    }
  }

  public void querySet(S actorStream,
		       long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _querySetHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " querySet " + payload
		   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, id, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " querySet not implemented for " + payload
		 + " {id: " + id + ", from: " + from + " to: " + to + "}");
      }

      String msg;
      msg = (actorStream + ": querySet is not implemented for this payload:\n"
	     + "  " + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

      ActorError error = new ActorError(ActorError.TYPE_CANCEL,
					ActorError.FEATURE_NOT_IMPLEMENTED,
					msg);

      actorStream.getBrokerStream().queryError(id, from, to, payload, error);
    }
  }

  public void queryResult(S actorStream,
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
	log.finest(actorStream + " queryResult " + payload
		   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, id, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " queryResult ignored " + payload
		 + " {id: " + id + ", from: " + from + " to: " + to + "}");
      }
    }
  }

  public void queryError(S actorStream,
			 long id,
			 String to,
			 String from,
			 Serializable payload,
			 ActorError error)
  {
    Method handler;

    if (payload != null)
      handler = _queryErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " queryError " + error + " " + payload
		   + " {id: " + id + ", from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, id, to, from, payload, error);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " queryError ignored " + error + " " + payload
		 + " {id: " + id + ", from: " + from + " to: " + to + "}");
      }
    }
  }

  public void presence(S actorStream,
		       String to,
		       String from,
		       Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presence " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presence ignored " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void presenceProbe(S actorStream,
			    String to,
			    String from,
			    Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceProbeHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceProbe " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presenceProbe ignored " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void presenceSubscribe(S actorStream,
				String to,
				String from,
				Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceSubscribeHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceSubscribe " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presenceSubscribe not implemented " + payload
		 + " {from: " + from + " to: " + to + "}");
      }

      String msg;
      msg = (actorStream + " presenceSubscribe does not implement the payload.\n"
	     + payload + " {from:" + from + ", to:" + to + "}");

      ActorError error = new ActorError(ActorError.TYPE_CANCEL,
					ActorError.FEATURE_NOT_IMPLEMENTED,
					msg);

      actorStream.getBrokerStream().presenceError(from, to, payload, error);
    }
  }

  public void presenceSubscribed(S actorStream,
				 String to,
				 String from,
				 Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceSubscribedHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceSubscribed " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presenceSubscribed ignored " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void presenceUnsubscribe(S actorStream,
				  String to,
				  String from,
				  Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceUnsubscribeHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceUnsubscribe " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presenceUnsubscribe not implemented " + payload
		 + " {from: " + from + " to: " + to + "}");
      }

      String msg;
      msg = (actorStream + " presenceUnsubscribe does not implement the payload.\n"
	     + payload + " {from:" + from + ", to:" + to + "}");

      ActorError error = new ActorError(ActorError.TYPE_CANCEL,
					ActorError.FEATURE_NOT_IMPLEMENTED,
					msg);

      actorStream.getBrokerStream().presenceError(from, to, payload, error);
    }
  }

  public void presenceUnsubscribed(S actorStream,
				   String to,
				   String from,
				   Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceUnsubscribedHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceUnsubscribed " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presenceUnsubscribed ignored " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void presenceUnavailable(S actorStream,
				  String to,
				  String from,
				  Serializable payload)
  {
    Method handler;

    if (payload != null)
      handler = _presenceUnavailableHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceUnavailable " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream + " presenceUnavailable ignored " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  public void presenceError(S actorStream,
			    String to,
			    String from,
			    Serializable payload,
			    ActorError error)
  {
    Method handler;

    if (payload != null)
      handler = _presenceErrorHandlers.get(payload.getClass());
    else
      handler = null;

    if (handler != null) {
      if (log.isLoggable(Level.FINEST)) {
	log.finest(actorStream + " presenceError " + error + " " + payload
		   + " {from:" + from + ", to:" + to + "}");
      }
      
      try {
	handler.invoke(actorStream, to, from, payload, error);
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
      if (log.isLoggable(Level.FINE)) {
	log.fine(actorStream
		 + " presenceError ignored " + error + " " + payload
		 + " {from: " + from + " to: " + to + "}");
      }
    }
  }

  //
  // introspection
  //

  protected void introspect(Class cl)
  {
    if (cl == null)
      return;

    introspect(cl.getSuperclass());
    
    Method[] methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      
      Class payloadType = getPayloadType(Message.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @Message {1} method={2}",
		       this, payloadType.getName(), method));

	method.setAccessible(true);
	
        _messageHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getPayloadType(MessageError.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @MessageError {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);
	
        _messageErrorHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(QueryGet.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QueryGet {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);
	
        _queryGetHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(QuerySet.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QuerySet {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);

        _querySetHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getQueryPayloadType(QueryResult.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QueryResult {1} method={2}",
		       this, payloadType.getName(), method));

	method.setAccessible(true);
	
        _queryResultHandlers.put(payloadType, method);
        continue;
      }
      
      payloadType = getQueryErrorPayloadType(QueryError.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @QueryError {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);
	
        _queryErrorHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getPayloadType(Presence.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @Presence {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);
	
        _presenceHandlers.put(payloadType, method);
	
        continue;
      }

      payloadType = getPayloadType(PresenceProbe.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceProbe {1} method={2}",
		       this, payloadType.getName(), method));

	method.setAccessible(true);
	
        _presenceProbeHandlers.put(payloadType, methods[i]);
	
        continue;
      }

      payloadType = getPayloadType(PresenceSubscribe.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceSubscribe {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);
	
        _presenceSubscribeHandlers.put(payloadType, methods[i]);
        continue;
      }

      payloadType = getPayloadType(PresenceSubscribed.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceSubscribe {1} method={2}",
		       this, payloadType.getName(), method));

	method.setAccessible(true);
	
        _presenceSubscribedHandlers.put(payloadType, methods[i]);
        continue;
      }

      payloadType = getPayloadType(PresenceUnsubscribe.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceUnsubscribe {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);

        _presenceUnsubscribeHandlers.put(payloadType, methods[i]);
        continue;
      }

      payloadType = getPayloadType(PresenceUnsubscribed.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceUnsubscribed {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);

        _presenceUnsubscribedHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getPayloadType(PresenceUnavailable.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceUnavailable {1} method={2}",
		       this, payloadType.getName(), method));
	
	method.setAccessible(true);
	
        _presenceUnavailableHandlers.put(payloadType, method);
        continue;
      }

      payloadType = getPayloadType(PresenceError.class, method);

      if (payloadType != null) {
        log.finest(L.l("{0} @PresenceError {1} method={2}",
		       this, payloadType.getName(), method));

	method.setAccessible(true);
	
        _presenceErrorHandlers.put(payloadType, methods[i]);
        continue;
      }
    }
  }

  private Class getPayloadType(Class annotationType, Method method)
  {
    Class []paramTypes = method.getParameterTypes();

    if (paramTypes.length < 3)
      return null;

    if (method.isAnnotationPresent(annotationType))
      return paramTypes[2];
    else
      return null;
  }

  private Class getQueryPayloadType(Class annotationType, Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;
    
    Class []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 4
	|| ! long.class.equals(paramTypes[0])
	|| ! String.class.equals(paramTypes[1])
	|| ! String.class.equals(paramTypes[2])
	|| ! Serializable.class.isAssignableFrom(paramTypes[3])) {
      throw new ActorException(method + " is an invalid "
			     + " @" + annotationType.getSimpleName()
			     + " because queries require (long, String, String, MyPayload)");
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

  private Class getQueryErrorPayloadType(Class annotationType, Method method)
  {
    if (! method.isAnnotationPresent(annotationType))
      return null;
    
    Class []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 5
	|| ! long.class.equals(paramTypes[0])
	|| ! String.class.equals(paramTypes[1])
	|| ! String.class.equals(paramTypes[2])
	|| ! Serializable.class.isAssignableFrom(paramTypes[3])
	|| ! ActorError.class.isAssignableFrom(paramTypes[4])) {
      throw new ActorException(method + " is an invalid "
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
 
  public static Skeleton getSkeleton(Class cl)
  {
    synchronized(_skeletons) {
      Skeleton skeleton = _skeletons.get(cl);

      if (skeleton == null) {
        skeleton = new Skeleton(cl);
        _skeletons.put(cl, skeleton);
      }

      return skeleton;
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cl.getName() + "]";
  }
}
