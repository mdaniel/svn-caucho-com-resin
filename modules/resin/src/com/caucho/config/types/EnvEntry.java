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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import com.caucho.util.L10N;

import com.caucho.config.BeanBuilderException;

import com.caucho.el.Expr;

import com.caucho.naming.Jndi;

/**
 * Configuration for the env-entry pattern.
 */
public class EnvEntry {
  private static L10N L = new L10N(EnvEntry.class);

  private String _name;
  private Class _type;
  private String _value;
  private String _description;

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the env-entry-name
   */
  public void setEnvEntryName(String name)
  {
    _name = name;
  }

  /**
   * Gets the env-entry-name
   */
  public String getEnvEntryName()
  {
    return _name;
  }

  /**
   * Sets the env-entry-type
   */
  public void setEnvEntryType(Class type)
  {
    _type = type;
  }

  /**
   * Gets the env-entry-type
   */
  public Class getEnvEntryType()
  {
    return _type;
  }

  /**
   * Sets the env-entry-value
   */
  public void setEnvEntryValue(String value)
  {
    _value = value;
  }

  /**
   * Gets the env-entry-value
   */
  public String getEnvEntryValue()
  {
    return _value;
  }

  /**
   * Gets the env-entry-value
   */
  public void init()
    throws Exception
  {
    if (_name == null)
      throw new BeanBuilderException(L.l("env-entry needs `env-entry-name' attribute"));
    if (_type == null)
      throw new BeanBuilderException(L.l("env-entry needs `env-entry-type' attribute"));
    if (_value == null)
      throw new BeanBuilderException(L.l("env-entry needs `env-entry-value' attribute"));

    Object value = _value;

    if (_type.equals(String.class)) {
    }
    else if (_type.equals(Boolean.class))
      value = new Boolean(Expr.toBoolean(_value, null));
    else if (_type.equals(Byte.class))
      value = new Byte((byte) Expr.toLong(_value, null));
    else if (_type.equals(Short.class))
      value = new Short((short) Expr.toLong(_value, null));
    else if (_type.equals(Integer.class))
      value = new Integer((int) Expr.toLong(_value, null));
    else if (_type.equals(Long.class))
      value = new Long(Expr.toLong(_value, null));
    else if (_type.equals(Float.class))
      value = new Float((float) Expr.toDouble(_value, null));
    else if (_type.equals(Double.class))
      value = new Double(Expr.toDouble(_value, null));

    if (_name.startsWith("java:comp"))
      Jndi.bindDeep(_name, value);
    else
      Jndi.bindDeep("java:comp/env/" + _name, value);
  }

  public String toString()
  {
    return "EnvEntry[" + _name + "]";
  }
}

