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

import com.caucho.config.*;
import com.caucho.config.annotation.StartupType;
import com.caucho.config.el.WebBeansContextResolver;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.BeanArg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.program.FieldEventProgram;
import com.caucho.config.scope.*;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.util.*;
import com.caucho.config.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.ref.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

import javax.decorator.Delegate;
import javax.el.*;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.*;
import javax.enterprise.inject.spi.*;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.naming.*;

/**
 * The web beans container for a given environment.
 */
public class ExtensionManager
{
  private static final L10N L = new L10N(ExtensionManager.class);
  private static final Logger log
    = Logger.getLogger(ExtensionManager.class.getName());

  private static final EnvironmentLocal<ExtensionManager> _localExtension
    = new EnvironmentLocal<ExtensionManager>();

  private HashMap<Class,ExtensionItem> _extensionMap
    = new HashMap<Class,ExtensionItem>();

  private ExtensionManager()
  {
  }

  public static void addExtension(InjectManager inject, Extension ext)
  {
    ExtensionManager extManager;

    synchronized (_localExtension) {
      ClassLoader loader = ext.getClass().getClassLoader();

      extManager = _localExtension.get(loader);

      if (extManager == null) {
        extManager = new ExtensionManager();
        _localExtension.set(extManager, loader);
      }
    }

    ExtensionItem item = extManager.introspect(ext.getClass());

    for (ExtensionMethod method : item.getExtensionMethods()) {
      ExtensionObserver observer;
      observer = new ExtensionObserver(ext,
                                       method.getMethod(),
                                       method.getArgs());

      inject.addExtensionObserver(observer,
                                  method.getBaseType(),
                                  method.getQualifiers());
    }
  }

  private synchronized ExtensionItem introspect(Class cl)
  {
    ExtensionItem item = _extensionMap.get(cl);

    if (item == null) {
      item = new ExtensionItem(cl);
      _extensionMap.put(cl, item);
    }

    return item;
  }

  static class ExtensionItem {
    private final Class _cl;

    private ArrayList<ExtensionMethod> _observers
      = new ArrayList<ExtensionMethod>();

    ExtensionItem(Class cl)
    {
      _cl = cl;

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

    private ExtensionMethod bindObserver(Class cl, Method method)
    {
      Type []param = method.getGenericParameterTypes();

      if (param.length < 1)
        return null;

      Annotation [][]paramAnn = method.getParameterAnnotations();

      if (! hasObserver(paramAnn))
        return null;

      InjectManager inject = InjectManager.create();

      BeanArg []args = new BeanArg[param.length];

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
    private final BeanArg []_args;

    ExtensionMethod(Method method,
                    BaseType type,
                    Annotation []qualifiers,
                    BeanArg []args)
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

    public BeanArg []getArgs()
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
  }

  static class ExtensionObserver extends AbstractObserverMethod<Object> {
    private Extension _extension;
    private Method _method;
    private BeanArg []_args;

    ExtensionObserver(Extension extension,
                      Method method,
                      BeanArg []args)
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

