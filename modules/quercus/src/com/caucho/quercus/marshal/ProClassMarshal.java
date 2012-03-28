/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

public class ProClassMarshal extends ClassMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProClassMarshal();

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    out.print(ProClassMarshal.class.getName());
    out.print(".toClass(env, ");
    expr.generate(out);
    out.print(")");
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    out.print("env.wrapJava(");
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
    out.print(")");
  }

  public static Class toClass(Env env, Value value)
  {
    Object obj = value.toJavaObject();

    if (obj == null)
      return null;
    else if (obj instanceof Class)
      return ((Class) obj);

    String className = value.toJavaString();

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      return Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      env.warning(e);

      return null;
    }
  }

  public boolean isByteCodeGenerator()
  {
    return false;
  }

  public void generateMarshal(CodeWriterAttribute code, int argIndex)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void generateUnmarshal(CodeWriterAttribute code)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
