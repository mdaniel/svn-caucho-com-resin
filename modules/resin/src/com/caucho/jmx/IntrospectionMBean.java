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

import java.lang.ref.SoftReference;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Annotation;

import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.*;

/**
 * Resin implementation of StandardMBean.
 */
public class IntrospectionMBean implements DynamicMBean {
  private static final Logger log
    = Logger.getLogger(IntrospectionMBean.class.getName());

  private static final Class[] NULL_ARG = new Class[0];

  private static final Class _descriptionAnn;
  private static final Class _nameAnn;

  private static final WeakHashMap<Class,SoftReference<MBeanInfo>> _cachedInfo
    = new WeakHashMap<Class,SoftReference<MBeanInfo>>();

  private Object _impl;
  private Class _mbeanInterface;
  private MBeanInfo _mbeanInfo;


  private static final Comparator<MBeanFeatureInfo> MBEAN_FEATURE_INFO_COMPARATOR
    = new Comparator<MBeanFeatureInfo>() {

    public int compare(MBeanFeatureInfo o1, MBeanFeatureInfo o2)
    {
      return o1.getName().compareTo(o2.getName());
    }
  };

  /**
   * Makes a DynamicMBean out of this.
   */
  public IntrospectionMBean(Object impl, Class mbeanInterface)
    throws NotCompliantMBeanException
  {
    if (impl == null)
      throw new NullPointerException();

    _mbeanInterface = mbeanInterface;

    _mbeanInfo = introspect(impl, mbeanInterface);
    _impl = impl;
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
      MBeanInfo info = null;

      if (infoRef != null && (info = infoRef.get()) != null)
        return info;

      String className = cl.getName();

      TreeSet<MBeanAttributeInfo> attributes
        = new TreeSet<MBeanAttributeInfo>(MBEAN_FEATURE_INFO_COMPARATOR);

      ArrayList<MBeanConstructorInfo> constructors
        = new ArrayList<MBeanConstructorInfo>();

      TreeSet<MBeanOperationInfo> operations
        = new TreeSet<MBeanOperationInfo>(MBEAN_FEATURE_INFO_COMPARATOR);

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

          attributes.add(new MBeanAttributeInfo(name,
                                                getDescription(method),
                                                method,
                                                setter));
        }
        else if (methodName.startsWith("is") && args.length == 0 &&
                 (retType.equals(boolean.class) ||
                  retType.equals(Boolean.class))) {
          String name = methodName.substring(2);

          Method setter = getSetter(methods, name, retType);

          attributes.add(new MBeanAttributeInfo(name,
                                                getDescription(method),
                                                method,
                                                setter));
        }
        else if (methodName.startsWith("set") && args.length == 1) {
          String name = methodName.substring(3);

          Method getter = getGetter(methods, name, args[0]);

          if (getter == null)
            attributes.add(new MBeanAttributeInfo(name,
                                                  getDescription(method),
                                                  method,
                                                  null));
        }
        else {
          operations.add(new MBeanOperationInfo(getName(method),
                                                getDescription(method),
                                                getSignature(method),
                                                method.getReturnType().getName(),
                                                MBeanOperationInfo.UNKNOWN));
        }
      }

      ArrayList<MBeanNotificationInfo> notifications
        = new ArrayList<MBeanNotificationInfo>();

      if (obj instanceof NotificationBroadcaster) {
        NotificationBroadcaster broadcaster;
        broadcaster = (NotificationBroadcaster) obj;

        MBeanNotificationInfo[] notifs = broadcaster.getNotificationInfo();

        if (notifs != null) {
          for (int i = 0; i < notifs.length; i++)
            notifications.add((MBeanNotificationInfo) notifs[i].clone());
        }
      }

      Collections.sort(notifications, MBEAN_FEATURE_INFO_COMPARATOR);

      MBeanAttributeInfo []attrArray = new MBeanAttributeInfo[attributes.size()];
      attributes.toArray(attrArray);
      MBeanConstructorInfo []conArray = new MBeanConstructorInfo[constructors.size()];
      constructors.toArray(conArray);
      MBeanOperationInfo []opArray = new MBeanOperationInfo[operations.size()];
      operations.toArray(opArray);
      MBeanNotificationInfo []notifArray = new MBeanNotificationInfo[notifications.size()];
      notifications.toArray(notifArray);

      info = new MBeanInfo(cl.getName(),
                           getDescription(cl),
                           attrArray,
                           conArray,
                           opArray,
                           notifArray);

      _cachedInfo.put(cl, new SoftReference<MBeanInfo>(info));

      return info;
    } catch (Exception e) {
      NotCompliantMBeanException exn;
      exn = new NotCompliantMBeanException(String.valueOf(e));

      exn.initCause(e);

      throw exn;
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

  /**
   * Returns the class's description.
   */
  static String getDescription(Class cl)
  {
    try {
      Description desc = (Description) cl.getAnnotation(_descriptionAnn);

      if (desc != null)
        return desc.value();
      else
        return "";
    } catch (Throwable e) {
      return "";
    }
  }

  /**
   * Returns the method's description.
   */
  static String getDescription(Method method)
  {
    try {
      Description desc = (Description) method.getAnnotation(_descriptionAnn);

      if (desc != null)
        return desc.value();
      else
        return "";
    } catch (Throwable e) {
      return "";
    }
  }

  /**
   * Returns the method's name, the optional {@link Name} annotation overrides.
   */
  static String getName(Method method)
  {
    try {
      Name name = (Name) method.getAnnotation(_nameAnn);

      if (name != null)
        return name.value();
      else
        return method.getName();
    } catch (Throwable e) {
      return method.getName();
    }
  }

  private static MBeanParameterInfo[] getSignature(Method method)
  {
    Class[] params = method.getParameterTypes();
    MBeanParameterInfo[] paramInfos = new MBeanParameterInfo[params.length];

    for (int i = 0; i < params.length; i++) {
      Class  cl = params[i];

      String name = "p" + i;
      String description = "";

      for (Annotation ann : method.getParameterAnnotations()[i]) {
        if (ann instanceof Name)
          name = ((Name) ann).value();
        else if (ann instanceof Description)
          description = ((Description) ann).value();
      }

      paramInfos[i] = new MBeanParameterInfo(name, cl.getName(), description);
    }

    return paramInfos;
  }

  private static Class findClass(String name)
  {
    try {
      return Class.forName(name);
    } catch (Throwable e) {
      return null;
    }
  }

  static {
    _descriptionAnn = findClass("com.caucho.jmx.Description");
    _nameAnn = findClass("com.caucho.jmx.Name");
  }
}
