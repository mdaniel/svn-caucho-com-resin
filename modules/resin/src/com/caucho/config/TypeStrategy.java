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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config;

import com.caucho.util.L10N;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

/**
 * Strategy for configuring types.
 */
public abstract class TypeStrategy {
  protected static final L10N L = new L10N(TypeStrategy.class);

  /**
   * Returns the type name.
   */
  public String getTypeName()
  {
    return getClass().getName();
  }
  
  /**
   * Creates a new instance of the type.
   */
  public Object create()
    throws Exception
  {
    return null;
  }

  /**
   * Sets the object's parent
   *
   * @param parent the parent
   */
  public void setParent(Object bean, Object parent)
    throws Exception
  {
  }

  /**
   * Called before the children are configured.
   */
  public void beforeConfigure(NodeBuilder builder, Object bean)
  {
  }

  /**
   * Return the attribute strategy for the given name.
   *
   * @param attrName the configuration attribute name
   * @return the attribute strategy or null if no strategy
   */
  public AttributeStrategy getAttributeStrategy(QName attrName)
          throws Exception
  {
    /*
    throw new ConfigException(L.l("'{0}' is an unknown attribute of {1}.",
                                  attrName.getName(),
				  getTypeName()));
    */
    return null;
  }

  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  abstract public Object configure(NodeBuilder builder, Node node, Object parent)
    throws Exception;

  /**
   * Configures the bean
   *
   * @param builder the context builder
   * @param bean the bean to be configured
   * @param top the configuration node
   */
  public void configureBean(NodeBuilder builder, Object bean, Node top)
    throws Exception
  {
    //builder.configureBeanImpl(this, bean, top);
  }

  /**
   * Configures based on an attribute.
   */
  public void configureAttribute(NodeBuilder builder, Object bean, Node attr)
    throws Exception
  {
    //builder.configureAttributeImpl(this, bean, attr);
  }

  /**
   * Initialize the bean
   */
  public void init(Object bean)
    throws Exception
  {
  }

  /**
   * Factory replacement
   */
  public Object replaceObject(Object bean)
    throws Exception
  {
    return bean;
  }
}
