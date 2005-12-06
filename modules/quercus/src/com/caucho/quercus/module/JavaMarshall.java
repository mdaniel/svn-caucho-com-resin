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

package com.caucho.quercus.module;

import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.NullLiteralExpr;
import com.caucho.quercus.expr.DefaultExpr;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.JavaClassDefinition;
import com.caucho.quercus.env.Callback;

import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.vfs.Path;

import com.caucho.util.L10N;

/**
 * Code for marshalling arguments.
 */
public class JavaMarshall extends Marshall {
  private static final L10N L = new L10N(JavaMarshall.class);
  
  private final JavaClassDefinition _def;

  public JavaMarshall(JavaClassDefinition def)
  {
    _def = def;
  }

  public Object marshall(Env env, Expr expr, Class argClass)
    throws Throwable
  {
    Value value = expr.eval(env);

    return marshall(env, value, argClass);
  }  
  
  public Object marshall(Env env, Value value, Class argClass)
    throws Throwable
  {
    Object obj = value.toJavaObject();

    if (obj == null)
      return null;
    else if (! argClass.isAssignableFrom(obj.getClass()))
      env.error(L.l("Can't assign to {0}", argClass));
    
    return obj;
  }
  
  public void generate(PhpWriter out, Expr expr, Class argClass)
    throws IOException
  {
    out.print("(" + argClass.getName() + ") ");
    expr.generate(out);
    out.print(".toJavaObject()");

  }
    
  public Value unmarshall(Env env, Object value)
    throws Throwable
  {
    return new JavaValue(value, _def);
  }
    
  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    out.print("env.wrapJava(");
  }
}

