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

import com.caucho.hmpp.HmppConnection;
import com.caucho.hmpp.HmppConnectionFactory;
import com.caucho.hmpp.HmppError;
import com.caucho.config.*;
import com.caucho.hemp.*;
import com.caucho.hemp.manager.*;
import com.caucho.hmpp.spi.AbstractHmppResource;
import com.caucho.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for a service
 */
public class GenericService extends AbstractHmppResource
{
  private static final L10N L = new L10N(GenericService.class);
  private static final Logger log
    = Logger.getLogger(GenericService.class.getName());
  
  private @In HmppConnectionFactory _manager;
  
  private String _name;
  private String _password;

  private HmppConnection _conn;

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

    _conn = _manager.registerResource(_name, this);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " init");
  }

  public HmppConnection getSession()
  {
    return _conn;
  }

  /**
   * Handles an incoming message
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onMessage to=" + to + " from=" + from
                + " value=" + value);
    }
  }

  /**
   * Handles an incoming query
   */
  public boolean sendQueryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onQueryGet id=" + id
		+ " to=" + to + " from=" + from
                + " query=" + query);
    }

    return false;
  }

  /**
   * Handles an incoming query
   */
  public boolean sendQuerySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onQuerySet id=" + id
		+ " to=" + to + " from=" + from
                + " query=" + query);
    }

    return false;
  }

  /**
   * Handles an incoming query
   */
  public void sendQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onQueryResult id=" + id
		+ " to=" + to + " from=" + from
                + " value=" + value);
    }
  }

  /**
   * Handles an incoming query
   */
  public void sendQueryError(long id,
			   String to,
			   String from,
			   Serializable value,
			   HmppError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onQueryError id=" + id
		+ " to=" + to + " from=" + from + " error=" + error);
    }
  }

  /**
   * Handles an incoming presence notification
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param value - any additional payload for the presence notification
   */
  public void sendPresence(String to, String from, Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresence to=" + to + " from=" + from);
  }

  /**
   * Handles an incoming presence notification
   *
   * @param fromJid - the jid of the client sending the notification
   * @param toJid - the jid of the resource managed by this service
   * @param value - any additional payload for the presence notification
   */
  public void sendPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceUnavailable to=" + to
		+ " from=" + from);
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
  public void sendPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceProbe to=" + to + " from=" + from);
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
  public void sendPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceSubscription to=" + to
		+ " from=" + from);
  }

  /**
   * Handles an incoming presence subscriptioned acceptance
   *
   * Clients receive a subscription success when successfully subscribing
   *
   * @param to - the jid of the resource managed by this service
   * @param from - the jid of the client sending the notification
   * @param data - any additional payload for the presence notification
   */
  public void sendPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceSubscribed to=" + to
		+ " from=" + from);
  }

  /**
   * Handles an incoming presence unsubscribe request
   *
   * Clients request a unsubscription to a roster based on a presence
   * unsubscription.
   *
   * @param to - the jid of the resource managed by this service
   * @param from - the jid of the client sending the notification
   * @param data - any additional payload for the presence notification
   */
  public void sendPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceUnsubscribe to=" + to
		+ " from=" + from);
  }

  /**
   * Handles an incoming presence unsubscriptioned acceptance
   *
   * Clients receive a unsubscription success when successfully unsubscribing
   *
   * @param to - the jid of the resource managed by this service
   * @param from - the jid of the client sending the notification
   * @param value - any additional payload for the presence notification
   */
  public void sendPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceUnsubscribed to=" + to
		+ " from=" + from);
  }

  /**
   * Handles a presence error notification
   *
   * @param to - the jid of the resource managed by this service
   * @param from - the jid of the client sending the notification
   * @param data - any additional payload for the presence notification
   * @param error - error information
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmppError error)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onPresenceError to=" + to + " from=" + from
                + " error=" + error);
  }

  @PreDestroy
  protected void destroy()
  {
    _conn.close();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " destroy");
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }

  public String getJid()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onLogin(String jid)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onLogout(String jid)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onClientPresenceSubscribe(String to, String from, Serializable[] data)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
