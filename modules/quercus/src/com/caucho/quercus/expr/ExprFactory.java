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

package com.caucho.quercus.expr;

import java.io.IOException;

import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.*;

import com.caucho.quercus.parser.QuercusParser;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.util.L10N;

/**
 * Factory for creating PHP expressions and statements
 */
public class ExprFactory {
  private static final L10N L = new L10N(ExprFactory.class);
  private static final Logger log
    = Logger.getLogger(ExprFactory.class.getName());

  protected ExprFactory()
  {
  }

  public static ExprFactory create()
  {
    try {
      Class cl = Class.forName("com.caucho.quercus.expr.ProExprFactory");

      return (ExprFactory) cl.newInstance();
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);

      return new ExprFactory();
    }
  }

  /**
   * Creates a null literal expression.
   */
  public Expr createNull()
  {
    return NullLiteralExpr.NULL;
  }

  /**
   * Creates a string literal expression.
   */
  public Expr createString(String lexeme)
  {
    return new StringLiteralExpr(lexeme);
  }

  /**
   * Creates a string literal expression.
   */
  public Expr createLong(int lexeme)
  {
    return new LongLiteralExpr(lexeme);
  }

  /**
   * Creates a string literal expression.
   */
  public Expr createLiteral(Value literal)
  {
    return new LiteralExpr(literal);
  }

  /**
   * Creates a var expression.
   */
  public VarExpr createVar(VarInfo var)
  {
    return new VarExpr(var);
  }

  /**
   * Creates an array get 'a[0]' expression.
   */
  public ArrayGetExpr createArrayGet(Expr base, Expr index)
  {
    return new ArrayGetExpr(base, index);
  }

  /**
   * Creates a post increment 'a++' expression.
   */
  public PostIncrementExpr createPostIncrement(Expr expr, int incr)
  {
    return new PostIncrementExpr(expr, incr);
  }

  /**
   * Creates an addition expression.
   */
  public Expr createAdd(Expr left, Expr right)
  {
    return new AddExpr(left, right);
  }

  /**
   * Creates a subtraction expression.
   */
  public Expr createSub(Expr left, Expr right)
  {
    return new SubExpr(left, right);
  }

  /**
   * Creates a multiplication expression.
   */
  public Expr createMul(Expr left, Expr right)
  {
    return new MulExpr(left, right);
  }

  /**
   * Creates a division expression.
   */
  public Expr createDiv(Expr left, Expr right)
  {
    return new DivExpr(left, right);
  }

  /**
   * Creates a modulo expression.
   */
  public Expr createMod(Expr left, Expr right)
  {
    return new ModExpr(left, right);
  }

  /**
   * Creates a left-shift expression.
   */
  public Expr createLeftShift(Expr left, Expr right)
  {
    return new LeftShiftExpr(left, right);
  }

  /**
   * Creates a right-shift expression.
   */
  public Expr createRightShift(Expr left, Expr right)
  {
    return new RightShiftExpr(left, right);
  }

  /**
   * Creates a bit-and expression.
   */
  public Expr createBitAnd(Expr left, Expr right)
  {
    return new BitAndExpr(left, right);
  }

  /**
   * Creates a bit-or expression.
   */
  public Expr createBitOr(Expr left, Expr right)
  {
    return new BitOrExpr(left, right);
  }

  /**
   * Creates a bit-xor expression.
   */
  public Expr createBitXor(Expr left, Expr right)
  {
    return new BitXorExpr(left, right);
  }

  /**
   * Creates an append expression
   */
  public final Expr createAppend(Expr left, Expr right)
  {
    AppendExpr leftAppend;

    // XXX: i18n binary vs unicode issues
    /*
    if (left instanceof ToStringExpr)
      left = ((ToStringExpr) left).getExpr();

    if (left instanceof StringLiteralExpr) {
      StringLiteralExpr string = (StringLiteralExpr) left;

      if (string.evalConstant().length() == 0)
	return ToStringExpr.create(right);
    }
    */

    if (left instanceof AppendExpr)
      leftAppend = (AppendExpr) left;
    else
      leftAppend = createAppendImpl(left, null);
    
    AppendExpr next;

    /*
    if (right instanceof ToStringExpr)
      right = ((ToStringExpr) right).getExpr();

    if (right instanceof StringLiteralExpr) {
      StringLiteralExpr string = (StringLiteralExpr) right;

      if (string.evalConstant().length() == 0)
	return ToStringExpr.create(left);
    }
    */

    if (right instanceof AppendExpr)
      next = (AppendExpr) right;
    else
      next = createAppendImpl(right, null);

    AppendExpr result = append(leftAppend, next);

    if (result.getNext() != null)
      return result;
    else
      return result.getValue();
  }

  /**
   * Appends the tail to the current expression, combining
   * constant literals.
   */
  private AppendExpr append(AppendExpr left, AppendExpr tail)
  {
    if (left == null)
      return tail;

    tail = append(left.getNext(), tail);

    if (false
	&& left.getValue() instanceof StringLiteralExpr
	&& tail.getValue() instanceof StringLiteralExpr) {
      StringLiteralExpr leftString = (StringLiteralExpr) left.getValue();
      StringLiteralExpr rightString = (StringLiteralExpr) tail.getValue();

      Expr value = createString(leftString.evalConstant().toString()
				+ rightString.evalConstant().toString());

      return createAppendImpl(value, tail.getNext());
    }
    else {
      left.setNext(tail);

      return left;
    }
  }
  
  protected AppendExpr createAppendImpl(Expr left, AppendExpr right)
  {
    return new AppendExpr(left, right);
  }

  /**
   * Creates a lt expression.
   */
  public Expr createLt(Expr left, Expr right)
  {
    return new LtExpr(left, right);
  }

  /**
   * Creates a leq expression.
   */
  public Expr createLeq(Expr left, Expr right)
  {
    return new LeqExpr(left, right);
  }

  /**
   * Creates a gt expression.
   */
  public Expr createGt(Expr left, Expr right)
  {
    return new GtExpr(left, right);
  }

  /**
   * Creates a geq expression.
   */
  public Expr createGeq(Expr left, Expr right)
  {
    return new GeqExpr(left, right);
  }

  /**
   * Creates an eq expression.
   */
  public Expr createEq(Expr left, Expr right)
  {
    return new EqExpr(left, right);
  }

  /**
   * Creates a neq expression.
   */
  public Expr createNeq(Expr left, Expr right)
  {
    return new NeqExpr(left, right);
  }

  /**
   * Creates an equals expression.
   */
  public Expr createEquals(Expr left, Expr right)
  {
    return new EqualsExpr(left, right);
  }

  /**
   * Creates an assignment expression.
   */
  public Expr createAssign(AbstractVarExpr left, Expr right)
  {
    return new AssignExpr(left, right);
  }

  /**
   * Creates an or expression.
   */
  public Expr createOr(Expr left, Expr right)
  {
    return new OrExpr(left, right);
  }

  /**
   * Creates an and expression.
   */
  public Expr createAnd(Expr left, Expr right)
  {
    return new AndExpr(left, right);
  }

  /**
   * Creates an conditional expression.
   */
  public Expr createConditional(Expr test, Expr left, Expr right)
  {
    return new ConditionalExpr(test, left, right);
  }

  /**
   * Creates a new function call.
   */
  public FunctionExpr createFunction(Location loc,
				     String name,
				     ArrayList<Expr> args)
  {
    return new FunctionExpr(loc, name, args);
  }

  /**
   * Creates an echo statement
   */
  public Statement createEcho(Location loc, Expr expr)
  {
    return new EchoStatement(loc, expr);
  }

  /**
   * Creates an expr statement
   */
  public Statement createExpr(Location loc, Expr expr)
  {
    return new ExprStatement(loc, expr);
  }

  public final Statement createBlock(Location loc,
				     ArrayList<Statement> statementList)
  {
    if (statementList.size() == 1)
      return statementList.get(0);

    Statement []statements = new Statement[statementList.size()];

    statementList.toArray(statements);

    return createBlockImpl(loc, statements);
  }

  public final Statement createBlock(Location loc, Statement []statementList)
  {
    if (statementList.length == 1)
      return statementList[0];

    Statement []statements = new Statement[statementList.length];

    System.arraycopy(statementList, 0, statements, 0, statementList.length);

    return createBlockImpl(loc, statements);
  }

  /**
   * Creates an expr statement
   */
  public final BlockStatement createBlockImpl(Location loc,
					      ArrayList<Statement> statementList)
  {
    Statement []statements = new Statement[statementList.size()];

    statementList.toArray(statements);
    
    return createBlockImpl(loc, statements);
  }

  /**
   * Creates an expr statement
   */
  public BlockStatement createBlockImpl(Location loc, Statement []statements)
  {
    return new BlockStatement(loc, statements);
  }

  /**
   * Creates a text statement
   */
  public Statement createText(Location loc, String text)
  {
    return new TextStatement(loc, text);
  }

  /**
   * Creates an if statement
   */
  public Statement createIf(Location loc,
			    Expr test,
			    Statement trueBlock,
			    Statement falseBlock)
  {
    return new IfStatement(loc, test, trueBlock, falseBlock);
  }

  /**
   * Creates a for statement
   */
  public Statement createFor(Location loc,
			     Expr init,
			     Expr test,
			     Expr incr,
			     Statement block)
  {
    return new ForStatement(loc, init, test, incr, block);
  }

  /**
   * Creates a return statement
   */
  public Statement createReturn(Location loc,
				Expr value)
  {
    return new ReturnStatement(loc, value);
  }

  /**
   * Creates a new function definition.
   */
  public Function createFunction(Location loc,
				 String name,
				 FunctionInfo info,
				 ArrayList<Arg> argList,
				 ArrayList<Statement> statementList)
  {
    return new Function(this, loc, name, info, argList, statementList);
  }

  /**
   * Creates a new object method definition.
   */
  public ObjectMethod createObjectMethod(Location loc,
					 InterpretedClassDef cl,
					 String name,
					 FunctionInfo info,
					 ArrayList<Arg> argList,
					 ArrayList<Statement> statementList)
  {
    return new ObjectMethod(this, loc, cl, name, info, argList, statementList);
  }

  /**
   * Creates a new object method definition.
   */
  public Function createMethodDeclaration(Location loc,
					 InterpretedClassDef cl,
					 String name,
					 FunctionInfo info,
					 ArrayList<Arg> argList)
  {
    return new MethodDeclaration(this, loc, cl, name, info, argList);
  }
}

