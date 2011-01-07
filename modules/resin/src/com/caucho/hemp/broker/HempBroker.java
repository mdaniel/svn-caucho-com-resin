/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

import com.caucho.bam.actor.Actor;
import com.caucho.bam.broker.AbstractManagedBroker;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.mailbox.PassthroughMailbox;
import com.caucho.bam.stream.ActorStream;
import com.caucho.config.inject.InjectManager;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.admin.AdminService;
import com.caucho.util.Alarm;
import com.caucho.util.Base64;
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

  private final AtomicLong _jidGenerator
    = new AtomicLong(Alarm.getCurrentTime());

  private HempBrokerManager _manager;
  private DomainManager _domainManager;

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
  private String _managerJid = "localhost";

  private ArrayList<String> _aliasList = new ArrayList<String>();

  /*
  private BrokerListener []_actorManagerList
    = new BrokerListener[0];
    */

  private volatile boolean _isClosed;

  public HempBroker(HempBrokerManager manager)
  {
    _manager = manager;

    Environment.addCloseListener(this);

    if (_localBroker.getLevel() == null)
      _localBroker.set(this);
  }

  public HempBroker(HempBrokerManager manager, String domain)
  {
    this(manager);
    
    _domain = domain;
    _managerJid = domain;
  }

  public static HempBroker getCurrent()
  {
    return _localBroker.get();
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

  /**
   * Returns the stream to the broker
   */
  /*
  @Override
  public ActorStream getBrokerMailbox()
  {
    return this;
  }
  */

  //
  // configuration
  //

  /**
   * Adds a broker implementation, e.g. the IM broker.
   */
  /*
  public void addBrokerListener(BrokerListener actorManager)
  {
    BrokerListener []actorManagerList
      = new BrokerListener[_actorManagerList.length + 1];

    System.arraycopy(_actorManagerList, 0, actorManagerList, 0,
                     _actorManagerList.length);
    actorManagerList[actorManagerList.length - 1] = actorManager;
    _actorManagerList = actorManagerList;
  }
  */

  //
  // API
  //

  /**
   * Creates a session
   */
  /*
  public String createClient(ActorStream clientStream,
                             String uid,
                             String resourceId)
  {
    String jid = generateJid(uid, resourceId);

    _actorStreamMap.put(jid, new WeakReference<Mailbox>(clientStream));

    if (log.isLoggable(Level.FINE))
      log.fine(clientStream + " " + jid + " created");

    return jid;
  }
  */

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
  @Override
  public void addMailbox(Mailbox mailbox)
  {
    String jid = mailbox.getJid();

    synchronized (_actorMap) {
      Mailbox oldMailbox = _actorMap.get(jid);

      if (oldMailbox != null)
        throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
                                            jid));

      _actorMap.put(jid, mailbox);
    }

    synchronized (_actorStreamMap) {
      WeakReference<Mailbox> oldRef = _actorStreamMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
        throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
                                            jid));

      _actorStreamMap.put(jid, new WeakReference<Mailbox>(mailbox));
    }

    if (log.isLoggable(Level.FINEST))
      log.finest(this + " addMailbox jid=" + jid + " " + mailbox);
 }

  /**
   * Removes a actor
   */
  @Override
  public void removeMailbox(Mailbox mailbox)
  {
    String jid = mailbox.getJid();

    synchronized (_actorMap) {
      _actorMap.remove(jid);
    }

    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(jid);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " removeActor jid=" + jid + " " + mailbox);
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

  @Override
  public Mailbox getMailbox(String jid)
  {
    if (jid == null)
      return null;

    WeakReference<Mailbox> ref = _actorStreamMap.get(jid);

    if (ref != null) {
      Mailbox mailbox = ref.get();

      if (mailbox != null)
        return mailbox;
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
        return putActorStream(jid, new MultiworkerMailbox(jid, actorStream, this, 1));
      }
    }
    else {
      /*
      if (! actor.startChild(jid))
        return null;
        */

      ref = _actorStreamMap.get(jid);

      if (ref != null)
        return ref.get();
    }

    return null;
  }

  private Mailbox putActorStream(String jid, Mailbox actorStream)
  {
    if (actorStream == null)
      return null;

    synchronized (_actorStreamMap) {
      WeakReference<Mailbox> ref = _actorStreamMap.get(jid);

      if (ref != null)
        return ref.get();

      _actorStreamMap.put(jid, new WeakReference<Mailbox>(actorStream));

      return actorStream;
    }
  }

  protected Actor findParentActor(String jid)
  {
    return null;
    /*
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
      */
  }

  protected Mailbox findDomain(String domain)
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

  protected boolean startActorFromManager(String jid)
  {
    /*
    for (BrokerListener manager : _actorManagerList) {
      if (manager.startActor(jid))
        return true;
    }
        */

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

      /*
      if (actor != null) {
        try {
          actor.onChildStop(jid);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
      */
    }

    _actorCache.remove(jid);

    synchronized (_actorStreamMap) {
      _actorStreamMap.remove(jid);
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

  private void startActor(Bean bean,
                          String name,
                          int threadMax)
  {
    InjectManager beanManager = InjectManager.getCurrent();

    Actor actor = (Actor) beanManager.getReference(bean);

    actor.setBroker(this);

    String jid = name;

    if (jid == null || "".equals(jid))
      jid = bean.getName();

    if (jid == null || "".equals(jid))
      jid = bean.getBeanClass().getSimpleName();

    if (jid.indexOf('@') < 0)
      jid = jid + '@' + getJid();
    else if (jid.endsWith("@"))
      jid = jid.substring(0, jid.length() - 1);

    actor.setJid(jid);

    Actor bamActor = actor;

    Mailbox mailbox;
    
    // queue
    if (threadMax > 0) {
      ActorStream actorStream = bamActor.getActorStream();
      mailbox = new MultiworkerMailbox(jid, actorStream, this, threadMax);
      // bamActor.setActorStream(actorStream);
    }
    else {
      mailbox = new PassthroughMailbox(jid, bamActor.getActorStream(), this);
    }

    addMailbox(mailbox);

    Environment.addCloseListener(new ActorClose(mailbox));
  }

  private void startActor(Bean bean, AdminService bamService)
  {
    InjectManager beanManager = InjectManager.getCurrent();

    Actor actor = (Actor) beanManager.getReference(bean);

    actor.setBroker(this);

    String jid = bamService.name();

    if (jid == null || "".equals(jid))
      jid = bean.getName();

    if (jid == null || "".equals(jid))
      jid = bean.getBeanClass().getSimpleName();

    actor.setJid(jid);

    int threadMax = bamService.threadMax();

    Actor bamActor = actor;
    Mailbox mailbox = null;
    // queue
    if (threadMax > 0) {
      ActorStream actorStream = bamActor.getActorStream();
      mailbox = new MultiworkerMailbox(jid, actorStream, this, threadMax);
      bamActor.setMailbox(mailbox);
    }

    addMailbox(mailbox);

    Environment.addCloseListener(new ActorClose(mailbox));
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
    com.caucho.remote.BamService bamAnn = findActor(annList);

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
}
