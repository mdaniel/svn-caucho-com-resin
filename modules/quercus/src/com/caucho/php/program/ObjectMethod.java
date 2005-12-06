/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.php.program;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.logging.Logger;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;

import com.caucho.php.expr.Expr;
import com.caucho.php.expr.VarExpr;
import com.caucho.php.expr.VarInfo;
import com.caucho.php.expr.VarState;
import com.caucho.php.expr.ExprHandle;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import com.caucho.php.env.Var;
import com.caucho.php.env.NullValue;

import com.caucho.php.gen.PhpWriter;

/**
 * Represents sequence of statements.
 */
public class ObjectMethod extends Function {
  private static final Logger log = Logger.getLogger(ObjectMethod.class.getName());
  private static final L10N L = new L10N(ObjectMethod.class);

  private String _className;

  ObjectMethod(InterpretedClassDef phpClass,
	       String name,
	       FunctionInfo info,
	       Arg []args,
	       Statement []statements)
  {
    super(name, info, args, statements);

    _className = phpClass.getName();
  }

  public ObjectMethod(InterpretedClassDef phpClass,
		      String name,
		      FunctionInfo info,
		      ArrayList<Arg> argList,
		      ArrayList<Statement> statementList)
  {
    super(name, info, argList, statementList);

    _className = phpClass.getName();
  }

  public String getClassName()
  {
    return _className;
  }

  public boolean isObjectMethod()
  {
    return true;
  }
}

