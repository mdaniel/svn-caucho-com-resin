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
import javax.webbeans.*;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

/**
 * Implements a single observer.
 */
public class ObserverImpl {
  private static final L10N L = new L10N(ObserverImpl.class);

  private static final Object []NULL_ARGS = new Object[0];

  private final ComponentImpl _component;
  
  private final Method _method;
  private final int _paramIndex;

  private boolean _hasBinding;
  private boolean _ifExists;

  private ComponentImpl []_args;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  public ObserverImpl(ComponentImpl comp,
		      Method method,
		      int paramIndex)
  {
    _component = comp;
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
   * Adds a component binding.
   */
  public void setBindingList(ArrayList<WbBinding> bindingList)
  {
    _bindingList = bindingList;
  }
  
  public ArrayList<WbBinding> getBindingList()
  {
    return _bindingList;
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

      WebBeansContainer webBeans = _component.getWebBeans().getContainer();
      String loc = LineConfigException.loc(_method);
      
      for (int i = 0; i < param.length; i++) {
	if (hasObserves(annList[i]))
	  continue;

	ComponentImpl comp = webBeans.bind(loc, param[i], annList[i]);

	if (comp == null) {
	  throw new ConfigException(loc
				    + L.l("Parameter '{0}' binding does not have a matching component",
					  getSimpleName(param[i])));
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

  public boolean isMatch(Annotation []bindList)
  {
    int size = _bindingList.size();
    
    if (bindList.length < size)
      return false;
    
    for (int i = 0; i < size; i++) {
      WbBinding binding = _bindingList.get(i);

      boolean isMatch = false;
      for (Annotation ann : bindList) {
	if (binding.isMatch(ann)) {
	  isMatch = true;
	  break;
	}
      }

      if (! isMatch)
	return false;
    }
    
    return true;
  }

  public void raiseEvent(Object event)
  {
    Object obj;

    if (_ifExists)
      obj = _component.getIfExists();
    else
      obj = _component.get();

    try {
      if (obj != null) {
	Object []args = new Object[_args.length];

	for (int i = 0; i < _args.length; i++) {
	  ComponentImpl comp = _args[i];
	  
	  if (comp != null)
	    args[i] = comp.get();
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

    if (! _component.equals(comp._component)) {
      return false;
    }

    int size = _bindingList.size();

    if (size != comp._bindingList.size()) {
      return false;
    }

    for (int i = size - 1; i >= 0; i--) {
      if (! comp._bindingList.contains(_bindingList.get(i))) {
	return false;
      }
    }

    return true;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(_method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(_method.getName());
    sb.append("[");

    ComponentImpl comp = _component;
    sb.append(comp.getTargetSimpleName());
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
