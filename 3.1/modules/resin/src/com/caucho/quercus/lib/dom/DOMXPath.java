/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;

public class DOMXPath
{
  private XPath _xpath;
  private XPathExpression _compiledXPath;
  private String _expression;
  private DOMNamespaceContext _context;
  
  private DOMDocument _document;
  
  public static DOMXPath __construct(Env env, DOMDocument document)
  {
    return new DOMXPath(document);
  }
  
  private DOMXPath(DOMDocument document)
  {
    _document = document;
    
    _context = new DOMNamespaceContext();
    
    _xpath = XPathFactory.newInstance().newXPath();
    _xpath.setNamespaceContext(_context);
  }
  
  public Object evaluate(Env env,
                         String expression)
  {
    Node node = _document.getDelegate();
    
    NodeList nodeList = (NodeList) query(env, expression, node);

    if (nodeList.getLength() == 1) {
      return _document.wrap(nodeList.item(0));
    }
    else
      return _document.wrap(nodeList);
      
  }
  
  public DOMNodeList query(Env env,
                           String expression,
                           @Optional DOMNode<Node> contextNode)
  {
    Node node;
    
    if (contextNode != null)
      node = contextNode.getDelegate();
    else
      node = _document.getDelegate();
    
    NodeList nodeList = (NodeList) query(env, expression, node);

    return _document.wrap(nodeList);
  }
  
  private NodeList query(Env env, String expression, Node node)
  {
    try {
      compile(expression);
      
      NodeList nodeList =
        (NodeList) _compiledXPath.evaluate(node,
                                           XPathConstants.NODESET);

      return nodeList;
    }
    catch (XPathExpressionException e) {
      throw new QuercusModuleException(e);
    }
  }
  
  private void compile(String expression)
  {
    try {
      if (_compiledXPath == null || ! expression.equals(_expression)) {
        _compiledXPath = _xpath.compile(expression);
        _expression = expression;
      }
    }
    catch (XPathExpressionException e) {
      throw new QuercusModuleException(e);
    }
  }
  
  public boolean registerNamespace(String prefix, String namespaceURI)
  {
    _context.addNamespace(prefix, namespaceURI);

    return true;
  }
  
  public class DOMNamespaceContext
    implements NamespaceContext
  {
    private HashMap<String, LinkedHashSet<String>> _namespaceMap
      = new HashMap<String, LinkedHashSet<String>>();
    
    protected void addNamespace(String prefix, String namespaceURI)
    {
      LinkedHashSet<String> list = _namespaceMap.get(namespaceURI);
      
      if (list == null) {
        list = new LinkedHashSet<String>();
        
        _namespaceMap.put(namespaceURI, list);
      }

      list.add(prefix);
    }
    
    public String getNamespaceURI(String prefix)
    {
      for (Map.Entry<String, LinkedHashSet<String>> entry
           : _namespaceMap.entrySet()) {
        if (entry.getValue().contains(prefix))
          return entry.getKey();
      }

      return null;
    }
    
    public String getPrefix(String namespaceURI)
    {
      Iterator<String> iter = getPrefixes(namespaceURI);
      
      if (iter != null)
        return iter.next();
      else
        return null;
    }
    
    public Iterator<String> getPrefixes(String namespaceURI)
    {
      LinkedHashSet<String> prefixList = _namespaceMap.get(namespaceURI);
      
      if (prefixList != null)
        return prefixList.iterator();
      else
        return null;
    }
  }
}
