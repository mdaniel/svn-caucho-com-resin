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

package com.caucho.config;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Node;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;
import com.caucho.util.NotImplementedException;
import com.caucho.xml.QName;

public class ProgramAttributeStrategy extends AttributeStrategy {
  static final L10N L = new L10N(ProgramAttributeStrategy.class);

  private Method _method;

  public ProgramAttributeStrategy(Method method)
  {
    _method = method;
  }
  
  /**
   * Gets the attribute's method
   */
  public Method getMethod()
  {
    return _method;
  }

  /**
   * Sets the attribute's method
   */
  public void setMethod(Method method)
  {
    _method = method;
  }

  /**
   * Return true if the attribute builder adds a program.
   */
  public void configure(NodeBuilder builder, Object bean, QName name, Node node)
    throws Exception
  {
    try {
      BuilderProgram program = new NodeBuilderChildProgram(builder, node);
      
      _method.invoke(bean, new Object[] { program });
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception)
	throw (Exception) e.getCause();
      else
	throw e;
    }
  }
}
