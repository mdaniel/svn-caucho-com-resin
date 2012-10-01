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
 * AMQP delivery accepted
 */
public class DeliveryReleased extends DeliveryState {
  public static final DeliveryReleased VALUE = new DeliveryReleased();

  /**
   * Called on a disposition update.
   */
  @Override
  public void update(long xid, DeliveryNode node)
  {
    node.onReleased(xid);
  }

  //
  // i/o methods
  //
  
  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_RELEASED;
  }
  
  @Override
  public DeliveryReleased createInstance()
  {
    return VALUE;
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    return 0;
  }
}
