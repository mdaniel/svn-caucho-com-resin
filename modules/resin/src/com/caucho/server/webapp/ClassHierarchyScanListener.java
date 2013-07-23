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

package com.caucho.server.webapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.enhancer.ScanClass;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Path;

/**
 * scans a class hierarchy.
 */
class ClassHierarchyScanListener implements ScanListener {
  private final static Logger log 
    = Logger.getLogger(ClassHierarchyScanListener.class.getName());
  
  private final ClassLoader _loader;
  private final ArrayList<Entry> _classList = new ArrayList<Entry>();
  
  ClassHierarchyScanListener(ClassLoader loader)
  {
    _loader = loader;
  }

  @Override
  public ScanClass scanClass(Path root, 
                             String packageRoot, 
                             String name,
                             int modifiers)
  {
    ClassHierarchyScanClass scanClass
      = new ClassHierarchyScanClass(this, name);

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
  public boolean isScanMatchAnnotation(CharBuffer string)
  {
    return false;
  }

  @Override
  public void classMatchEvent(EnvironmentClassLoader loader, 
                              Path root,
                              String className)
  {
  }
  
  @Override
  public int getScanPriority()
  {
    return 2;
  }

  @Override
  public boolean isRootScannable(Path root, String packageRoot)
  {
    return true;
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
