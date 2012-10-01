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
import java.util.Map;


/**
 * AMQP link flow
 */
public class FrameFlow extends AmqpAbstractFrame {
  private long _nextIncomingId; // uint seq (RFC1892)
  private int _incomingWindow; // uint (required)
  private long _nextOutgoingId; // uint seq (required) (RFC1892)
  private int _outgoingWindow; // uint (required)
  private int _handle;         // uint
  private long _deliveryCount; // uint seq
  private int _linkCredit;     // uint
  private int _available;      // uint
  private boolean _isDrain;
  private boolean _isEcho;
  private Map<String,?> _properties;
  
  public long getNextIncomingId()
  {
    return _nextIncomingId;
  }
  
  public int getIncomingWindow()
  {
    return _incomingWindow;
  }
  
  public long getNextOutgoingId()
  {
    return _nextOutgoingId;
  }
  
  public int getOutgoingWindow()
  {
    return _outgoingWindow;
  }
  
  public int getHandle()
  {
    return _handle;
  }
  
  public void setHandle(int handle)
  {
    _handle = handle;
  }
  
  public void setDeliveryCount(long deliveryCount)
  {
    _deliveryCount = deliveryCount;
  }
  
  public long getDeliveryCount()
  {
    return _deliveryCount;
  }
  
  public int getLinkCredit()
  {
    return _linkCredit;
  }
  
  public void setLinkCredit(int linkCredit)
  {
    _linkCredit = linkCredit;
  }
  
  public int getAvailable()
  {
    return _available;
  }
  
  public void setAvailable(int available)
  {
    _available = available;
  }
  
  public boolean isDrain()
  {
    return _isDrain;
  }
  
  public boolean isEcho()
  {
    return _isEcho;
  }
  
  public Map<String,?> getProperties()
  {
    return _properties;
  }
  
  @Override
  public long getDescriptorCode()
  {
    return FT_LINK_FLOW;
  }
  
  
  @Override
  public FrameFlow createInstance()
  {
    return new FrameFlow();
  }
  
  @Override
  public void invoke(AmqpReader ain, AmqpFrameHandler receiver)
    throws IOException
  {
    receiver.onFlow(this);
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _nextIncomingId = in.readInt();
    _incomingWindow = in.readInt();
    _nextOutgoingId = in.readInt();
    _outgoingWindow = in.readInt();
    
    _handle = in.readInt();
    
    _deliveryCount = in.readInt();
    if (in.isNull()) {
      _deliveryCount = -1;
    }
    
    _linkCredit = in.readInt();
    _available = in.readInt();
    
    _isDrain = in.readBoolean();
    _isEcho = in.readBoolean();
    
    _properties = in.readFieldMap();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeUint((int) _nextIncomingId);
    out.writeUint(_incomingWindow);
    out.writeUint((int) _nextOutgoingId);
    out.writeUint(_outgoingWindow);
    
    out.writeUint(_handle);
    
    if (_deliveryCount >= 0) {
      out.writeUint((int) _deliveryCount);
    }
    else {
      out.writeNull();
    }
    
    out.writeUint(_linkCredit);
    out.writeUint(_available);
    
    out.writeBoolean(_isDrain);
    out.writeBoolean(_isEcho);
    
    out.writeMap(_properties);
    
    return 11;
  }
}
