/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.gen;

import com.caucho.java.JavaWriter;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.program.QuercusProgram;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Writer which gathers additional info.
 */
public class ProPhpWriter extends PhpWriter {
  public ProPhpWriter(JavaWriter writer,
                      QuercusProgram program,
                      String className)
  {
    super(writer, program, className);

    writer.setPreferLast(true);
  }

  /**
   * Generates the tail.
   */
  public void generateCoda()
    throws IOException
  {
    if (! _exprMap.isEmpty())
      println();

    PrintWriter out = new PrintWriter(this);
    for (Map.Entry<Expr,String> entry : _exprMap.entrySet()) {
      ExprPro expr = (ExprPro) entry.getKey();
      String var = entry.getValue();

      println("static final com.caucho.quercus.expr.Expr " + var);
      print("  = ");
      expr.getGenerator().generateExpr(this);
      println(";");
    }

    if (! _exprArrayMap.isEmpty())
      println();

    for (Map.Entry<Expr[],String> entry : _exprArrayMap.entrySet()) {
      Expr []exprArray = entry.getKey();
      String var = entry.getValue();

      println("static final com.caucho.quercus.expr.Expr []" + var);
      print("  = new Expr[] {");

      for (int i = 0; i < exprArray.length; i++) {
        if (i != 0)
          print(", ");

        ExprPro expr = (ExprPro) exprArray[i];
        expr.getGenerator().generateExpr(this);
      }
      println("};");
    }

    if (! _valueMap.isEmpty())
      println();

    for (Map.Entry<Value,String> entry : _valueMap.entrySet()) {
      Value value = entry.getKey();
      String var = entry.getValue();

      print("static final");
      if (value instanceof StringValue)
        print(" StringValue");
      else
        print(" com.caucho.quercus.env.Value");

      println(" " + var);
      print("  = ");
      value.generate(out);
      println(";");
    }

    for (Map.Entry<String,Integer> entry : _localMap.entrySet()) {
      print("static final int ");
      print(entry.getKey());
      print(" = " + entry.getValue() + ";");
    }

    if (! _moduleMap.isEmpty())
      println();

    for (Map.Entry<QuercusModule,String> entry : _moduleMap.entrySet()) {
      QuercusModule module = entry.getKey();
      String var = entry.getValue();

      String moduleClass = module.getClass().getName();

      println("static " + moduleClass + " " + var + ";");
    }

    for (int i = 0; i < _staticVarList.size(); i++) {
      println("static StringValue " + _staticVarList.get(i) + ";");
    }

    for (Map.Entry<StringValue,String> entry : _stringValueMap.entrySet()) {
      StringValue string = entry.getKey();
      String var = entry.getValue();

      print("static final StringValue " + var + " = MethodIntern.intern(\"");
      printJavaString(string.toString());
      println("\");");
    }

    for (Map.Entry<String,String> entry : _charArrayMap.entrySet()) {
      String string = entry.getKey();
      String var = entry.getValue();

      print("static final char []" + var + " = \"");
      printJavaString(string.toString());
      println("\".toCharArray();");
    }

    println();
    for (Map.Entry<String,String> entry : _regexpMap.entrySet()) {
      String literalVar = entry.getKey();
      String var = entry.getValue();

      println("static final com.caucho.quercus.lib.regexp.Regexp " + var);
      print("  = com.caucho.quercus.lib.regexp.RegexpModule");
      print(".compileRegexp(");
      print(literalVar);
      println(".toStringValue());");
    }

    println();
    for (String var: _regexpWrapperList) {
      println("static final com.caucho.quercus.lib.regexp.RegexpCache " + var);
      print("  = new com.caucho.quercus.lib.regexp.RegexpCache();");
    }

    println();
    for (Map.Entry<String,String> entry : _eregMap.entrySet()) {
      String patternVar = entry.getKey();
      String var = entry.getValue();

      println("static final com.caucho.quercus.lib.regexp.Ereg " + var);
      print("  = com.caucho.quercus.lib.regexp.RegexpModule");
      print(".createEreg(");
      print(patternVar);
      println(");");
    }

    println();
    for (Map.Entry<String,String> entry : _eregiMap.entrySet()) {
      String patternVar = entry.getKey();
      String var = entry.getValue();

      println("static final com.caucho.quercus.lib.regexp.Eregi " + var);
      print("  = com.caucho.quercus.lib.regexp.RegexpModule");
      print(".createEregi(");
      print(patternVar);
      println(");");
    }

    println();
    for (Map.Entry<String,String> entry : _functionIdMap.entrySet()) {
      String name = entry.getKey();
      String id = entry.getValue();

      println("static int " + id + ";");
    }

    println("static int _fun_name_max;");

    println();
    for (Map.Entry<String,String> entry : _classIdMap.entrySet()) {
      String name = entry.getKey();
      String id = entry.getValue();

      println("static int " + id + ";");
    }

    println("static int _class_name_max;");

    println();
    for (Map.Entry<String,String> entry : _constIdMap.entrySet()) {
      String name = entry.getKey();
      String id = entry.getValue();

      println("static int " + id + ";");
    }

    println("static int _constant_name_max;");

    println();
    println("public void init(com.caucho.quercus.env.Env env)");
    println("{");
    pushDepth();

    if (_functionIdMap.size() > 0) {
      println("com.caucho.quercus.QuercusContext quercus = env.getQuercus();");
      println("com.caucho.quercus.function.AbstractFunction []fun = quercus.getFunctionMap();");

      for (Map.Entry<String,String> entry : _functionIdMap.entrySet()) {
        String id = entry.getValue();

        print("env.updateFunction(");
        print(id);
        print(", fun[");
        print(id);
        println("]);");
      }
    }

    popDepth();
    println("}");


    println();
    println("public void init(com.caucho.quercus.QuercusContext quercus)");
    println("{");
    pushDepth();

    println("_caucho_init(com.caucho.vfs.Vfs.lookup());");

    for (Map.Entry<QuercusModule,String> entry : _moduleMap.entrySet()) {
      QuercusModule module = entry.getKey();
      String var = entry.getValue();

      String moduleClass = module.getClass().getName();

      println(var + " = (" + moduleClass + ") quercus.findModule(\"" + moduleClass + "\");");
    }

    for (int i = 0; i < _staticVarList.size(); i++) {
      println(_staticVarList.get(i) + " = quercus.createStaticName();");
    }

    for (Map.Entry<String,String> entry : _functionIdMap.entrySet()) {
      String name = entry.getKey();
      String id = entry.getValue();

      print(id + " = quercus.getFunctionId(\"");
      printJavaString(name);
      println("\");");
    }

    println();
    println("_fun_name_max = quercus.getFunctionIdCount();");

    for (Map.Entry<String,String> entry : _classIdMap.entrySet()) {
      String name = entry.getKey();
      String id = entry.getValue();

      print(id + " = quercus.getClassId(\"");
      printJavaString(name);
      println("\");");
    }

    println();
    println("_class_name_max = quercus.getClassIdCount();");

    for (Map.Entry<String,String> entry : _constIdMap.entrySet()) {
      String name = entry.getKey();
      String id = entry.getValue();

      print(id + " = quercus.getConstantId(\"");
      printJavaString(name);
      println("\");");
    }

    println();
    println("_constant_name_max = quercus.getConstantIdCount();");

    println();
    println("initFunctions(quercus);");

    popDepth();
    println("}");
  }
}

