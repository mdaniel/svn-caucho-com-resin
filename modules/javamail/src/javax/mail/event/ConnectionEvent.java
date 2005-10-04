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

/**
 * Represents a connection event.
 */
public class ConnectionEvent extends MailEvent {
  private static final int OPENED = 1;
  private static final int DISCONNECTED = 2;
  private static final int CLOSED = 3;
    
  protected int type;

  public ConnectionEvent(Object source, int type)
  {
    super(source);

    this.type = type;
  }

  /**
   * Returns the event type.
   */
  public int getType()
  {
    return this.type;
  }

  /**
   * Dispatches the method.
   */
  public void dispatch(Object listenerObject)
  {
    ConnectionListener listener = (ConnectionListener) listenerObject;

    switch (this.type) {
    case OPENED:
      listener.opened(this);
      break;
    case DISCONNECTED:
      listener.disconnected(this);
      break;
    case CLOSED:
      listener.closed(this);
      break;
    default:
      throw new UnsupportedOperationException(toString());
    }
  }

  public String toString()
  {
    switch (this.type) {
    case OPENED:
      return getClass().getName() + "[opened]";
      
    case DISCONNECTED:
      return getClass().getName() + "[disconnected]";
      
    case CLOSED:
      return getClass().getName() + "[closed]";

    default:
      return super.toString();
    }
  }
}
