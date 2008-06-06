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
import com.caucho.hemp.*;
import com.caucho.bam.BamAgentStream;
import com.caucho.bam.BamService;
import com.caucho.bam.BamStream;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;


/**
 * Broker
 */
public class HempBroker implements BamBroker, BamStream
{
  private static final Logger log
    = Logger.getLogger(HempBroker.class.getName());
  private static final L10N L = new L10N(HempBroker.class);
  
  // agents
  private final HashMap<String,WeakReference<BamAgentStream>> _agentMap
    = new HashMap<String,WeakReference<BamAgentStream>>();
  
  private final HashMap<String,BamService> _serviceMap
    = new HashMap<String,BamService>();
  
  private final HashMap<String,WeakReference<BamService>> _serviceCache
    = new HashMap<String,WeakReference<BamService>>();
  
  private String _serverId = Resin.getCurrent().getServerId();

  private String _domain = "localhost";
  private String _managerJid = "localhost";

  private BamServiceManager []_serviceManagerList = new BamServiceManager[0];
  
  /**
   * Returns the stream to the broker
   */
  public BamStream getBrokerStream()
  {
    return this;
  }

  //
  // configuration
  //
  
  /**
   * Adds a broker implementation, e.g. the IM broker.
   */
  public void addServiceManager(BamServiceManager serviceManager)
  {
    BamServiceManager []serviceManagerList = new BamServiceManager[_serviceManagerList.length + 1];
    System.arraycopy(_serviceManagerList, 0, serviceManagerList, 0, _serviceManagerList.length);
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
    return getConnection(uid, password, null);
  }

  /**
   * Creates a session
   */
  public BamConnection getConnection(String uid,
				     String password,
				     String resourceId)
  {
    String jid = generateJid(uid, resourceId);

    HempConnectionImpl conn = new HempConnectionImpl(this, jid);

    BamAgentStream agentStream = conn.getAgentStreamHandler();
      
    synchronized (_agentMap) {
      _agentMap.put(jid, new WeakReference<BamAgentStream>(agentStream));
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

  protected String generateJid(String uid, String resource)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(uid);
    sb.append("/");

    if (resource != null)
      sb.append(resource);
    else {
      sb.append(_serverId);
      sb.append(":");

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
      WeakReference<BamAgentStream> oldRef = _agentMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));

      BamAgentStream agentStream = service.getAgentStream();
      _agentMap.put(jid, new WeakReference<BamAgentStream>(agentStream));
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
    
    synchronized (_serviceCache) {
      _serviceCache.remove(jid);
    }
    
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
   * Presence
   */
  public void sendPresence(String to, String from, Serializable value)
  {
    /*
    if (to == null) {
      BamServiceManager []resourceManagers = _serviceManagerList;

      for (BamServiceManager manager : resourceManagers) {
        manager.sendPresence(to, from, data);
      }
    }
    else {
    */
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresence(to, from, value);
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
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable data)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceUnavailable(to, from, data);
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
  public void sendPresenceProbe(String to,
			        String from,
			        Serializable data)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceProbe(to, from, data);
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
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceSubscribe(to, from, data);
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
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceSubscribed(to, from, data);
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
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceUnsubscribe(to, from, data);
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
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceUnsubscribed(to, from, data);
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
  public void sendPresenceError(String to,
			        String from,
			        Serializable data,
			        BamError error)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceError(to, from, data, error);
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
  public void sendMessage(String to, String from, Serializable value)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendMessage(to, from, value);
    else {
      log.fine(this + " sendMessage to=" + to + " from=" + from
	       + " is an unknown stream");
    }
  }

  /**
   * Sends a message
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendMessageError(to, from, value, error);
    else {
      log.fine(this + " sendMessageError to=" + to + " from=" + from
	       + " error=" + error + " is an unknown stream");
    }
  }

  /**
   * Query an entity
   */
  public boolean sendQueryGet(long id, String to, String from,
			      Serializable query)
  {
    BamStream stream = findAgent(to);

    if (stream != null) {
      if (! stream.sendQueryGet(id, to, from, query)) {
	if (log.isLoggable(Level.FINE)) {
	  log.fine(this + " queryGet to unknown feature to='" + to
		   + "' from=" + from + " query='" + query + "'"
		   + " stream=" + stream);
	}
	
	String msg = L.l("'{0}' is an unknown feature for to='{1}'",
			 query, to);
    
	BamError error = new BamError(BamError.TYPE_CANCEL,
					BamError.FEATURE_NOT_IMPLEMENTED,
					msg);
	
	sendQueryError(id, from, to, query, error);
      }

      return true;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet to unknown stream to='" + to
	       + "' from=" + from);
    }

    String msg = L.l("'{0}' is an unknown service for queryGet", to);
    
    BamError error = new BamError(BamError.TYPE_CANCEL,
				    BamError.SERVICE_UNAVAILABLE,
				    msg);
				    
    sendQueryError(id, from, to, query, error);
    
    return true;
  }

  /**
   * Query an entity
   */
  public boolean sendQuerySet(long id, String to, String from, Serializable query)
  {
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
				    
      sendQueryError(id, from, to, query, error);

      return true;
    }

    if (stream.sendQuerySet(id, to, from, query))
      return true;

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " querySet with unknown feature to=" + to
	       + " from=" + from + " resource=" + stream
	       + " query=" + query);
    }

    String msg = L.l("'{0}' is an unknown feature for querySet",
		     query);
    
    BamError error = new BamError(BamError.TYPE_CANCEL,
				    BamError.FEATURE_NOT_IMPLEMENTED,
				    msg);
				    
    sendQueryError(id, from, to, query, error);

    return true;
  }

  /**
   * Query an entity
   */
  public void sendQueryResult(long id, String to, String from, Serializable value)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendQueryResult(id, to, from, value);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  /**
   * Query an entity
   */
  public void sendQueryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 BamError error)
  {
    BamStream stream = findAgent(to);

    if (stream != null)
      stream.sendQueryError(id, to, from, query, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  protected BamAgentStream findAgent(String jid)
  {
    synchronized (_agentMap) {
      WeakReference<BamAgentStream> ref = _agentMap.get(jid);

      if (ref != null)
	return ref.get();
    }

    BamAgentStream agentStream;
    BamService service = findService(jid);
    
    if (service == null)
      return null;
    else if (jid.equals(service.getJid()))
      agentStream = service.getAgentStream();
    else
      agentStream = service.findAgent(jid);

    if (agentStream != null) {
      synchronized (_agentMap) {
	WeakReference<BamAgentStream> ref = _agentMap.get(jid);

	if (ref != null)
	  return ref.get();

	_agentMap.put(jid, new WeakReference<BamAgentStream>(agentStream));

	return agentStream;
      }
    }
    else
      return null;
  }

  protected BamService findService(String jid)
  {
    if (jid == null)
      return null;
    
    synchronized (_serviceCache) {
      WeakReference<BamService> ref = _serviceCache.get(jid);

      if (ref != null)
	return ref.get();
    }

    BamService service = findServiceFromManager(jid);
    
    if (service != null) {
      synchronized (_serviceCache) {
	WeakReference<BamService> ref = _serviceCache.get(jid);

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

  protected BamService findServiceFromManager(String jid)
  {
    for (BamServiceManager manager : _serviceManagerList) {
      BamService service = manager.findService(jid);

      if (service != null)
	return service;
    }

    return null;
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
    
    synchronized (_serviceCache) {
      _serviceCache.remove(jid);
    }
    
    synchronized (_agentMap) {
      _agentMap.remove(jid);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
