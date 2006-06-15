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
import javax.mail.event.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a mail transport, e.g. smtp
 */
public abstract class Transport extends Service {

  private ArrayList _listeners = new ArrayList();

  private BlockingQueue _blockingQueue = new LinkedBlockingQueue();

  private TransportDispatcherThread _transportDispatcherThread =
    new TransportDispatcherThread();

  protected Transport(Session session, URLName urlname)
  {
    super(session, urlname);
    _transportDispatcherThread.start();
  }

  /**
   * Sends a message to the specified recipients.
   */
  public abstract void sendMessage(Message msg,
				   Address []addresses)
    throws MessagingException;

  /**
   * Add a listener for Transport events.
   */
  public void addTransportListener(TransportListener l)
  {
    synchronized(_listeners)
      {
	_listeners.add(l);
      }
  }

  /**
   * Notify all TransportListeners.
   */
  protected void notifyTransportListeners(int type,
					  Address[] validSent,
					  Address[] validUnsent,
					  Address[] invalid,
					  Message msg){
    TransportEvent te =
      new TransportEvent(this, type, validSent, validUnsent, invalid, msg);
    _blockingQueue.add(te);
  }

  /**
   * Remove a listener for Transport events.
   */
  public void removeTransportListener(TransportListener l)
  {
    synchronized(_listeners)
      {
	_listeners.remove(l);
      }
  }

  /**
   * Send a message.
   */
  public static void send(Message msg) throws MessagingException
  {
    send(msg, msg.getAllRecipients());
  }

  /**
   * Send the message to the specified addresses, ignoring any
   * recipients specified in the message itself.
   */
  public static void send(Message msg, Address[] addresses)
    throws MessagingException
  {
    msg.saveChanges();
    
    // XXX: use Session.getTransport() here
  }


  private class TransportDispatcherThread extends Thread {
    public void run()
    {
      while(true)
	{
	  TransportEvent te =
	    (TransportEvent)_blockingQueue.remove();
	  
	  TransportListener[] listeners = null;
	  synchronized(_listeners)
	    {
	      listeners = new TransportListener[_listeners.size()];
	      _listeners.toArray(listeners);
	    }
	  
	  for(int i = 0; i < listeners.length; i++)
	    te.dispatch(listeners[i]);
	}
    }
  }

}
