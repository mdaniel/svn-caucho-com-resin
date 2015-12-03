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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;

/**
 * Represents a PHP resource
 */
public class ResourceValue extends Value
  implements EnvCleanup
{
  @Override
  public boolean isResource()
  {
    return true;
  }

  /**
   * Implements the EnvCleanup interface.
   */

  public void cleanup()
  {
  }

  /**
   * By default close() will call cleanup().
   * If implementation specific logic is
   * needed to clean up resources it should
   * be defined in an overloaded cleanup().
   */

  public void close()
  {
    cleanup();
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    return new LongValue(System.identityHashCode(this));
  }
  
  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("i:0;");
  }

  /**
   * Converts to a string.
   */
  @Override
  public String toString()
  {
    if (ResourceValue.class.equals(getClass())) {
      return ResourceValue.class.getSimpleName() + "[]";
    }
    else {
      return ResourceValue.class.getSimpleName()
             + "[" + getClass().getSimpleName() + "]";
    }
  }

  @Override
  protected void varDumpImpl(Env env,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("resource(" + toString(env) + ")");
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("resource(" + toString(env) + ")");
  }
}

