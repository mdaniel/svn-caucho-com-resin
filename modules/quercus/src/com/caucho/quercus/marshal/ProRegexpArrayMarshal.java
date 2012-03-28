/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.lib.regexp.RegexpModule;

import java.io.IOException;

public class ProRegexpArrayMarshal extends RegexpArrayMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProRegexpArrayMarshal();
  
  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    if (expr.isConstant()) {
      String literalVar = out.addValue((Value) expr.getConstant());
      
      String var = out.addRegexpArray(literalVar);

      out.print(var);
    }
    else {
      out.print("com.caucho.quercus.lib.regexp.RegexpModule.createRegexpArray(env, ");
      expr.generate(out);
      out.print(")");
    }
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
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
