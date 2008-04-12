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
import com.caucho.hmpp.HmppResource;
import com.caucho.hmpp.PresenceHandler;
import com.caucho.hmpp.MessageHandler;
import com.caucho.hmpp.QueryHandler;
import com.caucho.hmpp.HmppError;

import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.util.*;
import java.io.Serializable;
import java.util.logging.*;

/**
 * Manager
 */
public class HempSession implements HmppSession, HmppResource {
  private static final Logger log
    = Logger.getLogger(HempSession.class.getName());
  
  private static final L10N L = new L10N(HempSession.class);
  
  private final HempManager _manager;
  
  private final String _jid;
  
  private HmppResource _resource;
  
  private boolean _isClosed;

  private MessageHandler _messageHandler;
  private QueryHandler _queryHandler;
  private PresenceHandler _presenceHandler;

  HempSession(HempManager manager, String jid)
  {
    _manager = manager;
    _jid = jid;

    String uid = jid;
    int p = uid.indexOf('/');
    if (p > 0)
      uid = uid.substring(0, p);

    _resource = manager.getResource(uid);
  }

  /**
   * Returns the session's jid
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Sends a message
   */
  public void sendMessage(String to, Serializable msg)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.sendMessage(to, _jid, msg);
  }

  /**
   * Registers the listener
   */
  public void setMessageHandler(MessageHandler handler)
  {
    _messageHandler = handler;
  }

  public void onLogin(String jid)
  {
  }

  public void onLogout(String jid)
  {
  }

  /**
   * Forwards the message
   */
  public void onMessage(String to, String from, Serializable value)
  {
    MessageHandler handler = _messageHandler;
    
    if (handler != null)
      handler.onMessage(to, from, value);
  }

  //
  // Query/RPC handling
  //

  /**
   * Registers the listener
   */
  public void setQueryHandler(QueryHandler handler)
  {
    _queryHandler = handler;
  }

  /**
   * Queries the service
   */
  public Serializable query(String to, Serializable query)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    //return _manager.query(_jid, to, query);
    return null;
  }

  /**
   * Queries the service
   */
  public void queryGet(long id, String to, Serializable query)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.queryGet(id, to, _jid, query);
  }

  /**
   * Queries the service
   */
  public void querySet(long id, String to, Serializable query)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.querySet(id, to, _jid, query);
  }

  /**
   * Returns a query result
   */
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));

    manager.queryResult(id, to, from, value);
  }

  /**
   * Returns a query error (low-level api)
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 HmppError error)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));

    manager.queryError(id, to, from, query, error);
  }

  /**
   * Forwards the message
   */
  public boolean onQueryGet(long id,
		            String to,
		            String from,
		            Serializable query)
  {
    QueryHandler handler = _queryHandler;
    
    if (handler != null && handler.onQueryGet(id, to, from, query))
      return true;

    queryError(id, from, to, query,
	       new HmppError(HmppError.TYPE_CANCEL,
			     HmppError.FEATURE_NOT_IMPLEMENTED,
			     "unknown query: " + query.getClass().getName()));
    
    return true;
  }

  /**
   * Forwards the message
   */
  public boolean onQuerySet(long id,
		            String to,
		            String from,
		            Serializable query)
  {
    QueryHandler handler = _queryHandler;
    
    if (handler != null && handler.onQuerySet(id, to, from, query))
      return true;

    queryError(id, from, to, query,
	       new HmppError(HmppError.TYPE_CANCEL,
			     HmppError.FEATURE_NOT_IMPLEMENTED,
			     "unknown query: " + query.getClass().getName()));

    return true;
  }

  /**
   * Result from the message
   */
  public void onQueryResult(long id,
		            String to,
		            String from,
		            Serializable value)
  {
    QueryHandler handler = _queryHandler;

    if (handler != null)
      handler.onQueryResult(id, to, from, value);
  }

  /**
   * Error from the message
   */
  public void onQueryError(long id,
		           String to,
		           String from,
		           Serializable query,
		           HmppError error)
  {
    QueryHandler handler = _queryHandler;

    if (handler != null)
      handler.onQueryError(id, to, _jid, query, error);
  }

  //
  // presence handling
  //

  /**
   * Sets the presence listener
   */
  public void setPresenceHandler(PresenceHandler handler)
  {
    _presenceHandler = handler;
  }

  /**
   * Basic presence
   */
  public void presenceUnavailable(Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnavailable(_jid, data);
  }

  /**
   * Basic presence
   */
  public void presence(Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presence(_jid, data);
  }

  /**
   * Basic presence
   */
  public void presenceTo(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presence(to, _jid, data);
  }

  //
  // directed presence
  //

  /**
   * directed presence
   */
  public void presence(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presence(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceProbe(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceProbe(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceUnavailable(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnavailable(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceSubscribe(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));

    if (_resource != null)
      _resource.onClientPresenceSubscribe(to, _jid, data);
    else {
      log.fine(this + " presenceSubscribe to=" + to + " with no self resource");
    }
  }

  /**
   * directed presence
   */
  public void presenceSubscribed(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceSubscribed(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceUnsubscribe(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnsubscribe(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceUnsubscribed(String to, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnsubscribed(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceError(String to,
			    Serializable []data,
			    HmppError error)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceError(to, _jid, data, error);
  }

  //
  // low-level
  //

  /**
   * low-level presence
   */
  public void presence(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presence(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceProbe(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceProbe(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceUnavailable(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnavailable(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceSubscribe(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceSubscribe(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceSubscribed(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceSubscribed(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceUnsubscribe(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnsubscribe(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceUnsubscribed(String to, String from, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceUnsubscribed(to, from, data);
  }

  /**
   * low-level presence
   */
  public void presenceError(String to, String from, Serializable []data,
			    HmppError error)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presenceError(to, from, data, error);
  }

  /**
   * Forwards the presence
   */
  public void onPresence(String to, String from, Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresence(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void onPresenceProbe(String to,
		              String from,
			      Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceProbe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void onPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceUnavailable(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void onPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceSubscribe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void onPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceSubscribed(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void onPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceUnsubscribed(to, from, data);
  }
  
  /**
   * Forwards the presence
   */
  public void onPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceUnsubscribe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void onPresenceError(String to,
			      String from,
			      Serializable []data,
                              HmppError error)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceError(to, from, data, error);
  }

  //
  // client
  //
  public void onClientPresenceSubscribe(String to,
					String from,
					Serializable []data)
  {
  }
  
  /**
   * Returns true if the session is closed
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Closes the session
   */
  public void close()
  {
    _isClosed = true;
    
    _manager.close(_jid);
  }

  @Override
  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    close();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jid + "]";
  }
}
