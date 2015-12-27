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
 * @author Scott Ferguson;
 */

package com.caucho.v5.config.program;

import java.lang.reflect.Field;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.inject.InjectContext;
import com.caucho.v5.util.L10N;


public class FieldGeneratorProgram extends ConfigProgram
{
  private static final L10N L = new L10N(FieldGeneratorProgram.class);

  private Field _field;
  private ValueGenerator _gen;

  public FieldGeneratorProgram(Config config, Field field, ValueGenerator gen)
  {
    super(config);
    
    _field = field;
    _field.setAccessible(true);

    _gen = gen;
  }

  @Override
  public String getName()
  {
    return _field.getName();
  }

  Class<?> getType()
  {
    return _field.getType();
  }

  @Override
  public Class<?> getDeclaringClass()
  {
    return _field.getDeclaringClass();
  }

  @Override
  public <T> void inject(T bean, InjectContext env)
    throws ConfigException
  {
    Object value = null;
    
    try {
      value = _gen.create();

      /*
      // XXX TCK: ejb30/bb/session/stateless/sessioncontext/descriptor/getBusinessObjectLocal1, needs QA
      if (value != null
          && ! _field.getType().isAssignableFrom(value.getClass())
          && ! _field.getType().isPrimitive()) {
        value = PortableRemoteObject.narrow(value, _field.getType());
      }
      */
      

      _field.set(bean, value);
    } catch (ConfigException e) {
      throw e;
    } catch (ClassCastException e) {
      throw ConfigException.create(L.l("{0}: value {1} be cast to {2}",
                                       location(), value, _field.getType().getName()),
                                   e);
    } catch (Exception e) {
      throw ConfigException.create(location(), e);
    }
  }

  private String location()
  {
    return _field.getDeclaringClass().getName() + "." + _field.getName() + ": ";
  }
}
