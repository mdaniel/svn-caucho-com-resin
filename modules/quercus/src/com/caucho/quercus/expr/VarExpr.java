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

import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.parser.PhpParser;
import com.caucho.quercus.program.AnalyzeInfo;

import java.io.IOException;
import java.util.HashSet;

/**
 * Represents a PHP variable expression.
 */
public class VarExpr
  extends AbstractVarExpr
{
  private static final NullValue NULL = NullValue.create();

  private final VarInfo _var;
  private final String _name;

  private VarState _varState = VarState.INIT;

  public VarExpr(VarInfo var)
  {
    _var = var;
    _name = var.getName();
  }

  /**
   * Returns the variable info.
   */
  public VarInfo getVarInfo()
  {
    return _var;
  }

  /**
   * Returns the variable name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the java variable name.
   */
  public String getJavaVar()
  {
    return "v_" + _name;
  }

  /**
   * Copy for things like $a .= "test";
   */
  public Expr copy()
  {
    return new VarExpr(_var);
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssign(PhpParser parser, Expr value)
  {
    _var.setAssigned();

    return super.createAssign(parser, value);
  }

  /**
   * Creates the assignment.
   */
  public void assign(PhpParser parser)
  {
    _var.setAssigned();
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssignRef(PhpParser parser, Expr value)
  {
    _var.setAssigned();

    return super.createAssignRef(parser, value);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value eval(Env env)
    throws Throwable
  {
    Value value;

    if (_var.isGlobal())
      value = env.getGlobalValue(_name);
    else
      value = env.getValue(_name);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalCopy(Env env)
    throws Throwable
  {
    return eval(env).copy();
  }

  /**
   * Evaluates the expression, converting to an array if unset.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalArray(Env env)
    throws Throwable
  {
    Value value;

    if (_var.isGlobal()) {
      value = env.getGlobalValue(_name);

      if (value == null || ! value.isset()) {
        value = new ArrayValueImpl();

        env.setGlobalValue(_name, value);
      }
    } else {
      value = env.getValue(_name);

      if (value == null || ! value.isset()) {
        value = new ArrayValueImpl();

        env.setValue(_name, value);
      }
    }

    return value;
  }

  /**
   * Evaluates the expression, converting to an object if unset.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalObject(Env env)
    throws Throwable
  {
    Value value;

    if (_var.isGlobal()) {
      value = env.getGlobalValue(_name);

      if (value == null || ! value.isset()) {
        value = env.createObject();

        env.setGlobalValue(_name, value);
      }
    } else {
      value = env.getValue(_name);

      if (value == null || ! value.isset()) {
        value = env.createObject();

        env.setValue(_name, value);
      }
    }

    return value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalRef(Env env)
    throws Throwable
  {
    if (getVarInfo().isGlobal())
      return env.getGlobalVar(_name);
    else
      return env.getVar(_name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalArg(Env env)
    throws Throwable
  {
    // quercus/043k
    // quercus/0443

    if (getVarInfo().isGlobal())
      return env.getGlobalVar(_name);
    else
      return env.getVar(_name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   */
  public void evalAssign(Env env, Value value)
    throws Throwable
  {
    if (getVarInfo().isGlobal())
      env.setGlobalValue(_name, value);
    else
      env.setValue(_name, value);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   */
  public void evalUnset(Env env)
    throws Throwable
  {
    if (getVarInfo().isGlobal())
      env.unsetGlobalVar(_name);
    else
      env.unsetLocalVar(_name);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    VarExpr var = info.getVar(_name);

    if (var == null) {
      setVarState(VarState.UNSET);

      var = new VarExpr(_var);
      var.setVarState(VarState.VALID);
    } else
      setVarState(var.getVarState());

    info.addVar(var.analyzeVarState(VarState.VALID));
  }

  /**
   * Analyze a variable assignment
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    getVarInfo().setAssigned();

    VarExpr infoVar = info.getVar(getName());

    if (infoVar == null) {
      setVarState(VarState.UNSET);

      infoVar = new VarExpr(getVarInfo());
      infoVar.setVarState(VarState.VALID);
    } else if (_varState == VarState.INIT ||
               _varState == infoVar.getVarState()) {
      setVarState(infoVar.getVarState());

      infoVar = infoVar.analyzeVarState(VarState.VALID);
    } else {
      // quercus/3a0v
      setVarState(VarState.UNKNOWN);

      infoVar = infoVar.analyzeVarState(VarState.VALID);
    }

    info.addVar(infoVar);
  }

  /**
   * Analyze the expression as modified
   */
  public void analyzeSetModified(AnalyzeInfo info)
  {
    getVarInfo().setModified();
  }

  /**
   * Analyze the expression as modified
   */
  public void analyzeSetReference(AnalyzeInfo info)
  {
    getVarInfo().setReference();
  }

  /**
   * Analyze the expression
   */
  public void analyzeUnset(AnalyzeInfo info)
  {
    VarExpr var = info.getVar(_name);

    setVarState(VarState.UNSET);

    if (var != null)
      info.addVar(var.analyzeVarState(VarState.UNSET));
  }

  /**
   * Returns the variables used in the expression
   *
   * @param vars the variables used in the function
   */
  public void getVariables(HashSet<VarExpr> vars)
  {
    vars.add(this);
  }

  /**
   * Returns the assignment state of the variable.
   */
  public VarState getVarState()
  {
    return _varState;
  }

  /**
   * Sets the assignment state of the variable.
   */
  public void setVarState(VarState state)
  {
    // php/3a0i
    if (_varState == VarState.INIT || _varState == state)
      _varState = state;
    else
      _varState = VarState.UNKNOWN;
  }

  /**
   * Sets the assignment state of the variable.
   */
  public VarExpr analyzeVarState(VarState state)
  {
    if (_varState == state)
      return this;
    else {
      VarExpr var = new VarExpr(_var);
      var.setVarState(state);

      return var;
    }
  }

  /**
   * Sets the assignment state of the variable.
   */
  public VarExpr analyzeMerge(VarExpr mergeVar)
  {
    if (_varState == mergeVar._varState)
      return this;
    else
      return analyzeVarState(VarState.UNKNOWN);
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   * @param owner the owning expression
   */
  public VarState getVarState(VarExpr var, VarExpr owner)
  {
    if (var == this)
      return VarState.UNKNOWN;
    else if (var.equals(this))
      return VarState.VALID;
    else
      return VarState.UNKNOWN;
  }

  /**
   * Generates code to evaluate the expression as top level
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out)
    throws IOException
  {
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    VarState state = getVarState();

    if (state == VarState.INIT) {
      throw new IllegalStateException(getLocation() +
                                      "'" +
                                      this +
                                      "' is not analyzed.");
    } else if (! _var.isReference()) {
      out.print(getJavaVar());
    } else if (state == VarState.VALID) {
      out.print(getJavaVar());
    } else if (state == VarState.UNSET) {
      if (_var.isGlobal()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getGlobalVar(\"");
        out.printJavaString(_name);
        out.print("\"))");
      } else if (_var.isVariable()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getVar(\"");
        out.printJavaString(_name);
        out.print("\"))");
      } else if (_var.isReference()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = new Var())");
      } else {
        out.print("NullValue.NULL");
      }
    } else {
      if (_var.isGlobal()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getGlobalVar(\"");
        out.printJavaString(_name);
        out.print("\", ");
        out.print(getJavaVar());
        out.print("))");
      } else if (_var.isVariable()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getVar(\"");
        out.printJavaString(_name);
        out.print("\", ");
        out.print(getJavaVar());
        out.print("))");
      } else {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getLocalVar(");
        out.print(getJavaVar());
        out.print("))");
      }
    }

    // XXX: handle the .toValue()
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateValue(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toValue()");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".copy()"); // php/3a5o
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    VarState state = getVarState();

    if (state == VarState.INIT) {
      throw new IllegalStateException(getLocation() +
                                      "'" +
                                      this +
                                      "' is not analyzed.");
    } else if (! _var.isReference()) {
      if (isTop) {
        // php/3a60
        out.print(getJavaVar());
        out.print(" = ");
        value.generateCopy(out);
      } else {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = ");
        value.generateCopy(out);
        out.print(")");
      }
    } else if (state == VarState.VALID) {
      out.print(getJavaVar());
      out.print(".set(");
      value.generateCopy(out);
      out.print(")");
    } else if (state == VarState.UNSET) {
      if (_var.isGlobal()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getGlobalVar(\"");
        out.printJavaString(_name);
        out.print("\"))");
        out.print(".set(");
        value.generateCopy(out);
        out.print(")");
      } else if (_var.isVariable()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getVar(\"");
        out.printJavaString(_name);
        out.print("\"))");
        out.print(".set(");
        value.generateCopy(out);  // php/3a51
        out.print(")");
      } else if (_var.isReference()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = new Var())");
        out.print(".set(");
        value.generateCopy(out);
        out.print(")");
      } else {
        out.print("NullValue.NULL");
      }
    } else {
      if (_var.isGlobal()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getGlobalVar(\"");
        out.printJavaString(_name);
        out.print("\", ");
        out.print(getJavaVar());
        out.print("))");
        out.print(".set(");
        value.generateCopy(out);
        out.print(")");
      } else if (_var.isVariable()) {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getVar(\"");
        out.printJavaString(_name);
        out.print("\", ");
        out.print(getJavaVar());
        out.print("))");
        out.print(".set(");
        value.generateCopy(out);
        out.print(")");
      } else {
        out.print("(");
        out.print(getJavaVar());
        out.print(" = env.getLocalVar(");
        out.print(getJavaVar());
        out.print("))");
        out.print(".set(");
        value.generateCopy(out);
        out.print(")");
      }
    }
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    //VarState state = getVarState();

    if (! isTop)
      out.print("(");

    if (_var.isGlobal() || _var.isVariable()) {
      out.print(getJavaVar());
      out.print(" = env.setVar(\"");
      out.printJavaString(_name);
      out.print("\", ");
      value.generateRef(out);
      out.print(")");
    } else {
      // php/3475
      out.print(getJavaVar());
      out.print(" = Env.setRef(");
      out.print(getJavaVar());
      out.print(", ");
      value.generateRef(out);
      out.print(")");
    }

    if (! isTop)
      out.print(")");

    // XXX: handle the .toValue()
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, String value)
    throws IOException
  {
    if (_var.isGlobal()) {
      out.print(getJavaVar());
      out.print(" = env.setVar(\"");
      out.printJavaString(_name);
      out.print("\", " + value + ")");
    } else {
      out.print(getJavaVar());
      out.print(" = ");
      out.print(value);
    }
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    // php/3d11
    generate(out);
    // out.print(".getArray()");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateObject(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".getObject(env)");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    //out.print("env.unsetLocalVar(\"");
    //out.printJavaString(_name);
    //out.println("\");");
    out.print(getJavaVar() + " = NullValue.NULL");
    //out.print(getJavaVar());
    //out.print(" = env.unsetVar(\"");
    //out.printJavaString(_name);
    //out.print("\")");
  }

  public int hashCode()
  {
    return _name.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (getClass() != o.getClass())
      return false;

    VarExpr var = (VarExpr) o;

    return _var == var._var;
  }

  public String toString()
  {
    return "$" + _name;
  }
}

