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

package javax.mail;

/**
 * Represents a mail service
 */
public abstract class Service {
  protected boolean debug;
  protected Session session;
  protected URLName url;

  private boolean _isConnected;

  protected Service(Session session, URLName urlname)
  {
    this.session = session;
    this.url = urlname;
    this.debug = session.getDebug();
  }

  /**
   * Connect to the service.
   */
  public void connect()
    throws MessagingException
  {
    connect(null, -1, null, null);
  }

  /**
   * Connect to the service.
   */
  public void connect(String host, String user, String password)
    throws MessagingException
  {
    connect(host, -1, user, password);
  }

  /**
   * Connect to the service.
   */
  public void connect(String host, int port, String user, String password)
    throws MessagingException
  {
    if (host == null)
      host = this.session.getProperty("mail.smtp.host");

    if (host == null)
      host = this.session.getProperty("mail.host");

    if (port <= 0) {
      String portName = this.session.getProperty("mail.smtp.port");

      if (portName != null)
	port = Integer.parseInt(portName);
    }

    if (port <= 0) {
      String portName = this.session.getProperty("mail.port");

      if (portName != null)
	port = Integer.parseInt(portName);
    }

    protocolConnect(host, port, user, password);
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
    // XXX:
    
    setConnected(true);

    return true;
  }

  /**
   * Return true if connected.
   */
  public boolean isConnected()
  {
    return _isConnected;
  }

  /**
   * Set if connected.
   */
  protected void setConnected(boolean connected)
  {
    _isConnected = connected;
  }

  /**
   * Close connection.
   */
  public void close()
    throws MessagingException
  {
    setConnected(false);
  }
}
