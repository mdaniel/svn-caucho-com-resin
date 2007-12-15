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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.util.L10N;

import javax.naming.InitialContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JndiInjectProgram extends BuilderProgram {
  private static final L10N L = new L10N(JndiInjectProgram.class);

  private String _jndiName;
  private Method _method;

  public JndiInjectProgram(String jndiName, Method method)
  {
    _jndiName = jndiName;
    _method = method;

    method.setAccessible(true);
  }

  @Override
  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    try {
      Object value = new InitialContext().lookup(_jndiName);

      _method.invoke(bean, value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public Object configureImpl(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _method + "]";
  }
}
