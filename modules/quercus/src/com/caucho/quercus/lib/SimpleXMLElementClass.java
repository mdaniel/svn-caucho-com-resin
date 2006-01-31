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

import com.caucho.quercus.env.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * SimpleXMLElement object oriented API facade
 */

public class SimpleXMLElementClass extends Value {

  //private static final Logger log = Logger.getLogger(SimpleXMLElementClass.class.getName());
  //private static final L10N L = new L10N(SimpleXMLElementClass.class);

  private Document _document;
  private Element _element;
  
  private Value _attributes;
  private Value _children;

  /**
   *  need to pass document for setting text values
   * 
   * @param document
   * @param element
   */
  public SimpleXMLElementClass(Document document,
                               Element element)
  {
    _document = document;
    _element = element;
  }

  /**
   * helper function to fill _attributes if _element has
   * any attributes.  If not sets _attributes to NullValue.NULL.
   */
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

    /**
     * All child text nodes get ignored ifthere are more children
     * 
     * For example:
     * 
     * $foo = simplexml_load_string('<foo>before<fooChild>misc</fooChild>after</foo>');
     * print_r($foo) =>
     *  SimpleXMLElement Object
     *  (
     *    [fooChild] => misc
     *  )
     * 
     * print_r($foo->children()) => 
     *  SimpleXMLElement Object
     *  (
     *     [0] => misc
     *  )
     * 
     * COMPARE WITH:
     * 
     * $foo = simplexml_load_string('<foo>bar</foo>');
     * print_r($foo) =>
     *  SimpleXMLElement Object
     *  (
     *    [0] => bar
     *  )
     * 
     * print_r($foo->children()) =>
     *  SimpleXMLElement Object
     *  (
     *  )
     * Therefore, treat the case of 1 child as special case
     */

    if (nodeLength == 1) {
      Node firstChild = children.item(0);
      SimpleXMLElementClass simpleXMLChild = new SimpleXMLElementClass(_document,  (Element) firstChild);
      
      _children = new ArrayValueImpl();

      if (firstChild.getNodeType() == Node.TEXT_NODE)
        _children.put(new LongValue(0), simpleXMLChild);
      else
        _children.put(new StringValue(firstChild.getNodeName()), simpleXMLChild);

    } else {
      for (int i=0; i < nodeLength; i++) {
        Node child = children.item(i);

        //skip all text nodes since we know that there is more than 1 node
        if (child.getNodeType() == Node.TEXT_NODE)
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

        childArray.put(new SimpleXMLElementClass(_document, (Element) child));
      }
    }

    //loop through childrenList and put each element in _children
    if (childrenList.size() > 0) {
      _children = new ArrayValueImpl();
      for (StringValue childName : childrenList) {
        ArrayValue childArray = childrenHashMap.get(childName);
        if (childArray.getSize() == 1)
          _children.put(childName,childArray.get(new LongValue(0)));
        else
          _children.put(childName, childArray);
      }
    } else if (_children == null) { //_children wasn't set in case of exactly 1 child
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

  public Value getArray(Value name)
  {
    Value result;
    
    children();
    
    if (_children instanceof ArrayValue) {
      result = _children.get(name);
      if (result != null)
        return result;
    }
    
    return NullValue.NULL;
  }
  
  public Value get(Value name)
  {
    Value result;

    // First check to see if there are attributes
    // If so and name is an attribute,
    // then return the attribute value
    attributes();

    if (_attributes instanceof ArrayValue) {
      result = _attributes.get(name);
      if (result instanceof StringValue)
        return result;
    }

    children();

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
    //If this is a text node, then print the value
    NodeList children = _element.getChildNodes();
    if ((children.getLength() == 1) && (children.item(0).getNodeType() == Node.TEXT_NODE))
      return children.item(0).getNodeValue();

    return "SimpleXMLElement Object";
  }

  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    if ("attributes".equals(methodName)) {
      return attributes();
    } else if ("children".equals(methodName)) {
      return children();
    } else if ("asXML".equals(methodName)) {
      return asXML();
    }

    return super.evalMethod(env, methodName);
  }

  public Element getElement()
  { 
    return _element;
  }

  public Value put(Value name, Value value)
  {
    Value child;

    children();

    if (_children instanceof ArrayValue) {
      child = _children.get(name);

      if (child == null)
        return BooleanValue.FALSE;

      // Issue warning if array
      if (child instanceof ArrayValue) {

      } else {
        /**
         *  $foo = simplexml_load_string('<parent><child><sub1 /><sub2 /></child></parent>');
         *  $foo->child = "bar";
         * 
         *  EQUIVALENT TO:
         * 
         *  $foo = simplexml_load_string('<parent><child>bar</child></parent>');
         * 
         * So remove all children first then add text node; 
         */
        Element element = ((SimpleXMLElementClass) child).getElement();
        NodeList children = element.getChildNodes();
        int childrenLength = children.getLength();

        for (int i = 0; i < childrenLength; i++)
          element.removeChild(children.item(i));

        Text text = _document.createTextNode(value.toString());
        
        element.appendChild(text);
      }
    }

    return value;
  }
  
  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getObject(Env env, Value index)
  {
    Value result = children().get(index);
    
    if (result != null)
      return result;
    
    return NullValue.NULL;
  }

  /**
   * 
   * @return this SimpleXMLElement as well formed XML
   */
  public StringValue asXML()
  {
    StringBuffer result = new StringBuffer();
    
    result.append("<?xml version=\"1.0\"?>\n");
    result.append(generateXML().toString());
    
    return new StringValue(result.toString());
  }

  /**
   * recursive helper function for asXML
   * @return XML in string buffer
   */
  public StringBuffer generateXML()
  {
    StringBuffer sb = new StringBuffer();
    
    // If this is a text node, then just return the text
    if (_element.getNodeType() == Node.TEXT_NODE) {
      sb.append(_element.getNodeValue());
      return sb;
    }
    
    // not a text node
    sb.append("<");
    
    sb.append(_element.getNodeName());
    
    // add attributes, if any
    NamedNodeMap attrs = _element.getAttributes();
    int attrLength = attrs.getLength();
    
    for (int i=0; i < attrLength; i++) {
      Node attribute = attrs.item(i);
      sb.append(" " + attribute.getNodeName() + "=\"" + attribute.getNodeValue() + "\"");
    }
    
    // recurse through children, if any
    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();
    
    if (nodeLength == 0) {
      sb.append(" />");
      return sb;
    }
    
    sb.append(">");
    
    // there are children
    for (int i=0; i < nodeLength; i++) {
      Node child = children.item(i);
      
      if (child.getNodeType() == Node.TEXT_NODE) {
        sb.append(child.getNodeValue());
        continue;
      }
      SimpleXMLElementClass simple = new SimpleXMLElementClass(_document, (Element) child);
      sb.append(simple.generateXML());
    }
    
    // add closing tag
    sb.append("</" + _element.getNodeName() + ">");
    
    return sb;
  }
}
