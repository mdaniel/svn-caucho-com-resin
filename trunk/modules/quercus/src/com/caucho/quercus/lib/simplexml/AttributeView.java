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

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.WriteStream;

public class AttributeView extends SimpleView
{
  private final Attr _attr;

  public AttributeView(Attr attr)
  {
    super(attr.getOwnerDocument());

    _attr = attr;
  }

  @Override
  public String getNodeName()
  {
    return _attr.getNodeName();
  }

  @Override
  public String getNodeValue()
  {
    return _attr.getNodeValue();
  }

  @Override
  public SimpleView getIndex(Env env, Value indexV)
  {
    throw new UnsupportedOperationException();
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
    return _attr.getNodeValue();
  }

  @Override
  public Set<Map.Entry<Value,Value>> getEntrySet(Env env, QuercusClass cls)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean toXml(Env env, StringBuilder sb)
  {
    sb.append(' ');
    sb.append(_attr.getNodeName());

    sb.append('=');

    sb.append('"');
    sb.append(_attr.getNodeValue());
    sb.append('"');

    return true;
  }

  @Override
  public Value toDumpValue(Env env, QuercusClass cls, boolean isChildren)
  {
    ObjectValue obj = env.createObject();
    obj.setClassName(cls.getName());

    StringValue value = env.createString(getNodeValue());

    obj.putField(env, env.createString("0"), value);

    return obj;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _attr + "]";
  }
}
