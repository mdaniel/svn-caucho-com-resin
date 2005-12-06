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

package com.caucho.php.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;
import com.caucho.php.env.NullValue;
import com.caucho.php.env.PhpClass;

import com.caucho.php.page.PhpPage;

import com.caucho.php.Php;

import com.caucho.make.PersistentDependency;
import com.caucho.make.VersionDependency;

import com.caucho.java.gen.GenClass;

import com.caucho.vfs.Path;
import com.caucho.vfs.Depend;
import com.caucho.vfs.WriteStream;

/**
 * Represents a compiled PHP program.
 */
public class PhpProgram extends GenClass {
  private Php _php;

  private PhpPage _compiledPage;
  
  private Path _sourceFile;

  private HashMap<String,Function> _functionMap;
  private HashMap<String,Function> _functionMapLowerCase
    = new HashMap<String,Function>();

  private HashMap<String,InterpretedClassDef> _classMap;

  private FunctionInfo _functionInfo;
  private Statement _statement;

  private ArrayList<PersistentDependency> _dependList
    = new ArrayList<PersistentDependency>();

  /**
   * Creates a new php program
   *
   * @param php the owning php engine
   * @param sourceFile the path to the source file
   * @param statement the top-level statement
   */
  public PhpProgram(Php php, Path sourceFile,
		    HashMap<String, Function> functionMap,
		    HashMap<String, InterpretedClassDef> classMap,
		    FunctionInfo functionInfo,
		    Statement statement)
  {
    super(php.getClassName(sourceFile));
    
    _php = php;

    _sourceFile = sourceFile;
    addDepend(sourceFile);

    _functionMap = functionMap;

    for (Map.Entry<String,Function> entry : functionMap.entrySet()) {
      _functionMapLowerCase.put(entry.getKey().toLowerCase(),
				entry.getValue());
    }

    _classMap = classMap;

    _functionInfo = functionInfo;
    _statement = statement;

    // Java generation code
    setSuperClassName("com.caucho.php.page.PhpPage");

    addImport("com.caucho.php.Php");
    addImport("com.caucho.php.env.*");
    addImport("com.caucho.php.expr.*");
    addImport("com.caucho.php.program.*");
    
    PhpMain main = new PhpMain(this, functionInfo, statement);
    addComponent(main);
    
    addDependencyComponent().addDependency(new VersionDependency());
  }

  /**
   * Returns the engine.
   */
  public Php getPhp()
  {
    return _php;
  }

  /**
   * Returns the source path.
   */
  public Path getSourcePath()
  {
    return _sourceFile;
  }

  /**
   * Adds a dependency.
   */
  public void addDepend(Path path)
  {
    Depend depend = new Depend(path);
    
    _dependList.add(depend);
    addDependencyComponent().addDependency(depend);
  }

  /**
   * Returns true if the function is modified.
   */
  public boolean isModified()
  {
    for (int i = _dependList.size() - 1; i >= 0; i--) {
      PersistentDependency depend = _dependList.get(i);

      if (depend.isModified())
	return true;
    }

    return false;
  }

  /**
   * Returns the compiled page.
   */
  public PhpPage getCompiledPage()
  {
    return _compiledPage;
  }

  /**
   * Sets the compiled page.
   */
  public void setCompiledPage(PhpPage page)
  {
    _compiledPage = page;
  }

  /**
   * Finds a function.
   */
  public AbstractFunction findFunction(String name)
  {
    AbstractFunction fun = _functionMap.get(name);

    if (fun != null)
      return fun;

    fun = _functionMapLowerCase.get(name.toLowerCase());
    
    return fun;
  }

  /**
   * Returns the functions.
   */
  public Collection<Function> getFunctions()
  {
    return _functionMap.values();
  }

  /**
   * Returns the classes.
   */
  public Collection<InterpretedClassDef> getClasses()
  {
    return _classMap.values();
  }

  /**
   * Finds a function.
   */
  public InterpretedClassDef findClass(String name)
  {
    return _classMap.get(name);
  }

  /**
   * Creates a return for the final expr.
   */
  public PhpProgram createExprReturn()
  {
    // php/1515 - used to convert an eval string to return a value
    
    if (_statement instanceof ExprStatement) {
      ExprStatement exprStmt = (ExprStatement) _statement;

      _statement = new ReturnStatement(exprStmt.getExpr());
    }
    else if (_statement instanceof BlockStatement) {
      BlockStatement blockStmt = (BlockStatement) _statement;

      Statement []statements = blockStmt.getStatements();

      if (statements.length > 0 &&
	  statements[0] instanceof ExprStatement) {
	ExprStatement exprStmt
	  = (ExprStatement) statements[0];

	_statement = new ReturnStatement(exprStmt.getExpr());
      }
    }
    
    return this;
  }

  /**
   * Execute the program
   *
   * @param env the calling environment
   *
   * @throws Throwable
   */
  public Value execute(Env env)
    throws Throwable
  {
    Value value = _statement.execute(env);

    if (value != null)
      return value;
    else
      return NullValue.NULL;
  }

  /**
   * Imports the page definitions.
   */
  public void init(Env env)
  {
    /*
    for (Map.Entry<String,InterpretedClassDef> entry : _classMap.entrySet()) {
      entry.getValue().init(env);
    }
    */
  }

  /**
   * Imports the page definitions.
   */
  public void importDefinitions(Env env)
    throws Throwable
  {
    for (Map.Entry<String,Function> entry : _functionMap.entrySet()) {
      Function fun = entry.getValue();

      if (fun.isGlobal())
	env.addFunction(entry.getKey(), fun);
    }
    
    for (Map.Entry<String,InterpretedClassDef> entry : _classMap.entrySet()) {
      env.addClassDef(entry.getKey(), entry.getValue());
    }
  }

  //
  // Java generation code follows
  //
  
  public String toString()
  {
    return "PhpProgram[" + _sourceFile + "]";
  }
}

