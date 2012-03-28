/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueComponent;
import com.caucho.quercus.env.ConstArrayValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents the array function
 */
public class FunArrayExprPro extends FunArrayExpr
  implements ExprPro
{
  private static final L10N L = new L10N(FunArrayExprPro.class);

  private final boolean _isConstant;

  public FunArrayExprPro(ArrayList<Expr> keyList, ArrayList<Expr> valueList)
  {
    super(keyList, valueList);

    _isConstant = isConstant();
  }

  public FunArrayExprPro(Expr []keys, Expr []values)
  {
    super(keys, values);

    _isConstant = isConstant();
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  /**
   * Evaluates the expression as a constant.
   *
   * @return the expression value.
   */
  @Override
  public Value evalConstant()
  {
    if (_isConstant) {
      Value []keys = new Value[_keys.length];
      Value []values = new Value[_values.length];

      try {
        for (int i = 0; i < _keys.length; i++) {
          if (_keys[i] != null)
            keys[i] = _keys[i].eval(null);

          values[i] = _values[i].eval(null);;
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return new ConstArrayValue(keys, values);
    }
    else
      throw new IllegalStateException(L.l("{0} is not a constant expression", this));
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        for (int i = 0; i < _values.length; i++) {
          ExprPro key = (ExprPro) _keys[i];

          if (key != null)
            key.getGenerator().analyze(info);

          ExprPro value = (ExprPro) _values[i];
          value.getGenerator().analyze(info);
        }

        return ExprType.VALUE;
      }

      /**
       * Returns true for a constant.
       */
      @Override
      public boolean isConstant()
      {
        return _isConstant;
      }

      /**
       * Returns the constant value
       */
      @Override
      public Object getConstant()
      {
        return evalConstant();
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        // if (_isConstant) {
        if (isConstant() && _keys.length > 1) {
          out.print(out.addValue(evalConstant()));
          out.print(".copy()");
        }

        else if (_keys.length < 16) {
          out.print("new ArrayValueImpl()");

          for (int i = 0; i < _keys.length; i++) {
            ExprPro key = (ExprPro) _keys[i];
            ExprPro value = (ExprPro) _values[i];

            out.print(".");
            out.print("append(");
            if (key != null) {
              key.getGenerator().generateCopy(out);
              out.print(", ");
            }

            value.getGenerator().generateCopy(out);
            out.print(")");
          }
        }
        else if (_keys.length < ArrayValueComponent.MAX_DYNAMIC_SIZE) {
          out.print("new ArrayValueImpl(");
          out.print("new Value[] {");

          for (int i = 0; i < _keys.length; i++) {
            ExprPro key = (ExprPro) _keys[i];

            if (i != 0)
              out.print(", ");

            if (key != null)
              key.getGenerator().generateCopy(out);
            else
              out.print("null");
          }

          out.print("}, new Value[] {");

          for (int i = 0; i < _values.length; i++) {
            ExprPro value = (ExprPro) _values[i];

            if (i != 0)
              out.print(", ");

            value.getGenerator().generateCopy(out);
          }

          out.print("})");
        }
        else {
          out.print("new ArrayValueImpl(env, ");

          generateComponents(out);

          out.print(")");
        }
      }

      private void generateComponents(PhpWriter out)
        throws IOException
      {
        int maxSize = ArrayValueComponent.MAX_DYNAMIC_SIZE;

        int size = _keys.length;
        int bins = size / maxSize;

        if (size % maxSize > 0)
          bins++;

        int bin = 0;

        out.print("new ArrayValueComponent[] {");

        while (bin < bins) {
          int binSize = maxSize;

          if (bin != 0)
            out.print(", ");

          if (bin + 1 == bins)
            binSize = size - bin * maxSize;

          out.println("new ArrayValueComponent() {");
          out.println("    public void init(Env env) {");

          out.print("      _keys = new Value[] {");
          for (int i = 0; i < binSize; i++) {
            if (i != 0)
              out.print(", ");

            ExprPro key = (ExprPro) _keys[i + bin * maxSize];

            if (key != null)
              key.getGenerator().generateCopy(out);
            else
              out.print("null");
          }
          out.println("};");

          out.print("      _values = new Value[] {");
          for (int i = 0; i < binSize; i++) {
            if (i != 0)
              out.print(", ");

            ExprPro value = (ExprPro) _values[i + bin * maxSize];

            if (value != null)
              value.getGenerator().generateCopy(out);
            else
              out.print("null");
          }
          out.println("};");

          out.println("    }");
          out.println("  }");

          bin++;
        }

        out.print("}");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
        throws IOException
      {
        // quercus/3724
        out.print("new FunArrayExpr(");

        out.print("new Expr[] {");

        for (int i = 0; i < _keys.length; i++) {
          ExprPro key = (ExprPro) _keys[i];

          if (i != 0)
            out.print(", ");

          if (key != null)
            key.getGenerator().generateExpr(out);
          else
            out.print("null");
        }

        out.print("}, new Expr[] {");

        for (int i = 0; i < _values.length; i++) {
          ExprPro value = (ExprPro) _values[i];

          if (i != 0)
            out.print(", ");

          value.getGenerator().generateExpr(out);
        }

        out.print("})");
      }
    };
}

