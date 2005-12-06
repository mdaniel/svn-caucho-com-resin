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

package com.caucho.quercus.program;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.expr.VarInfo;

/**
 * Information about a function.
 */
public class FunctionInfo {
  private final Quercus _quercus;

  private final String _name;
  
  private final HashMap<String,VarInfo> _varMap
    = new HashMap<String,VarInfo>();

  private final ArrayList<String> _tempVarList
    = new ArrayList<String>();

  private AbstractClassDef _classDef;

  private boolean _isGlobal;
  private boolean _isReturnsReference;
  private boolean _isVariableVar;
  private boolean _isOutUsed;

  private boolean _isVariableArgs;
  private boolean _isUsesSymbolTable;

  public FunctionInfo(Quercus quercus, String name)
  {
    _quercus = quercus;
    _name = name;
  }

  /**
   * Returns the owning quercus.
   */
  public Quercus getPhp()
  {
    return _quercus;
  }

  /**
   * True for a global function (top-level script).
   */
  public boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * True for a global function.
   */
  public void setGlobal(boolean isGlobal)
  {
    _isGlobal = isGlobal;
  }

  /**
   * Sets the owning class.
   */
  public void setDeclaringClass(AbstractClassDef classDef)
  {
    _classDef = classDef;
  }

  /**
   * Gets the owning class.
   */
  public AbstractClassDef getDeclaringClass()
  {
    return _classDef;
  }

  /**
   * True for a method.
   */
  public boolean isMethod()
  {
    return _classDef != null;
  }

  /**
   * True if the function returns a reference.
   */
  public boolean isReturnsReference()
  {
    return _isReturnsReference;
  }

  /**
   * True if the function returns a reference.
   */
  public void setReturnsReference(boolean isReturnsReference)
  {
    _isReturnsReference = isReturnsReference;
  }

  /**
   * True if the function has variable vars.
   */
  public boolean isVariableVar()
  {
    return _isVariableVar;
  }

  /**
   * True if the function has variable vars
   */
  public void setVariableVar(boolean isVariableVar)
  {
    _isVariableVar = isVariableVar;
  }

  /**
   * True if the function has variable numbers of arguments
   */
  public boolean isVariableArgs()
  {
    return _isVariableArgs;
  }

  /**
   * True if the function has variable numbers of arguments
   */
  public void setVariableArgs(boolean isVariableArgs)
  {
    _isVariableArgs = isVariableArgs;
  }

  /**
   * True if the function uses the symbol table
   */
  public boolean isUsesSymbolTable()
  {
    return _isUsesSymbolTable;
  }

  /**
   * True if the function uses the symbol table
   */
  public void setUsesSymbolTable(boolean isUsesSymbolTable)
  {
    _isUsesSymbolTable = isUsesSymbolTable;
  }

  /**
   * Returns true if the out is used.
   */
  public boolean isOutUsed()
  {
    return _isOutUsed;
  }

  /**
   * Set true if the out is used.
   */
  public void setOutUsed()
  {
    _isOutUsed = true;
  }

  /**
   * Returns the variable.
   */
  public VarInfo createVar(String name)
  {
    VarInfo var = _varMap.get(name);

    if (var == null) {
      var = new VarInfo(name, this);
      var.setGlobal(_isGlobal);

      if (Quercus.isSuperGlobal(name))
	var.setGlobal(true);
      
      _varMap.put(name, var);
    }

    return var;
  }

  /**
   * Returns the variables.
   */
  public Collection<VarInfo> getVariables()
  {
    return _varMap.values();
  }

  /**
   * Adds a temp variable.
   */
  public void addTempVar(String name)
  {
    if (! _tempVarList.contains(name))
      _tempVarList.add(name);
  }

  /**
   * Returns the temp variables.
   */
  public Collection<String> getTempVariables()
  {
    return _tempVarList;
  }

  public String toString()
  {
    return "FunctionInfo[" + _name + "]";
  }
}

