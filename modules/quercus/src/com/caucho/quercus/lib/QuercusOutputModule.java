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

package com.caucho.quercus.lib;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Callback;
import com.caucho.quercus.env.OutputBuffer;

/**
 * PHP output routines.
 */
public class QuercusOutputModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusOutputModule.class);
  private static final Logger log
    = Logger.getLogger(QuercusOutputModule.class.getName());

  /**
   * Sets the implicit flush.
   */
  public Value flush(Env env)
    throws Throwable
  {
    env.getOut().flush();

    return NullValue.NULL;
  }

  /**
   * Clears the output buffer
   */
  public static Value ob_clean(Env env)
    throws Throwable
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.clean();

      return BooleanValue.TRUE;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pops the output buffer
   */
  public static boolean ob_end_clean(Env env)
    throws Throwable
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      ob.clean();

    return env.popOutputBuffer();
  }

  /**
   * Pops the output buffer
   */
  public static boolean ob_end_flush(Env env)
    throws Throwable
  {
    return env.popOutputBuffer();
  }

  /**
   * Returns the contents of the output buffer
   */
  public static Value ob_get_clean(Env env)
    throws Throwable
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      Value result = ob.getContents();

      ob.clean();

      return result;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pushes the output buffer
   */
  public static Value ob_get_contents(Env env)
    throws Throwable
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return ob.getContents();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pushes the output buffer
   */
  public static Value ob_get_length(Env env)
    throws Throwable
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return ob.getLength();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Clears the output buffer
   */
  public static Value ob_implicit_flush(Env env, @Optional int flag)
    throws Throwable
  {
    // XXX: stubbed out

    return NullValue.NULL;
  }

  /**
   * Pushes the output buffer
   */
  public boolean ob_start(Env env,
                          @Optional Callback callback,
                          @Optional int chunkSize,
                          @Optional boolean erase)
    throws Throwable
  {
    env.pushOutputBuffer(callback, chunkSize, erase);

    return true;
  }
}
