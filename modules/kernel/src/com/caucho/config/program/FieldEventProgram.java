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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;

import com.caucho.config.ConfigException;
import com.caucho.config.event.EventImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

public class FieldEventProgram extends ConfigProgram
{
  private static final L10N L = new L10N(FieldEventProgram.class);
  private static final Logger log
    = Logger.getLogger(FieldEventProgram.class.getName());

  private InjectManager _manager;
  private Field _field;
  private Class _eventType;
  private Annotation []_bindings;

  public FieldEventProgram(InjectManager manager,
			   Field field,
			   Annotation []bindings)
  {
    _manager = manager;
    _field = field;
    field.setAccessible(true);

    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();
    for (Annotation ann : bindings) {
      /*
      if (ann.annotationType().equals(Observable.class))
	continue;
      */
      
      bindingList.add(ann);
    }
    _bindings = new Annotation[bindingList.size()];
    bindingList.toArray(_bindings);

    if (! field.getType().equals(Event.class))
      throw new ConfigException(L.l("{0}:{1} is an invalid @Observable because it is not an javax.webbeans.Event",
				    field.getDeclaringClass().getSimpleName(),
				    field.getName()));

    Class eventType = null;
    Type type = field.getGenericType();
    if (! (type instanceof ParameterizedType)) {
      throw new ConfigException(L.l("{0}:{1} is an invalid @Observable because the Event must be parameterized",
				    field.getDeclaringClass().getSimpleName(),
				    field.getName()));
      
    }

    ParameterizedType pType = (ParameterizedType) type;

    _eventType = (Class) pType.getActualTypeArguments()[0];
  }

  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
  {
    Object value = new EventImpl(_manager, _eventType, _bindings);
    
    try {
      _field.set(bean, value);
    } catch (IllegalArgumentException e) {
      throw new ConfigException(ConfigException.loc(_field) + L.l("Can't set field value '{0}'", value), e);
    } catch (Exception e) {
      throw new ConfigException(ConfigException.loc(_field) + e.toString(), e);
    }
  }
}
