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

import com.caucho.amqp.common.DeliveryNode;


/**
 * AMQP delivery received
 */
public class DeliveryReceived extends DeliveryState {
  private long _sectionNumber; // uint mandatory
  private long _sectionOffset; // ulong mandatory
  
  public long getSectionNumber()
  {
    return _sectionNumber;
  }
  
  public void setSectionNumber(long sectionNumber)
  {
    _sectionNumber = sectionNumber;
  }
  
  public long getSectionOffset()
  {
    return _sectionOffset;
  }
  
  public void setSectionOffset(long sectionOffset)
  {
    _sectionOffset = sectionOffset;
  }
  
  //
  // action methods
  //

  /**
   * Called on a disposition update.
   */
  @Override
  public void update(long xid, DeliveryNode node)
  {
    node.onReceived(xid);
  }
  
  //
  // i/o methods

  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_RECEIVED;
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _sectionNumber = in.readLong();
    _sectionOffset = in.readLong();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeUint((int) _sectionNumber);
    out.writeUlong(_sectionOffset);
    
    return 2;
  }
}
