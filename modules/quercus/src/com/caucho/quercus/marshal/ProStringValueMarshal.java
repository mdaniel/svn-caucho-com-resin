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

public class ProStringValueMarshal extends StringValueMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProStringValueMarshal();
  
  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    expr.generateStringValue(out);
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
    code.invoke("com/caucho/quercus/env/Value", 
                "toStringValue", 
                "()Lcom/caucho/quercus/env/StringValue;",
                1,
                1);
  }

  public void generateUnmarshal(CodeWriterAttribute code)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
