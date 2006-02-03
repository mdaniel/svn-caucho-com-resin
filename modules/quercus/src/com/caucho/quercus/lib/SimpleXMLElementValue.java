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
import com.caucho.vfs.WriteStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * SimpleXMLElement object oriented API facade
 */

public class SimpleXMLElementValue extends Value {

  private Document _document;
  private Element _element;

  private Value _attributes;
  private Value _children;

  private HashMap<StringValue, Value> _childrenHashMap;


  /**
   *  need to pass document for setting text values
   * 
   * @param document
   * @param element
   */
  public SimpleXMLElementValue(Document document,
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

  @Override
  public void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException, Throwable
  {
    if (depth == 0) {
      out.println("SimpleXMLElement Object");
      out.println('(');
    }
    
    if (_childrenHashMap == null)
      fillChildrenHashMap();
    
    Set keyValues = _childrenHashMap.entrySet();
    int keyLength = keyValues.size();
    Iterator keyIterator = keyValues.iterator();
    Map.Entry entry;
    
    if (keyIterator.hasNext()) {
      entry = (Map.Entry) keyIterator.next();
    
      if (entry.getValue() instanceof StringValue) {
        if (depth == 0) {
          printDepth(out, 4);
          out.print("[0] => ");
        }
        
        out.println(entry.getValue().toString());
      } else {
        if (depth != 0) {
          out.println("SimpleXMLElement Object");
      
          printDepth(out, 4 * depth);
          out.println('(');
        }
   
        printAttributes(out, depth);
        
        // loop through each element of _childrenHashMap
        // but first reset iterator
        keyIterator = keyValues.iterator();
        for (int i = 0; i < keyLength; i++) {
          entry = (Map.Entry) keyIterator.next();
          printDepth(out, 4 * (depth + 1));
          out.print("[" + entry.getKey() + "] => ");
          if (entry.getValue() instanceof ArrayValue) {
            out.println("Array");
            printDepth(out, 4 * (depth + 2));
            out.println('(');
            // Iterate through each SimpleXMLElement
            for (Map.Entry<Value, Value> mapEntry : ((ArrayValue) entry.getValue()).entrySet()) {
              printDepth(out, 4 * (depth + 3));
              out.print("[" + mapEntry.getKey().toString() + "] => ");
              mapEntry.getValue().printR(env, out, depth + 4, valueSet);
              out.println();
            }
          } else 
            ((Value) entry.getValue()).printRImpl(env, out, depth + 2, valueSet);
          out.println();
        }
  
        //Print closing parenthesis
        if (depth != 0) {
          printDepth(out, 4 * depth);
          out.println(")");
        }
      }
    } else {
      if (depth != 0) {
        out.println("SimpleXMLElement Object");
        
        printDepth(out, 4 * depth);
        out.println('(');
      }
      printAttributes(out, depth);
    }
    
    if (depth == 0)
      out.print(')');
  }

  /**
   * helper function for print_r and var_dump
   * This is different from fillChildren(), for example:
   * 
   * $xmlString = "<parent><child role=\"son\"/><child role=\"daughter\"/></parent>";
   * $xml = simplexml_from_string($xmlString);
   * 
   * print_r($xml) returns:
   * 
   * SimpleXMLElement Object
   * (
   *   [child] => //2 SimpleXMLElement Objects
   * )
   * 
   * BUT
   * 
   * foreach ($xml->children() as $child) ... will return 2 elements
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

      if (firstChild.getNodeType() == Node.TEXT_NODE) {

        _children = new StringValue(firstChild.getNodeValue());

      } else {

        SimpleXMLElementValue simpleXMLChild = new SimpleXMLElementValue(_document,  (Element) firstChild);
        _children = new ArrayValueImpl();
        _children.put(new StringValue(firstChild.getNodeName()), simpleXMLChild);

      }
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

        childArray.put(new SimpleXMLElementValue(_document, (Element) child));
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

  /**
   * 
   */
  public void fillChildrenHashMap()
  {
    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();

    _childrenHashMap = new HashMap<StringValue, Value>();

    if (nodeLength == 0)
      return;
    
    // If <foo><bar>misc</bar></foo>, then put (bar , misc)
    // into foo's _childrenHashMap
    if ((nodeLength == 1) && (children.item(0).getNodeType() == Node.TEXT_NODE)) {
        _childrenHashMap.put(new StringValue("0"), new StringValue(children.item(0).getNodeValue()));
      
    } else {
      for (int i=0; i < nodeLength; i++) {
        Node child = children.item(i);
  
        if (child.getNodeType() == Node.TEXT_NODE)
          continue;
  
        StringValue childTagName = new StringValue(child.getNodeName());
  
        // Check to see if this is the first instance of a child
        // with this NodeName.  If so create a new ArrayValueImpl,
        // if not add to existing ArrayValueImpl
        ArrayValue childArray = (ArrayValue) _childrenHashMap.get(childTagName);
        if (childArray == null) {
          childArray = new ArrayValueImpl();
          _childrenHashMap.put(childTagName, childArray);
        }
  
        childArray.put(new SimpleXMLElementValue(_document, (Element) child));
      }
  
      // Iterate over _childrenHashMap and replace all entries with only one
      // element from ArrayValue to SimpleXMLElementValue
      Set keyValues = _childrenHashMap.entrySet();
      int keyLength = keyValues.size();
      Iterator keyIterator = keyValues.iterator();
      for (int i=0; i < keyLength; i++) {
        Map.Entry entry = (Map.Entry) keyIterator.next();
        ArrayValue childArray = (ArrayValue) entry.getValue();
        if (childArray.getSize() == 1)
          _childrenHashMap.put((StringValue) entry.getKey(), childArray.get(new LongValue(0)));
      }
    }
  }

  public Value fillForPrintR()
  {
    Value result = NullValue.NULL;
    HashMap<StringValue, ArrayValue> childrenHashMap = new HashMap<StringValue, ArrayValue>();
    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();

    ArrayList<StringValue> childrenList = new ArrayList<StringValue>();
    SimpleXMLElementValue childValue;

    if (nodeLength == 1) {
      Node firstChild = children.item(0);

      if (firstChild.getNodeType() == Node.TEXT_NODE) {

       return new StringValue(firstChild.getNodeValue());

      } else {
        childValue = new SimpleXMLElementValue(_document, (Element) firstChild);
        result = new ArrayValueImpl();

        result.put(new StringValue(firstChild.getNodeName()), childValue.fillForPrintR()); //recursion

        return result;
      }

    } else { //more than 1 node (so skip all text nodes)

      for (int i=0; i < nodeLength; i++) {
        Node child = children.item(i);

        if (child.getNodeType() == Node.TEXT_NODE)
          continue;

        StringValue childTagName = new StringValue(child.getNodeName());

        // Check to see if this is the first instance of a child
        // with this NodeName.  If so create a new ArrayValueImpl,
        // if not add to existing ArrayValueImpl
        ArrayValue childArray = childrenHashMap.get(childTagName);
        if (childArray == null) {
          childArray = new ArrayValueImpl();
          childrenHashMap.put(childTagName, childArray);
          childrenList.add(childTagName);
        }

        childValue = new SimpleXMLElementValue(_document, (Element) child);

        childArray.put(childValue.fillForPrintR()); // recursion
      }

      //loop through childrenList and put each element in result
      if (childrenList.size() > 0) {
        result = new ArrayValueImpl();
        for (StringValue childName : childrenList) {
          ArrayValue childArray = childrenHashMap.get(childName);
          if (childArray.getSize() == 1)
            result.put(childName, childArray.get(new LongValue(0)));
          else
            result.put(childName, childArray);
        }
      }

      return result;
    }
  }

  /**
   * NOTE: this function does not use fillChildren()
   * 
   * @return shallow array of immediate children()
   */
  public Value children()
  {
    ArrayValue childArray = new ArrayValueImpl();

    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();

    for (int i=0; i < nodeLength; i++) {
      Node child = children.item(i);

      if (child.getNodeType() == Node.TEXT_NODE)
        continue;

      childArray.put(new SimpleXMLElementValue(_document, (Element) child));
    }

    if (childArray.getSize() > 0)
      return childArray;
    else
      return NullValue.NULL;
  }

  public Value getArray(Value name)
  {
    Value result;

    fillChildren();

    if (_children instanceof ArrayValue) {
      result = _children.get(name);
      if (result != null)
        return result;
    }

    return NullValue.NULL;
  }

  /**
   * gets all children from expression $xml->foo
   * @param name
   * @return ArrayValue, SimpleXMLElement or null
   */
  public Value getField(String name)
  {
    Value result = null;
    
    fillChildrenHashMap();
    
    if (!_childrenHashMap.isEmpty())   
      result = _childrenHashMap.get(new StringValue(name));

    return result;
  }

  /**
   * gets the attribute 'name' from SimpleXMLElement
   * @param name
   * @return StringValue or null
   */
  public Value get(Value name)
  {
    Value result = null;
    
    // First check to see if there are attributes
    // If so and name is an attribute,
    // then return the attribute value
    attributes();

    if (_attributes instanceof ArrayValue) {
      result = _attributes.get(name);
      if (result instanceof StringValue)
        return result;
    }

    return result;
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

  /**
   * Evaluates a method with 1 arg.
   */
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("xpath".equals(methodName)) {
      return xpath(a0.toString());
    }

    return super.evalMethod(env, methodName, a0);
  }


  public Element getElement()
  {
    return _element;
  }

  public Value put(Value name, Value value)
  {
    Value child;

    fillChildren();

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
        Element element = ((SimpleXMLElementValue) child).getElement();
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
    if (_children == null)
      fillChildren();

    Value result = _children.get(index);

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
    StringBuilder result = new StringBuilder();

    result.append("<?xml version=\"1.0\"?>\n");
    result.append(generateXML().toString());

    return new StringValue(result.toString());
  }

  /**
   * recursive helper function for asXML
   * @return XML in string buffer
   */
  public StringBuilder generateXML()
  {
    StringBuilder sb = new StringBuilder();

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
      sb.append(" ")
        .append(attribute.getNodeName())
        .append("=\"")
        .append(attribute.getNodeValue())
        .append("\"");
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
      SimpleXMLElementValue simple = new SimpleXMLElementValue(_document, (Element) child);
      sb.append(simple.generateXML());
    }

    // add closing tag
    sb.append("</").append(_element.getNodeName()).append(">");

    return sb;
  }

  /**
   * Does not support relative xpath
   * 
   * @param path
   * @return NodeList
   * @throws XPathExpressionException
   */
  public Value xpath(String path)
    throws XPathExpressionException
  {
    XPath xpath = XPathFactory.newInstance().newXPath();

    InputSource is = new InputSource(new ByteArrayInputStream(asXML().toString().getBytes()));

    NodeList nodes = (NodeList) xpath.evaluate(path, is, XPathConstants.NODESET);

    int nodeLength = nodes.getLength();

    if (nodeLength == 0)
      return NullValue.NULL;

    // There are matching nodes
    Value result = new ArrayValueImpl();
    for (int i = 0; i < nodeLength; i++) {
      result.put(new SimpleXMLElementValue(_document, (Element) nodes.item(i)));
    }

    return result;
  }

  private void printAttributes(WriteStream out,
                               int depth)
    throws IOException
  {
    if (_attributes == null)
      attributes();

    // Print attributes if not null
    if (_attributes != NullValue.NULL) {
      printDepth(out, 4 * (depth + 1));
      out.println("[@attributes] => Array");
      printDepth(out, 4 * (depth + 2));
      out.println('(');
      
      // Iterate through each attribute ([name] => value)
      for (Map.Entry<Value, Value> mapEntry : ((ArrayValue) _attributes).entrySet()) {
        ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;
        printDepth(out, 4 * (depth + 3));
        out.println("[" + entry.getKey().toString() + "] => " + entry.getValue().toString());
      }
      out.println();
    }
  }
}
