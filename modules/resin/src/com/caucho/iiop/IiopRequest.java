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

package com.caucho.iiop;

import java.io.*;
import java.net.*;
import java.util.logging.*;

import com.caucho.vfs.*;

import com.caucho.server.port.ServerRequest;
import com.caucho.server.connection.Connection;

/**
 * Protocol specific information for each request.  ServerRequest
 * is reused to reduce memory allocations.
 *
 * <p>ServerRequests are created by Server.createRequest()
 */
public class IiopRequest implements ServerRequest {
  private static final Logger log =
    Logger.getLogger(IiopRequest.class.getName());

  Connection _conn;
  IiopProtocol _server;
  IiopReader _reader;

  ClassLoader _loader;

  StreamMessageWriter _messageWriter;
  IiopWriter _writer10;
  IiopWriter _writer11;
  IiopWriter _writer12;

  ReadStream _readStream;
  WriteStream _writeStream;

  CosServer _cos;
  IiopSkeleton _cosSkel;

  String _hostName;
  int _port;
  
  IiopRequest(IiopProtocol server, Connection conn)
  {
    _server = server;
    _conn = conn;

    _reader = new IiopReader();

    _messageWriter = new StreamMessageWriter();
    
    _writer10 = new Iiop10Writer();
    _writer10.init(_messageWriter);
    _writer11 = new Iiop11Writer();
    _writer11.init(_messageWriter);
    _writer12 = new Iiop12Writer();
    _writer12.init(_messageWriter);
    
    _cos = _server.getCos();

    _loader = Thread.currentThread().getContextClassLoader();
  }
  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  public boolean isWaitForRead()
  {
    return true;
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   *
   * @param conn Information about the connection, including buffered
   * read and write streams.
   */
  public boolean handleRequest() throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);
    
      log.finer("IIOP[" + _conn.getId() + "]: handle request");
    
      _readStream = _conn.getReadStream();
      _writeStream = _conn.getWriteStream();

      if (_cosSkel != null) {
      }
      else {
	InetAddress local = _conn.getLocalAddress();
	_hostName = local.getHostName();
	_port = _conn.getLocalPort();

	_cos.setHost(_hostName);
	_cos.setPort(_port);

	_cosSkel = new IiopSkeleton(_cos, _cos.getClass(), _loader,
				    _hostName, _port, "/NameService");
      }

      int ch = _readStream.read();

      if (ch < 0) {
	log.finer("IIOP[" + _conn.getId() + "]: end of stream");
	return false;
      }

      _readStream.unread();

      _reader.init(_readStream);

      _messageWriter.init(_writeStream);
      IiopWriter writer = _writer10;

      _reader.readRequest();

      switch (_reader.getMinorVersion()) {
      case 0:
	writer = _writer10;
	break;
      case 1:
	writer = _writer11;
	break;
      case 2:
	writer = _writer12;
	break;
      default:
	writer = _writer10;
	break;
      }

      writer.setHost(_hostName);
      writer.setPort(_port);

      String oid = _reader.getObjectKey().toString();

      if (log.isLoggable(Level.FINER))
	log.finer("IIOP[" + _conn.getId() + "] OID: " + oid);

      try {
	if (oid.equals("INIT")) {
	  String str = _reader.readString();

	  writer.startReplyOk(_reader.getRequestId());

	  if (str.equals("NameService")) {
	    String nameService = "IDL:omg.org/CosNaming/NamingContext:1.0";

	    IOR ior = new IOR(nameService, _hostName, _port, "/NameService");
	    byte []bytes = ior.getByteArray();
	    writer.write(bytes, 0, bytes.length);
	  }
	  else
	    writer.writeNullIOR();
	}
	else if (oid.equals("/NameService")) {
	  /*
	    cos.service(reader, writer);
	  */
	  _cosSkel.service(_cosSkel.getObject(), _reader, writer);
	}
	else {
	  IiopSkeleton skel = _server.getService(_hostName, _port, oid);

	  if (skel != null) {
	    skel.service(skel.getObject(), _reader, writer);
	  }
	  else {
	    log.fine("IIOP[" + _conn.getId() + "] can't find service: " + oid);

	    throw new IOException("bad oid: " + oid);
	  }

	  log.fine("IIOP[" + _conn.getId() + "] complete request");
	}
      } catch (org.omg.CORBA.SystemException e) {
	e.printStackTrace();
	
	log.log(Level.WARNING, e.toString(), e);
      
	writer.startReplySystemException(_reader.getRequestId(),
					 e.toString(),
					 e.minor,
					 e.completed.value());
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);

	// ejb/1110 vs ejb/114p
	//writer.startReplyUserException(_reader.getRequestId(), e.toString());
	// ejb/1110

	writer.startReplyUserException(_reader.getRequestId());
	writer.write_string(e.toString());

	/*
	  writer.write_value(e);

	  MarshallObject marshall = MarshallObject.create(e.getClass(), false);
	  marshall.marshall(e, writer);
	*/
      }
      
      _messageWriter.close();

      _reader.completeRead();

      if (log.isLoggable(Level.FINER))
	log.finer("IIOP[" + _conn.getId() + "]: recycle");

      return true;
    } catch (Throwable e) {
      e.printStackTrace();
      log.log(Level.WARNING, "IIOP[" + _conn.getId() + "] " + e.toString(), e);
      return false;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void protocolCloseEvent()
  {
  }

}
