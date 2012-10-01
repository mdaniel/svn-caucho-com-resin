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

import com.caucho.message.DistributionMode;
import com.caucho.util.L10N;

/**
 * Describes the source node of a link
 */
public class LinkSource extends AmqpAbstractComposite {
  private static final L10N L = new L10N(LinkSource.class);
  
  private String _address;
  private Durability _durable;
  private ExpiryPolicy _expiryPolicy;
  private long _timeout;  // uint seconds
  private boolean _isDynamic;
  private Map<String,Object> _dynamicNodeProperties;
  private DistributionMode _distributionMode; // symbol
  private Map<String,?> _filter; // filter-set
  private String _defaultOutcome; // symbol outcome
  private List<String> _outcomes; // symbol
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
  
  public DistributionMode getDistributionMode()
  {
    return _distributionMode;
  }
  
  public void setDistributionMode(DistributionMode distMode)
  {
    _distributionMode = distMode;
  }
  
  public Map<String,?> getFilter()
  {
    return _filter;
  }
  
  public String getDefaultOutcome()
  {
    return _defaultOutcome;
  }
  
  public List<String> getOutcomes()
  {
    return _outcomes;
  }
  
  public List<String> getCapabilities()
  {
    return _capabilities;
  }
  
  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_SOURCE;
  }
  
  @Override
  public LinkSource createInstance()
  {
    return new LinkSource();
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
    _dynamicNodeProperties = in.readFieldMap();
    
    _distributionMode = 
        DistributionMode.find(in.readSymbol());
    _filter = in.readFieldMap();
    
    _defaultOutcome = in.readSymbol();
    _outcomes = in.readSymbolArray();
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

    if (_distributionMode != null)
      out.writeSymbol(_distributionMode.getName());
    else
      out.writeNull();
    
    out.writeMap(_filter);
    
    out.writeSymbol(_defaultOutcome);
    out.writeSymbolArray(_outcomes);
    out.writeSymbolArray(_capabilities);
    
    return 11;
  }
  
  public enum Durability {
    NONE,
    CONFIGURATION,
    UNSETTLED_STATE;
  }
  
  public enum ExpiryPolicy {
    LINK_DETACH("link-detach"),
    SESSION_END("session-end"),
    CONNECTION_CLOSE("connection-close"),
    NEVER("never");
    
    private String _name;
    
    ExpiryPolicy(String name)
    {
      _name = name;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public static ExpiryPolicy find(String name)
    {
      if (name == null)
        return null;
      else if ("link-detach".equals(name))
        return LINK_DETACH;
      else if ("session-end".equals(name))
        return SESSION_END;
      else if ("connection-close".equals(name))
        return CONNECTION_CLOSE;
      else if ("never".equals(name))
        return NEVER;
      else
        throw new IllegalArgumentException(L.l("unknown type: " + name));
    }
  }
}
