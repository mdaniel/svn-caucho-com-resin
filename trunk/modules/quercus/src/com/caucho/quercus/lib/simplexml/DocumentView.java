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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

public class DocumentView extends SimpleView
{
  private final ElementView _element;

  public DocumentView(Document doc)
  {
    super(doc);

    Node node = doc.getDocumentElement();

    _element = new ElementView(node);
  }

  @Override
  public String getNodeName()
  {
    return _element.getNodeName();
  }

  @Override
  public ChildrenView getChildren(String namespace, String prefix)
  {
    return _element.getChildren(namespace, prefix);
  }

  @Override
  public AttributeListView getAttributes(String namespace)
  {
    return _element.getAttributes(namespace);
  }

  @Override
  public SimpleView addChild(Env env,
                             String name,
                             String value,
                             String namespace)
  {
    return _element.addChild(env, name, value, namespace);
  }

  @Override
  public void addAttribute(Env env,
                           String name,
                           String value,
                           String namespace)
  {
    _element.addAttribute(env, name, value, namespace);
  }

  @Override
  public SimpleView getIndex(Env env, Value indexV)
  {
    return _element.getIndex(env, indexV);
  }

  @Override
  public SimpleView setIndex(Env env, Value indexV, Value value)
  {
    return _element.setIndex(env, indexV, value);
  }

  @Override
  public SimpleView getField(Env env, Value indexV)
  {
    return _element.getField(env, indexV);
  }

  @Override
  public SimpleView setField(Env env, Value indexV, Value value)
  {
    return _element.setField(env, indexV, value);
  }

  @Override
  public List<SimpleView> xpath(Env env,
                                SimpleNamespaceContext context,
                                String expression)
  {
    return _element.xpath(env, context, expression);
  }

  @Override
  public int getCount()
  {
    return _element.getCount();
  }

  @Override
  public HashMap<String,String> getNamespaces(boolean isRecursive,
                                              boolean isFromRoot,
                                              boolean isCheckUsage)
  {
    return _element.getNamespaces(isRecursive, isFromRoot, isCheckUsage);
  }

  @Override
  public String toString(Env env)
  {
    return _element.toString(env);
  }

  @Override
  public Iterator<Map.Entry<IteratorIndex,SimpleView>> getIterator()
  {
    return _element.getIterator();
  }

  @Override
  public Set<Map.Entry<Value,Value>> getEntrySet(Env env, QuercusClass cls)
  {
    return _element.getEntrySet(env, cls);
  }

  @Override
  public boolean toXml(Env env, StringBuilder sb)
  {
    SimpleUtil.toXml(env, sb, getOwnerDocument());

    return true;
  }

  @Override
  public Value toDumpValue(Env env, QuercusClass cls, boolean isChildren)
  {
    return _element.toDumpValue(env, cls, true);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _element + "]";
  }
}
