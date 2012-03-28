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

abstract public class ProExpectMarshal extends ExpectMarshal
  implements ProMarshal
{
  public static final Marshal MARSHAL_EXPECT_STRING
    = new ProExpectStringMarshal(Type.STRING);
  public static final Marshal MARSHAL_EXPECT_NUMERIC
    = new ProExpectNumericMarshal(Type.NUMERIC);
  public static final Marshal MARSHAL_EXPECT_BOOLEAN
    = new ProExpectBooleanMarshal(Type.BOOLEAN);

  public ProExpectMarshal(Type type)
  {
    super(type);
  }

  abstract protected void generateExpected(PhpWriter out, ExprGenerator expr)
    throws IOException;

  public void generate(PhpWriter out, ExprGenerator expr, Class argClass)
    throws IOException
  {
    generateExpected(out, expr);
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
    code.pushObjectVar(argIndex);
  }

  public void generateUnmarshal(CodeWriterAttribute code)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  static class ProExpectStringMarshal extends ProExpectMarshal {
    ProExpectStringMarshal(Type type)
    {
      super(type);
    }

    protected void generateExpected(PhpWriter out, ExprGenerator expr)
      throws IOException
    {
      if (expr.isConstant()
          && (expr.isDefault()
              || expr.isString()
              || expr.isLong()
              || expr.isDouble()
              || expr.isBoolean())) {
        expr.generateValue(out);
      }
      else {
        out.print("env.expectString(");
        expr.generateValue(out);
        out.print(")");
      }
    }
  }

  static class ProExpectNumericMarshal extends ProExpectMarshal {
    ProExpectNumericMarshal(Type type)
    {
      super(type);
    }

    protected void generateExpected(PhpWriter out, ExprGenerator expr)
      throws IOException
    {
      if (expr.isConstant()
          && (expr.isDefault()
              || expr.isLong()
              || expr.isDouble())) {
        expr.generateValue(out);
      }
      else {
        out.print("env.expectNumeric(");
        expr.generateValue(out);
        out.print(")");
      }
    }
  }

  static class ProExpectBooleanMarshal extends ProExpectMarshal {
    ProExpectBooleanMarshal(Type type)
    {
      super(type);
    }

    protected void generateExpected(PhpWriter out, ExprGenerator expr)
      throws IOException
    {
      if (expr.isConstant()
          && (expr.isDefault()
              || expr.isBoolean()
              || expr.isString()
              || expr.isLong()
              || expr.isDouble())) {
        expr.generateValue(out);
      }
      else {
        out.print("env.expectBoolean(");
        expr.generateValue(out);
        out.print(")");
      }
    }
  }
}
