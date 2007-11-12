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

package com.caucho.webbeans;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.util.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.inject.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.URL;

import javax.webbeans.*;

/**
 * The web beans for a given environment (?)
 */
public class WebBeans implements EnvironmentListener {
  private static final L10N L = new L10N(WebBeans.class);
  private static final Logger log
    = Logger.getLogger(WebBeans.class.getName());
  
  private static final String SCHEMA = "com/caucho/webbeans/cfg/webbeans.rnc";

  private static final EnvironmentLocal<WebBeans> _localWebBeans
    = new EnvironmentLocal<WebBeans>();
  
  private EnvironmentClassLoader _loader;

  private WbWebBeans _wbWebBeans;
  
  private HashMap<Path,WbWebBeans> _webBeansMap
    = new HashMap<Path,WbWebBeans>();

  private HashMap<Class,WebComponent> _componentMap
    = new HashMap<Class,WebComponent>();

  private HashMap<String,WbComponent> _namedComponentMap
    = new HashMap<String,WbComponent>();

  private RuntimeException _configException;

  private WebBeans()
  {
    _loader = Environment.getEnvironmentClassLoader();
    
    _wbWebBeans = new WbWebBeans(this, null);

    Environment.addEnvironmentListener(this);
  }

  public static WebBeans getLocal()
  {
    WebBeans webBeans = null;
    
    synchronized (_localWebBeans) {
      webBeans = _localWebBeans.getLevel();

      if (webBeans != null)
	return webBeans;
      
      webBeans = new WebBeans();
      _localWebBeans.set(webBeans);
    }

    webBeans.init();

    return webBeans;
  }

  public WbWebBeans getWbWebBeans()
  {
    return _wbWebBeans;
  }

  private void init()
  {
    try {
      update();
    } catch (RuntimeException e) {
      _configException = e;
      
      throw _configException;
    } catch (Exception e) {
      _configException = new ConfigException(e);

      throw _configException;
    }

    try {
      
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    Environment.addEnvironmentListener(this);
  }

  public void addComponent(WbComponent comp)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " adding " + comp);
    
    addComponentRec(comp.getTargetType(), comp);

    String name = comp.getName();

    if (name != null && comp.getScopeAnnotation() != null)
      _namedComponentMap.put(name, comp);
  }

  private void addComponentRec(Class cl, WbComponent comp)
  {
    if (cl == null)
      return;
    
    WebComponent webComponent = _componentMap.get(cl);

    if (webComponent == null) {
      webComponent = new WebComponent(cl);
      _componentMap.put(cl, webComponent);
    }

    webComponent.addComponent(comp);

    addComponentRec(cl.getSuperclass(), comp);

    for (Class subClass : cl.getInterfaces()) {
      addComponentRec(subClass, comp);
    }
  }

  public ScopeContext getScopeContext(Annotation scopeAnn)
  {
    if (scopeAnn instanceof RequestScoped)
      return new RequestScope();
    else if (scopeAnn instanceof SessionScoped)
      return new SessionScope();
    else if (scopeAnn instanceof ApplicationScoped)
      return new ApplicationScope();
    else if (scopeAnn instanceof ConversationScoped)
      return new ConversationScope();
    else
      throw new IllegalArgumentException(L.l("'{0}' is an unknown scope.",
					     scopeAnn));
  }

  public void createProgram(ArrayList<BuilderProgram> initList,
			    AccessibleObject field,
			    String fieldName,
			    Class fieldType,
			    AccessibleInject inject)
    throws ConfigException
  {
    Annotation componentAnn = null;
    Annotation bindingAnn = null;
    String name = null;

    Named nameAnn = field.getAnnotation(Named.class);
    if (nameAnn != null)
      name = nameAnn.value();

    WebComponent component;

    component = _componentMap.get(fieldType);

    if (component == null) {
      throw injectError(field, L.l("'{0}' is an unknown component type.",
				   fieldType.getName()));
    }

    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();
    for (Annotation ann : field.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }

    component.createProgram(initList, field, name, inject, bindingList);
  }

  public void createProgram(ArrayList<BuilderProgram> initList,
			    Method method)
    throws ConfigException
  {
    // XXX: lazy binding
    try {
      Class []paramTypes = method.getParameterTypes();
      Annotation[][]paramAnn = method.getParameterAnnotations();
      
      WbComponent []args = new WbComponent[paramTypes.length];

      for (int i = 0; i < args.length; i++) {
	args[i] = bind(paramTypes[i], paramAnn[i]);
      }

      initList.add(new InjectMethodProgram(method, args));
    } catch (Exception e) {
      String className = method.getDeclaringClass().getSimpleName();
      String loc = className + '.' + method.getName() + ": ";

      if (e instanceof ConfigException)
	throw new ConfigException(loc + e.getMessage(), e);
      else
	throw new ConfigException(loc + e, e);
    }
  }

  /**
   * Returns the web beans component corresponding to a method
   * parameter.
   */
  public WbComponent bind(Class type, Annotation []paramAnn)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : paramAnn) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }

    return bind(type, bindingList);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public WbComponent bind(Class type, ArrayList<Annotation> bindingList)
  {
    WebComponent component = _componentMap.get(type);

    if (component == null) {
      throw new ConfigException(L.l("'{0}' is an unknown component type.",
				    type.getName()));
    }

    return component.bind(bindingList);
  }

  public Object findByName(String name)
  {
    WbComponent comp = _namedComponentMap.get(name);

    if (comp != null)
      return comp.get();
    else
      return null;
  }

  public void update()
  {
    try {
      _loader.make();
    
      fillClassPath();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  private void fillClassPath()
    throws IOException
  {
    Enumeration<URL> e = _loader.getResources("META-INF/web-beans.xml");

    while (e.hasMoreElements()) {
      URL url = e.nextElement();

      Path path = Vfs.lookup(url.toString());
      path.setUserPath(url.toString());
      
      Path root = path.getParent().getParent();

      if (_webBeansMap.get(path) != null)
	continue;

      if (log.isLoggable(Level.FINE))
	log.fine(this + " scanning " + root.getURL());

      WbWebBeans webBeans = new WbWebBeans(this, root);
      _webBeansMap.put(path, webBeans);

      new Config().configure(webBeans, path, SCHEMA);
    }
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
    throws Exception
  {
    loader.make();
    
    fillClassPath();

    _wbWebBeans.init();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }

  public static ConfigException injectError(AccessibleObject prop, String msg)
  {
    String location = "";
    
    if (prop instanceof Field) {
      Field field = (Field) prop;
      String className = field.getDeclaringClass().getName();

      int p = className.lastIndexOf('.');
      className = className.substring(p + 1);

      location = className + "." + field.getName() + ": ";
    }
    else if (prop instanceof Method) {
      Method method = (Method) prop;
      String className = method.getDeclaringClass().getName();

      int p = className.lastIndexOf('.');
      className = className.substring(p + 1);

      location = className + "." + method.getName() + ": ";
    }

    return new ConfigException(location + msg);
  }

  public String toString()
  {
    return "WebBeans[" + _loader.getId() + "]";
  }
}
