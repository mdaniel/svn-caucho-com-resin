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
import java.util.Vector;
import javax.mail.event.*;

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

  /**
   * Add a listener for Connection events on this service.  The
   * default implementation provided here adds this listener to an
   * internal list of ConnectionListeners.
   */
  public void addConnectionListener(ConnectionListener l){
    throw new UnsupportedOperationException("not implemented");
  }
  

  /**
   * Stop the event dispatcher thread so the queue can be garbage
   * collected.
   */
  protected void finalize() throws Throwable
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return a URLName representing this service. The returned
   * URLName does not include the password field.  Subclasses should
   * only override this method if their URLName does not follow the
   * standard format.  The implementation in the Service class
   * returns (usually a copy of) the url field with the password and
   * file information stripped out.
   */
  public URLName getURLName(){
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Notify all ConnectionListeners. Service implementations are
   * expected to use this method to broadcast connection events.
   * The provided default implementation queues the event into an
   * internal event queue. An event dispatcher thread dequeues
   * events from the queue and dispatches them to the registered
   * ConnectionListeners. Note that the event dispatching occurs in
   * a separate thread, thus avoiding potential deadlock problems.
   */
  protected void notifyConnectionListeners(int type){
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Add the event and vector of listeners to the queue to be delivered.
   */
  protected void queueEvent(MailEvent event, Vector vector)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Remove a Connection event listener.  The default implementation
   * provided here removes this listener from the internal list of
   * ConnectionListeners.
   */
  public void removeConnectionListener(ConnectionListener l)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the URLName representing this service. Normally used to
   * update the url field after a service has successfully connected.
   * Subclasses should only override this method if their URL does not
   * follow the standard format. In particular, subclasses should
   * override this method if their URL does not require all the
   * possible fields supported by URLName; a new URLName should be
   * constructed with any unneeded fields removed.  The implementation
   * in the Service class simply sets the url field.
   */
  protected void setURLName(javax.mail.URLName url)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return getURLName.toString() if this service has a URLName,
   * otherwise it will return the default toString.
   */
  public String toString()
  {
    if (getURLName()!=null)
      return getURLName().toString();

    return super.toString();
  }
}
