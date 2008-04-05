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
import com.caucho.hmpp.PresenceHandler;
import com.caucho.hmpp.MessageHandler;
import com.caucho.hmpp.QueryHandler;
import com.caucho.hmpp.HmppError;
import java.io.Serializable;

import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.util.*;

/**
 * Manager
 */
public class HempSession implements HmppSession {
  private static final L10N L = new L10N(HempSession.class);
  
  private final HempManager _manager;
  private final String _jid;
  
  private HempEntity _entity;

  private boolean _isClosed;

  private MessageHandler _messageListener;
  private QueryHandler _queryListener;
  private PresenceHandler _presenceHandler;

  HempSession(HempManager manager, HempEntity entity, String jid)
  {
    _manager = manager;
    _entity = entity;
    _jid = jid;

    _entity.addSession(this);
  }

  HempSession(HempManager manager, String jid)
  {
    _manager = manager;
    _jid = jid;
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
    
    _manager.sendMessage(_jid, to, msg);
  }

  /**
   * Registers the listener
   */
  public void setMessageListener(MessageHandler listener)
  {
    _messageListener = listener;
  }

  /**
   * Forwards the message
   */
  void onMessage(String fromJid, String toJid, Serializable value)
  {
    MessageHandler listener = _messageListener;
    
    if (listener != null)
      listener.onMessage(fromJid, toJid, value);
  }

  /**
   * Registers the listener
   */
  public void setQueryListener(QueryHandler listener)
  {
    _queryListener = listener;
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
  public void queryGet(String id, String to, Serializable query)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.queryGet(id, _jid, to, query);
  }

  /**
   * Queries the service
   */
  public void querySet(String id, String to, Serializable query)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.querySet(id, _jid, to, query);
  }

  /**
   * Returns a query result
   */
  public void queryResult(String id,
			  String from,
			  String to,
			  Serializable value)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));

    manager.queryResult(id, from, to, value);
  }

  /**
   * Returns a query error (low-level api)
   */
  public void queryError(String id,
			 String from,
			 String to,
			 Serializable query,
			 HmppError error)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));

    manager.queryError(id, from, to, query, error);
  }

  /**
   * Forwards the message
   */
  boolean onQueryGet(String id,
		     String fromJid,
		     String toJid,
		     Serializable query)
  {
    QueryHandler listener = _queryListener;
    
    if (listener != null && listener.onQueryGet(id, fromJid, toJid, query))
      return true;

    System.out.println("ON_Q: " + id);
    queryError(id, toJid, fromJid, query,
	       new HmppError("protocol-unknwon",
			     "unknown query: " + query.getClass().getName()));
    
    return true;
  }

  /**
   * Forwards the message
   */
  boolean onQuerySet(String id,
		     String fromJid,
		     String toJid,
		     Serializable query)
  {
    QueryHandler listener = _queryListener;
    
    if (listener != null)
      return listener.onQuerySet(id, fromJid, toJid, query);
    else
      return false;
  }

  /**
   * Result from the message
   */
  void onQueryResult(String id,
		     String fromJid,
		     String toJid,
		     Serializable value)
  {
    QueryHandler listener = _queryListener;

    if (listener != null)
      listener.onQueryResult(id, fromJid, toJid, value);
  }

  /**
   * Error from the message
   */
  void onQueryError(String id,
		    String fromJid,
		    String toJid,
		    Serializable query,
		    HmppError error)
  {
    QueryHandler listener = _queryListener;

    if (listener != null)
      listener.onQueryError(id, _jid, toJid, query, error);
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
  public void presenceTo(String toJid, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presence(_jid, toJid, data);
  }

  /**
   * Forwards the presence
   */
  protected void onPresence(String fromJid, String toJid, Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresence(fromJid, toJid, data);
  }

  /**
   * Forwards the presence
   */
  protected void onPresenceProbe(String fromJid,
				 String toJid,
				 Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceProbe(fromJid, toJid, data);
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
  public void presenceUnavailable(String toJid, Serializable []data)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.presence(_jid, toJid, data);
  }

  /**
   * Forwards the presence
   */
  protected void onPresenceUnavailable(String fromJid,
				       String toJid,
				       Serializable []data)
  {
    PresenceHandler handler = _presenceHandler;
    
    if (handler != null)
      handler.onPresenceUnavailable(fromJid, toJid, data);
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
    _entity.removeSession(this);
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
