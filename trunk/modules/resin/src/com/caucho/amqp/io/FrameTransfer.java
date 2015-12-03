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

import com.caucho.amqp.io.FrameAttach.ReceiverSettleMode;


/**
 * AMQP session-begin frame 
 */
public class FrameTransfer extends AmqpAbstractFrame {
  public static final int CODE = AmqpConstants.FT_MESSAGE_TRANSFER;

  private int _handle; // uint (required)
  private long _deliveryId; // uint seq (RFC1982)
  private byte []_deliveryTag; // up to 32 byte
  private int _messageFormat; // uint
  private boolean _isSettled;
  private boolean _isMore;
  private ReceiverSettleMode _receiverSettleMode
    = ReceiverSettleMode.FIRST;
  private DeliveryState _state; // delivery-state
  private boolean _isResume;
  private boolean _isAborted;
  private boolean _isBatchable;
  
  public int getHandle()
  {
    return _handle;
  }
  
  public void setHandle(int handle)
  {
    _handle = handle;
  }
  
  public long getDeliveryId()
  {
    return _deliveryId;
  }
  
  public void setDeliveryId(long deliveryId)
  {
    _deliveryId = deliveryId;
  }
  
  public byte []getDeliveryTag()
  {
    return _deliveryTag;
  }
  
  public int getMessageFormat()
  {
    return _messageFormat;
  }
  
  public boolean isSettled()
  {
    return _isSettled;
  }
  
  public void setSettled(boolean isSettled)
  {
    _isSettled = isSettled;
  }
  
  public boolean isMore()
  {
    return _isMore;
  }
  
  public ReceiverSettleMode getReceiverSettleMode()
  {
    return _receiverSettleMode;
  }
  
  public DeliveryState getDeliveryState()
  {
    return _state;
  }
  
  public boolean isResume()
  {
    return _isResume;
  }
  
  public boolean isAborted()
  {
    return _isAborted;
  }
  
  public boolean isBatchable()
  {
    return _isBatchable;
  }
  
  public long getDescriptorCode()
  {
    return FT_MESSAGE_TRANSFER;
  }
  
  @Override
  public FrameTransfer createInstance()
  {
    return new FrameTransfer();
  }
  
  @Override
  public void invoke(AmqpReader ain, AmqpFrameHandler handler)
    throws IOException
  {
    handler.onTransfer(ain, this);
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeUint(_handle);
    out.writeUint((int) _deliveryId);
    out.writeNull(); // delivery-tag (binary)
    
    out.writeUint(_messageFormat);
    
    out.writeBoolean(_isSettled);
    out.writeBoolean(_isMore);
    out.writeUbyte(_receiverSettleMode.ordinal());
    
    if (_state != null)
      _state.write(out);
    else
      out.writeNull();
    
    out.writeBoolean(_isResume);
    out.writeBoolean(_isAborted);
    out.writeBoolean(_isBatchable);
    
    return 11;
  }
 
  @Override
  public void readBody(AmqpReader in, int count)
  throws IOException
  {
    _handle = in.readInt();
    _deliveryId = in.readLong();
    
    _deliveryTag = null; in.read();
    
    _messageFormat = in.readInt();
    _isSettled = in.readBoolean();
    _isMore = in.readBoolean();
    _receiverSettleMode = ReceiverSettleMode.values()[in.readInt()];
    
    _state = in.readObject(DeliveryState.class);
    
    _isResume = in.readBoolean();
    _isAborted = in.readBoolean();
    _isBatchable = in.readBoolean();
  }
}
