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

import java.util.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;

import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.server.resin.*;
import com.caucho.util.*;

/**
 * Manager
 */
public class HempManager implements HmppManager {
  private static final Logger log
    = Logger.getLogger(HempManager.class.getName());
  private static final L10N L = new L10N(HempManager.class);

  private RosterManager _rosterManager = new RosterManager();
  
  private final HashMap<String,WeakReference<HempSession>> _sessionMap
    = new HashMap<String,WeakReference<HempSession>>();
  
  private final HashMap<String,HempEntity> _entityMap
    = new HashMap<String,HempEntity>();
  
  private String _serverId = Resin.getCurrent().getServerId();

  private String _domain = "localhost";
  private String _managerJid = "localhost";

  /**
   * Creates a session
   */
  public HmppSession createSession(String uid, String password)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(uid);
    sb.append("/");
    sb.append(_serverId);
    sb.append(":");

    Base64.encode(sb, RandomUtil.getRandomLong());
    
    String jid = sb.toString();

    HempEntity entity;
      
    synchronized (_entityMap) {
      entity = _entityMap.get(uid);
      
      if (entity == null) {
	entity = new HempEntity(this, uid);
	_entityMap.put(uid, entity);
      }
    }

    HempSession session = new HempSession(this, entity, jid);

    synchronized (_sessionMap) {
      _sessionMap.put(jid, new WeakReference<HempSession>(session));
    }

    if (log.isLoggable(Level.FINE))
      log.fine(session + " created");

    return session;
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

    synchronized (_sessionMap) {
      WeakReference<HempSession> oldRef = _sessionMap.get(jid);

      if (oldRef != null && oldRef.get() != null)
	throw new IllegalStateException(L.l("duplicated jid='{0}' is not allowed",
					    jid));
      
      oldRef = _sessionMap.put(jid, new WeakReference<HempSession>(session));
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
  }

  /**
   * Presence to
   */
  protected void presence(String fromJid,
			  String toJid,
			  Serializable []data)
  {
    HempEntity entity = getEntity(toJid);

    if (entity == null)
      return;

    entity.onPresence(fromJid, toJid, data);
  }

  /**
   * Presence
   */
  protected void presenceUnavailable(String fromJid, Serializable []data)
  {
    ArrayList<RosterItem> roster = getSubscriptions(fromJid);
    
    for (RosterItem item : roster) {
      String targetJid = item.getTarget();

      HempEntity entity = getEntity(targetJid);

      if (entity == null)
	continue;

      if (item.isSubscribedTo() || item.isSubscriptionFrom())
	entity.onPresenceUnavailable(fromJid, targetJid, data);
    }
  }

  /**
   * Presence to
   */
  protected void presenceUnavailable(String fromJid,
				     String toJid,
				     Serializable []data)
  {
    HempEntity entity = getEntity(toJid);

    if (entity == null)
      return;

    entity.onPresenceUnavailable(fromJid, toJid, data);
  }

  /**
   * Returns the roster for a user
   */
  protected ArrayList<RosterItem> getSubscriptions(String fromJid)
  {
    Roster roster = getRoster(fromJid);

    if (roster != null)
      return roster.getSubscriptions();
    else
      return new ArrayList<RosterItem>();
  }

  /**
   * Returns the RosterManager
   */
  protected RosterManager getRosterManager()
  {
    return _rosterManager;
  }

  /**
   * Sets the RosterManager
   */
  protected void setRosterManager(RosterManager manager)
  {
    _rosterManager = manager;
  }

  /**
   * Returns the roster
   */
  protected Roster getRoster(String fromJid)
  {
    return getRosterManager().getRoster(fromJid);
  }

  /**
   * Sends a message
   */
  void sendMessage(String fromJid, String toJid, Serializable value)
  {
    HempEntity entity = getEntity(toJid);

    if (entity != null)
      entity.onMessage(fromJid, toJid, value);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", toJid));
  }

  protected HempEntity getEntity(String jid)
  {
    synchronized (_entityMap) {
      return _entityMap.get(jid);
    }
  }

  protected HempSession getSession(String jid)
  {
    synchronized (_sessionMap) {
      WeakReference<HempSession> ref = _sessionMap.get(jid);

      if (ref != null)
	return ref.get();
      else
	return null;
    }
  }

  /**
   * Query an entity
   */
  Serializable query(String fromJid, String toJid, Serializable value)
  {
    HempEntity entity;
    
    synchronized (_entityMap) {
      entity = _entityMap.get(toJid);
    }

    if (entity != null)
      return entity.onQuery(fromJid, toJid, value);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", toJid));
  }

  /**
   * Query an entity
   */
  void queryGet(String id, String fromJid, String toJid, Serializable query)
  {
    HempSession session = getSession(toJid);

    if (session != null) {
      session.onQueryGet(id, fromJid, toJid, query);
      return;
    }
    
    HempEntity entity = getEntity(toJid);

    if (entity != null) {
      entity.onQueryGet(id, fromJid, toJid, query);
      return;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet to unknown resource '" + toJid
	       + "' from=" + fromJid);
    }

    String msg = L.l("'" + toJid + "' is an unknown service");
    
    HmppError error = new HmppError(HmppError.TYPE_CANCEL,
				    HmppError.SERVICE_UNAVAILABLE,
				    msg);
				    
    queryError(id, getManagerJid(), fromJid, query, error);
  }

  /**
   * Query an entity
   */
  void querySet(String id, String fromJid, String toJid, Serializable query)
  {
    HempSession session = getSession(toJid);

    if (session != null) {
      session.onQuerySet(id, fromJid, toJid, query);
      return;
    }
    
    HempEntity entity = getEntity(toJid);

    if (entity != null) {
      entity.onQuerySet(id, fromJid, toJid, query);
      return;
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " querySet to unknown resource '" + toJid
	       + "' from=" + fromJid);
    }

    String msg = L.l("'" + toJid + "' is an unknown service");
    
    HmppError error = new HmppError(HmppError.TYPE_CANCEL,
				    HmppError.SERVICE_UNAVAILABLE,
				    msg);
				    
    queryError(id, getManagerJid(), fromJid, query, error);
  }

  /**
   * Query an entity
   */
  void queryResult(String id, String fromJid, String toJid, Serializable value)
  {
    HempSession session = getSession(toJid);

    if (session != null)
      session.onQueryResult(id, fromJid, toJid, value);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", toJid));
  }

  /**
   * Query an entity
   */
  void queryError(String id,
		  String fromJid,
		  String toJid,
		  Serializable value,
		  HmppError error)
  {
    HempSession session = getSession(toJid);

    if (session != null)
      session.onQueryError(id, fromJid, toJid, value, error);
    else
      throw new RuntimeException(L.l("{0} is an unknown entity", toJid));
  }
  
  /**
   * Closes a connection
   */
  void close(String jid)
  {
    synchronized (_sessionMap) {
      _sessionMap.remove(jid);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
