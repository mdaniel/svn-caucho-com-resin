/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.lib.regexp.RegexpModule;

import java.io.IOException;

public class ProRegexpMarshal extends RegexpMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL = new ProRegexpMarshal();
  
  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    if (expr.isLiteral()) {
      String literalVar = out.addValue((Value) expr.getLiteral());

      String var = out.addRegexp(literalVar);

      out.print(var);
    }
    else {
      String var = out.addRegexpWrapper();
      
      out.print(var);
      out.print(".get(env, ");
      expr.generateStringValue(out);
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
