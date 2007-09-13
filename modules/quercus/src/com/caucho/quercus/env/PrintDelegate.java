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

package com.caucho.quercus.env;

import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;

/**
 * A delegate that performs print operations for Quercus objects.
 */
public class PrintDelegate {
  private PrintDelegate _next;

  public PrintDelegate()
  {
  }

  public void init(PrintDelegate next)
  {
    _next = next;
  }

  /**
   * @return false if printR is not implemented
   */
  public void printRImpl(Env env,
                        ObjectValue obj,
                        WriteStream out,
                        int depth,
                        IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _next.printRImpl(env, obj, out, depth, valueSet);
  }

  /**
   * @return false if varDump is not implemented
   */
  public void varDumpImpl(Env env,
                      ObjectValue obj,
                      WriteStream out,
                      int depth,
                      IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _next.varDumpImpl(env, obj, out, depth, valueSet);
  }

  public void varExport(Env env,
                        ObjectValue obj,
                        StringBuilder sb)
  {
    _next.varExport(env, obj, sb);
  }
}
