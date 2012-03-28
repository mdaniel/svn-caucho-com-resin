/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Code for marshalling arguments.
 */
public class ProJavaMarshal extends JavaMarshal
  implements ProMarshal
{
  private static final L10N L = new L10N(ProJavaMarshal.class);

  public ProJavaMarshal(JavaClassDef def,
                      boolean isNotNull)
  {
    this(def, isNotNull, false);
  }

  public ProJavaMarshal(JavaClassDef def,
                      boolean isNotNull,
                      boolean isUnmarshalNullAsFalse)
  {
    super(def, isNotNull, isUnmarshalNullAsFalse);
  }

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    if (argClass.isArray()) {
      Class<?> componentType = argClass.getComponentType();

      expr.generate(out);
      out.print(".valuesToArray(env, ");
      out.print(componentType.getName());
      out.print(".class)");
    }
    else if (_isNotNull) {
      out.print("(" + argClass.getName() + ") ");
      expr.generate(out);
      out.print(".toJavaObjectNotNull(env, " + argClass.getName() + ".class)");
    } else {
      out.print("(" + argClass.getName() + ") ");
      expr.generate(out);
      out.print(".toJavaObject(env, " + argClass.getName() + ".class)");
    }
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    out.print("env.wrapJava(");
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
    if (_isUnmarshalNullAsFalse)
      out.print(", true");

    out.print(")");
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

