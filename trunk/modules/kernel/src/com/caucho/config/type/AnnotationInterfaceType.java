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

package com.caucho.config.type;

import java.beans.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.PropertyStringProgram;
import com.caucho.config.*;
import com.caucho.config.annotation.NonEL;
import com.caucho.config.attribute.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.xml.*;
import com.caucho.vfs.*;
import com.caucho.xml.QName;

import org.w3c.dom.*;

/**
 * Represents an introspected bean type for configuration.
 */
public class AnnotationInterfaceType extends ConfigType
{
  private static final L10N L = new L10N(AnnotationInterfaceType.class);
  private static final Logger log
    = Logger.getLogger(AnnotationInterfaceType.class.getName());

  private static final QName TEXT = new QName("#text");

  private static final Object _introspectLock = new Object();

  private final Class _annClass;

  private HashMap<QName,Attribute> _nsAttributeMap
    = new HashMap<QName,Attribute>();

  private HashMap<String,Attribute> _attributeMap
    = new HashMap<String,Attribute>();

  public AnnotationInterfaceType(Class annClass)
  {
    _annClass = annClass;

    for (Method method : annClass.getMethods()) {
      if (method.getParameterTypes().length != 0)
        continue;
      else if (method.getName().equals("annotationType"))
        continue;


      boolean isEL = ! isAnnotationPresent(method.getAnnotations(),
                                           NonEL.class);

      _attributeMap.put(method.getName(),
                        new AnnotationAttribute(method.getName(),
                                                method.getReturnType(),
                                                isEL));
    }

    // createProxy(annClass);
  }

  private boolean isAnnotationPresent(Annotation []annList, Class annType)
  {
    for (int i = 0; i < annList.length; i++) {
      if (annList[i].annotationType().equals(annType))
        return true;
    }

    return false;
  }

  /**
   * Returns the given type.
   */
  public Class getType()
  {
    return _annClass;
  }

  /**
   * Creates a new instance of the type.
   */
  @Override
  public Object create(Object parent, QName name)
  {
    return new AnnotationConfig(this, _annClass);
  }

  /**
   * Returns the annotation
   */
  @Override
  public Object replaceObject(Object bean)
  {
    return ((AnnotationConfig) bean).replace();
  }

  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    AnnotationConfig ann = new AnnotationConfig(this, _annClass);

    if (! "".equals(text)) {
      // ioc/04e2
      Attribute attr = getAttribute(TEXT);

      if (attr == null)
        throw new ConfigException(L.l("'{0}' does not support value",
                                      this));

      attr.setText(ann, TEXT, text);
    }

    // ioc/2183

    return ann.replace();
  }

  /**
   * Returns the attribute with the given name.
   */
  public Attribute getAttribute(QName qName)
  {
    String name = qName.getLocalName();

    if ("#text".equals(name))
      name = "value";

    Attribute attr = _attributeMap.get(name);

    return attr;
  }

  public String toString(HashMap<String,Object> valueMap)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("@");
    sb.append(_annClass.getName());
    sb.append("(");

    boolean isFirst = true;
    for (Map.Entry<String,Object> entry : valueMap.entrySet()) {
      if (! isFirst)
        sb.append(",");
      isFirst = false;

      sb.append(entry.getKey());
      sb.append('=');
      sb.append(entry.getValue());
    }

    sb.append(")");

    return sb.toString();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _annClass.getName() + "]";
  }
}
