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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.webbeans.event;

import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.context.Context;
import javax.event.Observer;
import javax.event.Observes;
import javax.event.IfExists;
import javax.inject.manager.Bean;

import com.caucho.config.*;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.manager.InjectManager;
import com.caucho.util.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.manager.*;

/**
 * Implements a single observer.
 */
public class ObserverImpl implements Observer {
  private static final L10N L = new L10N(ObserverImpl.class);

  private static final Object []NULL_ARGS = new Object[0];

  private final InjectManager _webBeans;
  private final AbstractBean _bean;
  
  private final Method _method;
  private final int _paramIndex;

  private boolean _hasBinding;
  private boolean _ifExists;

  private Bean []_args;

  public ObserverImpl(InjectManager webBeans,
		      AbstractBean bean,
		      Method method,
		      int paramIndex)
  {
    _webBeans = webBeans;
    _bean = bean;
    _method = method;
    _method.setAccessible(true);
    _paramIndex = paramIndex;

    for (Annotation ann : method.getParameterAnnotations()[paramIndex]) {
      if (ann instanceof IfExists)
	_ifExists = true;
    }

    bind();
  }

  public Class getType()
  {
    return _method.getParameterTypes()[_paramIndex];
  }

  /**
   * Initialization.
   */
  public void init()
  {
    // _webbeans.addWbComponent(this);

    /*
    if (_name == null) {
      Named named = (Named) _cl.getAnnotation(Named.class);

      if (named != null)
	_name = named.value();

      if (_name == null || "".equals(_name)) {
	String className = _targetType.getName();
	int p = className.lastIndexOf('.');
      
	char ch = Character.toLowerCase(className.charAt(p + 1));
      
	_name = ch + className.substring(p + 2);
      }
    }
    */
  }

  public void bind()
  {
    synchronized (this) {
      if (_args != null)
	return;
      
      Type []param = _method.getGenericParameterTypes();
      Annotation [][]annList = _method.getParameterAnnotations();

      _args = new ComponentImpl[param.length];

      String loc = LineConfigException.loc(_method);
      
      for (int i = 0; i < param.length; i++) {
	if (hasObserves(annList[i]))
	  continue;

	Set beans = _webBeans.resolveByType(param[i], annList[i]);
	
	if (beans == null || beans.size() == 0) {
	  throw new ConfigException(loc
				    + L.l("Parameter '{0}' binding does not have a matching component",
					  getSimpleName(param[i])));
	}
	
	ComponentImpl comp = null;

	// XXX: error checking
	Iterator iter = beans.iterator();
	if (iter.hasNext()) {
	  comp = (ComponentImpl) iter.next();
	}

	_args[i] = comp;
      }
    }
  }

  private boolean hasObserves(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann instanceof Observes)
	return true;
    }

    return false;
  }

  public void notify(Object event)
  {
    Object obj = null;

    if (_ifExists) {
      Context context = _webBeans.getContext(_bean.getScopeType());

      if (context != null && context.isActive())
	obj = context.get(_bean, false);
    }
    else
      obj = _webBeans.getInstance(_bean);

    try {
      if (obj != null) {
	Object []args = new Object[_args.length];

	for (int i = 0; i < _args.length; i++) {
	  Bean bean = _args[i];
	  
	  if (bean != null)
	    args[i] = _webBeans.getInstance(bean);
	  else
	    args[i] = event;
	}
	
	_method.invoke(obj, args);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof ObserverImpl))
      return false;

    ObserverImpl comp = (ObserverImpl) obj;

    if (! _bean.equals(comp._bean)) {
      return false;
    }

    return true;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(_method.getName());
    sb.append("[");

    sb.append(_method.getParameterTypes()[_paramIndex].getSimpleName());
    sb.append("]");
    sb.append("]");

    return sb.toString();
  }

  protected static String getSimpleName(Type type)
  {
    if (type instanceof Class)
      return ((Class) type).getSimpleName();
    else
      return String.valueOf(type);
  }
}
