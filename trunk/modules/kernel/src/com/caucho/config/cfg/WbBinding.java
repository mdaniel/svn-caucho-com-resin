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
 * @author Scott Ferguson
 */

package com.caucho.config.cfg;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.util.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.util.Nonbinding;

/**
 * Configuration for the xml web bean component.
 */
public class WbBinding {
  private static final L10N L = new L10N(WbBinding.class);

  private String _signature;
  private Class _cl;

  private ArrayList<WbBindingValue> _valueList
    = new ArrayList<WbBindingValue>();

  private Annotation _annotation;

  public WbBinding()
  {
  }

  public WbBinding(Annotation ann)
  {
    _annotation = ann;

    setClass(ann.annotationType());

    try {
      for (Method method : _cl.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Nonbinding.class))
          continue;

        Object value = method.invoke(ann);

        _valueList.add(new WbBindingValue(method, value));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Annotation getAnnotation()
  {
    return _annotation;
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

  public void setText(String signature)
  {
    _signature = signature;
  }

  public Class getBindingClass()
  {
    return _cl;
  }

  public String getClassName()
  {
    if (_cl != null)
      return _cl.getName();
    else
      return null;
  }

  /**
   * Returns the binding values
   */
  public ArrayList<WbBindingValue> getValueList()
  {
    return _valueList;
  }

  /**
   * Parses the function signature.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_signature == null)
      throw new ConfigException(L.l("binding requires an annotation"));

    int p = _signature.indexOf('(');

    String className;

    if (p > 0)
      className = _signature.substring(0, p);
    else
      className = _signature;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      _cl = Class.forName(className, false, loader);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
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

  public boolean isMatch(Binding bind)
  {
    if (! _cl.equals(bind.getBindingClass()))
      return false;

    for (int i = 0; i < _valueList.size(); i++) {
      if (! _valueList.get(i).isMatch(bind))
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

  public long generateCrc64(long crc64)
  {
    crc64 = Crc64.generate(crc64, _cl.getName());

    for (WbBindingValue value : _valueList) {
      crc64 = Crc64.generate(crc64, value.getName());
      crc64 = Crc64.generate(crc64, String.valueOf(value.getValue()));
    }

    return crc64;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof WbBinding))
      return false;

    WbBinding binding = (WbBinding) o;

    if (! _cl.equals(binding._cl))
      return false;

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

  /**
   * Converts to a binding object
   */
  public Binding toBinding()
  {
    Binding binding = new Binding(_cl);

    for (WbBindingValue value : _valueList) {
      binding.put(value.getName(), value.getValue());
    }

    return binding;
  }

  /**
   * Converts to a string signature
   */
  public String toSignature()
  {
    if (_signature == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(_cl.getName());
      sb.append("(");

      for (int i = 0; i < _valueList.size(); i++) {
        WbBindingValue value = _valueList.get(i);

        if (value.getValue() == null)
          continue;

        if (i != 0)
          sb.append(",");

        sb.append(value.getName());
        sb.append("=");

        addValue(sb, value.getValue());
      }

      sb.append(")");

      _signature = sb.toString();
    }

    return _signature;
  }

  private void addValue(StringBuilder sb, Object value)
  {
    if (value.getClass().isArray()) {
      sb.append("{");
      Object []array = (Object []) value;
      for (int i = 0; i < array.length; i++) {
        if (i != 0)
          sb.append(",");

        addValue(sb, array[i]);
      }
      sb.append("}");
    }
    else if (value instanceof String) {
      String string = (String) value;

      sb.append("\"");

      for (int i = 0; i < string.length(); i++) {
        char ch = string.charAt(i);

        switch (ch) {
        case '\\':
          sb.append("\\\\");
          break;
        case '\"':
          sb.append("\\\"");
          break;
        default:
          sb.append(ch);
          break;
        }
      }

      sb.append("\"");
    }
    else
      sb.append(value);
  }

  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("@");
    sb.append(_cl.getSimpleName());

    if (_valueList.size() == 0)
      return sb.toString();

    sb.append("(");
    for (int i = 0; i < _valueList.size(); i++) {
      WbBindingValue value = _valueList.get(i);

      if (i != 0)
        sb.append(",");

      if (! value.getName().equals("value")) {
        sb.append(value.getName());
        sb.append("=");
      }

      Object objValue = value.getValue();

      if (objValue instanceof String) {
        sb.append("\"");
        sb.append(objValue);
        sb.append("\"");
      }
      else
        sb.append(objValue);
    }

    sb.append(")");

    return sb.toString();
  }

  public String toString()
  {
    return toDebugString();
  }

  public static class WbBindingValue {
    private Method _method;
    private Object _value;

    WbBindingValue(Method method, Object value)
    {
      _method = method;
      _value = value;
    }

    public String getName()
    {
      return _method.getName();
    }

    public Object getValue()
    {
      return _value;
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
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    boolean isMatch(Binding binding)
    {
      String key = _method.getName();

      Object value = binding.get(key);

      if (value == _value)
        return true;
      else if (value == null)
        return false;
      else
        return value.equals(_value);
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
