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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Represents a PHP list() = each() assignment expression.
 */
public class BinaryAssignListEachExprPro extends BinaryAssignListEachExpr
  implements ExprPro
  {
  private static final L10N L = new L10N(BinaryAssignListEachExprPro.class);

  public BinaryAssignListEachExprPro(ListHeadExpr listHead, Expr each)
  {
    super(listHead, each);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
    /**
     * Analyze the expression
     */
    @Override
    public ExprType analyze(AnalyzeInfo info)
    {
      // XXX: should be unique (?)
      info.getFunction().addTempVar("_quercus_list");

      ExprPro value = (ExprPro) _value;

      value.getGenerator().analyze(info);

      ListHeadExprPro head = (ListHeadExprPro) _listHead;

      head.getListEachGenerator().analyze(info);

      return ExprType.VALUE;
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out)
    throws IOException
    {
      ListHeadExprPro head = (ListHeadExprPro) _listHead;

      head.getListEachGenerator().generateAssign(out, _value, false);
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateStatement(PhpWriter out)
      throws IOException
    {
      ListHeadExprPro head = (ListHeadExprPro) _listHead;

      head.getListEachGenerator().generateListEachStatement(out, _value);
    }
  };
}

