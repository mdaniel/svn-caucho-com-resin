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

package com.caucho.quercus.program;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.util.L10N;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Represents the introspected static function information.
 */
public class ProByteCodeStaticFunction extends ProStaticFunction
  implements CompilingFunction
{
  private static final L10N L = new L10N(ProStaticFunction.class);
  private static final Logger log
    = Logger.getLogger(ProByteCodeStaticFunction.class.getName());

  private AbstractFunction _function;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public ProByteCodeStaticFunction(ModuleContext moduleContext,
                                   QuercusModule quercusModule,
                                   Method method,
                                   AbstractFunction function)
  {
    super(moduleContext, quercusModule, method);

    _function = function;
  }

  @Override
  public Value callMethod(Env env,
                          QuercusClass qClass,
                          Value qThis,
                          Value []args)
  {
    return _function.call(env, args);
  }
}
