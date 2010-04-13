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

package com.caucho.config.inject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Extension;

import com.caucho.config.program.BeanArg;
import com.caucho.config.reflect.BaseType;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * Manages custom extensions for the inject manager.
 */
class ExtensionManager
{
  private static final L10N L = new L10N(ExtensionManager.class);
  private static final Logger log
    = Logger.getLogger(ExtensionManager.class.getName());
  
  private final InjectManager _injectManager;

  private HashSet<URL> _extensionSet = new HashSet<URL>();
  
  private HashMap<Class<?>,ExtensionItem> _extensionMap
    = new HashMap<Class<?>,ExtensionItem>();
  
  private boolean _isCustomExtension;

  ExtensionManager(InjectManager injectManager)
  {
    _injectManager = injectManager;
  }
  
  boolean isCustomExtension()
  {
    return _isCustomExtension;
  }

  void updateExtensions()
  {
    try {
      ClassLoader loader = _injectManager.getClassLoader();

      if (loader == null)
        return;

      Enumeration<URL> e = loader.getResources("META-INF/services/" + Extension.class.getName());

      while (e.hasMoreElements()) {
        URL url = (URL) e.nextElement();

        if (_extensionSet.contains(url))
          continue;

        _extensionSet.add(url);

        InputStream is = null;
        try {
          is = url.openStream();
          ReadStream in = Vfs.openRead(is);

          String line;

          while ((line = in.readLine()) != null) {
            int p = line.indexOf('#');
            if (p >= 0)
              line = line.substring(0, p);
            line = line.trim();

            if (line.length() > 0) {
              loadExtension(line);
            }
          }

          in.close();
        } catch (IOException e1) {
          log.log(Level.WARNING, e1.toString(), e1);
        } finally {
          IoUtil.close(is);
        }
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  void createExtension(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName(className, false, loader);
      Constructor<?> ctor = cl.getConstructor(new Class[] { InjectManager.class });

      Extension extension = (Extension) ctor.newInstance(_injectManager);

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  void loadExtension(String className)
  {
    _injectManager.getScanManager().setIsCustomExtension(true);
//    _isCustomExtension = true;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName(className, false, loader);

      if (! Extension.class.isAssignableFrom(cl))
        throw new InjectionException(L.l("'{0}' is not a valid extension because it does not implement {1}",
                                         cl, Extension.class.getName()));

      Extension extension = (Extension) cl.newInstance();

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  void addExtension(Extension ext)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " add extension " + ext);
    
    ExtensionItem item = introspect(ext.getClass());

    for (ExtensionMethod method : item.getExtensionMethods()) {
      ExtensionObserver observer;
      observer = new ExtensionObserver(ext,
                                       method.getMethod(),
                                       method.getArgs());

      _injectManager.addExtensionObserver(observer,
                                          method.getBaseType(),
                                          method.getQualifiers());
    }
  }

  private ExtensionItem introspect(Class<?> cl)
  {
    ExtensionItem item = _extensionMap.get(cl);

    if (item == null) {
      item = new ExtensionItem(cl);
      _extensionMap.put(cl, item);
    }

    return item;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _injectManager + "]";
  }

  class ExtensionItem {
    private ArrayList<ExtensionMethod> _observers
      = new ArrayList<ExtensionMethod>();

    ExtensionItem(Class<?> cl)
    {
      for (Method method : cl.getDeclaredMethods()) {
        ExtensionMethod extMethod = bindObserver(cl, method);

        if (extMethod != null)
          _observers.add(extMethod);
      }
    }

    private ArrayList<ExtensionMethod> getExtensionMethods()
    {
      return _observers;
    }

    private ExtensionMethod bindObserver(Class<?> cl, Method method)
    {
      Type []param = method.getGenericParameterTypes();

      if (param.length < 1)
        return null;

      Annotation [][]paramAnn = method.getParameterAnnotations();

      if (! hasObserver(paramAnn))
        return null;

      InjectManager inject = _injectManager;

      BeanArg<?> []args = new BeanArg[param.length];

      for (int i = 1; i < param.length; i++) {
        Annotation []bindings = inject.getQualifiers(paramAnn[i]);

        if (bindings.length == 0)
          bindings = new Annotation[] { CurrentLiteral.CURRENT };

        args[i] = new BeanArg(param[i], bindings);
      }

      BaseType baseType = inject.createBaseType(param[0]);

      return new ExtensionMethod(method, baseType,
                                 inject.getQualifiers(paramAnn[0]),
                                 args);
    }

    private boolean hasObserver(Annotation [][]paramAnn)
    {
      for (int i = 0; i < paramAnn.length; i++) {
        for (int j = 0; j < paramAnn[i].length; j++) {
          if (paramAnn[i][j].annotationType().equals(Observes.class))
            return true;
        }
      }

      return false;
    }
  }

  static class ExtensionMethod {
    private final Method _method;
    private final BaseType _type;
    private final Annotation []_qualifiers;
    private final BeanArg<?> []_args;

    ExtensionMethod(Method method,
                    BaseType type,
                    Annotation []qualifiers,
                    BeanArg<?> []args)
    {
      _method = method;
      _type = type;
      _qualifiers = qualifiers;
      _args = args;
    }

    public Method getMethod()
    {
      return _method;
    }

    public BeanArg<?> []getArgs()
    {
      return _args;
    }

    public BaseType getBaseType()
    {
      return _type;
    }

    public Annotation []getQualifiers()
    {
      return _qualifiers;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _method + "]";
    }
  }

  static class ExtensionObserver extends AbstractObserverMethod<Object> {
    private Extension _extension;
    private Method _method;
    private BeanArg<?> []_args;

    ExtensionObserver(Extension extension,
                      Method method,
                      BeanArg<?> []args)
    {
      _extension = extension;
      _method = method;
      _args = args;
    }

    public void notify(Object event)
    {
      try {
        Object []args = new Object[_args.length];
        args[0] = event;

        for (int i = 1; i < args.length; i++) {
          args[i] = _args[i].eval(null);
        }

        _method.invoke(_extension, args);
      } catch (RuntimeException e) {
        throw e;
      } catch (InvocationTargetException e) {
        String loc = (_extension + "." + _method.getName() + ": ");

        throw new InjectionException(loc + e.getMessage(), e.getCause());
      } catch (Exception e) {
        String loc = (_extension + "." + _method.getName() + ": ");

        throw new InjectionException(loc + e.getMessage(), e);
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _extension + "," + _method.getName() + "]";
    }
  }
}

