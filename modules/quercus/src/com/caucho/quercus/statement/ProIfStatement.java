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

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.IfStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;

/**
 * Represents an if statement.
 */
public class ProIfStatement extends IfStatement
  implements CompilingStatement
{
  public ProIfStatement(Location location,
			Expr test,
			Statement trueBlock, Statement falseBlock)
  {
    super(location, test, trueBlock, falseBlock);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProIfStatement.this.getLocation();
      }
      
      protected ExprGenerator getTest()
      {
	return ((ExprPro) ProIfStatement.this.getTest()).getGenerator();
      }
      
      protected StatementGenerator getTrueBlock()
      {
	return ((CompilingStatement) ProIfStatement.this.getTrueBlock()).getGenerator();
      }
      
      protected StatementGenerator getFalseBlock()
      {
	if (ProIfStatement.this.getFalseBlock() != null)
	  return ((CompilingStatement) ProIfStatement.this.getFalseBlock()).getGenerator();
	else
	  return null;
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	getTest().analyze(info);

	AnalyzeInfo trueBlockInfo = info.copy();
	AnalyzeInfo falseBlockInfo = info.copy();

	boolean isComplete = false;

	if (getTrueBlock().analyze(trueBlockInfo)) {
	  info.merge(trueBlockInfo);
	  isComplete = true;
	}

	if (getFalseBlock() != null) {
	  if (getFalseBlock().analyze(falseBlockInfo)) {
	    info.merge(falseBlockInfo);
	    isComplete = true;
	  }
	}
	else
	  isComplete = true;

	return isComplete;
      }

      /**
       * Returns true if the statement can fallthrough.
       */
      @Override
      public int fallThrough()
      {
	if (getFalseBlock() == null) {
	  //return getTrueBlock().fallThrough() & BREAK_FALL_THROUGH;
	  // php/367x
	  return FALL_THROUGH;
	}
	else
	  return (getTrueBlock().fallThrough()
		  & getFalseBlock().fallThrough());
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
	throws IOException
      {
	out.print("if (");
	getTest().generateBoolean(out);
	out.println(") {");
	out.pushDepth();
	getTrueBlock().generate(out);
	out.popDepth();
	out.println("}");

	if (getFalseBlock() != null) {
	  out.println("else {");
	  out.pushDepth();
	  getFalseBlock().generate(out);
	  out.popDepth();
	  out.println("}");
	}
      }
    };
}

