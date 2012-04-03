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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.parser.QuercusParser;

import java.io.IOException;
import java.util.HashSet;

/**
 * Represents a PHP variable expression.
 */
public class VarExprPro extends VarExpr
  implements ExprPro
{
  private InfoVarPro _var;

  private VarState _varState = VarState.INIT;
  private ExprType _type = ExprType.INIT;

  public VarExprPro(InfoVarPro var)
  {
    super(var);

    _var = var;

    // arguments
    if (var.isVar())
      _type = ExprType.VALUE;
  }

  /**
   * Returns the variable info.
   */
  @Override
  public InfoVarPro getVarInfo()
  {
    return _var;
  }

  public boolean isValue()
  {
    return _var.isValue();
  }

  /**
   * Copy for things like $a .= "test";
   * @param location
   */
  @Override
  public Expr copy(Location location)
  {
    return new VarExprPro(_var);
  }

  /**
   * Creates the assignment.
   */
  @Override
  public void assign(QuercusParser parser)
  {
    _var.setAssigned();
  }

  /**
   * Creates the assignment.
   */
  @Override
  public Expr createAssignRef(QuercusParser parser,
                              Expr value)
  {
    _var.setAssigned();

    return super.createAssignRef(parser, value);
  }

  //
  // analyze/generate functions
  //

  /**
   * Sets the assignment state of the variable.
   */
  public VarExprPro analyzeVarState(VarState state)
  {
    if (_varState == state)
      return this;
    else {
      VarExprPro var = new VarExprPro(_var);
      var.setVarState(state);

      return var;
    }
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
  public VarExprPro analyzeMerge(VarExprPro mergeVar)
  {
    _type = _type.withType(mergeVar._type);

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

  public void setInitializedVar(boolean isInit)
  {
    _var.setInitializedVar(isInit);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractAssignGenerateExpr(getLocation()) {
      /**
       * Returns the type
       */
      @Override
      public ExprType getType()
      {
        return _var.getType();
      }

      @Override
      public boolean isVar()
      {
        return true;
      }

      /**
       * Analyze the expression as a statement
       */
      public void analyzeTop(AnalyzeInfo info)
      {
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        VarExprPro var = info.getVar(_name);

        if (var == null) {
          // php/3230 - if read w/o value, must be VALUE
          getVarInfo().withType(ExprType.VALUE);

          setVarState(VarState.UNSET);

          var = new VarExprPro(_var);
          var.setVarState(VarState.VALID);
        } else {
          if (var.getVarState() != VarState.VALID) {
            getVarInfo().withType(ExprType.VALUE);
            _type = ExprType.VALUE;
          }

          setVarState(var.getVarState());
          // php/3aam
          _type = _type.withType(var._type);
        }

        info.addVar(var.analyzeVarState(VarState.VALID));

        return _type;
      }

      /**
       * Analyze a variable assignment
       */
      @Override
      public ExprType analyzeAssign(AnalyzeInfo info, ExprGenerator value)
      {
        getVarInfo().setAssigned();

        VarExprPro infoVar = info.getVar(getName());

        if (infoVar == null) {
          setVarState(VarState.UNSET);

          infoVar = new VarExprPro(getVarInfo());
          infoVar.setVarState(VarState.VALID);
        } else if (_varState == VarState.INIT
                   || _varState == infoVar.getVarState()) {
          setVarState(infoVar.getVarState());

          infoVar = infoVar.analyzeVarState(VarState.VALID);
        } else {
          // quercus/3a0v
          setVarState(VarState.UNKNOWN);

          infoVar = infoVar.analyzeVarState(VarState.VALID);
        }

        info.addVar(infoVar);

        ExprType type = value.analyze(info);

        getVarInfo().withType(type);

        // php/3aam
        infoVar._type = infoVar._type.withType(type);
        _type = _type.withType(type);

        return type;
      }

      /**
       * Analyze the expression as modified, e.g. forcing an upgrade
       * to an object
       */
      @Override
      public void analyzeSetModified(AnalyzeInfo info)
      {
        _type = ExprType.VALUE;

        // php/3492
        if (isString()) {
          getVarInfo().setVar();
        }

        getVarInfo().setArrayModified(true);

        VarExprPro var = info.getVar(getName());

        if (var != null) {
          setVarState(var.getVarState()); // php/323i
        }
        else {
          setVarState(VarState.UNSET); // php/323j
        }

        // php/39o3
        // getVarInfo().setVar();
      }

      /**
       * Analyze the expression as referenced, i.e. forcing a var
       */
      @Override
      public void analyzeSetReference(AnalyzeInfo info)
      {
        _type = ExprType.VALUE;

        getVarInfo().setVar();
      }

      /**
       * Analyze the expression as modified
       */
      @Override
      public void analyzeSetPostIncrement()
      {
        // getVarInfo().setPostIncrement();
      }

      /**
       * Analyze the expression
       */
      @Override
      public void analyzeUnset(AnalyzeInfo info)
      {
        VarExprPro var = info.getVar(_name);

        setVarState(VarState.UNSET);
        // php/322a
        // _var.setVar();

        if (var != null) {
          info.addVar(var.analyzeVarState(VarState.UNSET));
        }
      }

      /**
       * Sets the assignment state of the variable.
       */
      public VarExprPro analyzeVarState(VarState state)
      {
        if (_varState == state)
          return VarExprPro.this;
        else {
          VarExprPro var = new VarExprPro(_var);
          var.setVarState(state);

          return var;
        }
      }

      /**
       * Generates code to evaluate the expression as top level
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateTop(PhpWriter out)
        throws IOException
      {
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generate(PhpWriter out)
        throws IOException
      {
        VarState state = getVarState();

        if (state == VarState.INIT) {
          throw new IllegalStateException(getLocation() +
                                          "'" + this + "' is not analyzed.");
        }

        if (_var.isValue()) {
          switch (_var.getType()) {
          case LONG:
            out.print("LongValue.create(" + getJavaVar() + ")");
            break;

          case DOUBLE:
            out.print("new DoubleValue(" + getJavaVar() + ")");
            break;

          case BOOLEAN:
            out.print("(" + getJavaVar() + " ? BooleanValue.TRUE : BooleanValue.FALSE)");
            break;

          default:
            out.print(getJavaVar());
          }
        }
        else if (_var.isLocalVar()) {
          out.print(getJavaVar());
        }
        else if (_var.isSymbolVar()) {
          out.print("_v[" + _var.getSymbolName() + "]");
        }
        else if (_var.isEnvVar()) {
          out.print(getJavaVar());
          out.print(".get()");
        }
        else
          throw new IllegalStateException();

        // XXX: handle the .toValue()
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateRef(PhpWriter out)
        throws IOException
      {
        if (_var.isSymbolVar()) {
          out.print("_v[" + _var.getSymbolName() + "]");
        }
        else if (_var.isEnvVar()) {
          out.print(getJavaVar());
          out.print(".getVar()");
        }
        else
          generate(out);
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateVar(PhpWriter out)
        throws IOException
      {
        generateRef(out);
      }

      /**
       * Generates code to evaluate the expression as a boolean
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateBoolean(PhpWriter out)
        throws IOException
      {
        // php/3aau
        if (! _var.isValue())
          super.generateBoolean(out);
        else if (isBoolean())
          out.print(getJavaVar());
        else if (isNumber())
          out.print("(" + getJavaVar() + " != 0)");
        else
          super.generateBoolean(out);
      }

      /**
       * Generates code to evaluate the expression as an int
       *
       * @param out the writer to the Java source code.
       */
      public void generateInt(PhpWriter out)
        throws IOException
      {
        if (! _var.isValue())
          super.generateInt(out);
        else if (isLong())
          out.print("(int) " + getJavaVar());
        else if (isDouble())
          out.print("(int) " + getJavaVar());
        else if (isBoolean())
          out.print("(" + getJavaVar() + " ? 1 : 0)");
        else
          super.generateInt(out);
      }

      /**
       * Generates code to evaluate the expression as a long
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateLong(PhpWriter out)
        throws IOException
      {
        if (! _var.isValue())
          super.generateLong(out);
        else if (isLong())
          out.print(getJavaVar());
        else if (isDouble())
          out.print("(double) " + getJavaVar());
        else if (isBoolean())
          out.print("(" + getJavaVar() + " ? 1 : 0)");
        else
          super.generateLong(out);
      }

      /**
       * Generates code to evaluate the expression as a double
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateDouble(PhpWriter out)
        throws IOException
      {
        if (! _var.isValue())
          super.generateDouble(out);
        else if (isDouble())
          out.print(getJavaVar());
        else if (isBoolean())
          out.print("(" + getJavaVar() + " ? 1 : 0)");
        else
          super.generateDouble(out);
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateModifiedRead(PhpWriter out)
        throws IOException
      {
        VarState state = getVarState();

        if (state == VarState.INIT) {
          throw new IllegalStateException(getLocation() +
                                          "'" + this + "' is not analyzed.");
        }

        /*
        if (_var.isVar()) {
          generate(out);
        }
        else {  // reference in Java local
          out.print(getJavaVar());
        }
        */

        generateRef(out);

        // XXX: handle the .toValue()
      }

      /**
       * Generates beginning code to convert to a value.
       */
      private void generateToValueBegin(PhpWriter out)
        throws IOException
      {
        switch (_var.getType()) {
          case LONG:
            out.print("LongValue.create(");
            break;

          case DOUBLE:
            out.print("new DoubleValue(");
            break;

          case BOOLEAN:
            out.print("BooleanValue.create(");
            break;

          default:
            out.print("(");
        }
      }

      /**
       * Generates ending code to convert to a value.
       */
      private void generateToValueEnd(PhpWriter out)
        throws IOException
      {
        out.print(")");
      }

      /**
       * Generates code for a reference.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateArg(PhpWriter out, boolean isTop)
        throws IOException
      {
        if (_var.isVar()) {
          // php/3d1p
          generateRef(out);
          // out.print(".toRefVar()");
        }
        else
          generate(out);
      }

      /**
       * Generates code for a reference.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateRefArg(PhpWriter out)
        throws IOException
      {
        if (_var.isVar()) {
          // php/3d1p
          generateRef(out);
          out.print(".toArgRef()");
        }
        else
          generate(out);
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateValue(PhpWriter out)
        throws IOException
      {
        generate(out);

        if (_var.isVar())
          out.print(".toValue()");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateCopy(PhpWriter out)
        throws IOException
      {
        generate(out);

        switch (_type) {
          case STRING:
          case BOOLEAN:
          case LONG:
          case DOUBLE:
            // php/33db
            if (_var.isVar())
              out.print(".toValue()");
                break;
          default:
            out.print(".copy()"); // php/3a5o
        }
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateAssign(PhpWriter out, Expr value,
                                 boolean isTop)
        throws IOException
      {
        VarState state = getVarState();

        if (state == VarState.INIT) {
          throw new IllegalStateException(getLocation() +
                          "'" + this + "' is not analyzed.");
        }

        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        if (_var.isValue()) {
          if (isTop) {
            // php/3a60

            out.print(getJavaVar());
            out.print(" = ");

            generateAssignValue(out, valueGen);
          } else {
            generateToValueBegin(out);

            out.print(getJavaVar());
            out.print(" = ");

            generateAssignValue(out, valueGen);
            generateToValueEnd(out);
          }
        }
        else if (_var.isSuperGlobal()) {
          // php/3867
          out.print("env.setGlobalValue(");
          out.printString(_var.getName());
          out.print(", ");
          valueGen.generateCopy(out);
          out.print(")");
        }
        else if (_var.isLocalVar()) {
          out.print(getJavaVar());
          out.print(".set(");
          valueGen.generateCopy(out);
          out.print(")");
        }
        else if (_var.isSymbolVar()) {
          out.print("_v[" + _var.getSymbolName() + "].set(");
          valueGen.generateCopy(out);
          out.print(")");
        }
        else if (_var.isEnvVar()) {
          out.print(getJavaVar());
          out.print(".set(");
          valueGen.generateCopy(out);
          out.print(")");
        }
        else
          throw new IllegalStateException();
      }

      private void generateAssignValue(PhpWriter out, ExprGenerator valueGen)
        throws IOException
      {
        switch (_var.getType()) {
          case LONG:
            valueGen.generateLong(out);
            break;

          case DOUBLE:
            valueGen.generateDouble(out);
            break;

          case BOOLEAN:
            valueGen.generateBoolean(out);
            break;

          case STRING:
            valueGen.generateStringValue(out);
            break;

          default:
            // php/3a5x
            valueGen.generateCopy(out);
            /*
            if (getVarInfo().isArrayModified())
              valueGen.generateCopy(out);
            else
              valueGen.generateValue(out);
              */
            break;
          }
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateAssignOpen(PhpWriter out)
        throws IOException
      {
        VarState state = getVarState();

        if (state == VarState.INIT) {
          throw new IllegalStateException(getLocation() +
                          "'" + this + "' is not analyzed.");
        }

        if (_var.isValue()) {
          out.print(getJavaVar());
          out.print(" = ");
        }
        else if (_var.isSuperGlobal()) {
          // php/3867
          out.print("env.setGlobalValue(");
          out.printString(_var.getName());
          out.print(", ");
        }
        else if (_var.isLocalVar()) {
          out.print(getJavaVar());
          out.print(".set(");
        }
        else if (_var.isSymbolVar()) {
          out.print("_v[" + _var.getSymbolName() + "].set(");
        }
        else if (_var.isEnvVar()) {
          out.print(getJavaVar());
          out.print(".set(");
        }
        else
          throw new IllegalStateException();
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateAssignClose(PhpWriter out)
        throws IOException
      {
        VarState state = getVarState();

        if (state == VarState.INIT) {
          throw new IllegalStateException(getLocation() +
                          "'" + this + "' is not analyzed.");
        }

        if (_var.isValue()) {
        }
        else if (_var.isSuperGlobal()) {
          out.print(")");
        }
        else if (_var.isLocalVar()) {
          out.print(")");
        }
        else if (_var.isSymbolVar()) {
          out.print(")");
        }
        else if (_var.isEnvVar()) {
          out.print(")");
        }
        else
          throw new IllegalStateException();
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssignBoolean(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        out.print("(");

        generateAssign(out, value, isTop);

        out.print(")");

        if (! _var.isValue() || ! isTop)
          out.print(".toBoolean()");
        else if (isNumber())
          out.print(" != 0");
        else if (isBoolean()) {
        }
        else
          out.print(".toBoolean()");
      }

      /**
       * Generates code to evaluate the expression as a reference
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        if (_var.isValue())
          throw new IllegalStateException(this + " cannot be a reference");

        //VarState state = getVarState();

        if (! isTop)
          out.print("(");

        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        //if (_var.isGlobal() || _var.isVarVar()) {
        if (_var.isLocalVar()) {
          // php/3475, php/3243

          out.print(getJavaVar());
          out.print(" = ");

          if (valueGen.isVar()) {
            valueGen.generateVar(out);
          }
          else {
            out.print(getJavaVar());
            out.print(".setRef(");
            valueGen.generateRef(out);
            out.print(")");
          }
          /*
          if (valueGen.isVar()) {
            valueGen.generateVar(out);
          }
          else {
            out.print(getJavaVar());
            out.print(".setRef(");
            valueGen.generateRef(out);
            out.print(")");
          }
          */
        }
        else if (_var.isSymbolVar()) {
          out.print("_v[" + _var.getSymbolName() + "] = ");

          if (valueGen.isVar()) {
            valueGen.generateVar(out);
          }
          else {
            out.print("_v[" + _var.getSymbolName() + "].setRef(");
            valueGen.generateRef(out);
            out.print(")");
          }
        }
        else if (_var.isEnvVar()) {
          out.print(getJavaVar());
          out.print(".setRef(");
          valueGen.generateRef(out);
          out.print(")");
        }
        else {
          throw new IllegalStateException(String.valueOf(_var));
        }

        if (! isTop)
          out.print(")");

        // XXX: handle the .toValue()
      }

      /**
       * Generates code for an array assignment $a[$index] = $value.
       */
      public void generateArrayAssign(PhpWriter out,
                                      ExprGenerator index,
                                      ExprGenerator value,
                                      boolean isTop)
        throws IOException
      {
        if (isValue()) {
          generate(out);
        }
        else {
          generateArray(out);
        }

        if (isTop) {
          out.print(".append(");
        }
        else {
          out.print(".put(");
        }

        index.generate(out);
        out.print(", ");

        value.generateCopy(out); // php/3a5k
        out.print(")");
      }

      /**
       * $a[0] = 3
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateArray(PhpWriter out)
            throws IOException
      {
        // php/3d11, php/3a68, php/348a

        if (_var.isValue()) {
          out.print("(");
          out.print(getJavaVar());
          out.print(" = ");

          out.print(getJavaVar());
          out.print(".toAutoArray())");
        }
        else
          generateRef(out);
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateObject(PhpWriter out)
        throws IOException
      {
        // php/39o3
        if (_var.isValue()) {
          out.print("(");
          out.print(getJavaVar());
          out.print(" = ");
          out.print(getJavaVar());
          out.print(".toAutoObject(env))");
        }
        else
          generateRef(out);
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateUnset(PhpWriter out)
        throws IOException
      {
        if (_var.isValue()) {
          out.print(getJavaVar());

          if (isLong())
            out.print(" = 0");
          else if (isDouble())
            out.print(" = 0.0");
          else if (isBoolean())
            out.print(" = false");
          else
            out.print(" = NullValue.NULL");
        }
        else if (_var.isSymbolVar()) {
          out.print("_v[" + _var.getSymbolName() + "] = new Var()");
        }
        else if (_var.isEnvVar()) {
          // php/3220
          out.print(getJavaVar());
          out.print(".setRef(new Var())");
        }
        else {
          // XXX: need to test ref
          out.print(getJavaVar());
          out.print(" = new Var()");
        }
      }

      public String toString()
      {
        return VarExprPro.this.toString();
      }
    };
}

