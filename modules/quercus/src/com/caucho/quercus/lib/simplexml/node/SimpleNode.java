/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.simplexml.node;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import java.util.LinkedHashMap;

/**
 * Represents the actual SimpleXML node implementation.
 */
public abstract class SimpleNode
{
  SimpleAttribute _namespace;
  String _prefix;
  
  String _name;
  String _value;

  ArrayList<SimpleNode> _children;
  
  ArrayList<SimpleElement> _elementList;
  TreeMap<String,SimpleElement> _elementMap;

  ArrayList<SimpleAttribute> _attributeList;
  HashMap<String,SimpleAttribute> _attributeMap;
  
  LinkedHashMap<String, SimpleAttribute> _namespaceMap;
  
  SimpleNode _parent;
  
  public SimpleNode getParent()
  {
    return _parent;
  }
  
  public void setParent(SimpleNode parent)
  {
    _parent = parent;
  }
  
  public String getPrefix()
  {
    int i = _name.indexOf(':');
    
    if (i >= 0)
      return _name.substring(0, i);
    else
      return null;
  }
  
  public void removePrefix()
  {
    int i = _name.indexOf(':');
    
    if (i >= 0)
      _name = _name.substring(i + 1);
  }

  public String getQName()
  {
    return _name;
  }
  
  public String getName()
  {
    int i = _name.indexOf(':');
    
    if (i >= 0)
      return _name.substring(i + 1);
    else
      return _name;
  }

  public void setQName(String name)
  {
    _name = name;
  }

  public SimpleAttribute getNamespace()
  {
    return _namespace;
  }
  
  public SimpleAttribute getNamespace(String qName, boolean isRecursive)
  {
    SimpleAttribute namespace = _namespaceMap.get(qName);
    
    if (namespace != null)
      return namespace;
    
    if (isRecursive && getParent() != null)
      return _parent.getNamespace(qName, isRecursive);
    else
      return null;
  }
  
  public void setNamespace(String namespace, String prefix)
  {
    SimpleAttribute attr = createNamespaceAttribute(namespace, prefix);

    setNamespace(attr);
  }
  
  public void setNamespace(SimpleAttribute namespace)
  {
    if (_namespace == null)
      _namespace = namespace;
  }
  
  public LinkedHashMap<String,SimpleAttribute> getNamespaces()
  {
    return _namespaceMap;
  }
  
  public String getValue()
  {
    return _value;
  }
  
  public static SimpleAttribute createNamespaceAttribute(String namespace,
                                                         String prefix)
  {
    if (prefix != null)
      return new SimpleAttribute("xmlns:" + prefix, namespace);
    else
      return new SimpleAttribute("xmlns", namespace);
  }
  
  public void setValue(String value)
  {
    _value = value;
  }

  public final void addChild(SimpleNode child)
  { 
    if (child.isElement()) {
      SimpleElement element = (SimpleElement) child;

      _children.add(element);
      _elementList.add(element);

      SimpleElement existingChild = _elementMap.get(element.getName());

      if (existingChild == null) {
        _elementMap.put(element.getName(), element);
      }
      else if (existingChild.isElementList()) {
        SimpleElementList list = (SimpleElementList) existingChild;

        list.addSameNameSibling(element);
      }
      else {
        SimpleElementList list = new SimpleElementList();
        list.addSameNameSibling(existingChild);
        list.addSameNameSibling(element);

        _elementMap.put(element.getName(), list);
      }
    }
    else if (child.isText()) {
      _children.add(child);
    }
    else if (child.isAttribute()) {
      addAttribute((SimpleAttribute)child);
    }
  }

  public SimpleElement getElement(String name)
  {
    return _elementMap.get(name);
  }

  public ArrayList<SimpleNode> getChildren()
  {
    return _children;
  }

  public SimpleNode get(int index)
  {
    if (index == 0
        && _elementList.size() == 0
        && _children.size() > 0)
      return this;

    return null;
  }

  public ArrayList<SimpleElement> getElementList()
  {
    return _elementList;
  }

  public Map<String,SimpleElement> getElementMap()
  {
    return _elementMap;
  }

  public void removeChildren()
  {
    _children.clear();
    
    _elementList.clear();
    _elementMap.clear();
  }

  public final void addAttribute(String name, String value)
  {
    addAttribute(name, value, null);
  }

  public final void addAttribute(String name, String value, String namespace)
  {
    SimpleAttribute attr = new SimpleAttribute(name, value, namespace);

    addAttribute(attr);
  }

  public void addAttribute(SimpleAttribute attribute)
  {
    addAttributeImpl(attribute);
  }

  private void addAttributeImpl(SimpleAttribute attr)
  {
    if (getNamespace() == null) {
      String name;
      
      if (getPrefix() != null)
        name = "xmlns:" + getPrefix();
      else
        name = "xmlns";

      if (attr.getQName().equals(name)) {
        setNamespace(attr);
        addNamespace(attr);
        
        return;
      }
    }
    
    if ("xmlns".equals(attr.getPrefix())
        || attr.getPrefix() == null && attr.getName().equals("xmlns")) {
      addNamespace(attr);
    }
    else if (_attributeMap.get(attr.getQName()) == null) {
      _attributeMap.put(attr.getQName(), attr);
      _attributeList.add(attr);
      
      if (attr.getNamespace() != null)
        if (addNamespace(attr.getNamespace()) == false)
          attr.setNamespace(null);
    }
    else if (attr.getPrefix() != null) {
      attr.removePrefix();
      addAttributeImpl(attr);
    }
  }
  
  public boolean addNamespace(SimpleAttribute namespace)
  {
    if (namespace.getPrefix() == null) {
      if (_namespaceMap.get(namespace.getQName()) == null) {
        _namespaceMap.put(namespace.getQName(), namespace);
        
        return true;
      }
      else
        return false;
    }
    else if (getNamespace(namespace.getQName(), true) == null) {
      _namespaceMap.put(namespace.getQName(), namespace);
      
      return true;
    }
    else
      return false;
  }
  
  public SimpleAttribute getAttribute(String name)
  {
    return _attributeMap.get(name);
  }

  public ArrayList<SimpleAttribute> getAttributes()
  {
    return _attributeList;
  }

  public HashMap<String,SimpleAttribute> getAttributeMap()
  {
    return _attributeMap;
  }
  
  public boolean isSameNamespace(String namespace)
  {
    if (namespace == null || namespace.length() == 0)
      return true;

    if (getNamespace() == null)
      return false;
    
    return namespace.equals(getNamespace().getValue());
  }
  
  public boolean isSamePrefix(String prefix)
  {
    if (prefix == null || prefix.length() == 0)
      return true;

    return prefix.equals(getPrefix());
  }

  public boolean isText()
  {
    return false;
  }

  public boolean isAttribute()
  {
    return false;
  }

  public boolean isElement()
  {
    return false;
  }

  public boolean isElementList()
  {
    return false;
  }

  public int getObjectSize()
  {
    return 1;
  }

  public Iterator<SimpleElement> iterator()
  {
    if (_elementList != null)
      return _elementList.iterator();
    else
      return null;
  }
  
  public StringValue toXML(Env env)
  {
    StringValue sb = env.createBinaryBuilder();
    
    toXMLImpl(sb);
    
    return sb;
  }
  
  abstract protected void toXMLImpl(StringValue sb);

  public String toString()
  {
    if (_value != null)
      return _value;
    else
      return "";
  }

  public void varDumpImpl(Env env,
                   WriteStream out,
                   int depth,
                   IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    varDumpNested(env, out, depth, valueSet);
  }

  abstract void varDumpNested(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
    throws IOException;

  public void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
  throws IOException
  {
    printRNested(env, out, depth, valueSet);
  }

  abstract void printRNested(Env env,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
  throws IOException;

  final void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }
}
