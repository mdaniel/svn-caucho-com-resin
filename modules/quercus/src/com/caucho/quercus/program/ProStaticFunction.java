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

package com.caucho.quercus.program;

import com.caucho.quercus.annotation.*;
import com.caucho.quercus.expr.*;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.ProMarshal;
import com.caucho.quercus.marshal.ProReferenceMarshal;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.module.StaticFunction;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

/**
 * Represents the introspected static function information.
 */
public class ProStaticFunction extends StaticFunction
  implements CompilingFunction
{
  private static final L10N L = new L10N(ProStaticFunction.class);
  private static final Logger log
    = Logger.getLogger(ProStaticFunction.class.getName());

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public ProStaticFunction(ModuleContext moduleContext,
                           QuercusModule quercusModule,
                           Method method)
  {
    super(moduleContext, quercusModule, method);
  }

  public FunctionGenerator getGenerator()
  {
    return GENERATOR;
  }

  private FunctionGenerator GENERATOR = new FunctionGenerator() {
    @Override
    public boolean isBoolean()
    {
      return ProStaticFunction.this.isBoolean();
    }

    @Override
    public boolean isString()
    {
      return ProStaticFunction.this.isString();
    }

    @Override
    public boolean isLong()
    {
      return ProStaticFunction.this.isLong();
    }

    @Override
    public boolean isDouble()
    {
      return ProStaticFunction.this.isDouble();
    }

    /**
     * Returns true if the function uses variable args.
     */
    @Override
    public boolean isCallUsesVariableArgs()
    {
      return _method.isAnnotationPresent(VariableArguments.class);
    }

    /**
     * Returns true if the function uses/modifies the local symbol table
     */
    @Override
    public boolean isCallUsesSymbolTable()
    {
      return _method.isAnnotationPresent(UsesSymbolTable.class);
    }

    /**
     * Returns true if the function modifies the local symbol table.
     *
     * i.e. if it can replace a Var with a new Var
     */
    @Override
    public boolean isCallReplacesSymbolTable()
    {
      UsesSymbolTable uses = _method.getAnnotation(UsesSymbolTable.class);

      return uses != null && uses.replace();
    }

    /**
     * True if the return type is void.
     */
    @Override
    public boolean isVoidReturn()
    {
      return _method.getReturnType().equals(void.class);
    }

    /**
     * Analyzes the arguments for read-only and reference.
     */
    @Override
    public void analyzeArguments(Expr []args, AnalyzeInfo info)
    {
      init();

      int env = getHasEnv() ? 1 : 0;

      Marshal []marshalArgs = getMarshalArgs();
      for (int i = 0; i < args.length; i++) {
        ExprPro arg = (ExprPro) args[i];

        if (marshalArgs.length <= i) {
          // XXX: not quite true
          arg.getGenerator().analyzeSetModified(info);
          arg.getGenerator().analyzeSetReference(info);

          continue;
        }

        Marshal marshal = marshalArgs[i];
        Annotation [][]paramAnn = getParamAnn();

        if (isReadOnly(paramAnn[i + env]) || marshal.isReadOnly()) {
        }
        else if (marshal.isReference()) {
          arg.getGenerator().analyzeSetModified(info);
          arg.getGenerator().analyzeSetReference(info);
        }
        else {
          // possibly modified, but not reference
          arg.getGenerator().analyzeSetModified(info);
        }
      }
    }

    private boolean isReadOnly(Annotation []ann)
    {
      for (int i = 0; i < ann.length; i++) {
        if (ReadOnly.class.isAssignableFrom(ann[i].annotationType()))
          return true;
      }

      return false;
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out,
                         ExprGenerator funExpr,
                         Expr []args)
      throws IOException
    {
      init();

      if (out.isProfile()) {
        generateProfileImpl(out, funExpr, args);
        return;
      }

      ProMarshal unmarshalReturn = (ProMarshal) getUnmarshalReturn();

      unmarshalReturn.generateResultStart(out);

      generateImpl(out, funExpr, args);

      unmarshalReturn.generateResultEnd(out);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateBoolean(PhpWriter out,
                                ExprGenerator funExpr,
                                Expr []args)
      throws IOException
    {
      init();

      if (out.isProfile()) {
        super.generateBoolean(out, funExpr, args);
      }
      else if (isBoolean())
        generateImpl(out, funExpr, args);
      else if (isLong() || isDouble()) {
        out.print("(");
        generateImpl(out, funExpr, args);
        out.print(" != 0)");
      }
      else
        super.generateBoolean(out, funExpr, args);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateLong(PhpWriter out,
                             ExprGenerator funExpr,
                             Expr []args)
      throws IOException
    {
      init();

      if (out.isProfile()) {
        super.generateLong(out, funExpr, args);
      }
      else if (isLong())
        generateImpl(out, funExpr, args);
      else
        super.generateLong(out, funExpr, args);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateDouble(PhpWriter out,
                               ExprGenerator funExpr,
                               Expr []args)
      throws IOException
    {
      init();

      if (out.isProfile()) {
        super.generateDouble(out, funExpr, args);
      }
      else if (isLong() || isDouble())
        generateImpl(out, funExpr, args);
      else
        super.generateDouble(out, funExpr, args);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateString(PhpWriter out,
                               ExprGenerator funExpr,
                               Expr []args)
      throws IOException
    {
      init();

      if (out.isProfile()) {
        super.generateDouble(out, funExpr, args);
      }
      else if (isString())
        generateImpl(out, funExpr, args);
      else
        super.generateString(out, funExpr, args);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateTop(PhpWriter out,
                            ExprGenerator funExpr,
                            Expr []args)
      throws IOException
    {
      init();

      if (out.isProfile())
        generateProfileImpl(out, funExpr, args);
      else
        generateImpl(out, funExpr, args);
    }

    /**
     * Generates code to evaluate as a double expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateCopy(PhpWriter out,
                             ExprGenerator funExpr,
                             Expr []args)
      throws IOException
    {
      init();

      generate(out, funExpr, args);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    private void generateImpl(PhpWriter out,
                              ExprGenerator funExpr,
                              Expr []args)
      throws IOException
    {
      init();

      String var = out.addModule(_quercusModule);

      if (out.isProfile()) {
        throw new IllegalStateException();
      }

      if (Modifier.isStatic(_method.getModifiers())) {
        String className = _method.getDeclaringClass().getName();
        if (className.startsWith("com.caucho.quercus.lib.")
            && className.indexOf('.', "com.caucho.quercus.lib.".length()) < 0) {
          out.print(className.substring("com.caucho.quercus.lib.".length()));
        } else
          out.print(className);
      } else
        out.print(var);

      out.print("." + _method.getName() + "(");

      Class<?> []param = _method.getParameterTypes();
      Marshal []marshalArgs = getMarshalArgs();

      boolean isFirst = true;

      boolean hasEnv = getHasEnv();

      if (hasEnv) {
        out.print("env");
        isFirst = false;
      }

      Expr []defaultExprs = getDefaultExprs();
      for (int i = 0; i < defaultExprs.length; i++) {
        ProMarshal marshalArg = (ProMarshal) marshalArgs[i];

        if (! isFirst)
          out.print(", ");
        isFirst = false;

        if (i < args.length) {
          ExprPro expr = (ExprPro) args[i];

          marshalArg.generate(out, expr.getGenerator(),
                              param[hasEnv ? i + 1 : i]);
        }
        else if (defaultExprs[i] != null) {
          ExprPro expr = (ExprPro) defaultExprs[i];

          // php/114r
          if (marshalArg instanceof ProReferenceMarshal) {
            ProReferenceMarshal refArg = (ProReferenceMarshal) marshalArg;

            refArg.generateOptional(out,
                                    expr.getGenerator(),
                                    param[hasEnv ? i + 1 : i]);
          }
          else {
            marshalArg.generate(out,
                                expr.getGenerator(),
                                param[hasEnv ? i + 1 : i]);
          }
        }
        else if (! getHasRestArgs()) {
          String msg = L.l("argument length mismatch for '{0}'",
              _method.getName());

          log.warning(funExpr.getLocation().getMessagePrefix() + msg);
          out.print("env.warning(\"" + msg + "\") != null ? ");

          ExprPro expr = (ExprPro) LiteralNullExprPro.NULL;

          marshalArg.generate(out,
              expr.getGenerator(),
              param[hasEnv ? i + 1 : i]);

          out.print(':');

          marshalArg.generate(out,
              expr.getGenerator(),
              param[hasEnv ? i + 1 : i]);

        }
      }

      if (! getHasRestArgs()) {
      }
      else if (marshalArgs.length == args.length) {
        if (! isFirst)
          out.print(", ");
        isFirst = false;

        out.print("Value.NULL_ARGS");
      }
      else {
        if (! isFirst)
          out.print(", ");
        isFirst = false;

        out.print("new Value[] {");

        for (int i = marshalArgs.length; i < args.length; i++) {
          ExprPro arg = (ExprPro) args[i];

          if (isRestReference())
            arg.getGenerator().generateRef(out);
          else {
            arg.getGenerator().generate(out);
            // php/3c1r
            out.print(".toValue()");
          }

          out.print(", ");
        }
        out.print("}");
      }

      out.print(")");
    }

    /**
     * Generates code to evaluate the expression for profiling code.
     *
     * @param out the writer to the Java source code.
     */
    private void generateProfileImpl(PhpWriter out,
                                     ExprGenerator funExpr,
                                     Expr []args)
      throws IOException
    {
      String ref = ""; // XXX:

      /*
         if (_isReturnsReference)
         ref = "Ref";
         else
         ref = "";
         */

      String funName = ProStaticFunction.this.getName();

      out.print("env._fun[" + out.addFunctionId(funName) + "]");

      out.print(".call" + ref + "(env");

      boolean isVarArgs = args.length > 5;

      if (isVarArgs)
        out.print(", new Value[] {");

      Marshal []marshalArgs = getMarshalArgs();

      for (int i = 0; i < args.length; i++) {
        ExprPro arg = (ExprPro) args[i];

        if (i != 0 || ! isVarArgs)
          out.print(", ");

        if (i < marshalArgs.length) {
          if (marshalArgs[i].isReference())
            arg.getGenerator().generateRef(out);
          else
            arg.getGenerator().generate(out);
        }
        else
          arg.getGenerator().generate(out);
      }

      if (isVarArgs)
        out.print("}");

      out.print(")");
    }

    @Override
    public String toString()
    {
      return ProStaticFunction.this.getClass().getSimpleName() + "$Gen[" + ProStaticFunction.this + "]";
    }
  };
}
