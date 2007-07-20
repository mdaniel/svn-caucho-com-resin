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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.cfg;

import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.loader.enhancer.EnhancerManager;
import com.caucho.util.L10N;
import com.caucho.vfs.PersistentDependency;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Configuration for an interceptor.
 */
public class Interceptor {
  private static final L10N L = new L10N(Interceptor.class);

  private String _interceptorClass;
  private String _aroundInvokeMethodName;

  private ClassLoader _loader;
  protected JClassLoader _jClassLoader;

  private JClass _interceptorJClass;

  ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();

  public Interceptor()
  {
    _loader = Thread.currentThread().getContextClassLoader();

    _jClassLoader = JClassLoaderWrapper.create(_loader);
  }

  public String getInterceptorClass()
  {
    return _interceptorClass;
  }

  public void setInterceptorClass(String interceptorClass)
  {
    _interceptorClass = interceptorClass;
  }

  public void init()
  {
    // XXX: EnhancerManager getJavaClassLoader()
    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
    JClassLoader jClassLoader = EnhancerManager.create(parentLoader).getJavaClassLoader();

    _interceptorJClass = jClassLoader.forName(_interceptorClass);

    for (JMethod method : _interceptorJClass.getMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class)) {
        _aroundInvokeMethodName = method.getName();

        // XXX: check invalid duplicated @AroundInvoke methods.
        break;
      }
    }
  }

  public static void makeAccessible(final Method method)
  {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run()
          {
            method.setAccessible(true);
            return null;
          }
        });
    } catch (PrivilegedActionException e) {
      throw new RuntimeException(e.getException());
    }
  }

  public String getAroundInvokeMethodName()
  {
    return _aroundInvokeMethodName;
  }
}
