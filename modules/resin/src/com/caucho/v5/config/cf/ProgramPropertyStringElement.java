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

package com.caucho.v5.config.cf;

import java.util.Objects;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * Program to assign parameters.
 */
public class ProgramPropertyStringElement extends ProgramIdBase
{
  private static final L10N L = new L10N(ProgramPropertyStringElement.class);
  private final NameCfg _id;
  private final String _value;
  
  ProgramPropertyStringElement(ConfigContext config, String id, String value)
  {
    super(config, id);
    
    _id = new NameCfg(id);
    _value = value;
  }
  
  @Override
  public <T> void inject(T bean, InjectContext env)
  {
    if (bean == null) {
      throw error("{0} received an unexpected null bean", this);
    }
    
    inject(bean, TypeFactoryConfig.getType(bean), env);
  }
  
  @Override
  public <T> void inject(T bean, ConfigType<T> type, InjectContext env)
  {
    Objects.requireNonNull(bean);
    
    AttributeConfig attr = getAttribute(type);
    
    if (attr == null) {
      throw new ConfigException(L.l("{0} is an unknown attribute of {1}{2}",
                                    _id.getLocalName(), bean,
                                    type.getAttributeUsage()));
    }
    
    if (attr.isProgram()) {
      attr.setValue(bean, _id, this);
    }
    else {
      // config/1201
      
      ConfigType subType = attr.getConfigType();
      
      String value = _value;
      
      AttributeConfig subAttr = null;
      
      if (subAttr == null) {
        subAttr = subType.getAttribute("_p0");
      }
      
      if (subAttr == null
          && attr.getConfigType().isConstructableFromString()) {
        attr.setValue(bean, _id, attr.getConfigType().valueOf(_value));
        return;
      }
      
      if (subAttr == null) {
        subAttr = subType.getAttribute(_value);
        value = "true";
      }
      
      if (subAttr != null) {
        Object child = attr.create(bean, _id);
        
        if (child == null) {
          System.out.println("SERK: " + attr);
        }
        Objects.requireNonNull(child);
        
        subAttr.setValue(child, _id, subAttr.getConfigType().valueOf(value));
        
        attr.getConfigType().init(child);
      
        attr.setValue(bean, _id, child);
      }
      else {
        System.out.println("program prop: _p0 " + subType + " " + _id + " " + _value);
        // attr.setValue(bean,  _id, _value);
      }
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id.getLocalName() + "=" + _value + "]";
  }
}

