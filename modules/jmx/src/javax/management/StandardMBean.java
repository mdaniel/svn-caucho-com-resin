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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.management;

import java.util.WeakHashMap;
  
import java.util.logging.Logger;
import java.util.logging.Level;

import java.lang.ref.SoftReference;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;

/**
 * Represents a standard MBean.
 */
public class StandardMBean implements DynamicMBean {
  private static final Logger log = Logger.getLogger(StandardMBean.class.getName());
  private static final Class[] NULL_ARG = new Class[0];

  private static WeakHashMap _introspectionCache = new WeakHashMap();
  
  private Object _impl;
  private Class _mbeanInterface;
  private MBeanInfo _reflectedInfo;
  private MBeanInfo _mbeanInfo;
  
  /**
   * Makes a DynamicMBean out of this.
   */
  protected StandardMBean(Class mbeanInterface)
    throws NotCompliantMBeanException
  {
    _mbeanInterface = mbeanInterface;
    
    MBeanInfo info = introspect(mbeanInterface);

    _reflectedInfo = info;
    
    setImplementation(this);
  }
  
  /**
   * Makes a DynamicMBean out of this.
   */
  public StandardMBean(Object impl, Class mbeanInterface)
    throws NotCompliantMBeanException
  {
    if (impl == null)
      throw new NullPointerException();
    
    _mbeanInterface = mbeanInterface;
    
    MBeanInfo info = introspect(mbeanInterface);

    _reflectedInfo = info;
    
    setImplementation(impl);
  }

  /**
   * Sets the implementation.
   */
  public void setImplementation(Object implementation)
    throws NotCompliantMBeanException
  {
    if (implementation == null)
      throw new NullPointerException();

    if (! _mbeanInterface.isAssignableFrom(implementation.getClass()))
      throw new NotCompliantMBeanException();
				     
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
   * Returns the implementation class
   */
  public Class getImplementationClass()
  {
    return _mbeanInterface;
  }

  /**
   * Returns the interface class
   */
  public final Class getMBeanInterface()
  {
    return _mbeanInterface;
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
	return method.invoke(_impl, null);
      else
	throw new AttributeNotFoundException(attribute);
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      throw new MBeanException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e.toString());
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

	if (method != null)
	  list.add(new Attribute(attributes[i], method.invoke(_impl, null)));
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
    MBeanInfo info = getCachedMBeanInfo();

    if (info != null)
      return info;

    info = calculateMBeanInfo(getReflectedInfo());

    cacheMBeanInfo(info);
    
    return info;
  }

  /**
   * Returns the className used in the mbeanInfo
   */
  protected String getClassName(MBeanInfo info)
  {
    return info.getClassName();
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanInfo info)
  {
    return info.getDescription();
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanFeatureInfo info)
  {
    return info.getDescription();
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanAttributeInfo info)
  {
    return getDescription((MBeanFeatureInfo) info);
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanConstructorInfo info)
  {
    return getDescription((MBeanFeatureInfo) info);
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanConstructorInfo info,
				  MBeanParameterInfo param,
				  int sequence)
  {
    return param.getDescription();
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getParameterName(MBeanConstructorInfo info,
				    MBeanParameterInfo param,
				    int sequence)
  {
    return param.getName();
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanOperationInfo info)
  {
    return getDescription((MBeanFeatureInfo) info);
  }

  /**
   * Returns the impact used in the mbeanInfo
   */
  protected int getImpact(MBeanOperationInfo info)
  {
    return info.getImpact();
  }

  /**
   * Returns the parameter name used in the mbeanInfo
   */
  protected String getParameterName(MBeanOperationInfo info,
				    MBeanParameterInfo param,
				    int sequence)
  {
    return param.getName();
  }

  /**
   * Returns the description used in the mbeanInfo
   */
  protected String getDescription(MBeanOperationInfo info,
				  MBeanParameterInfo param,
				  int sequence)
  {
    return param.getDescription();
  }

  /**
   * Returns the constructor used in the mbeanInfo
   */
  protected MBeanConstructorInfo []getConstructors(MBeanConstructorInfo []ctors,
						   Object impl)
  {
    if (impl != getImplementation())
      return null;
    
    return ctors;
  }

  /**
   * Returns the cached
   */
  protected MBeanInfo getCachedMBeanInfo()
  {
    return _mbeanInfo;
  }

  /**
   * Sets the cached
   */
  protected void cacheMBeanInfo(MBeanInfo info)
  {
    _mbeanInfo = info;
  }

  /**
   * Returns the introspection information for the MBean.
   */
  private MBeanInfo getReflectedInfo()
  {
    return _reflectedInfo;
  }

  /**
   * Calculate mbean info.
   */
  private MBeanInfo calculateMBeanInfo(MBeanInfo reflectedInfo)
  {
    String className = getClassName(reflectedInfo);
    String description = getDescription(reflectedInfo);
    
    MBeanConstructorInfo []oldCtors;
    oldCtors = getConstructors(reflectedInfo.getConstructors(),
			       getImplementation());

    MBeanConstructorInfo []ctors = null;
    if (oldCtors != null) {
      ctors = new MBeanConstructorInfo[oldCtors.length];

      for (int i = 0; i < oldCtors.length; i++) {
	String name = oldCtors[i].getName();
	String ctorDescription = getDescription(oldCtors[i]);

	MBeanParameterInfo []oldSig = oldCtors[i].getSignature();
	MBeanParameterInfo []sig = new MBeanParameterInfo[oldSig.length];

	for (int j = 0; j < sig.length; j++) {
	  String paramName = getParameterName(oldCtors[i], oldSig[j], j);
	  String paramType = oldSig[j].getType();
	  String paramDesc = getDescription(oldCtors[i], oldSig[j], j);

	  sig[j] = new MBeanParameterInfo(paramName, paramType, paramDesc);
	}

	ctors[i] = new MBeanConstructorInfo(name, ctorDescription, sig);
      }
    }

    MBeanAttributeInfo []oldAttrs = reflectedInfo.getAttributes();;
    MBeanAttributeInfo []attrs = null;
    if (oldAttrs != null) {
      attrs = new MBeanAttributeInfo[oldAttrs.length];

      for (int i = 0; i < oldAttrs.length; i++) {
	String name = oldAttrs[i].getName();
	String attrDescription = getDescription(oldAttrs[i]);
	String type = oldAttrs[i].getType();
	boolean isIs = oldAttrs[i].isIs();
	boolean isReadable = oldAttrs[i].isReadable();
	boolean isWritable = oldAttrs[i].isWritable();

	attrs[i] = new MBeanAttributeInfo(name, type, attrDescription,
					  isReadable, isWritable, isIs);
      }
    }

    MBeanOperationInfo []oldOps = reflectedInfo.getOperations();
    MBeanOperationInfo []ops = null;
    if (oldOps != null) {
      ops = new MBeanOperationInfo[oldOps.length];

      for (int i = 0; i < oldOps.length; i++) {
	String name = oldOps[i].getName();
	String opDescription = getDescription(oldOps[i]);

	String returnType = oldOps[i].getReturnType();
	int impact = getImpact(oldOps[i]);
	
	MBeanParameterInfo []oldSig = oldOps[i].getSignature();
	MBeanParameterInfo []sig = new MBeanParameterInfo[oldSig.length];

	for (int j = 0; j < sig.length; j++) {
	  String paramName = getParameterName(oldOps[i], oldSig[j], j);
	  String paramType = oldSig[j].getType();
	  String paramDesc = getDescription(oldOps[i], oldSig[j], j);

	  sig[j] = new MBeanParameterInfo(paramName, paramType, paramDesc);
	}

	ops[i] = new MBeanOperationInfo(name, opDescription, sig,
					returnType, impact);
      }
    }

    MBeanNotificationInfo []notifs = null;
    Object obj = getImplementation();

    if (obj instanceof NotificationBroadcaster) {
      NotificationBroadcaster broadcaster;
      broadcaster = (NotificationBroadcaster) obj;

      notifs = broadcaster.getNotificationInfo();
    }

    if (notifs == null)
      notifs = new MBeanNotificationInfo[0];

    return new MBeanInfo(className, description,
			 attrs, ctors, ops, notifs);
  }

  static MBeanInfo introspect(Class cl)
    throws NotCompliantMBeanException
  {
    try {
      SoftReference ref = (SoftReference) _introspectionCache.get(cl);
      MBeanInfo info;

      if (ref != null && (info = (MBeanInfo) ref.get()) != null)
	return info;
      
      String className = cl.getName();
      String description = "Standard MBean for " + className;

      ArrayList attributes;
      attributes = new ArrayList();

      ArrayList constructors;
      constructors = new ArrayList();

      ArrayList operations;
      operations = new ArrayList();

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
	
	  attributes.add(new MBeanAttributeInfo(name, name, method, setter));
	}
	else if (methodName.startsWith("is") && args.length == 0 &&
	    (retType.equals(boolean.class) ||
	     retType.equals(Boolean.class))) {
	  String name = methodName.substring(2);

	  Method setter = getSetter(methods, name, retType);
	
	  attributes.add(new MBeanAttributeInfo(name, name, method, setter));
	}
	else if (methodName.startsWith("set") && args.length == 1) {
	  String name = methodName.substring(3);

	  Method getter = getGetter(methods, name, args[0]);

	  if (getter == null)
	    attributes.add(new MBeanAttributeInfo(name, name, null, method));
	}

	operations.add(new MBeanOperationInfo(methodName, method));
      }

      MBeanNotificationInfo []notifs = new MBeanNotificationInfo[0];

      MBeanAttributeInfo []attrArray = new MBeanAttributeInfo[attributes.size()];
      attributes.toArray(attrArray);
      MBeanConstructorInfo []conArray = new MBeanConstructorInfo[constructors.size()];
      constructors.toArray(conArray);
      MBeanOperationInfo []opArray = new MBeanOperationInfo[operations.size()];
      operations.toArray(opArray);

      info = new MBeanInfo(className, description,
			   attrArray, conArray, opArray, notifs);

      _introspectionCache.put(cl, new SoftReference(info));

      return info;
    } catch (Exception e) {
      throw new NotCompliantMBeanException(String.valueOf(e));
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
}
