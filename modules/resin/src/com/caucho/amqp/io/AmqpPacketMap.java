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
import java.util.HashMap;

import com.caucho.util.L10N;

/**
 * an abstract amqp custom typed data
 */
public final class AmqpPacketMap {
  private static final HashMap<Long,AmqpAbstractPacket> _typeMap
    = new HashMap<Long,AmqpAbstractPacket>();
  
  static AmqpAbstractPacket getPacket(long descriptor)
  {
    return _typeMap.get(descriptor);
    
  }
  
  private static void addType(AmqpAbstractPacket factory)
  {
    _typeMap.put(factory.getDescriptorCode(), factory);
  }
  
  static {
    //System.out.println("INIT_PACKET: test");
    addType(new FrameOpen());
    addType(new FrameBegin());
    addType(new FrameAttach());
    addType(new FrameFlow());
    addType(new FrameTransfer());
    addType(new FrameDisposition());
    addType(new FrameDetach());
    addType(new FrameEnd());
    addType(new FrameClose());

    addType(new LinkSource());
    addType(new LinkTarget());

    addType(new DeliveryAccepted());
    addType(new DeliveryRejected());
    addType(new DeliveryReleased());
    addType(new DeliveryModified());

    addType(new MessageHeader());
    addType(new MessageDeliveryAnnotations());
    addType(new MessageAnnotations());
    addType(new MessageProperties());
    addType(new MessageAppProperties());
    addType(new MessageFooter());

    addType(new AmqpError());
  }
}
