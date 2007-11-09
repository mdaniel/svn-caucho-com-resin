/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.webbeans.cfg;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.util.*;
import com.caucho.webbeans.inject.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class WbBinding {
  private static final L10N L = new L10N(WbBinding.class);
  
  private Class _cl;

  private ArrayList<WbBindingValue> _valueList
    = new ArrayList<WbBindingValue>();

  public WbBinding()
  {
  }

  public WbBinding(Annotation ann)
  {
    setClass(ann.annotationType());

    try {
      for (Method method : _cl.getDeclaredMethods()) {
	Object value = method.invoke(ann);
	
	_valueList.add(new WbBindingValue(method, value));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  public void addValue(WbBindingValue value)
  {
    _valueList.add(value);
  }

  public void addValue(String name, Object value)
  {
    try {
      Method method = _cl.getMethod(name, new Class[0]);
      _valueList.add(new WbBindingValue(method, value));
    } catch (Exception e) {
      throw new ConfigException(L.l("{0}: '{1}' is an unknown method.",
				    _cl.getSimpleName(), name));
    }
  }

  public void setClass(Class cl)
  {
    _cl = cl;
  }

  public void setText(Class cl)
  {
    setClass(cl);
  }
  
  public String getClassName()
  {
    if (_cl != null)
      return _cl.getName();
    else
      return null;
  }

  public boolean isMatch(Annotation bindAnn)
  {
    if (! bindAnn.annotationType().equals(_cl))
      return false;

    for (int i = 0; i < _valueList.size(); i++) {
      if (! _valueList.get(i).isMatch(bindAnn))
	return false;
    }

    return true;
  }

  public boolean isBindingPresent(ArrayList<Annotation> bindingList)
  {
    for (int i = 0; i < bindingList.size(); i++) {
      Annotation ann = bindingList.get(i);

      if (ann.annotationType().equals(_cl))
	return true;
    }
    
    return false;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof WbBinding))
      return false;

    WbBinding binding = (WbBinding) o;

    int size = _valueList.size();
    if (size != binding._valueList.size()) {
      return false;
    }

    for (int i = size - 1; i >= 0; i--) {
      WbBindingValue value = _valueList.get(i);

      if (! binding._valueList.contains(value)) {
	return false;
      }
    }

    return true;
  }

  public String toString()
  {
    return "WbBinding[" + _cl.getName() + "]";
  }

  static class WbBindingValue {
    private Method _method;
    private Object _value;

    WbBindingValue(Method method, Object value)
    {
      _method = method;
      _value = value;
    }

    boolean isMatch(Annotation ann)
    {
      try {
	Object value = _method.invoke(ann);

	if (value == _value)
	  return true;
	else if (value == null)
	  return false;
	else
	  return value.equals(_value);
      } catch (RuntimeException e) {
	throw e;
      } catch (InvocationTargetException e) {
	throw new ConfigException(e.getCause());
      } catch (Exception e) {
	throw new ConfigException(e);
      }
    }

    public boolean equals(Object o)
    {
      if (this == o)
	return true;
      else if (! (o instanceof WbBindingValue))
	return false;

      WbBindingValue value = (WbBindingValue) o;

      if (! _method.equals(value._method))
	return false;
      else if (_value == value._value)
	return true;
      else
	return _value != null && _value.equals(value._value);
    }

    public String toString()
    {
      return "WbBinding[" + _method.getName() + ", " + _value + "]";
    }
  }
}
