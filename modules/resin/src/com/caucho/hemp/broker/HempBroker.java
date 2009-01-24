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

import com.caucho.bam.BamServiceManager;
import com.caucho.bam.BamBroker;
import com.caucho.bam.BamConnection;
import com.caucho.bam.BamError;
import com.caucho.bam.BamService;
import com.caucho.bam.AbstractBamService;
import com.caucho.bam.BamStream;
import com.caucho.bam.BamNotAuthorizedException;
import com.caucho.hemp.*;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.security.*;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import com.caucho.hemp.BamServiceBinding;
import com.caucho.webbeans.manager.BeanStartupEvent;
import com.caucho.webbeans.manager.WebBeansContainer;
import com.caucho.webbeans.component.CauchoBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.lang.ref.*;
import java.io.Serializable;
import javax.event.Observes;
import javax.inject.manager.Bean;
import javax.inject.manager.Manager;

/**
 * Broker
 */
public class HempBroker implements BamBroker, BamStream
{
  private static final Logger log
    = Logger.getLogger(HempBroker.class.getName());
  private static final L10N L = new L10N(HempBroker.class);

  private static final EnvironmentLocal<HempBroker> _localBroker
    = new EnvironmentLocal<HempBroker>();

  private HempBrokerManager _manager;
  private DomainManager _domainManager;
  
  // agents
  private final ConcurrentHashMap<String,WeakReference<BamStream>> _agentMap
    = new ConcurrentHashMap<String,WeakReference<BamStream>>();
  
  private final HashMap<String,BamService> _serviceMap
    = new HashMap<String,BamService>();
  
  private final Map<String,WeakReference<BamService>> _serviceCache
    = Collections.synchronizedMap(new HashMap<String,WeakReference<BamService>>());
  
  private String _serverId = Resin.getCurrent().getServerId();

  private String _domain = "localhost";
  private String _managerJid = "localhost";
  private HempDomainService _domainService;

  private boolean _isAdmin;
  private boolean _isAllowNullAdminAuthenticator;
  private Authenticator _auth;

  private ArrayList<String> _aliasList = new ArrayList<String>();

  private BamServiceManager []_serviceManagerList = new BamServiceManager[0];

  private volatile boolean _isClosed;

  public HempBroker()
  {
    _manager = HempBrokerManager.getCurrent();
    _domainManager = DomainManager.getCurrent();
    
    _domainService = new HempDomainService(this, "");

    if (_localBroker.getLevel() == null)
      _localBroker.set(this);

    Environment.addCloseListener(this);
  }

  public HempBroker(String domain)
  {
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
  public BamStream getBrokerStream()
  {
    return this;
  }
  
  /**
   * Returns the domain service
   */
  public BamService getDomainService()
  {
    return _domainService;
  }

  public void setAdmin(boolean isAdmin)
  {
    _isAdmin = isAdmin;
  }

  public void setAllowNullAdminAuthenticator(boolean isAllowNullAdmin)
  {
    _isAllowNullAdminAuthenticator = isAllowNullAdmin;
  }

  //
  // configuration
  //
  
  /**
   * Adds a broker implementation, e.g. the IM broker.
   */
  public void addServiceManager(BamServiceManager serviceManager)
  {
    BamServiceManager []serviceManagerList
      = new BamServiceManager[_serviceManagerList.length + 1];
    
    System.arraycopy(_serviceManagerList, 0, serviceManagerList, 0,
		     _serviceManagerList.length);
    serviceManagerList[serviceManagerList.length - 1] = serviceManager;
    _serviceManagerList = serviceManagerList;
  }

  //
  // API
  //

  /**
   * Creates a session
   */
  public BamConnection getConnection(String uid, String password)
  {
    return getConnection(null, uid, password, null, "127.0.0.1");
  }

  /**
   * Creates a session
   */
  public BamConnection getConnection(String uid,
				     String password,
				     String resourceId)
  {
    return getConnection(null, uid, password, resourceId, "127.0.0.1");
  }

  /**
   * Creates a session
   */
  public BamConnection getConnection(String uid,
				     String password,
				     String resourceId,
				     String ipAddress)
  {
    return getConnection(null, uid, password, resourceId, ipAddress);
  }

  /**
   * Creates a session
   */
  public BamConnection getConnection(BamStream agentStream,
				     String uid,
				     String password,
				     String resourceId,
				     String ipAddress)
  {
    String jid = login(uid, password, resourceId, ipAddress);

    HempConnectionImpl conn = new HempConnectionImpl(this, jid, agentStream);

    agentStream = conn.getAgentStream();
    
    synchronized (_agentMap) {
      _agentMap.put(jid, new WeakReference<BamStream>(agentStream));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(conn + " created");

    int p = jid.indexOf('/');
    if (p > 0) {
      String owner = jid.substring(0, p);
      
      BamService resource = findService(owner);

      if (resource != null)
	resource.onAgentStart(jid);
    }

    return conn;
  }

  protected String login(String uid,
			 String password,
			 String resource,
			 String ipAddress)
  {
    if (true)
      return generateJid(uid, resource);
    
    Authenticator auth = getAuthenticator();

    if (auth == null
	// && _isAllowNullAdminAuthenticator
	// && "127.0.0.1".equals(ipAddress)
	) {
      // server/2e2a
      // needed for watchdog (XXX: need watchdog testcase)
      
      return generateJid(uid, resource);
    }
    else if (auth == null)
      throw new BamNotAuthorizedException(L.l("remote access requires a configured authenticator, like <sec:AdminAuthenticator> for IP='{0}'",
				     ipAddress));
    else {
      authenticate(uid, password, ipAddress);
    }
    
    return generateJid(uid, resource);
  }

  protected void authenticate(String uid, String password, String ipAddress)
  {
    Authenticator auth = getAuthenticator();

    if (auth == null)
      throw new RuntimeException(L.l("can't login '{0}' for IP={1}",
				     uid, ipAddress));

    Principal user = auth.authenticate(new BasicPrincipal(uid),
				       new PasswordCredentials(password),
				       null);

    if (user == null) {
      throw new BamNotAuthorizedException(L.l("authentication failed '{0}' for IP={1}",
					      uid, ipAddress));
    }
  }

  protected Authenticator getAuthenticator()
  {
    if (_auth == null) {
      try {
	WebBeansContainer webBeans = WebBeansContainer.getCurrent();
      
	if (_isAdmin)
	  _auth = webBeans.getInstanceByType(AdminAuthenticator.class);
	else
	  _auth = webBeans.getInstanceByType(Authenticator.class);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);

	return null;
      }
    }
	
    return _auth;
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
      Base64.encode(sb, RandomUtil.getRandomLong());
    }
    
    return sb.toString();
  }
  
  /**
   * Registers a service
   */
  public void addService(BamService service)
  {
    String jid = service.getJid();

    synchronized (_serviceMap) {
      BamService oldService = _serviceMap.get(jid);

      if (oldService != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));
      
      _serviceMap.put(jid, service);
      _serviceCache.put(jid, new WeakReference<BamService>(service));
    }
    
    synchronized (_agentMap) {
      WeakReference<BamStream> oldRef = _agentMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));

      BamStream agentStream = service.getAgentStream();
      _agentMap.put(jid, new WeakReference<BamStream>(agentStream));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " addService jid=" + jid + " " + service);
 }
  
  /**
   * Removes a service
   */
  public void removeService(BamService service)
  {
    String jid = service.getJid();
    
    synchronized (_serviceMap) {
      _serviceMap.remove(jid);
    }
    
    _serviceCache.remove(jid);
    
    synchronized (_agentMap) {
      _agentMap.remove(jid);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " removeService jid=" + jid + " " + service);
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
  public void presence(String to, String from, Serializable value)
  {
    /*
    if (to == null) {
      BamServiceManager []resourceManagers = _serviceManagerList;

      for (BamServiceManager manager : resourceManagers) {
        manager.presence(to, from, data);
      }
    }
    else {
    */
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.presence(to, from, value);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresence (no resource) to=" + to
		  + " from=" + from + " value=" + value);
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
    BamStream stream = findAgent(to);

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
    BamStream stream = findAgent(to);

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
    BamStream stream = findAgent(to);

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
    BamStream stream = findAgent(to);

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
    BamStream stream = findAgent(to);

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
    BamStream stream = findAgent(to);

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
			        BamError error)
  {
    BamStream stream = findAgent(to);

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
    
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.message(to, from, value);
    else {
      log.fine(this + " sendMessage to=" + to + " from=" + from
	       + " is an unknown stream");
    }
  }

  /**
   * Sends a message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    Alarm.yieldIfTest();
    
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.messageError(to, from, value, error);
    else {
      log.fine(this + " sendMessageError to=" + to + " from=" + from
	       + " error=" + error + " is an unknown stream");
    }
  }

  /**
   * Query an entity
   */
  public boolean queryGet(long id, String to, String from,
			      Serializable query)
  {
    Alarm.yieldIfTest();
    
    BamStream stream = findAgent(to);

    if (stream != null) {
      try {
	if (! stream.queryGet(id, to, from, query)) {
	  if (log.isLoggable(Level.FINE)) {
	    log.fine(this + " queryGet to unknown feature to='" + to
		     + "' from=" + from + " query='" + query + "'"
		     + " stream=" + stream);
	  }

	  String msg = L.l("{0}: unknown queryGet feature {1} for jid={2} stream={3}",
			   this, query, stream.getJid(), stream);
    
	  BamError error = new BamError(BamError.TYPE_CANCEL,
					BamError.FEATURE_NOT_IMPLEMENTED,
					msg);
	
	  queryError(id, from, to, query, error);
	}

	return true;
      } catch (Exception e) {
	String msg = L.l("'{0}' threw an unexpected exception for '{1}'\n{2}",
			 to, query, e.toString());
	
	BamError error = new BamError(msg);
	
	queryError(id, from, to, query, error);

	return true;
      }
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet to unknown stream to='" + to
	       + "' from=" + from);
    }

    String msg = L.l("'{0}' is an unknown service for queryGet", to);
    
    BamError error = new BamError(BamError.TYPE_CANCEL,
				    BamError.SERVICE_UNAVAILABLE,
				    msg);
				    
    queryError(id, from, to, query, error);
    
    return true;
  }

  /**
   * Query an entity
   */
  public boolean querySet(long id, String to, String from, Serializable query)
  {
    Alarm.yieldIfTest();
    
    BamStream stream = findAgent(to);

    if (stream == null) {
      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " querySet to unknown stream '" + to
		 + "' from=" + from);
      }

      String msg = L.l("'{0}' is an unknown service for querySet", to);
    
      BamError error = new BamError(BamError.TYPE_CANCEL,
				      BamError.SERVICE_UNAVAILABLE,
				      msg);
				    
      queryError(id, from, to, query, error);

      return true;
    }

    if (stream.querySet(id, to, from, query))
      return true;

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " querySet with unknown feature to=" + to
	       + " from=" + from + " resource=" + stream
	       + " query=" + query);
    }

    String msg = L.l("{0}: unknown querySet feature {1} for jid={2} stream={3}",
		     this, query, stream.getJid(), stream);
    
    BamError error = new BamError(BamError.TYPE_CANCEL,
				  BamError.FEATURE_NOT_IMPLEMENTED,
				  msg);
				    
    queryError(id, from, to, query, error);

    return true;
  }

  /**
   * Query an entity
   */
  public void queryResult(long id, String to, String from, Serializable value)
  {
    Alarm.yieldIfTest();
    
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.queryResult(id, to, from, value);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  /**
   * Query an entity
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 BamError error)
  {
    Alarm.yieldIfTest();
    
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.queryError(id, to, from, query, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  protected BamStream findAgent(String jid)
  {
    WeakReference<BamStream> ref = _agentMap.get(jid);

    if (ref != null) {
      BamStream stream = ref.get();

      if (stream != null)
	return stream;
    }

    if (jid.endsWith("@")) {
      // jms/3d00
      jid = jid + getDomain();
    }

    BamStream agentStream;
    BamService service = findService(jid);

    if (service == null) {
      return putAgentStream(jid, findDomain(jid));
    }
    else if (jid.equals(service.getJid())) {
      agentStream = service.getAgentStream();

      if (agentStream != null) {
	return putAgentStream(jid, agentStream);
      }
    }
    else {
      if (! service.startAgent(jid))
        return null;

      ref = _agentMap.get(jid);

      if (ref != null)
	return ref.get();
    }

    return null;
  }

  private BamStream putAgentStream(String jid, BamStream agentStream)
  {
    if (agentStream == null)
      return null;
    
    synchronized (_agentMap) {
      WeakReference<BamStream> ref = _agentMap.get(jid);

      if (ref != null)
	return ref.get();

      _agentMap.put(jid, new WeakReference<BamStream>(agentStream));

      return agentStream;
    }
  }

  protected BamService findService(String jid)
  {
    if (jid == null)
      return null;

    WeakReference<BamService> ref = _serviceCache.get(jid);

    if (ref != null)
      return ref.get();

    if (startServiceFromManager(jid)) {
      ref = _serviceCache.get(jid);

      if (ref != null)
	return ref.get();
    }

    if (jid.indexOf('/') < 0 && jid.indexOf('@') < 0) {
      BamBroker broker = _manager.findBroker(jid);
      BamService service = null;

      if (broker instanceof HempBroker) {
	HempBroker hempBroker = (HempBroker) broker;

	service = hempBroker.getDomainService();
      }

      if (service != null) {
	ref = _serviceCache.get(jid);

	if (ref != null)
	  return ref.get();

	_serviceCache.put(jid, new WeakReference<BamService>(service));

	return service;
      }
    }
    
    int p;

    if ((p = jid.indexOf('/')) > 0) {
      String uid = jid.substring(0, p);

      return findService(uid);
    }
    else if ((p = jid.indexOf('@')) > 0) {
      String domainName = jid.substring(p + 1);
      
      return findService(domainName);
    }
    else
      return null;
  }

  protected BamStream findDomain(String domain)
  {
    if (domain == null)
      return null;

    if ("local".equals(domain))
      return getBrokerStream();

    BamBroker broker = _manager.findBroker(domain);

    if (broker instanceof HempBroker) {
      HempBroker hempBroker = (HempBroker) broker;

      BamService service = hempBroker.getDomainService();

      return service.getAgentStream();
    }
    
    if (broker == this)
      return null;

    BamStream stream = null;
    
    if (_domainManager != null)
      stream = _domainManager.findDomain(domain);

    return stream;
  }

  protected boolean startServiceFromManager(String jid)
  {
    for (BamServiceManager manager : _serviceManagerList) {
      if (manager.startService(jid))
        return true;
    }

    return false;
  }
  
  /**
   * Closes a connection
   */
  void closeAgent(String jid)
  {
    int p = jid.indexOf('/');
    if (p > 0) {
      String owner = jid.substring(0, p);
      
      BamService service = findService(owner);

      if (service != null) {
	try {
	  service.onAgentStop(jid);
	} catch (Exception e) {
	  log.log(Level.FINE, e.toString(), e);
	}
      }
    }
    
    _serviceCache.remove(jid);
    
    synchronized (_agentMap) {
      _agentMap.remove(jid);
    }
  }

  //
  // webbeans callbacks
  //

  /**
   * Called when a @BamService is annotated on the service
   */
  public void registerBamService(@Observes @BamServiceBinding
				 BeanStartupEvent event)
  {
    Manager manager = event.getManager();
    Bean bean = event.getBean();

    if (bean instanceof CauchoBean) {
      CauchoBean cauchoBean = (CauchoBean) bean;

      AbstractBamService service
	= (AbstractBamService) manager.getInstance(bean);

      Annotation []ann = cauchoBean.getAnnotations();

      service.setBrokerStream(this);

      String jid = getJid(service, ann);

      service.setJid(jid);

      int threadMax = getThreadMax(ann);

      BamService bamService = service;

      // queue
      if (threadMax > 0) {
	bamService = new MemoryQueueServiceFilter(bamService,
						  this,
						  threadMax);
      }

      addService(bamService);

      Environment.addCloseListener(new ServiceClose(bamService));
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

    _serviceMap.clear();
    _serviceCache.clear();
    _agentMap.clear();
  }

  private String getJid(BamService service, Annotation []annList)
  {
    com.caucho.config.BamService bamAnn = findBamService(annList);

    String name = "";

    if (bamAnn != null)
      name = bamAnn.name();
    
    if (name == null || "".equals(name))
      name = service.getJid();

    if (name == null || "".equals(name))
      name = service.getClass().getSimpleName();

    String jid = name;
    if (jid.indexOf('@') < 0 && jid.indexOf('/') < 0)
      jid = name + "@" + getJid();

    return jid;
  }

  private int getThreadMax(Annotation []annList)
  {
    com.caucho.config.BamService bamAnn = findBamService(annList);

    if (bamAnn != null)
      return bamAnn.threadMax();
    else
      return 1;
  }

  private com.caucho.config.BamService findBamService(Annotation []annList)
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

  class ServiceClose {
    private BamService _service;

    ServiceClose(BamService service)
    {
      _service = service;
    }
    
    public void close()
    {
      removeService(_service);
    }
  }
}
