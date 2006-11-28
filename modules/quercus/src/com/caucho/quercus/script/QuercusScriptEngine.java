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

import java.io.StringReader;
import java.io.Reader;
import java.io.Writer;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleBindings;
import javax.script.ScriptException;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.env.Env;

import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.page.InterpretedPage;

import com.caucho.quercus.parser.QuercusParser;

import com.caucho.quercus.program.QuercusProgram;

import com.caucho.vfs.*;

/**
 * Script engine
 */
public class QuercusScriptEngine
  extends AbstractScriptEngine
  implements Compilable {
  private QuercusScriptEngineFactory _factory;
  private final Quercus _quercus;

  QuercusScriptEngine(QuercusScriptEngineFactory factory)
  {
    _factory = factory;
    _quercus = new Quercus();
  }

  /**
   * Returns the Quercus object.
   */
  Quercus getQuercus()
  {
    return _quercus;
  }

  /**
   * evaluates based on a reader.
   */
  public Object eval(Reader script, ScriptContext cxt)
    throws ScriptException
  {
    try {
      QuercusProgram program = QuercusParser.parse(_quercus, null, script);

      Writer writer = cxt.getWriter();
      
      WriteStream out;

      if (writer != null)
	out = Vfs.openWrite(writer);
      else
	out = Vfs.lookup("null:").openWrite();

      QuercusPage page = new InterpretedPage(program);

      Env env = new Env(_quercus, page, out, null, null);

      env.setScriptContext(cxt);

      Object value = program.execute(env).toJavaObject();

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
   * evaluates based on a script.
   */
  public Object eval(String script, ScriptContext cxt)
    throws ScriptException
  {
    return eval(new StringReader(script), cxt);
  }

  /**
   * compiles based on a reader.
   */
  public CompiledScript compile(Reader script)
    throws ScriptException
  {
    try {
      QuercusProgram program = QuercusParser.parse(_quercus, null, script);

      return new QuercusCompiledScript(this, program);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ScriptException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * evaluates based on a script.
   */
  public CompiledScript compile(String script)
    throws ScriptException
  {
    return compile(new StringReader(script));
  }

  /**
   * Returns the engine's factory.
   */
  public QuercusScriptEngineFactory getFactory()
  {
    return _factory;
  }

  /**
   * Creates a bindings.
   */
  public Bindings createBindings()
  {
    return new SimpleBindings();
  }

  public String toString()
  {
    return "QuercusScriptEngine[]";
  }
}

