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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xpath.expr;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;
import com.caucho.xpath.*;
import com.caucho.xpath.pattern.*;

public abstract class Var {
  /**
   * Returns the value as a boolean.
   */
  boolean getBoolean()
    throws XPathException
  {
    return Expr.toBoolean(getObject());
  }
  
  /**
   * Returns the value as a double.
   */
  double getDouble()
    throws XPathException
  {
    Object o = getObject();
    return Expr.toDouble(getObject());
  }
  
  /**
   * Returns the value as a string.
   */
  String getString()
    throws XPathException
  {
    return Expr.toString(getObject());
  }
  
  /**
   * Returns the value as a string.
   */
  void getString(CharBuffer cb)
    throws XPathException
  {
    cb.append(getString());
  }

  /**
   * Returns the value as a node set.
   */
  NodeIterator getNodeSet(ExprEnvironment env)
    throws XPathException
  {
    Object obj = getObject();

    if (obj instanceof NodeList)
      return new NodeListIterator(env, (NodeList) obj);
    else if (obj instanceof ArrayList)
      return new NodeArrayListIterator(env, (ArrayList) obj);
    else if (obj instanceof Node)
      return new SingleNodeIterator(env, (Node) obj);
    else
      return new SingleNodeIterator(env, null);
  }
  
  /**
   * Returns the value as an object.
   */
  abstract Object getObject();
  
  /**
   * Frees the var.
   */
  public void free()
  {
  }
}
