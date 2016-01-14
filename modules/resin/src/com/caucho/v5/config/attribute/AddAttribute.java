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

package com.caucho.v5.config.attribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.custom.CustomBean;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.types.AnnotationConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

public class AddAttribute<T> extends AttributeConfig {
  private static final L10N L = new L10N(AddAttribute.class);

  public static final AddAttribute<CustomBean> ATTR
    = new AddAttribute<>(CustomBean.class);
  
  private final ConfigType<T> _configType;
  private final Method _setMethod;

  private AddAttribute(Class<T> cl)
  {
    this(null, (ConfigType<T>) TypeFactoryConfig.getType(cl));
  }
  
  public AddAttribute(Method setMethod, ConfigType<T> configType)
  {
    _configType = configType;
    _setMethod = setMethod;
  }

  @Override
  public ConfigType<T> getConfigType()
  {
    return _configType;
  }

  public Method getMethod()
  {
    return _setMethod;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg qName)
    throws ConfigException
  {
    Class<?> cl = TypeFactoryConfig.loadClass(qName);

    if (cl == null) {
      ConfigType<?> type = TypeFactoryConfig.getFactory().getEnvironmentType(qName);

      if (type != null)
        return type.create(parent, qName);

      throw new ConfigException(L.l("'{0}.{1}' is an unknown class for element '{2}'",
                                    qName.getNamespaceURI(), qName.getLocalName(), qName));
    }

    if (Annotation.class.isAssignableFrom(cl)) {
      return new AnnotationConfig(cl);
    }
    else {
      CustomBean<?> config = new CustomBean(qName, cl, parent);
      config.setInlineBean(true);

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
    Object objValue = create(bean, name);

    if (objValue instanceof CustomBean<?>) {
      CustomBean<?> config = (CustomBean<?>) objValue;

      if (! value.trim().equals("")) {
        config.addArg(new TextArgProgram(ConfigContext.getCurrent(), value));
      }

      config.init();
    }

    setValue(bean, name, objValue);
  }
  
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    try {
      if (value instanceof CustomBean<?>) {
        CustomBean<?> config = (CustomBean<?>) value;

        value = config.toObject();
      }
      else if (value instanceof AnnotationConfig) {
        AnnotationConfig config = (AnnotationConfig) value;

        value = config.replace();
      }

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
  
  static class TextArgProgram extends ConfigProgram {
    private String _arg;

    TextArgProgram(ConfigContext config, String arg)
    {
      super(config);
      
      _arg = arg;
    }
    
    @Override
    public <T> void inject(T bean, InjectContext env)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    public Object configure(ConfigType<?> type, InjectContext env)
      throws ConfigException
    {
      return type.valueOf(_arg);
    }
  }
}
