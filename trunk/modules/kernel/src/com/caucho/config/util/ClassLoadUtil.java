/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
package com.caucho.config.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.IllegalLoopbackException;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.util.L10N;

/**
 * Utilities to manage locks.
 */
public class ClassLoadUtil {
  public static Class<?> load(String className, ClassLoader loader) 
    throws ClassNotFoundException
  {
    if (loader instanceof DynamicClassLoader) {
      DynamicClassLoader dynLoad = (DynamicClassLoader) loader;
      
      dynLoad.updateScan();
    }
    
    return 
        Class.forName(className, false, loader);
  }
}
