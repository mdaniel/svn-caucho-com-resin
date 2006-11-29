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

package com.caucho.quercus.script;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Writer;

/**
 * Script engine
 */
public class QuercusCompiledScript extends CompiledScript {
  private final QuercusScriptEngine _engine;
  private final QuercusProgram _program;

  QuercusCompiledScript(QuercusScriptEngine engine, QuercusProgram program)
  {
    _engine = engine;
    _program = program;
  }

  /**
   * evaluates based on a reader.
   */
  public Object eval(ScriptContext cxt)
    throws ScriptException
  {
    try {
      Writer writer = cxt.getWriter();

      WriteStream out;

      if (writer != null)
	out = Vfs.openWrite(writer);
      else
	out = Vfs.lookup("null:").openWrite();

      QuercusPage page = new InterpretedPage(_program);

      Env env = new Env(_engine.getQuercus(), page, out, null, null);

      env.setScriptContext(cxt);

      Object value = _program.execute(env).toJavaObject();

      out.flushBuffer();
      out.free();

      return value;
      /*
    } catch (ScriptException e) {
      throw e;
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ScriptException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the script engine.
   */
  public ScriptEngine getEngine()
  {
    return _engine;
  }

  public String toString()
  {
    return "QuercusCompiledScript[]";
  }
}

