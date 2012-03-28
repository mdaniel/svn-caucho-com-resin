/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Sam
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.expr.ExprGenerator;

import java.io.IOException;

public class ProLongValueMarshal
  extends LongValueMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProLongValueMarshal();

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    expr.generateValue(out);
    out.print(".toLongValue()");
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
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
