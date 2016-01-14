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

package com.caucho.v5.config.cf;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.DirVar;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Program to assign parameters.
 */
abstract public class ProgramContainerBase extends ContainerProgram
{
  private static final L10N L = new L10N(ProgramContainerBase.class);
  
  private PathImpl _file;
  private String _location;
  
  public void setFile(PathImpl file)
  {
    _file = file;
  }
  
  public PathImpl getPath()
  {
    if (_file != null) {
      return _file;
    }
    
    String loc = getLocation();
    
    if (loc == null) {
      return null;
    }
    
    loc = loc.trim();
    if (loc.endsWith(":")) {
      loc = loc.substring(0, loc.length() - 1);
    }
    
    int p = loc.lastIndexOf(':');
    if (p < 0) {
      return null;
    }
    
    return VfsOld.lookup(loc.substring(0, p));
  }
  
  public String getLocation()
  {
    return _location;
  }
  
  public void setLocation(String location)
  {
    _location = location;
  }
  
  public void setLocation(String fileName, int line)
  {
    _location = fileName + ":" + line + ": ";
  }

  @Override
  public <T> void injectTop(T bean, InjectContext env)
  {
    if (_file != null) {
      PathImpl oldFile = (PathImpl) ConfigContext.getProperty("__PATH__");
      
      PathImpl path = getPath();

      if (path != null){
        ConfigContext.setProperty("__PATH__", path);
      }
      
      ConfigContext.setProperty("__DIR__", DirVar.__DIR__);
      
      try {
        inject(bean, env);
      } finally {
        ConfigContext.setProperty("__PATH__", oldFile);
      }
    }
    else {
      inject(bean, env);
    }
  }

  protected ConfigException error(String msg, Object ...args)
  {
    String location = getLocation();
    
    if (location != null) {
      return new ConfigExceptionLocation(location, L.l(msg, args));
    }
    else {
      return new ConfigException(L.l(msg, args));
    }
  }
  
  protected RuntimeException error(RuntimeException exn)
  {
    if (exn instanceof ConfigExceptionLocation) {
      return (ConfigExceptionLocation) exn;
    }
    
    String location = getLocation();
    
    if (location != null) {
      return ConfigExceptionLocation.wrap(location, exn);
    }
    else {
      return exn;
    }
  }
  
  protected RuntimeException error(Exception exn)
  {
    if (exn instanceof ConfigExceptionLocation) {
      return (ConfigExceptionLocation) exn;
    }
    
    String location = getLocation();
    
    if (location != null) {
      return ConfigExceptionLocation.wrap(location, exn);
    }
    else {
      return ConfigException.wrap(exn);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

