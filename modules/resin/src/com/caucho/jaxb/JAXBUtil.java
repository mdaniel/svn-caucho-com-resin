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

package com.caucho.jaxb;

import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * JAXB utilities.
 */
public class JAXBUtil {
  private static final Map<Class,String> _datatypeMap 
    = new HashMap<Class,String>();

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

    /* XXX: create wrappers
    Type[] exceptions = method.getGenericExceptionTypes();

    for (Type exception : exceptions)
      introspectType(exception, jaxbClasses);*/
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

  public static String classBasename(Class cl)
  {
    int i = cl.getName().lastIndexOf('$');

    if (i < 0)
      i = cl.getName().lastIndexOf('.');

    return cl.getName().substring(i + 1);
  }

  public static String getXmlSchemaDatatype(Class cl)
  {
    if (_datatypeMap.containsKey(cl))
      return _datatypeMap.get(cl);

    // XXX The JAXB standard seems to say that we should use the following: 
    //
    // String name = java.beans.Introspector.decapitalize(classBasename(cl));
    //
    // but the RI does simple decapitalization:

    String basename = classBasename(cl);
    String name = Character.toLowerCase(basename.charAt(0)) +
                  (basename.length() > 1 ? basename.substring(1) : "");

    if (cl.isAnnotationPresent(XmlType.class)) {
      XmlType xmlType = (XmlType) cl.getAnnotation(XmlType.class);

      return xmlType.name(); // XXX ""
    }

    return name;
  }

  static {
    _datatypeMap.put(String.class, "xsd:string");

    _datatypeMap.put(BigDecimal.class, "xsd:decimal");

    _datatypeMap.put(Boolean.class, "xsd:boolean");
    _datatypeMap.put(boolean.class, "xsd:boolean");

    _datatypeMap.put(Byte[].class, "xsd:base64Binary"); // XXX hexBinary
    _datatypeMap.put(byte[].class, "xsd:base64Binary"); // XXX hexBinary

    _datatypeMap.put(Byte.class, "xsd:byte");
    _datatypeMap.put(byte.class, "xsd:byte");

    _datatypeMap.put(Character.class, "xsd:unsignedShort");
    _datatypeMap.put(char.class, "xsd:unsignedShort");

    _datatypeMap.put(Calendar.class, "xsd:date");

    _datatypeMap.put(Double.class, "xsd:double");
    _datatypeMap.put(double.class, "xsd:double");

    _datatypeMap.put(Float.class, "xsd:float");
    _datatypeMap.put(float.class, "xsd:float");

    _datatypeMap.put(Integer.class, "xsd:int");
    _datatypeMap.put(int.class, "xsd:int");

    _datatypeMap.put(Long.class, "xsd:long");
    _datatypeMap.put(long.class, "xsd:long");

    _datatypeMap.put(Short.class, "xsd:short");
    _datatypeMap.put(short.class, "xsd:short");
  }
}
