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

public class ProValueMarshal extends ValueMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProValueMarshal(false);
  public static final Marshal MARSHAL_PASS_THRU = new ProValueMarshal(true);
  
  private ProValueMarshal(boolean isPassThru)
  {
    super(isPassThru);
  }
  
  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    // php/3a70 vs php/3783 - see generated code
    // generate vs generateValue
    expr.generateValue(out);
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
    return true;
  }

  public void generateMarshal(CodeWriterAttribute code, int argIndex)
  {
    code.pushObjectVar(argIndex);
  }

  public void generateUnmarshal(CodeWriterAttribute code)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
