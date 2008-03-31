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

package com.caucho.hemp.service;

import com.caucho.config.*;
import com.caucho.hemp.*;
import com.caucho.hemp.manager.*;
import com.caucho.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for a service
 */
public class GenericService
  implements MessageListener, QueryListener, PresenceHandler
{
  private static final L10N L = new L10N(GenericService.class);
  private static final Logger log
    = Logger.getLogger(GenericService.class.getName());
  
  private @In HmppManager _manager;
  
  private String _name;
  private String _password;

  private HmppSession _session;

  public void setName(String name)
  {
    _name = name;
  }

  public void setPassword(String password)
  {
    _password = password;
  }

  @PostConstruct
  private void init()
  {
    if (_name == null)
      throw new ConfigException(L.l("{0} requires a name",
				    getClass().getSimpleName()));

    _session = _manager.createSession(_name, _password);

    _session.setMessageListener(this);
    _session.setQueryListener(this);
    _session.setPresenceHandler(this);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " init");
  }

  /**
   * Handles an incoming message
   */
  public void onMessage(String fromJid, String toJid, Serializable value)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onMessage from=" + fromJid + " to=" + toJid);
  }

  /**
   * Handles an incoming query
   */
  public Serializable onQuery(String fromJid, String toJid, Serializable query)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onQuery from=" + fromJid + " to=" + toJid);
    
    return null;
  }

  /**
   * Handles an incoming query
   */
  public void onQueryGet(String id,
			 String fromJid,
			 String toJid,
			 Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onQueryGet id=" + id
		+ " from=" + fromJid + " to=" + toJid);
    }
  }

  /**
   * Handles an incoming query
   */
  public void onQuerySet(String id,
			 String fromJid,
			 String toJid,
			 Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onQuerySet id=" + id
		+ " from=" + fromJid + " to=" + toJid);
    }
  }

  /**
   * Handles an incoming presence notification
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param value - any additional payload for the presence notification
   */
  public void onPresence(String fromJid, String toJid, Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresence from=" + fromJid + " to=" + toJid);
  }

  /**
   * Handles an incoming presence notification
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param value - any additional payload for the presence notification
   */
  public void onPresenceUnavailable(String fromJid,
				    String toJid,
				    Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceUnavailable from=" + fromJid
		+ " to=" + toJid);
  }

  /**
   * Handles an incoming presence probe.
   *
   * Presence probes come from the server to a resource in response to
   * an original presence notification.
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param data - any additional payload for the presence notification
   */
  public void onPresenceProbe(String fromJid,
			      String toJid,
			      Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceProbe from=" + fromJid + " to=" + toJid);
  }

  /**
   * Handles an incoming presence subscription request
   *
   * Clients request a subscription to a roster based on a presence
   * subscription.
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param data - any additional payload for the presence notification
   */
  public void onPresenceSubscribe(String fromJid,
				  String toJid,
				  Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceSubscription from=" + fromJid
		+ " to=" + toJid);
  }

  /**
   * Handles an incoming presence subscriptioned acceptance
   *
   * Clients receive a subscription success when successfully subscribing
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param data - any additional payload for the presence notification
   */
  public void onPresenceSubscribed(String fromJid,
				   String toJid,
				   Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceSubscribed from=" + fromJid
		+ " to=" + toJid);
  }

  /**
   * Handles an incoming presence unsubscribe request
   *
   * Clients request a unsubscription to a roster based on a presence
   * unsubscription.
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param data - any additional payload for the presence notification
   */
  public void onPresenceUnsubscribe(String fromJid,
				    String toJid,
				    Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceUnsubscribe from=" + fromJid
		+ " to=" + toJid);
  }

  /**
   * Handles an incoming presence unsubscriptioned acceptance
   *
   * Clients receive a unsubscription success when successfully unsubscribing
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param value - any additional payload for the presence notification
   */
  public void onPresenceUnsubscribed(String fromJid,
				   String toJid,
				   Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceUnsubscribed from=" + fromJid
		+ " to=" + toJid);
  }

  /**
   * Handles a presence error notification
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param data - any additional payload for the presence notification
   * @param error - error information
   */
  public void onPresenceError(String fromJid,
			      String toJid,
			      Serializable []data,
			      HmppError error)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceError from=" + fromJid + " to=" + toJid);
  }

  @PreDestroy
  private void destroy()
  {
    _session.close();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " destroy");
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
