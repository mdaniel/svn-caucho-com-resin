/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.Attr;

public class DOMAttrValue extends DOMNodeValue {

 // Attr _attr;
  
  public DOMAttrValue(Attr attr)
  {
    _node = attr;
  }
  
  public Value isId()
  {
    if (_node == null)
      return NullValue.NULL;
    
    if (((Attr)_node).isId())
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  @Override
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    if ("isId".equals(methodName))
      return isId();
    
    return super.evalMethod(env, methodName);
  }
  
  public Attr getAttribute()
  {
    return (Attr) _node;
  }
  
  @Override
  public Value getField(String index)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if ("name".equals(index)) {
      
      return new StringValue(((Attr) _node).getName());
      
    } else if ("ownerElement".equals(index)) {
      
      return new DOMElementValue(((Attr) _node).getOwnerElement());
      
    } else if ("schemaTypeInfo".equals(index)) {
      
      return new DOMTypeInfoValue(((Attr) _node).getSchemaTypeInfo());
      
    } else if ("specified".equals(index)) {
      
      if (((Attr) _node).getSpecified())
        return BooleanValue.TRUE;
      else
        return BooleanValue.FALSE;
      
    } else if ("value".equals(index)) {
      
      return new StringValue(((Attr) _node).getValue());
      
    }
    
    return NullValue.NULL;
  }
  
  @Override
  public Value putField(Env env, String index, Value object)
  {
    if ("value".equals(index)) {

      ((Attr) _node).setValue(object.toString());
      return object;
    }
    
    return NullValue.NULL;
  }
}
