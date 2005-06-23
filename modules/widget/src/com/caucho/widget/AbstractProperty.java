/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.widget;

import java.io.IOException;

/**
 * A property is associated with a widget, it has a default value
 * and if the widget has an id it can be changed and stored as
 * a parameter.
 *
 * A property cannot have a null value.
 *
 * A property can be inherited.
 */
abstract public class AbstractProperty<T>
{
  private String _name;
  private String _parameterName;
  private Widget _widget;
  private VarDefinition _varDef;

  private T _defaultOverride;
  private boolean _isInit;

  private T _default;

  private final WidgetInterceptor _interceptor = new PropertyInterceptor();

  public AbstractProperty(Widget widget, String name, boolean isInherited, T deflt)
  {
    if (deflt == null)
      throw new IllegalArgumentException("default cannot be null for property `" + name + "'");

    _name = name;

    _parameterName = name + "!";
    _widget = widget;

    String varName = "com.caucho.widget.property." + name;

    _varDef = new PropertyVarDefinition(varName, deflt.getClass());

    _varDef.setInherited(isInherited);
    _varDef.setAllowNull(false);
    _varDef.setValue(deflt);
  }

  public String getName()
  {
    return _name;
  }

  private boolean isImmutable()
  {
    return _widget.getId() == null;
  }

  public void setValue(T value)
  {
    if (_isInit)
      throw new IllegalStateException("init() already called");

    if (value == null)
      throw new IllegalArgumentException("`value' cannot be null for property `" + getName() + "'");

    _defaultOverride = value;
  }

  public T getValue()
  {
    if (_isInit)
      throw new IllegalStateException("init() already called");

    if (_defaultOverride != null)
      return _defaultOverride;
    else
      return (T) _varDef.getValue();
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    if (_isInit)
      throw new IllegalStateException("init() already called");

    init.addVarDefinition(_varDef);
    init.addInterceptor(_interceptor);

    _isInit = true;

    if (_defaultOverride != null) {
      setValue(init, _defaultOverride);
      _default = _defaultOverride;
    }
    else
      _default = (T) _varDef.getValue();

    _defaultOverride = null;
  }

  /**
   * Setting a property to a null
   * value clears it; once cleared if the property is not inherited it allows the
   * default to come through; or if the property is inherited it allows the
   * inherited value to come through.
   */
  public void setValue(VarContext context, T value)
  {
    assert _isInit :  "missing init() for property `" + _name + "' widget " + _widget;

    boolean isInInit = (context instanceof WidgetInit);

    if (!isInInit && isImmutable())
      throw new UnsupportedOperationException("cannot set property '" + getName() + "' because widget does not have an id");

    if (value == null) {
      context.removeVar(_widget, _varDef);

      if (isInInit)
        _default = (T) _varDef.getValue();
    }
    else {
      context.setVar(_widget, _varDef, value);

      if (isInInit)
        _default = value;
    }
  }

  public T getValue(VarContext context)
  {
    assert _isInit :  "missing init() for property `" + _name + "' widget " + _widget;

    T value = null;

    if (isImmutable())
      value = _default;
    else
      value = (T) context.getVar(_widget, _varDef);

    return value;
  }

  abstract protected String convertToString(T value);
  abstract protected T convertFromString(String value);

  private class PropertyVarDefinition
    extends VarDefinition
  {
    public PropertyVarDefinition(String name, Class<?> type)
    {
      super(name, type);
    }

    public boolean isReadOnly()
    {
      if (AbstractProperty.this.isImmutable())
        return true;
      else
        return super.isReadOnly();
    }
  }

  private class PropertyInterceptor
    implements WidgetInterceptor
  {
    public void invocation(WidgetInvocation invocation,
                             WidgetInterceptorChain next)
      throws WidgetException
    {
      assert _isInit :  "missing init() for property `" + _name + "' widget " + _widget;

      if (!isImmutable()) {
        String stringValue = invocation.getParameter(_parameterName);

        if (stringValue != null) {

          T convertedValue = convertFromString(stringValue);

          if (convertedValue == null)
            throw new NullPointerException("convertFromString() in class " + getClass().getName() + " cannot return null");

          setValue(invocation, convertedValue);
        }
      }

      next.invocation(invocation);
    }

    public void request(WidgetRequest request, WidgetInterceptorChain next)
      throws WidgetException
    {
      next.request(request);
    }

    public void response(WidgetResponse response, WidgetInterceptorChain next)
      throws WidgetException, IOException
    {
      next.response(response);
    }

    public void url(WidgetURL url, WidgetInterceptorChain next)
      throws WidgetException
    {
      assert _isInit :  "missing init() for property `" + _name + "' widget " + _widget;

      if (!isImmutable() && !isTransient(url)) {
        T value = (T) url.getVar(_widget, _varDef, null);

        if (value == null)
          return;

        if (!value.equals(_default)) {
          String stringValue = convertToString(value);

          if (stringValue == null)
            throw new NullPointerException("convertToString() in class " + getClass().getName() + " cannot return null");

          url.setParameter(_parameterName, stringValue);
        }
      }

      next.url(url);
    }

    private boolean isTransient(WidgetURL url)
    {
      Boolean isTransient = url.getVar(_widget, TransientProperty.TRANSIENT);

      return isTransient == null ? false : isTransient.booleanValue();
    }

    public void destroy(WidgetDestroy destroy, WidgetInterceptorChain next)
    {
      _isInit = false;
      _defaultOverride = null;

      next.destroy(destroy);
    }
  }
}
