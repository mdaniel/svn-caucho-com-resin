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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

public class AttributeListView extends SimpleView
{
  private final ArrayList<AttributeView> _attrList;

  public AttributeListView(Document doc, ArrayList<AttributeView> attrList)
  {
    super(doc);

    _attrList = attrList;
  }

  @Override
  public String getNodeName()
  {
    if (_attrList.size() > 0) {
      return _attrList.get(0).getNodeName();
    }
    else {
      return "";
    }
  }

  @Override
  public SimpleView getIndex(Env env, Value indexV)
  {
    if (indexV.isString()) {
      String nodeName = indexV.toString();

      for (AttributeView view : _attrList) {
        if (view.getNodeName().equals(nodeName)) {
          return view;
        }
      }

      return null;
    }
    else {
      int index = indexV.toInt();

      if (index < _attrList.size()) {
        return _attrList.get(index);
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
    throw new UnsupportedOperationException();
  }

  @Override
  public SimpleView setField(Env env, Value indexV, Value value)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(Env env)
  {
    return _attrList.get(0).toString(env);
  }

  @Override
  public Iterator<Map.Entry<IteratorIndex,SimpleView>> getIterator()
  {
    LinkedHashMap<IteratorIndex,SimpleView> map
      = new LinkedHashMap<IteratorIndex,SimpleView>();

    for (int i = 0; i < _attrList.size(); i++) {
      SimpleView view = _attrList.get(i);

      map.put(IteratorIndex.create(view.getNodeName()), view);
    }

    return map.entrySet().iterator();
  }

  @Override
  public Set<Map.Entry<Value,Value>> getEntrySet(Env env, QuercusClass cls)
  {
    LinkedHashMap<Value,Value> map
      = new LinkedHashMap<Value,Value>();

    if (_attrList.size() > 0) {
      ArrayValue array = new ArrayValueImpl();

      for (AttributeView view : _attrList) {
        String name = view.getNodeName();
        String value = view.getNodeValue();

        array.put(env.createString(name),
                  env.createString(value));
      }

      map.put(env.createString("@attributes"), array);
    }

    return map.entrySet();
  }

  @Override
  public boolean toXml(Env env, StringBuilder sb)
  {
    if (_attrList.size() > 0) {
      SimpleView attr = _attrList.get(0);

      attr.toXml(env, sb);

      return true;
    }
    else {
      return false;
    }
  }

  @Override
  public Value toDumpValue(Env env, QuercusClass cls, boolean isChildren)
  {
    ObjectValue obj = env.createObject();
    obj.setClassName(cls.getName());

    Set<Map.Entry<Value,Value>> set = getEntrySet(env, cls);

    for (Map.Entry<Value,Value> entry : set) {
      obj.putField(env, entry.getKey().toString(), entry.getValue());
    }

    return obj;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _attrList + "]";
  }
}
