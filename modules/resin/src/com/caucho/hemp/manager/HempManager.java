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

package com.caucho.hemp.manager;

import com.caucho.hmpp.HmppSession;
import com.caucho.hmpp.HmppBroker;
import com.caucho.hmpp.HmppError;
import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.hmpp.HmppResource;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;


/**
 * Manager
 */
public class HempManager implements HmppBroker {
  private static final Logger log
    = Logger.getLogger(HempManager.class.getName());
  private static final L10N L = new L10N(HempManager.class);
  
  private final HashMap<String,WeakReference<HmppResource>> _resourceMap
    = new HashMap<String,WeakReference<HmppResource>>();
  
  private String _serverId = Resin.getCurrent().getServerId();

  private String _domain = "localhost";
  private String _managerJid = "localhost";

  /**
   * Creates a session
   */
  public HmppSession createSession(String uid, String password)
  {
    String jid = generateJid(uid);

    HempSession session = new HempSession(this, jid);

    synchronized (_resourceMap) {
      _resourceMap.put(jid, new WeakReference<HmppResource>(session));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(session + " created");

    return session;
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
   * Registers a resource
   */
  public HmppSession registerResource(String name)
  {
    String jid;
    
    int p;
    if ((p = name.indexOf('/')) > 0) {
      jid = name;
    }
    else if ((p = name.indexOf('@')) > 0) {
      jid = name;
    }
    else {
      jid = name + "@" + getDomain();
    }

    HempSession session = new HempSession(this, jid);

    synchronized (_resourceMap) {
      WeakReference<HmppResource> oldRef = _resourceMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));
      
      _resourceMap.put(jid, new WeakReference<HmppResource>(session));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(session + " created");

    return session;
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
  protected void presence(String fromJid, Serializable []data)
  {
    /*
    ArrayList<RosterItem> roster = getSubscriptions(fromJid);
    
    for (RosterItem item : roster) {
      String targetJid = item.getTarget();

      HempEntity entity = getEntity(targetJid);

      if (entity == null)
	continue;

      if (item.isSubscribedTo())
	entity.onPresenceProbe(fromJid, targetJid, data);

      if (item.isSubscriptionFrom())
	entity.onPresence(fromJid, targetJid, data);
    }
    */
  }

  /**
   * Presence to
   */
  protected void presence(String to,
			  String from,
			  Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresence(to, from, data);
  }

  /**
   * Presence probe
   */
  protected void presenceProbe(String to,
			       String from,
			       Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceProbe(to, from, data);
  }

  /**
   * Presence unavailable
   */
  protected void presenceUnavailable(String to,
				     String from,
				     Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceUnavailable(to, from, data);
  }

  /**
   * Presence subscribe
   */
  protected void presenceSubscribe(String to,
				   String from,
				   Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceSubscribe(to, from, data);
  }

  /**
   * Presence subscribed
   */
  protected void presenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceSubscribed(to, from, data);
  }

  /**
   * Presence unsubscribe
   */
  protected void presenceUnsubscribe(String to,
				     String from,
				     Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceUnsubscribe(to, from, data);
  }

  /**
   * Presence unsubscribed
   */
  protected void presenceUnsubscribed(String to,
				      String from,
				      Serializable []data)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceUnsubscribed(to, from, data);
  }

  /**
   * Presence error
   */
  protected void presenceError(String to,
			       String from,
			       Serializable []data,
			       HmppError error)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onPresenceError(to, from, data, error);
  }

  /**
   * Presence unavailable
   */
  protected void presenceUnavailable(String fromJid, Serializable []data)
  {
    /*
    ArrayList<RosterItem> roster = getSubscriptions(fromJid);
    
    for (RosterItem item : roster) {
      String targetJid = item.getTarget();

      HempEntity entity = getEntity(targetJid);

      if (entity == null)
	continue;

      if (item.isSubscribedTo() || item.isSubscriptionFrom())
	entity.onPresenceUnavailable(fromJid, targetJid, data);
    }
    */
  }

  /**
   * Returns the roster for a user
   */
  /*
  protected ArrayList<RosterItem> getSubscriptions(String fromJid)
  {
    Roster roster = getRoster(fromJid);

    if (roster != null)
      return roster.getSubscriptions();
    else
      return new ArrayList<RosterItem>();
  }
  */

  /**
   * Returns the RosterManager
   */
  /*
  protected RosterManager getRosterManager()
  {
    return _rosterManager;
  }
  */

  /**
   * Sets the RosterManager
   */
  /*
  protected void setRosterManager(RosterManager manager)
  {
    _rosterManager = manager;
  }
  */

  /**
   * Returns the roster
   */
  /*
  protected Roster getRoster(String fromJid)
  {
    return getRosterManager().getRoster(fromJid);
  }
  */

  /**
   * Sends a message
   */
  void sendMessage(String to, String from, Serializable value)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onMessage(to, from, value);
    else
      throw new RuntimeException(L.l("'{0}' is an unknown resource", to));
  }

  /**
   * Query an entity
   */
  void queryGet(long id, String to, String from, Serializable query)
  {
    HmppResource resource = getResource(to);

    if (resource != null) {
      // XXX: error
      resource.onQueryGet(id, to, from, query);
      return;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet to unknown resource to='" + to
	       + "' from=" + from);
    }

    String msg = L.l("'{0}' is an unknown service for queryGet", to);
    
    HmppError error = new HmppError(HmppError.TYPE_CANCEL,
				    HmppError.SERVICE_UNAVAILABLE,
				    msg);
				    
    queryError(id, from, to, query, error);
  }

  /**
   * Query an entity
   */
  void querySet(long id, String to, String from, Serializable query)
  {
    HmppResource resource = getResource(to);

    if (resource != null) {
      // XXX: error
      resource.onQuerySet(id, to, from, query);
      return;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " querySet to unknown resource '" + to
	       + "' from=" + from);
    }

    String msg = L.l("'{0}' is an unknown service for querySet", to);
    
    HmppError error = new HmppError(HmppError.TYPE_CANCEL,
				    HmppError.SERVICE_UNAVAILABLE,
				    msg);
				    
    queryError(id, from, to, query, error);
  }

  /**
   * Query an entity
   */
  void queryResult(long id, String to, String from, Serializable value)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onQueryResult(id, to, from, value);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  /**
   * Query an entity
   */
  void queryError(long id,
		  String to,
		  String from,
		  Serializable query,
		  HmppError error)
  {
    HmppResource resource = getResource(to);

    if (resource != null)
      resource.onQueryError(id, to, from, query, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", to));
  }

  protected HmppResource getResource(String jid)
  {
    synchronized (_resourceMap) {
      WeakReference<HmppResource> ref = _resourceMap.get(jid);

      if (ref != null)
	return ref.get();
    }

    HmppResource resource = lookupResource(jid);

    if (resource != null) {
      synchronized (_resourceMap) {
	WeakReference<HmppResource> ref = _resourceMap.get(jid);

	if (ref != null)
	  return ref.get();

	_resourceMap.put(jid, new WeakReference<HmppResource>(resource));

	return resource;
      }
    }
    else
      return null;
  }

  protected HmppResource lookupResource(String jid)
  {
    return null;
  }
  
  /**
   * Closes a connection
   */
  void close(String jid)
  {
    synchronized (_resourceMap) {
      _resourceMap.remove(jid);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
