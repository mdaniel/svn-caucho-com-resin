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

package com.caucho.ejb.manager;

import com.caucho.inject.Module;
import com.caucho.loader.EnvironmentLocal;

/**
 * Environment-based container.
 */
@Module
public class EjbModule {
  private static final EnvironmentLocal<EjbModule> _localModule
    = new EnvironmentLocal<EjbModule>();
  
  private final String _moduleName;

  private EjbModule(String name)
  {
    _moduleName = name;
  }

  public static EjbModule getCurrent()
  {
    return _localModule.get();
  }
  
  public static EjbModule create(String name)
  {
    EjbModule module = _localModule.getLevel();
    
    if (module == null) {
      module = new EjbModule(name);
      _localModule.set(module);
    }
    
    return module;
  }
  
  public static EjbModule replace(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return replace(name, loader);
  }
  
  public static EjbModule replace(String name, ClassLoader loader)
  {
    EjbModule module = new EjbModule(name);
    _localModule.set(module, loader);
    
    return module;
  }
  
  public String getModuleName()
  {
    return _moduleName;
  }
}
