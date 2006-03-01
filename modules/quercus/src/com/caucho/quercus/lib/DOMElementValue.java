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

import org.w3c.dom.Element;

public class DOMElementValue extends DOMNodeValue {
  
  public DOMElementValue(Element element)
  {
    _node = element;
  }
  
  @Override
  public Value setNodeValue(Value value)
  {
    if (_node == null)
      return NullValue.NULL;
    
    ((Element) _node).setNodeValue(value.toString());
    return value;
  }
  
  @Override
  public Value getField(String name)
  {
     if ("schemaTypeInfo".equals(name))
       return NullValue.NULL;
     else if ("tagName".equals(name)) {
       
       if (_node == null)
         return NullValue.NULL;
       
       return new StringValue(((Element) _node).getNodeName());
       
     } else
       return NullValue.NULL;
  }

  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("getAttribute".equals(methodName))
      return getAttribute(a0);
    else if ("getAttributeNode".equals(methodName))
      return getAttributeNode(a0);
    else if ("getElementsByTagName".equals(methodName))
      return getElementsByTagName(a0);
    else if ("hasAttribute".equals(methodName))
      return hasAttribute(a0);
    else if ("removeAttribute".equals(methodName))
      return removeAttribute(a0);
    else if ("removeAttributeNode".equals(methodName))
      return removeAttributeNode(a0);
    else if ("setAttributeNode".equals(methodName))
      return setAttributeNode(a0);
    else if ("setAttributeNodeNS".equals(methodName))
      return setAttributeNodeNS(a0);
    
    return super.evalMethod(env, methodName, a0);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    if ("getAttributeNodeNS".equals(methodName))
      return getAttributeNodeNS(a0, a1);
    else if ("getAttributeNS".equals(methodName))
      return getAttributeNS(a0, a1);
    else if ("getElementsByTagNameNS".equals(methodName))
      return getElementsByTagNameNS(a0, a1);
    else if ("hasAttributeNS".equals(methodName))
      return hasAttributeNS(a0, a1);
    else if ("removeAttributeNS".equals(methodName))
      return removeAttributeNS(a0, a1);
    else if ("setAttribute".equals(methodName))
      return setAttribute(a0, a1);
    
    return super.evalMethod(env, methodName, a0, a1);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1, Value a2)
    throws Throwable
  {
    if ("setAttributeNS".equals(methodName))
      return setAttributeNS(a0, a1, a2);
    
    return super.evalMethod(env, methodName, a0, a1, a2);
  }
  
  public Value getAttribute(Value name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new StringValue(((Element) _node).getAttribute(name.toString()));
  }
  
  public Value getAttributeNode(Value name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new DOMAttrValue(((Element) _node).getAttributeNode(name.toString()));
  }
  
  public Value getAttributeNodeNS(Value namespaceURI,
                                  Value localName)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new DOMAttrValue(((Element) _node).getAttributeNodeNS(namespaceURI.toString(), localName.toString()));
  }
  
  public Value getAttributeNS(Value namespaceURI,
                              Value localName)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new StringValue(((Element) _node).getAttributeNS(namespaceURI.toString(), localName.toString()));
  }
  
  public Value getElementsByTagName(Value name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new DOMNodeListValue(((Element) _node).getElementsByTagName(name.toString()));
  }
  
  public Value getElementsByTagNameNS(Value namespaceURI, Value localName)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new DOMNodeListValue(((Element) _node).getElementsByTagNameNS(namespaceURI.toString(), localName.toString()));
  }
 
  public Value hasAttribute(Value name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if (((Element) _node).hasAttribute(name.toString()))
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value hasAttributeNS(Value namespaceURI,
                              Value localName)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if (((Element) _node).hasAttributeNS(namespaceURI.toString(), localName.toString()))
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value removeAttribute(Value name)
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    ((Element) _node).removeAttribute(name.toString());
    
    return BooleanValue.TRUE;
  }
  
  public Value removeAttributeNode(Value oldAttr)
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    ((Element) _node).removeAttributeNode(((DOMAttrValue) oldAttr).getAttribute());
    
    return BooleanValue.TRUE;
  }
  
  public Value removeAttributeNS(Value namespaceURI,
                                 Value localName)
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    ((Element) _node).removeAttributeNS(namespaceURI.toString(), localName.toString());
    
    return BooleanValue.TRUE;
  }
  
  public Value setAttribute(Value name,
                            Value value)
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    ((Element) _node).setAttribute(name.toString(), value.toString());
    
    return BooleanValue.TRUE;
  }
  
  public Value setAttributeNode(Value attrNode)
  {
    if ((_node == null) || (!(attrNode instanceof DOMAttrValue)))
      return NullValue.NULL;
    
    return new DOMAttrValue(((Element) _node).setAttributeNode(((DOMAttrValue)attrNode).getAttribute()));
  }
  
  public Value setAttributeNodeNS(Value attrNode)
  {
    if ((_node == null) || (!(attrNode instanceof DOMAttrValue)))
      return NullValue.NULL;
    
    return new DOMAttrValue(((Element) _node).setAttributeNode(((DOMAttrValue)attrNode).getAttribute()));
  }
  
  public Value setAttributeNS(Value namespaceURI,
                              Value qualifiedName,
                              Value value)
  {
    if (_node == null)
      return NullValue.NULL;
    
    ((Element) _node).setAttributeNS(namespaceURI.toString(), qualifiedName.toString(), value.toString());
    
    return NullValue.NULL;
  }
}
