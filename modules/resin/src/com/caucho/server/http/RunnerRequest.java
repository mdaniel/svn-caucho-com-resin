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

package com.caucho.server.http;

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
public class RunnerRequest extends AbstractHttpRequest
  implements ServerRequest {
  private static final Logger log = Log.open(RunnerRequest.class);
  
  public static final int CSE_NULL =            '?';
  public static final int CSE_PATH_INFO =       'b';
  public static final int CSE_PROTOCOL =        'c';
  public static final int CSE_METHOD =          'd';
  public static final int CSE_QUERY_STRING =    'e';
  public static final int CSE_SERVER_NAME =     'f';
  public static final int CSE_SERVER_PORT =     'g';
  public static final int CSE_REMOTE_HOST =     'h';
  public static final int CSE_REMOTE_ADDR =     'i';
  public static final int CSE_REMOTE_PORT =     'j';
  public static final int CSE_REAL_PATH =       'k';
  public static final int CSE_SCRIPT_FILENAME = 'l';
  public static final int CSE_REMOTE_USER =     'm';
  public static final int CSE_AUTH_TYPE =       'n';
  public static final int CSE_URI =             'o';
  public static final int CSE_CONTENT_LENGTH =  'p';
  public static final int CSE_CONTENT_TYPE =    'q';
  public static final int CSE_IS_SECURE =       'r';
  public static final int CSE_SESSION_GROUP =   's';
  public static final int CSE_CLIENT_CERT =     't';
  public static final int CSE_SERVER_TYPE =     'u';
  
  public static final int CSE_STATUS =          'S';
  public static final int CSE_SEND_HEADER =     'G';

  public static final int CSE_HEADER =          'H';
  public static final int CSE_VALUE =           'V';

  public static final int CSE_DATA =            'D';
  public static final int CSE_FLUSH =           'F';
  public static final int CSE_KEEPALIVE =       'K';
  public static final int CSE_ACK =             'A';
  public static final int CSE_END =             'Z';
  public static final int CSE_CLOSE =           'X';

  // other, specialized protocols
  public static final int CSE_QUERY =           'Q';
  public static final int CSE_PING =            'P';
  public static final int CSE_SAVE_SESSION =    'B';
  public static final int CSE_LOAD_SESSION =    'C';
  public static final int CSE_SESSION_DATA =    'E';
  public static final int CSE_KILL_SESSION =    'I';
  public static final int CSE_SESSION_SRUN =    'J';
  public static final int CSE_DUMP_SESSION =    'L';
  
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
  
  private boolean _isSecure;
  private ByteBuffer _clientCert;

  private CharBuffer []_headerKeys;
  private CharBuffer []_headerValues;
  private int _headerSize;

  private final byte []_lengthBuf;

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

  private BackingManager _backingManager;
  private Cluster _cluster;
  
  private ErrorPageManager _errorManager = new ErrorPageManager();

  private int _srunIndex;

  public RunnerRequest(DispatchServer server, Connection conn)
  {
    super(server, conn);

    _response = new RunnerResponse(this);

    _rawWrite = conn.getWriteStream();
    _writeStream = new WriteStream();
    _writeStream.setReuseBuffer(true);

    // XXX: response.setIgnoreClientDisconnect(server.getIgnoreClientDisconnect());

    _backingManager = (BackingManager) server.getAttribute("caucho.dist.backing");
    _cluster = (Cluster) server.getAttribute("caucho.dist.group");
    
    _uri = new ByteBuffer();

    _method = new CharBuffer();
    _host = new CharBuffer();
    _protocol = new CharBuffer();

    _headerKeys = new CharBuffer[64];
    _headerValues = new CharBuffer[64];
    for (int i = 0; i < _headerKeys.length; i++) {
      _headerKeys[i] = new CharBuffer();
      _headerValues[i] = new CharBuffer();
    }

    _remoteHost = new CharBuffer();
    _remoteAddr = new CharBuffer();
    _serverName = new CharBuffer();
    _serverPort = new CharBuffer();

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
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "start request");

    _filter.init(this, _rawRead, _rawWrite);
    _writeStream.init(_filter);

    _response.init(_writeStream);

    _serverType = 0;
    
    boolean hasRequest = false;
    
    try {
      start();
      _response.start();

      try {
        if (! scanHeaders()) {
          killKeepalive();
          return false;
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

          decoder.splitQueryAndUnescape(invocation, _uri.getBuffer(),
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

          return false;
        }
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

      if (log.isLoggable(Level.FINE))
	log.fine(dbgId() + "complete request");
    }

    return allowKeepalive();
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

    _clientCert.clear();

    _pendingData = 0;
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
      
    while (_rawRead.readAll(_lengthBuf, 0, 4) == 4) {
      int code = _lengthBuf[0] & 0xff;
      int len = (((_lengthBuf[1] & 0xff) << 16) +
		 ((_lengthBuf[2] & 0xff) << 8) +
		 ((_lengthBuf[3] & 0xff)));

      switch (code) {
      case CSE_REAL_PATH:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);
	code = _rawRead.read();
	if (code != CSE_VALUE)
	  throw new IOException("protocol expected CSE_VALUE");
	_cb2.clear();
	_rawRead.readAll(_cb2, readLength());

	//http.setRealPath(_cb1.toString(), _cb2.toString());
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " +
		  _cb1.toString() + "->" + _cb2.toString());
	//throw new RuntimeException();
	break;

      case CSE_REMOTE_HOST:
	_rawRead.readAll(_remoteHost, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + _remoteHost);
	break;

      case CSE_REMOTE_ADDR:
	_rawRead.readAll(_remoteAddr, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + _remoteAddr);
	break;

      case CSE_SERVER_NAME:
	_rawRead.readAll(_serverName, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " server-host: " + _serverName);
	break;

      case CSE_SERVER_PORT:
	_rawRead.readAll(_serverPort, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code +
                  " server-port: " + _serverPort);
	break;
	
      case CSE_METHOD:
	_rawRead.readAll(_method, len);
	if (isLoggable)
	  log.fine(dbgId() + 
                   (char) code + " method: " + _method);
	break;

      case CSE_URI:
        hasURI = true;
        _uri.setLength(len);
	_rawRead.readAll(_uri.getBuffer(), 0, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " uri: " + _uri);
	break;
	
      case CSE_QUERY_STRING:
        if (len > 0) {
          _uri.add('?');
          _uri.ensureCapacity(_uri.getLength() + len);
          _rawRead.readAll(_uri.getBuffer(), _uri.getLength(), len);
          _uri.setLength(_uri.getLength() + len);
        }
	break;

      case CSE_PROTOCOL:
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

      case CSE_HEADER:
	int headerSize = _headerSize;
	if (_headerKeys.length <= headerSize)
	  resizeHeaders();
	
	CharBuffer key = _headerKeys[headerSize];
	key.clear();
	
	CharBuffer value = _headerValues[headerSize];
	value.clear();
	
	_rawRead.readAll(key, len);
	code = _rawRead.read();
	if (code != CSE_VALUE)
	  throw new IOException("protocol expected CSE_VALUE");
	_rawRead.readAll(value, readLength());

	addHeaderInt(key.getBuffer(), 0, key.length(), value);

	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + key + "=" + value);
	
	_headerSize = headerSize + 1;
	break;

      case CSE_CONTENT_LENGTH:
	if (_headerSize >= _headerKeys.length)
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
	if (_headerSize >= _headerKeys.length)
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
	_isSecure = true;
	if (isLoggable)
	  log.fine(dbgId() + "secure");
	_rawRead.skip(len);
	break;

      case CSE_CLIENT_CERT:
	_clientCert.clear();
        _clientCert.setLength(len);
	_rawRead.readAll(_clientCert.getBuffer(), 0, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " cert=" + _clientCert + " len:" + len);
	break;

      case CSE_SERVER_TYPE:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " server=" + _cb1);
        if (_cb1.length() > 0)
          _serverType = _cb1.charAt(0);
	break;

      case CSE_REMOTE_USER:
        cb.clear();
	_rawRead.readAll(cb, len);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + cb);
        setAttribute(com.caucho.server.security.AbstractAuthenticator.LOGIN_NAME,
                     new com.caucho.security.BasicPrincipal(cb.toString()));
	break;
	
      case CSE_DATA:
	_pendingData = len;
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " post-data: " + len);
	return hasURI;

      case CSE_PING:
	_rawWrite.write(CSE_END);
	_rawWrite.write(0);
	_rawWrite.write(0);
	_rawWrite.write(0);
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " ping: " + len);
	return false;

      case CSE_SESSION_SRUN:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);

	if (isLoggable)
	  log.fine(dbgId() + "srun: " + len);

        _srunIndex = 0;
        for (int i = 0; i < len; i++) {
          char ch = _cb1.charAt(i);
          if (ch >= '0' && ch <= '9')
            _srunIndex = 10 * _srunIndex + ch - '0';
        }
        ClusterServer distServer;
        distServer = _cluster.getServer(_srunIndex);
        if (distServer != null)
          distServer.wake();
        break;

      case CSE_SAVE_SESSION:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);

        if (! saveSession(_cb1)) {
          killKeepalive();
          return false;
        }
        break;

      case CSE_LOAD_SESSION:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);

        if (! loadSession(_cb1)) {
          killKeepalive();
          return false;
        }
        break;
        
      case CSE_DUMP_SESSION:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);

        if (! dumpSession(_cb1)) {
          killKeepalive();
          return false;
        }
        break;

      case CSE_KILL_SESSION:
	_cb1.clear();
	_rawRead.readAll(_cb1, len);

        if (! killSession(_cb1)) {
          killKeepalive();
          return false;
        }
        break;

      case CSE_END:
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + len);
	return hasURI;

      case CSE_CLOSE:
	if (isLoggable)
	  log.fine(dbgId() + "close");
	_filter.setClientClosed(true);
	killKeepalive();
	return false;

      default:
	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + len);
	_rawRead.skip(len);
	break;
      }
    }

    _filter.setClientClosed(true);

    return false;
  }

  /**
   * Save the session
   *
   * @param cb char buffer containing the session id
   */
  private boolean saveSession(CharBuffer cb)
    throws IOException
  {
    int p = cb.lastIndexOf(';');

    if (p < 0)
      return false;

    String distributionId = cb.substring(0, p);
    String objectId = cb.substring(p + 1);

    return false; // XXX:
    /*
    ObjectBacking backing;

    if (_backingManager == null)
      return false;

    backing = _backingManager.getBacking(distributionId, objectId);
    if (backing == null)
      return false;

    SessionImpl session = (SessionImpl) backing.getObject();
      
    if (session != null) {
      if (backing.canLog()) {
        backing.log(dbgId() + "remote-save replace session " + cb);
      }

      synchronized (session) {
        session.unbind();
        session.update();
      }
    }

    synchronized (backing) {
      WriteStream os = backing.openWrite();
      if (backing.canLog())
        backing.log(dbgId() + "save session: " + cb + " " + os.getPath());

      try {
        while (_rawRead.readAll(_lengthBuf, 0, 4) == 4) {
          int code = _lengthBuf[0] & 0xff;
          int len = (((_lengthBuf[1] & 0xff) << 16) +
                     ((_lengthBuf[2] & 0xff) << 8) +
                     ((_lengthBuf[3] & 0xff)));

          switch (code) {
          case CSE_SESSION_DATA:
            os.writeStream(_rawRead, len);
            break;

          case CSE_CLOSE:
            return false;

          case CSE_END:
            _lengthBuf[0] = (byte) CSE_END;
            _lengthBuf[1] = (byte) 0;
            _lengthBuf[2] = (byte) 0;
            _lengthBuf[3] = (byte) 0;
            _rawWrite.write(_lengthBuf, 0, 4);
            return true;
          
          default:
            return false;
          }
        }
      } finally {
        os.close();
      }
    }

    return false;
    */
  }

  /**
   * Returns the serialized session based on a foreign request.
   */
  private boolean loadSession(CharBuffer cb)
    throws IOException
  {
    int p = cb.lastIndexOf(';');

    if (p < 0)
      return false;

    String distributionId = cb.substring(0, p);
    String objectId = cb.substring(p + 1);
    
    ObjectBacking backing;

    if (_backingManager == null)
      return false;

    backing = _backingManager.getBacking(distributionId, objectId);
    if (backing == null)
      return false;

    ReadStream is = null;

    try {
      synchronized (backing) {
        try {
          is = backing.openRead();
        } catch (IOException e) {
          if (backing.canLog()) {
            backing.log(dbgId() + "load no session: " + backing.getPath());
          }
      
          _lengthBuf[0] = (byte) CSE_END;
          _lengthBuf[1] = 0;
          _lengthBuf[2] = 0;
          _lengthBuf[3] = 0;
          _rawWrite.write(_lengthBuf, 0, 4);

          return true;
        }
    
        TempBuffer tb = TempBuffer.allocate();

        try {
          byte []buffer = tb.getBuffer();

          int len;
          while ((len = is.read(buffer, 0, buffer.length)) > 0) {
            _lengthBuf[0] = (byte) CSE_SESSION_DATA;
            _lengthBuf[1] = (byte) (len >> 16);
            _lengthBuf[2] = (byte) (len >> 8);
            _lengthBuf[3] = (byte) len;

            _rawWrite.write(_lengthBuf, 0, 4);
            _rawWrite.write(buffer, 0, len);
          }
      
          if (backing.canLog()) {
            backing.log(dbgId() + 
                        "load session: " + cb.toString() + " " + backing.getPath());
          }
      
          return true;
        } finally {
          _lengthBuf[0] = (byte) CSE_END;
          _lengthBuf[1] = 0;
          _lengthBuf[2] = 0;
          _lengthBuf[3] = 0;
          _rawWrite.write(_lengthBuf, 0, 4);

          TempBuffer.free(tb);
        }
      }
    } finally {
      if (is != null)
        is.close();
    }
  }

  /**
   * Returns the serialized session based on a foreign request.
   */
  private boolean dumpSession(CharBuffer cb)
    throws IOException
  {
    String runId = cb.toString();

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "session dump " + runId + " for " + _srunIndex);
    /*
    SessionManager manager = (SessionManager) _backingManager.getContext(runId);

    if (manager == null)
      return false;

    int callerSrunIndex = _srunIndex;

    Logger log = manager.getDebug();
    
    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();

    Iterator iter = manager.getSessionPaths(callerSrunIndex);
    while (iter.hasNext()) {
      Path sessionPath = (Path) iter.next();
      ReadStream is = null;

      try {
        is = sessionPath.openRead();
      } catch (IOException e) {
        continue;
      }

      try {
        String sessionId = sessionPath.getTail();

        writeString(CSE_DUMP_SESSION, sessionId);
        
        int len;
        while ((len = is.read(buffer, 0, buffer.length)) > 0) {
          _lengthBuf[0] = (byte) CSE_SESSION_DATA;
          _lengthBuf[1] = (byte) (len >> 16);
          _lengthBuf[2] = (byte) (len >> 8);
          _lengthBuf[3] = (byte) len;

          _rawWrite.write(_lengthBuf, 0, 4);
          _rawWrite.write(buffer, 0, len);
        }
      
        if (log.isLoggable(Level.FINE)) {
          log.fine(dbgId() + cb.toString() + " " + sessionPath);
        }
      } finally {
        is.close();
      }
    }
    
    TempBuffer.free(tb);
    */

    _lengthBuf[0] = (byte) CSE_END;
    _lengthBuf[1] = 0;
    _lengthBuf[2] = 0;
    _lengthBuf[3] = 0;
    _rawWrite.write(_lengthBuf, 0, 4);

    return true;
  }

  /**
   * Remove the named session from the store in response to an invalidate
   * request.
   *
   * @param cb the name of the session.
   */
  private boolean killSession(CharBuffer cb)
    throws IOException
  {
    /*
    int p = cb.lastIndexOf(';');

    if (p < 0)
      return false;

    String distributionId = cb.substring(0, p);
    String objectId = cb.substring(p + 1);
    
    ObjectBacking backing;
    
    if (_backingManager == null)
      return false;

    backing = _backingManager.getBacking(distributionId, objectId);
    if (backing == null)
      return false;

    try {
      SessionImpl session = (SessionImpl) backing.getObject();
      
      if (session != null) {
        synchronized (session) {
          if (session.canLog()) {
            session.log(dbgId() + "remote invalidate session " + cb);
          }

          session.getManager().removeSession(session);
          session.invalidateLocal();
        }
      }
    } catch (Exception e) {
    }

    backing.remove();
    */
    
    _lengthBuf[0] = (byte) CSE_END;
    _lengthBuf[1] = 0;
    _lengthBuf[2] = 0;
    _lengthBuf[3] = 0;
    _rawWrite.write(_lengthBuf, 0, 4);

    return true;
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
    return ((_rawRead.read() << 16) +
	    (_rawRead.read() << 8) +
	    _rawRead.read());
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

  public byte []getUriBuffer()
  {
    return _uri.getBuffer();
  }

  public int getUriLength()
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

  int getVersion()
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
    if (_headerSize >= _headerKeys.length)
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

  public String getRemoteHost()
  {
    return _remoteHost.toString();
  }

  // Response data
  void writeStatus(CharBuffer message)
    throws IOException
  {
    writeString(CSE_STATUS, message);
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
    writeString(CSE_HEADER, key); 
    writeString(CSE_VALUE, value); 
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
    writeString(CSE_HEADER, key); 
    writeString(CSE_VALUE, value); 
  }

  void writeString(int code, String value)
    throws IOException
  {
    int len = value.length();
    
    _lengthBuf[0] = (byte) code;
    _lengthBuf[1] = (byte) (len >> 16);
    _lengthBuf[2] = (byte) (len >> 8);
    _lengthBuf[3] = (byte) len;
    
    _rawWrite.write(_lengthBuf, 0, 4);
    _rawWrite.print(value);
    
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + " " + value);
  }

  void writeString(int code, CharBuffer cb)
    throws IOException
  {
    int len = cb.length();
    
    _lengthBuf[0] = (byte) code;
    _lengthBuf[1] = (byte) (len >> 16);
    _lengthBuf[2] = (byte) (len >> 8);
    _lengthBuf[3] = (byte) len;
    
    _rawWrite.write(_lengthBuf, 0, 4);
    _rawWrite.print(cb.getBuffer(), 0, len);

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + " " + cb);
  }
  
  /**
   * Cleans up at the end of the request
   */
  public void finish()
    throws IOException
  {
    super.finish();

    ReadStream stream = getStream();
    while ((stream.skip(8192) > 0)) {
    }
  }
    
  String dbgId()
  {
    return "[" + _server.getServerId() + ", " + getConnection().getId() + "] ";
  }

  /**
   * Implements the protocol for data reads and writes.  Data from the
   * web server to the JVM must be acked, except for the first data.
   * Data back to the web server needs no ack.
   */
  static class ServletFilter extends StreamImpl {
    RunnerRequest _request;
    ReadStream _nextRead;
    WriteStream _nextWrite;
    byte []_buffer = new byte[16];
    int _pendingData;
    boolean _isFirst = true;
    boolean _isClosed;
    boolean _isClientClosed;

    ServletFilter()
    {
    }

    void init(RunnerRequest request,
	      ReadStream nextRead, WriteStream nextWrite)
    {
      _request = request;
      _nextRead = nextRead;
      _nextWrite = nextWrite;
      _pendingData = 0;
      _isClosed = false;
      _isClientClosed = false;
      _isFirst = true;
    }

    void setPending(int pendingData)
    {
      _pendingData = pendingData;
    }

    void setClientClosed(boolean isClientClosed)
    {
      _isClientClosed = isClientClosed;
    }

    public boolean canRead() { return true; }

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
      if (_pendingData <= 0)
	return -1;

      int sublen = _pendingData;
      if (length < sublen)
	sublen = length;

      int readLen = _nextRead.read(buf, offset, sublen);
      _pendingData -= readLen;

      while (_pendingData == 0) {
        if (! _isFirst) {
          _buffer[0] = CSE_ACK;
          _buffer[1] = 0;
          _buffer[2] = 0;
          _buffer[3] = 0;
          _nextWrite.write(_buffer, 0, 4);
          
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "ack");
        }
        _isFirst = false;

	int code = _nextRead.read();
	int len = ((_nextRead.read() << 16) +
		   (_nextRead.read() << 8) +
		   _nextRead.read());

	if (code == CSE_DATA) {
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "D " + len);
          
	  _pendingData = len;
        }
	else {
	  // XXX: requests?
	  return readLen;
	}
      }

      return readLen;
    }

    public boolean canWrite() { return true; }

    /**
     * Send data back to the web server
     */
    public void write(byte []buf, int offset, int length, boolean isEnd)
      throws IOException
    {
      if (log.isLoggable(Level.FINE))
	log.fine(_request.dbgId() + "d " + length);

      byte []tempBuf = _buffer;
      tempBuf[0] = CSE_DATA;
      tempBuf[1] = (byte) (length >> 16);
      tempBuf[2] = (byte) (length >> 8);
      tempBuf[3] = (byte) length;

      _nextWrite.write(tempBuf, 0, 4);

      _nextWrite.write(buf, offset, length);
    }

    public void flush()
      throws IOException
    {
      if (log.isLoggable(Level.FINE))
	log.fine(_request.dbgId() + "F flush");

      _buffer[0] = CSE_FLUSH;
      _buffer[1] = 0;
      _buffer[2] = 0;
      _buffer[3] = 0;

      _nextWrite.write(_buffer, 0, 4);
      _nextWrite.flush();
    }

    public void close()
      throws IOException
    {
      if (_isClosed)
	return;

      _isClosed = true;

      if (_pendingData > 0) {
	_nextRead.skip(_pendingData);
	_pendingData = 0;
      }

      if (! _isClientClosed) {
	_buffer[1] = 0;
	_buffer[2] = 0;
	_buffer[3] = 0;

        /*
	if (! request.keepalive) // XXX: || ! request.conn.allocateKeepalive())
	  request.killKeepalive = false;
        */

	if (log.isLoggable(Level.FINE))
	  log.fine(_request.dbgId() + (_request.allowKeepalive() ? "Z" : "X"));

	_buffer[0] = (byte) (_request.allowKeepalive() ? CSE_END : CSE_CLOSE);
	_nextWrite.write(_buffer, 0, 4);
      }

      if (_request.allowKeepalive())
        _nextWrite.flush();
      else
        _nextWrite.close();
      //nextRead.close();
    }
  }
}

