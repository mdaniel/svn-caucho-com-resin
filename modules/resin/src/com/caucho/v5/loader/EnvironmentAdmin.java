/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.loader;

import java.io.File;
import java.lang.ref.WeakReference;

import com.caucho.v5.jmx.server.EnvironmentMXBean;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.util.CurrentTime;

public class EnvironmentAdmin extends ManagedObjectBase
  implements EnvironmentMXBean
{
  private final WeakReference<EnvironmentClassLoader> _loaderRef;

  public EnvironmentAdmin(EnvironmentClassLoader loader)
  {
    _loaderRef = new WeakReference(loader);
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public String []getClassPath()
  {
    EnvironmentClassLoader loader = _loaderRef.get();

    if (loader != null) {
      String classPath = loader.getClassPath();

      return classPath.split("[" + File.pathSeparatorChar + "]");
    }
    else
      return null;
  }

  void register()
  {
    //if (! CurrentTime.isTest()) {
    try {
      registerSelf();
    } catch (Exception e) {
      e.printStackTrace();
    }
    //}
  }

  void unregister()
  {
    //if (! CurrentTime.isTest()) { 
      unregisterSelf();
    //}
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
