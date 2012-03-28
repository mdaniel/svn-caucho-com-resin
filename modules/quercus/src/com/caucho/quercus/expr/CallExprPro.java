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
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.lib.regexp.Regexp;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.CompilingFunction;
import com.caucho.quercus.program.FunctionGenerator;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents a PHP function expression.
 */
public class CallExprPro extends CallExpr
  implements ExprPro
{
  private static final L10N L = new L10N(CallExprPro.class);

  private FunctionGenerateExpr _generator;

  public CallExprPro(Location location, String name, ArrayList<Expr> args)
  {
    super(location, name, args);
  }

  public CallExprPro(Location location, String name, Expr []args)
  {
    super(location, name, args);
  }

  /**
   * Returns the reference of the value.
   * @param location
   */
  /*
  public Expr createRef(Location location)
  {
    return new UnaryRefExpr(location, this);
  }
  */

  public ExprGenerator getGenerator()
  {
    if (_generator != null) {
    }
    else if ("assert".equals(_name)) {
      _generator = new AssertGenerateExpr(getLocation());
    }
    else if ("preg_replace".equals(_name)) {
      _generator = new PregReplaceGenerateExpr(getLocation());
    }
    else {
      _generator = new FunctionGenerateExpr(getLocation());
    }

    return _generator;
  }

  class FunctionGenerateExpr extends ExprGenerator
  {
    FunctionGenerateExpr(Location loc)
    {
      super(loc);
    }

    //
    // Java code generation
    //

    /**
     * Analyzes the function.
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      QuercusContext quercus = info.getFunction().getQuercus();
      AbstractFunction fun = info.findFunction(_name);

      if (fun != null)
        fun = fun.getActualFunction(_args);

      FunctionGenerator funGen = null;

      if (fun != null && fun instanceof CompilingFunction)
        funGen = ((CompilingFunction) fun).getGenerator();

      for (int i = 0; i < _args.length; i++) {
        ExprPro arg = (ExprPro) _args[i];
        
        arg.getGenerator().analyze(info);
      }

      if (funGen != null && funGen.isCallUsesVariableArgs())
        info.getFunction().setVariableArgs(true);

      analyzeUsesSymbolTable(info, funGen);

      // check for read-only and refs

      if (funGen != null) {
        funGen.analyzeArguments(_args, info);
      }
      else {
        // if unknown, need to be pessimistic
        for (int i = 0; i < _args.length; i++) {
          ExprPro arg = (ExprPro) _args[i];

          arg.getGenerator().analyzeSetModified(info);
          arg.getGenerator().analyzeSetReference(info);
        }
      }

      // XXX: can check function type for static functions

      return ExprType.VALUE;
    }

    protected void analyzeUsesSymbolTable(AnalyzeInfo info,
                                          FunctionGenerator funGen)
    {
      if (funGen != null && funGen.isCallUsesSymbolTable()) {
        // php/1729
        info.getFunction().setUsesSymbolTable(true);

        if (funGen.isCallReplacesSymbolTable()) {
          info.clear();
        }
      }
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out)
      throws IOException
    {
      generateImpl(out, Value.class, false, false);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
      public void generateCopy(PhpWriter out)
      throws IOException
    {
      generateImpl(out, Value.class, false, true);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateRef(PhpWriter out)
      throws IOException
    {
      // php/3243
      generateImpl(out, Value.class, true, false);
      // out.print(".toRefVar()");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateVar(PhpWriter out)
      throws IOException
    {
      // php/3243
      generateImpl(out, Value.class, true, false);
      // out.print(".toRefVar()");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateTop(PhpWriter out)
      throws IOException
    {
      generateImpl(out, void.class, false, false);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateBoolean(PhpWriter out)
      throws IOException
    {
      generateImpl(out, boolean.class, false, false);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateValue(PhpWriter out)
      throws IOException
    {
      generateImpl(out, Value.class, false, false);  // php/3c4k
      // out.print(".toValue()");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    private void generateImpl(PhpWriter out,
                              Class<?> retType,
                              boolean isRef,
                              boolean isCopy)
      throws IOException
    {
      // Quercus quercus = out.getQuercus();

      // StaticFunction fun = quercus.findFunction(_name);

      QuercusProgram program = out.getProgram();

      AbstractFunction fun = program.findFunction(_name);

      if (fun == null || ! fun.isGlobal())
        fun = program.getPhp().findFunction(_name);

      if (fun != null)
        fun = fun.getActualFunction(_args);

      FunctionGenerator funGen = null;

      if (fun != null && fun instanceof CompilingFunction)
        funGen = ((CompilingFunction) fun).getGenerator();

      if (funGen != null
          && funGen.canGenerateCall(_args)
          && ! funGen.isVoidReturn()) {
        if (void.class.equals(retType))
          funGen.generateTop(out, this, _args);
        else if (boolean.class.equals(retType))
          funGen.generateBoolean(out, this, _args);
        else if (isRef && fun.isReturnsReference()) // php/3442
          funGen.generateRef(out, this, _args);
        else if (isCopy || isRef) // php/3442
          funGen.generateCopy(out, this, _args);
        else
          funGen.generate(out, this, _args);
      }
      else {
        // super.generate(out);

        // XXX: need to check where it's from

        String id = out.addFunctionId(_name);

        out.print("env._fun[" + id + "]");

        if (isRef)
          out.print(".callRef(env");
        else
          out.print(".call(env");

        if (_args.length <= COMPILE_ARG_MAX) {
          for (int i = 0; i < _args.length; i++) {
            ExprPro arg = (ExprPro) _args[i];

            out.print(", ");

            arg.getGenerator().generateArg(out, true);
          }
        }
        else {
          out.print(", new Value[] {");

          for (int i = 0; i < _args.length; i++) {
            ExprPro arg = (ExprPro) _args[i];

            if (i != 0)
              out.print(", ");

            arg.getGenerator().generateArg(out, true);
          }

          out.print("}");
        }

        out.print(")");

        if (boolean.class.equals(retType))
          out.print(".toBoolean()");
        else if (isRef)
          out.print(".toRef()");
        else if (isCopy && ! isRef) {
          // php/347b
          out.print(".copyReturn()");
        }
      }
    }

    /**
     * Generates code to recreate the expression.  Used for default values.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateExpr(PhpWriter out)
      throws IOException
    {
      out.print("new FunctionExpr(\"");
      out.printJavaString(_name);
      out.print("\", new Expr[] {");

      for (int i = 0; i < _args.length; i++) {
        ExprPro arg = (ExprPro) _args[i];

        arg.getGenerator().generateExpr(out);
        out.print(", ");
      }

      out.print("})");
    }
  }

  class AssertGenerateExpr extends FunctionGenerateExpr
  {
    AssertGenerateExpr(Location loc)
    {
      super(loc);
    }

    protected void analyzeUsesSymbolTable(AnalyzeInfo info,
                                          FunctionGenerator funGen)
    {
      if (_args.length == 1 && _args[0].isBoolean()) {
        // php/3a91
        // boolean assert does not use the symbol table
      }
      else {
        super.analyzeUsesSymbolTable(info, funGen);
      }
    }
  }

  class PregReplaceGenerateExpr extends FunctionGenerateExpr
  {
    PregReplaceGenerateExpr(Location loc)
    {
      super(loc);
    }

    @Override
    protected void analyzeUsesSymbolTable(AnalyzeInfo info,
                                          FunctionGenerator funGen)
    {
      // php/3a92
      // check for the 'e' value
      if (_args.length > 0 && _args[0].isConstant()) {
        Value value = _args[0].evalConstant();

        if (value != null && value.isArray()) {
          ArrayValue array = (ArrayValue) value;
          for (Map.Entry<Value,Value> entry : array.entrySet()) {
            StringValue entryStringValue = entry.getValue().toStringValue();

            if (isEvalPossible(entryStringValue, entryStringValue)) {
              super.analyzeUsesSymbolTable(info, funGen);

              return;
            }
          }

          return;
        }

        Value prefix = _args[0].evalConstantPrefix();
        Value suffix = _args[0].evalConstantSuffix();

        if (prefix != null && suffix != null
            && ! isEvalPossible(prefix.toStringValue(),
                                suffix.toStringValue())) {
          return;
        }
      }

      super.analyzeUsesSymbolTable(info, funGen);
    }

    /**
     * Check if the expression is an /e expression.
     */
    private boolean isEvalPossible(StringValue prefix, StringValue suffix)
    {
      if (prefix.length() == 0)
        return true;

      char ch = prefix.charAt(0);
      char end = ch;

      switch (ch) {
      case '{':
        end = '}';
        break;
      case '[':
        end = ']';
        break;
      case '<':
        end = '>';
        break;
      }

      int p = suffix.lastIndexOf(end);

      // if the 'e' flag doesn't exist, then it
      // doesn't use the symbol table
      return p < 0 || suffix.indexOf('e', p) > 0;
    }
  }
}

