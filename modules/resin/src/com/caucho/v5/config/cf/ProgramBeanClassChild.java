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

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * Program to assign parameters.
 */
public class ProgramBeanClassChild extends ProgramContainerBase
{
  private static final L10N L = new L10N(ProgramBeanClassChild.class);
  
  private final NameCfg _id;
  private final ProgramCommandClassName _childProgram;
  
  ProgramBeanClassChild(NameCfg id,
                        ProgramCommandClassName childProgram)
  {
    _id = id;
    _childProgram = childProgram;
  }
  
  @Override
  public <T> void inject(T bean, InjectContext env)
    throws ConfigException
  {
    try {
      ConfigType<T> type = TypeFactoryConfig.getType(bean);
    
      inject(bean, type, env);
    } catch (RuntimeException e) {
      throw error(e);
    } catch (Exception e) {
      throw error(e);
    }
  }
  
  @Override
  public <T> void inject(T bean, ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    try {
      AttributeConfig attr = type.getAttribute(_id);

      if (attr == null) {
        throw error("{0} is an unknown attribute of {1}{2}", 
                    _id.getLocalName(), 
                    bean,
                    type.getAttributeUsage());
      }

      if (attr.isProgram()) {
        attr.setValue(bean, _id, this);
        return;
      }

      Object child = _childProgram.create(bean);

      attr.setValue(bean, _id, child);
    } catch (RuntimeException e) {
      throw error(e);
    } catch (Exception e) {
      throw error(e);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}

