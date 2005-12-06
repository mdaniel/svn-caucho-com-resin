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

package com.caucho.quercus.script;

import java.io.Reader;

import javax.script.GenericScriptEngine;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleNamespace;
import javax.script.ScriptException;

import com.caucho.quercus.parser.PhpParser;

import com.caucho.quercus.program.PhpProgram;
import com.caucho.quercus.Quercus;

/**
 * Script engine factory
 */
public class PhpScriptEngine extends GenericScriptEngine {
  private PhpScriptEngineFactory _factory;

  PhpScriptEngine(PhpScriptEngineFactory factory)
  {
    _factory = factory;
  }

  /**
   * evaluates based on a reader.
   */
  public Object eval(Reader script, ScriptContext cxt)
    throws ScriptException
  {
    try {
      PhpProgram program = null; // new Quercus().parse(null, script);

      System.out.println("PGM: " + program);
      
      cxt.getWriter().write("HI");
    
      return null;
      /*
    } catch (ScriptException e) {
      throw e;
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ScriptException(e);
    }
  }

  /**
   * evaluates based on a script.
   */
  public Object eval(String script, ScriptContext cxt)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the engine's factory.
   */
  public PhpScriptEngineFactory getFactory()
  {
    return _factory;
  }

  /**
   * Creates a namespace.
   */
  public Namespace createNamespace()
  {
    return new SimpleNamespace();
  }
}

