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

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.simplexml.node.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SimpleXMLElement object oriented API facade.
 * Also acts as the DOM document.
 */
public class SimpleXMLElement
{
  private static final Logger log
    = Logger.getLogger(SimpleXMLElement.class.getName());
  private static final L10N L = new L10N(SimpleXMLElement.class);
  
  private SimpleNode _node;
  private boolean _isRoot;
  
  protected SimpleXMLElement(SimpleNode node)
  {
    _node = node;
  }
  
  protected SimpleXMLElement(SimpleNode node, boolean isRoot)
  {
    _node = node;
    _isRoot = isRoot;
  }
  
  private SimpleNode getNode()
  {
    return _node;
  }
  
  /**
   * Returns a new instance based on the xml from 'data'.
   * 
   * @param env
   * @param data xml data
   * @param options
   * @param dataIsUrl
   * @param namespaceV
   * @param isPrefix
   */
  @ReturnNullAsFalse
  public static SimpleXMLElement __construct(Env env,
                                             Value data,
                                             @Optional int options,
                                             @Optional boolean dataIsUrl,
                                             @Optional Value namespaceV,
                                             @Optional boolean isPrefix)
  { 
    if (data.length() == 0)
      return null;
    
    try {
      String namespace = null;

      if (! namespaceV.isNull())
        namespace = namespaceV.toString();
      
      Node node = parse(env, data, options, dataIsUrl, namespace, isPrefix);
      
      if (node == null)
        return null;
      
      SimpleNode simpleNode = buildNode(env, null, node, namespace, isPrefix);
      
      return new SimpleXMLElement(simpleNode, true);

    } catch (IOException e) {
      env.warning(e);
      
      return null;
    }
    catch (ParserConfigurationException e) {
      env.warning(e);
      
      return null;
    }
    catch (SAXException e) {
      env.warning(e);
      
      return null;
    }
  }
  
  private static Node parse(Env env,
                            Value data,
                            int options,
                            boolean dataIsUrl,
                            String namespace,
                            boolean isPrefix)
    throws IOException,
           ParserConfigurationException,
           SAXException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();

    Document document = null;

    if (dataIsUrl) {
      Path path = env.lookup(data.toString());

      // PHP throws an Exception instead
      if (path == null) {
        log.log(Level.FINE, L.l("Cannot read file/URL '{0}'", data));
        env.warning(L.l("Cannot read file/URL '{0}'", data));

        return null;
      }

      ReadStream is = path.openRead();

      try {
        document = builder.parse(is);
      } finally {
        is.close();
      }
    }
    else if (data.isUnicode()) {
      StringReader reader = new java.io.StringReader(data.toString());

      document = builder.parse(new InputSource(reader));
    }
    else {
      InputStream in = data.toInputStream();

      document = builder.parse(in);
    }

    NodeList childList = document.getChildNodes();

    // php/1x70
    for (int i = 0; i < childList.getLength(); i++) {
      if (childList.item(i).getNodeType() == Node.ELEMENT_NODE)
        return childList.item(i);
    }
    
    return childList.item(0);
  }
  
  private static SimpleNode buildNode(Env env,
                                      SimpleNode parent,
                                      Node node,
                                      String namespace,
                                      boolean isPrefix)
  {
    if (node.getNodeType() == Node.TEXT_NODE) {
      return new SimpleText(node.getNodeValue());
    }
    
    // passed in namespace appears to have no effect in PHP, so just ignore
    // it by passing in null
    SimpleNode simpleNode = new SimpleElement(parent, node.getNodeName(), null);

    NamedNodeMap attrs = node.getAttributes();
    
    if (attrs != null) {
      int length = attrs.getLength();
      
      for (int i = 0; i < length; i++) {
        Attr attr = (Attr)attrs.item(i);
        
        simpleNode.addAttribute(attr.getName(), attr.getValue(), namespace);
      }
    }

    NodeList nodeList = node.getChildNodes();

    int length = nodeList.getLength();
    
    for (int i = 0; i < length; i++) {
      Node childNode = nodeList.item(i);

      SimpleNode child = buildNode(env, simpleNode, childNode, namespace, isPrefix);

      simpleNode.addChild(child);
    }
    
    return simpleNode;
  }

  /**
   * Adds an attribute to this node.
   * 
   * @param name
   * @param value
   * @param namespace
   */
  public void addAttribute(String name,
                           String value,
                           @Optional String namespace)
  {
    _node.addAttribute(name, value, namespace);
  }

  /**
   * Adds a child to this node.
   * 
   * @param env
   * @param name of the child node
   * @param value of the text node of the child
   * @param namespace
   * @return
   */
  public SimpleXMLElement addChild(Env env,
                                   String name,
                                   String value,
                                   @Optional Value namespaceV)
  {
    String namespace = null;

    if (! namespaceV.isNull())
      namespace = namespaceV.toString();
    
    SimpleText text = new SimpleText(value);
    SimpleElement child = new SimpleElement(getNode(), name, namespace);
    child.addChild(text);

    addChild(child);
    
    return new SimpleXMLElement(child);
  }

  private void addChild(SimpleNode child)
  {
    getNode().addChild(child);
  }
  
  /**
   * Returns the attributes of this node.
   * 
   * @param env
   * @param namespaceV
   * @param isPrefix
   */
  public SimpleXMLElement attributes(Env env,
                                     @Optional Value namespaceV,
                                     @Optional boolean isPrefix)
  {
    String namespace = null;
    if (! namespaceV.isNull())
      namespace = namespaceV.toString();
    
    SimpleResultSet result = new SimpleResultSet(getNode().getName());
    
    for (SimpleAttribute attr : getNode().getAttributes()) {
      if (attr.isSameNamespace(namespace))
        result.addAttribute(attr);
    }

    return new SimpleXMLElement(result);
  }
  
  /**
   * Returns all the children of this node, including the attributes of
   * this node.
   * 
   * @param env
   * @param namespaceV
   * @param isPrefix
   */
  public SimpleXMLElement children(Env env,
                                   @Optional Value namespaceV,
                                   @Optional boolean isPrefix)
  {
    String namespace = null;
    if (! namespaceV.isNull())
      namespace = namespaceV.toString();
    
    SimpleResultSet result = new SimpleResultSet(getNode().getName());
    
    for (SimpleElement child : getNode().getElementList()) {
      if (isPrefix) {
        if (child.isSamePrefix(namespace) ||
            child.getPrefix() == null
              && getNode().isSamePrefix(namespace)) {
          result.addChild(child);
        }
      }
      else {
        if (child.isSameNamespace(namespace) ||
            child.getNamespace() == null
              && getNode().isSameNamespace(namespace)) {
          result.addChild(child);
        }
      }
    }

    for (SimpleAttribute attr : getNode().getAttributes()) {
      if (attr.isSameNamespace(namespace))
        result.addAttribute(attr);
    }

    return new SimpleXMLElement(result);
  }
  
  /**
   * Converts node tree to a valid xml string.
   * 
   * @return xml string
   */
  @ReturnNullAsFalse
  public StringValue asXML(Env env)
  {
    StringValue xml = getNode().toXML(env);
    
    if (_isRoot) {
      StringValue root = env.createBinaryBuilder();

      root.append("<?xml version=\"1.0\"?>\n");
      root.append(xml);
      root.append("\n");
      
      return root;
    }
    else
      return xml;
  }

  /**
   * Returns the name of the node.
   * 
   * @return name of the node
   */
  @Name("getName")
  public String simplexml_getName()
  {
    return getNode().getName();
  }

  /**
   * Alias of getNamespaces().
   */
  public Value getDocNamespaces(Env env, @Optional boolean isRecursive)
  {
    return getNamespaces(env, isRecursive);
  }
  
  /**
   * Returns the namespaces used in this document.
   */
  public Value getNamespaces(Env env, @Optional boolean isRecursive)
  {
    ArrayValue array = new ArrayValueImpl();
    
    getNamespaces(array, getNode(), isRecursive);

    return array;
  }
  
  private static void getNamespaces(ArrayValue array,
                                    SimpleNode node,
                                    boolean isRecursive)
  {
    for (Map.Entry<String,SimpleAttribute> entry : node.getNamespaces().entrySet()) {
      SimpleAttribute namespace = entry.getValue();
      
      String name;
      
      if (namespace.getPrefix() == null)
        name = "";
      else
        name = namespace.getName();
      
      array.put(name, namespace.getValue());
    }
    
    if (isRecursive) {
      for (SimpleElement child : node.getElementList()) {
        getNamespaces(array, child, isRecursive);
      }
    }
  }
  
  /**
   * Runs an XPath expression on this node.
   * 
   * @param env
   * @param expression
   * @return array of results
   * @throws XPathExpressionException
   */
  public Value xpath(Env env, String expression)
  {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();

      InputSource is = new InputSource(asXML(env).toInputStream());
      NodeList nodes = (NodeList) xpath.evaluate(expression, is, XPathConstants.NODESET);

      int nodeLength = nodes.getLength();

      if (nodeLength == 0)
        return NullValue.NULL;

      // There are matching nodes
      ArrayValue result = new ArrayValueImpl();
      for (int i = 0; i < nodeLength; i++) {
        Node node = nodes.item(i);
        
        boolean isPrefix = node.getPrefix() != null;
        
        SimpleNode simpleNode
          = buildNode(env, null, nodes.item(i), node.getNamespaceURI(), isPrefix);
        
        SimpleXMLElement xml = new SimpleXMLElement(simpleNode);
        
        result.put(env.wrapJava(xml));
      }

      return result;
    }
    catch (XPathExpressionException e) {
      env.warning(e);
      log.log(Level.FINE, e.getMessage());
      
      return NullValue.NULL;
    }
  }
  
  /**
   * Implementation for getting the indices of this class.
   * i.e. <code>$a->foo[0]</code>
   */
  public SimpleXMLElement __get(Env env, Value indexV)
  {
    if (indexV.isString()) {
      String name = indexV.toString();
      
      SimpleAttribute attr = getNode().getAttribute(name);

      if (attr == null)
        return null;

      return new SimpleXMLElement(attr);
    }
    else if (indexV.isLongConvertible()) {
      int i = indexV.toInt();

      SimpleNode child = getNode().get(i);
      
      if (child != null)
        return new SimpleXMLElement(child);
      else
        return null;
    }
    else
      return null;
  }
  
  /**
   * Implementation for setting the indices of this class.
   * i.e. <code>$a->foo[0] = "hello"</code>
   */
  public void __set(String name, String value)
  {
    addAttribute(name, value, null);
  }
  
  /**
   * Implementation for getting the fields of this class.
   * i.e. <code>$a->foo</code>
   */
  public SimpleXMLElement __getField(String name)
  {
    SimpleElement child = getNode().getElement(name);
    
    if (child != null)
      return new SimpleXMLElement(child);
    else
      return null; 
  }
  
  /**
   * Implementation for setting the fields of this class.
   * i.e. <code>$a->foo = "hello"</code>
   */
  public void __setField(String name, String value)
  {
    SimpleText text = new SimpleText(value);
    
    SimpleElement child = getNode().getElement(name);
    
    if (child == null) {
      child = new SimpleElement(getNode(), name, null);
      getNode().addChild(child);
    }
    else {
      child.removeChildren();
    }
    
    child.addChild(text);
  }
  
  /**
   * Required for 'foreach' loops with only values specified in the loop.
   * i.e. <code>foreach($a as $b)</code>
   */
  public Iterator iterator()
  {
    return new SimpleXMLElementIterator(getNode().iterator());
  }
  
  /**
   * Required for 'foreach' loops with name/value pairs.
   * i.e. <code>foreach($a as $b=>$c)</code>
   */
  public Set<String> keySet()
  {
    return getNode().getAttributeMap().keySet();
  }
  
  /**
   * var_dump() implementation
   */
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    SimpleNode node = getNode();

    printDepth(out, 2 * depth);
    out.println("object(SimpleXMLElement) (" + node.getObjectSize() + ") {");
    
    node.varDumpImpl(env, out, depth + 1, valueSet);
    
    printDepth(out, 2 * depth);
    out.print('}');
  }
  
  /**
   * print_r() implementation
   */
  public void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.println("SimpleXMLElement Object");
    
    printDepth(out, 4 * depth);
    out.println('(');
    
    getNode().printRImpl(env, out, depth + 1, valueSet);
    
    printDepth(out, 4 * depth);
    out.println(')');
  }

  void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }
  
  public String toString()
  {
    return getNode().toString();
  }
}
