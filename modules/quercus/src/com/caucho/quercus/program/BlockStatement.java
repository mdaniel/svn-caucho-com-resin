/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.program;

import java.util.ArrayList;

import java.io.IOException;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents sequence of statements.
 */
public class BlockStatement extends Statement {
  private Statement []_statements;

  BlockStatement(Location location, Statement []statements)
  {
    super(location);

    _statements = statements;
  }

  public BlockStatement(Location location, ArrayList<Statement> statementList)
  {
    super(location);

    _statements = new Statement[statementList.size()];
    statementList.toArray(_statements);
  }

  public static Statement create(Location location, ArrayList<Statement> statementList)
  {
    if (statementList.size() == 1)
      return statementList.get(0);

    Statement []statements = new Statement[statementList.size()];

    statementList.toArray(statements);

    return new BlockStatement(location, statements);
  }

  public static Statement create(Location location, Statement []statementList)
  {
    if (statementList.length == 1)
      return statementList[0];

    Statement []statements = new Statement[statementList.length];

    System.arraycopy(statementList, 0, statements, 0, statementList.length);

    return new BlockStatement(location, statements);
  }

  public BlockStatement append(ArrayList<Statement> statementList)
  {
    Statement []statements
      = new Statement[_statements.length + statementList.size()];

    System.arraycopy(_statements, 0, statements, 0, _statements.length);

    for (int i = 0; i < statementList.size(); i++)
      statements[i + _statements.length] = statementList.get(i);

    return new BlockStatement(getLocation(), statements);
  }

  public Statement []getStatements()
  {
    return _statements;
  }

  public Value execute(Env env)
    throws Throwable
  {
    try {
      for (int i = 0; i < _statements.length; i++) {
        Statement statement = _statements[i];

        Value value = statement.execute(env);

        if (value != null) {
          return value;
        }
      }
    }
    catch (Throwable t) {
      rethrow(t);
    }

    return null;
  }

  //
  // java generation code
  //

  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    for (int i = 0; i < _statements.length; i++) {
      if (! _statements[i].analyze(info))
        return false;
    }

    return true;
  }

  /**
   * Returns true if the statement can fallthrough.
   */
  public int fallThrough()
  {

    for (int i = 0; i < _statements.length; i++) {
      int fallThrough = _statements[i].fallThrough();

      if (fallThrough != FALL_THROUGH)
        return fallThrough;
    }

    return FALL_THROUGH;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  protected void generateImpl(PhpWriter out)
    throws IOException
  {
    for (int i = 0; i < _statements.length; i++) {
      _statements[i].generate(out);

      if (_statements[i].fallThrough() != FALL_THROUGH)
        return;
    }
  }

  /**
   * Generates the Java footer code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateCoda(PhpWriter out)
    throws IOException
  {
    try {
      for (int i = 0; i < _statements.length; i++)
        _statements[i].generateCoda(out);
    }
    catch (Throwable t) {
      rethrow(t, IOException.class);
    }
  }

  /**
   * Disassembly.
   */
  public void debug(JavaWriter out)
    throws IOException
  {
    out.println("{");
    out.pushDepth();
    for (int i = 0; i < _statements.length; i++)
      _statements[i].debug(out);
    out.popDepth();
    out.println("}");
  }

}

