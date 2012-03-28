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

public class ProEregiMarshal extends EregiMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProEregiMarshal();
  
  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    if (expr.isLiteral()) {
      String literalVar = out.addValue((Value) expr.getLiteral());
      
      String var = out.addEregi(literalVar);

      out.print(var);
    }
    else {
      out.print("com.caucho.quercus.lib.regexp.RegexpModule.createEregi(env, ");
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
