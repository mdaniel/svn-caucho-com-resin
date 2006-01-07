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

package com.caucho.quercus.expr;

import com.caucho.quercus.program.FunctionInfo;

/**
 * Information about a variable's use in a function.
 */
public class VarInfo {
  private final FunctionInfo _function;

  private final String _name;
  
  private boolean _isGlobal;
  private boolean _isArgument;
  private boolean _isRefArgument;
  private boolean _isAssigned;
  
  // true if the variable is a reference
  private boolean _isReference;

  // XXX: move to arg?
  private int _argumentIndex;

  public VarInfo(String name, FunctionInfo function)
  {
    _name = name;
    
    _function = function;

    if (function != null && function.isPageMain())
      _isReference = true;
  }

  /**
   * Returns the variable name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the owning function.
   */
  public FunctionInfo getFunction()
  {
    return _function;
  }

  /**
   * True if the variable is global.
   */
  public boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * True if the variable is global.
   */
  public void setGlobal(boolean isGlobal)
  {
    _isGlobal = isGlobal;
  }

  /**
   * True if the variable is variable
   */
  public boolean isVariable()
  {
    // php/3251
    return _function != null && (_function.isUsesSymbolTable() ||
				 _function.isVariableVar());
  }

  /**
   * True if the variable is a function argument
   */
  public boolean isArgument()
  {
    return _isArgument;
  }

  /**
   * True if the variable is a function argument
   */
  public void setArgument(boolean isArgument)
  {
    _isArgument = isArgument;
  }

  /**
   * Sets the argument index
   */
  public void setArgumentIndex(int index)
  {
    _argumentIndex = index;
  }

  /**
   * Returns the argument indext
   */
  public int getArgumentIndex()
  {
    return _argumentIndex;
  }

  /**
   * True if the variable is a reference function argument
   */
  public boolean isRefArgument()
  {
    return _isRefArgument;
  }

  /**
   * True if the variable is a reference function argument
   */
  public void setRefArgument(boolean isRef)
  {
    _isRefArgument = isRef;
    
    setReference();
  }

  /**
   * True if the variable is assigned in the function.
   */
  public boolean isAssigned()
  {
    return _isAssigned;
  }

  /**
   * True if the variable is assigned in the function.
   */
  public void setAssigned(boolean isAssigned)
  {
    _isAssigned = isAssigned;

    if (isReference()) {
      // php/3a7c
      setModified();
    }
  }

  /**
   * True if the variable is used as a reference in the function.
   */
  public boolean isReference()
  {
    return (_isReference || isGlobal() || isArgument() || isVariable());
  }

  /**
   * True if the variable is assigned in the function.
   */
  public void setReference()
  {
    _isReference = true;

    if (_isAssigned)
      setModified();
  }

  /**
   * Returns true for a read-only variable, i.e. one that doesn't
   * need to be copied as an argument.
   */
  public boolean isReadOnly()
  {
    return _function.isReadOnly();
  }

  /**
   * Sets as modified.
   */
  public void setModified()
  {
    _function.setModified();
  }

  public String toString()
  {
    return "VarInfo[" + _name + "]";
  }
}

