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
 * @author Scott Ferguson
 */

package com.caucho.hemp.broker;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.Actor;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.broker.AbstractManagedBroker;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.mailbox.PassthroughMailbox;
import com.caucho.bam.manager.BamManager;
import com.caucho.bam.manager.SimpleBamManager;
import com.caucho.bam.packet.Message;
import com.caucho.bam.packet.MessageError;
import com.caucho.bam.packet.Packet;
import com.caucho.bam.packet.Query;
import com.caucho.bam.packet.QueryError;
import com.caucho.bam.packet.QueryResult;
import com.caucho.bam.proxy.ProxyActor;
import com.caucho.bam.stream.MessageStream;
import com.caucho.config.inject.InjectManager;
import com.caucho.env.service.AfterResinStartListener;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.admin.AdminService;
import com.caucho.util.Alarm;
import com.caucho.util.Base64;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

/**
 * Broker
 */
public class HempBroker extends AbstractManagedBroker
{
  private static final Logger log
    = Logger.getLogger(HempBroker.class.getName());
  private static final L10N L = new L10N(HempBroker.class);
  
  private final static EnvironmentLocal<HempBroker> _localBroker
    = new EnvironmentLocal<HempBroker>();

  private final AtomicLong _addressGenerator
    = new AtomicLong(CurrentTime.getCurrentTime());

  private HempBrokerManager _manager;
  private DomainManager _domainManager;
  private BamManager _bamManager;

  // actors and clients
  private final
    ConcurrentHashMap<String,WeakReference<Mailbox>> _actorStreamMap
    = new ConcurrentHashMap<String,WeakReference<Mailbox>>();

  // permanent registered actors
  private final HashMap<String,Mailbox> _actorMap
    = new HashMap<String,Mailbox>();

  private final Map<String,WeakReference<Mailbox>> _actorCache
    = Collections.synchronizedMap(new HashMap<String,WeakReference<Mailbox>>());

  private String _domain = "localhost";
  private String _managerAddress = "localhost";

  private ArrayList<String> _aliasList = new ArrayList<String>();
  
  private ArrayList<Packet> _startupPacketList = new ArrayList<Packet>();

  private ResinSystem _resinSystem;
  /*
  private BrokerListener []_actorManagerList
    = new BrokerListener[0];
    */

  private volatile boolean _isClosed;

  public HempBroker(HempBrokerManager manager)
  {
    _resinSystem = manager.getResinSystem();
    _manager = manager;
    _bamManager = new SimpleBamManager(this);
    
    Environment.addCloseListener(this);

    if (_localBroker.getLevel() == null)
      _localBroker.set(this);
    
    if (_resinSystem != null)
      _resinSystem.addListener(new AfterStartListener(this));
  }

  public HempBroker(HempBrokerManager manager, String domain)
  {
    this(manager);
    
    _domain = domain;
    _managerAddress = domain;
  }

  public static HempBroker getCurrent()
  {
    return _localBroker.get();
  }

  public BamManager getBamManager()
  {
    return _bamManager;
  }
  
  public void setDomainManager(DomainManager domainManager)
  {
    _domainManager = domainManager;
  }

  /**
   * Returns true if the broker is closed
   */
  @Override
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
  
  public void afterStart()
  {
    deliverStartupPackets();
    
    ArrayList<Packet> deadPackets = new ArrayList<Packet>(_startupPacketList);
    _startupPacketList.clear();
    
    for (Packet packet : deadPackets) {
      packet.dispatch(this, this);
    }
  }

  //
  // API
  //

  protected String generateAddress(String uid, String resource)
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
      Base64.encode(sb, _addressGenerator.incrementAndGet());
    }

    return sb.toString();
  }

  /**
   * Registers a actor
   */
  @Override
  public void addMailbox(String address, Mailbox mailbox)
  {
    synchronized (_actorMap) {
      Mailbox oldMailbox = _actorMap.get(address);

      if (oldMailbox != null)
        throw new IllegalStateException(L.l("duplicated address='{0}' is not allowed",
                                            address));

      _actorMap.put(address, mailbox);
    }

    synchronized (_actorStreamMap) {
      WeakReference<Mailbox> oldRef = _actorStreamMap.get(address);

      if (oldRef != null && oldRef.get() != null)
        throw new IllegalStateException(L.l("duplicated address='{0}' is not allowed",
                                            address));

      _actorStreamMap.put(address, new WeakReference<Mailbox>(mailbox));
    }

    if (log.isLoggable(Level.FINEST))
      log.finest(this + " addMailbox address=" + address + " " + mailbox);
    
    // if in startup phase, deliver the queued messages
    if (isBeforeActive()) {
      deliverStartupPackets();
    }
 }

  /**
   * Removes a actor
   */
  @Override
  public void removeMailbox(Mailbox mailbox)
  {
    String address = mailbox.getAddress();

    synchronized (_actorMap) {
      _actorMap.remove(address);
    }

    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(address);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " removeActor address=" + address + " " + mailbox);
  }

  /**
   * Returns the manager's own id.
   */
  protected String getManagerAddress()
  {
    return _managerAddress;
  }

  /**
   * Returns the domain
   */
  protected String getDomain()
  {
    return _domain;
  }

  /**
   * getAddress() returns null for the broker
   */
  @Override
  public String getAddress()
  {
    return _domain;
  }
  
  //
  // state methods
  //
  
  private boolean isBeforeActive()
  {
    if (_resinSystem != null)
      return _resinSystem.isBeforeActive();
    else
      return false;
  }
  
  //
  // packet methods
  //

  /**
   * Sends a message to the desination mailbox.
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    Mailbox mailbox = getMailbox(to);

    if (mailbox != null) {
      mailbox.message(to, from, payload);
      return;
    }
    
    // on startup, queue the messages until the startup completes
    if (isBeforeActive() && addStartupPacket(new Message(to, from, payload))) {
      // startup packets are successful
    }
    else {
      // use default error handling
      super.message(to, from, payload);
    }
  }

  /**
   * Sends a messageError to the desination mailbox.
   */
  @Override
  public void messageError(String to, 
                           String from, 
                           Serializable payload,
                           BamError error)
  {
    Mailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.messageError(to, from, payload, error);
      return;
    }
    
    // on startup, queue the messages until the startup completes
    if (isBeforeActive()
        && addStartupPacket(new MessageError(to, from, payload, error))) {
      // startup packets are successful
    }
    else {
      // use default error handling
      super.messageError(to, from, payload, error);
    }
  }

  /**
   * Sends a query to the destination mailbox.
   */
  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    Mailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.query(id, to, from, payload);
      return;
    }
    
    // on startup, queue the messages until the startup completes
    if (isBeforeActive()
        && addStartupPacket(new Query(id, to, from, payload))) {
      // startup packets are successful
    }
    else {
      // use default error handling
      super.query(id, to, from, payload);
    }
  }

  /**
   * Sends a query to the destination mailbox.
   */
  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    Mailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.queryResult(id, to, from, payload);
      return;
    }
    
    // on startup, queue the messages until the startup completes
    if (isBeforeActive()
        && addStartupPacket(new QueryResult(id, to, from, payload))) {
      // startup packets are successful
    }
    else {
      // use default error handling
      super.queryResult(id, to, from, payload);
    }
  }

  /**
   * Sends a query to the destination mailbox.
   */
  @Override
  public void queryError(long id, String to, String from, Serializable payload,
                         BamError error)
  {
    Mailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.queryError(id, to, from, payload, error);
      return;
    }
    
    // on startup, queue the messages until the startup completes
    if (isBeforeActive()
        && addStartupPacket(new QueryError(id, to, from, payload, error))) {
      // startup packets are successful
    }
    else {
      // use default error handling
      super.queryError(id, to, from, payload, error);
    }
  }
   
  private boolean addStartupPacket(Packet packet)
  {
    synchronized (_startupPacketList) {
      _startupPacketList.add(packet);
    }
    
    deliverStartupPackets();
    
    return true;
  }
  
  private void deliverStartupPackets()
  {
    Packet packet;
    
    while ((packet = extractStartupPacket()) != null) {
      Mailbox mailbox = getMailbox(packet.getTo());
      
      if (mailbox != null)
        packet.dispatch(mailbox, this);
      else {
        log.warning(this + " failed to find mailbox " + packet.getTo()
                    + " for " + packet);
      }
    }
  }
  
  /**
   * Return a queued packet that has an active mailbox.
   */
  private Packet extractStartupPacket()
  {
    synchronized (_startupPacketList) {
      int size = _startupPacketList.size();
      
      for (int i = 0; i < size; i++) {
        Packet packet = _startupPacketList.get(i);
        
        Mailbox mailbox = getMailbox(packet.getTo());
        
        if (mailbox != null) {
          _startupPacketList.remove(i);
          
          return packet;
        }
      }
    }
    
    return null;
  }
  
  //
  // mailbox methods
  //
  
  /**
   * Returns the mailbox for the given address
   */
  @Override
  public Mailbox getMailbox(String address)
  {
    if (address == null)
      return null;

    WeakReference<Mailbox> ref = _actorStreamMap.get(address);

    if (ref != null) {
      Mailbox mailbox = ref.get();

      if (mailbox != null)
        return mailbox;
    }

    if (address.endsWith("@")) {
      // jms/3d00
      address = address + getDomain();
    }

    return putActorStream(address, findDomain(address));
  }

  private Mailbox putActorStream(String address, Mailbox actorStream)
  {
    if (actorStream == null)
      return null;

    synchronized (_actorStreamMap) {
      WeakReference<Mailbox> ref = _actorStreamMap.get(address);

      if (ref != null)
        return ref.get();

      _actorStreamMap.put(address, new WeakReference<Mailbox>(actorStream));

      return actorStream;
    }
  }

  private Mailbox findDomain(String domain)
  {
    if (domain == null)
      return null;

    if ("local".equals(domain))
      return getBrokerMailbox();

    Broker broker = null;
    
    if (_manager != null)
      broker = _manager.findBroker(domain);

    if (broker == this)
      return null;

    Mailbox stream = null;

    if (_domainManager != null)
      stream = _domainManager.findDomain(domain);

    return stream;
  }

  protected boolean startActorFromManager(String address)
  {
    /*
    for (BrokerListener manager : _actorManagerList) {
      if (manager.startActor(address))
        return true;
    }
        */

    return false;
  }

  /**
   * Closes a connection
   */
  void closeActor(String address)
  {
    int p = address.indexOf('/');
    if (p > 0) {
      String owner = address.substring(0, p);

      ActorHolder actor = null;

      /*
      if (actor != null) {
        try {
          actor.onChildStop(address);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
      */
    }

    _actorCache.remove(address);

    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(address);
    }
  }

  //
  // CDI callbacks
  //

  public void addStartupActor(Bean bean,
                              String name,
                              int threadMax)
  {
    ActorStartup startup
      = new ActorStartup(bean, name, threadMax);

    Environment.addEnvironmentListener(startup);
  }

  private void startActor(Bean<?> bean,
                          String name,
                          int threadMax)
  {
    InjectManager beanManager = InjectManager.getCurrent();
    
    Object beanInstance = beanManager.getReference(bean);

    String address = createAddress(name, bean);
    
    Actor actor = createActor(beanInstance, address);

    Mailbox mailbox;
    
    // queue
    if (threadMax > 0) {
      mailbox = new MultiworkerMailbox(address, actor, this, threadMax);
      // bamActor.setActorStream(actorStream);
    }
    else {
      mailbox = new PassthroughMailbox(address, actor, this);
    }

    addMailbox(address, mailbox);

    Environment.addCloseListener(new ActorClose(mailbox));
  }
  
  private Actor createActor(Object instance, String address)
  {
    if (instance instanceof ActorHolder) {
      ActorHolder actor = (ActorHolder) instance;
      
      actor.setAddress(address);
      actor.setBroker(this);
      
      return actor.getActor();
    }
    else {
      ProxyActor proxyActor = new ProxyActor(instance, address, this);
      
      return proxyActor;
    }
  }
  
  private String createAddress(String address, Bean<?> bean)
  {
    if (address == null || "".equals(address))
      address = bean.getName();

    if (address == null || "".equals(address))
      address = bean.getBeanClass().getSimpleName();

    if (address.indexOf('@') < 0)
      address = address + '@' + getAddress();
    else if (address.endsWith("@"))
      address = address.substring(0, address.length() - 1);
    
    return address;

  }

  private void startActor(Bean bean, AdminService bamService)
  {
    InjectManager beanManager = InjectManager.getCurrent();

    ActorHolder actor = (ActorHolder) beanManager.getReference(bean);

    actor.setBroker(this);

    String address = bamService.name();

    if (address == null || "".equals(address))
      address = bean.getName();

    if (address == null || "".equals(address))
      address = bean.getBeanClass().getSimpleName();

    actor.setAddress(address);

    int threadMax = bamService.threadMax();

    ActorHolder bamActor = actor;
    Mailbox mailbox = null;
    // queue
    if (threadMax > 0) {
      MessageStream actorStream = bamActor.getActor();
      mailbox = new MultiworkerMailbox(address, actorStream, this, threadMax);
      bamActor.setMailbox(mailbox);
    }

    addMailbox(address, mailbox);

    Environment.addCloseListener(new ActorClose(mailbox));
  }

  public void close()
  {
    _isClosed = true;

    _manager.removeBroker(_domain);

    for (String alias : _aliasList)
      _manager.removeBroker(alias);
    
    ArrayList<Mailbox> mailboxes = new ArrayList<Mailbox>(_actorMap.values());

    _actorMap.clear();
    _actorCache.clear();
    _actorStreamMap.clear();
    
    for (Mailbox mailbox : mailboxes) {
      try {
        mailbox.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  private String getAddress(ActorHolder actor, Annotation []annList)
  {
    com.caucho.remote.BamService bamAnn = findActor(annList);

    String name = "";

    if (bamAnn != null)
      name = bamAnn.name();

    if (name == null || "".equals(name))
      name = actor.getAddress();

    if (name == null || "".equals(name))
      name = actor.getClass().getSimpleName();

    String address = name;
    if (address.indexOf('@') < 0 && address.indexOf('/') < 0)
      address = name + "@" + getAddress();

    return address;
  }

  private int getThreadMax(Annotation []annList)
  {
    com.caucho.remote.BamService bamAnn = findActor(annList);

    if (bamAnn != null)
      return bamAnn.threadMax();
    else
      return 1;
  }

  private com.caucho.remote.BamService findActor(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().equals(com.caucho.remote.BamService.class))
        return (com.caucho.remote.BamService) ann;

      // XXX: stereotypes
    }

    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _domain + "]";
  }

  public class ActorStartup implements EnvironmentListener{
    private Bean<?> _bean;
    private String _name;
    private int _threadMax;

    ActorStartup(Bean<?> bean, String name, int threadMax)
    {
      _bean = bean;

      _name = name;
      _threadMax = threadMax;
    }

    Bean<?> getBean()
    {
      return _bean;
    }

    String getName()
    {
      return _name;
    }

    int getThreadMax()
    {
      return _threadMax;
    }

    public void environmentConfigure(EnvironmentClassLoader loader)
    {
    }

    public void environmentBind(EnvironmentClassLoader loader)
    {
    }

    public void environmentStart(EnvironmentClassLoader loader)
    {
      startActor(_bean, _name, _threadMax);
    }

    public void environmentStop(EnvironmentClassLoader loader)
    {
    }
  }

  public class ActorClose {
    private Mailbox _actor;

    ActorClose(Mailbox actor)
    {
      _actor = actor;
    }

    public void close()
    {
      removeMailbox(_actor);
    }
  }
  
  static class AfterStartListener implements AfterResinStartListener {
    private WeakReference<HempBroker> _brokerRef;
    
    AfterStartListener(HempBroker broker)
    {
      _brokerRef = new WeakReference<HempBroker>(broker);
    }
    
    @Override
    public void afterStart()
    {
      HempBroker broker = _brokerRef.get();
      
      if (broker != null)
        broker.afterStart();
    }
  }
}
