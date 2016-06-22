/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.simplexml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class SelectedView extends SimpleView
{
  private final SimpleView _parent;
  private final String _nodeName;

  private final ArrayList<SimpleView> _childList;
  private final ArrayList<AttributeView> _attrList;

  public SelectedView(SimpleView parent,
                      String nodeName,
                      ArrayList<SimpleView> childList,
                      ArrayList<AttributeView> attrList)
  {
    super(parent.getOwnerDocument());

    _parent = parent;
    _nodeName = nodeName;

    _childList = childList;
    _attrList = attrList;
  }

  @Override
  public String getNodeName()
  {
    if (_childList.size() > 0) {
      return _childList.get(0).getNodeName();
    }
    else {
      return null;
    }
  }

  @Override
  public ChildrenView getChildren(String namespace, String prefix)
  {
    if (_childList.size() > 0) {
      return _childList.get(0).getChildren(namespace, prefix);
    }
    else {
      return null;
    }
  }

  @Override
  public AttributeListView getAttributes(String namespace)
  {
    if (_childList.size() > 0) {
      return _childList.get(0).getAttributes(namespace);
    }
    else {
      return null;
    }
  }

  @Override
  public SimpleView addChild(Env env,
                             String name,
                             String value,
                             String namespace)
  {
    if (_childList.size() > 0) {
      return _childList.get(0).addChild(env, name, value, namespace);
    }
    else {
      return null;
    }
  }

  @Override
  public HashMap<String,String> getNamespaces(boolean isRecursive,
                                              boolean isFromRoot,
                                              boolean isCheckUsage)
  {
    if (_childList.size() > 0) {
      return _childList.get(0).getNamespaces(isRecursive, isFromRoot, isCheckUsage);
    }
    else {
      return null;
    }
  }

  @Override
  public SimpleView getIndex(Env env, Value indexV)
  {
    if (indexV.isString()) {
      if (_childList.size() > 0) {
        return _childList.get(0).getIndex(env, indexV);
      }
      else {
        return null;
      }
    }
    else {
      int index = indexV.toInt();

      if (index < _childList.size()) {
        return _childList.get(index);
      }
      else {
        return null;
      }
    }
  }

  @Override
  public SimpleView setIndex(Env env, Value indexV, Value value)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public SimpleView getField(Env env, Value indexV)
  {
    if (_childList.size() > 0) {
      return _childList.get(0).getField(env, indexV);
    }
    else {
      return null;
    }
  }

  @Override
  public SimpleView setField(Env env, Value indexV, Value value)
  {
    if (_childList.size() > 0) {
      SimpleView firstChild = _childList.get(0);

      return firstChild.setField(env, indexV, value);
    }
    else {
      return null;
    }
  }

  @Override
  public int getCount()
  {
    return _childList.size();
  }

  @Override
  public List<SimpleView> xpath(Env env,
                                SimpleNamespaceContext context,
                                String expression)
  {
    if (_childList.size() > 0) {
      SimpleView firstChild = _childList.get(0);

      return firstChild.xpath(env, context, expression);
    }
    else {
      return null;
    }
  }

  @Override
  public String toString(Env env)
  {
    if (_childList.size() > 0) {
      SimpleView firstChild = _childList.get(0);

      return firstChild.toString(env);
    }
    else {
      return "";
    }
  }

  @Override
  public Iterator<Map.Entry<IteratorIndex,SimpleView>> getIterator()
  {
    LinkedHashMap<IteratorIndex,SimpleView> map
      = new LinkedHashMap<IteratorIndex,SimpleView>();

    for (int i = 0; i < _childList.size(); i++) {
      SimpleView view = _childList.get(i);

      map.put(IteratorIndex.create(view.getNodeName()), view);
    }

    return map.entrySet().iterator();
  }

  @Override
  public Set<Map.Entry<Value,Value>> getEntrySet(Env env, QuercusClass cls)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean toXml(Env env, StringBuilder sb)
  {
    if (_childList.size() > 0) {
      SimpleView firstChild = _childList.get(0);

      firstChild.toXml(env, sb);

      return true;
    }
    else {
      return false;
    }
  }

  @Override
  public Value toDumpValue(Env env, QuercusClass cls, boolean isChildren)
  {
    int childSize = _childList.size();

    if (_nodeName != null && childSize == 1) {
      SimpleView child = _childList.get(0);
      Value childValue = child.toDumpValue(env, cls, true);

      return childValue;
    }

    ObjectValue obj = env.createObject();
    obj.setClassName(cls.getName());

    if (childSize > 0) {
      for (int i = 0; i < childSize; i++) {
        SimpleView child = _childList.get(i);

        Value childValue = child.toDumpValue(env, cls, false);

        if (_nodeName != null) {
          obj.putField(env, env.createString(i), childValue);
        }
        else {
          StringValue nodeName = env.createString(child.getNodeName());

          obj.putField(env, nodeName, childValue);
        }

        /*
        Value existing = obj.getField(env, nodeName);

        if (existing == UnsetValue.UNSET) {
          obj.putField(env, nodeName, childValue);
        }
        else if (existing.isArray()) {
          existing.toArrayValue(env).append(childValue);
        }
        else {
          ArrayValue array = new ArrayValueImpl();

          array.append(existing);
          array.append(childValue);

          obj.putField(env, nodeName, array);
        }
        */
      }
    }
    else if (_attrList.size() > 0) {
      ArrayValue array = new ArrayValueImpl();

      for (AttributeView view : _attrList) {
        StringValue attrName = env.createString(view.getNodeName());
        StringValue attrValue = env.createString(view.getNodeValue());

        array.append(attrName, attrValue);
      }

      obj.putField(env, env.createString("@attributes"), array);
    }

    return obj;
  }

  @Override
  public String toString()
  {
    SimpleView firstChild = null;

    if (_childList.size() > 0) {
      firstChild = _childList.get(0);
    }

    return getClass().getSimpleName() + "[name=" + _nodeName + ",first=" + firstChild + ",parent=" + _parent + "]";
  }
}
