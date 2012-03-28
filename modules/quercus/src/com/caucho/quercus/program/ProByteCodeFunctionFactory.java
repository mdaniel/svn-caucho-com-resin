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

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaMethod;
import com.caucho.loader.ProxyClassLoader;
import com.caucho.quercus.annotation.*;
import com.caucho.quercus.expr.*;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.ProMarshal;
import com.caucho.quercus.marshal.ProReferenceMarshal;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.module.StaticFunction;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

/**
 * Represents the introspected static function information.
 */
public class ProByteCodeFunctionFactory
{
  private static final L10N L = new L10N(ProByteCodeFunctionFactory.class);
  private static final Logger log
    = Logger.getLogger(ProByteCodeFunctionFactory.class.getName());

  private ModuleContext _context;

  public ProByteCodeFunctionFactory(ModuleContext context)
  {
    _context = context;
  }

  public ProStaticFunction create(QuercusModule module, Method method)
  {
    ProStaticFunction fun = new ProStaticFunction(_context, module, method);

    if (isByteCodeGenerator(method, fun))
      return enhanceByteCode(module, method, fun);

    return fun;
  }

  private boolean isByteCodeGenerator(Method method, ProStaticFunction fun)
  {
    if (! Modifier.isStatic(method.getModifiers())) {
      return false;
    }

    if (fun.getHasRestArgs())
      return false;

    if (method.getParameterTypes().length > 5)
      return false;

    for (Marshal marshal : fun.getMarshalArgs()) {
      ProMarshal proMarshal = (ProMarshal) marshal;

      if (! proMarshal.isByteCodeGenerator())
        return false;
    }

    if (! Value.class.isAssignableFrom(method.getReturnType())) 
      return false;
    
    if (method.isAnnotationPresent(ReturnNullAsFalse.class))
      return false;

    return true;
  }

  private ProStaticFunction enhanceByteCode(QuercusModule module, 
                                            Method method,
                                            ProStaticFunction fun)
  {
    WriteStream out = null;

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      out = Vfs.openWrite(bos);

      JavaClass javaClass = new JavaClass();

      javaClass.setAccessFlags(Modifier.PUBLIC);

      javaClass.setWrite(true);

      javaClass.setMajor(49);
      javaClass.setMinor(0);

      javaClass.setThisClass("_quercus/" + method.getName());

      int argLength = method.getParameterTypes().length;

      if (fun.getHasEnv())
        argLength -= 1;

      String superClass = "com/caucho/quercus/function/CompiledFunction_" 
                          + argLength;

      javaClass.setSuperClass(superClass);

      createConstructor(javaClass, superClass, argLength);
      createCall(javaClass, method, argLength, fun);

      javaClass.write(out);

      out.close();

      byte []buffer = bos.toByteArray();

      String cleanName = "_quercus." + method.getName();
      Class proxyClass = new ProxyClassLoader().loadClass(cleanName, buffer);
      Constructor proxyCtor = proxyClass.getConstructors()[0];    

      Object[] args = new Object[argLength + 1];
      args[0] = method.getName();

      for (int i = 1; i < args.length; i++)
        args[i] = ParamDefaultExpr.DEFAULT;

      AbstractFunction function 
        = (AbstractFunction) proxyCtor.newInstance(args);

      return new ProByteCodeStaticFunction(_context, module, method, function);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
    finally {
      IoUtil.close(out);
    }

    return new ProStaticFunction(_context, module, method);
  }

  private void createConstructor(JavaClass javaClass, 
                                 String superClass,
                                 int argLength)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("(Ljava/lang/String;");

    for (int i = 0; i < argLength; i++) {
      sb.append("Lcom/caucho/quercus/expr/Expr;");
    }

    sb.append(")V");

    String sig = sb.toString();
    
    JavaMethod ctor = javaClass.createMethod("<init>", sig);
    ctor.setAccessFlags(Modifier.PUBLIC);

    CodeWriterAttribute code = ctor.createCodeWriter();
    code.setMaxLocals(argLength + 2);
    code.setMaxStack(argLength + 3);

    code.pushObjectVar(0);
    code.pushObjectVar(1);

    for (int i = 0; i < argLength; i++) {
      code.pushObjectVar(i + 2);
    }

    code.invokespecial(superClass, "<init>", sig, argLength + 2, 0);
    code.addReturn();
    code.close();
  }

  private void createCall(JavaClass javaClass, 
                          Method method, 
                          int argLength,
                          ProStaticFunction fun)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("(Lcom/caucho/quercus/env/Env;");

    for (int i = 0; i < argLength; i++) {
      sb.append("Lcom/caucho/quercus/env/Value;");
    }

    sb.append(")Lcom/caucho/quercus/env/Value;");

    String callSig = sb.toString();

    JavaMethod ctor = javaClass.createMethod("call", callSig);
    ctor.setAccessFlags(Modifier.PUBLIC);

    CodeWriterAttribute code = ctor.createCodeWriter();
    code.setMaxLocals(argLength + 2);
    code.setMaxStack(argLength + 3);

    String className = method.getDeclaringClass().getName();
    className = className.replace('.', '/'); 

    sb = new StringBuilder();

    sb.append('(');

    Class []parameterTypes = method.getParameterTypes();

    for (int i = 0; i < parameterTypes.length; i++) {
      sb.append('L');
      sb.append(parameterTypes[i].getName().replace('.', '/'));
      sb.append(';');
    }

    sb.append(')');
    sb.append('L');
    sb.append(method.getReturnType().getName().replace('.', '/'));
    sb.append(';');

    String sig = sb.toString();

    Marshal []marshals = fun.getMarshalArgs();

    if (fun.getHasEnv())
      code.pushObjectVar(1);

    try {
      for (int i = 0; i < argLength; i++) {
        ProMarshal proMarshal = (ProMarshal) marshals[i];

        proMarshal.generateMarshal(code, i + 2);

        // code.pushObjectVar(i + 2);
      }
    }
    catch (Throwable e) {
      System.out.println("method = " + method);
      e.printStackTrace();
    }

    code.invokestatic(className, method.getName(), sig, argLength, 0);
                      
    code.addObjectReturn();
    code.close();
  }
}
