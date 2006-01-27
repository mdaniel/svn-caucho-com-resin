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

package com.caucho.quercus.lib;

import com.caucho.quercus.env.*;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;


/**
 * SimpleXML object oriented API facade
 */
public class SimpleXMLElementClass extends Value {
  private static final Logger log = Logger.getLogger(SimpleXMLElementClass.class.getName());
  private static final L10N L = new L10N(SimpleXMLElementClass.class);

  private Document _document;
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
  public SimpleXMLElementClass(@NotNull String data,
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
  public SimpleXMLElementClass(@NotNull Path file,
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
  public SimpleXMLElementClass(Element element,
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

  public Value children(Env env)
  {
    return BooleanValue.FALSE;
    /*
    NodeList nodes = _element.getChildNodes();
    ArrayValueImpl children = new ArrayValueImpl();

    for (int i = 0; i < nodes.getLength(); i++) {
      NamedNodeMap attributes = nodes.item(i).getAttributes();
      ArrayValueImpl child = new ArrayValueImpl();
      children.put(env.wrapJava(new SimpleXMLElementClass((Element) nodes.item(i),_className, _options)));
    }*/
  }

  public Value get(Value name)
  {
    ArrayValueImpl result = new ArrayValueImpl();

    NodeList nodes = _element.getElementsByTagName(name.toString());
    int count = 0;
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node.getParentNode() == _element) {
        ArrayValueImpl nodeArray = new ArrayValueImpl();
        NodeList children = node.getChildNodes();

        HashMap<StringValue, ArrayValueImpl> childArrayHashMap = new HashMap<StringValue, ArrayValueImpl>();
        
        for (int j = 0; j < children.getLength(); j++) {
          Node child = children.item(j);
          // Check to see if child is empty (ie: "\n")
          if (child.getChildNodes().getLength() > 0) {
            StringValue childName = new StringValue(child.getNodeName());
            ArrayValueImpl childArray = childArrayHashMap.get(childName);
            if (childArray == null) {
              childArray = new ArrayValueImpl();
              childArrayHashMap.put(childName, childArray);
            }

            // Check to see if child is single node or element itself
            if (child.getChildNodes().getLength() > 1)
              childArray.put(new SimpleXMLElementClass((Element) child, _className, _options));
            else
              childArray.put(new StringValue(child.getFirstChild().getNodeValue()));
            
            // if childArray has only one element, only put the Value
            if (childArray.size() == 1)
              nodeArray.put(childName,childArray.get(new LongValue(0)));
            else
              nodeArray.put(childName, childArray);
          }
        }
        result.put(new LongValue(count), nodeArray);
        count++;
      }
    }

    return result;
  }
  
  public void print(Env env)
    throws Throwable
  {
    NodeList childNodes = _element.getChildNodes();
    env.getOut().print("Charlie");
  }
  /*
  public Value toValue()
  {
    return get(new StringValue(_element.getNodeName()));
  }*/
  //@todo attributes
  //@todo xpath
}
