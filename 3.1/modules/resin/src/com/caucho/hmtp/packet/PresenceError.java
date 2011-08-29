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

package com.caucho.hmtp.packet;

import com.caucho.hmtp.HmtpStream;
import com.caucho.hmtp.packet.Presence;
import com.caucho.hmtp.HmtpError;
import java.io.Serializable;

/**
 * PresenceError returns an error response to a presence packet
 */
public class PresenceError extends Presence {
  private final HmtpError _error;
  
  /**
   * zero-arg constructor for Hessian
   */
  private PresenceError()
  {
    _error = null;
  }

  /**
   * The subscribed response to the original client
   *
   * @param to the target client
   * @param from the source
   * @param data a collection of presence data
   * @param error the error information
   */
  public PresenceError(String to,
		       String from,
		       Serializable []data,
		       HmtpError error)
  {
    super(to, from, data);

    _error = error;
  }

  /**
   * Returns the error information
   */
  public HmtpError getError()
  {
    return _error;
  }

  /**
   * SPI method to dispatch the packet to the proper handler
   */
  @Override
  public void dispatch(HmtpStream handler, HmtpStream toSource)
  {
    handler.sendPresenceError(getTo(), getFrom(), getData(), getError());
  }
}
