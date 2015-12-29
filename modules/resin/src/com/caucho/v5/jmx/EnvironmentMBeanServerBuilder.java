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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

import com.caucho.v5.loader.EnvLoader;

/**
 * implementation for an MBeanServer factory.
 */
public class EnvironmentMBeanServerBuilder extends MBeanServerBuilder {
  private static boolean _isInit;
  private static boolean _isJdkManagementInit;
  
  private static EnvironmentMBeanServer _globalServer;
  
  public EnvironmentMBeanServerBuilder()
  {
  }
  
  /**
   * Creates the delegate.
   */
  @Override
  public MBeanServerDelegate newMBeanServerDelegate()
  {
    return new MBeanServerDelegateImpl("Baratine");
  }
  
  /**
   * Creates the mbean server
   */
  @Override
  public MBeanServer newMBeanServer(String defaultDomain,
                                    MBeanServer outer,
                                    MBeanServerDelegate delegate)
  {
    if (! _isJdkManagementInit) {
      Exception e = new Exception();
      e.fillInStackTrace();
      StackTraceElement []stackTrace = e.getStackTrace();

      for (int i = 0; i < stackTrace.length; i++) {
        if (stackTrace[i].getClassName().equals("java.lang.management.ManagementFactory")) {
          _isJdkManagementInit = true;

          return createGlobal(defaultDomain, outer, delegate);
        }
      }
    }

    if (! _isInit) {
      _isInit = true;
      
      if (_globalServer == null) {
        _globalServer = createGlobal(defaultDomain, outer, delegate);
      }
    }

    return super.newMBeanServer(defaultDomain, outer, delegate);
  }
  
  public static EnvironmentMBeanServer getGlobal()
  {
    if (_globalServer == null) {
      EnvLoader.init();
      
      EnvironmentMBeanServerBuilder builder = new EnvironmentMBeanServerBuilder();
      builder.newMBeanServer(JmxUtilResin.DOMAIN, null, builder.newMBeanServerDelegate());
    }
    
    return _globalServer;
  }
  
  public static EnvironmentMBeanServer getInitGlobal()
  {
    if (_globalServer == null) {
      EnvironmentMBeanServerBuilder builder = new EnvironmentMBeanServerBuilder();
      builder.newMBeanServer(JmxUtilResin.DOMAIN, null, builder.newMBeanServerDelegate());
    }
    
    return _globalServer;
  }
  
  private EnvironmentMBeanServer createGlobal(String defaultDomain,
                                              MBeanServer outer,
                                              MBeanServerDelegate delegate)
  {
    if (_globalServer != null) {
      return _globalServer;
    }
    
    if (defaultDomain == null) {
      //defaultDomain = Jmx.DOMAIN;
    }
    
    if (outer == null) {
      outer = super.newMBeanServer(defaultDomain, outer, delegate);
    }
    
    _globalServer = new EnvironmentMBeanServer(defaultDomain, outer, delegate);
    
    return _globalServer;
  }
}
