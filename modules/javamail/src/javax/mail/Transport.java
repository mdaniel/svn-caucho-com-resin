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

/**
 * Represents a mail transport, e.g. smtp
 */
public abstract class Transport extends Service {
  protected Transport(Session session, URLName urlname)
  {
    super(session, urlname);
  }

  /**
   * Sends a message to the specified recipients.
   */
  public abstract void sendMessage(Message msg,
				   Address []addresses)
    throws MessagingException;

  /**
   * Add a listener for Transport events.  The default
   * implementation provided here adds this listener to an internal
   * list of TransportListeners.
   */
  public void addTransportListener(TransportListener l)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Notify all TransportListeners. Transport implementations are
   * expected to use this method to broadcast TransportEvents.  The
   * provided default implementation queues the event into an internal
   * event queue. An event dispatcher thread dequeues events from the
   * queue and dispatches them to the registered
   * TransportListeners. Note that the event dispatching occurs in a
   * separate thread, thus avoiding potential deadlock problems.
   */
  protected void notifyTransportListeners(int type,
					  Address[] validSent,
					  Address[] validUnsent,
					  Address[] invalid,
					  Message msg){
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Remove a listener for Transport events.  The default
   * implementation provided here removes this listener from the
   * internal list of TransportListeners.
   */
  public void removeTransportListener(TransportListener l)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Send a message. The message will be sent to all recipient
   * addresses specified in the message (as returned from the Message
   * method getAllRecipients), using message transports appropriate to
   * each address. The send method calls the saveChanges method on the
   * message before sending it.  If any of the recipient addresses is
   * detected to be invalid by the Transport during message
   * submission, a SendFailedException is thrown. Clients can get more
   * detail about the failure by examining the exception. Whether or
   * not the message is still sent succesfully to any valid addresses
   * depends on the Transport implementation. See SendFailedException
   * for more details. Note also that success does not imply that the
   * message was delivered to the ultimate recipient, as failures may
   * occur in later stages of delivery. Once a Transport accepts a
   * message for delivery to a recipient, failures that occur later
   * should be reported to the user via another mechanism, such as
   * returning the undeliverable message.
   */
  public static void send(Message msg) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Send the message to the specified addresses, ignoring any
   * recipients specified in the message itself. The send method calls
   * the saveChanges method on the message before sending it.
   */
  public static void send(Message msg, Address[] addresses)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
