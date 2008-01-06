/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.config;

import com.caucho.util.L10N;

import java.lang.reflect.Method;

/**
 * TypeBuilder for primitives, primitive wrappers, and Strings
 */
public class ObjectAttributeStrategy extends AttributeStrategy {
  static L10N L = new L10N(ObjectAttributeStrategy.class);

  private final Method _setter;
  private String _name;
  
  public ObjectAttributeStrategy(Method setter, String name, Class type)
    throws BeanBuilderException
  {
    _setter = setter;
    _name = name;
  }
  
  public void setString(Object bean, String value)
    throws BeanBuilderException
  {
    try {
      if (value == null) {
        setNull(bean);
        return;
      }
    
      _setter.invoke(bean, new Object[] { _name, evalString(value) });
    } catch (Exception e) {
      throw new BeanBuilderException(e);
    }
  }

  private void setNull(Object bean)
    throws Exception
  {
    _setter.invoke(bean, new Object[] { _name, null });
  }

  public String toString()
  {
    return "ObjectAttributeStrategy[" + _setter + "]";
  }
}
