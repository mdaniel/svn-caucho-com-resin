/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.cfg;

import com.caucho.config.gen.ApiMethod;
import com.caucho.util.L10N;

/**
 * Configuration for a method of a view.
 */
public class EjbBaseMethod {
  private static final L10N L = new L10N(EjbBaseMethod.class);

  private EjbBean _bean;

  private ApiMethod _method;

  /**
   * Creates a new method.
   *
   * @param bean the owning bean
   * @param method the method from the implementation
   */
  public EjbBaseMethod(EjbBean bean, ApiMethod method)
  {
    if (method == null)
      throw new NullPointerException();
    
    _bean = bean;
    _method = method;
  }

  /**
   * Returns the view.
   */
  public EjbBean getBean()
  {
    return _bean;
  }

  /**
   * Returns the impl method.
   */
  public ApiMethod getMethod()
  {
    return _method;
  }

  /**
   * Returns true if these are equivalent.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof EjbBaseMethod))
      return false;

    EjbBaseMethod baseMethod = (EjbBaseMethod) o;

    if (_bean != baseMethod._bean)
      return false;
    else if (! _method.equals(baseMethod._method))
      return false;
    else
      return true;
  }

  public String toString()
  {
    return "EJBBaseMethod[" + _method + "]";
  }
}
