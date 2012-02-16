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

package com.caucho.mqueue.amqp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AMQP link-attach frame 
 */
public class AmqpLinkAttach extends AmqpAbstractPacket {
  public static final int CODE = AmqpConstants.FT_LINK_ATTACH;
  
  public static final int SENDER_SETTLE_MODE_MIXED = 2;

  private String _name; // (required)
  private int _handle; // uint (required)
  private Role _role = Role.RECEIVER; // required
  private SenderSettleMode _senderSettleMode  // ubyte
    = SenderSettleMode.MIXED;
  private ReceiverSettleMode _receiverSettleMode // ubyte
    = ReceiverSettleMode.FIRST;
  
  private String _source;
  private String _target;
  
  private Map<?,?> _unsettled;
  
  private boolean _isIncompleteUnsettled;
  private long _initialDeliveryCount; // uint sequence (RFC1982)
  private long _maxMessageSize;       // ulong
  
  private List<String> _offeredCapabilities;
  private List<String> _desiredCapabilities;
  
  private Map<String,?> _properties;
  
  @Override
  public void write(AmqpWriter out)
    throws IOException
  {
    out.writeDescriptor(FT_LINK_ATTACH);
    
    out.writeString(_name);
    out.writeUint(_handle);
    
    out.writeBoolean(_role == Role.RECEIVER);
    
    out.writeUbyte(_senderSettleMode.ordinal());
    out.writeUbyte(_receiverSettleMode.ordinal());
    
    out.writeNull(); // source
    out.writeNull(); // target
    
    out.writeMap(_unsettled);
    out.writeBoolean(_isIncompleteUnsettled);
    
    out.writeUint((int) _initialDeliveryCount);
    out.writeUlong(_maxMessageSize);
    
    out.writeArray(_offeredCapabilities);
    out.writeArray(_desiredCapabilities);
    out.writeMap(_properties);
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
