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
 * Code for marshaling arguments.
 */
public class ProJavaListMarshal extends JavaListMarshal
  implements ProMarshal
{
  private static final L10N L = new L10N(JavaMarshal.class);

  public ProJavaListMarshal(JavaClassDef def,
                      boolean isNotNull,
                      boolean isUnmarshalNullAsFalse)
  {
    super(def, isNotNull, isUnmarshalNullAsFalse);
  }

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    out.print("(" + argClass.getName() + ") ");
    expr.generate(out);
    out.print(".toJavaList(env, " + argClass.getName() + ".class)");
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

