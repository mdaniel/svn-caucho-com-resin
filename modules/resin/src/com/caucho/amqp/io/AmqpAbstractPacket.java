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

package com.caucho.amqp.io;

import java.io.IOException;

import com.caucho.util.L10N;

/**
 * an abstract amqp custom typed data
 */
abstract public class AmqpAbstractPacket implements AmqpConstants {
  private static final L10N L = new L10N(AmqpAbstractPacket.class);
  
  public void write(AmqpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void read(AmqpReader in)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void readValue(AmqpReader in)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected long getDescriptorCode()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public AmqpAbstractPacket createInstance()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  static <T extends AmqpAbstractPacket>
  T readType(AmqpReader in, long typeCode, Class<T> type)
    throws IOException
  {
    AmqpAbstractPacket factory = AmqpPacketMap.getPacket(typeCode);

    if (factory == null) {
      throw new IOException(L.l("0x{0} is an unknown type expected {1}",
                                Long.toHexString(typeCode), type.getName()));
    }
    
    AmqpAbstractPacket packet = factory.createInstance();
    
    if (! type.isAssignableFrom(packet.getClass())) {
      throw new ClassCastException(L.l("Cannot cast {0} to {1}",
                                       packet.getClass().getName(),
                                       type.getName()));
    }
    
    packet.readValue(in);
    
    return (T) packet;
  }
}
