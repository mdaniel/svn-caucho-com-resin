/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.hmux;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import java.security.cert.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.log.Log;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.jsp.*;

import com.caucho.server.resin.ServletServer;

import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.DispatchServer;

import com.caucho.server.connection.Connection;
import com.caucho.server.connection.AbstractHttpRequest;

import com.caucho.server.port.ServerRequest;

import com.caucho.server.webapp.ErrorPageManager;

import com.caucho.server.http.InvocationKey;

import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.BackingManager;
import com.caucho.server.cluster.ObjectBacking;

/**
 * Handles requests from a remote dispatcher.  For example, mod_caucho
 * and the IIS plugin use this protocol to talk to the backend.
 *
 * <p>Packets are straightforward:
 * <pre>code l2 l1 l0 contents</pre>
 * Where code is the code of the packet and l2-0 give 12 bits of length.
 *
 * <p>The protocol is designed to allow pipelining and buffering whenever
 * possible.  So most commands are not acked.  Data from the frontend (POST)
 * does need acks to prevent deadlocks at either end while waiting
 * for new data.
 *
 * <p>The overriding protocol is controlled by requests from the
 * frontend server.
 * 
 * <p>A ping request:
 * <pre>
 * Frontend       Backend
 * CSE_PING
 * CSE_END
 *                CSE_END
 * <pre>
 * 
 * <p>A GET request:
 * <pre>
 * Frontend       Backend
 * CSE_METHOD
 * ...
 * CSE_HEADER/CSE_VALUE
 * CSE_END
 *                CSE_DATA
 *                CSE_DATA
 *                CSE_END
 * <pre>
 * 
 * <p>Short POST:
 * <pre>
 * Frontend       Backend
 * CSE_METHOD
 * ...
 * CSE_HEADER/CSE_VALUE
 * CSE_DATA
 * CSE_END
 *                CSE_DATA
 *                CSE_DATA
 *                CSE_END
 * <pre>
 * 
 * <p>Long POST:
 * <pre>
 * Frontend       Backend
 * CSE_METHOD
 * ...
 * CSE_HEADER/CSE_VALUE
 * CSE_DATA
 *                CSE_DATA (optional)
 * CSE_DATA
 *                CSE_ACK
 *                CSE_DATA (optional)
 * CSE_DATA
 *                CSE_ACK
 * CSE_END
 *                CSE_DATA
 *                CSE_END
 * <pre>
 * 
 */
public class HmuxRequest extends AbstractHttpRequest
  implements ServerRequest {
  private static final Logger log = Log.open(HmuxRequest.class);

  // HMUX channel control codes
  public static final int HMUX_CHANNEL =        'C';
  public static final int HMUX_ACK =            'A';
  public static final int HMUX_ERROR =          'E';
  public static final int HMUX_YIELD =          'Y';
  public static final int HMUX_QUIT =           'Q';
  public static final int HMUX_EXIT =           'X';

  public static final int HMUX_DATA =           'D';
  public static final int HMUX_URI =            'U';
  public static final int HMUX_STRING =         'S';
  public static final int HMUX_HEADER =         'H';
  public static final int HMUX_PROTOCOL =       'P';

  // The following are HTTP codes
  public static final int CSE_NULL =            '?';
  public static final int CSE_PATH_INFO =       'b';
  public static final int CSE_PROTOCOL =        'c';
  public static final int CSE_REMOTE_USER =     'd';
  public static final int CSE_QUERY_STRING =    'e';
  public static final int HMUX_FLUSH  =         'f';
  public static final int CSE_SERVER_PORT =     'g';
  public static final int CSE_REMOTE_HOST =     'h';
  public static final int CSE_REMOTE_ADDR =     'i';
  public static final int CSE_REMOTE_PORT =     'j';
  public static final int CSE_REAL_PATH =       'k';
  public static final int CSE_SCRIPT_FILENAME = 'l';
  public static final int HMUX_METHOD =         'm';
  public static final int CSE_AUTH_TYPE =       'n';
  public static final int CSE_URI =             'o';
  public static final int CSE_CONTENT_LENGTH =  'p';
  public static final int CSE_CONTENT_TYPE =    'q';
  public static final int CSE_IS_SECURE =       'r';
  public static final int HMUX_STATUS =         's';
  public static final int CSE_CLIENT_CERT =     't';
  public static final int CSE_SERVER_TYPE =     'u';
  public static final int HMUX_SERVER_NAME =    'v';
  
  public static final int CSE_SEND_HEADER =     'G';

  public static final int CSE_DATA =            'D';
  public static final int CSE_FLUSH =           'F';
  public static final int CSE_KEEPALIVE =       'K';
  public static final int CSE_ACK =             'A';
  public static final int CSE_END =             'Z';
  public static final int CSE_CLOSE =           'X';

  // other, specialized protocols
  public static final int CSE_QUERY =           'Q';
  public static final int CSE_PING =            'P';

  public static final int HMUX_CLUSTER_PROTOCOL = 0x101;
  public static final int HMUX_DISPATCH_PROTOCOL = 0x102;
  
  static final int HTTP_0_9 = 0x0009;
  static final int HTTP_1_0 = 0x0100;
  static final int HTTP_1_1 = 0x0101;

  private static final int HEADER_CAPACITY = 256;
  
  static final CharBuffer _getCb = new CharBuffer("GET");
  static final CharBuffer _headCb = new CharBuffer("HEAD");
  static final CharBuffer _postCb = new CharBuffer("POST");

  private final CharBuffer _method;   // "GET"
  private String _methodString;       // "GET"
  // private CharBuffer scheme;       // "http:"
  private CharBuffer _host;            // www.caucho.com
  private int _port;                   // :80

  private ByteBuffer _uri;             // "/path/test.jsp/Junk"
  private CharBuffer _protocol;        // "HTTP/1.0"
  private int _version;

  private CharBuffer _remoteAddr;
  private CharBuffer _remoteHost;
  private CharBuffer _serverName;
  private CharBuffer _serverPort;
  private CharBuffer _remotePort;
  
  private boolean _isSecure;
  private ByteBuffer _clientCert;

  private CharBuffer []_headerKeys;
  private CharBuffer []_headerValues;
  private int _headerSize;

  private byte []_lengthBuf;

  private int _serverType;

  // write stream from the connection
  private WriteStream _rawWrite;
  // servlet write stream
  private WriteStream _writeStream;
  
  // StreamImpl to break reads and writes to the underlying protocol
  private ServletFilter _filter;
  private int _pendingData;

  private InvocationKey _invocationKey = new InvocationKey();

  private CharBuffer _cb1;
  private CharBuffer _cb2;
  private boolean _hasRequest;

  private AbstractClusterRequest _clusterRequest;
  private HmuxDispatchRequest _dispatchRequest;
  private BackingManager _backingManager;
  private Cluster _cluster;
  
  private ErrorPageManager _errorManager = new ErrorPageManager();

  private int _srunIndex;

  public HmuxRequest(DispatchServer server, Connection conn)
  {
    super(server, conn);

    _response = new HmuxResponse(this);

    _rawWrite = conn.getWriteStream();
    _writeStream = new WriteStream();
    _writeStream.setReuseBuffer(true);

    // XXX: response.setIgnoreClientDisconnect(server.getIgnoreClientDisconnect());

    _cluster = Cluster.getLocal();
    if (_cluster != null) {
      try {
	Class cl = Class.forName("com.caucho.server.hmux.HmuxClusterRequest");

	_clusterRequest = (AbstractClusterRequest) cl.newInstance();
	_clusterRequest.setRequest(this);
	_clusterRequest.setCluster(_cluster);
      } catch (ClassNotFoundException e) {
	log.finer(e.toString());
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }
    
    _dispatchRequest = new HmuxDispatchRequest(this);
    
    _uri = new ByteBuffer();

    _method = new CharBuffer();
    _host = new CharBuffer();
    _protocol = new CharBuffer();

    _headerKeys = new CharBuffer[HEADER_CAPACITY];
    _headerValues = new CharBuffer[_headerKeys.length];
    for (int i = 0; i < _headerKeys.length; i++) {
      _headerKeys[i] = new CharBuffer();
      _headerValues[i] = new CharBuffer();
    }

    _remoteHost = new CharBuffer();
    _remoteAddr = new CharBuffer();
    _serverName = new CharBuffer();
    _serverPort = new CharBuffer();
    _remotePort = new CharBuffer();

    _clientCert = new ByteBuffer();

    _cb1 = new CharBuffer();
    _cb2 = new CharBuffer();

    _lengthBuf = new byte[16];

    _filter = new ServletFilter();
  }

  /**
   * Handles a new request.  Initializes the protocol handler and
   * the request streams.
   *
   * <p>Note: ClientDisconnectException must be rethrown to
   * the caller.
   */
  public boolean handleRequest()
    throws IOException
  {
    // XXX: should be moved to TcpConnection
    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(_server.getClassLoader());
    
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "start request");

    _filter.init(this, _rawRead, _rawWrite);
    _writeStream.init(_filter);
    // _writeStream.setWritePrefix(3);

    _response.init(_writeStream);

    _serverType = 0;
    _uri.setLength(0);
    
    boolean hasRequest = false;
    
    try {
      start();
      _response.start();

      try {
        if (! scanHeaders()) {
          killKeepalive();
          return false;
        }
        else if (_uri.size() == 0) {
          return true;
	}
      } catch (InterruptedIOException e) {
        killKeepalive();
        log.fine(dbgId() + "interrupted keepalive");
        return false;
      }

      if (_isSecure)
        getClientCertificate();

      hasRequest = true;
      // setStartDate();

      _filter.setPending(_pendingData);

      try {
        if (_method.getLength() == 0)
          throw new RuntimeException("HTTP protocol exception");
          
        _invocationKey.init(getHost(), getServerPort(),
                            _uri.getBuffer(), _uri.getLength());

        Invocation invocation;

        invocation = _server.getInvocation(_invocationKey);

        if (invocation == null) {
          invocation = _server.createInvocation();

          if (_host != null)
            invocation.setHost(_host.toString());
          
          invocation.setPort(getServerPort());

          InvocationDecoder decoder = _server.getInvocationDecoder();

          decoder.splitQueryAndUnescape(invocation,
                                        _uri.getBuffer(),
                                        _uri.getLength());

          _server.buildInvocation(_invocationKey.clone(), invocation);
        }

	setInvocation(invocation);
      
        invocation.service(this, _response);
      } catch (ClientDisconnectException e) {
        throw e;
      } catch (Throwable e) {
        try {
          _errorManager.sendServletError(e, this, _response);
        } catch (ClientDisconnectException e1) {
          throw e1;
        } catch (Exception e1) {
          log.log(Level.FINE, e1.toString(), e1);
        }

	return false;
      }
    } finally {
      if (! hasRequest)
	_response.setHeaderWritten(true);
      
      try {
	finish();
	_response.finish();
      } catch (ClientDisconnectException e) {
        throw e;
      } catch (Exception e) {
	killKeepalive();
        log.log(Level.FINE, dbgId() + e, e);
      }

      try {
	_writeStream.setDisableClose(false);
	_writeStream.close();
      } catch (ClientDisconnectException e) {
	killKeepalive();
        log.log(Level.FINE, dbgId() + e, e);

        throw e;
      } catch (Exception e) {
	killKeepalive();
        log.log(Level.FINE, dbgId() + e, e);
      }

      try {
	_readStream.setDisableClose(false);
	_readStream.close();
      } catch (ClientDisconnectException e) {
        throw e;
      } catch (Exception e) {
	killKeepalive();
        log.log(Level.FINE, dbgId() + e, e);
      }
    }

    boolean allowKeepalive = allowKeepalive();
    
    if (log.isLoggable(Level.FINE)) {
      if (allowKeepalive)
	log.fine(dbgId() + "complete request - keepalive");
      else
	log.fine(dbgId() + "complete request");
    }

    return allowKeepalive;
  }

  /**
   * Initialize the read stream from the raw stream.
   */
  protected boolean initStream(ReadStream readStream,
                               ReadStream rawStream)
    throws IOException
  {
    readStream.init(_filter, null);

    return true;
  }

  private void getClientCertificate()
  {
    String cipher = getHeader("SSL_CIPHER");
    if (cipher == null)
      cipher = getHeader("HTTPS_CIPHER");
    if (cipher != null)
      setAttribute("javax.servlet.request.cipher_suite", cipher);
    
    String keySize = getHeader("SSL_CIPHER_USEKEYSIZE");
    if (keySize == null)
      keySize = getHeader("SSL_SECRETKEYSIZE");
    if (keySize != null)
      setAttribute("javax.servlet.request.key_size", keySize);
    
    if (_clientCert.size() == 0)
      return;
    
    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      InputStream is = _clientCert.createInputStream();
      Object cert = cf.generateCertificate(is);
      is.close();
      setAttribute("javax.servlet.request.X509Certificate", cert);
      setAttribute(com.caucho.server.security.AbstractAuthenticator.LOGIN_NAME,
                   ((X509Certificate) cert).getSubjectDN());
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Returns true for the top-level request, but false for any include()
   * or forward()
   */
  public boolean isTop()
  {
    return true;
  }
  
  protected boolean checkLogin()
  {
    return true;
  }

  /**
   * Clears variables at the start of a new request.
   */
  protected void start()
    throws IOException
  {
    super.start();

    _method.clear();
    _methodString = null;
    _protocol.clear();
    _version = 0;
    _uri.clear();
    _host.clear();
    _port = 0;

    _headerSize = 0;

    _remoteHost.clear();
    _remoteAddr.clear();
    _serverName.clear();
    _serverPort.clear();
    _remotePort.clear();

    _clientCert.clear();

    _pendingData = 0;

    _isSecure = false;
  }
  
  /**
   * Fills request parameters from the stream.
   */
  private boolean scanHeaders()
    throws IOException
  {
    boolean hasURI = false;
    CharBuffer cb = _cb;
    boolean isLoggable = log.isLoggable(Level.FINE);
    ReadStream is = _rawRead;
    int code;
    int len;

    code = is.read();
    if (code != HMUX_CHANNEL) {
      if (isLoggable)
        log.fine(dbgId() + "closing " + (char) code + "(" + code + ")");
      return false;
    }

    int channel = (is.read() << 8) + is.read();

    while (true) {
      code = is.read();

      switch (code) {
      case -1:
        if (isLoggable)
          log.fine(dbgId() + "end of file");
        return false;

      case HMUX_QUIT:
        if (isLoggable)
          log.fine(dbgId() + (char) code + ": end of request");
        
        return hasURI;

      case HMUX_EXIT:
        if (isLoggable)
          log.fine(dbgId() + (char) code + ": end of socket");

        killKeepalive();
        
        return hasURI;

      case HMUX_PROTOCOL:
        len = (is.read() << 8) + is.read();

        if (len != 4) {
          log.fine(dbgId() + (char) code + ": protocol length (" + len + ") must be 4.");
          killKeepalive();
          return false;
        }
        
        int value = ((is.read() << 24) +
                     (is.read() << 16) +
                     (is.read() << 8) +
                     (is.read()));

	boolean isKeepalive = false;
        if (value == HMUX_CLUSTER_PROTOCOL) {
          if (isLoggable)
            log.fine(dbgId() + (char) code + ": cluster protocol");
          _filter.setClientClosed(true);
	  
          isKeepalive = _clusterRequest.handleRequest(is, _rawWrite);
        }
        else if (value == HMUX_DISPATCH_PROTOCOL) {
          if (isLoggable)
            log.fine(dbgId() + (char) code + ": dispatch protocol");
          _filter.setClientClosed(true);
          isKeepalive = _dispatchRequest.handleRequest(is, _rawWrite);
        }
	else {
	  log.fine(dbgId() + (char) code + ": unknown protocol (" + value + ")");
	  isKeepalive = false;
	}

	if (! allowKeepalive())
	  isKeepalive = false;
	
        if (isKeepalive) {
          _rawWrite.write(HMUX_QUIT);
	  _rawWrite.flush();
	}
	else {
          _rawWrite.write(HMUX_EXIT);
	  _rawWrite.close();
	}

	return isKeepalive;

      case HMUX_URI:
        hasURI = true;
        len = (is.read() << 8) + is.read();
        _uri.setLength(len);
	_rawRead.readAll(_uri.getBuffer(), 0, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + ":uri " + _uri);
	break;
	
      case HMUX_METHOD:
        len = (is.read() << 8) + is.read();
	is.readAll(_method, len);
	if (isLoggable)
	  log.fine(dbgId() + 
                   (char) code + ":method " + _method);
	break;

      case CSE_REAL_PATH:
        len = (is.read() << 8) + is.read();
	_cb1.clear();
	_rawRead.readAll(_cb1, len);
	code = _rawRead.read();
	if (code != HMUX_STRING)
	  throw new IOException("protocol expected HMUX_STRING");
	_cb2.clear();
	_rawRead.readAll(_cb2, readLength());

	//http.setRealPath(cb1.toString(), cb2.toString());
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " +
                   _cb1.toString() + "->" + _cb2.toString());
	//throw new RuntimeException();
	break;

      case CSE_REMOTE_HOST:
        len = (is.read() << 8) + is.read();
	_rawRead.readAll(_remoteHost, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + _remoteHost);
	break;

      case CSE_REMOTE_ADDR:
        len = (is.read() << 8) + is.read();
	_rawRead.readAll(_remoteAddr, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + _remoteAddr);
	break;

      case HMUX_SERVER_NAME:
        len = (is.read() << 8) + is.read();
	_rawRead.readAll(_serverName, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " server-host: " + _serverName);
	break;

      case CSE_REMOTE_PORT:
        len = (is.read() << 8) + is.read();
	_rawRead.readAll(_remotePort, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code +
                  " remote-port: " + _remotePort);
	break;

      case CSE_SERVER_PORT:
        len = (is.read() << 8) + is.read();
	_rawRead.readAll(_serverPort, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code +
                  " server-port: " + _serverPort);
	break;
	
      case CSE_QUERY_STRING:
        len = (is.read() << 8) + is.read();
        if (len > 0) {
          _uri.add('?');
          _uri.ensureCapacity(_uri.getLength() + len);
          _rawRead.readAll(_uri.getBuffer(), _uri.getLength(), len);
          _uri.setLength(_uri.getLength() + len);
        }
	break;

      case CSE_PROTOCOL:
        len = (is.read() << 8) + is.read();
	_rawRead.readAll(_protocol, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " protocol: " + _protocol);
	for (int i = 0; i < len; i++) {
	  char ch = _protocol.charAt(i);
	  if (ch >= '0' && ch <= '9')
	    _version = 16 * _version + ch - '0';
	  else if (ch == '.')
	    _version = 16 * _version;
	}
	break;

      case HMUX_HEADER:
        len = (is.read() << 8) + is.read();
	
	int headerSize = _headerSize;
	
	CharBuffer key = _headerKeys[headerSize];
	key.clear();
	
	CharBuffer valueCb = _headerValues[headerSize];
	valueCb.clear();
	
	_rawRead.readAll(key, len);
	code = _rawRead.read();
	if (code != HMUX_STRING)
	  throw new IOException("protocol expected HMUX_STRING at " + (char) code);
	_rawRead.readAll(valueCb, readLength());

	addHeaderInt(key.getBuffer(), 0, key.length(), valueCb);

	if (isLoggable)
	  log.fine(dbgId() + "H " + key + "=" + valueCb);

	_headerSize++;
	break;

      case CSE_CONTENT_LENGTH:
        len = (is.read() << 8) + is.read();
	if (_headerKeys.length <= _headerSize)
	  resizeHeaders();
	_headerKeys[_headerSize].clear();
	_headerKeys[_headerSize].append("Content-Length");
	_headerValues[_headerSize].clear();
	_rawRead.readAll(_headerValues[_headerSize], len);

	if (isLoggable)
	  log.fine(dbgId() + (char) code + " content-length=" +
                   _headerValues[_headerSize]);
	_headerSize++;
	break;

      case CSE_CONTENT_TYPE:
        len = (is.read() << 8) + is.read();
	if (_headerKeys.length <= _headerSize)
	  resizeHeaders();
	_headerKeys[_headerSize].clear();
	_headerKeys[_headerSize].append("Content-Type");
	_headerValues[_headerSize].clear();
	_rawRead.readAll(_headerValues[_headerSize], len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " content-type=" +
                   _headerValues[_headerSize]);
	_headerSize++;
	break;

      case CSE_IS_SECURE:
        len = (is.read() << 8) + is.read();
	_isSecure = true;
	if (isLoggable)
	  log.fine(dbgId() + "secure");
	_rawRead.skip(len);
	break;

      case CSE_CLIENT_CERT:
        len = (is.read() << 8) + is.read();
	_clientCert.clear();
        _clientCert.setLength(len);
	_rawRead.readAll(_clientCert.getBuffer(), 0, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " cert=" + _clientCert +
                   " len:" + len);
	break;

      case CSE_SERVER_TYPE:
        len = (is.read() << 8) + is.read();
	_cb1.clear();
	_rawRead.readAll(_cb1, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " server=" + _cb1);
        if (_cb1.length() > 0)
          _serverType = _cb1.charAt(0);
	break;

      case CSE_REMOTE_USER:
        len = (is.read() << 8) + is.read();
        _cb.clear();
	_rawRead.readAll(_cb, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + _cb);
        setAttribute(com.caucho.server.security.AbstractAuthenticator.LOGIN_NAME,
                     new com.caucho.security.BasicPrincipal(_cb.toString()));
	break;
	
      case CSE_DATA:
        len = (is.read() << 8) + is.read();
	_pendingData = len;
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " post-data: " + len);
	return hasURI;

      default:
        len = (is.read() << 8) + is.read();

	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + len);
	is.skip(len);
	break;
      }
    }

    // _filter.setClientClosed(true);

    // return false;
  }

  private void resizeHeaders()
  {
    CharBuffer []newKeys = new CharBuffer[_headerSize * 2];
    CharBuffer []newValues = new CharBuffer[_headerSize * 2];

    for (int i = 0; i < _headerSize; i++) {
      newKeys[i] = _headerKeys[i];
      newValues[i] = _headerValues[i];
    }
    
    for (int i = _headerSize; i < newKeys.length; i++) {
      newKeys[i] = new CharBuffer();
      newValues[i] = new CharBuffer();
    }

    _headerKeys = newKeys;
    _headerValues = newValues;
  }

  private int readLength()
    throws IOException
  {
    return ((_rawRead.read() << 8) + _rawRead.read());
  }

  /**
   * Returns the header.
   */
  public String getMethod()
  {
    if (_methodString == null) {
      CharSegment cb = getMethodBuffer();
      if (cb.length() == 0) {
        _methodString = "GET";
        return _methodString;
      }
      
      switch (cb.charAt(0)) {
      case 'G':
        _methodString = cb.equals(_getCb) ? "GET" : cb.toString();
        break;
        
      case 'H':
        _methodString = cb.equals(_headCb) ? "HEAD" : cb.toString();
        break;
        
      case 'P':
        _methodString = cb.equals(_postCb) ? "POST" : cb.toString();
        break;

      default:
        _methodString = cb.toString();
      }
    }

    return _methodString;
    
  }

  public CharSegment getMethodBuffer()
  {
    return _method;
  }

  /**
   * Returns a char buffer containing the host.
   */
  protected CharBuffer getHost()
  {
    if (_host.length() > 0)
      return _host;
    
    _host.append(_serverName);
    _host.toLowerCase();

    return _host;
  }

  public final byte []getUriBuffer()
  {
    return _uri.getBuffer();
  }

  public final int getUriLength()
  {
    return _uri.getLength();
  }

  /**
   * Returns the protocol.
   */
  public String getProtocol()
  {
    return _protocol.toString();
  }

  public CharSegment getProtocolBuffer()
  {
    return _protocol;
  }

  final int getVersion()
  {
    return _version;
  }

  /**
   * Returns true if the request is secure.
   */
  public boolean isSecure()
  {
    return _isSecure;
  }

  /**
   * Returns the header.
   */
  public String getHeader(String key)
  {
    CharSegment buf = getHeaderBuffer(key);
    if (buf != null)
      return buf.toString();
    else
      return null;
  }

  public CharSegment getHeaderBuffer(String key)
  {
    for (int i = 0; i < _headerSize; i++) {
      CharBuffer test = _headerKeys[i];

      if (test.equalsIgnoreCase(key))
	return _headerValues[i];
    }

    return null;
  }

  public CharSegment getHeaderBuffer(char []buf, int length)
  {
    for (int i = 0; i < _headerSize; i++) {
      CharBuffer test = _headerKeys[i];

      if (test.length() != length)
        continue;
      
      char []keyBuf = test.getBuffer();
      int j;
      for (j = 0; j < length; j++) {
        char a = buf[j];
        char b = keyBuf[j];
        if (a == b)
          continue;

        if (a >= 'A' && a <= 'Z')
          a += 'a' - 'A';
        if (b >= 'A' && b <= 'Z')
          b += 'a' - 'A';
        if (a != b)
          break;
      }

      if (j == length)
        return _headerValues[i];
    }

    return null;
  }

  public void setHeader(String key, String value)
  {
    if (_headerKeys.length <= _headerSize)
      resizeHeaders();
    
    _headerKeys[_headerSize].clear();
    _headerKeys[_headerSize].append(key);
    _headerValues[_headerSize].clear();
    _headerValues[_headerSize].append(value);
    _headerSize++;
  }

  public void getHeaderBuffers(String key, ArrayList<CharSegment> values)
  {
    CharBuffer cb = _cb;
    
    cb.clear();
    cb.append(key);

    int size = _headerSize;
    for (int i = 0; i < size; i++) {
      CharBuffer test = _headerKeys[i];
      if (test.equalsIgnoreCase(cb))
	values.add(_headerValues[i]);
    }
  }

  public Enumeration getHeaderNames()
  {
    HashSet<String> names = new HashSet<String>();
    for (int i = 0; i < _headerSize; i++)
      names.add(_headerKeys[i].toString());

    return Collections.enumeration(names);
  }

  /**
   * Returns the URI for the request, special casing the IIS issues.
   * Because IIS already escapes the URI before sending it, the URI
   * needs to be re-escaped.
   */
  public String getRequestURI() 
  {
    if (_serverType == 'R')
      return super.getRequestURI();

    String _rawURI = super.getRequestURI();
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < _rawURI.length(); i++) {
      char ch = _rawURI.charAt(i);

      switch (ch) {
      case ' ':
      case '?':
        addHex(cb, ch);
        break;

      default:
        cb.append(ch);
        break;
      }
    }

    return cb.close();
  }

  /**
   * Adds a hex escape.
   *
   * @param cb the char buffer containing the escape.
   * @param ch the character to be escaped.
   */
  private void addHex(CharBuffer cb, int ch)
  {
    cb.append('%');

    int d = (ch >> 4) & 0xf;
    if (d < 10)
      cb.append((char) ('0' + d));
    else
      cb.append((char) ('a' + d - 10));
    
    d = ch & 0xf;
    if (d < 10)
      cb.append((char) ('0' + d));
    else
      cb.append((char) ('a' + d - 10));
  }

  /**
   * Returns the server name.
   */
  public String getServerName()
  {
    CharBuffer host = getHost();
    if (host == null) {
      InetAddress addr = getConnection().getRemoteAddress();
      return addr.getHostName();
    }

    int p = host.indexOf(':');
    if (p >= 0)
      return host.substring(0, p);
    else
      return host.toString();
  }

  public int getServerPort()
  {
    int len = _serverPort.length();
    int port = 0;
    for (int i = 0; i < len; i++) {
      char ch = _serverPort.charAt(i);
      port = 10 * port + ch - '0';
    }

    return port;
  }

  public String getRemoteAddr()
  {
    return _remoteAddr.toString();
  }

  public void getRemoteAddr(CharBuffer cb)
  {
    cb.append(_remoteAddr);
  }

  public int printRemoteAddr(byte []buffer, int offset)
    throws IOException
  {
    char []buf = _remoteAddr.getBuffer();
    int len = _remoteAddr.getLength();

    for (int i = 0; i < len; i++)
      buffer[offset + i] = (byte) buf[i];

    return offset + len;
  }

  public String getRemoteHost()
  {
    return _remoteHost.toString();
  }

  /**
   * Called for a connection: close
   */
  protected void connectionClose()
  {
    // ignore for hmux
  }

  // Response data
  void writeStatus(CharBuffer message)
    throws IOException
  {
    int channel = 2;

    WriteStream os = _rawWrite;
    
    os.write(HMUX_CHANNEL);
    os.write(channel >> 8);
    os.write(channel);
              
    writeString(HMUX_STATUS, message);
  }

  /**
   * Complete sending of all headers.
   */
  void sendHeader()
    throws IOException
  {
    writeString(CSE_SEND_HEADER, "");
  }

  /**
   * Writes a header to the plugin.
   *
   * @param key the header's key
   * @param value the header's value
   */
  void writeHeader(String key, String value)
    throws IOException
  {
    writeString(HMUX_HEADER, key); 
    writeString(HMUX_STRING, value); 
  }

  /**
   * Writes a header to the plugin.
   *
   * @param key the header's key
   * @param value the header's value
   */
  void writeHeader(String key, CharBuffer value)
    throws IOException
  {
    writeString(HMUX_HEADER, key); 
    writeString(HMUX_STRING, value); 
  }

  void writeString(int code, String value)
    throws IOException
  {
    int len = value.length();

    WriteStream os = _rawWrite;
    
    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(value);
    
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + " " + value);
  }

  void writeString(int code, CharBuffer cb)
    throws IOException
  {
    int len = cb.length();

    WriteStream os = _rawWrite;
    
    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(cb.getBuffer(), 0, len);

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + " " + cb);
  }
    
  String dbgId()
  {
    String id = _server.getServerId();

    if (id.equals(""))
      return "[" + getConnection().getId() + "] ";
    else
      return "[" + id + ":" + getConnection().getId() + "] ";
  }

  /**
   * Implements the protocol for data reads and writes.  Data from the
   * web server to the JVM must be acked, except for the first data.
   * Data back to the web server needs no ack.
   */
  static class ServletFilter extends StreamImpl {
    HmuxRequest _request;
    ReadStream _is;
    WriteStream _os;
    byte []_buffer = new byte[16];
    int _pendingData;
    boolean _needsAck;
    boolean _isClosed;
    boolean _isClientClosed;

    ServletFilter()
    {
    }

    void init(HmuxRequest request,
	      ReadStream nextRead, WriteStream nextWrite)
    {
      _request = request;
      _is = nextRead;
      _os = nextWrite;
      _pendingData = 0;
      _isClosed = false;
      _isClientClosed = false;
      _needsAck = false;
    }

    void setPending(int pendingData)
    {
      _pendingData = pendingData;
    }

    void setClientClosed(boolean isClientClosed)
    {
      _isClientClosed = isClientClosed;
    }

    public boolean canRead()
    {
      return true;
    }

    public int getAvailable()
    {
      return _pendingData;
    }

    /**
     * Reads available data.  If the data needs an ack, then do so.
     */
    public int read(byte []buf, int offset, int length)
      throws IOException
    {
      int sublen = _pendingData;
      ReadStream is = _is;
      
      if (sublen <= 0)
	return -1;

      if (length < sublen)
	sublen = length;

      int readLen = is.read(buf, offset, sublen);
      _pendingData -= readLen;

      if (log.isLoggable(Level.FINEST))
        log.finest(new String(buf, offset, readLen));

      while (_pendingData == 0) {
        if (_needsAck) {
          int channel = 2;
          
          _os.write(HMUX_ACK);
          _os.write(channel >> 8);
          _os.write(channel);
          
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "A:ack channel 2");
        }
        
        _needsAck = false;

	int code = is.read();

        if (code == HMUX_DATA) {
          int len = (is.read() << 8) + is.read();
          
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "D:post-data " + len);

          _pendingData = len;
        }
        else if (code == HMUX_QUIT) {
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "Q:quit");
	  
          return readLen;
        }
        else if (code == HMUX_EXIT) {
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "X:exit");
	  
	  _request.killKeepalive();
          return readLen;
        }
        else if (code == HMUX_YIELD) {
          _needsAck = true;
        }
	else if (code == HMUX_CHANNEL) {
	  int channel = (is.read() << 8) + is.read();
	  
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "channel " + channel);
	}
        else if (code < 0) {
	  _request.killKeepalive();
	
          return readLen;
	}
        else {
	  _request.killKeepalive();
	  
          int len = (is.read() << 8) + is.read();
          
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "unknown `" + (char) code + "' " + len);

          is.skip(len);
        }
      }

      return readLen;
    }

    public boolean canWrite()
    {
      return true;
    }

    /**
     * Send data back to the web server
     */
    public void write(byte []buf, int offset, int length, boolean isEnd)
      throws IOException
    {
      if (log.isLoggable(Level.FINE)) {
	log.fine(_request.dbgId() + (char) HMUX_DATA + ":data " + length);
        
        if (log.isLoggable(Level.FINEST))
          log.finest(_request.dbgId() + "data <" + new String(buf, offset, length) + ">");
      }

      byte []tempBuf = _buffer;
      
      while (length > 0) {
	int sublen = length;

	if (32 * 1024 < sublen)
	  sublen = 32 * 1024;
	
        // The 3 bytes are already allocated by setPrefixWrite
	tempBuf[0] = HMUX_DATA;
	tempBuf[1] = (byte) (sublen >> 8);
	tempBuf[2] = (byte) sublen;

	_os.write(tempBuf, 0, 3);
	_os.write(buf, offset, sublen);

	length -= sublen;
	offset += sublen;
      }
    }

    public void flush()
      throws IOException
    {
      if (log.isLoggable(Level.FINE))
	log.fine(_request.dbgId() + (char) HMUX_FLUSH + ":flush");

      _os.write(HMUX_FLUSH);
      _os.write(0);
      _os.write(0);
      _os.flush();
    }

    public void close()
      throws IOException
    {
      if (_isClosed)
	return;

      _isClosed = true;

      if (_pendingData > 0) {
	_is.skip(_pendingData);
	_pendingData = 0;
      }

      boolean keepalive = _request.allowKeepalive();

      if (! _isClientClosed) {
	if (log.isLoggable(Level.FINE)) {
          if (keepalive)
            log.fine(_request.dbgId() + (char) HMUX_QUIT + ": quit channel");
          else
            log.fine(_request.dbgId() + (char) HMUX_EXIT + ": exit socket");
        }

        if (keepalive)
          _os.write(HMUX_QUIT);
        else
          _os.write(HMUX_EXIT);
      }

      if (keepalive)
        _os.flush();
      else
        _os.close();
      //nextRead.close();
    }
  }
}
