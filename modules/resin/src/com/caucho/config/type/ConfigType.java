/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.type;

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.util.*;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

/**
 * Represents an introspected configuration type.
 */
abstract public class ConfigType
{
  private static final L10N L = new L10N(ConfigType.class);
  
  /**
   * Returns the Java type.
   */
  abstract public Class getType();

  /**
   * Introspect the type.
   */
  public void introspect()
  {
  }

  /**
   * Returns a printable name of the type.
   */
  public String getTypeName()
  {
    return getType().getSimpleName();
  }
  
  /**
   * Creates a new instance of the type.
   */
  public Object create(Object parent)
  {
    return null;
  }
  
  /**
   * Initialize the type
   */
  public void init(Object bean)
  {
  }
  
  /**
   * Replace the type with the generated object
   */
  public Object replaceObject(Object bean)
  {
    return bean;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  abstract public Object valueOf(String text);
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof String)
      return valueOf((String) value);
    else
      return value;
  }

  /**
   * Returns true for a bean-style type.
   */
  public boolean isBean()
  {
    return false;
  }

  /**
   * Return true for non-trim.
   */
  public boolean isNoTrim()
  {
    return false;
  }

  /**
   * Returns true for a program type.
   */
  public boolean isProgram()
  {
    return BuilderProgram.class.isAssignableFrom(getType());
  }

  /**
   * Returns the attribute with the given name.
   */
  public Attribute getAttribute(QName qName)
  {
    throw new ConfigException(L.l("{0} does not allow attributes at '{1}'",
				  getTypeName(), qName));
  }

  /**
   * Returns the program attribute.
   */
  public Attribute getProgramAttribute()
  {
    return null;
  }

  /**
   * Called before the children are configured.
   */
  public void beforeConfigureBean(NodeBuilder builder, Object bean, Node node)
  {
  }

  /**
   * Called before the children are configured.  Also called for
   * attribute configuration, e.g. for macros and web-app-default.
   */
  public void beforeConfigure(NodeBuilder builder, Object bean, Node node)
  {
  }

  /**
   * Called after the children are configured.
   */
  public void afterConfigure(NodeBuilder builder, Object bean)
  {
  }
}
