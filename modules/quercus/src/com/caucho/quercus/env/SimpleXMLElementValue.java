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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.env;

import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SimpleXML object oriented API facade
 */
public class SimpleXMLElementValue extends ArrayValueImpl {
  private static final Logger log = Logger.getLogger(SimpleXMLElementValue.class.getName());
  private static final L10N L = new L10N(SimpleXMLElementValue.class);

  private Document _document;
  private Element _element;

  private String _className;
  private int _options;
/*
  public SimpleXMLElementValue(SimpleXMLElementValue copy)
  {
    this(copy.getSize());

    for (Entry ptr = copy.getHead(); ptr != null; ptr = ptr._next) {
      // php/0662 for copy
      put(ptr.getKey(), ptr.getRawValue().copyArrayItem());
    }
  }*/
  
  /**
   * constructor used by simplexml_load_string
   * XXX: className and options currently are unsupported
   * 
   * @param data
   * @param className
   * @param options
   */
  public SimpleXMLElementValue(@NotNull String data,
                               @Optional String className,
                               @Optional int options)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      
      DocumentBuilder builder = factory.newDocumentBuilder();
      _document = builder.parse(new ByteArrayInputStream(data.getBytes()));
      _element = _document.getDocumentElement();
      
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.toString()), e);
    }

    _className = className;
    _options = options;
  }

  /**
   * constructor used by simplexml_load_file
   * XXX: className and options currently are unsupported
   * @param file
   * @param className
   * @param options
   */
  public SimpleXMLElementValue(@NotNull Path file,
                               @Optional String className,
                               @Optional int options)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      
      DocumentBuilder builder = factory.newDocumentBuilder();
      _document = builder.parse(file.openRead());
      _element = _document.getDocumentElement();
      
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.toString()), e);
    }

    _className = className;
    _options = options;
  }

  /**
   * constructor used by SimpleXMLElement->children
   * that function returns an Array of SimpleXMLElements
   * 
   * @param element
   * @param className
   * @param options
   */
  public SimpleXMLElementValue(Element element,
                               String className,
                               int options)
  {
    _element = element;
    _className = className;
    _options = options;
  }

  /**
   * 
   * @param file
   * @return XML string if file is not specified otherwise true on success, false on failure
   */
  public Value asXML(@Optional Path file)
  {

    return BooleanValue.FALSE;
  }

  public Element getElement()
  {
    return _element;
  }
  
  public SimpleXMLElementValue fillChildren(SimpleXMLElementValue simpleXMLElement)
  {
    Element node = simpleXMLElement.getElement();
    
    NodeList children = node.getChildNodes();
    
    // Loop through children of node.
    // If a child of node has no children itself, then it is empty
    // and will be skipped.  If a child has only 1 child,
    // then it is text, and the text will be added to childArray
    // If a child has more than 1 child, then it is a parent node
    // and will be added to childArray as a SimpleXMLElement.
    HashMap<StringValue, ArrayValueImpl> childrenHashMap = new HashMap<StringValue, ArrayValueImpl>();
    
    for (int j = 0; j < children.getLength(); j++) {
      Node child = children.item(j);
      
      //skip empty children
      if (child.getChildNodes().getLength() == 0)
        continue;
      
      StringValue childName = new StringValue(child.getNodeName());
      
      // Check to see if this is the first instance of a child
      // with this NodeName.  If so create a new ArrayValueImpl,
      // if not add to existing ArrayValueImpl
      ArrayValueImpl childArray = childrenHashMap.get(childName);
      if (childArray == null) {
        childArray = new ArrayValueImpl();
        childrenHashMap.put(childName, childArray);
      }
      
      //if text node return value
      //otherwise recursive fill new SimpleXMLElement
      if (child.getChildNodes().getLength() == 1)
        childArray.put(new StringValue(child.getFirstChild().getNodeValue()));
      else {
        childArray.put(fillChildren(new SimpleXMLElementValue((Element) child, _className, _options))); // recursion
      }
      
      // if childArray has only one element, only put the Value
      // Note: if it turns out that a subsequent iteration adds
      // to this particular childArray (ie: childArrayHashMap.get(childName)
      // does not return null), then nodeArray.put() will replace
      // whatever was stored in nodeArray.get(childName)
      // with the updated childArray.
      if (childArray.size() == 1)
        simpleXMLElement.put(childName,childArray.get(new LongValue(0)), false);
      else
        simpleXMLElement.put(childName, childArray, false);
    }
    
    return simpleXMLElement;
  }

  /**
   * If name is a LongValue, then treat this as ArrayValueImpl.
   * 
   * If name is a StringValue then get returns a
   * new SimpleXMLElementValue but also treats it as an
   * ArrayValueImpl in that it puts each child element into it.
   * 
   * @param name tagName of immediate children of _element
   * @return either this(name) or new SimpleXMLElementValue
   */
  
  public Value get(Value name)
  {
    // If name is Long, then treat this as an array
    // else treat as SimpleXMLElementValue
    if (name instanceof LongValue) {
      
      return super.get(name);

    } else {
      
      // First check to see if there is an @attribute array
      // If @attribute array exists and name is an attribute,
      // then return the attribute value
      Value attributeArray =  super.get(new StringValue("@attributes"));
      if (attributeArray instanceof ArrayValue) {
        Value value = attributeArray.get(name);
        if (value instanceof StringValue)
          return value;
      }
      
      // Either there is no @attribute array or name is not an attribute
      // so assume we are meant to return a SimpleXMLElementValue
      SimpleXMLElementValue result = new SimpleXMLElementValue(_element, _className, _options);
      
      NodeList nodes = _element.getElementsByTagName(name.toString());
      int nodeLength = nodes.getLength();
      
      for (int i = 0; i < nodeLength; i++) {
        Node node = nodes.item(i);
        if (node.getParentNode() == _element) {
          
          SimpleXMLElementValue childElement = new SimpleXMLElementValue((Element) node, _className, _options);
          
          //Generate @attributes array, if applicable
          NamedNodeMap attrs = node.getAttributes();
          attributeArray = new ArrayValueImpl();
          int attrLength = attrs.getLength();
          
          if (attrLength > 0) {
            for (int j=0; j < attrLength; j++) {
              Node attribute = attrs.item(j);
              StringValue nodeName = new StringValue(attribute.getNodeName());
              StringValue nodeValue = new StringValue(attribute.getNodeValue());
              attributeArray.put(nodeName, nodeValue);
            }
            childElement.put(new StringValue("@attributes"), attributeArray, false);
          }
          
          // Generate fillChildren array and store all
          // values in result.  Also store attributes in result
          // but use associative array (ie: simpleXmlElement['foo'] => bar)
          if (node.getChildNodes().getLength() > 1)
            result.put(fillChildren(childElement), false);
          else {
            childElement.put(new StringValue(node.getFirstChild().getNodeValue()), false);
            result.put(childElement, false);
          }
        }
      }
  
      return result;
    }
  }
  
  private Value put(Value value, boolean setPhpValue)
  {
    Value key = createTailKey();
      
    put(key, value,setPhpValue);
    return this;
  }

  /**
   * if this is a PHP assignment, then put finds the first element whose
   * tagname == key.  Then replaces its firstChild by a new child with the
   * string value.toString(). 
   * 
   * @param key
   * @param value
   * @param setPhpValue if true, then this is a Php assignment
   * @return SimpleXMLElementValue
   */
  private Value put(Value key, Value value, boolean setPhpValue)
  {
    if (setPhpValue) {
      
      // get 1st child with tagname == key
      NodeList nodes = _element.getElementsByTagName(key.toString());
      Node node = null;
      for (int i = 0; i < nodes.getLength(); i++) {
        node = nodes.item(i);
        if (node.getParentNode() == _element)
          break;
      }
      
      // XXX: Need to handle case if node does not exist
      if (node == null)
        return BooleanValue.FALSE;
      
      // Replace contents of node with value.toString()
      //Node replacementNode = node.cloneNode(false);
      //System.out.println("Existing: " + node.getFirstChild().getNodeValue());
      //node.setNodeValue(value.toString());
      

      //System.out.println("Replacement: " + node.getFirstChild().getNodeValue());
      //XXX: figure out code to replace element with text
      return super.put(key, value);
    } else {
      return super.put(key, value);
    }
  }
  
  public ArrayValue put(Value value)
  {
    return (ArrayValue) put(value, true);
  }
  
  public Value put(Value key, Value value)
  {
    return put(key, value, true);
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    SimpleXMLElementValue result = new SimpleXMLElementValue(_element, _className, _options);
    
    for (Entry ptr = this.getHead(); ptr != null; ptr = ptr._next) {
      result.put(ptr.getKey(), ptr.getRawValue().copyArrayItem(), false);
    }
    
    return result;
  }

  public String toString()
  {
    return "SimpleXMLElement Object";
  }
  //@todo attributes
  //@todo xpath
}
