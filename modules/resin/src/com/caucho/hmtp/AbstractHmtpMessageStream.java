/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.hmtp;

import java.io.Serializable;
import java.util.logging.*;

/**
 * HmtpMessageStream is the HMTP stream interface for unidirectional messages.
 * 
 * Messages in HMTP consist of a target JID (to), a source JID (from), and
 * a payload (value).
 * 
 * The payload is typed according to the application, so an IM application
 * might use a payload called ImMessage, while a game might have MoveMessage,
 * FireLaserMessage, etc.
 */
public class AbstractHmtpMessageStream implements HmtpMessageStream {
  private static final Logger log
    = Logger.getLogger(AbstractHmtpMessageStream.class.getName());
  
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendMessage to=" + to + " from=" + from
		+ " value=" + value);
    }
  }
  
  /**
   * Handlers an error message
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   * @param error the message error
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendMessageError to=" + to + " from=" + from
		+ " error=" + error);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
