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

import javax.mail.Message;
import javax.mail.Folder;

/**
 * Represents a message count event.
 */
public class MessageCountEvent extends MailEvent {
  private static final int ADDED = 1;
  private static final int REMOVED = 2;
    
  protected int type;
  protected boolean removed;
  protected transient Message []msgs;

  public MessageCountEvent(Folder folder, int type, boolean removed,
			   Message []msgs)
  {
    super(folder);

    this.type = type;
    this.removed = removed;
    this.msgs = msgs;
  }

  /**
   * Returns the event type.
   */
  public int getType()
  {
    return this.type;
  }

  /**
   * Returns the messages
   */
  public Message []getMessages()
  {
    return this.msgs;
  }

  /**
   * Returns true for an expunge
   */
  public boolean isRemoved()
  {
    return this.removed;
  }

  /**
   * Dispatches the method.
   */
  public void dispatch(Object listenerObject)
  {
    MessageCountListener listener = (MessageCountListener) listenerObject;

    switch (this.type) {
    case ADDED:
      listener.messagesAdded(this);
      break;
    case REMOVED:
      listener.messagesRemoved(this);
      break;
    default:
      throw new UnsupportedOperationException(toString());
    }
  }

  public String toString()
  {
    switch (this.type) {
    case ADDED:
      return getClass().getName() + "[added]";
      
    case REMOVED:
      return getClass().getName() + "[removed]";

    default:
      return super.toString();
    }
  }
}
