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

package com.caucho.quercus.module;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.JavaInvoker;
import com.caucho.quercus.expr.DefaultExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.NullLiteralExpr;
import com.caucho.quercus.expr.RequiredExpr;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
public class StaticFunction extends JavaInvoker {
  private static final L10N L = new L10N(StaticFunction.class);
  private static final Logger log =
    Logger.getLogger(StaticFunction.class.getName());

  private final QuercusModule _quercusModule;
  private final Method _method;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public StaticFunction(ModuleContext moduleContext,
                        QuercusModule quercusModule,
                        Method method)
  {
    super(moduleContext,
          method.getName(),
          method.getParameterTypes(),
          method.getParameterAnnotations(),
          method.getAnnotations(),
          method.getReturnType());

    _method = method;
    _quercusModule = quercusModule;
  }

  /**
   * Returns the owning module object.
   *
   * @return the module object
   */
  public QuercusModule getModule()
  {
    return _quercusModule;
  }

  /**
   * Returns the function's method.
   *
   * @return the reflection method.
   */
  public Method getMethod()
  {
    return _method;
  }

  /**
   * Evalutes the function.
   */
  @Override
  public Object invoke(Object obj, Object []javaArgs)
  {
    try {
      return _method.invoke(_quercusModule, javaArgs);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw QuercusModuleException.create(e.getCause());
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  //
  // Java generation code.
  //

  /**
   * Analyzes the arguments for read-only and reference.
   */
  public void analyzeArguments(Expr []args, AnalyzeInfo info)
  {
    init();

    int env = getHasEnv() ? 1 : 0;

    Marshall []marshallArgs = getMarshallArgs();
    for (int i = 0; i < args.length; i++) {
      if (marshallArgs.length <= i) {
        // XXX: not quite true
        args[i].analyzeSetModified(info);
        args[i].analyzeSetReference(info);

        continue;
      }

      Marshall marshall = marshallArgs[i];
      Annotation [][]paramAnn = getParamAnn();

      if (isReadOnly(paramAnn[i + env]) || marshall.isReadOnly()) {
      }
      else if (marshall.isReference()) {
        args[i].analyzeSetModified(info);
        args[i].analyzeSetReference(info);
      }
      else {
        // possibly modified, but not reference
        args[i].analyzeSetModified(info);
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
  public void generate(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    getUnmarshallReturn().generateResultStart(out);

    generateImpl(out, funExpr, args);

    getUnmarshallReturn().generateResultEnd(out);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    if (isBoolean())
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
  public void generateLong(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    if (isLong())
      generateImpl(out, funExpr, args);
    else
      super.generateLong(out, funExpr, args);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    if (isLong() || isDouble())
      generateImpl(out, funExpr, args);
    else
      super.generateDouble(out, funExpr, args);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    if (isString())
      generateImpl(out, funExpr, args);
    else
      super.generateString(out, funExpr, args);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    generateImpl(out, funExpr, args);
  }

  /**
   * Generates code to evaluate as a double expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out, Expr funExpr, Expr []args)
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
  private void generateImpl(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    init();

    String var = out.addModule(_quercusModule);

    if (Modifier.isStatic(_method.getModifiers())) {
      String className = _method.getDeclaringClass().getName();

      if (className.startsWith("com.caucho.quercus.lib.") &&
          className.indexOf('.', "com.caucho.quercus.lib.".length()) < 0) {
        out.print(className.substring("com.caucho.quercus.lib.".length()));
      } else
        out.print(className);
    } else
      out.print(var);

    out.print("." + _method.getName() + "(");

    Class []param = _method.getParameterTypes();
    Marshall []marshallArgs = getMarshallArgs();

    boolean isFirst = true;

    boolean hasEnv = getHasEnv();

    if (hasEnv) {
      out.print("env");
      isFirst = false;
    }

    Expr []defaultExprs = getDefaultExprs();
    for (int i = 0; i < defaultExprs.length; i++) {
      if (! isFirst)
        out.print(", ");
      isFirst = false;

      if (i < args.length)
        marshallArgs[i].generate(out, args[i], param[hasEnv ? i + 1 : i]);
      else if (defaultExprs[i] != null)
        marshallArgs[i].generate(out,
                                 defaultExprs[i],
                                 param[hasEnv ? i + 1 : i]);
      else if (! getHasRestArgs()) {
        // XXX: error?
        log.warning(L.l(funExpr.getLocation().getMessagePrefix() +
                        "argument length mismatch for '{0}'",
                        _method.getName()));
        marshallArgs[i].generate(out,
                                 RequiredExpr.REQUIRED,
                                 param[hasEnv ? i + 1 : i]);
      }
    }

    if (getHasRestArgs()) {
      if (! isFirst)
        out.print(", ");
      isFirst = false;

      out.print("new Value[] {");

      for (int i = marshallArgs.length; i < args.length; i++) {
        if (isRestReference())
          args[i].generateRef(out);
        else {
          args[i].generate(out);
          // php/3c1r
          out.print(".toValue()");
        }

        out.print(", ");
      }
      out.print("}");
    }

    out.print(")");
  }

  public String toString()
  {
    return "StaticFunction[" + _method + "]";
  }
}
