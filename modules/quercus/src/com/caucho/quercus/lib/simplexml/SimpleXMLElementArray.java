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

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class SimpleXMLElementArray extends ArrayValueImpl
{
  /**
   * This class exists solely because in PHP
   * $xml->bar[0]['attrName'] is equiv. to $xml->bar['attrName']
   *
   * In other words, without an offset, 0 is the assumed offset
   *
   * @param key
   * @return appropriate attribute
   */
  @Override
  public Value get(Value key)
  {
    Value value;
    
    if (key instanceof StringValue) {
      value = super.get(LongValue.ZERO).get(key);
    } else {
      value = super.get(key);
    }

    return value;
  }

  /**
   * This is a field getter for the 0th entry in this array
   * This is created to allow for:
   * $foo->bar[0] and
   * $foo->bar->someTag
   */
  @Override
  public Value getField(Env env, String index)
  {
    return super.get(LongValue.ZERO).getField(env, index);
  }

 /**
  * Returns the field ref.
  */
 @Override
 public Value putField(Env env, String index, Value object)
 {
   return super.get(LongValue.ZERO).putField(env, index, object);
 }

  /**
   * Prints the value.
   * @param env
   */
  @Override
  public void print(Env env)
  {
    super.get(LongValue.ZERO).print(env);
  }

  @Override
  public Value toValue()
  {
    return super.get(LongValue.ZERO).toValue();
  }

  @Override
  public long toLong()
  {
    return super.get(LongValue.ZERO).toLong();
  }

  @Override
  public double toDouble()
  {
    return super.get(LongValue.ZERO).toDouble();
  }

  public Value copy()
  {
    return this;
  }

  public String toString()
  {
    Value value = super.get(LongValue.ZERO);

    return value.toString();
  }

}
