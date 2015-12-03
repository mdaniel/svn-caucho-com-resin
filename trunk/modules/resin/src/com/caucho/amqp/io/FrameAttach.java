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
import java.util.List;
import java.util.Map;


/**
 * AMQP link-attach frame 
 */
public class FrameAttach extends AmqpAbstractFrame {
  public static final int CODE = AmqpConstants.FT_LINK_ATTACH;
  
  private String _name; // (required)
  private int _handle; // uint (required)
  private Role _role = Role.RECEIVER; // required
  private SenderSettleMode _senderSettleMode  // ubyte
    = SenderSettleMode.MIXED;
  private ReceiverSettleMode _receiverSettleMode // ubyte
    = ReceiverSettleMode.FIRST;
  
  private LinkSource _source;
  private LinkTarget _target;
  
  private Map<?,?> _unsettled;
  
  private boolean _isIncompleteUnsettled;
  private long _initialDeliveryCount; // uint sequence (RFC1982)
  private long _maxMessageSize;       // ulong
  
  private List<String> _offeredCapabilities;
  private List<String> _desiredCapabilities;
  
  private Map<String,?> _properties;
  
  public String getName()
  {
    return _name;
  }
  
  public void setName(String name)
  {
    _name = name;
  }
  
  public int getHandle()
  {
    return _handle;
  }
  
  public void setHandle(int handle)
  {
    _handle = handle;
  }
  
  public Role getRole()
  {
    return _role;
  }
  
  public void setRole(Role role)
  {
    _role = role;
  }
  
  public void setSenderSettleMode(SenderSettleMode mode)
  {
    _senderSettleMode = mode;
  }
  
  public SenderSettleMode getSenderSettleMode()
  {
    return _senderSettleMode;
  }
  
  public void setReceiverSettleMode(ReceiverSettleMode mode)
  {
    _receiverSettleMode = mode;
  }
  
  public ReceiverSettleMode getReceiverSettleMode()
  {
    return _receiverSettleMode;
  }
  
  public LinkSource getSource()
  {
    return _source;
  }
  
  public void setSource(LinkSource source)
  {
    _source = source;
  }
  
  public LinkTarget getTarget()
  {
    return _target;
  }
  
  public void setTarget(LinkTarget target)
  {
    _target = target;
  }
  
  public Map<?,?> getUnsettled()
  {
    return _unsettled;
  }
  
  public boolean isIncompleteUnsettled()
  {
    return _isIncompleteUnsettled;
  }
  
  public long getInitialDeliveryCount()
  {
    return _initialDeliveryCount;
  }
  
  public void setInitialDeliveryCount(long deliveryCount)
  {
    _initialDeliveryCount = deliveryCount;
  }
  
  public long getMaxMessageSize()
  {
    return _maxMessageSize;
  }
  
  public List<String> getOfferedCapabilities()
  {
    return _offeredCapabilities;
  }
  
  public List<String> getDesiredCapabilities()
  {
    return _desiredCapabilities;
  }
  
  public void setProperties(Map<String,?> properties)
  {
    _properties = properties;
  }
  
  public Map<String,?> getProperties()
  {
    return _properties;
  }
  
  @Override
  public long getDescriptorCode()
  {
    return FT_LINK_ATTACH;
  }
  
  @Override
  public FrameAttach createInstance()
  {
    return new FrameAttach();
  }
  
  @Override
  public void invoke(AmqpReader ain, AmqpFrameHandler receiver)
    throws IOException
  {
    receiver.onAttach(this);
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeString(_name);
    out.writeUint(_handle);
    
    out.writeBoolean(_role == Role.RECEIVER);
    
    out.writeUbyte(_senderSettleMode.ordinal());
    out.writeUbyte(_receiverSettleMode.ordinal());
    
    if (_source != null) {
      _source.write(out);
    }
    else {
      out.writeNull();
    }
    
    if (_target != null) {
      _target.write(out);
    }
    else {
      out.writeNull();
    }
    
    out.writeMap(_unsettled);
    out.writeBoolean(_isIncompleteUnsettled);
    
    out.writeUint((int) _initialDeliveryCount);
    out.writeUlong(_maxMessageSize);
    
    out.writeSymbolArray(_offeredCapabilities);
    out.writeSymbolArray(_desiredCapabilities);
    out.writeFieldsMap(_properties);
    
    return 14;
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _name = in.readString();
    _handle = in.readInt();
    
    _role = Role.values()[in.readBoolean() ? 1 : 0];
    _senderSettleMode = SenderSettleMode.values()[in.readInt()];
    _receiverSettleMode = ReceiverSettleMode.values()[in.readInt()];
    
    _source = in.readObject(LinkSource.class);
    _target = in.readObject(LinkTarget.class);
    
    _unsettled = in.readMap();
    _isIncompleteUnsettled = in.readBoolean();
    _initialDeliveryCount = in.readInt();
    _maxMessageSize = in.readLong();
    
    _offeredCapabilities = in.readSymbolArray();
    _desiredCapabilities = in.readSymbolArray();
    _properties = in.readFieldMap();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handle + "," + _name + "]";
  }
  
  // boolean
  public enum Role {
    SENDER,   // false
    RECEIVER; // true
  }
  
  // ubyte
  public enum SenderSettleMode {
    UNSETTLED,
    SETTLED,
    MIXED;
  }
  
  public enum ReceiverSettleMode {
    FIRST,
    SECOND;
  }
}
