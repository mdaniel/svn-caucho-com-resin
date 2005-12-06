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

package com.caucho.php.script;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Script engine factory
 */
public class PhpScriptEngineFactory implements ScriptEngineFactory {
  /**
   * Returns the full name of the ScriptEngine.
   */
  public String getEngineName()
  {
    return "Resin PHP Script Engine";
  }

  /**
   * Returns the version of the ScriptEngine.
   */
  public String getEngineVersion()
  {
    return "0.1";
  }

  /**
   * Returns an array of filename extensions normally used by this
   * language.
   */
  public String []getExtensions()
  {
    return new String[] {};
  }

  /**
   * Returns the mime-types for scripts for the engine.
   */
  public String []getMimeTypes()
  {
    return new String[] {};
  }

  /**
   * Returns the short names for the scripts for the engine,
   * e.g. {"javascript", "rhino"}
   */
  public String []getNames()
  {
    return new String[] {"resin-php"};
  }

  /**
   * Returns the name of the supported language.
   */
  public String getLanguageName()
  {
    return "php";
  }

  /**
   * Returns the version of the scripting language.
   */
  public String getLanguageVersion()
  {
    return "5.0";
  }

  /**
   * Returns engine-specific properties.
   *
   * Predefined keys include:
   * <ul>
   * <li>THREADING
   * </ul>
   */
  public Object getParameter(String key)
  {
    return null;
  }

  /**
   * Returns a string which could invoke a method of a Java object.
   */
  public String getMethodCallSyntax(String obj, String m, String []args)
  {
    return "";
  }

  /**
   * Returns a string which generates an output statement.
   */
  public String getOutputStatement(String toDisplay)
  {
    return "";
  }

  /**
   * Returns a string which generates a valid program.
   */
  public String getProgram(String []statements)
  {
    return "";
  }
  
  /**
   * Returns a ScriptEngine instance.
   */
  public ScriptEngine getScriptEngine()
  {
    return new PhpScriptEngine(this);
  }
}

