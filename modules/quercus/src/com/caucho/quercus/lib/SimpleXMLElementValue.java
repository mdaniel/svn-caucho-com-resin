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

  private HashMap<StringValue, Value> _childMap;


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
  protected void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException, Throwable
  {
    if (depth == 0) {
      out.println("SimpleXMLElement Object");
      out.println('(');
    }
    
    // Need to refill _childMap because elements might have been
    // replaced
    fillChildMap();
    
    Set keyValues = _childMap.entrySet();
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
        
        // loop through each element of _childMap
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
            printDepth(out, 4 * (depth + 2));
            out.println(')');
          } else 
            ((Value) entry.getValue()).printR(env, out, depth + 2, valueSet);
          
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
        
        printAttributes(out, depth);
        printDepth(out, 4 * depth);
        out.println(')');
      }
    }
    
    if (depth == 0)
      out.println(')');
  }

  /**
   * 
   */
  public void fillChildMap()
  {
    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();

    _childMap = new HashMap<StringValue, Value>();

    if (nodeLength == 0)
      return;
    
    // If <foo><bar>misc</bar></foo>, then put (bar , misc)
    // into foo's _childMap
    if ((nodeLength == 1) && (children.item(0).getNodeType() == Node.TEXT_NODE)) {
        _childMap.put(new StringValue("0"), new StringValue(children.item(0).getNodeValue()));
      
    } else {
      for (int i=0; i < nodeLength; i++) {
        Node child = children.item(i);
  
        if (child.getNodeType() == Node.TEXT_NODE)
          continue;
  
        StringValue childTagName = new StringValue(child.getNodeName());
  
        // Check to see if this is the first instance of a child
        // with this NodeName.  If so create a new ArrayValueImpl,
        // if not add to existing ArrayValueImpl
        ArrayValue childArray = (ArrayValue) _childMap.get(childTagName);
        if (childArray == null) {
          childArray = new ArrayValueImpl();
          _childMap.put(childTagName, childArray);
        }
  
        childArray.put(new SimpleXMLElementValue(_document, (Element) child));
      }
  
      // Iterate over _childMap and replace all entries with only one
      // element from ArrayValue to SimpleXMLElementValue
      Set keyValues = _childMap.entrySet();
      int keyLength = keyValues.size();
      Iterator keyIterator = keyValues.iterator();
      for (int i=0; i < keyLength; i++) {
        Map.Entry entry = (Map.Entry) keyIterator.next();
        ArrayValue childArray = (ArrayValue) entry.getValue();
        if (childArray.getSize() == 1)
          _childMap.put((StringValue) entry.getKey(), childArray.get(new LongValue(0)));
      }
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

  /**
   * 
   * @param name
   * @return ArrayValue, SimpleXMLElement or null
   */
  public Value getField(String name)
  {
    Value result = null;
    
    // Always fillChildMap because an element may have been overwritten
    fillChildMap();
    
    if (!_childMap.isEmpty())   
      result = _childMap.get(new StringValue(name));

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

  /**
   * XXX: Currently does not work for $xml->inside->wayinside = "foo";
   * 
   * $xml = simpleXML_load_string("<foo><inside><wayinside>bar</wayinside></inside></foo>");
   * 
   * need to check for the following 2 cases:
   * $xml->inside->wayinside = "New Value";
   * or
   * $temp = $xml->inside;
   * $temp->wayinside = "New Value;
   * 
   * Both are valid
   * 
   */
  public Value putField(String name, Value value)
  { 
    // always recreated _childMap
    fillChildMap();
    
    if (!_childMap.isEmpty()) {
      Value result = _childMap.get(new StringValue(name));  
      
      if (result == null)
        return BooleanValue.FALSE;

      // Issue warning if array
      if (result instanceof ArrayValue) {
        //XXX: Need to put a warning here "Cannot assign to an array of nodes (duplicate subnodes or attr detected)"
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
        Element element = ((SimpleXMLElementValue) result).getElement();
        while (element.hasChildNodes()) {
          element.removeChild(element.getFirstChild());
        }

        Text text = _document.createTextNode(value.toString());

        element.appendChild(text);
        
        return value;
      }
    }
    
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
      printDepth(out, 4 * (depth + 2));
      out.println(')');
    }
  }
}
