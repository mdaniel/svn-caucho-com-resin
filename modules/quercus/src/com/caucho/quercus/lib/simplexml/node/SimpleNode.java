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
import com.caucho.quercus.env.Value;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * Represents the actual SimpleXML node implementation.
 */
public abstract class SimpleNode
{
  String _namespace;
  String _prefix;
  
  String _name;
  String _value;

  ArrayList<SimpleNode> _children;
  ArrayList<SimpleElement> _elementList;

  HashMap<String,SimpleElement> _elementMap;
  HashMap<String,SimpleAttribute> _attributeMap;

  public String getNamespace()
  {
    return _namespace;
  }

  public void setNamespace(String namespace)
  {
    _namespace = namespace;
  }
  
  public String getPrefix()
  {
    return _prefix;
  }
  
  public void setPrefix(String prefix)
  {
    _prefix = prefix;
  }

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    int i = name.indexOf(':');
    
    if (i >= 0) {
      setPrefix(name.substring(0, i));
      name = name.substring(i + 1);
    }

    _name = name;
  }

  public String getValue()
  {
    return _value;
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

  HashMap<String,SimpleElement> getElementMap()
  {
    return _elementMap;
  }

  public void removeChildren()
  {
    _elementList.clear();
    _children.clear();
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
    if (getPrefix() != null && attribute.getPrefix().equals("xmlns")) {
      if (getPrefix().equals(attribute.getName())) {
        setNamespace(attribute.getValue());    
        return;
      }
    }
    
    if (attribute.getName().equals("xmlns")
        && attribute.getPrefix() == null) {
      setNamespace(attribute.getValue());
    }
    else if (_attributeMap.get(attribute.getName()) == null) {
      _attributeMap.put(attribute.getName(), attribute);
      
      if (attribute.getPrefix() != null
          && attribute.getNamespace() != null) {
        SimpleAttribute nsAttr = new SimpleAttribute(attribute.getPrefix(),
                                                     attribute.getNamespace(),
                                                     null);

        nsAttr.setPrefix("xmlns");
        
        addAttribute(nsAttr);
      }
    }
  }

  public SimpleAttribute getAttribute(String name)
  {
    return _attributeMap.get(name);
  }

  public HashMap<String,SimpleAttribute> getAttributes()
  {
    return _attributeMap;
  }

  public boolean isSameNamespace(String namespace)
  {
    if (namespace == null || namespace.length() == 0)
      return true;

    return namespace.equals(_namespace);
  }
  
  public boolean isSamePrefix(String prefix)
  {
    if (prefix == null || prefix.length() == 0)
      return true;

    return prefix.equals(_prefix);
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
    return _elementList.iterator();
  }
  
  public String toXML()
  {
    StringBuilder sb = new StringBuilder();
    
    toXMLImpl(sb);
    
    return sb.toString();
  }
  
  abstract protected void toXMLImpl(StringBuilder sb);

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
