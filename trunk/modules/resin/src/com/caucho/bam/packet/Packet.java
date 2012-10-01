/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.bam.packet;

import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.stream.MessageStream;

/**
 * Base packet class.  Contains only a 'to' and a 'from' field.
 */
public class Packet
{
  private static final Logger log
    = Logger.getLogger(Packet.class.getName());

  private final String _to;
  private final String _from;

  /**
   * null constructor for Hessian deserialization
   */
  public Packet()
  {
    _to = null;
    _from = null;
  }

  /**
   * Creates a packet with a destination and a source.
   *
   * @param to the destination address
   * @param from the source address
   */
  public Packet(String to, String from)
  {
    _to = to;
    _from = from;
  }

  /**
   * Returns the 'to' field
   */
  public final String getTo()
  {
    return _to;
  }

  /**
   * Returns the 'from' field
   */
  public final String getFrom()
  {
    return _from;
  }

  /**
   * SPI method to dispatch the packet to the proper handler
   */
  public void dispatch(MessageStream handler, MessageStream toSource)
  {
  }

  /**
   * SPI method to dispatch the packet to the proper handler
   */
  public void dispatchError(MessageStream handler,
                            MessageStream toSource,
                            BamError error)
  {
    log.fine(this + " dispatchError " + error);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[to=");
    sb.append(_to);

    if (_from != null) {
      sb.append(",from=");
      sb.append(_from);
    }

    sb.append("]");

    return sb.toString();
  }
}
