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

/**
 * The message transport header.
 */
public class MessageHeader extends AmqpAbstractComposite {
  private boolean _isDurable;
  private int _priority = -1;  // ubyte
  private long _ttl = -1; // milliseconds
  private boolean _isFirstAcquirer;
  private int _deliveryCount = -1; // uint
  
  public boolean isDurable()
  {
    return _isDurable;
  }

  public void setDurable(boolean durable)
  {
    _isDurable = durable;
  }
  
  public int getPriority()
  {
    return _priority;
  }
  
  public void setPriority(int priority)
  {
    _priority = priority;
  }

  public void setDeliveryCount(int deliveryCount)
  {
    _deliveryCount = deliveryCount;
  }

  public void setFirstAcquirer(boolean isFirstAcquirer)
  {
    _isFirstAcquirer = isFirstAcquirer;
  }

  public void setTimeToLive(long timeToLive)
  {
    _ttl = timeToLive;
  }
  
  public long getTimeToLive()
  {
    return _ttl;
  }
 
  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_HEADER;
  }
  
  @Override
  public MessageHeader createInstance()
  {
    return new MessageHeader();
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _isDurable = in.readBoolean();
    _priority = in.readInt();
    
    if (in.isNull()) {
      _priority = -1;
    }
    
    _ttl = in.readLong();
    if (in.isNull()) {
      _ttl = -1;
    }
    
    _isFirstAcquirer = in.readBoolean();
    _deliveryCount = in.readInt();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeBoolean(_isDurable);
    
    if (_priority >= 0)
      out.writeUbyte(_priority);
    else
      out.writeNull();

    if (_ttl >= 0)
      out.writeUint((int) _ttl);
    else
      out.writeNull();
    
    out.writeBoolean(_isFirstAcquirer);
    out.writeUint(_deliveryCount);
    
    return 5;
  }
}
