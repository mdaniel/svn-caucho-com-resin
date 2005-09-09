/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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

package com.caucho.jmx;

import com.caucho.util.L10N;

import java.lang.ref.SoftReference;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.WeakHashMap;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.*;

/**
 * Resin implementation of DynamicMBean that uses introspection of an
 * <i>implementation</i> and an <i>interface</i> to
 * determine the structure of the bean.
 *
 * <p>
 * Attributes and Operations are discovered by introspecting the specified
 * <i>interface</i>.
 * </p>
 *
 * <p>
 * A {@link InstrospectionDescriptor} is created for the bean, initially empty.
 * If the <i>implementation</i> has a method with  the signature
 * <code>void describe(IntrospectionDescriptor)</code>
 * it is called and the <i>implementation</i> can add descriptor information.
 * </p>
 *
 * <p>
 * A {@link InstrospectionDescriptor} is created for each attribute
 * and operation, initially empty.
 * If the <i>implementation</i> has a method with the signature
 * <code>void describe<i>Name</i>(IntrospectionDescriptor)</code>
 * where <i>Name</i> is the name of the
 * operation or attribute,
 * it is called and the <i>implementation</i> can add descriptor information.
 * </p>
 */
public class IntrospectionMBean implements DynamicMBean {
  private static final L10N L = new L10N(IntrospectionMBean.class);
  private static final Logger log
    = Logger.getLogger(IntrospectionMBean.class.getName());

  private static final Class[] NULL_ARG = new Class[0];

  private static final WeakHashMap<Class,SoftReference<MBeanInfo>> _cachedInfo
    = new WeakHashMap<Class,SoftReference<MBeanInfo>>();

  private Object _impl;
  private Class _mbeanInterface;
  private MBeanInfo _mbeanInfo;

  /**
   * Makes a DynamicMBean out of an interface and an implementation of
   * the interface.
   */
  public IntrospectionMBean(Object implementation, Class mbeanInterface)
    throws NotCompliantMBeanException
  {
    if (implementation == null)
      throw new NullPointerException();

    _mbeanInterface = mbeanInterface;

    MBeanInfo info = introspect(implementation, mbeanInterface);

    _mbeanInfo = info;
    _impl = implementation;
  }

  /**
   * Returns the implementation.
   */
  public Object getImplementation()
  {
    return _impl;
  }

  /**
   * Returns an attribute value.
   */
  public Object getAttribute(String attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    try {
      Method method = getGetMethod(attribute);

      if (method != null)
        return method.invoke(_impl, (Object []) null);
      else
        throw new AttributeNotFoundException(attribute);
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception)
        throw new ReflectionException((Exception) e.getCause());
      else
        throw (Error) e.getCause();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets an attribute value.
   */
  public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
           MBeanException, ReflectionException
  {
    try {
      Method method = getSetMethod(attribute.getName(), attribute.getValue());

      if (method != null)
        method.invoke(_impl, new Object[] { attribute.getValue() });
      else
        throw new AttributeNotFoundException(attribute.getName());
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      throw new MBeanException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e.toString());
    }
  }

  /**
   * Returns matching attribute values.
   */
  public AttributeList getAttributes(String []attributes)
  {
    AttributeList list = new AttributeList();

    for (int i = 0; i < attributes.length; i++) {
      try {
        Method method = getGetMethod(attributes[i]);

        if (method != null) {
          Object value = method.invoke(_impl, (Object []) null);

          list.add(new Attribute(attributes[i], value));
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return list;
  }

  /**
   * Sets attribute values.
   */
  public AttributeList setAttributes(AttributeList attributes)
  {
    AttributeList list = new AttributeList();

    for (int i = 0; i < attributes.size(); i++) {
      try {
        Attribute attr = (Attribute) attributes.get(i);
        Method method = getSetMethod(attr.getName(), attr.getValue());

        if (method != null) {
          method.invoke(_impl, new Object[] { attr.getValue() });
          list.add(new Attribute(attr.getName(), attr.getValue()));
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return list;
  }

  /**
   * Returns the set method matching the name.
   */
  private Method getGetMethod(String name)
  {
    String getName = "get" + name;
    String isName = "is" + name;

    Method []methods = _mbeanInterface.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(getName) &&
          ! methods[i].getName().equals(isName))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length == 0 && ! methods[i].getReturnType().equals(void.class))
        return methods[i];
    }

    return null;
  }

  /**
   * Returns the set method matching the name.
   */
  private Method getSetMethod(String name, Object value)
  {
    name = "set" + name;

    Method []methods = _mbeanInterface.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(name))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length != 1)
        continue;

      /*
      if (value != null && ! args[0].isAssignableFrom(value.getClass()))
	continue;
      */

      return methods[i];
    }

    return null;
  }

  /**
   * Invokes a method on the bean.
   */
  public Object invoke(String actionName,
                       Object []params,
                       String []signature)
    throws MBeanException, ReflectionException
  {
    try {
      Method []methods = _mbeanInterface.getMethods();

      int length = 0;
      if (signature != null)
        length = signature.length;
      if (params != null)
        length = params.length;

      for (int i = 0; i < methods.length; i++) {
        if (! methods[i].getName().equals(actionName))
          continue;

        Class []args = methods[i].getParameterTypes();

        if (args.length != length)
          continue;

        boolean isMatch = true;
        for (int j = length - 1; j >= 0; j--) {
          if (! args[j].getName().equals(signature[j]))
            isMatch = false;
        }

        if (isMatch)
          return methods[i].invoke(_impl, params);
      }

      if (actionName.equals("hashCode") && signature.length == 0)
        return _impl.hashCode();
      else if (actionName.equals("toString") && signature.length == 0)
        return _impl.toString();
      else
        return null;
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception)
        throw new ReflectionException((Exception) e.getCause());
      else
        throw (Error) e.getCause();
    }
  }

  /**
   * Returns the introspection information for the MBean.
   */
  public MBeanInfo getMBeanInfo()
  {
    return _mbeanInfo;
  }

  static MBeanInfo introspect(Object obj, Class cl)
    throws NotCompliantMBeanException
  {
    try {

      SoftReference<MBeanInfo> infoRef = _cachedInfo.get(cl);

      MBeanInfo info;

      // XXX: can no longer cache, because Descriptor can be different
      // for each implementation
//      if (infoRef != null)
//        info = infoRef.get();
//      else
//        info = null;
//
//      if (info != null)
//        return info;

      String className = cl.getName();

      ArrayList<MBeanAttributeInfo> attributes;
      attributes = new ArrayList<MBeanAttributeInfo>();

      ArrayList<MBeanConstructorInfo> constructors;
      constructors = new ArrayList<MBeanConstructorInfo>();

      ArrayList<MBeanOperationInfo> operations;
      operations = new ArrayList<MBeanOperationInfo>();

      Method []methods = cl.getMethods();

      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];

        String methodName = method.getName();
        Class []args = method.getParameterTypes();
        Class retType = method.getReturnType();

        if (methodName.startsWith("get") && args.length == 0 &&
            ! retType.equals(void.class)) {
          String name = methodName.substring(3);

          Method setter = getSetter(methods, name, retType);

          MBeanAttributeInfo attributeInfo = createMBeanAttributeInfo(obj, name, method, setter);

          attributes.add(attributeInfo);
        }
        else if (methodName.startsWith("is") && args.length == 0 &&
            (retType.equals(boolean.class) ||
             retType.equals(Boolean.class))) {
          String name = methodName.substring(2);

          Method setter = getSetter(methods, name, retType);

          MBeanAttributeInfo attributeInfo = createMBeanAttributeInfo(obj, name, method, setter);

          attributes.add(attributeInfo);
        }
        else if (methodName.startsWith("set") && args.length == 1) {
          String name = methodName.substring(3);

          Method getter = getGetter(methods, name, args[0]);

          if (getter == null){
            MBeanAttributeInfo attributeInfo = createMBeanAttributeInfo(obj, name, null, method);

            attributes.add(attributeInfo);
          }
        }
        else {
          MBeanOperationInfo operationInfo = createMBeanOperationInfo(obj, methodName, method);

          operations.add(operationInfo);
        }
      }

      MBeanNotificationInfo []notifs;

      if (obj instanceof NotificationBroadcaster) {
        NotificationBroadcaster broadcaster;
        broadcaster = (NotificationBroadcaster) obj;

        notifs = broadcaster.getNotificationInfo();

        if (notifs == null)
          notifs = new MBeanNotificationInfo[0];

        for (int i = 0; i < notifs.length; i++) {
          notifs[i] = (MBeanNotificationInfo) notifs[i].clone();
        }
      }
      else
        notifs = new MBeanNotificationInfo[0];

      MBeanAttributeInfo []attrArray = new MBeanAttributeInfo[attributes.size()];
      attributes.toArray(attrArray);
      MBeanConstructorInfo []conArray = new MBeanConstructorInfo[constructors.size()];
      constructors.toArray(conArray);
      MBeanOperationInfo []opArray = new MBeanOperationInfo[operations.size()];
      operations.toArray(opArray);

      info = createMBeanInfo(obj, className, attrArray, conArray, opArray, notifs);

      // XXX: can no longer cache, because Descriptor can be different
      // for each implementation
      // _cachedInfo.put(cl, new SoftReference<MBeanInfo>(info));

      return info;
    } catch (Exception e) {
      NotCompliantMBeanException exn;
      exn = new NotCompliantMBeanException(String.valueOf(e));

      exn.initCause(e);

      throw exn;
    }
  }

  private static MBeanInfo createMBeanInfo(Object mbean,
                                           String className,
                                           MBeanAttributeInfo[] attrArray,
                                           MBeanConstructorInfo[] conArray,
                                           MBeanOperationInfo[] opArray,
                                           MBeanNotificationInfo[] notifs)
    throws NotCompliantMBeanException
  {
    IntrospectionMBeanDescriptor descriptor = new IntrospectionMBeanDescriptor();

    describe(mbean, descriptor);

    String description = descriptor.getDescription();

    if (description == null)
      description = "MBean for " + className;

    return new IntrospectionMBeanInfo(className,
                                      description,
                                      attrArray,
                                      conArray,
                                      opArray,
                                      notifs,
                                      descriptor);
  }

  private static MBeanAttributeInfo createMBeanAttributeInfo(Object mbean,
                                                             String name,
                                                             Method method,
                                                             Method setter)
    throws NotCompliantMBeanException, IntrospectionException
  {
    IntrospectionAttributeDescriptor descriptor = new IntrospectionAttributeDescriptor();

    describe(mbean, name, descriptor);

    String description = descriptor.getDescription();

    if (description == null)
      description = name;


    return new IntrospectionMBeanAttributeInfo(name,
                                               description,
                                               method,
                                               setter,
                                               descriptor);
  }

  private static MBeanOperationInfo createMBeanOperationInfo(Object mbean,
                                                             String name,
                                                             Method method)
    throws NotCompliantMBeanException
  {
    IntrospectionOperationDescriptor descriptor = new IntrospectionOperationDescriptor();

    describe(mbean, name, descriptor);

    String description = descriptor.getDescription();

    if (description == null)
      description = name;

    int impact = descriptor.getImpact();

    return new IntrospectionMBeanOperationInfo(name,
                                               description,
                                               method,
                                               impact,
                                               descriptor);
  }

  /**
   * Invoke a method on the mbean to describe the mbean.
   */
  private static void describe(Object mbean,
                               IntrospectionDescriptor descriptor)
    throws NotCompliantMBeanException
  {
    describe(mbean, null, descriptor);
  }
  /**
   * Invoke a method on the mbean to describe a named feature.
   */
  private static void describe(Object mbean,
                               String name,
                               IntrospectionDescriptor descriptor)
    throws NotCompliantMBeanException
  {
    String methodName = "describe";

    if (name != null)
      methodName += Character.toUpperCase(name.charAt(0)) + name.substring(1);

    Method[] methods = mbean.getClass().getMethods();

    for (Method method : methods) {

      if (!methodName.equals(method.getName()))
        continue;

      try {
        method.invoke(mbean, descriptor);
      }
      catch (Exception ex) {
        throw new NotCompliantMBeanException(L.l("{0} for {1}: {2}", methodName, mbean.getClass().getName(), ex.toString()));
      }

      break;
    }

  }

  /**
   * Returns the matching setter.
   */
  static Method getSetter(Method []methods, String property, Class type)
  {
    String name = "set" + property;

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(name))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length != 1 || ! args[0].equals(type))
        continue;

      return methods[i];
    }

    return null;
  }

  /**
   * Returns the matching getter.
   */
  static Method getGetter(Method []methods, String property, Class type)
  {
    String getName = "get" + property;
    String isName = "is" + property;

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(getName) &&
          ! methods[i].getName().equals(isName))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length != 0)
        continue;

      Class retType = methods[i].getReturnType();

      if (! retType.equals(type))
        continue;

      return methods[i];
    }

    return null;
  }

  public static class IntrospectionMBeanInfo
    extends MBeanInfo
  {
    private final Descriptor _descriptor;

    private IntrospectionMBeanInfo(String className,
                                   String description,
                                   MBeanAttributeInfo[] attributes,
                                   MBeanConstructorInfo[] constructors,
                                   MBeanOperationInfo[] operations,
                                   MBeanNotificationInfo[] notifications,
                                   Descriptor descriptor)
      throws IllegalArgumentException
    {
      super(className, description, attributes, constructors, operations, notifications);

      _descriptor = descriptor;
    }

    public Descriptor getDescriptor()
    {
      return _descriptor;
    }
  }

  public static class IntrospectionMBeanAttributeInfo
    extends MBeanAttributeInfo
  {
    private final Descriptor _descriptor;

    private IntrospectionMBeanAttributeInfo(String name,
                                            String description,
                                            Method getter,
                                            Method setter,
                                            Descriptor descriptor)
      throws IntrospectionException
    {
      super(name, description, getter, setter);

      _descriptor = descriptor;
    }

    public Descriptor getDescriptor()
    {
      return _descriptor;
    }
  }

  public static class IntrospectionMBeanOperationInfo
    extends MBeanOperationInfo
  {
    private final int _impact;
    private final Descriptor _descriptor;

    private IntrospectionMBeanOperationInfo(String name,
                                            String description,
                                            Method method,
                                            int impact,
                                            Descriptor descriptor)
      throws IllegalArgumentException
    {
      super(description, method);

      _impact = impact;
      _descriptor = descriptor;
    }

    public int getImpact()
    {
      return _impact;
    }

    public Descriptor getDescriptor()
    {
      return _descriptor;
    }
  }
}
