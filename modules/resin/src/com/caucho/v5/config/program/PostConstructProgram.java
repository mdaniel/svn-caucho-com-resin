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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

import javax.inject.Qualifier;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.ModulePrivate;

@ModulePrivate
public class PostConstructProgram extends ConfigProgram
{
  private Method _init;
  private MethodHandle _initHandle;
  //private ParamProgram []_program;

  public PostConstructProgram(ConfigContext config,
                              Method init)
  {
    super(config);
    
    Objects.requireNonNull(init);
    
    _init = init;
    init.setAccessible(true);
    
    try {
      _initHandle = MethodHandles.lookup().unreflect(init);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }

    introspect();
  }
  
  @Override
  public Class<?> getDeclaringClass()
  {
    return _init.getDeclaringClass();
  }
  
  @Override
  public String getName()
  {
    return _init.getName();
  }

  protected void introspect()
  {
    // XXX:
    /*
    // XXX: type
    Type []paramTypes = _init.getGenericParameterTypes();

    if (paramTypes.length == 0)
      return;

    _program = new ParamProgram[paramTypes.length];
    
    Annotation [][]paramAnns = _init.getParameterAnnotations();

    CandiManager webBeans = CandiManager.create();
    
    for (int i = 0; i < paramTypes.length; i++) {
      Annotation []bindings = createBindings(paramAnns[i]);
      
      _program[i] = new ParamProgram(webBeans, paramTypes[i],
                                     bindings, paramAnns[i]);
    }
    */
  }

  Annotation []createBindings(Annotation []annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingList.add(ann);
    }

    if (bindingList.size() == 0)
      return null;

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }

  @Override
  public <T> void inject(T bean, InjectContext env)
    throws ConfigException
  {
    try {
      /*
      if (_program != null) {
        Object []args = new Object[_program.length];

        for (int i = 0; i < args.length; i++) {
          args[i] = _program[i].eval(env);
        }

        _init.invoke(bean, args);
      }
      else {
        _initHandle.invoke(bean);
      }
      */
      _initHandle.invoke(bean);
    } catch (Throwable e) {
      e.printStackTrace();
      throw ConfigExceptionLocation.wrap(_init, e);
    }
  }

  @Override
  public int hashCode()
  {
    return _init.getName().hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof PostConstructProgram))
      return false;

    PostConstructProgram program = (PostConstructProgram) o;
    Method init = program._init;

    if (! _init.getName().equals(init.getName()))
      return false;
    
    if (! init.getDeclaringClass().equals(_init.getDeclaringClass()))
      return false;

    Class<?> []aParam = _init.getParameterTypes();
    Class<?> []bParam = init.getParameterTypes();

    if (aParam.length != bParam.length)
      return false;

    for (int i = 0; i < aParam.length; i++) {
      if (! aParam[i].equals(bParam[i]))
        return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _init + "]";
  }

  /*
  private static class ParamProgram {
    private final CandiManager _inject;
    private final InjectionPointBase _injectionPoint;

    ParamProgram(CandiManager inject,
                 Type type,
                 Annotation []bindings,
                 Annotation []annList)
    {
      _inject = inject;
      Bean<?> bean = null;
      Member member = null;
      HashSet<Annotation> bindingSet = new HashSet<Annotation>();

      if (bindings != null) {
        for (Annotation ann :  bindings)
          bindingSet.add(ann);
      }
      else
        bindingSet.add(CurrentLiteral.CURRENT);

      _injectionPoint = new InjectionPointBase(inject,
                                                   bean, member, type,
                                                   bindingSet, annList);
    }

    public Object eval(CreationalContext<?> env)
    {
      return _inject.getInjectableReference(_injectionPoint, env);
    }
  }
  */
}
