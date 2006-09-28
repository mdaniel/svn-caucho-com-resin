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
  /**
   * Finds all the classes mentioned in a method signature (return type and
   * parameters) and adds them to the passed in classList.
   */
  public static void introspectMethod(Method method, ArrayList<Class> classList)
  {
    // XXX do generic types
    classList.add(method.getReturnType());

    for (Class cl : method.getParameterTypes())
      classList.add(cl);
  }

  public static void introspectClass(Class cl, ArrayList<Class> classList)
  {
    Method[] methods = cl.getMethods();

    for (Method method : methods)
      introspectMethod(method, classList);
  }
}
