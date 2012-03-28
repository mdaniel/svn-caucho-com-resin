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

public class ProNullAsFalseMarshal extends NullAsFalseMarshal
  implements ProMarshal
{
  public ProNullAsFalseMarshal(Marshal marshal)
  {
    super(marshal);
  }

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    ((ProMarshal) _marshal).generate(out, expr, argClass);
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    out.print("env.nullAsFalse(");
    ((ProMarshal) _marshal).generateResultStart(out);
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
    ((ProMarshal) _marshal).generateResultEnd(out);
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

