/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.loader;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.lang.ref.WeakReference;

import java.lang.reflect.Method;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import com.caucho.vfs.EnvironmentStream;
import com.caucho.vfs.SchemeMap;

import com.caucho.naming.Jndi;

import com.caucho.transaction.TransactionManagerImpl;

import com.caucho.jca.UserTransactionProxy;

import com.caucho.util.ThreadPool;

import com.caucho.jmx.Jmx;

import com.caucho.security.PolicyImpl;

/**
 * ClassLoader that initalizes the Resin environment, and allows byte code
 * enhancement of classes in the system classpath.
 * <pre>
 * java -Djava.system.class.loader=com.caucho.loader.SystemClassLoader ...
 * </pre>
 */
public class SystemClassLoader extends EnvironmentClassLoader {
  /**
   * Creates a new SystemClassLoader.
   */
  public SystemClassLoader(ClassLoader parent)
  {
    super(parent);
  }
}
