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
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import com.caucho.v5.util.L10N;

import javax.interceptor.InvocationContext;

import java.util.HashMap;

public abstract class AbstractInvocationContext implements InvocationContext
{
  private static final L10N L = new L10N(AbstractInvocationContext.class);
  private static final HashMap<Class<?>,Class<?>> _boxMap = new HashMap<>();

  private Object []_parameters;

  public AbstractInvocationContext(Object []args)
  {
    _parameters = args;
  }

  @Override
  public Object []getParameters() throws IllegalStateException
  {
    return _parameters;
  }

  protected abstract Class<?> []getParameterTypes();

  protected abstract Class<?> getDeclaringClass();

  protected abstract String getName();

  @Override
  public final void setParameters(Object []parameters)
    throws IllegalStateException
  {
    Class<?> []paramType = getParameterTypes();

    if (parameters == null) {
      throw new IllegalArgumentException(L.l(
        "{0}.{1}: interception parameters cannot be null",
        getDeclaringClass().getName(),
        getName()));
    }

    if (paramType.length != parameters.length) {
      throw new IllegalArgumentException(L.l(
        "{0}.{1}: interception parameters length '{2}' do not match the expected '{3}'",
        getDeclaringClass().getName(),
        getName(),
        parameters.length,
        paramType.length));
    }

    for (int i = paramType.length - 1; i >= 0; i--) {
      Object value = parameters[i];

      Class<?> argType = getArgType(paramType[i]);

      if (value != null && !argType.isAssignableFrom(value.getClass())) {
        throw new IllegalArgumentException(L.l(
          "{0}.{1}: interception parameters '{2}' do not match the expected '{3}'",
          getDeclaringClass().getName(),
          getName(),
          value,
          paramType[i].getName()));
      }
    }

    _parameters = parameters;
  }

  private Class<?> getArgType(Class<?> type)
  {
    Class<?> boxType = _boxMap.get(type);

    if (boxType != null)
      return boxType;
    else
      return type;
  }

  static {
    _boxMap.put(boolean.class, Boolean.class);
    _boxMap.put(char.class, Character.class);
    _boxMap.put(byte.class, Byte.class);
    _boxMap.put(short.class, Short.class);
    _boxMap.put(int.class, Integer.class);
    _boxMap.put(long.class, Long.class);
    _boxMap.put(float.class, Float.class);
    _boxMap.put(double.class, Double.class);
  }
}
