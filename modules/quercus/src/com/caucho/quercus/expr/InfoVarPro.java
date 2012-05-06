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
public class InfoVarPro extends VarInfo {
  // XXX: move to arg?
  private int _argumentIndex;

  private boolean _isSuperGlobal;

  // a function argument, foo($a)
  private boolean _isArgument;
  // a function ref argument, foo(&$a)
  private boolean _isRefArgument;

  // the expected argument class
  private String _expectedClass;
  // true if has a default value
  private boolean _isDefaultArg;

  // true if the variable must be a Var, not a Value
  private boolean _isVar;

  // assigned in the function, $a = 3; or foo($a);
  private boolean _isAssigned;

  // true if the var is initialized explicitly before any implicit use
  private boolean _isInitializedVar;

  // true if the vars contents may be modified as an array
  private boolean _isArrayModified;

  private String _symbolName;

  // the assigned types
  private ExprType _type = ExprType.INIT;
  private boolean _isPostIncrement;

  public InfoVarPro(StringValue name, FunctionInfo function)
  {
    super(name, function);

    _isSuperGlobal = QuercusContext.isSuperGlobal(name);

    if (_isSuperGlobal)
      _isVar = true;

    if (function != null && function.isPageMain()) {
      _isVar = true;
      _type = ExprType.VALUE;
    }
  }

  public InfoVarPro(VarInfo var)
  {
    this(var.getName(), var.getFunction());
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
    _type = ExprType.VALUE;
  }

  /**
   * Sets the argument index
   */
  public void setArgumentIndex(int index)
  {
    _type = ExprType.VALUE;
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
   * Sets the symbol index
   */
  public void setSymbolName(String symbolName)
  {
    _symbolName = symbolName;
    _isVar = true;
  }

  /**
   * Gets the symbol index
   */
  public String getSymbolName()
  {
    return _symbolName;
  }

  /**
   * The type hinted expected class
   */
  public String getExpectedClass()
  {
    return _expectedClass;
  }

  /**
   * The type hinted expected class
   */
  public void setExpectedClass(String expectedClass)
  {
    _expectedClass = expectedClass;
  }

  /**
   * True if the arg has a default value
   */
  public boolean isDefaultArg()
  {
    return _isDefaultArg;
  }

  /**
   * True if the arg has a default value.
   */
  public void setDefaultArg(boolean isDefaultArg)
  {
    _isDefaultArg = isDefaultArg;
  }

  /**
   * Returns true for a read-only variable, i.e. one that doesn't
   * need to be copied as an argument.
   */
  public boolean isReadOnly()
  {
    // php/3a43

    return ! isAssigned();

    // return _function.isReadOnly();
  }

  /**
   * True if the variable is assigned in the function.
   */
  public boolean isAssigned()
  {
    return _isAssigned;
  }

  /**
   * True if the variable is assigned in the function, e.g.
   *
   * $a = 3;
   * foo($a);  // assuming foo(&$x)
   */
  public void setAssigned()
  {
    _isAssigned = true;

    if (_isArgument)
      _isVar = true;
  }

  /**
   * True if the variable is initialized directly
   *
   * function foo()
   * {
   *   global $a;
   * }
   */
  public boolean isInitializedVar()
  {
    return _isInitializedVar;
  }

  /**
   * True if the variable is initialized directly
   *
   * function foo()
   * {
   *   global $a;
   * }
   */
  public void setInitializedVar(boolean isInit)
  {
    _isInitializedVar = isInit;
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
  public void setRefArgument()
  {
    _isRefArgument = true;
    _type = ExprType.VALUE;
    _isVar = true;
  }

  /**
   * Superglobals always load from $_GLOBAL
   */
  public boolean isSuperGlobal()
  {
    return _isSuperGlobal;
  }

  /**
   * Sets as modified, e.g. $a[0] = 3; // $a could be null
   */
  public void setVar()
  {
    _type = ExprType.VALUE;
    _isVar = true;
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
    // php/3a70 vs php/3995
    // php/3a45
    return (_isVar || isEnvVar());
  }

  /**
   * Value variables stored as Java locals.  The variables must never be
   * a Var.
   */
  public boolean isValue()
  {
    // php/3a71
    // php/3435
    return ! isVar();
  }

  /**
   * Var variables stored as Java variables.
   */
  public boolean isLocalVar()
  {
    return isVar() && ! isEnvVar();
  }

  /**
   * True if the variable is used from the symbol table, e.g. $$v or
   * an include or main
   */
  public boolean isEnvVar()
  {
    FunctionInfo fun = getFunction();

    // php/3251
    if (fun == null)
      return true;
    else if (fun.isUsesSymbolTable() || fun.isVariableVar())
      return true;
    else
      return false;
  }

  /**
   * Var variables stored as Java variables.
   */
  public boolean isSymbolVar()
  {
    return _symbolName != null;
  }

  public boolean isJavaLong()
  {
    return isLocalVar() && _type == ExprType.LONG;
  }

  public boolean isArrayModified()
  {
    return _isArrayModified;
  }

  public void setArrayModified(boolean isArrayModified)
  {
    _isArrayModified = isArrayModified;
  }

  /**
   * Returns the analyzed type
   */
  public ExprType getType()
  {
    return _type;
  }

  /**
   * Updates the analyzed type
   */
  public ExprType withType(ExprType type)
  {
    _type = _type.withType(type);

    return _type;
  }

  /**
   * Generates the function initialization code
   */
  public void printInitType(PhpWriter out, boolean isVariableArgs)
    throws IOException
  {
    if (isValue()) {
      if (_type == ExprType.LONG)
        out.print("long ");
      else if (_type == ExprType.DOUBLE)
        out.print("double ");
      else if (_type == ExprType.BOOLEAN)
        out.print("boolean ");
      else if (_type == ExprType.STRING)
        out.print("StringValue ");
      else
        out.print("Value ");
    }
    else if (isLocalVar()) {
      out.print("Var ");
    }
    else if (isSymbolVar()) {
    }
    else if (isEnvVar()) {
      out.print("EnvVar ");
    }
    else
      throw new IllegalStateException();
  }

  /**
   * Generates the function initialization code
   */
  public void generateInit(PhpWriter out,
                           String varName,
                           String argName)
    throws IOException
  {
    if (isValue()) {
      if (isArgument()) {
        if (isArrayModified()) {
          out.println(varName + " = " + argName + ".toLocalValue();");
        }
        else {
          out.println(varName + " = " + argName + ".toLocalValueReadOnly();");
        }
      }
      else if (_type == ExprType.LONG)
        out.println(varName + " = 0;");
      else if (_type == ExprType.DOUBLE)
        out.println(varName + " = 0.0;");
      else if (_type == ExprType.BOOLEAN)
        out.println(varName + " = false;");
      else if (_type == ExprType.STRING) {
        out.println(varName + " = StringValue.EMPTY;");
      }
      else
        out.println(varName + " = NullValue.NULL;");
    }
    else if (isLocalVar() || isSymbolVar()) {
      if (isRefArgument())
        out.println(varName + " = " + argName + ".toLocalVarDeclAsRef();");
      else if (isArgument())
        out.println(varName + " = " + argName + ".toLocalVar();");
      else if (isSuperGlobal())
        out.println(varName + " = env.getGlobalVar(\"" + getName() + "\");");
      else if (isInitializedVar())
        out.println(varName + " = null;");
      else
        out.println(varName + " = new Var();");
    }
    else if (isEnvVar()) {
      // php/3205
      out.print(varName + " = env.getLazyEnvVar(");
      out.printString(getName());
      out.println(");");

      if (isRefArgument())
	out.println(varName + ".setRef(" + argName + ".toLocalVarDeclAsRef());");
      else if (isArgument())
	out.println(varName + ".setRef(" + argName + ".toLocalVar());");
    }
    else
      throw new IllegalStateException();
  }
}

