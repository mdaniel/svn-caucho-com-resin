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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Callback;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public class FilterIterator extends IteratorIterator
  implements Traversable,
             Iterator,
             OuterIterator
{
  @Name("__construct")
  public FilterIterator(Env env,
			Value iterator)
  {
    super(env, iterator);
  }

  /**
   * Moves to the next value
   */
  @Override
  public void next(Env env)
  {
    super.next(env);
    fetch(env);
  }

  /**
   * Resets the iterator
   */
  @Override
  public void rewind(Env env)
  {
    super.rewind(env);
    fetch(env);
  }

  /**
   * Returns the next value
   */
  public void fetch(Env env)
  {
    for (; valid(env) && ! accept(env); super.next(env)) {
    }
  }

  /**
   * Tests for acceptance
   */
  public boolean accept(Env env)
  {
    return true;
  }
}
