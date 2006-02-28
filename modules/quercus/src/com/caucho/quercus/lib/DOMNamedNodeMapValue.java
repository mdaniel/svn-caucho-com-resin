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
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.NamedNodeMap;

public class DOMNamedNodeMapValue extends Value {

  private NamedNodeMap _namedNodeMap;
  
  public DOMNamedNodeMapValue(NamedNodeMap namedNodeMap)
  {
    _namedNodeMap = namedNodeMap;
  }
  
  public Value getNamedItem(Value name)
  {
    if (_namedNodeMap == null)
      return NullValue.NULL;
    
    return new DOMNodeValue(_namedNodeMap.getNamedItem(name.toString()));
      
  }
  
  public Value getNamedItemNS(Value namespaceURI,
                              Value localName)
  {
    if (_namedNodeMap == null)
      return NullValue.NULL;
    
    return new DOMNodeValue(_namedNodeMap.getNamedItemNS(namespaceURI.toString(), localName.toString()));
  }
  
  public Value item(Value index)
  {
    if (_namedNodeMap == null)
      return NullValue.NULL;
    
    return new DOMNodeValue(_namedNodeMap.item(index.toInt()));
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("getNamedItem".equals(methodName))
      return getNamedItem(a0);
    else if ("item".equals(methodName))
      return item(a0);
    
    return super.evalMethod(env, methodName, a0);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
     if ("getNamedItemNS".equals(methodName))
       return getNamedItemNS(a0, a1);
    
    return super.evalMethod(env, methodName, a0, a1);
  }
}
