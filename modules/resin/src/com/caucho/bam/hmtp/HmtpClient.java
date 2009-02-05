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

package com.caucho.bam.hmtp;

import com.caucho.bam.QueryCallback;
import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorClient;
import com.caucho.bam.SimpleActorClient;
import com.caucho.bam.SimpleActorStream;
import com.caucho.bam.ActorException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.security.PublicKey;

/**
 * HMTP client protocol
 */
public class HmtpClient extends SimpleActorClient {
  private static final Logger log
    = Logger.getLogger(HmtpClient.class.getName());

  private String _url;
  private String _scheme;
  private String _host;
  private String _virtualHost;
  private int _port;
  private String _path;
  
  private InetAddress _address;
  
  private boolean _isEncryptPassword = true;

  protected Socket _s;
  protected InputStream _is;
  protected OutputStream _os;

  private ActorException _connException;

  private ClientLinkManager _linkManager = new ClientLinkManager();
  
  private ClientToLinkStream _brokerStream;
  private String _jid;

  private ActorStream _streamHandler;

  public HmtpClient(String url)
  {
    this(url, null);
  }

  public HmtpClient(String url, ActorStream actorStream)
  {
    _url = url;
    parseURL(url);

    if (actorStream == null)
      actorStream = new SimpleActorStream();
    
    setActorStream(actorStream);
  }

  public void setVirtualHost(String host)
  {
    _virtualHost = host;
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

  @Override
  public void setActorStream(ActorStream actorStream)
  {
    if (actorStream == null)
      throw new NullPointerException();

    super.setActorStream(actorStream);
  }

  public void setEncryptPassword(boolean isEncrypt)
  {
    _isEncryptPassword = isEncrypt;
  }

  public void connect(String user, String password)
  {
    connectImpl();

    loginImpl(user, password);
  }

  protected void connectImpl()
  {
    if (_s != null)
      throw new IllegalStateException(this + " is already connected");

    try {
      openSocket(_host, _port);

      // http upgrade

      print(_os, "CONNECT " + _path + " HTTP/1.1\r\n");

      String host = _virtualHost;
      if (host == null)
	host = _host;
      
      print(_os, "Host: " + host + ":" + _port + "\r\n");
      print(_os, "Upgrade: HMTP/0.9\r\n");
      print(_os, "Connection: Upgrade\r\n");
      print(_os, "Content-Length: 0\r\n");
      print(_os, "\r\n");
      _os.flush();

      String status = readLine(_is);

      String header;
	
      while (! (header = readLine(_is)).trim().equals("")) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " " + header);
      }

      if (status.startsWith("HTTP/1.1 101")) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " " + status);

	_brokerStream = new ClientToLinkStream(null, _os);
	setBrokerStream(_brokerStream);

	executeThread(new ClientFromLinkStream(this, _is));
      }
      else {
	_os.close();

	StringBuilder text = new StringBuilder();
	try {
	  int ch;
	  while ((ch = _is.read()) >= 0)
	    text.append((char) ch);
	} catch (Exception e) {
	  if (log.isLoggable(Level.FINER))
	    log.log(Level.FINER, e.toString(), e);
	}
	
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " " + status + "\n" + text);
      
	throw new RemoteConnectionFailedException("Failed to upgrade to HMTP\n" + status + "\n\n" + text);
      }
    } catch (ActorException e) {
      _connException = e;

      throw _connException;
    } catch (IOException e) {
      _connException = new ActorException(e);

      throw _connException;
    }
  }
      
  /**
   * Login to the server
   */
  protected void loginImpl(String uid, String password)
  {
    try {
      Serializable credentials = password;

      if (_isEncryptPassword) {
	GetPublicKeyQuery pkValue
	  = (GetPublicKeyQuery) queryGet(null, new GetPublicKeyQuery());

	PublicKey publicKey = _linkManager.getPublicKey(pkValue);

	ClientLinkManager.Secret secret = _linkManager.generateSecret();

	EncryptedObject encPassword
	  = _linkManager.encrypt(secret, publicKey, password);

	credentials = encPassword;
      }

      AuthResult result;
      result = (AuthResult) querySet(null, new AuthQuery(uid, credentials));

      _jid = result.getJid();

      if (log.isLoggable(Level.FINE))
	log.fine(this + " login");
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Returns the broker jid
   */
  public String getBrokerJid()
  {
    String jid = getJid();

    if (jid == null)
      return null;

    int p = jid.indexOf('@');
    int q = jid.indexOf('/');

    if (p >= 0 && q >= 0)
      return jid.substring(p + 1, q);
    else if (p >= 0)
      return jid.substring(p + 1);
    else if (q >= 0)
      return jid.substring(0, q);
    else
      return jid;
  }

  /**
   * Returns the current stream to the broker, throwing an exception if
   * it's unavailable
   */
  public ActorStream getBrokerStream()
  {
    ActorStream stream = _brokerStream;

    if (stream != null)
      return stream;
    else if (_connException != null)
      throw _connException;
    else
      throw new RemoteConnectionFailedException(_url + " connection has been closed");
  }

  public void flush()
    throws IOException
  {
    ClientToLinkStream stream = _brokerStream;

    if (stream != null)
      stream.flush();
  }

  public boolean isClosed()
  {
    return _brokerStream == null;
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

  /**
   * Spawns the thread to handle the inbound packets
   */
  protected void executeThread (Runnable r)
  {
    Thread thread = new Thread(r);
    thread.setName("hmtp-reader-" + _host + "-" + _port);
    thread.setDaemon(true);
    thread.start();
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


  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");

    super.close();
    
    try {
      Socket s;
      InputStream is;
      OutputStream os;
      ClientToLinkStream stream;
      
      synchronized (this) {
	s = _s;
	_s = null;
	
	is = _is;
	_is = null;

	stream = _brokerStream;
	_brokerStream = null;
	
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
}
