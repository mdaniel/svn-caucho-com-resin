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
 * @author Emil Ong
 */

package com.caucho.util;

import java.lang.reflect.*;
import java.util.*;
import javax.xml.bind.*;

/**
 * JAXB utilities.
 */
public class JAXBUtil {
  public static void introspectClass(Class cl, 
                                     Collection<Class> jaxbClasses)
  {
    Method[] methods = cl.getMethods();

    for (Method method : methods)
      introspectMethod(method, jaxbClasses);
  }

  /**
   * Finds all the classes mentioned in a method signature (return type and
   * parameters) and adds them to the passed in classList.  Pass in a set if
   * you expect multiple references.
   */
  public static void introspectMethod(Method method, 
                                      Collection<Class> jaxbClasses)
  {
    introspectType(method.getReturnType(), jaxbClasses);

    Type[] params = method.getGenericParameterTypes();

    for (Type param : params)
      introspectType(param, jaxbClasses);
  }

  /**
   * Add all classes referenced by type to jaxbClasses.
   */
  private static void introspectType(Type type, Collection<Class> jaxbClasses)
  {
    if (type instanceof Class) {
      Class cl = (Class) type;

      if (! cl.isInterface())
        jaxbClasses.add((Class) type);
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      introspectType(pType.getRawType(), jaxbClasses);
      introspectType(pType.getOwnerType(), jaxbClasses);

      Type[] arguments = pType.getActualTypeArguments();

      for (Type argument : arguments)
        introspectType(argument, jaxbClasses);
    }
    else if (type != null) {
      // Type variables must be instantiated
      throw new UnsupportedOperationException("Method arguments cannot have " +
                                              "uninstantiated type variables " +
                                              "or wildcards (" + type + ")");
    }
  }
}
