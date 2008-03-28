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

import java.io.Serializable;

import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.util.*;

/**
 * Manager
 */
public class HempSession {
  private static final L10N L = new L10N(HempSession.class);
  
  private final HempManager _manager;
  private final HempEntity _entity;
  private final String _jid;

  private boolean _isClosed;

  private MessageListener _messageListener;
  private QueryListener _queryListener;

  HempSession(HempManager manager, HempEntity entity, String jid)
  {
    _manager = manager;
    _entity = entity;
    _jid = jid;

    _entity.addSession(this);
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
  public void send(String to, Serializable msg)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    _manager.send(_jid, to, msg);
  }

  /**
   * Queries the service
   */
  public Serializable query(String to, Serializable query)
  {
    HempManager manager = _manager;

    if (manager == null)
      throw new IllegalStateException(L.l("session is closed"));
    
    return _manager.query(_jid, to, query);
  }

  /**
   * Registers the listener
   */
  public void setMessageListener(MessageListener listener)
  {
    _messageListener = listener;
  }

  /**
   * Forwards the message
   */
  void onMessage(String fromJid, String toJid, Serializable value)
  {
    MessageListener listener = _messageListener;
    
    if (listener != null)
      listener.onMessage(fromJid, toJid, value);
  }

  /**
   * Registers the listener
   */
  public void setQueryListener(QueryListener listener)
  {
    _queryListener = listener;
  }

  /**
   * Forwards the message
   */
  Serializable onQuery(String fromJid, String toJid, Serializable query)
  {
    QueryListener listener = _queryListener;
    
    if (listener != null)
      return listener.onQuery(fromJid, toJid, query);
    else
      return null;
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
