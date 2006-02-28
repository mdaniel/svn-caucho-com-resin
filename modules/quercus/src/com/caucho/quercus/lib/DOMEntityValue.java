/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.Entity;

public class DOMEntityValue extends DOMNodeValue {

  public DOMEntityValue(Entity entity)
  {
    _node = entity;
  }
  @Override
  public Value getField(String name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if ("publicId".equals(name)) {
      return new StringValue(((Entity)_node).getPublicId());
    } else if ("systemId".equals(name)) {
      return new StringValue(((Entity)_node).getSystemId());
    } else if ("notationName".equals(name)) {
      return new StringValue(((Entity)_node).getNotationName());
    } else if ("actualEncoding".equals(name)) {
      return new StringValue(((Entity)_node).getInputEncoding());
    } else if ("encoding".equals(name)) {
      return new StringValue(((Entity)_node).getXmlEncoding());
    } else if ("version".equals(name)) {
      return new StringValue(((Entity)_node).getXmlVersion());
    }
    
    return NullValue.NULL;
  }
}
