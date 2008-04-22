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

package com.caucho.hmpp.client;

import com.caucho.hmpp.*;
import com.caucho.hmpp.auth.*;
import com.caucho.hmpp.packet.*;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * HMPP client protocol
 */
public class HmppClient implements HmppConnection {
  private static final Logger log
    = Logger.getLogger(HmppClient.class.getName());

  private String _url;
  private String _scheme;
  private String _host;
  private int _port;
  private String _path;
  
  private InetAddress _address;
  
  private String _to;

  protected Socket _s;
  protected InputStream _is;
  protected OutputStream _os;

  private HmppClientStream _clientStream;
  private String _jid;

  private MessageStream _messageHandler;
  private QueryStream _queryHandler;
  private PresenceStream _presenceHandler;

  private HashMap<Long,QueryItem> _queryMap
    = new HashMap<Long,QueryItem>();
    
  private long _qId;

  private boolean _isFinest;

  public HmppClient(String url)
  {
    _url = url;
    parseURL(url);

    _isFinest = log.isLoggable(Level.FINEST);
  }

  protected void parseURL(String url)
  {
    int p = url.indexOf("://");

    if (p < 0)
      throw new IllegalArgumentException("URL '" + url + "' is not well-formed");

    _scheme = url.substring(0, p);

    url = url.substring(p + 3);
    
    p = url.indexOf("/");
    if (p >= 0) {
      _path = url.substring(p);
      url = url.substring(0, p);
    }
    else {
      _path = "/";
    }

    p = url.indexOf(':');
    if (p > 0) {
      _host = url.substring(0, p);
      _port = Integer.parseInt(url.substring(p + 1));
    }
    else {
      _host = url;
      if ("https".equals(_scheme))
	_port = 443;
      else
	_port = 80;
    }
  }

  public String getHost()
  {
    return _host;
  }

  public int getPort()
  {
    return _port;
  }

  public void connect()
    throws IOException
  {
    if (_s != null)
      throw new IllegalStateException(this + " is already connected");

    openSocket(_host, _port);

    print(_os, "POST /hemp HTTP/1.1\r\n");
    print(_os, "Host: " + _to + ":" + _port + "\r\n");
    print(_os, "Upgrade: HMPP/0.9\r\n");
    print(_os, "Content-Length: 0\r\n");
    print(_os, "\r\n");
    _os.flush();

    String result;

    result = readLine(_is);

    if (result.startsWith("HTTP/1.1 101")) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " " + result);
      
      while (! (result = readLine(_is)).trim().equals("")) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " " + result);
      }

      _clientStream = new HmppClientStream(_is, _os);

      executeThread(new ClientInboundStream(this));
    }
    else {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " " + result);
      
      throw new IOException("Unexpected result: " + result);
    }
  }

  protected void executeThread (Runnable r)
  {
    Thread thread = new Thread(r);
    thread.setDaemon(true);
    thread.start();
  }
      

  protected void openSocket(String host, int port)
    throws IOException
  {
    _s = new Socket(_host, _port);

    InputStream is = _s.getInputStream();
    OutputStream os = _s.getOutputStream();
    
    _os = new BufferedOutputStream(os);
    _is = new BufferedInputStream(is);
  }

  protected void print(OutputStream os, String s)
    throws IOException
  {
    int len = s.length();

    for (int i = 0; i < len; i++)
      os.write(s.charAt(i));
  }

  protected String readLine(InputStream is)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    int ch;

    while ((ch = is.read()) >= 0 && ch != '\n') {
      sb.append((char) ch);
    }

    return sb.toString();
  }

  /**
   * Login to the server
   */
  public void login(String uid, String password)
  {
    try {
      AuthResult result;
      result = (AuthResult) querySet("", new AuthQuery(uid, password));

      _jid = result.getJid();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  Hessian2StreamingInput getStreamingInput()
  {
    return _clientStream.getStreamingInput();
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Sets the message listener
   */
  public void setMessageHandler(MessageStream listener)
  {
    _messageHandler = listener;
  }

  /**
   * Gets the message listener
   */
  public MessageStream getMessageHandler()
  {
    return _messageHandler;
  }

  /**
   * Sets the presence handler
   */
  public void setPresenceHandler(PresenceStream handler)
  {
    _presenceHandler = handler;
  }

  /**
   * Gets the message listener
   */
  public PresenceStream getPresenceHandler()
  {
    return _presenceHandler;
  }

  /**
   * Returns the client stream
   */
  public HmppStream getStream()
  {
    return _clientStream;
  }

  /**
   * Sends a message to a given jid
   */
  public void sendMessage(String to, Serializable value)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.sendMessage(to, null, value);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresence(null, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresence(to, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceUnavailable(null, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceUnavailable(to, null, data);
  }

  /**
   * Sends a presence probe packet to the server
   */
  public void presenceProbe(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceProbe(to, null, data);
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceSubscribe(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceSubscribe(to, null, data);
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceSubscribed(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceSubscribed(to, null, data);
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceUnsubscribe(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceUnsubscribe(to, null, data);
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceUnsubscribed(String to, Serializable []data)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceUnsubscribed(to, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceError(String to, Serializable []data, HmppError error)
  {
    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendPresenceError(to, null, data, error);
  }

  /**
   * Sets the query handler
   */
  public void setQueryHandler(QueryStream handler)
  {
    _queryHandler = handler;
  }

  /**
   * Gets the query handler
   */
  public QueryStream getQueryHandler()
  {
    return _queryHandler;
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable queryGet(String to,
			       Serializable query)
  {
    WaitQueryCallback callback = new WaitQueryCallback();

    queryGet(to, query, callback);

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
		       QueryCallback callback)
  {
    long id;
      
    synchronized (this) {
      id = _qId++;

      _queryMap.put(id, new QueryItem(id, callback));
    }

    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    _clientStream.sendQueryGet(id, to, null, value);
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable querySet(String to,
			       Serializable query)
  {
    WaitQueryCallback callback = new WaitQueryCallback();

    querySet(to, query, callback);

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
		       QueryCallback callback)
  {
    long id;
      
    synchronized (this) {
      id = _qId++;

      _queryMap.put(id, new QueryItem(id, callback));
    }

    HmppStream stream = _clientStream;

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.sendQuerySet(id, to, null, value);
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
  /*
  public void queryResult(long id, String to, Serializable value)
    throws IOException
  {
    _clientStream.sendQueryResult(id, to, null, value);
  }
  */

  /**
   * Low-level query error
   */
  /*
  public void queryError(long id,
			 String to,
			 Serializable value,
			 HmppError error)
  {
    _clientStream.sendQueryError(id, to, null, value, error);
  }
  */

  public void flush()
    throws IOException
  {
    HmppClientStream stream = _clientStream;

    if (stream != null)
      stream.flush();
  }

  public boolean isClosed()
  {
    return _clientStream == null;
  }

  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");
    
    try {
      Socket s;
      InputStream is;
      OutputStream os;
      HmppClientStream stream;
      
      synchronized (this) {
	s = _s;
	_s = null;
	
	is = _is;
	_is = null;

	stream = _clientStream;
	_clientStream = null;
	
	os = _os;
	_os = null;
      }

      if (stream != null)
	stream.close();

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

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jid + "," + _url + "]";
  }

  @Override
  protected void finalize()
  {
    close();
  }

  static class QueryItem {
    private final long _id;
    private final QueryCallback _callback;

    QueryItem(long id, QueryCallback callback)
    {
      _id = id;
      _callback = callback;
    }

    void onQueryResult(String to, String from, Serializable value)
    {
      if (_callback != null)
	_callback.onQueryResult(to, from, value);
    }

    void onQueryError(String to,
		      String from,
		      Serializable value,
		      HmppError error)
    {
      if (_callback != null)
	_callback.onQueryError(to, from, value, error);
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
			      Serializable value)
    {
      _result = value;

      synchronized (this) {
	_isResult = true;
	notifyAll();
      }
    }
  
    public void onQueryError(String fromJid, String toJid,
			     Serializable value, HmppError error)
    {
      _error = error;

      synchronized (this) {
	_isResult = true;
	notifyAll();
      }
    }
  }
}
