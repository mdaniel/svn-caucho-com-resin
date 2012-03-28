/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.gen;

import com.caucho.java.JavaWriter;
import com.caucho.java.LineMap;
import com.caucho.java.gen.ClassComponent;
import com.caucho.quercus.expr.VarInfo;
import com.caucho.quercus.expr.InfoVarPro;
import com.caucho.quercus.program.*;
import com.caucho.quercus.statement.CompilingStatement;
import com.caucho.quercus.statement.Statement;
import com.caucho.quercus.statement.StatementGenerator;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;

/**
 * Represents the main method of a compiled PHP program.
 */
public class QuercusMain extends ClassComponent
{
  private QuercusProgram _program;
  private String _className;

  private FunctionInfo _functionInfo;
  private Statement _statement;

  private String _userPath;
  private boolean _isProfile;

  private String _initMethod = "_caucho_init";
  private String _isModifiedMethod = "_caucho_is_modified";

  /**
   * Creates a new quercus main
   *
   * @param program the owning quercus program
   */
  public QuercusMain(QuercusProgram program,
                     String className)
  {
    _program = program;
    _className = className;

    _functionInfo = program.getFunctionInfo().copy(); // php/390p
    _functionInfo.setHasThis(true); // php/390p
    _statement = program.getStatement();

    if (! (_statement instanceof CompilingStatement))
      throw new IllegalArgumentException(_statement.getClass().getName() + " needs to be a compiling statement");
  }

  public void setProfile(boolean isProfile)
  {
    _isProfile = isProfile;
  }

  public void setUserPath(String userPath)
  {
    _userPath = userPath;
  }

  /**
   * Generates the code for the class component.
   *
   * @param javaOut the writer to the output stream.
   */
  protected void generate(JavaWriter javaOut)
    throws IOException
  {
    PhpWriter out = new ProPhpWriter(javaOut, _program, _className);

    out.setProfile(_isProfile);

    Path dstPath = javaOut.getWriteStream().getPath();
    String dstFilename = dstPath.getFullPath();
    LineMap lineMap = new LineMap(dstFilename);

    lineMap.setSourceType("PHP");

    javaOut.setLineMap(lineMap);

    out.println();
    out.println("static com.caucho.vfs.Path _quercus_selfPath;");
    out.println("static com.caucho.vfs.Path _quercus_selfDirectory;");

    out.println();
    out.println("public static void quercus_setSelfPath(com.caucho.vfs.Path path)");
    out.println("{");
    out.println("  _quercus_selfPath = path;");
    out.println("  _quercus_selfDirectory = path.getParent();");
    out.println("}");

    out.println();
    out.println("public com.caucho.vfs.Path getSelfPath(Env env)");
    out.println("{");
    out.println("  return _quercus_selfPath;");
    out.println("}");

    out.println();
    out.println("public String getUserPath()");
    out.println("{");
    out.print("  return \"");
    out.printJavaString(_userPath);
    out.println("\";");
    out.println("}");

    out.println();
    out.println("public static String getUserPathStatic()");
    out.println("{");
    out.print("  return \"");
    out.printJavaString(_userPath);
    out.println("\";");
    out.println("}");

    out.println();
    out.println("public static String getUserDirStatic()");
    out.println("{");
    
    int p = _userPath.lastIndexOf('/');
    
    String userDir;
    if (p >= 0)
      userDir = _userPath.substring(0, p + 1);
    else
      userDir = _userPath;
    
    out.print("  return \"");
    out.printJavaString(userDir);
    out.println("\";");
    out.println("}");

    AnalyzeInfo info = new AnalyzeInfo(_program, _functionInfo);

    StatementGenerator statementGen = ((CompilingStatement) _statement).getGenerator();

    boolean hasReturn = ! statementGen.analyze(info);

    for (Function fun : _program.getFunctionList()) {
      ((CompilingFunction) fun).getGenerator().analyze(_program);
    }

    for (InterpretedClassDef cl : _program.getClassList()) {
      ((ProClassDef) cl).analyze(_program);
    }

    out.println();
    out.println("public Value execute(com.caucho.quercus.env.Env env)");
    //out.println("  throws Throwable");
    out.println("{");
    out.pushDepth();

    out.println("com.caucho.vfs.WriteStream _quercus_out = env.getOut();");
    out.println("Value q_this = env.getThis();");

    for (VarInfo var : _functionInfo.getVariables()) {
      InfoVarPro varInfo = (InfoVarPro) var;

      varInfo.printInitType(out, false);
      varInfo.generateInit(out, "v_" + var.getName(), null);
      // out.println("Value v_" + var.getName() + " = null;");
    }

    for (String var : _functionInfo.getTempVariables()) {
      out.println("Value " + var + ";");
    }

    statementGen.generate(out);

    if (! hasReturn)
      out.println("return LongValue.ONE;");

    out.popDepth();
    out.println("}");

    statementGen.generateCoda(out);

    for (Function fun : _program.getFunctionList()) {
      FunctionGenerator funGen = ((CompilingFunction) fun).getGenerator();

      funGen.generate(out);
    }

    /*
    for (Function fun : _program.getConditionalFunctions()) {
      FunctionGenerator funGen = ((CompilingFunction) fun).getGenerator();

      funGen.generate(out);
    }
    */

    for (InterpretedClassDef cl : _program.getClassList()) {
      ProClassDef classGen = (ProClassDef) cl;

      classGen.generate(out);
    }

    /*
    for (InterpretedClassDef cl : _program.getConditionalClasses()) {
      ProClassDef classGen = (ProClassDef) cl;

      classGen.generate(out);
    }
    */

    out.println();
    out.println("private void initFunctions(QuercusContext quercus)");
    out.println("{");

    out.pushDepth();

    for (Function fun : _program.getFunctionList()) {
      FunctionGenerator funGen = ((CompilingFunction) fun).getGenerator();

      funGen.generateInit(out);
    }

    /*
    for (InterpretedClassDef cl : _program.getClasses()) {
      ProClassDef classGen = (ProClassDef) cl;

      classGen.generateInit(out);
    }
    */

    out.popDepth();
    out.println("}");

    generateImport(out);

    generateIsModified(out);

    out.generateCoda();

    javaOut.generateSmap();
  }

  /**
   * Generates the import code.
   *
   * @param out the writer to the output stream.
   */
  protected void generateImport(PhpWriter out)
    throws IOException
  {
    out.println();
    out.println("public void importDefinitions(Env env)");
    out.println("{");
    out.pushDepth();

    if (_program.getFunctions().size() > 0) {
      out.println("AbstractFunction []fun = env._fun;");
      out.println("if (fun.length < _fun_name_max) {");
      out.println("  fun = new AbstractFunction[_fun_name_max + 256];");
      out.println("  System.arraycopy(env._fun, 0, fun, 0, env._fun.length);");
      out.println("  env._fun = fun;");
      out.println("}");

      for (Function fun : _program.getFunctions()) {
        if (fun.isGlobal()) {
          String id = out.addFunctionId(fun.getName());

          out.println("fun[" + id + "] = fun_" + fun.getCompilationName() + ".toFun();");
        }
      }
    }

    if (_program.getClasses().size() > 0) {
      out.println("ClassDef []def = env._classDef;");

      out.println("if (def.length < _class_name_max) {");
      out.println("  def = new ClassDef[_class_name_max + 256];");
      out.println("  System.arraycopy(env._classDef, 0, def, 0, env._classDef.length);");
      out.println("  env._classDef = def;");
      out.println();
      out.println("  QuercusClass []qClass = new QuercusClass[def.length];");
      out.println("  System.arraycopy(env._qClass, 0, qClass, 0, env._qClass.length);");
      out.println("  env._qClass = qClass;");
      out.println("}");

      for (InterpretedClassDef clDef : _program.getClasses()) {
        if (clDef.isTopScope()) {
          String id = out.addClassId(clDef.getName());

          out.println("def[" + id + "] = q_cl_" + clDef.getCompilationName() + ".toClassDef();");
        }
      }
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the isModified code.
   *
   * @param out the writer to the output stream.
   */
  protected void generateIsModified(PhpWriter out)
    throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public boolean isModified()");
    out.println("{");
    out.pushDepth();

    out.println("return " + _isModifiedMethod + "();");

    out.popDepth();
    out.println("}");
  }
}

