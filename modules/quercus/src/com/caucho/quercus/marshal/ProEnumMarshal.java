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
public class ProEnumMarshal extends EnumMarshal
  implements ProMarshal
{
  private static final L10N L = new L10N(ProEnumMarshal.class);

  public ProEnumMarshal(Class enumType,
			boolean isNotNull,
			boolean isNullAsFalse)
  {
    super(enumType);
  }

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    out.print("Enum.valueOf(" + argClass.getCanonicalName() + ".class, ");
    expr.generateString(out);
    out.print(")");
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    if (out.getPhp().isUnicodeSemantics())
      out.print("UnicodeBuilderValue.create(String.valueOf(");
    else
      out.print("StringBuilderValue.create(String.valueOf(");
  }

  public void generateResultEnd(PhpWriter out)
    throws IOException
  {
    out.print("))");
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

