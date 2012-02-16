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
 * AMQP connection-open frame 
 */
public class AmqpConnectionOpen extends AmqpAbstractPacket {
  public static final int CODE = AmqpConstants.FT_CONN_OPEN;
  
  private String _containerId;    // required
  private String _hostname;
  private int _maxFrameSize = 0; // uint
  private int _channelMax = 0;   // ushort
  private long _idleTimeout;     // uint (milliseconds)
  private List<String> _outgoingLocales;     // symbol*
  private List<String> _incomingLocales;     // symbol*
  private List<String> _offeredCapabilities; // symbol*
  private List<String> _desiredCapabilities; // symbol*
  
  private Map<String,?> _properties;         // field
  
  public void setContainerId(String id)
  {
    _containerId = id;
  }
  
  public void setHostname(String hostname)
  {
    _hostname = hostname;
  }
  
  public void setMaxFrameSize(int size)
  {
    _maxFrameSize = size;
  }
  
  public void setChannelMax(int max)
  {
    _channelMax = max;
  }
  
  public void setIdleTimeout(long timeout)
  {
    _idleTimeout = timeout;
  }
  
  @Override
  public void write(AmqpWriter out)
    throws IOException
  {
    out.writeDescriptor(FT_CONN_OPEN);
    
    out.writeString(_containerId);
    out.writeString(_hostname);
      
    out.writeUint(_maxFrameSize);
    out.writeUshort(_channelMax);
    out.writeUint((int) _idleTimeout);
    
    out.writeArray(_outgoingLocales);
    out.writeArray(_incomingLocales);
    out.writeArray(_offeredCapabilities);
    out.writeArray(_desiredCapabilities);
    
    out.writeMap(_properties);
  }
}
