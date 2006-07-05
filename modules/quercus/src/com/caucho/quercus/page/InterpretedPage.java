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

package com.caucho.quercus.page;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.java.gen.GenClass;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InterpretedClassDef;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

/**
 * Represents an interpreted Quercus program.
 */
public class InterpretedPage extends QuercusPage {
  private final QuercusProgram _program;

  public InterpretedPage(QuercusProgram program)
  {
    _program = program;
  }
  
  /**
   * Execute the program
   *
   * @param env the calling environment
   */
  public Value execute(Env env)
  {
    return _program.execute(env);
  }

  /**
   * Returns the pwd according to the source page.
   */
  public Path getPwd(Env env)
  {
    return getSelfPath(env).getParent();
  }

  /**
   * Returns the pwd according to the source page.
   */
  public Path getSelfPath(Env env)
  {
    return _program.getSourcePath();
  }
  
  /**
   * Imports the page definitions.
   */
  public void init(Env env)
  {
    _program.init(env);
  }

  /**
   * Imports the page definitions.
   */
  public void importDefinitions(Env env)
  {
    _program.importDefinitions(env);
  }

  /**
   * Finds the function
   */
  public AbstractFunction findFunction(String name)
  {
    return _program.findFunction(name);
  }

  /**
   * Finds the class
   */
  public InterpretedClassDef findClass(String name)
  {
    return _program.findClass(name);
  }

  /**
   * Returns the class map.
   */
  public HashMap<String,ClassDef> getClassMap()
  {
    return _program.getClassMap();
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof InterpretedPage))
      return false;

    InterpretedPage page = (InterpretedPage) o;

    return _program == page._program;
  }
  
  public String toString()
  {
    return "InterpretedPage[" +  _program.getSourcePath() + "]";
  }
}

