/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * afloat with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.jaxb;

import java.util.*;
import java.lang.reflect.*;

import javax.el.*;
import javax.xml.bind.annotation.adapters.*;

import org.w3c.dom.Node;

import com.caucho.util.*;

import com.caucho.el.*;

import com.caucho.config.*;
import com.caucho.xml.*;

public class AdapterType extends TypeStrategy {
  private final TypeStrategy _valueType;
  private final XmlAdapter _adapter;
  
  public AdapterType(TypeStrategy valueType,
		     XmlAdapter adapter)
  {
    _valueType = valueType;
    _adapter = adapter;
  }

  /**
   * Returns the type class.
   */
  public Class getType()
  {
    return List.class;
  }

  /**
   * Returns the type name.
   */
  public String getTypeName()
  {
    return getType().getName();
  }

  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  public Object configure(NodeBuilder builder, Node node, Object parent)
    throws Exception
  {
    Object bean = _valueType.configure(builder, node, parent);

    return _adapter.unmarshal(bean);
  }

  /**
   * Returns the appropriate strategy for the bean.
   *
   * @param attrName
   * @return the strategy
   * @throws ConfigException
   */
  public AttributeStrategy getAttributeStrategy(QName attrName)
    throws Exception
  {
    return _valueType.getAttributeStrategy(attrName);
  }
}
