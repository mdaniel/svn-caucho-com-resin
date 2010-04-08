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

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.AbstractInjectionPoint;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.scope.DependentScope;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.util.*;

import java.util.Set;
import java.util.HashSet;
import java.util.logging.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Qualifier;

public class FieldComponentProgram extends ConfigProgram

{
  private static final L10N L = new L10N(FieldComponentProgram.class);
  private static final Logger log
    = Logger.getLogger(FieldComponentProgram.class.getName());

  private InjectManager _manager;
  private Bean _bean;
  private Field _field;
  private AbstractInjectionPoint _ij;

  public FieldComponentProgram(InjectManager manager,
			       Bean bean,
			       Field field)
  {
    _manager = manager;
    _bean = bean;
    _field = field;

    field.setAccessible(true);

    HashSet<Annotation> bindingSet = new HashSet<Annotation>();

    for (Annotation ann : _field.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
	bindingSet.add(ann);
    }

    _ij = new AbstractInjectionPoint(manager,
				     bean,
				     _field,
				     _field.getGenericType(),
				     bindingSet,
				     _field.getAnnotations());
  }

  public <T> void inject(T bean, CreationalContext<T> env)
  {
    Object value = null;
    
    try {
      value = _manager.getInjectableReference(_ij, env);

      _field.set(bean, value);
    } catch (IllegalArgumentException e) {
      throw new ConfigException(ConfigException.loc(_field) + L.l("Can't set field value '{0}'", value), e);
    } catch (Exception e) {
      throw new ConfigException(ConfigException.loc(_field) + e.toString(), e);
    }
  }
  
  public Set<Annotation> getBindings()
  {
    return null;
  }
  
  public Type getType()
  {
    return _field.getType();
  }
  
  public Bean<?> getBean()
  {
    return _bean;
  }
  
  public Member getMember()
  {
    return _field;
  }
  
  public <T extends Annotation> T getAnnotation(Class<T> annotationType)
  {
    return _field.getAnnotation(annotationType);
  }
  
  public Annotation []getAnnotations()
  {
    return _field.getAnnotations();
  }
  
  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    return _field.isAnnotationPresent(annType);
  }

  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _field.getDeclaringClass().getSimpleName()
	    + "." + _field.getName()
	    + "," + _field.getType().getSimpleName()
	    + "]");
  }
}
