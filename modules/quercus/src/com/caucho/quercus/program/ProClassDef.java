/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import java.io.IOException;
import java.util.Map;

/**
 * Represents an interpreted PHP class definition.
 */
public class ProClassDef extends InterpretedClassDef
{
  public ProClassDef(Location location,
                     String name,
                     String parentName,
                     String []ifaceList,
                     int index)
  {
    super(location, name, parentName, ifaceList, index);
  }

  /**
   * Analyzes the class
   */
  public void analyze(QuercusProgram program)
  {
    for (AbstractFunction fun : _functionMap.values()) {
      FunctionGenerator funGen = ((CompilingFunction) fun).getGenerator();

      funGen.analyze(program);
    }
  }

  /**
   * Generates the class.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.addClass(this);

    String implClass;

    if (isInterface())
      implClass = "CompiledInterfaceDef";
    else
      implClass = isAbstract() ? "CompiledAbstractClassDef" : "CompiledClassDef";

    String javaClassName = getCompilationName();

    String oldClassName = out.getCurrentClassName();
    out.setCurrentClassName("quercus_" + javaClassName);

    out.println();
    out.println("private static LazyClassDef q_cl_" + javaClassName + " = ");
    out.print("  new LazyClassDef(\"");
    out.printJavaString(getName());
    out.print("\", " + out.getClassName() + ".class");
    out.println(", \"quercus_" + javaClassName + "\");");

    out.println();
    out.println("public static class quercus_" + javaClassName + " extends CompiledClassDef implements InstanceInitializer, CompiledClass {");
    out.pushDepth();

    out.println("public quercus_" + javaClassName + "()");
    out.println("{");
    out.pushDepth();

    out.print("super(");
    Location location = getLocation();
    out.print("new com.caucho.quercus.Location(");
    out.printQuotedJavaString(location.getFileName());
    out.print(",");
    out.print(location.getLineNumber());
    out.print(",");
    out.printQuotedJavaString(location.getClassName());
    out.print(",");
    out.printQuotedJavaString(location.getFunctionName());
    out.print("), ");

    out.print("\"");
    out.printJavaString(getName());
    out.print("\", ");

    if (getParentName() != null) {
      out.print("\"");
      out.printJavaString(getParentName());
      out.print("\"");
    }
    else
      out.print("null");

    out.print(", new String[] {");
    String []ifaceList = getInterfaces();
    for (int i = 0; i < ifaceList.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("\"");
      out.printJavaString(ifaceList[i]);
      out.print("\"");
    }

    out.print("}");

    if (isFinal())
      out.print(", true");

    out.println(");");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void initClass(QuercusClass cl)");
    out.println("{");
    out.pushDepth();

    out.println("cl.addInitializer(this);");
    out.println();

    if (_constructor != null) {
      // php/393g
      // php/393i
      out.println("cl.setConstructor(fun_" + _constructor.getCompilationName() + ".toFun(cl));");

      // php/393o
      //out.println("cl.addMethod(\"__construct\", fun_" + _constructor.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    if (_getField != null) {
      out.println("cl.setFieldGet(fun_" + _getField.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    if (_setField != null) {
      out.println("cl.setFieldSet(fun_" + _setField.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    if (_call != null) {
      out.println("cl.setCall(fun_" + _call.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    if (_invoke != null) {
      out.println("cl.setInvoke(fun_" + _invoke.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    if (_toString != null) {
      out.println("cl.setToString(fun_" + _toString.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    // php/39ku
    if (_isset != null) {
      out.println("cl.setIsset(fun_" + _isset.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    if (_unset != null) {
      out.println("cl.setUnset(fun_" + _unset.getCompilationName() + ".toFun(cl));");
      out.println();
    }

    for (Map.Entry<String,AbstractFunction> entry : _functionMap.entrySet()) {
      /* XXX: abstract methods need to be added to QuercusClass
              php/393g, php/393i, php/39j2
      if (fun instanceof MethodDeclaration)
        continue;
        */

      out.print("cl.addMethod(\"");
      out.printJavaString(entry.getKey());
      out.print("\", fun_" + entry.getValue().getCompilationName());
      out.println(".toFun(cl));");
    }

    for (Map.Entry<StringValue,FieldEntry> entry : _fieldMap.entrySet()) {
      FieldEntry fieldEntry = entry.getValue();
      ExprGenerator exprGen
        = ((ExprPro) fieldEntry.getValue()).getGenerator();

      out.print("cl.addField(");
      out.print(entry.getKey());
      out.print(", ");
      exprGen.generateExpr(out);

      out.print(", com.caucho.quercus.env.FieldVisibility.");
      out.print(fieldEntry.getVisibility());
      out.println(");");
    }

    for (Map.Entry<String,StaticFieldEntry> entry
         : _staticFieldMap.entrySet()) {
      StaticFieldEntry field = entry.getValue();

      ExprGenerator exprGen = ((ExprPro) field.getValue()).getGenerator();

      out.print("cl.addStaticFieldExpr(\"");
      out.printJavaString(getName());
      out.print("\", \"");
      out.printJavaString(entry.getKey());
      out.print("\", ");
      exprGen.generateExpr(out);
      out.println(");");
    }

    for (Map.Entry<String,Expr> entry : _constMap.entrySet()) {
      ExprGenerator exprGen = ((ExprPro) entry.getValue()).getGenerator();

      out.print("cl.addConstant(\"");
      out.printJavaString(entry.getKey());
      out.print("\", ");
      exprGen.generateExpr(out);
      out.println(");");
    }

    out.popDepth();
    out.println("}");

    /*
    out.println();
    out.println("public void init(QuercusClass cl)");
    out.println("{");
    out.pushDepth();
    out.println("quercus_" + getName() + " def = new quercus_" + getName() +
  "(cl);");
    out.popDepth();
    out.println("}");
    */

    out.println();
    out.println("public void initInstance(Env env, Value valueArg)");
    //out.println("   throws Throwable");
    out.println("{");
    out.pushDepth();

    //out.println("CompiledObjectValue value = (CompiledObjectValue) valueArg;");
    out.println("ObjectValue value = (ObjectValue) valueArg;");

    for (Map.Entry<StringValue,FieldEntry> entry : _fieldMap.entrySet()) {
      StringValue key = entry.getKey();
      FieldEntry fieldEntry = entry.getValue();
      Expr value = fieldEntry.getValue();

      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      out.print("value.initField(");
      out.print(key);
      out.print(", ");
      valueGen.generate(out);

      out.print(", com.caucho.quercus.env.FieldVisibility.");
      out.print(fieldEntry.getVisibility());
      out.println(");");
    }

    /*
    for (AbstractFunction fun : _functionMap.values()) {
      fun.generateInit(out);
    }
    */

    out.popDepth();
    out.println("}");

    for (AbstractFunction fun : _functionMap.values()) {
      FunctionGenerator funGen = ((CompilingFunction) fun).getGenerator();

      funGen.generate(out);
    }

    // XXX:
    out.println();
    out.println("public void init(Env env)");
    out.println("{");
    out.pushDepth();

    out.print("QuercusClass qClass = env.getClass(\"");
    out.printJavaString(getName());
    out.println("\");");

    for (Map.Entry<String,StaticFieldEntry> entry
         : _staticFieldMap.entrySet()) {

      StaticFieldEntry field = entry.getValue();

      ExprGenerator varGen = ((ExprPro) field.getValue()).getGenerator();

      out.print("qClass.getStaticFieldVar(env, ");
      out.printString(entry.getKey());
      out.print(").set(");
      varGen.generate(out);
      out.println(");");
    }

    out.popDepth();
    out.println("}");

    out.println();
    generateProperties(out);
    out.println();

    generateFieldProperties(out);

    out.println();

    out.popDepth();
    out.println("}");

    out.setCurrentClassName(oldClassName);
  }

  public void generateInit(PhpWriter out, boolean useEnv)
    throws IOException
  {
    // XXX: test case for useEnv?

    out.print("env.");
    out.print("addClass(q_cl_" + getCompilationName());
    out.print(", " + out.addClassId(getName()));

    if (getParentName() != null)
      out.print(", " + out.addClassId(getParentName()));
    else
      out.print(", -1");

    out.println(");");
  }

  /**
   * Generates the class initialization.
   */
  public void generateInit(PhpWriter out)
    throws IOException
  {
    generateInit(out, false);
  }

  /**
   * Generates the properties for this class.
   */
  private void generateProperties(PhpWriter out)
    throws IOException
  {
    if (isFinal()) {
      out.println("@Override");
      out.println("public boolean isFinal()");
      out.println("{");
      out.pushDepth();
      out.println("return " + isFinal() + ";");
      out.popDepth();
      out.println("}");
    }

    if (isInterface()) {
      out.println("@Override");
      out.println("public boolean isInterface()");
      out.println("{");
      out.pushDepth();
      out.println("return " + isInterface() + ";");
      out.popDepth();
      out.println("}");
    }

    if (isAbstract()) {
      out.println("@Override");
      out.println("public boolean isAbstract()");
      out.println("{");
      out.pushDepth();
      out.println("return " + isAbstract() + ";");
      out.popDepth();
      out.println("}");
    }

    if (getComment() != null) {
      out.println("@Override");
      out.println("public String getComment()");
      out.println("{");
      out.pushDepth();
      out.print("return \"");
      out.printJavaString(getComment());
      out.println("\";");
      out.popDepth();
      out.println("}");
    }
  }

  /**
   * Generates the field properties.
   */
  private void generateFieldProperties(PhpWriter out)
    throws IOException
  {
    out.println("@Override");
    out.println("public String getFieldComment(StringValue name)");
    out.println("{");
    out.pushDepth();

    boolean isFirst = true;
    for (Map.Entry<StringValue,FieldEntry> entry : _fieldMap.entrySet()) {
      StringValue key = entry.getKey();
      FieldEntry fieldEntry = entry.getValue();

      if (fieldEntry.getComment() != null) {
        if (isFirst) {
          isFirst = false;

          out.print("if ");
        }
        else
          out.print("else if ");

        out.print("(name.equals(");
        out.print(key);
        out.println("))");
        out.pushDepth();
        out.print("return \"");
        out.printJavaString(fieldEntry.getComment());
        out.println("\";");
        out.popDepth();
      }
    }

    out.println();
    out.println("return null;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("@Override");
    out.println("public String getStaticFieldComment(String name)");
    out.println("{");
    out.pushDepth();

    isFirst = true;

    for (Map.Entry<String,StaticFieldEntry> entry
         : _staticFieldMap.entrySet()) {
      StaticFieldEntry field = entry.getValue();

      if (field.getComment() != null) {
        if (isFirst) {
          isFirst = false;
          out.print("if ");
        }
        else
          out.print("else if ");

        out.print("(name.equals(\"");
        out.printJavaString(entry.getKey());
        out.println("\"))");
        out.pushDepth();
        out.print("return \"");
        out.printJavaString(field.getComment());
        out.println("\";");
        out.popDepth();
      }
    }

    out.println();
    out.println("return null;");

    out.popDepth();
    out.println("}");

  }

  public String toString()
  {
    return "ProClassDef[" + getName() + "]";
  }
}

