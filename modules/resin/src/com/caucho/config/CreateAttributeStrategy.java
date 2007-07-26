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
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config;

import com.caucho.util.L10N;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

import java.lang.reflect.Method;

public class CreateAttributeStrategy extends AttributeStrategy {
  static final L10N L = new L10N(CreateAttributeStrategy.class);

  private Method _createMethod;
  private Method _setterMethod;

  public CreateAttributeStrategy(Method createMethod, Method setter)
  {
    _createMethod = createMethod;
    _setterMethod = setter;
  }

  /**
   * Gets the attribute's method
   */
  public Method getCreateMethod()
  {
    return _createMethod;
  }

  /**
   * Creates an instance.
   */
  @Override
  public Object create(NodeBuilder builder, Object parent)
    throws Exception
  {
    return _createMethod.invoke(parent);
  }

  /**
   * Configures the attribute with the given configuration node.
   *
   * @param builder the node builder context
   * @param bean the bean to be configured
   * @param name the attribute name
   * @param node the configuration node
   * @throws Exception
   */

  public void configure(NodeBuilder builder,
                        Object bean,
                        QName name,
                        Node node)
          throws Exception
  {
    // server/23j0
    Object child = builder.createResinType(node);

    if (child == null)
      child = _createMethod.invoke(bean);

    TypeStrategy childStrategy;

    childStrategy = TypeStrategyFactory.getTypeStrategy(child.getClass());

    child = builder.configureImpl(childStrategy, child, node);

    setChild(bean, name, child);
  }

  /**
   * Creates the child instance.
   */
  public Object create(Object parent)
    throws Exception
  {
    return _createMethod.invoke(parent, new Object[0]);
  }

  /**
   * Sets the child object.
   */
  public void setChild(Object bean, QName name, Object child)
    throws Exception
  {
    if (_setterMethod != null) {
      try {
        _setterMethod.invoke(bean, new Object[] { child });
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(_setterMethod.getName() + ": " + e);
      }
    }
  }

  /**
   * Sets the child object.
   */
  public void setAttribute(Object bean, QName name, Object child)
    throws Exception
  {
    if (_setterMethod != null) {
      try {
        _setterMethod.invoke(bean, new Object[] { child });
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(_setterMethod.getName() + ": " + e);
      }
    }
  }
}
