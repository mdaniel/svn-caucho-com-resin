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
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusLanguageException;

import com.caucho.quercus.expr.AbstractVarExpr;
import com.caucho.quercus.expr.RawExpr;
import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents sequence of statements.
 */
public class TryStatement extends Statement {
  private final Statement _block;
  private final ArrayList<Catch> _catchList = new ArrayList<Catch>();

  public TryStatement(Location location, Statement block)
  {
    super(location);

    _block = block;
  }

  public void addCatch(String id, AbstractVarExpr lhs, Statement block)
  {
    _catchList.add(new Catch(id, lhs, block));
  }

  public Value execute(Env env)
  {
    try {
      return _block.execute(env);
    } catch (QuercusLanguageException e) {
      Value value = e.getValue();

      for (int i = 0; i < _catchList.size(); i++) {
	Catch item = _catchList.get(i);

	if (value != null && value.isA(item.getId()) ||
	    value == null && item.getId().equals("Exception")) {
	  if (value != null)
	    item.getExpr().evalAssign(env, value);
	  else
	    item.getExpr().evalAssign(env, NullValue.NULL);
	  
	  return item.getBlock().execute(env);
	}
      }

      throw e;
    }
  }

  //
  // java generation code
  //

  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    AnalyzeInfo topInfo = info.copy();
    
    _block.analyze(info);

    for (int i = 0; i < _catchList.size(); i++) {
      Catch item = _catchList.get(i);
      
      topInfo.clear();

      item.getExpr().analyzeAssign(topInfo);
      item.getBlock().analyze(topInfo);
    }

    info.clear();

    return true;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  protected void generateImpl(PhpWriter out)
    throws IOException
  {
    out.println("try {");
    out.pushDepth();
    
    _block.generate(out);
    
    out.popDepth();

    int id = out.generateId();

    String e = "e" + id;
    
    out.println("} catch (QuercusLanguageException " + e + ") {");
    out.pushDepth();

    String v = "v" + id;
    
    out.println("Value " + v + " = " + e + ".getValue();");
    
    for (int i = 0; i < _catchList.size(); i++) {
      Catch item = _catchList.get(i);
      
      if (i != 0)
	out.print("else ");
      
      out.println("if (" + v + ".isA(\"" + item.getId() + "\")) {");
      out.pushDepth();

      item.getExpr().generateAssign(out, new RawExpr(getLocation(), v), true);
      out.println(";");
      item.getBlock().generate(out);
      
      out.popDepth();
      out.println("}");
    }
    out.println("else");
    out.println("  throw " + e + ";");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Disassembly.
   */
  public void debug(JavaWriter out)
    throws IOException
  {
    _block.debug(out);
  }

  static class Catch {
    private final String _id;
    private final AbstractVarExpr _lhs;
    private final Statement _block;

    Catch(String id, AbstractVarExpr lhs, Statement block)
    {
      _id = id;
      _lhs = lhs;
      _block = block;

      if (id == null)
	throw new NullPointerException();
    }

    public String getId()
    {
      return _id;
    }

    public AbstractVarExpr getExpr()
    {
      return _lhs;
    }

    public Statement getBlock()
    {
      return _block;
    }
  }
}

