/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Scott Ferguson
 */

package com.caucho.config.type;

import java.lang.reflect.Method;

import org.w3c.dom.Node;

import com.caucho.config.ConfigException;
import com.caucho.config.core.ContextConfig;
import com.caucho.xml.QNode;

/**
 * Represents an inline bean type for configuration.
 */
public class InlineBeanTypeResin<T> extends InlineBeanType<T>
{
  // XXX: needs update for introspect
  private Method _setConfigNode;

  public InlineBeanTypeResin(TypeFactoryConfig typeFactory, Class<T> beanClass)
  {
    super(typeFactory, beanClass);
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(ContextConfig env, Object bean, Node node)
  {
    super.beforeConfigure(env, bean, node);

    if (_setConfigNode != null) {
      try {
        _setConfigNode.invoke(bean, node);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    if (node instanceof QNode) {
      QNode qNode = (QNode) node;
      
      setLocation(bean, qNode.getBaseURI(), qNode.getLine());
    }
  }
}
