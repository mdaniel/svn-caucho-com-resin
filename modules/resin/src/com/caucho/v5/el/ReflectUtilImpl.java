/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Paul Cowan
 */

package com.caucho.v5.el;

import java.lang.reflect.*;
import java.util.*;

import javax.el.*;

public class ReflectUtilImpl extends javax.el.ReflectUtil
{
  public ReflectUtilImpl()
  {

  }

  @Override
  protected Constructor<?> findConstructor(Class<?> cl,
                                        Class<?> []paramTypes,
                                        Object []params)
  {
    if (paramTypes != null) {
      try {
        Constructor<?> constructor = cl.getConstructor(paramTypes);
        if (Modifier.isPublic(cl.getModifiers())) {
          return constructor;
        }
      } catch (NoSuchMethodException e) {
      }
    }

    int numParams = params == null ? 0 : params.length;
    for (Constructor<?> constructor: cl.getConstructors()) {
      if (constructor.isVarArgs() ||
          constructor.getParameterTypes().length == numParams) {
        if (Modifier.isPublic(cl.getModifiers())) {
          return constructor;
        }
      }
    }

    throw new MethodNotFoundException(
      String.format("constructor '%s.%s' not found",
                    cl.getName(),
                    "<init>"));
  }

  @Override
  protected Object invokeConstructor(ELContext env,
                                     Constructor<?> constructor,
                                     Object []params)
  {
    Object []marshalledParams = marshal(env,
                                        params,
                                        constructor.getParameterTypes());
    try {
      return constructor.newInstance(marshalledParams);
    } catch (ELException e) {
      throw e;
    } catch (InstantiationException e) {
      throw new ELException(e.getCause());
    } catch (InvocationTargetException e) {
      throw new ELException(String.format("failed to invoke constructor '%s'",
                                          constructor), e.getCause());
    } catch (Exception e) {
      throw new ELException(String.format("failed to invoke constructor '%s'",
                                          constructor), e);
    }
  }

  @Override
  protected Method findMethod(Class<?> cl,
                              String methodName,
                              Class<?> []paramTypes,
                              Object []params,
                              boolean staticOnly)
  {
    if (paramTypes == null && params != null) {
      paramTypes = new Class<?>[params.length];

      for (int i = 0; i < params.length; i++) {
        paramTypes[i] = params[i].getClass();
      }
    }

    if (paramTypes != null) {
      try {
        Method method = cl.getMethod(methodName, paramTypes);
        int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers) && (! staticOnly ||
                                             Modifier.isStatic(modifiers))) {
          return method;
        }
      } catch (NoSuchMethodException e)  {
      }
    }

    List<Method> canidates = new ArrayList<>();

    int numParams = params == null ? 0 : params.length;
    for (Method method: cl.getMethods()) {
      if (method.getName().equals(methodName) &&
          method.getParameterTypes().length == numParams) {
        int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers) && (! staticOnly ||
                                             Modifier.isStatic(modifiers))) {
          canidates.add(method);
        }
      }
    }

    if (canidates.isEmpty())
      throw new MethodNotFoundException(
        String.format("method '%s.%s' not found",
                      cl.getName(),
                      methodName));

    if (canidates.size() == 1)
      return canidates.get(0);

    for (Method method : canidates) {
      if (parametersMatch(method.getParameterTypes(), paramTypes)) {
        return method;
      }
    }

    return canidates.get(0);
  }

  private static boolean parametersMatch(Class<?> []methodParams,
                                         Class<?> []searchParams)
  {
    for (int i=0; i<methodParams.length; i++) {
      if (! methodParams[i].isAssignableFrom(searchParams[i])) {
        return false;
      }
    }

    return true;
  }

  @Override
  protected Object invokeMethod(ELContext env,
                                Method method,
                                Object base,
                                Object []params)
  {
    Object []marshalledParams = marshal(env,
                                        params,
                                        method.getParameterTypes());

    try {
      return method.invoke(base, marshalledParams);
    } catch (ELException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw new ELException(String.format("failed to invoke method '%s'",
                                          method), e.getCause());
    } catch (Exception e) {
      throw new ELException(String.format("failed to invoke method '%s'",
                                          method), e);
    }
  }

  private static Object []marshal(ELContext env,
                                  Object []params,
                                  Class<?> []paramTypes)
  {
    Object []marshalledParams = new Object[paramTypes.length];

    try {
      if (paramTypes.length > 0) {
        for (int i=0; i<paramTypes.length; i++) {
//          Class<?> paramType = paramTypes[i];
// TODO: need to add handling for varargs
//          if (paramType.isArray())
//            paramType = paramType.getComponentType();
          marshalledParams[i] = env.convertToType(params[i], paramTypes[i]);
        }
      }
    } catch (Exception e) {
      throw new ELException(e);
    }

    return marshalledParams;
  }

}
