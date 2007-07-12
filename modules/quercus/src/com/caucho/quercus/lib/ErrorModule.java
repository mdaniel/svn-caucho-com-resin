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

import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.FunctionExpr;
import com.caucho.quercus.expr.IncludeExpr;
import com.caucho.quercus.expr.MethodCallExpr;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * PHP error handling.
 */
public class ErrorModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(ErrorModule.class);
  private static final Logger log
    = Logger.getLogger(ErrorModule.class.getName());

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

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
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  /**
   * Exits
   */
  public Value die(Env env, @Optional String msg)
  {
    if (msg != null)
      return env.die(msg);
    else
      return env.die();
  }
  
  /**
   * Produces a backtrace
   */
  public static Value debug_backtrace(Env env)
  {
    ArrayValue result = new ArrayValueImpl();

    Exception e = new Exception();
    e.fillInStackTrace();

    StackTraceElement []stack = e.getStackTrace();
    int depth = 0;

    for (int i = 1; i < stack.length; i++) {
      StackTraceElement elt = stack[i];

      String name = elt.getMethodName();
      String className = elt.getClassName();

      // System.out.println("NAME: " + name + " " + elt.getClassName());
    
      if (name.equals("executeTop")) {
	return result;
      }
      else if (className.startsWith("_quercus._")
	  && name.equals("call")) {
	String path = unmangleFile(className);
	String fileName = env.getPwd().lookup("./" + path).getNativePath();
	
	ArrayValue call = new ArrayValueImpl();
	result.put(call);

	call.put("file", fileName);
	call.put("line", env.getSourceLine(className, elt.getLineNumber()));
	
	call.put("function", unmangleFunction(className));

	call.put(new StringValueImpl("args"), new ArrayValueImpl());
      }
      else if (className.startsWith("_quercus._")
	       && name.equals("callMethod")) {
	String path = unmangleFile(className);
	String fileName = env.getPwd().lookup("./" + path).getNativePath();
	
	ArrayValue call = new ArrayValueImpl();
	result.put(call);

	call.put("file", fileName);
	call.put("line", env.getSourceLine(className, elt.getLineNumber()));
	
	call.put("function", unmangleFunction(className));
	call.put("class", unmangleClass(className));
	call.put("type", "->");

	call.put(new StringValueImpl("args"), new ArrayValueImpl());
      }
      else if (className.startsWith("_quercus._")
	       && name.equals("execute")) {
	if (stack[i - 1].getMethodName().equals("include") &&
	    stack[i - 1].getClassName().equals("com.caucho.quercus.env.Env")) {
	  String path = unmangleFile(className);
	  String fileName = env.getPwd().lookup("./" + path).getNativePath();
	
	  ArrayValue call = new ArrayValueImpl();
	  result.put(call);

	  call.put("file", fileName);
	  call.put("line", env.getSourceLine(className, elt.getLineNumber()));

	  call.put(new StringValueImpl("args"), new ArrayValueImpl());
	}
      }
      else if (className.equals("com.caucho.quercus.expr.FunctionExpr")
	       && name.equals("evalImpl")) {
	if (stack[i - 1].getMethodName().equals("evalArguments")) {
	}
	else if (result.getSize() == 0 && depth == 0)
	  depth++;
	else
	  addInterpreted(env, result, depth++);
      }
      else if (className.equals("com.caucho.quercus.expr.MethodCallExpr")
	       && name.equals("eval")) {
	if (stack[i - 1].getMethodName().equals("evalArguments")) {
	}
	else if (result.getSize() == 0 && depth == 0)
	  depth++;
	else
	  addInterpreted(env, result, depth++);
      }
      else if (className.equals("com.caucho.quercus.expr.IncludeExpr")
	       && name.equals("eval")) {
	addInterpreted(env, result, depth++);
      }
      else if (className.startsWith("com.caucho.quercus")) {
      }
      else if (name.equals("invoke") || name.equals("invoke0")) {
      }
      else {
	ArrayValue call = new ArrayValueImpl();
	result.put(call);

	call.put("file", elt.getFileName());
	call.put("line", elt.getLineNumber());
	
	call.put("function", elt.getMethodName());
	call.put("class", elt.getClassName());

	call.put(new StringValueImpl("args"), new ArrayValueImpl());
      }
    }

    return result;
  }

  private static void addInterpreted(Env env, ArrayValue result, int i)
  {
    Expr expr = env.peekCall(i);
    if (expr instanceof FunctionExpr) {
      FunctionExpr callExpr = (FunctionExpr) expr;

      ArrayValue call = new ArrayValueImpl();
      result.put(call);
      
      if (callExpr.getFileName() != null) {
	call.put("file", callExpr.getFileName());
	call.put("line", callExpr.getLine());
      }
	
      call.put("function", callExpr.getName());

      call.put(new StringValueImpl("args"), new ArrayValueImpl());
    }
    else if (expr instanceof MethodCallExpr) {
      MethodCallExpr callExpr = (MethodCallExpr) expr;

      ArrayValue call = new ArrayValueImpl();
      result.put(call);
      
      if (callExpr.getFileName() != null) {
	call.put("file", callExpr.getFileName());
	call.put("line", callExpr.getLine());
      }
	
      call.put("function", callExpr.getName());

      call.put("class",
	       env.peekCallThis(i).getQuercusClass().getName());

      call.put("type", "->");

      call.put(new StringValueImpl("args"), new ArrayValueImpl());
    }
    else if (expr instanceof IncludeExpr) {
      ArrayValue call = new ArrayValueImpl();
      result.put(call);
      
      if (expr.getFileName() != null) {
	call.put("file", expr.getFileName());
	call.put("line", expr.getLine());
      }
	
      call.put("function", "include");

      call.put(new StringValueImpl("args"), new ArrayValueImpl());
    }
  }

  private static String unmangleFile(String className)
  {
    int i = "_quercus".length();
    int end = className.indexOf('$');

    if (end < 0)
      end = className.length();
    
    StringBuilder sb = new StringBuilder();

    for (; i < end; i++) {
      char ch = className.charAt(i);

      if (ch == '.' && className.charAt(i + 1) == '_') {
	sb.append('/');
	i++;
      }
      else if (ch != '_') {
	sb.append(ch);
      }
      else if (className.charAt(i + 1) == '_') {
	sb.append('.');
	i++;
      }
      else {
	//System.out.println("UNKNOWN:" + className.charAt(i + 1) + " " + className);
      }
    }

    return sb.toString();
  }

  private static String unmangleFunction(String className)
  {
    int p = className.lastIndexOf("$fun_");

    if (p > 0)
      return className.substring(p + "$fun_".length());
    else
      return className;
  }

  private static String unmangleClass(String className)
  {
    int p = className.lastIndexOf("$quercus_");
    int q = className.lastIndexOf("$");

    if (p > 0 && p < q)
      return className.substring(p + "$quercus_".length(), q);
    else
      return className;
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
  public Value exit(Env env, @Optional Value msg)
  {
    return env.exit(msg);
  }

  /**
   * Send a message to the log.
   */
  public static boolean error_log(String message,
                                  @Optional int type,
                                  @Optional String destination,
                                  @Optional String extraHeaders)
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
   * Sets an exception handler
   *
   * @param env the quercus environment
   * @param fun the exception handler
   */
  public static Value set_exception_handler(Env env,
					    Callback fun)
  {
    return env.setExceptionHandler(fun);
  }

  /**
   * Restore an exception handler
   *
   * @param env the quercus environment
   */
  public static Value restore_exception_handler(Env env)
  {
    env.restoreExceptionHandler();

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
  {
    switch (code) {
    case Env.E_USER_NOTICE:
      env.error(Env.B_USER_NOTICE, "", msg);
      return BooleanValue.TRUE;

    case Env.E_USER_WARNING:
      env.error(Env.B_USER_WARNING, "", msg);
      return BooleanValue.TRUE;

    case Env.E_USER_ERROR:
      env.error(Env.B_USER_ERROR, "", msg);
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
  {
    return trigger_error(env, msg, code);
  }

  static {
    addIni(_iniMap, "error_reporing", null, PHP_INI_ALL);
    addIni(_iniMap, "display_errors", "1", PHP_INI_ALL);
    addIni(_iniMap, "display_startup_errors", "0", PHP_INI_ALL);
    addIni(_iniMap, "log_errors", "0", PHP_INI_ALL);
    addIni(_iniMap, "log_errors_max_len", "1024", PHP_INI_ALL);
    addIni(_iniMap, "ignore_repeated_errors", "0", PHP_INI_ALL);
    addIni(_iniMap, "ignore_repeated_source", "0", PHP_INI_ALL);
    addIni(_iniMap, "report_memleaks", "1", PHP_INI_ALL);
    addIni(_iniMap, "track_errors", "0", PHP_INI_ALL);
    addIni(_iniMap, "html_errors", "1", PHP_INI_ALL);
    addIni(_iniMap, "docref_root", "", PHP_INI_ALL);
    addIni(_iniMap, "docref_ext", "", PHP_INI_ALL);
    addIni(_iniMap, "error_prepend_string", null, PHP_INI_ALL);
    addIni(_iniMap, "error_append_string", null, PHP_INI_ALL);
    addIni(_iniMap, "error_log", null, PHP_INI_ALL);
    addIni(_iniMap, "warn_plus_overloading", null, PHP_INI_ALL);
  }
}

