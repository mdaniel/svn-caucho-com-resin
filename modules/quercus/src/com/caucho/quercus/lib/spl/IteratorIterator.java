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
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Callback;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public class IteratorIterator
  implements OuterIterator
{
  private static final L10N L = new L10N(IteratorIterator.class);

  private static final StringBuilderValue M_CURRENT
    = new StringBuilderValue("current");
  private static final StringBuilderValue M_ITERATOR
    = new StringBuilderValue("iterator");
  private static final StringBuilderValue M_KEY
    = new StringBuilderValue("key");
  private static final StringBuilderValue M_NEXT
    = new StringBuilderValue("next");
  private static final StringBuilderValue M_REWIND
    = new StringBuilderValue("rewind");
  private static final StringBuilderValue M_VALID
    = new StringBuilderValue("valid");
  
  private Value _iterator;
  
  @Name("__construct")
  public IteratorIterator(Env env,
			  Value iterator)
  {
    _iterator = iterator;
    /*
    if (iterator.isA("Iterator")) 
      _iterator = iterator;
    else
      throw new QuercusException(L.l("IteratorIterator constructor requires an Iterator argument"));
    */
  }

  /**
   * Returns the component iterator.
   */
  public Value getInnerIterator()
  {
    return _iterator;
  }
  
  /**
   * Returns the current value.
   */
  public Value current(Env env)
  {
    return _iterator.callMethod(env, M_CURRENT);
  }

  /**
   * Returns the current key.
   */
  public Value key(Env env)
  {
    return _iterator.callMethod(env, M_KEY);
  }

  /**
   * Advances to the next row.
   */
  public void next(Env env)
  {
    _iterator.callMethod(env, M_NEXT);
  }

  /**
   * Rewinds the iterator so it is at the first row.
   */
  public void rewind(Env env)
  {
    _iterator.callMethod(env, M_REWIND);
  }

  /**
   * Returns the wrapped valid value.
   */
  public boolean valid(Env env)
  {
    return _iterator.callMethod(env, M_VALID).toBoolean();
  }
}
