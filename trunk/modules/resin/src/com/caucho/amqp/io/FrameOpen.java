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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.caucho.util.L10N;


/**
 * AMQP connection-open frame 
 */
public class FrameOpen extends AmqpAbstractFrame {
  private static final L10N L = new L10N(FrameOpen.class);
  
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
  
  @Override
  protected long getDescriptorCode()
  {
    return AmqpConstants.FT_CONN_OPEN;
  }
  
  public void setContainerId(String id)
  {
    _containerId = id;
  }
  
  public String getContainerId()
  {
    return _containerId;
  }
  
  public void setHostname(String hostname)
  {
    _hostname = hostname;
  }
  
  public String getHostname()
  {
    return _hostname;
  }
  
  public void setMaxFrameSize(int size)
  {
    _maxFrameSize = size;
  }
  
  public int getMaxFrameSize()
  {
    return _maxFrameSize;
  }
  
  public void setChannelMax(int max)
  {
    _channelMax = max;
  }
  
  public int getChannelMax()
  {
    return _channelMax;
  }
  
  public void setIdleTimeout(long timeout)
  {
    _idleTimeout = timeout;
  }
  
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }
  
  public void addOutgoingLocale(String locale)
  {
    if (_outgoingLocales == null) {
      _outgoingLocales = new ArrayList<String>();
    }
    
    _outgoingLocales.add(locale);
  }
  
  public List<String> getOutgoingLocales()
  {
    return _outgoingLocales;
  }
  
  public void addIncomingLocale(String locale)
  {
    if (_incomingLocales == null) {
      _incomingLocales = new ArrayList<String>();
    }
    
    _incomingLocales.add(locale);
  }
  
  public List<String> getIncomingLocales()
  {
    return _incomingLocales;
  }
  
  public void addOfferedCapability(String capability)
  {
    if (_offeredCapabilities == null) {
      _offeredCapabilities = new ArrayList<String>();
    }
    
    _offeredCapabilities.add(capability);
  }
  
  public List<String> getOfferedCapabilities()
  {
    return _offeredCapabilities;
  }
  
  public void addDesiredCapability(String capability)
  {
    if (_desiredCapabilities == null) {
      _desiredCapabilities = new ArrayList<String>();
    }
    
    _desiredCapabilities.add(capability);
  }
  
  public List<String> getDesiredCapabilities()
  {
    return _desiredCapabilities;
  }
  
  public Map<String,?> getProperties()
  {
    return _properties;
  }

  @Override
  public FrameOpen createInstance()
  {
    return new FrameOpen();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeString(_containerId);
    out.writeString(_hostname);
      
    out.writeUint(_maxFrameSize);
    out.writeUshort(_channelMax);
    out.writeUint((int) _idleTimeout);
    
    out.writeSymbolArray(_outgoingLocales);
    out.writeSymbolArray(_incomingLocales);
    out.writeSymbolArray(_offeredCapabilities);
    out.writeSymbolArray(_desiredCapabilities);
    
    out.writeMap(_properties);
    
    return 10;
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _containerId = in.readString();
    _hostname = in.readString();
      
    _maxFrameSize = in.readInt();
    _channelMax = in.readInt();
    _idleTimeout = in.readInt();
    
    _outgoingLocales = in.readSymbolArray();
    _incomingLocales = in.readSymbolArray();
    _offeredCapabilities = in.readSymbolArray();
    _desiredCapabilities = in.readSymbolArray();
    
    _properties = in.readFieldMap();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _containerId + "," + _hostname + "]";
  }
}
