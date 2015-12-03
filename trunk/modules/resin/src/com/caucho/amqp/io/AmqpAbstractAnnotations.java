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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * annotations have a symbol or long key
 */
abstract public class AmqpAbstractAnnotations extends AmqpAbstractPacket {
  private Map<Object,Object> _map;
  
  public boolean isEmpty()
  {
    if (_map != null) {
      return _map.isEmpty();
    }
    else {
      return false;
    }
  }
  
  public int getSize()
  {
    if (_map != null) {
      return _map.size();
    }
    else {
      return 0;
    }
  }
  public void put(Object key, Object value)
  {
    if (_map == null) {
      _map = new HashMap<Object,Object>();
    }
    
    _map.put(key, value);
  }
  
  public void putAll(Iterator iter)
  {
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      
      put(entry.getKey(), entry.getValue());
    }
  }
  
  public Map<?,?> getMap()
  {
    return _map;
  }
  
  abstract protected long getDescriptorCode();

  @Override
  public final void write(AmqpWriter out)
    throws IOException
  {
    out.writeDescriptor(getDescriptorCode());

    out.writeAnnotationsMap(_map);
  }
  
  @Override
  public void read(AmqpReader in)
    throws IOException
  {
    long code = in.readDescriptor();
    
    readValue(in);
  }
  
  @Override
  public void readValue(AmqpReader in)
    throws IOException
  {
    _map = (Map) in.readMap();
  }
}
