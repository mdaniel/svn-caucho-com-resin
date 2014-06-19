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

import com.caucho.xpath.ExprEnvironment;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Iterates through matching nodes.
 */
public class NodeListIterator extends NodeIterator {
  protected NodeList _list;
  protected int _position;

  public NodeListIterator(ExprEnvironment env, NodeList list)
  {
    super(env);
    
    _list = list;
  }
  
  /**
   * Returns the current position.
   */
  public int getContextPosition()
  {
    return _position;
  }
  
  /**
   * Returns the current position.
   */
  public int getContextSize()
  {
    return _list.getLength();
  }
  
  /**
   * True if there's more data.
   */
  public boolean hasNext()
  {
    return _position < _list.getLength();
  }
  
  /**
   * Returns the next node.
   */
  public Node next()
  {
    return nextNode();
  }
  
  /**
   * Returns the next node.
   */
  public Node nextNode()
  {
    return _list.item(_position++);
  }

  /**
   * clones the iterator
   */
  public Object clone()
  {
    NodeListIterator clone = new NodeListIterator(_env, _list);
    
    clone._position = _position;

    return clone;
  }
}
