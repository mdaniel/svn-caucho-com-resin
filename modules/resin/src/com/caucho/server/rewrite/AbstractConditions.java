/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.rewrite;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import java.util.ArrayList;

abstract public class AbstractConditions
  extends AbstractCondition
{
  private final RewriteDispatch _rewriteDispatch;
  private final ArrayList<Condition> _conditions = new ArrayList<Condition>();

  public AbstractConditions(RewriteDispatch rewriteDispatch)
  {
    _rewriteDispatch = rewriteDispatch;
  }

  private <T extends Condition> T add(T condition)
  {
    _conditions.add(condition);
    return condition;
  }

  public void addCondition(Condition condition)
  {
    add(condition);
  }

  public void addWhen(ConditionConfig condition)
  {
    add(condition.getCondition());
  }

  public void addUnless(ConditionConfig condition)
  {
    NotConditions not = new NotConditions();
    not.addCondition(condition.getCondition());

    add(not);
  }

  public AndConditions createAnd()
  {
    return new AndConditions(_rewriteDispatch);
  }

  public void addAnd(AndConditions and)
  {
    and.init();
    add(and);
  }

  public void addAuthTypeEquals(AuthTypeEqualsCondition authTypeEqualsCondition)
  {
    add(authTypeEqualsCondition);
  }

  public void addAuthTypeExists(AuthTypeEqualsCondition authTypeEqualsCondition)
  {
    add(authTypeEqualsCondition);
  }

  public ExprCondition createExpr()
  {
    return new ExprCondition(this);
  }

  public void addExpr(ExprCondition expr)
  {
    expr.init();
    add(expr);
  }

  public NotConditions createNot()
  {
    return new NotConditions(_rewriteDispatch);
  }

  public void addNot(NotConditions not)
  {
    not.init();
    add(not);
  }

  public OrConditions createOr()
  {
    return new OrConditions(_rewriteDispatch);
  }

  public void addOr(OrConditions or)
  {
    or.init();
    add(or);
  }

  protected ArrayList<Condition> getConditions()
  {
    return _conditions;
  }

  @PostConstruct
  public void init()
  {
    _conditions.trimToSize();
  }

  ELContext getParseContext()
  {
    return _rewriteDispatch.getParseContext();
  }
}
