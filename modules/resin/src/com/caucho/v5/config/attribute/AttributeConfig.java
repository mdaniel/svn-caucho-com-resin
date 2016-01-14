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

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

public abstract class AttributeConfig
{
  private static final L10N L = new L10N(AttributeConfig.class);
  
  private static NameCfg TEXT_QNAME = new NameCfg("#text");
  private static NameCfg VALUE_QNAME = new NameCfg("value");
  
  /**
   * Returns the config type of the attribute value.
   */
  abstract public ConfigType<?> getConfigType();

  /**
   * Returns true for a bean-style attribute.
   */
  public boolean isBean()
  {
    return getConfigType().isBean();
  }

  /**
   * Returns true for an EL attribute.
   */
  public boolean isEL()
  {
    return getConfigType().isEL();
  }

  /**
   * Returns true for a node attribute.
   */
  public boolean isNode()
  {
    return getConfigType().isNode();
  }

  /**
   * Returns true for a program-style attribute.
   */
  public boolean isProgram()
  {
    return getConfigType().isProgram();
  }

  /**
   * True if it allows inline beans
   */
  public boolean isAllowInline()
  {
    return false;
  }

  /**
   * True if the inline type matches
   */
  public boolean isInlineType(ConfigType<?> type)
  {
    return false;
  }

  /**
   * True if it allows text.
   */
  public boolean isAllowText()
  {
    return true;
  }

  /**
   * True if the attribute is annotated with a @Configurable
   */
  public boolean isConfigurable()
  {
    return false;
  }

  public boolean isAssignableFrom(AttributeConfig oldAttr)
  {
    return false;
  }
  
  public boolean isAssignableFrom(ConfigType<?> type)
  {
    return true;
  }

  public boolean isAssignableFrom(Class<?> classChild)
  {
    ConfigType<?> type = getConfigType();
    
    if (type == null) {
      return false;
    }
    else if (type.getType() == null) {
      return false;
    }
    else {
      return type.getType().isAssignableFrom(classChild);
    }
  }
  
  /**
   * Sets the value of the attribute as text
   */
  public void setText(Object bean, NameCfg name, String value)
    throws ConfigException
  {
    Object childBean = create(bean, name);

    if (childBean != null) {
      ConfigType<?> type = TypeFactoryConfig.getType(childBean.getClass());
      
      if (! value.trim().equals("")) {
        AttributeConfig attributeText = type.getAttribute("#text");
        AttributeConfig attributeValue = type.getAttribute("value");
        
        if (attributeText != null) {
          attributeText.setText(childBean, TEXT_QNAME, value.trim());
        }
        else if (attributeValue != null) {
          attributeValue.setText(childBean, VALUE_QNAME, value.trim());
        }
        else {
          throw new ConfigException(L.l("{0}: '{1}' does not allow text for attribute {2}.",
                                        this,
                                        getConfigType().getTypeName(),
                                        name));
        }
      }

      type.init(childBean);

      Object newBean = replaceObject(childBean);

      setValue(bean, name, newBean);

      return;
    }

    throw new ConfigException(L.l("{0}: '{1}' does not allow text for attribute {2}.",
                                  this,
                                  getConfigType().getTypeName(),
                                  name));
  }
  
  /**
   * Sets the value of the attribute
   */
  abstract public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException;

  /**
   * Returns true for attributes which create objects.
   */
  public boolean isSetter()
  {
    return true;
  }

  /**
   * Creates the child bean.
   */
  public Object create(Object parent, NameCfg name, ConfigType<?> type)
    throws ConfigException
  {
    return create(parent, name);
  }

  /**
   * Creates the child bean.
   */
  public Object create(Object parent, NameCfg name)
    throws ConfigException
  {
    return null;
  }
  
  /**
   * Returns the config type of the child bean.
   */
  public ConfigType<?> getType(Object childBean)
  {
    return getConfigType().getType(childBean);
  }

  /**
   * Replaces the given bean.
   */
  public Object replaceObject(Object bean)
  {
    return getConfigType().replaceObject(bean);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getConfigType() + "]";
  }
}
