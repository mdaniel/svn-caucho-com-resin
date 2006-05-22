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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.parser;

import java.util.ArrayList;
import java.util.HashMap;

import com.caucho.util.L10N;

import com.caucho.quercus.program.Function;

import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InterpretedClassDef;

/**
 * Parse scope.
 */
public class GlobalScope extends Scope {
  private final static L10N L = new L10N(GlobalScope.class);

  private HashMap<String,Function> _functionMap
    = new HashMap<String,Function>();

  private HashMap<String,InterpretedClassDef> _classMap
    = new HashMap<String,InterpretedClassDef>();

  /**
   * Adds a function.
   */
  public void addFunction(String name, Function function)
  {
    _functionMap.put(name.toLowerCase(), function);
  }

  /**
   * Adds a class
   */
  public InterpretedClassDef addClass(String name,
				      String parentName,
				      ArrayList<String> ifaceList)
  {
    InterpretedClassDef cl = _classMap.get(name);

    if (cl == null) {
      String []ifaceArray = new String[ifaceList.size()];
      ifaceList.toArray(ifaceArray);
      
      cl = new InterpretedClassDef(name, parentName, ifaceArray);
      _classMap.put(name, cl);
    }

    return cl;
  }

  /**
   * Returns the function map.
   */
  public HashMap<String,Function> getFunctionMap()
  {
    return _functionMap;
  }

  /**
   * Returns the class map.
   */
  public HashMap<String,InterpretedClassDef> getClassMap()
  {
    return _classMap;
  }
}

