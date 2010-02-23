/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.el.Expr;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the env-entry pattern.
 */
public class EnvEntry extends ResourceGroupConfig implements Validator {
  private static final L10N L = new L10N(EnvEntry.class);
  private static final Logger log = Logger.getLogger(EnvEntry.class.getName());

  private String _name;
  private Class _type;
  private String _value;

  private Object _objValue;

  public EnvEntry()
  {
  }

  public void setId(String id)
  {
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
  // XXX: ejb/0fd0 vs ejb/0g03
  // PostConstruct called from com.caucho.ejb.cfg.EjbSessionBean.
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_name == null)
      throw new ConfigException(L.l("env-entry needs 'env-entry-name' attribute"));
    if (_type == null)
      throw new ConfigException(L.l("env-entry needs 'env-entry-type' attribute"));

    super.init();

    // actually, should register for validation
    if (_value == null)
      return;

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
    else if (_type.equals(Character.class)) {
      String v = Expr.toString(_value, null);

      if (v == null || v.length() == 0)
        value = null;
      else
        value = new Character(v.charAt(0));
    }

    _objValue = value;

    InjectManager webBeans = InjectManager.create();
    BeanFactory factory = webBeans.createBeanFactory(value.getClass());
    factory.name(_name);
    // server/1516
    factory.binding(Names.create(_name));

    webBeans.addBean(factory.singleton(value));

    Jndi.bindDeepShort(_name, value);
  }

  /**
   * Validates the resource-ref, i.e. checking that it exists in
   * JNDI.
   */
  public void validate()
    throws ConfigException
  {
    Object obj = null;

    try {
      obj = new InitialContext().lookup("java:comp/env/" + _name);
    } catch (NamingException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (obj == null)
      throw error(L.l("env-entry '{0}' was not configured.  All resources defined by <env-entry> tags must be defined in a configuration file.",
                      _name));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

