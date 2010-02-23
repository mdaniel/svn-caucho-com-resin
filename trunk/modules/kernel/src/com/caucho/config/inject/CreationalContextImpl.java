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
 */

package com.caucho.config.inject;

import com.caucho.config.Config;
import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.program.NodeBuilderChildProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.scope.DependentScope;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.Validator;
import com.caucho.config.type.*;
import com.caucho.config.types.*;
import com.caucho.config.attribute.*;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;

import org.w3c.dom.*;

import javax.el.*;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Stack of partially constructed beans.
 */
public class CreationalContextImpl<T> implements CreationalContext<T> {
  private final CreationalContextImpl<?> _next;
  private final Contextual<T> _bean;
  private final InjectionPoint _injectionPoint;
  private T _value;
  
  public CreationalContextImpl(Contextual<T> bean,
                               CreationalContext<?> next,
                               InjectionPoint ij)
  {
    _next = (CreationalContextImpl<?>) next;
    _bean = bean;
    _injectionPoint = ij;
  }
  
  public CreationalContextImpl()
  {
    _next = null;
    _bean = null;
    _injectionPoint = null;
  }
  
  public CreationalContextImpl(Contextual<T> bean,
                               CreationalContext<?> next)
  {
    _bean = bean;
    _next = (CreationalContextImpl<?>) next;
    _injectionPoint = null;
  }
  
  CreationalContextImpl(Contextual<T> bean)
  {
    _next = null;
    _bean = bean;
    _injectionPoint = null;
  }
  
  public static CreationalContextImpl create()
  {
    return new CreationalContextImpl();
  }
  
  <X> X get(Contextual<X> bean)
  {
    return find(this, bean);    
  }
  
  @SuppressWarnings("unchecked")
  static <X> X find(CreationalContextImpl<?> ptr, Contextual<X> bean)
  {
    for (; ptr != null; ptr = ptr._next){
      Contextual<?> testBean = ptr._bean;
      
      if (testBean == bean) {
        return (X) ptr._value;
      }
    }
    
    return null;
  }
  
  static Object findByName(CreationalContextImpl<?> ptr, String name)
  {
    for (; ptr != null; ptr = ptr._next) {
      Contextual<?> testBean = ptr._bean;
      
      if (! (testBean instanceof Bean))
        continue;
      
      Bean<?> bean = (Bean<?>) testBean;
      
      if (name.equals(bean.getName())) {
        return ptr._value;
      }
    }
    
    return null;
  }
  
  public InjectionPoint getInjectionPoint()
  {
    return _injectionPoint;
  }

  @Override
  public void push(T value)
  {
    _value = value;
  }

  @Override
  public void release()
  {
    // Bean.remove?
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "," + _value + ",next=" + _next + "]";
  }
}
