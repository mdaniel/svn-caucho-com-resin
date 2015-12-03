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

import com.caucho.amqp.io.LinkSource.Durability;
import com.caucho.amqp.io.LinkSource.ExpiryPolicy;

/**
 * Describes the source node of a link
 */
public class LinkTarget extends AmqpAbstractComposite {
  private String _address;
  private Durability _durable;
  private ExpiryPolicy _expiryPolicy;
  private long _timeout;  // uint seconds
  private boolean _isDynamic;
  private Map<String,Object> _dynamicNodeProperties;
  private List<String> _capabilities; // symbol
  
  public String getAddress()
  {
    return _address;
  }
  
  public void setAddress(String address)
  {
    _address = address;
  }
  
  public Durability getDurable()
  {
    return _durable;
  }
  
  public ExpiryPolicy getExpiryPolicy()
  {
    return _expiryPolicy;
  }
  
  public long getTimeout()
  {
    return _timeout;
  }
  
  public boolean isDynamic()
  {
    return _isDynamic;
  }
  
  public void setDynamicNodeProperties(Map<String,Object> props)
  {
    _dynamicNodeProperties = props;
  }
  
  public Map<String,Object> getDynamicNodeProperties()
  {
    return _dynamicNodeProperties;
  }
  
  public List<String> getCapabilities()
  {
    return _capabilities;
  }
  
  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_TARGET;
  }
  
  @Override
  public LinkTarget createInstance()
  {
    return new LinkTarget();
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _address = in.readString();
    
    _durable = Durability.values()[in.readInt()];
    
    _expiryPolicy = ExpiryPolicy.find(in.readSymbol());
    _timeout = in.readLong();
    _isDynamic = in.readBoolean();
    _dynamicNodeProperties = (Map) in.readMap();
    
    _capabilities = in.readSymbolArray();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeString(_address);
    
    if (_durable != null)
      out.writeUint(_durable.ordinal());
    else
      out.writeNull();
    
    if (_expiryPolicy != null)
      out.writeSymbol(_expiryPolicy.getName());
    else
      out.writeNull();
    
    out.writeUint((int) _timeout);
    out.writeBoolean(_isDynamic);
    out.writeFieldsMap(_dynamicNodeProperties);
    
    out.writeSymbolArray(_capabilities);
    
    return 7;
  }
}
