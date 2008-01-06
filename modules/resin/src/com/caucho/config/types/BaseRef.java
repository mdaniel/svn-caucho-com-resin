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
 * @author Rodrigo Westrupp
 */

package com.caucho.config.types;

import com.caucho.bytecode.*;
import com.caucho.ejb.*;
import com.caucho.util.BeanUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.lang.reflect.*;
import java.util.logging.Logger;

/**
 * Base configuration for ejb-ref and resource-env-ref.
 */
abstract public class BaseRef {
  private static final L10N L = new L10N(BaseRef.class);
  private static final Logger log
    = Logger.getLogger(BaseRef.class.getName());

  protected final Path _modulePath;

  protected String _sourceEjbName;

  protected InjectionTarget _injectionTarget;


  public BaseRef()
  {
    _modulePath = Vfs.getPwd();
  }

  public BaseRef(Path modulePath)
  {
    _modulePath = modulePath;
  }

  public BaseRef(Path modulePath, String sourceEjbName)
  {
    _modulePath = modulePath;
    _sourceEjbName = sourceEjbName;
  }

  public InjectionTarget getInjectionTarget()
  {
    return _injectionTarget;
  }

  public void setInjectionTarget(InjectionTarget injectionTarget)
  {
    _injectionTarget = injectionTarget;
  }

  public Class getJavaClass(String className)
    throws Exception
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    JClassLoader jClassLoader = JClassLoaderWrapper.create(loader);

    JClass jClass = jClassLoader.forName(className);

    return jClass.getJavaClass();
  }

  public AccessibleObject getFieldOrMethod(Class cl, String fieldName)
    throws Exception
  {
    EjbServerManager manager = EjbServerManager.getLocal();

    if (manager == null)
      return null;

    Method method = BeanUtil.getSetMethod(cl, fieldName);

    if (method != null)
      return method;

    return cl.getDeclaredField(fieldName);
  }
}
