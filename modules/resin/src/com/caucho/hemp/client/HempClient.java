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

package com.caucho.hemp.client;

import com.caucho.hmpp.*;
import com.caucho.hmpp.packet.*;
import com.caucho.server.connection.*;
import com.caucho.server.port.*;
import com.caucho.hemp.*;
import com.caucho.hemp.service.*;
import com.caucho.hessian.io.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * HeMPP client protocol
 */
public class HempClient {
  private static final L10N L = new L10N(HempClient.class);
  private static final Logger log
    = Logger.getLogger(HempClient.class.getName());

  private InetAddress _address;
  private int _port;
  
  private String _to;

  private Socket _s;
  private InputStream _is;
  private OutputStream _os;

  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  private MessageHandler _messageHandler;
  private QueryHandler _queryHandler;
  private PresenceHandler _presenceHandler;

  private HashMap<Long,QueryItem> _queryMap
    = new HashMap<Long,QueryItem>();
    
  private long _qId;

  private boolean _isFinest;

  public HempClient(InetAddress address, int port)
  {
    _address = address;
    _port = port;

    _isFinest = log.isLoggable(Level.FINEST);
  }

  public HempClient(String address, int port)
  {
    this(getByName(address), port);
    
    _to = address;
  }

  private static InetAddress getByName(String address)
  {
    try {
      return InetAddress.getByName(address);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void connect()
    throws IOException
  {
    if (_s != null)
      throw new IllegalStateException(L.l("{0} is already connected", this));

    _s = new Socket(_address, _port);

    SocketStream ss = new SocketStream(_s);

    WriteStream os = new WriteStream(ss);
    ReadStream is = new ReadStream(ss);
    
    _os = os;
    _is = is;

    os.println("POST /hemp HTTP/1.1\r");
    os.println("Host: localhost\r");
    os.println("Upgrade: HMPP/0.9\r");
    os.println("Content-Length: 0\r");
    os.println("\r");
    os.flush();

    String result;

    result = is.readLine();

    if (result.startsWith("HTTP/1.1 101")) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " " + result);
      
      while (! (result = is.readLine()).trim().equals("")) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " " + result);
      }

      if (log.isLoggable(Level.FINEST)) {
	_os = new HessianDebugOutputStream(_os, log, Level.FINEST);
	_is = new HessianDebugInputStream(_is, log, Level.FINEST);
      }
      
      _out = new Hessian2StreamingOutput(_os);
      _in = new Hessian2StreamingInput(_is);

      ThreadPool.getThreadPool().start(new ClientPacketHandler(this));
    }
    else {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " " + result);
      
      throw new IOException("Unexpected result: " + result);
    }
  }

  Hessian2StreamingInput getStreamingInput()
  {
    return _in;
  }

  public void writePacket(Packet packet)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(packet);
    }
  }

  /**
   * Sets the message listener
   */
  public void setMessageHandler(MessageHandler listener)
  {
    _messageHandler = listener;
  }

  /**
   * Gets the message listener
   */
  public MessageHandler getMessageHandler()
  {
    return _messageHandler;
  }

  /**
   * Sets the presence handler
   */
  public void setPresenceHandler(PresenceHandler handler)
  {
    _presenceHandler = handler;
  }

  /**
   * Gets the message listener
   */
  public PresenceHandler getPresenceHandler()
  {
    return _presenceHandler;
  }

  /**
   * Sends a message to a given jid
   */
  public void sendMessage(String to, Serializable value)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new Message(to, value));
      out.flush();
    }
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(Serializable []data)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new Presence(data));
      out.flush();
    }
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(String to, Serializable []data)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new Presence(to, data));
      out.flush();
    }
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(Serializable []data)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new PresenceUnavailable(data));
      out.flush();
    }
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(String to, Serializable []data)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new PresenceUnavailable(to, data));
      out.flush();
    }
  }

  /**
   * Sends a presence probe packet to the server
   */
  public void presenceProbe(String to, Serializable []data)
    throws IOException
  {
    String from = null;
    
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new PresenceProbe(to, from, data));
      out.flush();
    }
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceSubscribe(String to, Serializable []data)
    throws IOException
  {
    String from = null;
    
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new PresenceSubscribe(to, from, data));
      out.flush();
    }
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceSubscribed(String to, Serializable []data)
    throws IOException
  {
    String from = null;
    
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new PresenceSubscribed(to, from, data));
      out.flush();
    }
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceUnsubscribe(String to, Serializable []data)
    throws IOException
  {
    String from = null;
    
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new PresenceUnsubscribe(to, from, data));
      out.flush();
    }
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceUnsubscribed(String to, Serializable []data)
    throws IOException
  {
    String from = null;
    
    Hessian2StreamingOutput out = _out;
    
    if (out != null) {
      out.writeObject(new PresenceUnsubscribed(to, from, data));
      out.flush();
    }
  }

  /**
   * Sets the query handler
   */
  public void setQueryHandler(QueryHandler handler)
  {
    _queryHandler = handler;
  }

  /**
   * Gets the query handler
   */
  public QueryHandler getQueryHandler()
  {
    return _queryHandler;
  }

  /**
   * Sends a query-get packet to the server
   */
  public void queryGet(String to,
		       Serializable value,
		       QueryCallback callback,
		       Object handback)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      long id;
      
      synchronized (this) {
	id = _qId++;

	_queryMap.put(id, new QueryItem(id, callback, handback));
      }

      out.writeObject(new QueryGet(id, to, value));
      out.flush();
    }
  }

  /**
   * Sends a query-set packet to the server
   */
  public void querySet(String to,
		       Serializable value,
		       QueryCallback callback,
		       Object handback)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      long id;
      
      synchronized (this) {
	id = _qId++;

	_queryMap.put(id, new QueryItem(id, callback, handback));
      }

      out.writeObject(new QuerySet(id, to, value));
      out.flush();
    }
  }

  /**
   * Sends a query-get packet to the server
   *
   * XXX: api needs response
   */
  public void querySet(String to, Serializable value)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      long id;
      
      synchronized (this) {
	id = _qId++;
      }
      
      out.writeObject(new QuerySet(id, to, value));
      out.flush();
    }
  }

  /**
   * Callback for the response
   */
  void onQueryResult(long id, String to, String from, Serializable value)
  {
    QueryItem item = null;
    
    synchronized (this) {
      item = _queryMap.remove(id);
    }

    if (item != null)
      item.onQueryResult(to, from, value);
  }

  /**
   * Callback for the response
   */
  void onQueryError(long id,
		    String to,
		    String from,
		    Serializable value,
		    HmppError error)
  {
    QueryItem item = null;
    
    synchronized (this) {
      item = _queryMap.remove(id);
    }

    if (item != null)
      item.onQueryError(to, from, value, error);
  }

  /**
   * Low-level query response
   */
  public void queryResult(long id, String to, Serializable value)
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.writeObject(new QueryResult(id, to, value));
      out.flush();
    }
  }

  /**
   * Low-level query error
   */
  public void queryError(long id,
			 String to,
			 Serializable value,
			 HmppError error)
  {
    try {
      Hessian2StreamingOutput out = _out;

      if (out != null) {
	out.writeObject(new QueryError(id, to, null, value, error));
	out.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void flush()
    throws IOException
  {
    Hessian2StreamingOutput out = _out;

    if (out != null) {
      out.flush();
    }
  }

  public boolean isClosed()
  {
    return _s == null;
  }

  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");
    
    try {
      Socket s;
      InputStream is;
      OutputStream os;
      
      synchronized (this) {
	s = _s;
	_s = null;
	
	is = _is;
	_is = null;

	_in = null;
	
	os = _os;
	_os = null;

	_out = null;
      }

      if (os != null) {
	try { os.close(); } catch (IOException e) {}
      }

      if (is != null) {
	is.close();
      }

      if (s != null) {
	s.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _port + "]";
  }

  protected void finalize()
  {
    close();
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
}
