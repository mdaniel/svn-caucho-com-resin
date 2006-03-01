/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.CharacterData;

public class DOMCharacterDataValue extends DOMNodeValue {
  
  public DOMCharacterDataValue(CharacterData characterData)
  {
    super(characterData);
  }
  
  @Override
  public Value getField(String name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if ("data".equals(name)) {

      return new StringValue(((CharacterData) _node).getData());

    } else if ("length".equals(name)) {
      
      return new LongValue(((CharacterData) _node).getLength());
    }

    return NullValue.NULL;
  }
  
  @Override
  public Value putField(Env env, String key, Value value)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if ("data".equals(key)) {
      
      ((CharacterData) _node).setData(value.toString());
      
      return value;
    }
    
    return NullValue.NULL;
  }

  public Value appendData(Value data)
  {
    if (_node == null)
      return NullValue.NULL;
    
    ((CharacterData) _node).appendData(data.toString());
    
    return NullValue.NULL;
  }
  
  public Value deleteData(Value offset, Value count)
  {
    if (_node == null)
      return NullValue.NULL;
    
    ((CharacterData) _node).deleteData(offset.toInt(), count.toInt());
    
    return NullValue.NULL;
  }
  
  public Value insertData(Value offset, Value data)
  {
    if (_node == null)
      return NullValue.NULL;
    
    ((CharacterData) _node).insertData(offset.toInt(), data.toString());
    
    return NullValue.NULL;
  }
  
  public Value replaceData(Value offset, Value count, Value data)
  {
    if (_node == null)
      return NullValue.NULL;
    
    ((CharacterData) _node).replaceData(offset.toInt(), count.toInt(), data.toString());
    
    return NullValue.NULL;
  }
  
  public Value substringData(Value offset, Value count)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new StringValue(((CharacterData) _node).substringData(offset.toInt(), count.toInt()));
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("appendData".equals(methodName))
      return appendData(a0);
    
    return super.evalMethod(env, methodName, a0);
  }

  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    if ("deleteData".equals(methodName))
      return deleteData(a0, a1);
    else if ("insertData".equals(methodName))
      return insertData(a0, a1);
    else if ("substringData".equals(methodName))
      return substringData(a0, a1);
    
    return super.evalMethod(env, methodName, a0, a1);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1, Value a2)
    throws Throwable
  {
    if ("replaceData".equals(methodName))
      return replaceData(a0, a1, a2);
    
    return super.evalMethod(env, methodName, a0, a1, a2);
  }
  
}
