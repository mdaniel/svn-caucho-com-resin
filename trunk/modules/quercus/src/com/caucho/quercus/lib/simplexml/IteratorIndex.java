/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

public abstract class IteratorIndex
{
  public static IteratorIndex create(int index)
  {
    return new IntIteratorIndex(index);
  }

  public static IteratorIndex create(String index)
  {
    return new StringIteratorIndex(index);
  }

  public abstract Value toValue(Env env, String encoding);

  public static class IntIteratorIndex extends IteratorIndex {
    private int _index;

    public IntIteratorIndex(int index)
    {
      _index = index;
    }

    @Override
    public Value toValue(Env env, String encoding)
    {
      return LongValue.create(_index);
    }
  }

  public static class StringIteratorIndex extends IteratorIndex {
    private String _index;

    public StringIteratorIndex(String index)
    {
      _index = index;
    }

    @Override
    public Value toValue(Env env, String encoding)
    {
      return env.createString(_index);
    }
  }
}
