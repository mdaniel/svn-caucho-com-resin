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

package com.caucho.v5.config.custom;

import java.lang.annotation.Annotation;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.types.AnnotationConfig;
import com.caucho.v5.util.L10N;

/**
 * Attribute for configuring an XML annotation's value.
 * 
 * The XML equivalent for an attribute @mypkg.MyAttribute(myfield="my-value")
 * is the following:
 * 
 * <code><pre>
 * &lt;mypkg:MyAttribute>
 *   &lt;myfield>my-value&lt;/myfield>
 * &lt;/mypkg:MyAttribute>
 * </pre></code>
 * 
 */
public class AttributeCustomAnnotation<T> extends AttributeConfig {
  private static final L10N L = new L10N(AttributeCustomAnnotation.class);

  private static final NameCfg VALUE = new NameCfg("value");

  private final ConfigType<T> _configType;

  public AttributeCustomAnnotation(Class<T> cl)
  {
    _configType = TypeFactoryConfig.getType(cl);
  }

  @Override
  public ConfigType<T> getConfigType()
  {
    return _configType;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg qName)
    throws ConfigException
  {
    return _configType.create(parent, qName);
  }

  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    CustomBean<T> customBean = (CustomBean<T>) bean;

    if (value instanceof Annotation) {
      customBean.addAnnotation((Annotation) value);
    }
    else {
      AnnotationConfig annConfig = (AnnotationConfig) value;
      customBean.addAnnotation(annConfig.replace());
    }
  }

  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object parent, NameCfg name, String text)
    throws ConfigException
  {
    Object bean = create(parent, name);

    AttributeConfig attr = _configType.getAttribute(VALUE);

    if (attr != null) {
      attr.setText(bean, VALUE, text);

      setValue(parent, name, bean);
    }
    else if (text == null || "".equals(text)) {
      // server/2pad
      setValue(parent, name, bean);
    }
    else {
      throw new ConfigException(L.l("'{0}' does not have a 'value' attribute, so it cannot have a text value.",
                                    name));
    }
  }
}
