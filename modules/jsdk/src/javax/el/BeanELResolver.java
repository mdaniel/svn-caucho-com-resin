/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package javax.el;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

/**
 * Resolves properties based on beans.
 */
public class BeanELResolver extends ELResolver {
  private final static Logger log
    = Logger.getLogger(MapELResolver.class.getName());
  
  private final boolean _isReadOnly;

  private WeakHashMap<Class,BeanProperties> _classMap
    = new WeakHashMap<Class,BeanProperties>();
  
  public BeanELResolver()
  {
    _isReadOnly = false;
  }
  
  public BeanELResolver(boolean isReadOnly)
  {
    _isReadOnly = isReadOnly;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base == null)
      return null;
    
    return Object.class;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
							   Object base)
  {
    return null;
  }

  @Override
  public Class<?> getType(ELContext context,
			  Object base,
			  Object property)
  {
    Object value = getValue(context, base, property);

    if (value != null)
      return value.getClass();
    else
      return null;
  }

  @Override
  public Object getValue(ELContext context,
			 Object base,
			 Object property)
  {
    if (base == null || ! (property instanceof String))
      return null;

    String fieldName = (String) property;

    if (fieldName.length() == 0)
      return null;

    Class cl = base.getClass();
    BeanProperties props = _classMap.get(cl);

    if (props == null) {
      if (cl.isArray()
	  || Collection.class.isAssignableFrom(cl)
	  || Map.class.isAssignableFrom(cl)) {
	return null;
      }
	  
      props = new BeanProperties(cl);
      _classMap.put(cl, props);
    }

    BeanProperty prop = props.getBeanProperty(fieldName);

    context.setPropertyResolved(true);

    if (prop == null)
      return null;//throw new PropertyNotFoundException(fieldName);

    try {
      return prop.getReadMethod().invoke(base);
    } catch (IllegalAccessException e) {
      throw new ELException(e);
    } catch (InvocationTargetException e) {
      throw new ELException(e.getCause());
    }
  }

  @Override
  public boolean isReadOnly(ELContext env,
			    Object base,
			    Object property)
  {
    BeanProperties props = getProp(env, base, property);

    if (props != null) {
      env.setPropertyResolved(true);

      return _isReadOnly;
    }
    else
      return true;
  }

  @Override
  public void setValue(ELContext context,
		       Object base,
		       Object property,
		       Object value)
  {
  }

  private BeanProperties getProp(ELContext context,
				 Object base,
				 Object property)
  {
    if (base == null || ! (property instanceof String))
      return null;

    String fieldName = (String) property;

    if (fieldName.length() == 0)
      return null;

    Class cl = base.getClass();
    BeanProperties props = _classMap.get(cl);

    if (props == null) {
      if (cl.isArray()
	  || Collection.class.isAssignableFrom(cl)
	  || Map.class.isAssignableFrom(cl)) {
	return null;
      }
	  
      props = new BeanProperties(cl);
      _classMap.put(cl, props);
    }

    return props;
  }

  protected static final class BeanProperties
  {
    private Class _base;

    private HashMap<String,BeanProperty> _propMap
      = new HashMap<String,BeanProperty>();
    
    public BeanProperties(Class baseClass)
    {
      _base = baseClass;

      try {
	BeanInfo info = Introspector.getBeanInfo(baseClass);

	for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
	  _propMap.put(descriptor.getName(),
		       new BeanProperty(baseClass, descriptor));
	}

	Method []methods = baseClass.getMethods();

	for (int i = 0; i < methods.length; i++) {
	  Method method = methods[i];

	  String name = method.getName();

	  if (method.getParameterTypes().length != 0)
	    continue;

	  if (! Modifier.isPublic(method.getModifiers()))
	    continue;

	  if (Modifier.isStatic(method.getModifiers()))
	    continue;

	  String propName;
	  if (name.startsWith("get"))
	    propName = Introspector.decapitalize(name.substring(3));
	  else if (name.startsWith("is"))
	    propName = Introspector.decapitalize(name.substring(2));
	  else
	    continue;

	  if (_propMap.get(propName) != null)
	    continue;

	  _propMap.put(propName, new BeanProperty(baseClass,
						  propName,
						  method));
	}
      } catch (IntrospectionException e) {
	throw new ELException(e);
      }
    }

    public BeanProperty getBeanProperty(String property)
    {
      return _propMap.get(property);
    }
  }

  protected static final class BeanProperty {
    private Class _base;
    private PropertyDescriptor _descriptor;
    
    public BeanProperty(Class baseClass,
			PropertyDescriptor descriptor)
    {
      _base = baseClass;
      _descriptor = descriptor;

      if (descriptor.getReadMethod() != null)
	descriptor.getReadMethod().setAccessible(true);
    }
    
    public BeanProperty(Class baseClass,
			String name,
			Method getter)
    {
      try {
	_base = baseClass;
	_descriptor = new PropertyDescriptor(name, getter, null);

	if (getter != null)
	  getter.setAccessible(true);
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }

    public Class getPropertyType()
    {
      return _descriptor.getPropertyType();
    }

    public Method getReadMethod()
    {
      return _descriptor.getReadMethod();
    }

    public Method getWriteMethod()
    {
      return _descriptor.getWriteMethod();
    }

    public boolean isReadOnly()
    {
      return getWriteMethod() == null;
    }
  }
}
