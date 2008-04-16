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

import com.caucho.hemp.broker.HempConnectionOutboundStream;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hmpp.spi.HmppResource;
import com.caucho.hmpp.*;

import com.caucho.util.*;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * Manager
 */
public class HempConnectionImpl implements HmppConnection {
  private static final Logger log
    = Logger.getLogger(HempConnectionImpl.class.getName());
  
  private static final L10N L = new L10N(HempConnectionImpl.class);
  
  private final HempBroker _broker;
  private final HempConnectionOutboundStream _handler;
  
  private final String _jid;

  private HashMap<Long,QueryItem> _queryMap
    = new HashMap<Long,QueryItem>();

  private long _qId;
  
  private HmppStream _inboundFilter;
  private HmppStream _outboundFilter;
  
  private HmppStream _inboundStream;
  private HmppStream _outboundStream;
  
  private HmppResource _resource;

  private boolean _isClosed;

  HempConnectionImpl(HempBroker manager, String jid)
  {
    _broker = manager;
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
      _inboundFilter = _resource.getInboundFilter(_broker);
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
   * Sends a query-set packet to the server
   */
  public Serializable queryGet(String to,
			       Serializable query)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    WaitQueryCallback callback = new WaitQueryCallback();

    queryGet(to, query, callback, null);

    if (callback.waitFor())
      return callback.getResult();
    else
      throw new RuntimeException(String.valueOf(callback.getError()));
  }

  /**
   * Sends a query-get packet to the server
   */
  public void queryGet(String to,
		       Serializable value,
		       QueryCallback callback,
		       Object handback)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    long id;
      
    synchronized (this) {
      id = _qId++;

      _queryMap.put(id, new QueryItem(id, callback, handback));
    }

    getStream().sendQueryGet(id, to, _jid, value);
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable querySet(String to,
			       Serializable query)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    WaitQueryCallback callback = new WaitQueryCallback();

    querySet(to, query, callback, null);

    if (callback.waitFor())
      return callback.getResult();
    else
      throw new RuntimeException(String.valueOf(callback.getError()));
  }

  /**
   * Sends a query-set packet to the server
   */
  public void querySet(String to,
		       Serializable value,
		       QueryCallback callback,
		       Object handback)
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
    
    long id;
      
    synchronized (this) {
      id = _qId++;

      _queryMap.put(id, new QueryItem(id, callback, handback));
    }

    getStream().sendQuerySet(id, to, _jid, value);
  }

  /**
   * Callback for the response
   */
  boolean onQueryResult(long id, String to, String from, Serializable value)
  {
    QueryItem item = null;
    
    synchronized (this) {
      item = _queryMap.remove(id);
    }

    if (item != null) {
      item.onQueryResult(to, from, value);
      return true;
    }
    else
      return false;
  }

  /**
   * Callback for the response
   */
  boolean onQueryError(long id,
		       String to,
		       String from,
		       Serializable value,
		       HmppError error)
  {
    QueryItem item = null;
    
    synchronized (this) {
      item = _queryMap.remove(id);
    }

    if (item != null) {
      item.onQueryError(to, from, value, error);
      return true;
    }
    else
      return false;
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
    
    _broker.close(_jid);
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

  static class QueryItem {
    private final long _id;
    private final QueryCallback _callback;
    private final Object _handback;

    QueryItem(long id, QueryCallback callback, Object handback)
    {
      _id = id;
      _callback = callback;
      _handback = handback;
    }

    void onQueryResult(String to, String from, Serializable value)
    {
      if (_callback != null)
	_callback.onQueryResult(to, from, value, _handback);
    }

    void onQueryError(String to,
		      String from,
		      Serializable value,
		      HmppError error)
    {
      if (_callback != null)
	_callback.onQueryError(to, from, value, error, _handback);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _id + "," + _callback + "]";
    }
  }

  static class WaitQueryCallback implements QueryCallback {
    private Serializable _result;
    private HmppError _error;
    private boolean _isResult;

    public Serializable getResult()
    {
      return _result;
    }
    
    public HmppError getError()
    {
      return _error;
    }

    boolean waitFor()
    {
      try {
	synchronized (this) {
	  if (! _isResult)
	    this.wait(10000);
	}
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      }

      return _isResult;
    }
    
    public void onQueryResult(String fromJid, String toJid,
			      Serializable value, Object handback)
    {
      _result = value;

      synchronized (this) {
	_isResult = true;
	notifyAll();
      }
    }
  
    public void onQueryError(String fromJid, String toJid,
			     Serializable value, HmppError error,
			     Object handback)
    {
      _error = error;

      synchronized (this) {
	_isResult = true;
	notifyAll();
      }
    }
  }
}
