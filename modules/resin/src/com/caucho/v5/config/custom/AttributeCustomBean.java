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
import java.lang.reflect.Method;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.types.AnnotationConfig;
import com.caucho.v5.util.L10N;

public class AttributeCustomBean extends AttributeConfig
{
  private static final L10N L = new L10N(AttributeCustomBean.class);

  public static final AttributeCustomBean ATTRIBUTE
    = new AttributeCustomBean();
  
  private static final ConfigType<?> _configTypeCustom
    = TypeFactoryConfig.getType(CustomBean.class);

  private final ConfigType<?> _beanConfigType;
  private final Method _setMethod;

  private AttributeCustomBean()
  {
    this(null, TypeFactoryConfig.getType(CustomBean.class));
  }

  public AttributeCustomBean(Method setMethod,
                             ConfigType<?> beanConfigType)
  {
    _beanConfigType = beanConfigType;
    _setMethod = setMethod;
  }

  @Override
  public ConfigType<?> getConfigType()
  {
    return _configTypeCustom;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg qName)
    throws ConfigException
  {
    String uri = qName.getNamespaceURI();
    String localName = qName.getLocalName();

    if (! uri.startsWith("urn:java:"))
      throw new IllegalStateException(L.l("'{0}' is an unexpected namespace, expected 'urn:java:...'", uri));

    String pkg = uri.substring("uri:java:".length());
    
    // ioc/13jm
    ConfigType<?> type = _beanConfigType.getFactory().getEnvironmentType(qName);
    // ioc/0401
    if (type != null && type.isEnvBean()) {
      return type.create(parent, qName);
    }
    
    Class<?> cl = TypeFactoryConfig.loadClass(pkg, localName);

    if (cl == null) {
      throw new ConfigException(L.l("'{0}.{1}' is an unknown class for element '{2}'",
                                    pkg, localName, qName));
    }

    if (Annotation.class.isAssignableFrom(cl)) {
      return new AnnotationConfig(cl);
    }
    else {
      CustomBean config
        = TypeFactoryConfig.createCustomBean(qName, cl, parent);

      if (cl.isAnnotationPresent(InlineConfig.class)) {
        config.setInlineBean(true);
      }
      // config.setScope("singleton");
      return config;
    }
  }

  /**
   * Sets the value of the attribute as text
   */
  @Override
  public void setText(Object bean, NameCfg name, String value)
    throws ConfigException
  {
    Object beanChild = create(bean, name);

    if (beanChild instanceof CustomBean) {
      CustomBean custom = (CustomBean) beanChild;

      if (! value.trim().equals("")) {
        custom.addArg(new TextArgProgram(ConfigContext.getCurrent(), value));
      }

      custom.init();
    }
    else {
      ConfigType<?> childType = TypeFactoryConfig.getType(beanChild);
      
      childType.setProperty(beanChild, ContextConfig.TEXT, value);
      
      childType.init(beanChild);
    }

    setValue(bean, name, beanChild);
  }

  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    try {
      if (value instanceof AnnotationConfig)
        value = ((AnnotationConfig) value).replace();

      if (_setMethod != null && value != null) {
        if (! _setMethod.getParameterTypes()[0].isAssignableFrom(value.getClass()))
          throw new ConfigException(L.l("'{0}.{1}' is not assignable from {2}",
                                        _setMethod.getDeclaringClass().getSimpleName(),
                                        _setMethod.getName(),
                                        value));


        _setMethod.invoke(bean, value);
      }
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
}
