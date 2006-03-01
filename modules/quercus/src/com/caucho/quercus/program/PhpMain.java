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

import java.util.HashSet;

import java.io.IOException;

import com.caucho.vfs.Path;

import com.caucho.java.JavaWriter;
import com.caucho.java.LineMap;

import com.caucho.java.gen.ClassComponent;

import com.caucho.quercus.env.QuercusClass;

import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.expr.VarInfo;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.InterpretedClassDef;

/**
 * Represents the main method of a compiled PHP program.
 */
public class PhpMain extends ClassComponent {
  private PhpProgram _program;

  private FunctionInfo _functionInfo;
  private Statement _statement;

  /**
   * Creates a new quercus main
   *
   * @param program the owning quercus program
   * @param statement the top-level statement
   */
  public PhpMain(PhpProgram program,
		 FunctionInfo functionInfo,
		 Statement statement)
  {
    _program = program;
    _functionInfo = functionInfo;
    _statement = statement;
  }
  
  /**
   * Generates the code for the class component.
   *
   * @param out the writer to the output stream.
   */
  protected void generate(JavaWriter javaOut)
    throws IOException
  {
    PhpWriter out = new PhpWriter(javaOut, _program);

    Path dstPath = javaOut.getWriteStream().getPath();
    String dstFilename = dstPath.getFullPath();
    LineMap lineMap = new LineMap(dstFilename);

    lineMap.setSourceType("PHP");

    javaOut.setLineMap(lineMap);

    out.println();
    out.println("static com.caucho.vfs.Path _quercus_selfPath;");

    out.println();
    out.println("public static void quercus_setSelfPath(com.caucho.vfs.Path path)");
    out.println("{");
    out.println("  _quercus_selfPath = path;");
    out.println("}");

    out.println();
    out.println("public com.caucho.vfs.Path getSelfPath(Env env)");
    out.println("{");
    out.println("  return _quercus_selfPath;");
    out.println("}");

    AnalyzeInfo info = new AnalyzeInfo(_functionInfo);

    boolean hasReturn = ! _statement.analyze(info);
    
    for (Function fun : _program.getFunctions()) {
      fun.analyze();
    }
    
    for (InterpretedClassDef cl : _program.getClasses()) {
      cl.analyze();
    }
      
    out.println("public Value execute(com.caucho.quercus.env.Env env)");
    out.println("  throws Throwable");
    out.println("{");
    out.pushDepth();

    out.println("com.caucho.vfs.WriteStream _quercus_out = env.getOut();");

    for (VarInfo var : _functionInfo.getVariables()) {
      out.println("Value v_" + var.getName() + " = null;");
    }

    for (String var : _functionInfo.getTempVariables()) {
      out.println("Value " + var + ";");
    }

    _statement.generate(out);
      
    if (! hasReturn)
      out.println("return NullValue.NULL;");
    
    out.popDepth();
    out.println("}");

    _statement.generateCoda(out);

    for (Function fun : _program.getFunctions()) {
      fun.generate(out);
    }

    for (InterpretedClassDef cl : _program.getClasses()) {
      cl.generate(out);
    }

    out.println();
    out.println("private void initFunctions()");
    out.println("{");

    out.pushDepth();
    
    for (Function fun : _program.getFunctions()) {
      fun.generateInit(out);
    }
    
    for (InterpretedClassDef cl : _program.getClasses()) {
      cl.generateInit(out);
    }

    out.popDepth();
    out.println("}");
    
    out.generateCoda();

    javaOut.generateSmap();
  }
}

