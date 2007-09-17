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
 * @author Sam
 */

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.ArrayDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;

/**
 * A delegate that intercepts array acces methods on the
 * target objects that implement
 * the {@link com.caucho.quercus.lib.spl.ArrayAccess} interface.
 */
public class ArrayAccessDelegate
  extends ArrayDelegate
{
  @Override
  public Value get(Env env, ObjectValue obj, Value offset)
  {
    return obj.findFunction("offsetGet").callMethod(env, obj, offset);
  }

  @Override
  public Value put(Env env, ObjectValue obj, Value offset, Value value)
  {
    return obj.findFunction("offsetSet").callMethod(env, obj, offset, value);
  }

  @Override
  public Value put(Env env, ObjectValue obj, Value offset)
  {
    return obj.findFunction("offsetSet").callMethod(env, obj, UnsetValue.UNSET, offset);
  }

  @Override
  public Value remove(Env env, ObjectValue obj, Value offset)
  {
    return obj.findFunction("offsetUnset").callMethod(env, obj, offset);
  }
}
