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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.CallExpr;
import com.caucho.quercus.expr.FunIncludeExpr;
import com.caucho.quercus.expr.FunIncludeOnceExpr;
import com.caucho.quercus.expr.ObjectMethodExpr;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.lib.file.BinaryOutput;
import com.caucho.quercus.lib.file.BinaryStream;
import com.caucho.quercus.lib.file.FileModule;

import com.caucho.util.L10N;
import com.caucho.util.QDate;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP error handling.
 */
public class ErrorModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(ErrorModule.class);
  private static final Logger log
    = Logger.getLogger(ErrorModule.class.getName());

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

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
  public static final int E_RECOVERABLE_ERROR = Env.E_RECOVERABLE_ERROR;
  public static final int E_DEPRECATED = Env.E_DEPRECATED;
  public static final int E_USER_DEPRECATED = Env.E_USER_DEPRECATED;

  public static final int DEBUG_BACKTRACE_PROVIDE_OBJECT = 1;
  public static final int DEBUG_BACKTRACE_IGNORE_ARGS = 2;

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  /**
   * Exits
   */
  public static Value die(Env env, @Optional String msg)
  {
    if (msg != null)
      return env.die(msg);
    else
      return env.die();
  }

  /**
   * Produces a backtrace
   */
  public static ArrayValue debug_backtrace(Env env,
                                           @Optional("DEBUG_BACKTRACE_PROVIDE_OBJECT") int options,
                                           @Optional int limit)
  {
    Exception e = new Exception();
    e.fillInStackTrace();

    return debug_backtrace_exception(env, e, options);
  }

  public static ArrayValue debug_backtrace_exception(Env env,
                                                     Throwable e,
                                                     int options)
  {
    boolean isPrintArgs = (options & DEBUG_BACKTRACE_IGNORE_ARGS) == 0;

    ArrayValue result = new ArrayValueImpl();

    StackTraceElement []stack = e.getStackTrace();
    int depth = 0;

    for (int i = 1; i < stack.length; i++) {
      StackTraceElement elt = stack[i];

      String name = elt.getMethodName();
      String className = elt.getClassName();

      if (name.equals("executeTop")) {
        return result;
      }
      else if (className.startsWith("_quercus._")
               && name.equals("call")) {
        String path = unmangleFile(className);
        String fileName = env.getQuercus().getPwd().lookup("./" + path).getNativePath();

        String fun = findFunction(stack, i);

        if (fun == null || fun.equals("debug_backtrace"))
          continue;

        ArrayValue call = new ArrayValueImpl();
        result.put(call);

        call.put(env.createString("file"), env.createString(fileName));
        call.put(env.createString("line"),
                 LongValue.create(env.getSourceLine(className, elt.getLineNumber())));

        call.put(env.createString("function"), env.createString(fun));

        if (isPrintArgs) {
          call.put(env.createString("args"), new ArrayValueImpl());
        }
      }
      else if (className.startsWith("_quercus._")
               && name.equals("callMethod")) {
        String path = unmangleFile(className);
        String fileName = env
            .getQuercus().getPwd().lookup("./" + path).getNativePath();

        ArrayValue call = new ArrayValueImpl();
        result.put(call);

        call.put(env.createString("file"), env.createString(fileName));
        call.put(env.createString("line"), LongValue.create(
            env.getSourceLine(className, elt.getLineNumber())));

        call.put(env.createString("function"), env.createString(unmangleFunction(className)));
        call.put(env.createString("class"), env.createString(unmangleClass(className)));
        call.put(env.createString("type"), env.createString("->"));

        if (isPrintArgs) {
          call.put(env.createString("args"), new ArrayValueImpl());
        }
      }
      else if (className.startsWith("_quercus._")
               && name.equals("execute")) {
        String methodName = stack[i - 1].getMethodName();

        String path = unmangleFile(className);

        String fileName = env.getQuercus().getPwd().lookup("./" + path).getNativePath();

        ArrayValue call = new ArrayValueImpl();

        call.put(env.createString("file"), env.createString(fileName));
        call.put(env.createString("line"), LongValue.create(env.getSourceLine(className, elt.getLineNumber())));

        if (methodName.equals("includeOnce")) {
          call.put(env.createString("function"), env.createString("include_once"));

          result.put(call);
        }
        else if (methodName.equals("include")) {
          call.put(env.createString("function"), env.createString("include"));

          result.put(call);
        }
        else if (methodName.equals("callNew")) {
        }
        else if (methodName.equals("createException")) {
        }
        else {
          String fun = findFunction(stack, i);

          if (fun == null || fun.equals("debug_backtrace")) {
          }
          else {
            call.put(env.createString("function"), env.createString(fun));

            result.put(call);
          }
        }
      }
      else if (className.equals("com.caucho.quercus.expr.FunctionExpr")
               && name.equals("evalImpl")) {
        if (stack[i - 1].getMethodName().equals("evalArguments")) {
        }
        else
          addInterpreted(env, result, depth++, isPrintArgs);
      }
      else if (className.equals("com.caucho.quercus.expr.MethodCallExpr")
               && name.equals("eval")) {
        if (stack[i - 1].getMethodName().equals("evalArguments")) {
        }
        else
          addInterpreted(env, result, depth++, isPrintArgs);
      }
      else if (className.equals("com.caucho.quercus.expr.NewExpr")
               && name.equals("eval")) {
        if (stack[i - 1].getMethodName().equals("evalArguments")) {
        }
        else
          addInterpreted(env, result, depth++, isPrintArgs);
      }
      else if (className.equals("com.caucho.quercus.expr.IncludeExpr")
               && name.equals("eval")) {
        addInterpreted(env, result, depth++, isPrintArgs);
      }
      else if (className.equals("com.caucho.quercus.expr.IncludeOnceExpr")
               && name.equals("eval")) {
        addInterpreted(env, result, depth++, isPrintArgs);
      }
      else if (className.equals("com.caucho.quercus.expr.CallExpr")) {
        addInterpreted(env, result, depth++, isPrintArgs);
      }
      else if (className.equals("com.caucho.quercus.env.Env")
               && name.equals("close")) {
        return result;
      }
      else if (className.startsWith("com.caucho.quercus")) {
      }
      else if (name.equals("invoke") || name.equals("invoke0")) {
      }
      else {
        ArrayValue call = new ArrayValueImpl();
        result.put(call);

        call.put(env.createString("file"), env.createString(elt.getFileName()));
        call.put(env.createString("line"), LongValue.create(elt.getLineNumber()));

        call.put(env.createString("function"), env.createString(elt.getMethodName()));
        call.put(env.createString("class"), env.createString(elt.getClassName()));

        if (isPrintArgs) {
          call.put(env.createString("args"), new ArrayValueImpl());
        }
      }
    }

    return result;
  }

  private static String findFunction(StackTraceElement []stack, int i)
  {
    String className = stack[i].getClassName();
    String methodName = stack[i].getMethodName();

    if (i == 0)
      return unmangleFunction(className);

    String prevClassName = stack[i - 1].getClassName();
    String prevMethodName = stack[i - 1].getMethodName();

    if (className.startsWith("_quercus._")
        && methodName.startsWith("call"))
      return unmangleFunction(className);
    else if (prevClassName.startsWith("_quercus._")
        && prevMethodName.startsWith("call"))
      return unmangleFunction(prevClassName);
    else
      return null;
  }

  private static void addInterpreted(Env env, ArrayValue result, int i, boolean isPrintArgs)
  {
    Expr expr = env.peekCall(i);

    if (expr instanceof CallExpr) {
      CallExpr callExpr = (CallExpr) expr;

      StringValue functionName = callExpr.getName();
      if (functionName.equalsString("debug_backtrace")) {
        return;
      }

      ArrayValue call = new ArrayValueImpl();
      result.put(call);

      if (callExpr.getFileName() != null) {
        call.put(env.createString("file"),
                 env.createString(callExpr.getFileName()));
        call.put(env.createString("line"),
                 LongValue.create(callExpr.getLine()));
      }

      call.put(env.createString("function"), callExpr.getName());

      // Create "args" argument value array

      // evaluating args a second time is problematic, affecting mediawiki
      // php/180q
      //ArrayValueImpl args = evalArgsArray(env, callExpr);

      if (isPrintArgs) {
        ArrayValueImpl args = new ArrayValueImpl(env.peekArgs(i));
        call.put(env.createString("args"), args);
      }
    }
    else if (expr instanceof ObjectMethodExpr) {
      ObjectMethodExpr callExpr = (ObjectMethodExpr) expr;

      ArrayValue call = new ArrayValueImpl();
      result.put(call);

      if (callExpr.getFileName() != null) {
        call.put(env.createString("file"),
                 env.createString(callExpr.getFileName()));
        call.put(env.createString("line"),
                 LongValue.create(callExpr.getLine()));
      }

      call.put(env.createString("function"),
               env.createString(callExpr.getName()));

      call.put(env.createString("class"),
               env.createString(env.peekCallThis(i).getClassName()));

      call.put(env.createString("type"), env.createString("->"));

      if (isPrintArgs) {
        call.put(env.createString("args"), new ArrayValueImpl());
      }
    }
    else if (expr instanceof FunIncludeExpr) {
      ArrayValue call = new ArrayValueImpl();
      result.put(call);

      if (expr.getFileName() != null) {
        call.put(env.createString("file"), env.createString(expr.getFileName()));
        call.put(env.createString("line"), LongValue.create(expr.getLine()));
      }

      call.put(env.createString("function"), env.createString("include"));
    }
    else if (expr instanceof FunIncludeOnceExpr) {
      boolean isRequire = ((FunIncludeOnceExpr) expr).isRequire();

      ArrayValue call = new ArrayValueImpl();
      result.put(call);

      if (expr.getFileName() != null) {
        call.put(env.createString("file"), env.createString(expr.getFileName()));
        call.put(env.createString("line"), LongValue.create(expr.getLine()));
      }

      String name;

      if (isRequire)
        name = "require_once";
      else
        name = "include_once";

      call.put(env.createString("function"), env.createString(name));
    }
  }

  // Return an array that contains the values passed
  // into a function as the arguments. IF no values
  // were passed this method returns an empty array.

  private static ArrayValueImpl evalArgsArray(Env env, CallExpr callExpr)
  {
    ArrayValueImpl args = new ArrayValueImpl();

    Value []argsValues = callExpr.evalArguments(env);

    if (argsValues != null) {
      for (int index = 0; index < argsValues.length; index++) {
        Value ref = argsValues[index].toLocalVarDeclAsRef();
        args.put(ref);
      }
    }

    return args;
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
//        System.out.println(
//            "UNKNOWN:" + className.charAt(i + 1) + " " + className);
      }
    }

    return sb.toString();
  }

  private static String unmangleFunction(String className)
  {
    int p = className.lastIndexOf("$fun_");

    if (p > 0) {
      className = className.substring(p + "$fun_".length());

      p = className.lastIndexOf('_');
      if (p > 0)
        return className.substring(0, p);
      else
        return className;
    }
    else
      return className;
  }

  private static String unmangleClass(String className)
  {
    int p = className.lastIndexOf("$quercus_");
    int q = className.lastIndexOf("$");

    if (p > 0 && p < q)
      className = className.substring(p + "$quercus_".length(), q);

    int i = className.lastIndexOf("_");

    if (i >= 0)
      return className.substring(0, i);
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
   * Returns the last error.
   */
  public static Value error_get_last(Env env)
  {
    return env.getLastError();
  }

  /**
   * Send a message to the log.
   */
  public static boolean error_log(Env env,
                                  StringValue message,
                                  @Optional int type,
                                  @Optional StringValue destination,
                                  @Optional StringValue extraHeaders)
  {
    if (type == 3) {
      if (destination.length() == 0) {
        destination = ErrorModule.INI_ERROR_LOG.getAsStringValue(env);
      }

      BinaryStream stream = null;

      try {
        if (destination.length() != 0) {
          stream = FileModule.openForAppend(env, destination, false);
        }
      }
      catch (IOException e) {
        e.printStackTrace();

        env.warning(e);

        log.log(Level.FINE, e.getMessage(), e);
      }

      if (stream == null) {
        System.err.println(message);

        return true;
      }

      try {
        BinaryOutput os = (BinaryOutput) stream;

        String format = "[%d-%b-%Y %H:%M:%S %Z] ";
        String date = QDate.formatGMT(env.getCurrentTime(), format);

        os.print(date);
        os.print(message.toString());
        os.print('\n');

        return true;
      }
      catch (IOException e) {
        env.warning(e);

        return false;
      }
      finally {
        stream.close();
      }
    }
    else if (type == 1) {
      // XXX : message sent by email to the address in destination

      return false;
    }
    else {
      // message sent to PHP's system logger
      StringValue dest = ErrorModule.INI_ERROR_LOG.getAsStringValue(env);
      if(dest.equalsString("syslog")) {
        log.warning(message.toString());

        return true;
      }
      else {
        return ErrorModule.error_log(env, message, 3, dest, extraHeaders);
      }
    }
  }

  /**
   * Changes the error reporting value.
   */
  public static long error_reporting(Env env,
                                     @Optional Value levelV)
  {
    long oldMask = env.getIni("error_reporting").toLong();

    if (! levelV.isDefault()) {
      env.setIni("error_reporting", levelV);
    }

    return oldMask;
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
                                          Callable fun,
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
                                            Callable fun)
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
  {
    return trigger_error(env, msg, code);
  }

  static final IniDefinition INI_ERROR_REPORING
    = _iniDefinitions.add("error_reporting", Env.E_DEFAULT, PHP_INI_ALL);
  static final IniDefinition INI_DISPLAY_ERRORS
    = _iniDefinitions.add("display_errors", "1", PHP_INI_ALL);
  static final IniDefinition INI_DISPLAY_STARTUP_ERRORS
    = _iniDefinitions.add("display_startup_errors", false, PHP_INI_ALL);
  static final IniDefinition INI_LOG_ERRORS
    = _iniDefinitions.add("log_errors", false, PHP_INI_ALL);
  static final IniDefinition INI_LOG_ERRORS_MAX_LEN
    = _iniDefinitions.add("log_errors_max_len", 1024, PHP_INI_ALL);
  static final IniDefinition INI_IGNORE_REPEATED_ERRORS
    = _iniDefinitions.add("ignore_repeated_errors", false, PHP_INI_ALL);
  static final IniDefinition INI_IGNORE_REPEATED_SOURCE
    = _iniDefinitions.add("ignore_repeated_source", false, PHP_INI_ALL);
  static final IniDefinition INI_REPORT_MEMLEAKS
    = _iniDefinitions.add("report_memleaks", true, PHP_INI_ALL);
  static final IniDefinition INI_TRACK_ERRORS
    = _iniDefinitions.add("track_errors", false, PHP_INI_ALL);
  static final IniDefinition INI_HTML_ERRORS
    = _iniDefinitions.add("html_errors", true, PHP_INI_ALL);
  static final IniDefinition INI_DOCREF_ROOT
    = _iniDefinitions.add("docref_root", "", PHP_INI_ALL);
  static final IniDefinition INI_DOCREF_EXT
    = _iniDefinitions.add("docref_ext", "", PHP_INI_ALL);
  static final IniDefinition INI_ERROR_PREPEND_STRING
    = _iniDefinitions.add("error_prepend_string", null, PHP_INI_ALL);
  static final IniDefinition INI_ERROR_APPEND_STRING
    = _iniDefinitions.add("error_append_string", null, PHP_INI_ALL);
  static final IniDefinition INI_ERROR_LOG
    = _iniDefinitions.add("error_log", null, PHP_INI_ALL);
  static final IniDefinition INI_WARN_PLUS_OVERLOADING
    = _iniDefinitions.add("warn_plus_overloading", null, PHP_INI_ALL);
}

