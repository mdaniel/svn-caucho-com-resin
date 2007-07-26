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

import com.caucho.log.Log;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Attribute strategy for a setXXX or addXXX method.
 */
public class SetterAttributeStrategy extends AttributeStrategy {
  private static final Logger log = Log.open(SetterAttributeStrategy.class);
  private static final L10N L = new L10N(SetterAttributeStrategy.class);

  private final Method _setter;
  private final TypeStrategy _typeStrategy;

  public SetterAttributeStrategy(Method setter)
         throws Exception
  {
    if (! Modifier.isPublic(setter.getModifiers()))
      throw new IllegalStateException(L.l("'{0}' is not public.", setter.toString()));

    Class []param = setter.getParameterTypes();
    _setter = setter;
    _typeStrategy = TypeStrategyFactory.getTypeStrategy(param[0]);
  }

  /**
   * Creates an instance.
   */
  @Override
  public Object create(NodeBuilder builder, Object parent)
    throws Exception
  {
    return _typeStrategy.create();
  }

  /**
   * Configures the primitive value.
   *
   * @param builder the owning node builder
   * @param bean the bean to be configured
   * @param name the attribute name
   * @param node the configuration node
   */
  public void configure(NodeBuilder builder,
                        Object bean,
                        QName name,
                        Node node)
    throws Exception
  {
    Object value = _typeStrategy.configure(builder, node, bean);

    setAttribute(bean, name, value);
  }

  /**
   * Sets the named attribute value.
   *
   * @param bean the owning bean
   * @param name the attribute name
   * @param value the attribute value
   * @throws Exception
   */
  public void setAttribute(Object bean, QName name, Object value)
         throws Exception
  {
    try {
      _setter.invoke(bean, value);
    } catch (IllegalArgumentException e) {
      log.log(Level.FINE, e.toString(), e);

      throw new ConfigException(L.l("Can't assign {0} ({1}) to a {2}.",
				    value, value.getClass(), _setter.getParameterTypes()[0]),
                                e);
    }
  }

  public String toString()
  {
    return "SetterAttributeStrategy[" + _setter + "]";
  }
}
