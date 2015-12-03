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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xpath.pattern;

import com.caucho.xpath.ExprEnvironment;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Represents a match with the document root.
 */
public class FromRoot extends Axis {
  public FromRoot()
  {
    super(null);
  }

  /**
   * Matches the owner document.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true if the node is the owner document.
   */
  public boolean match(Node node, ExprEnvironment env)
  {
    return (node instanceof Document);
  }

  /**
   * Returns the first node in the selection order.
   *
   * @param node the current node
   *
   * @return the first node
   */
  public Node firstNode(Node node, ExprEnvironment env)
  {
    Node doc = node.getOwnerDocument();

    if (doc != null)
      return doc;
    else
      return node;
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param lastNode the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node lastNode)
  {
    return null;
  }

  /**
   * The root is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    return true;
  }
  
  /**
   * Returns true if the pattern selects a single node
   */
  boolean isSingleSelect()
  {
    return true;
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    return b instanceof FromRoot;
  }

  public String toString()
  {
    return "/";
  }
}
