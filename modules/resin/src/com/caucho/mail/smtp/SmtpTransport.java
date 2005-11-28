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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.mail.smtp;

import java.net.InetAddress;
import java.net.Socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.MessagingException;

import com.caucho.util.L10N;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.SocketStream;

/**
 * Resin's SMTP transport implementation.
 */
public class SmtpTransport extends Transport {
  private static final L10N L = new L10N(SmtpTransport.class);
  private static final Logger log
    = Logger.getLogger(SmtpTransport.class.getName());

  private Socket _socket;

  private ReadStream _is;
  private WriteStream _os;
  
  public SmtpTransport(Session session, URLName urlName)
  {
    super(session, urlName);
  }
  /**
   * Connect for the protocol.
   */
  protected boolean protocolConnect(String host,
				    int port,
				    String user,
				    String password)
    throws MessagingException
  {
    if (host == null)
      throw new MessagingException(L.l("Unknown mail host in connect."));

    if (port < 0)
      port = 25;

    // XXX: pooling
    if (_socket != null)
      throw new MessagingException(L.l("Attempted to connect to open connection."));

    try {
      _socket = new Socket(host, port);
      _socket.setSoTimeout(10000);
      SocketStream s = new SocketStream(_socket);
    
      _is = new ReadStream(s);
      _os = new WriteStream(s);

      String line = _is.readLine();
      
      log.fine("smtp connection to " + host + ":" + port + " succeeded");

      setConnected(true);
    } catch (IOException e) {
      log.fine("smtp connection to " + host + ":" + port + " failed: " + e);

      log.log(Level.FINER, e.toString(), e);
      
      throw new MessagingException("smtp connection to " + host + ":" + port + " failed.\n" + e);
    }

    return true;
  }

  /**
   * Sends a message to the specified recipients.
   *
   * @param msg the message to send
   * @param addresses the destination addresses
   */
  public void sendMessage(Message msg, Address []addresses)
    throws MessagingException
  {
    if (! isConnected())
      throw new MessagingException("Transport does not have an active connection.");
  }

  /**
   * Close connection.
   */
  public void close()
    throws MessagingException
  {
    Socket socket = _socket;
    _socket = null;
    
    setConnected(false);

    try {
      if (socket != null)
	socket.close();
    } catch (IOException e) {
    }
  }
}
