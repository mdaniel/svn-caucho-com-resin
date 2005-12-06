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

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusExitException;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.VarMap;
import com.caucho.quercus.env.ChainedMap;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.Callback;

import com.caucho.quercus.module.PhpModule;
import com.caucho.quercus.module.AbstractPhpModule;
import com.caucho.quercus.module.Optional;

import com.caucho.vfs.WriteStream;

/**
 * PHP error handling.
 */
public class PhpErrorModule extends AbstractPhpModule {
  private static final L10N L = new L10N(PhpErrorModule.class);
  private static final Logger log
    = Logger.getLogger(PhpErrorModule.class.getName());
  
  private static final HashMap<String,Value> _constMap
    = new HashMap<String,Value>();

  private long _errorReporting = Env.E_DEFAULT;

  static {
    _constMap.put("E_ERROR", new LongValue(Env.E_ERROR));
    _constMap.put("E_WARNING", new LongValue(Env.E_WARNING));
    _constMap.put("E_PARSE", new LongValue(Env.E_PARSE));
    _constMap.put("E_NOTICE", new LongValue(Env.E_NOTICE));
    _constMap.put("E_CORE_ERROR", new LongValue(Env.E_CORE_ERROR));
    _constMap.put("E_CORE_WARNING", new LongValue(Env.E_CORE_WARNING));
    _constMap.put("E_COMPILE_ERROR", new LongValue(Env.E_COMPILE_ERROR));
    _constMap.put("E_COMPILE_WARNING", new LongValue(Env.E_COMPILE_WARNING));
    _constMap.put("E_USER_ERROR", new LongValue(Env.E_USER_ERROR));
    _constMap.put("E_USER_WARNING", new LongValue(Env.E_USER_WARNING));
    _constMap.put("E_USER_NOTICE", new LongValue(Env.E_USER_NOTICE));
    _constMap.put("E_ALL", new LongValue(Env.E_ALL));
    _constMap.put("E_STRICT", new LongValue(Env.E_STRICT));
  }

  /**
   * Adds the constant to the PHP engine's constant map.
   *
   * @return the new constant chain
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }

  /**
   * Exits
   */
  public Value die(Env env, @Optional String msg)
    throws Exception
  {
    return exit(env, msg);
  }

  /**
   * Write an error
   */
  /*
  public Value error(Env env, String msg)
    throws Exception
  {
    // XXX: valiate
    env.error(msg);

    return NullValue.NULL;
  }
  */

  /**
   * Exits
   */
  public Value exit(Env env, @Optional String msg)
    throws Exception
  {
    if (msg != null) {
      env.getOut().print(msg);

      env.exit(msg);
    }
    else
      env.exit();

    throw new IllegalStateException();
  }

  /**
   * Send a message to the log.
   */
  public static boolean error_log(String message,
				  @Optional int type,
				  @Optional String destination,
				  @Optional String extraHeaders)
    throws Exception
  {
    log.warning(message);

    // XXX: optional parameters not implemented since they seem to
    // conflict with the java.util.logging methodology
    
    return true;
  }

  /**
   * Changes the error reporting value.
   */
  public static long error_reporting(Env env,
				     @Optional Value levelV)
    throws Exception
  {
    if (levelV instanceof DefaultValue)
      return env.getErrorMask();
    else
      return env.setErrorMask(levelV.toInt());
  }

  /**
   * Sets an error handler
   *
   * @param env the quercus environment
   * @param fun the error handler
   * @param code errorMask error level
   */
  public static Value set_error_handler(Env env,
					Callback fun,
					@Optional("E_USER_NOTICE") int errorMask)
    throws Exception
  {
    env.setErrorHandler(errorMask, fun);

    return BooleanValue.TRUE;
  }

  /**
   * Triggers an error.
   *
   * @param env the quercus environment
   * @param msg the error message
   * @param code the error level
   */
  public static Value trigger_error(Env env,
				    String msg,
				    @Optional("E_USER_NOTICE") int code)
    throws Exception
  {
    switch (code) {
    case Env.E_USER_NOTICE:
      env.error(Env.B_USER_NOTICE, msg);
      return BooleanValue.TRUE;
      
    case Env.E_USER_WARNING:
      env.error(Env.B_USER_WARNING, msg);
      return BooleanValue.TRUE;
      
    case Env.E_USER_ERROR:
      env.error(Env.B_USER_ERROR, msg);
      return BooleanValue.TRUE;

    default:
      env.warning(L.l("'0x{0}' is an invalid error type",
		      Integer.toHexString(code)));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Triggers an error.
   *
   * @param env the quercus environment
   * @param msg the error message
   * @param code the error level
   */
  public Value user_error(Env env,
			  String msg,
			  @Optional("E_USER_NOTICE") int code)
    throws Exception
  {
    return trigger_error(env, msg, code);
  }

}

