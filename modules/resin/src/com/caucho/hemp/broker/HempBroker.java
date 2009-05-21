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
 * @author Scott Ferguson
 */

package com.caucho.hemp.broker;

import com.caucho.bam.ActorManager;
import com.caucho.bam.Broker;
import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorError;
import com.caucho.bam.Actor;
import com.caucho.bam.AbstractActor;
import com.caucho.bam.ActorStream;
import com.caucho.bam.NotAuthorizedException;
import com.caucho.config.inject.BeanStartupEvent;
import com.caucho.config.inject.CauchoBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.hemp.*;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.security.*;
import com.caucho.server.cluster.Server;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import com.caucho.hemp.BamServiceBinding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.lang.ref.*;
import java.io.Serializable;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * Broker
 */
public class HempBroker implements Broker, ActorStream
{
  private static final Logger log
    = Logger.getLogger(HempBroker.class.getName());
  private static final L10N L = new L10N(HempBroker.class);

  private static final EnvironmentLocal<HempBroker> _localBroker
    = new EnvironmentLocal<HempBroker>();

  private final AtomicLong _jidGenerator
    = new AtomicLong(Alarm.getCurrentTime());

  private HempBrokerManager _manager;
  private DomainManager _domainManager;
  
  // actors
  private final
    ConcurrentHashMap<String,WeakReference<ActorStream>> _actorStreamMap
    = new ConcurrentHashMap<String,WeakReference<ActorStream>>();
  
  private final HashMap<String,Actor> _actorMap
    = new HashMap<String,Actor>();
  
  private final Map<String,WeakReference<Actor>> _actorCache
    = Collections.synchronizedMap(new HashMap<String,WeakReference<Actor>>());
  
  private String _serverId;

  private String _domain = "localhost";
  private String _managerJid = "localhost";
  private HempDomainService _domainService;

  private ArrayList<String> _aliasList = new ArrayList<String>();

  private ActorManager []_actorManagerList = new ActorManager[0];

  private volatile boolean _isClosed;

  public HempBroker()
  {
    Server server = Server.getCurrent();

    if (server == null) {
      throw new IllegalStateException(L.l("{0} must be created from an active server context",
					  this));
    }

    _serverId = server.getServerId();

    _manager = HempBrokerManager.getCurrent();
    _domainManager = DomainManager.getCurrent();
    
    _domainService = new HempDomainService(this, "");

    if (_localBroker.getLevel() == null)
      _localBroker.set(this);

    Environment.addCloseListener(this);
  }

  public HempBroker(String domain)
  {
    Server server = Server.getCurrent();

    if (server == null) {
      throw new IllegalStateException(L.l("{0} must be created from an active server context",
					  this));
    }

    _serverId = server.getServerId();
    
    _manager = HempBrokerManager.getCurrent();
    _domainManager = DomainManager.getCurrent();
    
    _domain = domain;
    _managerJid = domain;

    _domainService = new HempDomainService(this, domain);

    if (_localBroker.getLevel() == null)
      _localBroker.set(this);
  }

  public static HempBroker getCurrent()
  {
    return _localBroker.get();
  }

  /**
   * Returns true if the broker is closed
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Adds a domain alias
   */
  public void addAlias(String domain)
  {
    _aliasList.add(domain);
  }
  
  /**
   * Returns the stream to the broker
   */
  public ActorStream getBrokerStream()
  {
    return this;
  }
  
  /**
   * Returns the domain service
   */
  public Actor getDomainService()
  {
    return _domainService;
  }

  //
  // configuration
  //
  
  /**
   * Adds a broker implementation, e.g. the IM broker.
   */
  public void addActorManager(ActorManager actorManager)
  {
    ActorManager []actorManagerList
      = new ActorManager[_actorManagerList.length + 1];
    
    System.arraycopy(_actorManagerList, 0, actorManagerList, 0,
		     _actorManagerList.length);
    actorManagerList[actorManagerList.length - 1] = actorManager;
    _actorManagerList = actorManagerList;
  }

  //
  // API
  //

  /**
   * Creates a session
   */
  public ActorClient getConnection(String uid,
				   String resourceId)
  {
    return getConnection(null, uid, resourceId);
  }

  /**
   * Creates a session
   */
  public ActorClient getConnection(ActorStream actorStream,
				   String uid,
				   String resourceId)
  {
    String jid = generateJid(uid, resourceId);

    HempConnectionImpl conn = new HempConnectionImpl(this, jid, actorStream);

    actorStream = conn.getActorStream();
    
    _actorStreamMap.put(jid, new WeakReference<ActorStream>(actorStream));

    if (log.isLoggable(Level.FINE))
      log.fine(conn + " created");

    int p = jid.indexOf('/');
    if (p > 0) {
      String owner = jid.substring(0, p);
      
      Actor resource = findParentActor(owner);

      if (resource != null)
	resource.onChildStart(jid);
    }

    return conn;
  }

  protected String generateJid(String uid, String resource)
  {
    StringBuilder sb = new StringBuilder();

    if (uid == null)
      uid = "anonymous";

    if (uid.indexOf('@') > 0)
      sb.append(uid);
    else
      sb.append(uid).append('@').append(getDomain());
    sb.append("/");

    if (resource != null)
      sb.append(resource);
    else {
      Base64.encode(sb, _jidGenerator.incrementAndGet());
    }
    
    return sb.toString();
  }
  
  /**
   * Registers a actor
   */
  public void addActor(Actor actor)
  {
    String jid = actor.getJid();

    synchronized (_actorMap) {
      Actor oldActor = _actorMap.get(jid);

      if (oldActor != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));
      
      _actorMap.put(jid, actor);
      _actorCache.put(jid, new WeakReference<Actor>(actor));
    }
    
    synchronized (_actorStreamMap) {
      WeakReference<ActorStream> oldRef = _actorStreamMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));

      ActorStream actorStream = actor.getActorStream();
      _actorStreamMap.put(jid, new WeakReference<ActorStream>(actorStream));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " addActor jid=" + jid + " " + actor);
 }
  
  /**
   * Removes a actor
   */
  public void removeActor(Actor actor)
  {
    String jid = actor.getJid();
    
    synchronized (_actorMap) {
      _actorMap.remove(jid);
    }
    
    _actorCache.remove(jid);
    
    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(jid);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " removeActor jid=" + jid + " " + actor);
  }

  /**
   * Returns the manager's own id.
   */
  protected String getManagerJid()
  {
    return _managerJid;
  }

  /**
   * Returns the domain
   */
  protected String getDomain()
  {
    return _domain;
  }
  
  /**
   * getJid() returns null for the broker
   */
  public String getJid()
  {
    return _domain;
  }

  /**
   * Presence
   */
  public void presence(String to, String from, Serializable payload)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presence(to, from, payload);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " presence (no actor) " + payload
		  + " {to:" + to + ", from:" + from + "}");
      }
    }
  }

  /**
   * Presence unavailable
   */
  public void presenceUnavailable(String to,
				      String from,
				      Serializable data)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceUnavailable(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceUnavailable (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Presence probe
   */
  public void presenceProbe(String to,
			        String from,
			        Serializable data)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceProbe(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceProbe (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Presence subscribe
   */
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceSubscribe(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceSubscribe (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Presence subscribed
   */
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceSubscribed(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceSubscribed (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Presence unsubscribe
   */
  public void presenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceUnsubscribe(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceUnsubscribe (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Presence unsubscribed
   */
  public void presenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceUnsubscribed(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceUnsubscribed (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Presence error
   */
  public void presenceError(String to,
			        String from,
			        Serializable data,
			        ActorError error)
  {
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.presenceError(to, from, data, error);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresenceError (no resource) to=" + to
		  + " from=" + from + " value=" + data);
      }
    }
  }

  /**
   * Sends a message
   */
  public void message(String to, String from, Serializable value)
  {
    Alarm.yieldIfTest();
    
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.message(to, from, value);
    else {
      log.fine(this + " sendMessage to=" + to + " from=" + from
	       + " is an unknown actor stream.");
    }
  }

  /**
   * Sends a message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       ActorError error)
  {
    Alarm.yieldIfTest();
    
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.messageError(to, from, value, error);
    else {
      log.fine(this + " sendMessageError to=" + to + " from=" + from
	       + " error=" + error + " is an unknown actor stream.");
    }
  }

  /**
   * Query an entity
   */
  public void queryGet(long id, String to, String from,
			      Serializable payload)
  {
    Alarm.yieldIfTest();
    
    ActorStream stream = findActorStream(to);

    if (stream != null) {
      try {
	stream.queryGet(id, to, from, payload);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
	
	ActorError error = ActorError.create(e);
	
	queryError(id, from, to, payload, error);
      }

      return;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet to unknown stream to='" + to
	       + "' from=" + from);
    }

    String msg = L.l("'{0}' is an unknown actor for queryGet", to);
    
    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
				    ActorError.SERVICE_UNAVAILABLE,
				    msg);
				    
    queryError(id, from, to, payload, error);
  }

  /**
   * Query an entity
   */
  public void querySet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    Alarm.yieldIfTest();
    
    ActorStream stream = findActorStream(to);

    if (stream == null) {
      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " querySet to unknown stream '" + to
		 + "' from=" + from);
      }

      String msg = L.l("'{0}' is an unknown actor for querySet", to);
    
      ActorError error = new ActorError(ActorError.TYPE_CANCEL,
				      ActorError.SERVICE_UNAVAILABLE,
				      msg);
				    
      queryError(id, from, to, payload, error);

      return;
    }

    stream.querySet(id, to, from, payload);
  }

  /**
   * Query an entity
   */
  public void queryResult(long id, String to, String from, Serializable value)
  {
    Alarm.yieldIfTest();
    
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.queryResult(id, to, from, value);
    else
      throw new RuntimeException(L.l("{0}: {1} is an unknown actor stream.",
				     this, to));
  }

  /**
   * Query an entity
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable payload,
			 ActorError error)
  {
    Alarm.yieldIfTest();
    
    ActorStream stream = findActorStream(to);

    if (stream != null)
      stream.queryError(id, to, from, payload, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown actor stream.", to));
  }

  protected ActorStream findActorStream(String jid)
  {
    if (jid == null)
      return null;
    
    WeakReference<ActorStream> ref = _actorStreamMap.get(jid);

    if (ref != null) {
      ActorStream stream = ref.get();

      if (stream != null)
	return stream;
    }
    
    if (jid.endsWith("@")) {
      // jms/3d00
      jid = jid + getDomain();
    }

    ActorStream actorStream;
    Actor actor = findParentActor(jid);

    if (actor == null) {
      return putActorStream(jid, findDomain(jid));
    }
    else if (jid.equals(actor.getJid())) {
      actorStream = actor.getActorStream();

      if (actorStream != null) {
	return putActorStream(jid, actorStream);
      }
    }
    else {
      if (! actor.startChild(jid))
        return null;

      ref = _actorStreamMap.get(jid);

      if (ref != null)
	return ref.get();
    }

    return null;
  }

  private ActorStream putActorStream(String jid, ActorStream actorStream)
  {
    if (actorStream == null)
      return null;
    
    synchronized (_actorStreamMap) {
      WeakReference<ActorStream> ref = _actorStreamMap.get(jid);

      if (ref != null)
	return ref.get();

      _actorStreamMap.put(jid, new WeakReference<ActorStream>(actorStream));

      return actorStream;
    }
  }

  protected Actor findParentActor(String jid)
  {
    if (jid == null)
      return null;

    WeakReference<Actor> ref = _actorCache.get(jid);

    if (ref != null)
      return ref.get();

    if (startActorFromManager(jid)) {
      ref = _actorCache.get(jid);

      if (ref != null)
	return ref.get();
    }

    if (jid.indexOf('/') < 0 && jid.indexOf('@') < 0) {
      Broker broker = _manager.findBroker(jid);
      Actor actor = null;

      if (broker instanceof HempBroker) {
	HempBroker hempBroker = (HempBroker) broker;

	actor = hempBroker.getDomainService();
      }

      if (actor != null) {
	ref = _actorCache.get(jid);

	if (ref != null)
	  return ref.get();

	_actorCache.put(jid, new WeakReference<Actor>(actor));

	return actor;
      }
    }
    
    int p;

    if ((p = jid.indexOf('/')) > 0) {
      String uid = jid.substring(0, p);

      return findParentActor(uid);
    }
    else if ((p = jid.indexOf('@')) > 0) {
      String domainName = jid.substring(p + 1);
      
      return findParentActor(domainName);
    }
    else
      return null;
  }

  protected ActorStream findDomain(String domain)
  {
    if (domain == null)
      return null;

    if ("local".equals(domain))
      return getBrokerStream();

    Broker broker = _manager.findBroker(domain);

    if (broker instanceof HempBroker) {
      HempBroker hempBroker = (HempBroker) broker;

      Actor actor = hempBroker.getDomainService();

      return actor.getActorStream();
    }
    
    if (broker == this)
      return null;

    ActorStream stream = null;
    
    if (_domainManager != null)
      stream = _domainManager.findDomain(domain);

    return stream;
  }

  protected boolean startActorFromManager(String jid)
  {
    for (ActorManager manager : _actorManagerList) {
      if (manager.startActor(jid))
        return true;
    }

    return false;
  }
  
  /**
   * Closes a connection
   */
  void closeActor(String jid)
  {
    int p = jid.indexOf('/');
    if (p > 0) {
      String owner = jid.substring(0, p);
      
      Actor actor = findParentActor(owner);

      if (actor != null) {
	try {
	  actor.onChildStop(jid);
	} catch (Exception e) {
	  log.log(Level.FINE, e.toString(), e);
	}
      }
    }
    
    _actorCache.remove(jid);
    
    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(jid);
    }
  }

  //
  // webbeans callbacks
  //

  /**
   * Called when a @Actor is annotated on the actor
   */
  public void registerActor(@Observes @BamServiceBinding
			    BeanStartupEvent event)
  {
    BeanManager manager = event.getManager();
    Bean bean = event.getBean();

    if (bean instanceof CauchoBean) {
      CauchoBean cauchoBean = (CauchoBean) bean;

      AbstractActor actor
	= manager.getReference(bean, AbstractActor.class);

      Annotation []ann = cauchoBean.getAnnotations();

      actor.setBrokerStream(this);

      String jid = getJid(actor, ann);

      actor.setJid(jid);

      int threadMax = getThreadMax(ann);

      Actor bamActor = actor;

      // queue
      if (threadMax > 0) {
	bamActor = new MemoryQueueServiceFilter(bamActor,
						  this,
						  threadMax);
      }

      addActor(bamActor);

      Environment.addCloseListener(new ActorClose(bamActor));
    }
    else {
      log.warning(this + " can't register " + bean + " because it's not a CauchoBean");
    }
  }

  public void close()
  {
    _isClosed = true;
    
    _manager.removeBroker(_domain);

    for (String alias : _aliasList)
      _manager.removeBroker(alias);

    _actorMap.clear();
    _actorCache.clear();
    _actorStreamMap.clear();
  }

  private String getJid(Actor actor, Annotation []annList)
  {
    com.caucho.config.BamService bamAnn = findActor(annList);

    String name = "";

    if (bamAnn != null)
      name = bamAnn.name();
    
    if (name == null || "".equals(name))
      name = actor.getJid();

    if (name == null || "".equals(name))
      name = actor.getClass().getSimpleName();

    String jid = name;
    if (jid.indexOf('@') < 0 && jid.indexOf('/') < 0)
      jid = name + "@" + getJid();

    return jid;
  }

  private int getThreadMax(Annotation []annList)
  {
    com.caucho.config.BamService bamAnn = findActor(annList);

    if (bamAnn != null)
      return bamAnn.threadMax();
    else
      return 1;
  }

  private com.caucho.config.BamService findActor(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().equals(com.caucho.config.BamService.class))
	return (com.caucho.config.BamService) ann;

      // XXX: stereotypes
    }

    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _domain + "]";
  }

  class ActorClose {
    private Actor _actor;

    ActorClose(Actor actor)
    {
      _actor = actor;
    }
    
    public void close()
    {
      removeActor(_actor);
    }
  }
}
