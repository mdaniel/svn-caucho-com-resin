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

package com.caucho.quercus.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;

import com.caucho.quercus.page.QuercusPage;

import com.caucho.quercus.Quercus;

import com.caucho.make.PersistentDependency;
import com.caucho.make.VersionDependency;
import com.caucho.make.DependencyContainer;

import com.caucho.java.gen.GenClass;

import com.caucho.vfs.Path;
import com.caucho.vfs.Depend;
import com.caucho.vfs.WriteStream;

/**
 * Represents a compiled Quercus program.
 */
public class QuercusProgram extends GenClass {
  private Quercus _quercus;

  private QuercusPage _compiledPage;
  
  private Path _sourceFile;

  private HashMap<String,Function> _functionMap;
  private HashMap<String,Function> _functionMapLowerCase
    = new HashMap<String,Function>();

  private HashMap<String,InterpretedClassDef> _classMap;

  private FunctionInfo _functionInfo;
  private Statement _statement;

  private ArrayList<PersistentDependency> _dependList
    = new ArrayList<PersistentDependency>();
  private DependencyContainer _depend
    = new DependencyContainer();

  /**
   * Creates a new quercus program
   *
   * @param quercus the owning quercus engine
   * @param sourceFile the path to the source file
   * @param statement the top-level statement
   */
  public QuercusProgram(Quercus quercus, Path sourceFile,
			HashMap<String, Function> functionMap,
			HashMap<String, InterpretedClassDef> classMap,
			FunctionInfo functionInfo,
			Statement statement)
  {
    super(quercus.getClassName(sourceFile));
    
    _quercus = quercus;

    _sourceFile = sourceFile;
    if (sourceFile != null)
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
    setSuperClassName("com.caucho.quercus.page.QuercusPage");

    addImport("com.caucho.quercus.Quercus");
    addImport("com.caucho.quercus.env.*");
    addImport("com.caucho.quercus.expr.*");
    addImport("com.caucho.quercus.program.*");
    addImport("com.caucho.quercus.lib.*");
    
    QuercusMain main = new QuercusMain(this, functionInfo, statement);
    addComponent(main);
    
    addDependencyComponent().addDependency(new VersionDependency());
  }

  /**
   * Returns the engine.
   */
  public Quercus getPhp()
  {
    return _quercus;
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
    _depend.add(depend);
  }

  /**
   * Returns true if the function is modified.
   */
  public boolean isModified()
  {
    return _depend.isModified();
  }

  /**
   * Returns the compiled page.
   */
  public QuercusPage getCompiledPage()
  {
    return _compiledPage;
  }

  /**
   * Sets the compiled page.
   */
  public void setCompiledPage(QuercusPage page)
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
   * Finds a function.
   */
  public HashMap<String,ClassDef> getClassMap()
  {
    return new HashMap<String,ClassDef>(_classMap);
  }

  /**
   * Creates a return for the final expr.
   */
  public QuercusProgram createExprReturn()
  {
    // quercus/1515 - used to convert an call string to return a value
    
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
   */
  public Value execute(Env env)
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
    return "QuercusProgram[" + _sourceFile + "]";
  }
}

