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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SimpleXMLElement is a pseudo ArrayValueImpl
 * Internally looks like:
 * SimpleXMLElementValue Object
 * (
 *   [@attributes] => Array Value (if there are attributes)
 *   [0] => StringValue or SimpleXMLElementValue (if any children)
 *   ...
 *   [N] => StringValue or SimpleXMLElementValue (if any children)
 * )
 * 
 * $xmlString = "<root><a><b a1=\"v1\">foo</b><b a1=\"v2\">bar</b></a></root>"
 * $xml = simplexml_load_string($xmlstr);
 * 
 * $xml->root has 1 element so you can access by $xml->root[0] as well
 * $xml->root->a is equivalent to $xml->root[0]->a
 * foreach ($xml->root->a->b as $b) {echo $b['a1']." ";} will output "v1 v2"
 * 
 */
public class SimpleXMLElementValue extends ArrayValueImpl {
  private static final Logger log = Logger.getLogger(SimpleXMLElementValue.class.getName());
  private static final L10N L = new L10N(SimpleXMLElementValue.class);

  private Element _element;

  private String _className;
  private int _options;

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
      Document document = builder.parse(new ByteArrayInputStream(data.getBytes()));
      _element = document.getDocumentElement();
      
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.toString()), e);
    }

    _className = className;
    _options = options;
    fillSimpleXMLElement();
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
      Document document = builder.parse(file.openRead());
      _element = document.getDocumentElement();
      
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
    
    fillSimpleXMLElement();
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

  /**
   * 
   * @param data currently ignored
   * @return attribute array
   */
  public Value attributes(@Optional String data)
  {
    return super.get(new StringValue("@attributes"));
  }
  
  public Element getElement()
  {
    return _element;
  }

  /**
   * 
   * @param nsprefix ignored for now
   * @return SimpleXMLElementValue filled with the children
   */
  public Value children(@Optional String nsprefix)
  {
    return this;
  }
  
  public Value get(Value name)
  {
    // First check to see if there is an @attribute array
    // If @attribute array exists and name is an attribute,
    // then return the attribute value
    Value attributeArray =  super.get(new StringValue("@attributes"));
    if (attributeArray instanceof ArrayValue) {
      Value value = attributeArray.get(name);
      if (value instanceof StringValue)
        return value;
    }
    
    return super.get(name);
  }

  /**
   * helper function to make Attribute Array
   * @param node
   * @return either ArrayValueImpl or NullValue.NULL
   */
  private Value getAttributes(Element node)
  {
    NamedNodeMap attrs = node.getAttributes();
    ArrayValueImpl attributeArray = new ArrayValueImpl();
    int attrLength = attrs.getLength();
    
    if (attrLength > 0) {
      for (int j=0; j < attrLength; j++) {
        Node attribute = attrs.item(j);
        StringValue nodeName = new StringValue(attribute.getNodeName());
        StringValue nodeValue = new StringValue(attribute.getNodeValue());
        attributeArray.put(nodeName, nodeValue);
      }
      
      return attributeArray;
    }
    
    return NullValue.NULL;
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
      
      node.getFirstChild().setNodeValue(value.toString());

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

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws Throwable
  {
    env.getOut().print(get(new LongValue(0)));
  }

  /**
   * if simpleXMLElement has attributes, need to check here to see if it
   * has just one other entry.  If so, return the String stored there.
   * 
   * @return either node value or "SimpleXMLElement Object"
   */
  public String toString()
  {
    
    if ((getSize() == 1)){
       return super.get(new LongValue(0)).toString();
     }
    /*
     if (getSize() == 2) {
       Value attributeArray = super.get(new StringValue("@attributes"));
       if (attributeArray instanceof ArrayValue) {
         return super.get(new LongValue(0)).toString();
       } 
     }*/
    
    return "SimpleXMLElement Object";
  }
  
  private void fillSimpleXMLElement()
  {
    Value attrs = getAttributes(_element);
    if (attrs instanceof ArrayValueImpl)
      put (new StringValue("@attributes"), attrs, false);
    
    NodeList children = _element.getChildNodes();
    int nodeLength = children.getLength();
    HashMap<StringValue, ArrayValueImpl> childrenHashMap = new HashMap<StringValue, ArrayValueImpl>();
    ArrayList<StringValue> childrenList = new ArrayList<StringValue>();
    
    for (int j = 0; j < nodeLength; j++) {
      Node child = children.item(j);
      
      //skip empty children (ie: "\n ");
      if ((child.getNodeName().equals("#text")) && (child.getChildNodes().getLength() == 0))
        continue;
      
      StringValue childTagName = new StringValue(child.getNodeName());
      
      // Check to see if this is the first instance of a child
      // with this NodeName.  If so create a new ArrayValueImpl,
      // if not add to existing ArrayValueImpl
      ArrayValueImpl childArray = childrenHashMap.get(childTagName);
      if (childArray == null) {
        childArray = new ArrayValueImpl();
        childrenHashMap.put(childTagName, childArray);
        childrenList.add(childTagName);
      }
      
      // Recursion happens here
      // if only text, put StringValue 
      if ((child.getChildNodes().getLength() == 1) && (child.getFirstChild().getNodeName().equals("#text")))
        childArray.put(new StringValue(child.getFirstChild().getNodeValue()));
      else
        childArray.put(new SimpleXMLElementValue((Element) child, _className, _options));
    }
    
    //loop through childrenHashMap and put each element in this SimpleXMLElement
    for (StringValue childName : childrenList) {
      ArrayValueImpl childArray = childrenHashMap.get(childName);
      
      if (childArray.size() == 1)
        put(childName,childArray.get(new LongValue(0)), false);
      else
        put(childName, childArray, false);
    }
  }
  //@todo attributes
  //@todo xpath
}
