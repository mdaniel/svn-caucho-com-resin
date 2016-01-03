/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import com.caucho.v5.bytecode.scan.ScanClass;
import com.caucho.v5.bytecode.scan.ScanListenerByteCode;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.vfs.PathImpl;

/**
 * scans a class hierarchy.
 */
class ScanListenerClassHierarchy implements ScanListenerByteCode
{
  private final static Logger log 
    = Logger.getLogger(ScanListenerClassHierarchy.class.getName());
  
  private final ClassLoader _loader;
  private final ArrayList<Entry> _classList = new ArrayList<Entry>();
  
  private final HashSet<PathImpl> _rootPaths = new HashSet<>();
  
  ScanListenerClassHierarchy(ClassLoader loader)
  {
    _loader = loader;
  }

  public void addRoot(PathImpl rootPath)
  {
    _rootPaths.add(rootPath);
  }

  @Override
  public boolean isRootScannable(PathImpl root, String packageRoot)
  {
    // return _rootPaths.contains(root);
    return true;
  }

  @Override
  public ScanClass scanClass(PathImpl root, 
                             String packageRoot, 
                             String name,
                             int modifiers)
  {
    ScanClassClassHierarchy scanClass
      = new ScanClassClassHierarchy(this, name);

    return scanClass;
  }
  
  void addEntry(String name, String superName)
  {
    if (superName == null || "java.lang.Object".equals(superName)) {
      return;
    }
    
    _classList.add(new Entry(name, superName));
  }

  public HashSet<Class<?>> findClasses(Class<?>[] value)
  {
    ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
    
    for (Class<?> cl : value) {
      classList.add(cl);
    }
    
    int oldSize;
    
    do {
      oldSize = classList.size();
      
      for (int i = oldSize - 1; i >= 0; i--) {
        findClasses(classList, classList.get(i).getName());
      }
    } while (classList.size() != oldSize);
    
    if (classList.size() == value.length) {
      return null;
    }
    
    LinkedHashSet<Class<?>> classSet = new LinkedHashSet<Class<?>>();

    for (int i = 0; i < value.length; i++) {
      classList.remove(0);
    }
    
    Collections.sort(classList, new ClassComparator());
    
    for (int i = 0; i < classList.size(); i++) {
      classSet.add(classList.get(i));
    }
    
    return classSet;
  }
  
  private void findClasses(ArrayList<Class<?>> classList, String superName)
  {
    ArrayList<Entry> classEntryList = _classList;

    int size = classEntryList.size();
    
    for (int i = 0; i < size; i++) {
      Entry entry = classEntryList.get(i);
      
      if (superName.equals(entry.getSuperName())) {
        try {
          Class<?> cl = Class.forName(entry.getName(), false, _loader);
          
          if (! classList.contains(cl)) {
            classList.add(cl);
          }
        } catch (Exception e) {
          log.finer("ClassHierarchyScan: " + e.toString());
        }
      }
    }
  }

  @Override
  public int getScanPriority()
  {
    return 2;
  }
  
  static class Entry {
    private final String _name;
    private final String _superName;
    
    Entry(String name, String superName)
    {
      _name = name;
      _superName = superName;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public String getSuperName()
    {
      return _superName;
    }
  }
  
  static class ClassComparator implements Comparator<Class<?>> {
    @Override
    public int compare(Class<?> a, Class<?> b)
    {
      return a.getName().compareTo(b.getName());
    }
  }
}
