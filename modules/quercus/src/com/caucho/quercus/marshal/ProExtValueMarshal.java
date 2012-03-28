/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

public class ProExtValueMarshal extends ExtValueMarshal
  implements ProMarshal
{
  public ProExtValueMarshal(Class expectedClass)
  {
    super(expectedClass);
  }
  
  public void generate(PhpWriter out, ExprGenerator expr, Class expectedClass)
    throws IOException
  {
    String className = expectedClass.getName();

    out.print("((");
    out.printClass(expectedClass);
    out.print(") env.cast(");
    out.printClass(expectedClass);
    out.print(".class, ");

    expr.generateValue(out);
    out.print("))");
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    out.print("((Value) ");
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
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
