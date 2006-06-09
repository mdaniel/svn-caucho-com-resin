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

package javax.mail.event;
import javax.mail.*;

import javax.mail.Message;
import javax.mail.Address;
import javax.mail.Transport;

/**
 * Represents a transport event.
 */
public class TransportEvent extends MailEvent {
  private static final int MESSAGE_DELIVERED = 1;
  private static final int MESSAGE_NOT_DELIVERED = 2;
  private static final int MESSAGE_PARTIALLY_DELIVERED = 3;
    
  protected int type;
  
  protected transient Message msg;
  protected transient Address []invalid;
  protected transient Address []validSent;
  protected transient Address []validUnsent;

  public TransportEvent(Transport transport,
			int type,
			Address []validSent,
			Address []validUnsent,
			Address []invalid,
			Message msg)
  {
    super(transport);

    this.type = type;
    this.validSent = validSent;
    this.validUnsent = validUnsent;
    this.invalid = invalid;
    this.msg = msg;
  }

  /**
   * Returns the event type.
   */
  public int getType()
  {
    return this.type;
  }

  /**
   * Returns the valid sent messages
   */
  public Address []getValidSentAddresses()
  {
    return this.validSent;
  }

  /**
   * Returns the valid unsent messages
   */
  public Address []getValidUnsentAddresses()
  {
    return this.validUnsent;
  }

  /**
   * Returns the invalid messages
   */
  public Address []getInvalidAddresses()
  {
    return this.invalid;
  }

  /**
   * Returns the messages
   */
  public Message getMessage()
  {
    return this.msg;
  }

  /**
   * Dispatches the method.
   */
  public void dispatch(Object listenerObject)
  {
    TransportListener listener = (TransportListener) listenerObject;

    switch (this.type) {
    case MESSAGE_DELIVERED:
      listener.messageDelivered(this);
      break;
    case MESSAGE_NOT_DELIVERED:
      listener.messageNotDelivered(this);
      break;
    case MESSAGE_PARTIALLY_DELIVERED:
      listener.messagePartiallyDelivered(this);
      break;
    default:
      throw new UnsupportedOperationException(toString());
    }
  }

  public String toString()
  {
    switch (this.type) {
    case MESSAGE_DELIVERED:
      return getClass().getName() + "[delivered]";
      
    case MESSAGE_NOT_DELIVERED:
      return getClass().getName() + "[not_delivered]";
      
    case MESSAGE_PARTIALLY_DELIVERED:
      return getClass().getName() + "[partially_delivered]";

    default:
      return super.toString();
    }
  }
}
