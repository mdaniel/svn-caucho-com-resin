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

import javax.annotation.PostConstruct;

import com.caucho.mqueue.amqp.AmqpLinkAttach.ReceiverSettleMode;
import com.caucho.mqueue.amqp.AmqpLinkAttach.Role;
import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;

/**
 * AMQP session-begin frame 
 */
public class AmqpMessageTransfer extends AmqpAbstractPacket {
  public static final int CODE = AmqpConstants.FT_MESSAGE_TRANSFER;

  private int _handle; // uint (required)
  private long _deliveryId; // uint seq (RFC1982)
  private byte []_deliveryTag; // up to 32 byte
  private int _messageFormat; // uint
  private boolean _isSettled;
  private boolean _isMore;
  private ReceiverSettleMode _receiverSettleMode
    = ReceiverSettleMode.FIRST;
  private String _state; // delivery-state
  private boolean _isResume;
  private boolean _isAborted;
  private boolean _isBatchable;
  
  @Override
  public void write(AmqpWriter out)
    throws IOException
  {
    out.writeDescriptor(FT_MESSAGE_TRANSFER);
    
    out.writeUint(_handle);
    out.writeUint((int) _deliveryId);
    out.writeNull(); // delivery-tag (binary)
    out.writeUint(_messageFormat);
    out.writeBoolean(_isSettled);
    out.writeBoolean(_isMore);
    out.writeUbyte(_receiverSettleMode.ordinal());
    out.writeNull(); // delivery-state
    out.writeBoolean(_isResume);
    out.writeBoolean(_isAborted);
    out.writeBoolean(_isBatchable);
  }

}
