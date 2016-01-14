/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.type;

import com.caucho.v5.config.ConfigException;

public class NamespaceBeanConfig {
  private TypeFactoryConfig _factory;

  private String _name;
  private String _className;

  private ConfigType<?> _configType;
  private ClassLoader _loader;

  NamespaceBeanConfig(TypeFactoryConfig factory, String ns, boolean isDefault)
  {
    _factory = factory;
    // _loader = Thread.currentThread().getContextClassLoader();
    _loader = factory.getClassLoader();
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public void setClass(String className)
  {
    _className = className;
  }

  public ConfigType<?> getConfigType()
  {
    try {
      if (_configType == null) {
        Class<?> cl = Class.forName(_className, false, _loader);

        ConfigType<?> type = _factory.createType(cl);

        // ioc/0401
        type.setEnvBean(true);
        type.introspect();

        _configType = type;
      }

      return _configType;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /*
  @PostConstruct
  public void init()
  {
    if (_name == null)
      throw new ConfigException(L.l("bean requires a 'name' attribute"));

    if (_className == null)
      throw new ConfigException(L.l("bean requires a 'class' attribute"));
  }
  */
}
