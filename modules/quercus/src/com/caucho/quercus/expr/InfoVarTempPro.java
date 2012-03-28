/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.program.FunctionInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Information about a variable's use in a function.
 */
public class InfoVarTempPro extends InfoVarPro {
  public InfoVarTempPro(StringValue name)
  {
    super(name, null);
  }
  
  /**
   * Variables must be stored as Var if they are used as references or
   * grabbed from the symbol table.
   * 
   * $b = &$a;
   * $b = 3;
   * 
   * In this case, $a and $b must be a Var, never a Value since
   * modifying $b will modify $a.
   */
  public boolean isVar()
  {
    return false;
  }

  /**
   * Value variables stored as Java locals.  The variables must never be
   * a Var.
   */
  public boolean isValue()
  {
    return true;
  }
 
  /**
   * Var variables stored as Java variables.
   */
  public boolean isLocalVar()
  {
    return true;
  }
 
  /**
   * Variables stored in the Env symbol table
   */
  public boolean isEnvVar()
  {
    return false;
  }

  /**
   * Var variables stored as Java variables.
   */
  public boolean isSymbolVar()
  {
    return false;
  }
}

