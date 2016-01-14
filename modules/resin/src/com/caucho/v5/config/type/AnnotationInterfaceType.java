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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Qualifier;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.annotation.NonEL;
import com.caucho.v5.config.attribute.AnnotationAttribute;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.types.AnnotationConfig;
import com.caucho.v5.util.L10N;

/**
 * Represents an introspected bean type for configuration.
 */
public class AnnotationInterfaceType<T> extends ConfigType<T>
{
  private static final L10N L = new L10N(AnnotationInterfaceType.class);

  private static final NameCfg TEXT = new NameCfg("#text");

  private final Class<T> _annClass;

  private HashMap<String,AttributeConfig> _attributeMap
    = new HashMap<String,AttributeConfig>();

  public AnnotationInterfaceType(Class<T> annClass)
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

  private boolean isAnnotationPresent(Annotation []annList, Class<?> annType)
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
  @Override
  public Class<T> getType()
  {
    return _annClass;
  }

  @Override
  public boolean isQualifier()
  {
    return _annClass.isAnnotationPresent(Qualifier.class);
  }
  /**
   * Creates a new instance of the type.
   */
  @Override
  public Object create(Object parent, NameCfg name)
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
      AttributeConfig attr = getAttribute(TEXT);

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
  public AttributeConfig getAttribute(NameCfg qName)
  {
    String name = qName.getLocalName();

    if ("#text".equals(name))
      name = "value";

    AttributeConfig attr = _attributeMap.get(name);

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
      
      Object value = entry.getValue();
      
      if (value == null)
        sb.append(value);
      else if (value.getClass().isArray()) {
        Object []values = (Object []) value;
        
        sb.append("[");
        for (int i = 0; i < values.length; i++) {
          if (i != 0)
            sb.append(", ");
          
          sb.append(values[i]);
        }
        sb.append("]");
      }
      else
        sb.append(value);
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
