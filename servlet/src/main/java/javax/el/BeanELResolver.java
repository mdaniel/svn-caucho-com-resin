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
 * @author Scott Ferguson
 */

package javax.el;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolves properties based on beans.
 */
public class BeanELResolver extends ELResolver 
{
  private static WeakHashMap<Class<?>,SoftReference<BeanProperties>> _classMap
    = new WeakHashMap<>();
  
  private final boolean _isReadOnly;
  
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
    if (base == null)
      return null;

    Class<?> cl = base.getClass();
    BeanProperties props = getProps(cl);

    if (props == null) {
      if (cl.isArray()
          || Collection.class.isAssignableFrom(cl)
          || Map.class.isAssignableFrom(cl)) {
        return null;
      }

      props = new BeanProperties(cl);
      setProps(cl, props);
    }

    ArrayList<FeatureDescriptor> descriptors = new ArrayList<>();

    for (BeanProperty prop : props.getProperties()) {
      descriptors.add(prop.getDescriptor());
    }

    return descriptors.iterator();
  }

  /**
   * If the base object is not null, returns the most general type of the
   * property
   *
   * @param context
   * @param base
   * @param property
   * @return
   */
  @Override
  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    if (base == null || property == null)
      return null;

    if (! (property instanceof String))
      return null;

    String fieldName = (String) property;

    if (fieldName.length() == 0)
      return null;

    Class<?> cl = base.getClass();
    BeanProperties props = getProps(cl);

    if (props == null) {
      if (cl.isArray()
          || Collection.class.isAssignableFrom(cl)
          || Map.class.isAssignableFrom(cl)) {
        return null;
      }

      props = new BeanProperties(cl);
      setProps(cl, props);
    }

    BeanProperty prop = props.getBeanProperty(fieldName);

    context.setPropertyResolved(true);
    
    // #5080, jsp/30cl
    if (prop == null) {
    }
    else if (prop.getWriteMethod() != null) {
      return prop.getWriteMethod().getParameterTypes()[0];
    }
    else if (prop.getReadMethod() != null) {
      return prop.getReadMethod().getReturnType();
    }

    throw new PropertyNotFoundException("'" + property
                                        + "' is an unknown bean property of '"
                                        + base.getClass().getName()
                                        + "'");
  }

  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    if (base == null || property == null) {
      return null;
    }

    String fieldName = String.valueOf(property);

    if (fieldName.length() == 0) {
      return null;
    }
    
    Class<?> cl = base.getClass();
    BeanProperties props = getProps(cl);

    if (props == null) {
      if (cl.isArray()
          || Collection.class.isAssignableFrom(cl)
          || Map.class.isAssignableFrom(cl)) {
        return null;
      }

      props = new BeanProperties(cl);
      setProps(cl, props);
    }

    BeanProperty prop = props.getBeanProperty(fieldName);

    context.setPropertyResolved(base, property);

    if (prop == null || prop.getReadMethod() == null) {
      throw new PropertyNotFoundException("'" + property + "' is an unknown bean property of '" + base.getClass().getName() + "'");
    }

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
    if (base == null)
      return false;
    
    BeanProperties props = getProp(env, base, property);

    if (props != null) {
      env.setPropertyResolved(true);

      if (_isReadOnly)
        return true;

      BeanProperty prop = props.getBeanProperty((String) property);

      if (prop != null)
        return prop.isReadOnly();
    }

    throw new PropertyNotFoundException("'" + property + "' is an unknown bean property of '" + base.getClass().getName() + "'");
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base == null || property == null)
      return;

    String fieldName = String.valueOf(property);

    if (fieldName.length() == 0)
      return;

    Class<?> cl = base.getClass();
    BeanProperties props = getProps(cl);

    if (props == null) {
      if (cl.isArray()
          || Collection.class.isAssignableFrom(cl)
          || Map.class.isAssignableFrom(cl)) {
        return;
      }

      props = new BeanProperties(cl);
      setProps(cl, props);
    }

    BeanProperty prop = props.getBeanProperty(fieldName);

    context.setPropertyResolved(base, property);

    if (prop == null)
      throw new PropertyNotFoundException(fieldName);
    else if (_isReadOnly || prop.getWriteMethod() == null)
      throw new PropertyNotWritableException(fieldName);
    
    Object arg = context.convertToType(value, prop.getPropertyType());
    
    try {
      prop.getWriteMethod().invoke(base, arg);
    } catch (IllegalAccessException e) {
      throw new ELException(e);
    } catch (InvocationTargetException e) {
      throw new ELException(e.getCause());
    }    
  }

  @Override
  public Object invoke(ELContext context,
                       Object base,
                       Object methodObj,
                       Class<?> []paramTypes,
                       Object []params)
  {
    if (base == null)
      throw new ELException("base object is null");

    String methodName;

    if (methodObj instanceof String)
      methodName = (String) methodObj;
    else if (methodObj instanceof Enum<?>)
      methodName = ((Enum<?>) methodObj).name();
    else
      methodName = methodObj.toString();

    Class<?> baseClass = base.getClass();
    
    Method method = ReflectUtil.getInstance().findMethod(baseClass, 
                                                         methodName, 
                                                         paramTypes, 
                                                         params,
                                                         false);
    
    Object result = ReflectUtil.getInstance().invokeMethod(context, 
                                                           method, 
                                                           base, 
                                                           params);
    
    context.setPropertyResolved(base, methodObj);
    
    return result;
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

    Class<?> cl = base.getClass();
    BeanProperties props = getProps(cl);

    if (props == null) {
      if (cl.isArray()
          || Collection.class.isAssignableFrom(cl)
          || Map.class.isAssignableFrom(cl)) {
        return null;
      }

      props = new BeanProperties(cl);
      setProps(cl, props);
    }

    return props;
  }

  static BeanProperties getProps(Class<?> cl)
  {
    synchronized (_classMap) {
      SoftReference<BeanProperties> ref = _classMap.get(cl);

      if (ref != null)
        return ref.get();
      else
        return null;
    }
  }

  static void setProps(Class<?> cl, BeanProperties props)
  {
    synchronized (_classMap) {
      _classMap.put(cl, new SoftReference<BeanProperties>(props));
    }
  }

  protected static final class BeanProperties
  {
    private HashMap<String,BeanProperty> _propMap = new HashMap<>();
    
    public BeanProperties(Class<?> baseClass)
    {
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

          // jsp/30ci
          BeanProperty oldProp = _propMap.get(propName);

          if (oldProp == null) {
            _propMap.put(propName, new BeanProperty(baseClass,
                                                    propName,
                                                    method,
                                                    null));
          }
          else if (oldProp != null
                   && method.equals(oldProp.getReadMethod())) {
          }
          else {
            _propMap.put(propName, new BeanProperty(baseClass,
                                                    propName,
                                                    method,
                                                    null));
          }
        }
      } catch (IntrospectionException e) {
        throw new ELException(e);
      }
    }
    

    public BeanProperty getBeanProperty(String property)
    {
      return _propMap.get(property);
    }

    private Collection<BeanProperty> getProperties()
    {
      return _propMap.values();
    }
  }

  protected static final class BeanProperty 
  {
    private Class<?> _base;
    private PropertyDescriptor _descriptor;
    private Method _readMethod;
    private Method _writeMethod;
    private boolean _isInit;
    
    public BeanProperty(Class<?> baseClass,
                        PropertyDescriptor descriptor)
    {
      _base = baseClass;
      _descriptor = descriptor;

      // #3598
      Method readMethod = descriptor.getReadMethod();
      try {
        if (readMethod != null)
          //create a copy of the method
          _readMethod = _base.getMethod(readMethod.getName(),
                                        readMethod.getParameterTypes());
      } catch (NoSuchMethodException e) {
      }

      if (_readMethod != null) {
        _readMethod.setAccessible(true);
      }
      
      _writeMethod = descriptor.getWriteMethod();

      if (_writeMethod != null) {
        _writeMethod.setAccessible(true);
      }
    }
    
    private BeanProperty(Class<?> baseClass,
                         String name,
                         Method getter,
                         Method setter)
    {
      try {
        _base = baseClass;
        
        if (getter != null && ! void.class.equals(getter.getReturnType()))
          _descriptor = new PropertyDescriptor(name, getter, setter);
        else
          _descriptor = new PropertyDescriptor(name, null, null);

        //create a copy of the method
        if (getter != null) {
          _readMethod = _base.getMethod(getter.getName(),
                                        getter.getParameterTypes());
        }

        if (_readMethod != null)
          _readMethod.setAccessible(true);
        
        _writeMethod = setter;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    void initDescriptor()
    {
      if (_isInit) {
        return;
      }
      
      _isInit = true;
      
      Method readMethod = _readMethod;

      if (readMethod != null) {
        _descriptor.setValue(ELResolver.TYPE, readMethod.getReturnType());
      }

      _descriptor.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME,
                           Boolean.TRUE);
    }

    private PropertyDescriptor getDescriptor()
    {
      initDescriptor();
      
      return _descriptor;
    }

    public Class<?> getPropertyType()
    {
      return _descriptor.getPropertyType();
    }

    public Method getReadMethod()
    {
      return _readMethod;
    }

    public Method getWriteMethod()
    {
      return _writeMethod;
    }

    public boolean isReadOnly()
    {
      return getWriteMethod() == null;
    }
  }
}
