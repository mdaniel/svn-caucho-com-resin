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

package com.caucho.amqp.client;

import com.caucho.amqp.AmqpReceiver;

/**
 * AMQP client
 */
class AmqpClientReceiver implements AmqpReceiver {
  private final AmqpConnectionImpl _client;
  
  private final String _address;
  private final int _handle;
  
  private final boolean _isAutoAck;
  
  private String _value;
  
  AmqpClientReceiver(AmqpConnectionImpl client,
                     AmqpClientReceiverFactory builder,
                     int handle)
  {
    _client = client;
    _address = builder.getAddress();
    _handle = handle;
    
    _isAutoAck = builder.getAckMode();
  }
  
  @Override
  public String take()
  {
    String value = _value;
    
    _value = null;
    
    if (value != null && _isAutoAck) {
      _client.dispositionAccept(_handle);
    }
    
    return value;
  }
  
  @Override
  public void accept()
  {
    _client.dispositionAccept(_handle);
  }
  
  @Override
  public void reject()
  {
    _client.dispositionReject(_handle);
  }
  
  @Override
  public void release()
  {
    _client.dispositionRelease(_handle);
  }
  
  void setValue(String value)
  {
    _value = value;
  }
  
  public void close()
  {
    _client.closeReceiver(_handle);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handle + "," + _address + "]";
  }
}
