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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.quercus.program;

import com.caucho.quercus.env.CountDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaInvoker;
import com.caucho.quercus.env.ObjectValue;

/**
 * A delegate that performs Array operations for Quercus objects.
 */
public class FunctionCountDelegate implements CountDelegate {
  private JavaInvoker _count;

  public FunctionCountDelegate()
  {
  }

  /**
   * Sets the custom function for the array get.
   */
  public void setCount(JavaInvoker count)
  {
    _count = count;
  }
  
  /**
   * Returns the value for the specified key.
   */
  @Override
  public int count(Env env, ObjectValue qThis)
  {
    if (_count != null) {
      return _count.callMethod(env,
                               _count.getQuercusClass(),
                               qThis).toInt();
    }
    else
      return 1;
  }
}
