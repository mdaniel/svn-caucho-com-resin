/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xpath.pattern;

import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

import java.util.Iterator;

/**
 * matches if the expression returns a node set and the test-node is
 * contained in that node set.
 *
 * <p>The code interprets Iterators, ArrayLists, and Nodes as node sets.
 */
public class FromExpr extends AbstractPattern {
  private Expr _expr;

  public FromExpr(AbstractPattern parent, Expr expr)
  {
    super(parent);
    
    _expr = expr;
  }

  /**
   * matches if the expression returns a node set and the test-node is
   * contained in that node set.
   *
   * @param node the node to test
   * @param env the variable environment.
   *
   * @return true if the node matches the pattern.
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    NodeIterator iter = _expr.evalNodeSet(node, env);

    while (iter.hasNext()) {
      Node subnode = iter.nextNode();
      
      if (subnode == node)
        return true;
    }

    return false;
  }

  /**
   * Creates a new node iterator.
   *
   * @param node the starting node
   * @param env the variable environment
   * @param match the axis match pattern
   *
   * @return the node iterator
   */
  public NodeIterator createNodeIterator(Node node, ExprEnvironment env,
                                         AbstractPattern match)
    throws XPathException
  {
    return _expr.evalNodeSet(node, env);
  }

  /**
   * The position is the position in the expression node-set.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    Iterator iter = _expr.evalNodeSet(node, env);

    int i = 1;
    while (iter.hasNext()) {
      if (iter.next() == node)
        return i;
      i++;
    }

    return 0;
  }

  /**
   * The count is the size of the expression node-set.
   */
  public int count(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    Iterator iter = _expr.evalNodeSet(node, env);
    int count = 0;

    while (iter.hasNext()) {
      iter.next();
      count++;
    }

    return count;
  }

  public String toString()
  {
    return getPrefix() + "(" + _expr + ")";
  }
}
