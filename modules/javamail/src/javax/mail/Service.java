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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a mail service
 */
public abstract class Service {
  protected boolean debug;
  protected Session session;
  protected URLName url;

  private boolean _isConnected;

  private ArrayList _listeners = new ArrayList();
  private BlockingQueue _blockingQueue = new LinkedBlockingQueue();
  /* XXX
  private TransportDispatcherThread _transportDispatcherThread =
    new TransportDispatcherThread();
  */

  /** used to tell the dispatcher thread to quit */
  private static MailEvent SHUTDOWN =
    new MailEvent(null) {
      public void dispatch(Object o) { }
    };
  
  protected Service(Session session, URLName urlname)
  {
    this.session = session;
    this.url = urlname;
    this.debug = session.getDebug();
    //_transportDispatcherThread.start();
  }

  /**
   * Connect to the service.
   */
  public void connect()
    throws MessagingException
  {
    connect(null, null, null);
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
    return false;
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
   * Add a listener for Connection events on this service.
   */
  public void addConnectionListener(ConnectionListener l){
    _listeners.add(l);
  }
  
  /**
   * Stop the event dispatcher thread so the queue can be garbage
   * collected.
   */
  protected void finalize() throws Throwable
  {
    // XXX
    //_queue.add(SHUTDOWN);
  }

  /**
   * Return a URLName representing this service w/o username or passord.
   */
  public URLName getURLName(){
    return new URLName(url.getProtocol(),
		       url.getHost(),
		       url.getPort(),
		       url.getFile(),
		       null,
		       null);
  }

  /**
   * Notify all ConnectionListeners.
   */
  protected void notifyConnectionListeners(int type){
    _blockingQueue.add(new Integer(type));
  }

  /**
   * Add the event and vector of listeners to the queue to be delivered.
   */
  protected void queueEvent(MailEvent event, Vector vector)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Remove a Connection event listener.
   */
  public void removeConnectionListener(ConnectionListener l)
  {
    _listeners.remove(l);
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

  private class ConnectionDispatcherThread extends Thread {
    public void run()
    {
      while(true)
	{
	  ConnectionEvent te =
	    (ConnectionEvent)_blockingQueue.remove();
	  
	  ConnectionListener[] listeners = null;
	  synchronized(_listeners)
	    {
	      listeners = new ConnectionListener[_listeners.size()];
	      _listeners.toArray(listeners);
	    }
	  
	  for(int i = 0; i < listeners.length; i++)
	    te.dispatch(listeners[i]);
	}
    }
  }
}
