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

package com.caucho.config.j2ee;

import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.SingletonGenerator;
import com.caucho.config.program.ComponentValueGenerator;
import com.caucho.config.program.FieldGeneratorProgram;
import com.caucho.config.program.MethodGeneratorProgram;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;


import javax.annotation.*;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.naming.*;
import javax.inject.BindingType;
import javax.inject.Produces;
import javax.inject.Disposes;
import javax.inject.Initializer;
import javax.inject.manager.Bean;
import javax.interceptor.*;
import javax.persistence.*;
import javax.event.Observes;
import javax.inject.Disposes;
//import javax.xml.ws.WebServiceRef;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Analyzes a bean for @Inject tags.
 */
public class InjectIntrospector {
  private static final L10N L = new L10N(InjectIntrospector.class);
  private static final Logger log
    = Logger.getLogger(InjectIntrospector.class.getName());

  private static HashMap<Class,Class> _primitiveTypeMap
    = new HashMap<Class,Class>();

  public static InjectProgram
    introspectProgram(Class type,
		      HashMap<Method,Annotation[]> methodAnnMap)
  {
    ArrayList<ConfigProgram> injectList = new ArrayList<ConfigProgram>();

    if (methodAnnMap == null)
      methodAnnMap = new HashMap<Method,Annotation[]>();

    introspectInject(injectList, type);
    introspectInit(injectList, type, methodAnnMap);

    return new InjectProgram(injectList);
  }
  
  public static void
    introspectInit(ArrayList<ConfigProgram> initList,
		   Class type,
		   HashMap<Method,Annotation[]> methodAnnotationMap)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectInit(initList, type.getSuperclass(), methodAnnotationMap);

    for (Method method : type.getDeclaredMethods()) {
      Annotation []annList = null;

      if (methodAnnotationMap != null)
	annList = methodAnnotationMap.get(method);

      if (annList == null)
	annList = method.getAnnotations();
      
      if (! isAnnotationPresent(annList, PostConstruct.class)
	  && ! isAnnotationPresent(annList, Initializer.class)) {
	continue;
      }

      if (method.getParameterTypes().length == 1
	  && InvocationContext.class.equals(method.getParameterTypes()[0]))
	continue;
      
      if (isAnnotationPresent(annList, PostConstruct.class)
	  && method.getParameterTypes().length != 0) {
          throw new ConfigException(location(method)
				    + L.l("{0}: @PostConstruct is requires zero arguments"));
      }
	
      PostConstructProgram initProgram
	= new PostConstructProgram(method);

      if (! initList.contains(initProgram))
	initList.add(initProgram);
    }
  }

  private static boolean isAnnotationPresent(Annotation []annList, Class type)
  {
    if (annList == null)
      return false;

    for (Annotation ann : annList) {
      if (ann.annotationType().equals(type))
	return true;
    }

    return false;
  }

  private static Annotation getAnnotation(Annotation []annList, Class type)
  {
    if (annList == null)
      return null;

    for (Annotation ann : annList) {
      if (ann.annotationType().equals(type))
	return ann;
    }

    return null;
  }
  
  public static void
    introspectDestroy(ArrayList<ConfigProgram> destroyList, Class type)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectDestroy(destroyList, type.getSuperclass());

    for (Method method : type.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PreDestroy.class)) {
	Class []types = method.getParameterTypes();
	
        if (types.length == 0) {
	}
	else if (types.length == 1 && types[0].equals(InvocationContext.class)) {
	  // XXX:
	  continue;
	}
	else
          throw new ConfigException(location(method)
				    + L.l("@PreDestroy is requires zero arguments"));
	
        PreDestroyInject destroyProgram
          = new PreDestroyInject(method);

        if (! destroyList.contains(destroyProgram))
          destroyList.add(destroyProgram);
      }
    }
  }

  public static void
    introspectConstruct(ArrayList<ConfigProgram> initList, Class type)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    for (Method method : type.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PostConstruct.class)) {
        if (method.getParameterTypes().length != 0)
          throw new ConfigException(L.l("{0}: @PostConstruct is requires zero arguments",
                                        method.getName()));

        PostConstructProgram initProgram
          = new PostConstructProgram(method);

        if (! initList.contains(initProgram))
          initList.add(initProgram);
      }

      if (method.isAnnotationPresent(PreDestroy.class)) {
        if (method.getParameterTypes().length != 0)
          throw new ConfigException(L.l("{0}: @PreDestroy is requires zero arguments",
                                        method.getName()));

        initList.add(new PreDestroyProgram(method));
      }
    }
    
    introspectConstruct(initList, type.getSuperclass());
  }

  public static void introspectInject(ArrayList<ConfigProgram> injectList,
				      Class type)
    throws ConfigException
  {
    try {
      introspectInjectImpl(injectList, type);
    } catch (ClassNotFoundException e) {
      log.warning(type + " injection " + e);
    } catch (NoClassDefFoundError e) {
      log.warning(type + " injection " + e);
    }
  }

  private static void introspectInjectImpl(ArrayList<ConfigProgram> injectList,
					   Class type)
    throws ConfigException, ClassNotFoundException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectInjectImpl(injectList, type.getSuperclass());

    configureClassResources(injectList, type);

    for (Field field : type.getDeclaredFields()) {
      if (hasBindingAnnotation(field)) {
        InjectManager webBeans = InjectManager.create();

	boolean isOptional = isBindingOptional(field);
	
        webBeans.createProgram(injectList, field, isOptional);

        continue;
      }
      
      introspect(injectList, field);
    }

    for (Method method : type.getDeclaredMethods()) {
      String fieldName = method.getName();
      Class []param = method.getParameterTypes();

      /*
      if (hasBindingAnnotation(method)) {
        WebBeansContainer webBeans = WebBeansContainer.create();

        webBeans.createProgram(injectList, method);

        continue;
      }
      */

      if (param.length != 1)
        continue;

      /*
      if (fieldName.startsWith("set") && fieldName.length() > 3) {
        fieldName = fieldName.substring(3);

        char ch = fieldName.charAt(0);

        if (Character.isUpperCase(ch)
	    && (fieldName.length() == 1
		|| Character.isLowerCase(fieldName.charAt(1)))) {
          fieldName = Character.toLowerCase(ch) + fieldName.substring(1);
        }
      }
      */

      introspect(injectList, method);
    }
  }

  public static void
    configureClassResources(ArrayList<ConfigProgram> initList,
                            Class type)
    throws ConfigException
  {
    String location = type.getName() + ": ";

    Resources resources = (Resources) type.getAnnotation(Resources.class);
    if (resources != null) {
      for (Resource resource : resources.value()) {
        introspectClassResource(initList, type, resource);
      }
    }

    Resource resource = (Resource) type.getAnnotation(Resource.class);
    if (resource != null) {
      introspectClassResource(initList, type, resource);
    }

    PersistenceContext pc
      = (PersistenceContext) type.getAnnotation(PersistenceContext.class);

    if (pc != null)
      introspectClassPersistenceContext(initList, type, pc);

    // ejb/0f66
    EJB ejb = (EJB) type.getAnnotation(EJB.class);

    // ejb/0f67
    EJBs ejbs = (EJBs) type.getAnnotation(EJBs.class);

    if (ejb != null && ejbs != null) {
      throw new ConfigException(L.l("{0} cannot have both @EJBs and @EJB",
                                    type.getName()));
    } else if (ejb != null) {
      if (Object.class.equals(ejb.beanInterface()))
	throw new ConfigException(location
				  + L.l("@EJB at the class level must have a beanInterface()"));
      
      if ("".equals(ejb.name()))
	throw new ConfigException(location
				  + L.l("@EJB at the class level must have a name()"));
      
      generateEjb(location, Object.class, "", ejb);
    } else if (ejbs != null) {
      for (EJB childEjb : ejbs.value()) {
	if (Object.class.equals(childEjb.beanInterface()))
	  throw new ConfigException(location
				    + L.l("@EJB at the class level must have a beanInterface()"));
      
	if ("".equals(childEjb.name()))
	  throw new ConfigException(location
				    + L.l("@EJB at the class level must have a name()"));
      
	generateEjb(location, Object.class, "", childEjb);
      }
    }
  }

  private static void
    introspectClassResource(ArrayList<ConfigProgram> initList,
                            Class type,
                            Resource resource)
    throws ConfigException
  {
    String name = resource.name();

    Field field = findField(type, name);

    if (field != null) {
      ValueGenerator gen
	= generateResource(location(field), field.getType(), "", resource);

      return;
    }

    Method method = findMethod(type, name);

    if (method != null) {
      ValueGenerator gen
	= generateResource(location(method),
			   method.getParameterTypes()[0],
			   "", resource);

      return;
    }
  }

  private static void
    introspectClassPersistenceContext(ArrayList<ConfigProgram> initList,
				      Class type,
				      PersistenceContext pContext)
    throws ConfigException
  {
    String location = type.getSimpleName() + ": ";

    ValueGenerator gen
      = generatePersistenceContext(location, EntityManager.class,
				   "", pContext);
  }

  private static void introspect(ArrayList<ConfigProgram> injectList,
				 Field field)
    throws ConfigException
  {
    String location = location(field);
    ValueGenerator gen = null;

    // default jndiName
    String jndiName
      = field.getDeclaringClass().getName() + "/" + field.getName();

    if (field.isAnnotationPresent(Resource.class)) {
      Resource resource = field.getAnnotation(Resource.class);

      gen = generateResource(location, field.getType(), jndiName, resource);
    }
    else if (field.isAnnotationPresent(EJB.class)) {
      EJB ejb = field.getAnnotation(EJB.class);

      gen = generateEjb(location, field.getType(), jndiName, ejb);
    }
    else if (field.isAnnotationPresent(PersistenceUnit.class)) {
      PersistenceUnit pUnit = field.getAnnotation(PersistenceUnit.class);

      gen = generatePersistenceUnit(location, field.getType(), jndiName, pUnit);
    }
    else if (field.isAnnotationPresent(PersistenceContext.class)) {
      PersistenceContext pContext
	= field.getAnnotation(PersistenceContext.class);

      gen = generatePersistenceContext(location, field.getType(), jndiName, pContext);
    }
    /*
    else if (field.isAnnotationPresent(WebServiceRef.class)) {
      WebServiceRef webService
	= field.getAnnotation(WebServiceRef.class);

      gen = generateWebService(location, field.getType(), jndiName, webService);
    }
    */
    else if (hasBindingAnnotation(field))
      introspectWebBean(injectList, field);

    if (gen != null)
      injectList.add(new FieldGeneratorProgram(field, gen));
  }

  private static void introspect(ArrayList<ConfigProgram> injectList,
				 Method method)
    throws ConfigException
  {
    String location = location(method);
    ValueGenerator gen = null;
    Class type = null;

    // default jndi name
    String jndiName
      = method.getDeclaringClass().getName() + "/" + method.getName();

    if (method.getParameterTypes().length > 0)
      type = method.getParameterTypes()[0];

    if (method.isAnnotationPresent(Resource.class)) {
      Resource resource = method.getAnnotation(Resource.class);

      gen = generateResource(location, type, jndiName, resource);
    }
    else if (method.isAnnotationPresent(EJB.class)) {
      EJB ejb = method.getAnnotation(EJB.class);

      gen = generateEjb(location, type, jndiName, ejb);
    }
    else if (method.isAnnotationPresent(PersistenceUnit.class)) {
      PersistenceUnit pUnit = method.getAnnotation(PersistenceUnit.class);

      gen = generatePersistenceUnit(location, type, jndiName, pUnit);
    }
    else if (method.isAnnotationPresent(PersistenceContext.class)) {
      PersistenceContext pContext
	= method.getAnnotation(PersistenceContext.class);

      gen = generatePersistenceContext(location, type, jndiName, pContext);
    }
    /*
    else if (method.isAnnotationPresent(WebServiceRef.class)) {
      WebServiceRef webService
	= method.getAnnotation(WebServiceRef.class);

      gen = generateWebService(location, type, jndiName, webService);
    }
    */
    /*
    else if (hasBindingAnnotation(method))
      introspectWebBean(injectList, method);
    */

    if (gen != null)
      injectList.add(new MethodGeneratorProgram(method, gen));
  }

  /*
  private static ValueGenerator
    generateWebService(String location,
		       Class type,
		       String jndiName,
		       WebServiceRef ref)
    throws ConfigException
  {
    String mappedName = ref.mappedName();

    if (! "".equals(ref.name()))
      jndiName = ref.name();

    return null;
  }
  */

  private static ValueGenerator
    generatePersistenceContext(String location,
			       Class type,
			       String jndiName,
			       PersistenceContext pContext)
    throws ConfigException
  {
    // AmberContainer.create().start();

    PersistenceContextType pType = pContext.type();

    if (PersistenceContextType.EXTENDED.equals(pType))
      return generateExtendedPersistenceContext(location, type,
						jndiName, pContext);
    
    if (! type.isAssignableFrom(EntityManager.class)) {
      throw new ConfigException(location + L.l("@PersistenceContext field type '{0}' must be assignable from EntityManager", type.getName()));
    }

    String unitName = pContext.unitName();

    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
    
    Bean bean;

    if ("".equals(unitName)) {
      bean = bind(location, EntityManager.class, null);

      if (bean == null)
	throw new ConfigException(location + L.l("@PersistenceContext cannot find any persistence contexts.  No JPA persistence-units have been deployed"));
    }
    else {
      bean = bind(location, EntityManager.class, unitName);

      if (bean == null)
	throw new ConfigException(location + L.l("'{0}' is an unknown @PersistenceContext.",
						 unitName));
    }

    bindJndi(location, jndiName, bean);

    return new ComponentValueGenerator(location, (ComponentImpl) bean);
  }

  private static ValueGenerator
    generateExtendedPersistenceContext(String location,
				       Class type,
				       String jndiName,
				       PersistenceContext pContext)
    throws ConfigException
  {
    // AmberContainer.create().start();
    
    if (! type.isAssignableFrom(EntityManager.class)) {
      throw new ConfigException(location + L.l("@PersistenceContext field type '{0}' must be assignable from EntityManager", type.getName()));
    }

    PersistenceContextGenerator gen;
    
    gen = new PersistenceContextGenerator(location, pContext);

    bindJndi(location, jndiName, gen);

    return gen;
  }

  private static ValueGenerator
    generatePersistenceUnit(String location,
			    Class type,
			    String jndiName,
			    PersistenceUnit pUnit)
    throws ConfigException
  {
    if (! type.isAssignableFrom(EntityManagerFactory.class)) {
      throw new ConfigException(location + L.l("@PersistenceUnit field type '{0}' must be assignable from EntityManagerFactory", type.getName()));
    }

    String unitName = pUnit.unitName();

    if (! "".equals(pUnit.name()))
      jndiName = pUnit.name();
    
    InjectManager webBeans = InjectManager.create();
    
    Bean component;

    if ("".equals(unitName)) {
      component = bind(location, EntityManagerFactory.class);

      if (component == null)
	throw new ConfigException(location + L.l("@PersistenceUnit cannot find any persistence units.  No JPA persistence-units have been deployed"));
    }
    else {
      component = bind(location, EntityManagerFactory.class, unitName);

      if (component == null)
	throw new ConfigException(location + L.l("@PersistenceUnit(unitName='{0}') is an unknown persistence unit.  No matching JPA persistence-units have been deployed", unitName));
    }

    bindJndi(location, jndiName, component);

    return new ComponentValueGenerator(location, component);
  }

  private static ValueGenerator generateEjb(String location,
					    Class fieldType,
					    String jndiName,
					    EJB ejb)
    throws ConfigException
  {
    Class type = ejb.beanInterface();
    String mappedName = ejb.mappedName();
    String beanName = ejb.beanName();

    if (! "".equals(ejb.name()))
      jndiName = ejb.name();

    return generateJndiComponent(location, fieldType, type,
				 jndiName, beanName, mappedName);
  }

  private static ValueGenerator generateResource(String location,
						 Class fieldType,
						 String jndiName,
						 Resource resource)
    throws ConfigException
  {
    String mappedName = resource.mappedName();
    Class type = resource.type();

    if (! "".equals(resource.name()))
      jndiName = resource.name();

    return generateJndiComponent(location, fieldType, type,
				 jndiName, mappedName, "");
  }

  private static void introspectWebBean(ArrayList<ConfigProgram> injectList,
					Field field)
    throws ConfigException
  {
    InjectManager webBeans = InjectManager.create();

    boolean isOptional = false;
    webBeans.createProgram(injectList, field, isOptional);
  }

  private static void introspectWebBean(ArrayList<ConfigProgram> injectList,
					Method method)
    throws ConfigException
  {
    InjectManager webBeans = InjectManager.create();

    webBeans.createProgram(injectList, method);
  }

  /**
   * Creates the value.
   */
  private static ValueGenerator
    generateJndiComponent(String location,
			  Class fieldType,
			  Class type,
			  String jndiName,
			  String mappedName,
			  String beanName)
  {
    if (! fieldType.isAssignableFrom(type))
      type = fieldType;

    if (type.isPrimitive())
      type = _primitiveTypeMap.get(type);
    
    Object value = Jndi.lookup(jndiName);

    // XXX: can use lookup-link and store the proxy

    if (value != null)
      return new SingletonGenerator(value);
    
    Bean component = null;

    if (mappedName == null || "".equals(mappedName))
      mappedName = jndiName;

    component = bind(location, type, mappedName);

    if (component != null) {
      bindJndi(location, jndiName, component);
      
      return new ComponentValueGenerator(location, component);
    }
    
    if (component == null && beanName != null && ! "".equals(beanName)) {
      component = bind(location, type, beanName);
      if (component != null) {
	bindJndi(location, jndiName, component);
      
	return new ComponentValueGenerator(location, component);
      }
    }
    
    if (component == null && jndiName != null && ! "".equals(jndiName)) {
      component = bind(location, type, jndiName);

      if (component != null) {
	bindJndi(location, jndiName, component);
      
	return new ComponentValueGenerator(location, component);
      }
    }

    if (component == null)
      component = bind(location, type);

    if (component != null) {
      bindJndi(location, jndiName, component);
      
      return new ComponentValueGenerator(location, component);
    }

    else
      throw new ConfigException(location + L.l("{0} with mappedName={1}, beanName={2}, and jndiName={3} does not match anything",
                                             type.getName(),
                                             mappedName,
                                             beanName,
                                             jndiName));

    /*
      if (_component != null && _jndiName != null && ! "".equals(_jndiName)) {
	try {
	  Jndi.bindDeepShort(_jndiName, _component);
	} catch (NamingException e) {
	  throw new ConfigException(e);
	}
      }
    }

    if (component != null)
      return component.get();
    else
      return getJndiValue(_type);
    */
  }

  public static Bean bind(String location, Class type)
  {
    return bind(location, type, null);
  }

  public static Bean bind(String location, Class type, String name)
  {
    InjectManager webBeans = InjectManager.create();

    Set<Bean> beans = webBeans.resolveAllByType(type);

    if (beans == null || beans.size() == 0)
      return null;

    for (Bean bean : beans) {
      // XXX: dup
      
      if (name == null || name.equals(bean.getName()))
	return bean;
    }

    return null;
  }

  private static void bindJndi(String location, String name, Object value)
  {
    try {
      if (! "".equals(name))
        Jndi.bindDeepShort(name, value);
    } catch (NamingException e) {
      throw new ConfigException(location + e.getMessage(), e);
    }
  }

  private static Field findField(Class type, String name)
  {
    for (Field field : type.getDeclaredFields()) {
      if (field.getName().equals(name))
        return field;
    }

    return null;
  }

  private static Method findMethod(Class type, String name)
  {
    for (Method method : type.getDeclaredMethods()) {
      if (method.getParameterTypes().length != 1)
        continue;

      String methodName = method.getName();
      if (! methodName.startsWith("set"))
        continue;

      methodName = Introspector.decapitalize(methodName.substring(3));

      if (name.equals(methodName))
        return method;
    }

    return null;
  }

  private static boolean hasBindingAnnotation(Field field)
  {
    for (Annotation ann : field.getAnnotations()) {
      Class annType = ann.annotationType();

      if (annType.isAnnotationPresent(BindingType.class))
	return true;
    }

    return false;
  }

  private static boolean isBindingOptional(Field field)
  {
    for (Annotation ann : field.getAnnotations()) {
      Class annType = ann.annotationType();
      
      if (annType.isAnnotationPresent(BindingType.class))
	return false;
    }

    return false;
  }

  private static boolean hasBindingAnnotation(Method method)
  {
    for (Annotation ann : method.getAnnotations()) {
      Class annType = ann.annotationType();
      
      if (annType.equals(Produces.class))
	return false;
      /*
      else if (annType.equals(Destroys.class))
	return false;
      */
    }

    boolean hasBinding = false;
    for (Annotation []annList : method.getParameterAnnotations()) {
      if (annList == null)
        continue;

      for (Annotation ann : annList) {
	Class annType = ann.annotationType();
	
	if (annType.equals(Observes.class))
	  return false;
	if (annType.equals(Disposes.class))
	  return false;
        else if (annType.isAnnotationPresent(BindingType.class))
	  hasBinding = true;
      }
    }

    return hasBinding;
  }

  private static String toFullName(String jndiName)
  {
    int colon = jndiName.indexOf(':');
    int slash = jndiName.indexOf('/');

    if (colon < 0 || slash > 0 && slash < colon)
      jndiName = "java:comp/env/" + jndiName;

    return jndiName;
  }

  private static ConfigException error(Field field, String msg)
  {
    return new ConfigException(location(field) + msg);
  }

  private static ConfigException error(Method method, String msg)
  {
    return new ConfigException(location(method) + msg);
  }

  private static String location(Field field)
  {
    String className = field.getDeclaringClass().getName();

    return className + "." + field.getName() + ": ";
  }

  private static String location(Method method)
  {
    String className = method.getDeclaringClass().getName();

    return className + "." + method.getName() + ": ";
  }

  static {
    _primitiveTypeMap.put(boolean.class, Boolean.class);
    _primitiveTypeMap.put(byte.class, Byte.class);
    _primitiveTypeMap.put(char.class, Character.class);
    _primitiveTypeMap.put(short.class, Short.class);
    _primitiveTypeMap.put(int.class, Integer.class);
    _primitiveTypeMap.put(long.class, Long.class);
    _primitiveTypeMap.put(float.class, Float.class);
    _primitiveTypeMap.put(double.class, Double.class);
  }
}
