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

import java.io.InputStream;
import java.io.OutputStream;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import com.caucho.java.ScriptStackTrace;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;

import com.caucho.quercus.program.PhpProgram;

/**
 * PHP mysql routines.
 */
public class QuercusMiscModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusMiscModule.class);
  private static final Logger log
    = Logger.getLogger(QuercusMiscModule.class.getName());

  /**
   * Comples and evaluates an expression.
   */
  public Value eval(Env env, String code)
    throws Throwable
  {
    if (log.isLoggable(Level.FINER))
      log.finer(code);

    Quercus quercus = env.getPhp();

    PhpProgram program = quercus.parseCode(code);

    Value value = program.execute(env);

    return value;
  }

  /**
   * Comples and evaluates an expression.
   */
  public Value resin_debug(String code)
    throws Throwable
  {
    log.info(code);

    return NullValue.NULL;
  }

  /**
   * Dumps the stack.
   */
  public static Value dump_stack()
    throws Throwable
  {
    Exception e = new Exception("Stack trace");
    e.fillInStackTrace();

    com.caucho.vfs.WriteStream out = com.caucho.vfs.Vfs.openWrite("stderr:");
    try {
      ScriptStackTrace.printStackTrace(e, out.getPrintWriter());
    } finally {
      out.close();
    }

    return NullValue.NULL;
  }

  /**
   * Execute a system command.
   */
  public static String exec(Env env, String command,
			    @Optional Value output,
			    @Optional @Reference Value result)
  {
    String []args = new String[3];

    try {
      args[0] = "sh";
      args[1] = "-c";
      args[2] = command;
      Process process = Runtime.getRuntime().exec(args);

      InputStream is = process.getInputStream();
      OutputStream os = process.getOutputStream();
      os.close();

      StringBuilder sb = new StringBuilder();
      String line = "";

      int ch;
      boolean hasCr = false;
      while ((ch = is.read()) >= 0) {
	if (ch == '\n') {
	  if (! hasCr) {
	    line = sb.toString();
	    sb.setLength(0);
	    if (output != null)
	      output.put(new StringValue(line));
	  }
	  hasCr = false;
	}
	else if (ch == '\r') {
	  line = sb.toString();
	  sb.setLength(0);
	  output.put(new StringValue(line));
	  hasCr = true;
	}
	else
	  sb.append((char) ch);
      }

      if (sb.length() > 0) {
	line = sb.toString();
	sb.setLength(0);
	output.put(new StringValue(line));
      }

      is.close();

      int status = process.waitFor();

      result.set(new LongValue(status));

      return line;
    } catch (Exception e) {
      env.warning(e.getMessage(), e);

      return null;
    }
  }

  /**
   * Returns the disconnect ignore setting
   */
  public static int ignore_user_abort(@Optional boolean set)
  {
    return 0;
  }

  /**
   * Returns a unique id.
   */
  public String uniqid(@Optional String prefix, @Optional boolean moreEntropy)
  {
    StringBuilder sb = new StringBuilder();

    if (prefix != null)
      sb.append(prefix);

    addUnique(sb);

    if (moreEntropy)
      addUnique(sb);

    return sb.toString();
  }

  private void addUnique(StringBuilder sb)
  {
    long value = RandomUtil.getRandomLong();

    if (value < 0)
      value = -value;

    int limit = 13;

    for (; limit > 0; limit--) {
      long digit = value % 26;
      value = value / 26;

      sb.append((char) ('a' + digit));
    }
  }

  /**
   * Sleep for a number of microseconds.
   */
  public static Value usleep(long microseconds)
  {
    try {
      Thread.sleep(microseconds / 1000);
    } catch (Throwable e) {
    }

    return NullValue.NULL;
  }

  /**
   * Execute a system command.
   */
  public static String system(Env env, String command,
			      @Optional @Reference Value result)
  {
    String []args = new String[3];

    try {
      args[0] = "sh";
      args[1] = "-c";
      args[2] = command;
      Process process = Runtime.getRuntime().exec(args);

      InputStream is = process.getInputStream();
      OutputStream os = process.getOutputStream();
      os.close();

      StringBuilder sb = new StringBuilder();
      String line = "";

      int ch;
      boolean hasCr = false;
      while ((ch = is.read()) >= 0) {
	sb.append((char) ch);
      }

      is.close();

      int status = process.waitFor();

      result.set(new LongValue(status));

      return sb.toString();
    } catch (Exception e) {
      env.warning(e.getMessage(), e);

      return null;
    }
  }
}
