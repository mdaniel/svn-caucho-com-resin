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

import com.caucho.hmpp.spi.HmppResource;
import com.caucho.hmpp.*;

import com.caucho.util.*;
import java.io.Serializable;
import java.util.logging.*;

/**
 * Manager
 */
public class HempConnectionImpl implements HmppConnection {
  private static final Logger log
    = Logger.getLogger(HempConnectionImpl.class.getName());
  
  private static final L10N L = new L10N(HempConnectionImpl.class);
  
  private final HempManager _manager;
  private final HempConnectionOutboundStream _handler;
  
  private final String _jid;
  
  private HmppStream _inboundFilter;
  private HmppStream _outboundFilter;
  
  private HmppStream _inboundStream;
  private HmppStream _outboundStream;
  
  private HmppResource _resource;

  private boolean _isClosed;

  HempConnectionImpl(HempManager manager, String jid)
  {
    _manager = manager;
    _jid = jid;

    _handler = new HempConnectionOutboundStream(this);

    _inboundStream = manager;
    _outboundStream = _handler;
    
    String uid = jid;
    int p = uid.indexOf('/');
    if (p > 0)
      uid = uid.substring(0, p);

    _resource = manager.getResource(uid);

    if (_resource != null) {
      _inboundFilter = _resource.getInboundFilter(_manager);
      _inboundStream = _inboundFilter;
      
      _outboundFilter = _resource.getOutboundFilter(null);
    }
  }

  /**
   * Returns the session's jid
   */
  public String getJid()
  {
    return _jid;
  }

  HempConnectionOutboundStream getStreamHandler()
  {
    return _handler;
  }
  
  public HmppStream getStream()
  {
    return _inboundStream;
  }

  /**
   * Registers the listener
   */
  public void setMessageHandler(MessageStream handler)
  {
    _handler.setMessageHandler(handler);
  }

  /**
   * Registers the listener
   */
  public void setQueryHandler(QueryStream handler)
  {
    _handler.setQueryHandler(handler);
  }

  /**
   * Sets the presence listener
   */
  public void setPresenceHandler(PresenceStream handler)
  {
    _handler.setPresenceHandler(handler);
  }

  /**
   * Sends a message
   */
  public void sendMessage(String to, Serializable msg)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));

    _inboundStream.sendMessage(to, _jid, msg);
  }

  //
  // Query/RPC handling
  //

  /**
   * Queries the service
   */
  public Serializable queryGet(String to, Serializable query)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    //return _manager.query(_jid, to, query);
    return null;
  }

 /**
   * Queries the service
   */
  public Serializable querySet(String to, Serializable query)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    //return _manager.query(_jid, to, query);
    return null;
  }

  //
  // presence handling
  //

  /**
   * Basic presence
   */
  public void presence(Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresence(null, _jid, data);
  }

  /**
   * directed presence
   */
  public void presence(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));

    _inboundStream.sendPresence(to, _jid, data);
  }

  /**
   * Basic presence
   */
  public void presenceUnavailable(Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));

    _inboundStream.sendPresenceUnavailable(null, _jid, data);
 }
  
  /**
   * directed presence
   */
  public void presenceUnavailable(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresenceUnavailable(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceProbe(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresenceProbe(to, _jid, data);
  }


  /**
   * directed presence
   */
  public void presenceSubscribe(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));

    _inboundStream.sendPresenceSubscribe(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceSubscribed(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresenceSubscribed(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceUnsubscribe(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresenceUnsubscribe(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceUnsubscribed(String to, Serializable []data)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresenceUnsubscribed(to, _jid, data);
  }

  /**
   * directed presence
   */
  public void presenceError(String to,
			    Serializable []data,
			    HmppError error)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    _inboundStream.sendPresenceError(to, _jid, data, error);
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
