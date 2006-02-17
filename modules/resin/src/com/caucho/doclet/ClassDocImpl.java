/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.doclet;

import java.util.*;
import java.util.logging.*;

import com.caucho.log.Log;

/**
 * Represents a class.
 */
public class ClassDocImpl extends DocImpl {
  private static final Logger log = Log.open(ClassDocImpl.class);

  private RootDocImpl _root;

  private ClassDocImpl _superclass;
  
  private ArrayList<ClassDocImpl> _interfaces =
  new ArrayList<ClassDocImpl>();
  
  private ArrayList<MethodDocImpl> _methods =
  new ArrayList<MethodDocImpl>();

  public ClassDocImpl(RootDocImpl root)
  {
    _root = root;
  }

  /**
   * Sets the superclass.
   */
  public void setSuperclass(String className)
  {
    _superclass = _root.getClass(className);
  }

  /**
   * Adds the description of a class interfaces.
   */
  public void addInterface(String className)
  {
    _interfaces.add(_root.getClass(className));
  }

  public void addMethod(MethodDocImpl method)
  {
    _methods.add(method);
  }

  public ArrayList<MethodDocImpl> getMethods()
  {
    return _methods;
  }

  /**
   * Returns true if the class can be assigned to the given class.
   */
  public boolean isAssignableTo(String className)
  {
    if (className.equals(getName()))
      return true;

    for (int i = _interfaces.size() - 1; i >= 0; i--)
      if (_interfaces.get(i).isAssignableTo(className))
        return true;
    
    if (_superclass != null && _superclass.isAssignableTo(className))
      return true;

    return false;
  }

  public String toString()
  {
    return "ClassDocImpl[" + getName() + "]";
  }
}
