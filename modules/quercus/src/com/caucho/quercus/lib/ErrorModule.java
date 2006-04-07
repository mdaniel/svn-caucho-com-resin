/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.Callback;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

/**
 * PHP error handling.
 */
public class ErrorModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(ErrorModule.class);
  private static final Logger log
    = Logger.getLogger(ErrorModule.class.getName());

  public static final int E_ERROR = Env.E_ERROR;
  public static final int E_WARNING = Env.E_WARNING;
  public static final int E_PARSE = Env.E_PARSE;
  public static final int E_NOTICE = Env.E_NOTICE;
  public static final int E_CORE_ERROR = Env.E_CORE_ERROR;
  public static final int E_CORE_WARNING = Env.E_CORE_WARNING;
  public static final int E_COMPILE_ERROR = Env.E_COMPILE_ERROR;
  public static final int E_COMPILE_WARNING = Env.E_COMPILE_WARNING;
  public static final int E_USER_ERROR = Env.E_USER_ERROR;
  public static final int E_USER_WARNING = Env.E_USER_WARNING;
  public static final int E_USER_NOTICE = Env.E_USER_NOTICE;
  public static final int E_ALL = Env.E_ALL;
  public static final int E_STRICT = Env.E_STRICT;

  private long _errorReporting = Env.E_DEFAULT;

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
   * Restores the error handler
   *
   * @param env the quercus environment
   */
  public static boolean restore_error_handler(Env env)
  {
    env.restoreErrorHandler();

    return true;
  }

  /**
   * Sets an error handler
   *
   * @param env the quercus environment
   * @param fun the error handler
   * @param code errorMask error level
   */
  public static boolean set_error_handler(Env env,
					  Callback fun,
					  @Optional("E_ALL") int errorMask)
  {
    env.setErrorHandler(errorMask, fun);

    return true;
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

