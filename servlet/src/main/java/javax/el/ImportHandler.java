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
 * @author Paul Cowan
 */

package javax.el;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.*;

public class ImportHandler 
{
  private final static Logger log
    = Logger.getLogger(ImportHandler.class.getName());

  private final Map<String, String> importedClassesMap = new HashMap<>();
  private final List<String> importedPackages = new ArrayList<>();
  private final Map<String, String> importedStaticMembersMap = new HashMap<>();
  private final Map<String, Class<?>> resolvedClassesMap = new HashMap<>();
  
  public ImportHandler()
  {
    importPackage("java.lang");
  }
  
  public void importClass(String name) throws ELException 
  {
    int index = name.lastIndexOf('.');
    if (index < 0) {
      throw new ELException("name must be fully qualified");
    }
    
    importedClassesMap.put(name.substring(index+1), name);
  }

  public void importPackage(String packageName)
  {
    importedPackages.add(packageName);
  }

  public void importStatic(String name) throws ELException 
  {
    int index = name.lastIndexOf('.');
    if (index < 0) {
      throw new ELException("name must be fully qualified");
    }
    
    importedStaticMembersMap.put(name.substring(index+1), 
                                 name.substring(0, index));
  }

  public Class<?> resolveClass(String name) 
  {
    Class<?> cl = resolvedClassesMap.get(name);
    if (cl != null) {
      return cl;
    }
    
    String className = importedClassesMap.get(name);
    if (className != null) {
      cl = checkClass(name, className);
      if (cl != null) {
        return cl;
      }
    }
    
    for (String packageName : importedPackages) {
      className = String.format("%s.%s", packageName, name);
      cl = checkClass(name, className);
      
      if (cl != null) {
        return cl;
      }
    }
    
    return null;
  }
  
  private Class<?> checkClass(String key, String className)
  {
    if (className == null) {
      return null;
    }
    
    Class<?> cl = null;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    try {
      // cl = loader.loadClass(className);
      cl = Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      /*
      if (log.isLoggable(Level.FINEST)) {
        log.finest(e.toString());
      }
      
      if (log.isLoggable(Level.ALL)) {
        log.log(Level.ALL, e.toString(), e);
      }
      */
      return null;
    }
    
    int modifiers = cl.getModifiers();
    if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers) 
        || ! Modifier.isPublic(modifiers)) {
      throw new ELException(String.format("%s is not instanceable", cl));
    }
    
    resolvedClassesMap.put(key, cl);
    return cl;
  }

  public Class<?> resolveStatic(String name) 
  {
    String className = importedStaticMembersMap.get(name);
    if (className == null)
      return null;
    
    return checkClass(name, className);
  }
}
