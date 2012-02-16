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
 * AMQP link flow
 */
public class AmqpFlow extends AmqpAbstractPacket {
  public static final int CODE = AmqpConstants.FT_LINK_FLOW;

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
  
  @Override
  public void write(AmqpWriter out)
    throws IOException
  {
    out.writeDescriptor(FT_LINK_FLOW);
    
    out.writeUint((int) _nextIncomingId);
    out.writeUint(_incomingWindow);
    out.writeUint((int) _nextOutgoingId);
    out.writeUint(_outgoingWindow);
    out.writeUint(_handle);
    out.writeUint((int) _deliveryCount);
    out.writeUint(_linkCredit);
    out.writeUint(_available);
    out.writeBoolean(_isDrain);
    out.writeBoolean(_isEcho);
    out.writeMap(_properties);
  }

}
