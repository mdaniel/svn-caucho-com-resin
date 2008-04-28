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

import com.caucho.hmtp.spi.HmtpServiceManager;
import com.caucho.hmtp.spi.HmtpBroker;
import com.caucho.hmtp.HmtpConnection;
import com.caucho.hmtp.HmtpError;
import com.caucho.hemp.*;
import com.caucho.hmtp.HmtpAgentStream;
import com.caucho.hmtp.spi.HmtpService;
import com.caucho.hmtp.HmtpStream;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;


/**
 * Broker
 */
public class HempBroker implements HmtpBroker, HmtpStream
{
  private static final Logger log
    = Logger.getLogger(HempBroker.class.getName());
  private static final L10N L = new L10N(HempBroker.class);
  
  // agents
  private final HashMap<String,WeakReference<HmtpAgentStream>> _agentMap
    = new HashMap<String,WeakReference<HmtpAgentStream>>();
  
  private final HashMap<String,HmtpService> _serviceMap
    = new HashMap<String,HmtpService>();
  
  private final HashMap<String,WeakReference<HmtpService>> _serviceCache
    = new HashMap<String,WeakReference<HmtpService>>();
  
  private String _serverId = Resin.getCurrent().getServerId();

  private String _domain = "localhost";
  private String _managerJid = "localhost";

  private HmtpServiceManager []_serviceManagerList = new HmtpServiceManager[0];
  
  /**
   * Returns the stream to the broker
   */
  public HmtpStream getBrokerStream()
  {
    return this;
  }

  //
  // configuration
  //
  
  /**
   * Adds a broker implementation, e.g. the IM broker.
   */
  public void addServiceManager(HmtpServiceManager serviceManager)
  {
    HmtpServiceManager []serviceManagerList = new HmtpServiceManager[_serviceManagerList.length + 1];
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
  public HmtpConnection getConnection(String uid, String password)
  {
    String jid = generateJid(uid);

    HempConnectionImpl conn = new HempConnectionImpl(this, jid);

    HmtpAgentStream agentStream = conn.getAgentStreamHandler();
      
    synchronized (_agentMap) {
      _agentMap.put(jid, new WeakReference<HmtpAgentStream>(agentStream));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(conn + " created");

    int p = jid.indexOf('/');
    if (p > 0) {
      String owner = jid.substring(0, p);
      
      HmtpService resource = findService(owner);

      if (resource != null)
	resource.onAgentStart(jid);
    }

    return conn;
  }

  protected String generateJid(String uid)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(uid);
    sb.append("/");
    sb.append(_serverId);
    sb.append(":");

    Base64.encode(sb, RandomUtil.getRandomLong());
    
    return sb.toString();
  }
  
  /**
   * Registers a service
   */
  public void addService(HmtpService service)
  {
    String jid = service.getJid();

    synchronized (_serviceMap) {
      HmtpService oldService = _serviceMap.get(jid);

      if (oldService != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));
      
      _serviceMap.put(jid, service);
      _serviceCache.put(jid, new WeakReference<HmtpService>(service));
    }
    
    synchronized (_agentMap) {
      WeakReference<HmtpAgentStream> oldRef = _agentMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));

      HmtpAgentStream agentStream = service.getAgentStream();
      _agentMap.put(jid, new WeakReference<HmtpAgentStream>(agentStream));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " addService jid=" + jid + " " + service);
 }
  
  /**
   * Removes a service
   */
  public void removeService(HmtpService service)
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
  public void sendPresence(String to, String from, Serializable []data)
  {
    /*
    if (to == null) {
      HmtpServiceManager []resourceManagers = _serviceManagerList;

      for (HmtpServiceManager manager : resourceManagers) {
        manager.sendPresence(to, from, data);
      }
    }
    else {
    */
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresence(to, from, data);
    else {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendPresence (no resource) to=" + to
		  + " from=" + from);
      }
    }
  }

  /**
   * Presence unavailable
   */
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable []data)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceUnavailable(to, from, data);
  }

  /**
   * Presence probe
   */
  public void sendPresenceProbe(String to,
			        String from,
			        Serializable []data)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceProbe(to, from, data);
  }

  /**
   * Presence subscribe
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceSubscribe(to, from, data);
  }

  /**
   * Presence subscribed
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceSubscribed(to, from, data);
  }

  /**
   * Presence unsubscribe
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceUnsubscribe(to, from, data);
  }

  /**
   * Presence unsubscribed
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceUnsubscribed(to, from, data);
  }

  /**
   * Presence error
   */
  public void sendPresenceError(String to,
			        String from,
			        Serializable []data,
			        HmtpError error)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendPresenceError(to, from, data, error);
  }

  /**
   * Sends a message
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    HmtpStream stream = findAgent(to);

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
			       HmtpError error)
  {
    HmtpStream stream = findAgent(to);

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
  public boolean sendQueryGet(long id, String to, String from, Serializable query)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null) {
      if (! stream.sendQueryGet(id, to, from, query)) {
	if (log.isLoggable(Level.FINE)) {
	  log.fine(this + " queryGet to unknown feature to='" + to
		   + "' from=" + from + " query='" + query + "'");
	}
	
	String msg = L.l("'{0}' is an unknown feature for to='{1}'",
			 query, to);
    
	HmtpError error = new HmtpError(HmtpError.TYPE_CANCEL,
					HmtpError.FEATURE_NOT_IMPLEMENTED,
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
    
    HmtpError error = new HmtpError(HmtpError.TYPE_CANCEL,
				    HmtpError.SERVICE_UNAVAILABLE,
				    msg);
				    
    sendQueryError(id, from, to, query, error);
    
    return true;
  }

  /**
   * Query an entity
   */
  public boolean sendQuerySet(long id, String to, String from, Serializable query)
  {
    HmtpStream stream = findAgent(to);

    if (stream == null) {
      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " querySet to unknown stream '" + to
		 + "' from=" + from);
      }

      String msg = L.l("'{0}' is an unknown service for querySet", to);
    
      HmtpError error = new HmtpError(HmtpError.TYPE_CANCEL,
				      HmtpError.SERVICE_UNAVAILABLE,
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
    
    HmtpError error = new HmtpError(HmtpError.TYPE_CANCEL,
				    HmtpError.FEATURE_NOT_IMPLEMENTED,
				    msg);
				    
    sendQueryError(id, from, to, query, error);

    return true;
  }

  /**
   * Query an entity
   */
  public void sendQueryResult(long id, String to, String from, Serializable value)
  {
    HmtpStream stream = findAgent(to);

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
			 HmtpError error)
  {
    HmtpStream stream = findAgent(to);

    if (stream != null)
      stream.sendQueryError(id, to, from, query, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  protected HmtpAgentStream findAgent(String jid)
  {
    synchronized (_agentMap) {
      WeakReference<HmtpAgentStream> ref = _agentMap.get(jid);

      if (ref != null)
	return ref.get();
    }

    HmtpAgentStream agentStream;
    HmtpService service = findService(jid);
    
    if (service == null)
      return null;
    else if (jid.equals(service.getJid()))
      agentStream = service.getAgentStream();
    else
      agentStream = service.findAgent(jid);

    if (agentStream != null) {
      synchronized (_agentMap) {
	WeakReference<HmtpAgentStream> ref = _agentMap.get(jid);

	if (ref != null)
	  return ref.get();

	_agentMap.put(jid, new WeakReference<HmtpAgentStream>(agentStream));

	return agentStream;
      }
    }
    else
      return null;
  }

  protected HmtpService findService(String jid)
  {
    if (jid == null)
      return null;
    
    synchronized (_serviceCache) {
      WeakReference<HmtpService> ref = _serviceCache.get(jid);

      if (ref != null)
	return ref.get();
    }

    HmtpService service = findServiceFromManager(jid);
    
    if (service != null) {
      synchronized (_serviceCache) {
	WeakReference<HmtpService> ref = _serviceCache.get(jid);

	if (ref != null)
	  return ref.get();

	_serviceCache.put(jid, new WeakReference<HmtpService>(service));

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

  protected HmtpService findServiceFromManager(String jid)
  {
    for (HmtpServiceManager manager : _serviceManagerList) {
      HmtpService service = manager.findService(jid);

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
      
      HmtpService service = findService(owner);

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
