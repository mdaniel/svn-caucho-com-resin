/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */


package com.caucho.quercus.lib;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * SimpleXMLElement object oriented API facade
 */

public class SimpleXMLElementClass extends Value {

  Element _element;
  Value _attributes;
  Value _children;

  public SimpleXMLElementClass(Element element)
  {
    _element = element;
  }

  private void setAttributes()
  {
    NamedNodeMap attrs = _element.getAttributes();
    int attrLength = attrs.getLength();

    if (attrLength > 0) {
      _attributes = new ArrayValueImpl();
      
      for (int j=0; j < attrLength; j++) {
        Node attribute = attrs.item(j);
        StringValue nodeName = new StringValue(attribute.getNodeName());
        StringValue nodeValue = new StringValue(attribute.getNodeValue());
        _attributes.put(nodeName, nodeValue);
      }

    } else {

      _attributes = NullValue.NULL;

    }
  }
  
  /**
   * 
   * @return either ArrayValueImpl or NullValue.NULL
   */
  public Value attributes()
  {
    if (_attributes == null) {
      setAttributes();
    }

    return _attributes;
  }

  /**
   * helper function for children
   */
  private void fillChildren()
  {
    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();
    HashMap<StringValue, ArrayValue> childrenHashMap = new HashMap<StringValue, ArrayValue>();
    ArrayList<StringValue> childrenList = new ArrayList<StringValue>();
    
    for (int i=0; i < nodeLength; i++) {
      Node child = children.item(i);
      
      //skip empty children (ie: "\n ")
      if ((child.getNodeName().equals("#text")) && (child.getChildNodes().getLength() == 0))
        continue;
      
      StringValue childTagName = new StringValue((child.getNodeName()));
      
      // Check to see if this is the first instance of a child
      // with this NodeName.  If so create a new ArrayValueImpl,
      // if not add to existing ArrayValueImpl
      ArrayValue childArray = childrenHashMap.get(childTagName);
      if (childArray == null) {
        childArray = new ArrayValueImpl();
        childrenHashMap.put(childTagName, childArray);
        childrenList.add(childTagName);
      }
      
      // if only text, put StringValue 
      if ((child.getChildNodes().getLength() == 1) && (child.getFirstChild().getNodeName().equals("#text")))
        childArray.put(new StringValue(child.getFirstChild().getNodeValue()));
      else
        childArray.put(new SimpleXMLElementClass((Element) child));
    }
    
    //loop through childrenHashMap and put each element in this SimpleXMLElement
    if (childrenList.size() > 0) {
      for (StringValue childName : childrenList) {
        ArrayValue childArray = childrenHashMap.get(childName);
        _children = new ArrayValueImpl();
        if (childArray.getSize() == 1)
          _children.put(childName,childArray.get(new LongValue(0)));
        else
          _children.put(childName, childArray);
      }
    } else {
      _children = NullValue.NULL;
    }
  }

  public Value children()
  {
    if (_children == null) {
      fillChildren();
    }

    return _children;
  }

  public Value get(Value name)
  {
    Value result;
    
    // First check to see if there are attributes
    // If so and name is an attribute,
    // then return the attribute value
    if (_attributes == null)
      setAttributes();
    
    if (_attributes instanceof ArrayValue) {
      result = _attributes.get(name);
      if (result instanceof StringValue)
        return result;
    }
    
    if (_children == null)
      fillChildren();
    
    if (_children instanceof ArrayValue) {
      result = _children.get(name);
      if (result != null)
        return result;
    }
  
    return NullValue.NULL;
  }
  
  /**
   * Converts to a string.
   */
  public String toString()
  {
    return "SimpleXMLElement Object";
  }
    
}
