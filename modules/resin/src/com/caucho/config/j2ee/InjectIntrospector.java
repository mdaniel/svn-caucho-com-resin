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

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.webbeans.component.ComponentImpl;
import com.caucho.webbeans.manager.WebBeansContainer;

import org.omg.CORBA.ORB;

import javax.annotation.*;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.ejb.SessionContext;
import javax.ejb.MessageDrivenContext;
import javax.jms.QueueConnectionFactory;
import javax.naming.*;
import javax.persistence.*;
import javax.transaction.UserTransaction;
import javax.webbeans.*;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.*;

/**
 * Analyzes a bean for @Inject tags.
 */
public class InjectIntrospector {
  private static final L10N L = new L10N(InjectIntrospector.class);
  private static final Logger log = Log.open(InjectIntrospector.class);
  
  public static void
    introspectInit(ArrayList<Inject> initList, Class type)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectInit(initList, type.getSuperclass());

    for (Method method : type.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PostConstruct.class)) {
        if (method.getParameterTypes().length != 0)
          throw new ConfigException(location(method)
				    + L.l("{0}: @PostConstruct is requires zero arguments"));
	
	/*
        PostConstructProgram initProgram
          = new PostConstructProgram(method);

        if (! initList.contains(initProgram))
          initList.add(initProgram);
	*/
      }
    }
  }

  public static void
    introspectConstruct(ArrayList<BuilderProgram> initList, Class type)
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

  public static void
    introspectDestroy(ArrayList<BuilderProgram> initList, Class type)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectDestroy(initList, type.getSuperclass());

    for (Method method : type.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PreDestroy.class)) {
        if (method.getParameterTypes().length != 0)
          throw new ConfigException(L.l("{0}: @PreDestroy is requires zero arguments",
                                        method.getName()));

        initList.add(new PreDestroyProgram(method));
      }
    }
  }

  public static void introspectInject(ArrayList<Inject> injectList,
				      Class type)
    throws ConfigException
  {
    try {
      introspectInjectImpl(injectList, type);
    } catch (ClassNotFoundException e) {
    } catch (NoClassDefFoundError e) {
      // occurs in some TCK tests
    }
  }

  private static void introspectInjectImpl(ArrayList<Inject> injectList,
					   Class type)
    throws ConfigException, ClassNotFoundException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectInjectImpl(injectList, type.getSuperclass());

    configureClassResources(injectList, type);

    for (Field field : type.getDeclaredFields()) {
      if (hasBindingAnnotation(field)) {
        WebBeansContainer webBeans = WebBeansContainer.create();

        webBeans.createProgram(injectList, field);

        continue;
      }
      
      introspect(injectList, field);
    }

    for (Method method : type.getDeclaredMethods()) {
      String fieldName = method.getName();
      Class []param = method.getParameterTypes();

      if (hasBindingAnnotation(method)) {
        WebBeansContainer webBeans = WebBeansContainer.create();

        webBeans.createProgram(injectList, method);

        continue;
      }

      if (param.length != 1)
        continue;

      if (fieldName.startsWith("set") && fieldName.length() > 3) {
        fieldName = fieldName.substring(3);

        char ch = fieldName.charAt(0);

        if (Character.isUpperCase(ch)
	    && (fieldName.length() == 1
		|| Character.isLowerCase(fieldName.charAt(1)))) {
          fieldName = Character.toLowerCase(ch) + fieldName.substring(1);
        }
      }

      introspect(injectList, method);
    }
  }

  public static void
    configureClassResources(ArrayList<Inject> initList,
                            Class type)
    throws ConfigException
  {
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
    /*
    if (pc != null)
      introspectClassPersistenceContext(initList, type);
    }
    */

    // ejb/0f66
    EJB ejb = (EJB) type.getAnnotation(EJB.class);

    // ejb/0f67
    EJBs ejbs = (EJBs) type.getAnnotation(EJBs.class);

    if (ejb != null && ejbs != null) {
      throw new ConfigException(L.l("{0} cannot have both @EJBs and @EJB",
                                    type.getName()));
    } else if (ejb != null) {
      /*
      initList.add(new JndiBindProgram("java:comp/env/" + ejb.name(),
                                       localJndiPrefix + "/" + ejb.beanName(),
                                       null));
      initList.add(new EjbRefProgram("java:comp/env/" + ejb.name(),
                                     ejb.beanName(),
                                     ejb.beanInterface()));
      */
    } else if (ejbs != null) {
      /*
      for (EJB e : ejbs.value()) {
        initList.add(new EjbRefProgram("java:comp/env/" + e.name(),
                                       e.beanName(),
                                       e.beanInterface()));
      }
      */
    }
  }

  private static void
    introspectClassResource(ArrayList<Inject> initList,
                            Class type,
                            Resource resource)
    throws ConfigException
  {
    /*
    String name = resource.name();

    Field field = findField(type, name);

    if (field != null) {
      ValueGenerator gen
	= createResourceGenertor(location(field), field.getType(), resource);

      return;
    }

    Method method = findMethod(type, name);

    if (method != null) {
      ValueGenerator gen
	= createResourceGenerator(location(method),
				  method.getParameterTypes()[0],
				  resource);

      return;
    }
    */
  }

  private static void introspect(ArrayList<Inject> injectList,
				 Field field)
    throws ConfigException
  {
    String location = location(field);
    ValueGenerator gen = null;

    if (field.isAnnotationPresent(Resource.class)) {
      Resource resource = field.getAnnotation(Resource.class);

      gen = generateResource(location, field.getType(), resource);
    }
    else if (field.isAnnotationPresent(EJB.class)) {
      EJB ejb = field.getAnnotation(EJB.class);

      gen = generateEjb(location, field.getType(), ejb);
    }
    else if (field.isAnnotationPresent(PersistenceUnit.class)) {
      PersistenceUnit pUnit = field.getAnnotation(PersistenceUnit.class);

      gen = generatePersistenceUnit(location, field.getType(), pUnit);
    }
    else if (field.isAnnotationPresent(PersistenceContext.class)) {
      PersistenceContext pContext
	= field.getAnnotation(PersistenceContext.class);

      gen = generatePersistenceContext(location, field.getType(), pContext);
    }
    else if (field.isAnnotationPresent(WebServiceRef.class)) {
      WebServiceRef webService
	= field.getAnnotation(WebServiceRef.class);

      gen = generateWebService(location, field.getType(), webService);
    }
    else if (hasBindingAnnotation(field))
      introspectWebBean(injectList, field);

    if (gen != null)
      injectList.add(new FieldInject(field, gen));
  }

  private static void introspect(ArrayList<Inject> injectList,
				 Method method)
    throws ConfigException
  {
    String location = location(method);
    ValueGenerator gen = null;
    Class type = null;

    if (method.getParameterTypes().length > 0)
      type = method.getParameterTypes()[0];

    if (method.isAnnotationPresent(Resource.class)) {
      Resource resource = method.getAnnotation(Resource.class);

      gen = generateResource(location, type, resource);
    }
    else if (method.isAnnotationPresent(EJB.class)) {
      EJB ejb = method.getAnnotation(EJB.class);

      gen = generateEjb(location, type, ejb);
    }
    else if (method.isAnnotationPresent(PersistenceUnit.class)) {
      PersistenceUnit pUnit = method.getAnnotation(PersistenceUnit.class);

      gen = generatePersistenceUnit(location, type, pUnit);
    }
    else if (method.isAnnotationPresent(PersistenceContext.class)) {
      PersistenceContext pContext
	= method.getAnnotation(PersistenceContext.class);

      gen = generatePersistenceContext(location, type, pContext);
    }
    else if (method.isAnnotationPresent(WebServiceRef.class)) {
      WebServiceRef webService
	= method.getAnnotation(WebServiceRef.class);

      gen = generateWebService(location, type, webService);
    }
    else if (hasBindingAnnotation(method))
      introspectWebBean(injectList, method);

    if (gen != null)
      injectList.add(new MethodInject(method, gen));
  }

  private static ValueGenerator
    generateWebService(String location,
		       Class type,
		       WebServiceRef ref)
    throws ConfigException
  {
    String jndiName = ref.name();
    String mappedName = ref.mappedName();

    /*
    if (Service.class.isAssignableFrom(fieldType)) {
      program = new ServiceInjectProgram(name,
                                         fieldType,
                                         inject);
    }
    else {
      program = new ServiceProxyInjectProgram(name,
                                              fieldType,
                                              inject);
    }
    */

    return null;
  }

  private static ValueGenerator
    generatePersistenceContext(String location,
			       Class type,
			       PersistenceContext pContext)
    throws ConfigException
  {
    if (! type.isAssignableFrom(EntityManager.class)) {
      throw new ConfigException(location + L.l("@PersistenceContext field type '{0}' must be assignable from EntityManager", type.getName()));
    }

    String unitName = pContext.unitName();
    String jndiName = pContext.name();
    
    WebBeansContainer webBeans = WebBeansContainer.create();
    
    ComponentImpl component;

    if ("".equals(unitName))
      component = webBeans.bind(location, EntityManager.class);
    else
      component = webBeans.bind(location, EntityManager.class, unitName);


    bindJndi(location, jndiName, component);

    return new ComponentGenerator(location, component);
  }

  private static ValueGenerator
    generatePersistenceUnit(String location,
			    Class type,
			    PersistenceUnit pUnit)
    throws ConfigException
  {
    if (! type.isAssignableFrom(EntityManagerFactory.class)) {
      throw new ConfigException(location + L.l("@PersistenceUnit field type '{0}' must be assignable from EntityManagerFactory", type.getName()));
    }

    String unitName = pUnit.unitName();
    String jndiName = pUnit.name();
    
    WebBeansContainer webBeans = WebBeansContainer.create();
    
    ComponentImpl component;

    if ("".equals(unitName))
      component = webBeans.bind(location, EntityManager.class);
    else
      component = webBeans.bind(location, EntityManager.class, unitName);

    bindJndi(location, jndiName, component);

    return new ComponentGenerator(location, component);
  }

  private static ValueGenerator generateEjb(String location,
					    Class fieldType,
					    EJB ejb)
    throws ConfigException
  {
    Class type = ejb.beanInterface();
    String jndiName = ejb.name();
    String mappedName = ejb.mappedName();
    String beanName = ejb.beanName();

    return generateJndiComponent(location, fieldType, type,
				 jndiName, mappedName, beanName);
  }

  private static ValueGenerator generateResource(String location,
						 Class fieldType,
						 Resource resource)
    throws ConfigException
  {
    String mappedName = resource.mappedName();
    String jndiName = resource.name();
    Class type = resource.type();

    return generateJndiComponent(location, fieldType, type,
				 jndiName, mappedName, "");
  }

  private static void introspectWebBean(ArrayList<Inject> injectList,
					Field field)
    throws ConfigException
  {
    WebBeansContainer webBeans = WebBeansContainer.create();

    webBeans.createProgram(injectList, field);
  }

  private static void introspectWebBean(ArrayList<Inject> injectList,
					Method method)
    throws ConfigException
  {
    WebBeansContainer webBeans = WebBeansContainer.create();

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
    
    WebBeansContainer webBeans = WebBeansContainer.create();

    ComponentImpl component = null;

    if (mappedName != null && ! "".equals(mappedName)) {
      component = webBeans.bind(location, type, mappedName);

      if (component != null) {
	bindJndi(location, jndiName, component);
      
	return new ComponentGenerator(location, component);
      }
    }
    else if (beanName != null && ! "".equals(beanName)) {
      component = webBeans.bind(location, type, beanName);

      if (component != null) {
	bindJndi(location, jndiName, component);
      
	return new ComponentGenerator(location, component);
      }
    }
    
    if (jndiName != null && ! "".equals(jndiName)) {
      component = webBeans.bind(location, type, jndiName);

      if (component != null) {
	bindJndi(location, jndiName, component);
      
	return new ComponentGenerator(location, component);
      }
    }
    
    component = webBeans.bind(location, type);

    if (component != null) {
      bindJndi(location, jndiName, component);
      
      return new ComponentGenerator(location, component);
    }

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

  private static void bindJndi(String location, String name, Object value)
  {
    try {
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
    if (field.isAnnotationPresent(In.class))
      return true;

    for (Annotation ann : field.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	return true;
    }

    return false;
  }

  private static boolean hasBindingAnnotation(Method method)
  {
    if (method.isAnnotationPresent(Produces.class))
      return false;
    else if (method.isAnnotationPresent(In.class))
      return true;

    for (Annotation []annList : method.getParameterAnnotations()) {
      if (annList == null)
        continue;

      for (Annotation ann : annList) {
        if (ann.annotationType().isAnnotationPresent(BindingType.class))
          return true;
      }
    }

    return false;
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
}
