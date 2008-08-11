/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.Location;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Parse scope.
 */
public class IfScope extends Scope {
  private final static L10N L = new L10N(IfScope.class);

  private ExprFactory _exprFactory;
  private Scope _parentScope;

  private HashMap<String,Function> _functionMap
    = new HashMap<String,Function>();
  
  private HashMap<String,Function> _conditionalFunctionMap;

  private HashMap<String,InterpretedClassDef> _classMap
    = new HashMap<String,InterpretedClassDef>();
  
  private HashMap<String,InterpretedClassDef> _conditionalClassMap;

  IfScope(ExprFactory exprFactory, Scope scope)
  {
    _exprFactory = exprFactory;
    
    _parentScope = scope;
  }

  /*
   * Returns true if scope is local to a function.
   */
  public boolean isIf()
  {
    return false;
  }
  
  /**
   * Adds a function.
   */
  public void addFunction(String name, Function function)
  {
    _functionMap.put(name.toLowerCase(), function);
    
    _parentScope.addConditionalFunction(function);
  }
  
  /*
   *  Adds a function defined in a conditional block.
   */
  protected void addConditionalFunction(Function function)
  {
    if (_conditionalFunctionMap == null)
      _conditionalFunctionMap = new HashMap<String,Function>(4);

    _conditionalFunctionMap.put(function.getCompilationName(), function);
    
    _parentScope.addConditionalFunction(function);
  }

  /**
   * Adds a class
   */
  public InterpretedClassDef addClass(Location location,
                                      String name,
                                      String parentName,
                                      ArrayList<String> ifaceList,
                                      int index)
  {
    InterpretedClassDef cl = _classMap.get(name);

    if (cl == null) {
      String []ifaceArray = new String[ifaceList.size()];
      ifaceList.toArray(ifaceArray);

      cl = _exprFactory.createClassDef(location,
                                       name, parentName, ifaceArray,
                                       index);
      
      _classMap.put(name, cl);
      
      _parentScope.addConditionalClass(cl);
    }
    else {
      // class statically redeclared
      // XXX: should throw a runtime error?
      
      // dummy classdef for parsing only
      cl = _exprFactory.createClassDef(location,
                                       name, parentName, new String[0],
                                       index);
    }

    return cl;
  }
  
  /*
   *  Adds a conditional class.
   */
  protected void addConditionalClass(InterpretedClassDef def)
  {
    if (_conditionalClassMap == null)
      _conditionalClassMap = new HashMap<String,InterpretedClassDef>(1);

    _conditionalClassMap.put(def.getCompilationName(), def);
    
    _parentScope.addConditionalClass(def);
  }
}

