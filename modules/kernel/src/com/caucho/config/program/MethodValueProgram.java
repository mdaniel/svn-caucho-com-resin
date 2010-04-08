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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.program;

import java.lang.reflect.*;

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.*;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.util.*;

/**
 * Injects a method with a constant value
 */
public class MethodValueProgram extends NamedProgram {
  private static final L10N L = new L10N(MethodValueProgram.class);
  
  private final Method _method;
  private final Object _value;

  public MethodValueProgram(Method method, Object value)
  {
    _method = method;
    _value = value;

    _method.setAccessible(true);
  }
  
  /**
   * Returns the injection name.
   */
  public String getName()
  {
    return _method.getName();
  }
  
  /**
   * Injects the bean with the dependencies
   */
  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
  {
    try {
      _method.invoke(bean, _value);
    } catch (IllegalArgumentException e) {
      throw new ConfigException(ConfigException.loc(_method) + L.l("Can't set method value '{0}'", _value), e);
    } catch (Exception e) {
      throw new ConfigException(ConfigException.loc(_method) + e.toString(), e);
    }
  }
}

