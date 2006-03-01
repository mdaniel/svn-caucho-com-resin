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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.NodeList;

public class DOMNodeListValue extends Value {

  private NodeList _nodeList;
  
  public DOMNodeListValue(NodeList nodeList)
  {
    _nodeList = nodeList;
  }
  
  public Value item(Value index)
  {
    if (_nodeList == null)
      return NullValue.NULL;
    
    return new DOMNodeValue(_nodeList.item(index.toInt()));
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("item".equals(methodName))
      return item(a0);
    
    return super.evalMethod(env, methodName, a0);
  }
  
  @Override
  public Value getField(String name)
  {
    if (_nodeList == null)
      return NullValue.NULL;
    
    if ("length".equals(name))
      return new LongValue(_nodeList.getLength());
    
    return NullValue.NULL;
  }
}
