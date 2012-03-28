/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.gen;

import com.caucho.java.JavaWriter;
import com.caucho.java.gen.JavaWriterWrapper;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.InfoVarPro;
import com.caucho.quercus.expr.VarInfo;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.program.FunctionInfo;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.QuercusProgram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Writer which gathers additional info.
 */
public class PhpWriter extends JavaWriterWrapper {
  protected QuercusProgram _program;

  private String _className;
  private String _currentClassName;

  private boolean _isProfile;

  protected HashMap<Value, String> _valueMap = new HashMap<Value, String>();

  protected HashMap<Expr, String> _exprMap = new HashMap<Expr, String>();

  protected HashMap<Expr[], String> _exprArrayMap =
    new HashMap<Expr[], String>();

  protected HashMap<QuercusModule, String> _moduleMap =
    new HashMap<QuercusModule, String>();

  protected ArrayList<InterpretedClassDef> _classList =
    new ArrayList<InterpretedClassDef>();

  protected HashMap<StringValue, String> _stringValueMap =
    new HashMap<StringValue, String>();

  protected HashMap<String, String> _charArrayMap =
    new HashMap<String, String>();

  protected HashMap<String, String> _functionIdMap =
    new HashMap<String, String>();

  protected HashMap<String, String> _classIdMap = new HashMap<String, String>();

  protected HashMap<String, String> _constIdMap = new HashMap<String, String>();

  protected HashMap<String, Integer> _localMap = new HashMap<String, Integer>();

  protected HashMap<String, String> _regexpMap = new HashMap<String, String>();

  protected ArrayList<String> _regexpWrapperList = new ArrayList<String>();

  protected HashMap<String, String> _regexpArrayMap =
    new HashMap<String, String>();

  protected HashMap<String, String> _eregMap = new HashMap<String, String>();

  protected HashMap<String, String> _eregiMap = new HashMap<String, String>();

  protected ArrayList<String> _staticVarList = new ArrayList<String>();

  public PhpWriter(JavaWriter writer, QuercusProgram program, String className)
  {
    super(writer);

    _program = program;
    _className = className;
    _currentClassName = className;
  }

  /**
   * Returns the engine.
   */
  public QuercusContext getPhp()
  {
    return _program.getPhp();
  }

  /**
   * Returns the program
   */
  public QuercusProgram getProgram()
  {
    return _program;
  }

  public String getClassName()
  {
    return _className;
  }

  public String getCurrentClassName()
  {
    return _currentClassName;
  }

  public void setCurrentClassName(String name)
  {
    _currentClassName = name;
  }

  public boolean isMethod()
  {
    return !_className.equals(_currentClassName);
  }

  /**
   * True when compiling in profile mode.
   */
  public boolean isProfile()
  {
    return _isProfile;
  }

  /**
   * True when compiling in profile mode.
   */
  public void setProfile(boolean isProfile)
  {
    _isProfile = isProfile;
  }

  /**
   * Prints a contstant.
   */
  public void print(Value value) throws IOException
  {
    print(addValue(value));
  }

  /**
   * Prints a contstant.
   */
  public void printIntern(StringValue value) throws IOException
  {
    String code = addStringValue(value);

    // print(addValue(value));
    print(code);
  }

  /**
   * Adds a constant value.
   * 
   * @return the generated id for the value
   */
  public String addValue(Value value)
  {
    String var = _valueMap.get(value);

    if (var == null) {
      String sValue = value.toString();

      StringBuilder name = new StringBuilder();
      name.append("qv_");

      for (int i = 0; i < 16 && i < sValue.length(); i++) {
        char ch = sValue.charAt(i);

        if (Character.isJavaIdentifierPart(ch))
          name.append(ch);
      }

      name.append("_");
      name.append(generateId());

      var = name.toString();

      _valueMap.put(value, var);
    }

    return var;
  }

  /**
   * Adds a regexp
   * 
   * @return the generated id for the value
   */
  public String addRegexp(String literalVar)
  {
    String var = _regexpMap.get(literalVar);

    if (var == null) {
      StringBuilder name = new StringBuilder();
      name.append("qregexp_");

      name.append(literalVar);

      name.append("_");
      name.append(generateId());

      var = name.toString();

      _regexpMap.put(literalVar, var);
    }

    return var;
  }

  /**
   * Adds a regexp
   * 
   * @return the generated id for the value
   */
  public String addRegexpWrapper()
  {
    StringBuilder name = new StringBuilder();
    name.append("qregexp_wrapper_");

    name.append("_");
    name.append(generateId());

    String var = name.toString();

    _regexpWrapperList.add(var);

    return var;
  }

  /**
   * Adds a regexp
   * 
   * @return the generated id for the value
   */
  public String addRegexpArray(String literalVar)
  {
    String var = _regexpArrayMap.get(literalVar);

    if (var == null) {
      StringBuilder name = new StringBuilder();
      name.append("qregexp_array_");

      name.append(literalVar);

      name.append("_");
      name.append(generateId());

      var = name.toString();

      _regexpArrayMap.put(literalVar, var);
    }

    return var;
  }

  /**
   * Adds a regexp
   * 
   * @return the generated id for the value
   */
  public String addEreg(String literalVar)
  {
    String var = _eregMap.get(literalVar);

    if (var == null) {
      StringBuilder name = new StringBuilder();
      name.append("qereg_");

      name.append(literalVar);

      name.append("_");
      name.append(generateId());

      var = name.toString();

      _eregMap.put(literalVar, var);
    }

    return var;
  }

  /**
   * Adds a regexp
   * 
   * @return the generated id for the value
   */
  public String addEregi(String literalVar)
  {
    String var = _eregiMap.get(literalVar);

    if (var == null) {
      StringBuilder name = new StringBuilder();
      name.append("qeregi_");

      name.append(literalVar);

      name.append("_");
      name.append(generateId());

      var = name.toString();

      _eregiMap.put(literalVar, var);
    }

    return var;
  }

  /**
   * Adds a constant value.
   * 
   * @return the generated id for the value
   */
  public String addLocal(StringValue name, int index)
  {
    String symName = "si_" + name + "_" + index;

    _localMap.put(symName, index);

    return symName;
  }

  /**
   * Adds a constant value.
   * 
   * @return the generated id for the value
   */
  public int getLocal(String symName)
  {
    Integer v = _localMap.get(symName);

    if (v != null)
      return v;
    else
      return -1;
  }

  /**
   * Adds a constant value.
   * 
   * @return the generated id for the value
   */
  public void addClass(InterpretedClassDef cl)
  {
    _classList.add(cl);
  }

  /**
   * Adds an expression
   * 
   * @return the generated id for the expression
   */
  public String addExpr(Expr expr)
  {
    String var = _exprMap.get(expr);

    if (var == null) {
      var = "_quercus_expr_" + generateId();

      _exprMap.put(expr, var);
    }

    return var;
  }

  /**
   * Adds an expression
   * 
   * @return the generated id for the expression
   */
  public String addExprArray(Expr[] exprArray)
  {
    String var = _exprArrayMap.get(exprArray);

    if (var == null) {
      var = "_quercus_expr_" + generateId();

      _exprArrayMap.put(exprArray, var);
    }

    return var;
  }

  public void addSymbolMap(String compilationName, FunctionInfo info)
      throws IOException
  {
    println();
    String symName = "sym_" + compilationName;

    print("private static final com.caucho.util.IntMap");
    println(" " + symName);
    println("  = new com.caucho.util.IntMap();");

    println("static {");
    pushDepth();
    for (VarInfo var : info.getVariables()) {
      InfoVarPro proVar = (InfoVarPro) var;

      int index = getLocal(proVar.getSymbolName());
      if (index >= 0) {
        print(symName + ".put(MethodIntern.intern(\"");
        printJavaString(proVar.getName().toString());
        println("\"), " + index + ");");
      }
    }
    popDepth();
    println("}");
  }

  /**
   * Adds a module
   * 
   * @return the generated id for the expression
   */
  public String addModule(QuercusModule module)
  {
    String var = _moduleMap.get(module);

    if (var == null) {
      var = "_quercus_module_" + generateId();

      _moduleMap.put(module, var);
    }

    return var;
  }

  /**
   * Returns a static variable name.
   */
  public String createStaticVar()
  {
    String varName = "__quercus_static_" + _staticVarList.size();

    _staticVarList.add(varName);

    return varName;
  }

  /**
   * Adds a function id
   */
  public String addFunctionId(String name)
  {
    if (!getPhp().isStrict())
      name = name.toLowerCase(Locale.ENGLISH);

    String id = "f_" + name;

    // XXX: name conflicts
    id = id.replace("\\", "__");

    _functionIdMap.put(name, id);

    return id;
  }

  /**
   * Adds a class id
   */
  public String addClassId(String name)
  {
    String javaName = name.replace("__", "___");
    javaName = javaName.replace("\\", "__");

    String id = "cl_" + javaName;

    _classIdMap.put(name, id);

    return id;
  }

  /**
   * Adds a const id
   */
  public String addConstantId(String name)
  {
    String javaName = name.replace("__", "___");
    javaName = javaName.replace("\\", "__");

    String id = "const_" + javaName;

    _constIdMap.put(name, id);

    return id;
  }

  public String printString(String string) throws IOException
  {
    return printString(new StringBuilderValue(string));
  }

  public String printString(StringValue string) throws IOException
  {
    String code = addStringValue(string);

    print(code);

    return code;
  }

  /**
   * Adds an expression
   * 
   * @return the generated id for the expression
   */
  public String addStringValue(StringValue string)
  {
    String varName = _stringValueMap.get(string);

    if (varName != null)
      return varName;

    StringBuilder sb = new StringBuilder("_s_");

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      if (Character.isJavaIdentifierPart(ch))
        sb.append(ch);
    }

    varName = sb.toString();

    if (!_stringValueMap.values().contains(varName)) {
      _stringValueMap.put(string, varName);
      return varName;
    }

    String baseName = varName;

    int i = 0;
    do {
      varName = baseName + "_" + i++;
    } while (_stringValueMap.values().contains(varName));

    _stringValueMap.put(string, varName);

    return varName;
  }

  /**
   * Adds an expression
   * 
   * @return the generated id for the expression
   */
  public String addCharArray(String string)
  {
    String varName = _charArrayMap.get(string);

    if (varName != null)
      return varName;

    StringBuilder sb = new StringBuilder("_sc_");

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      if (Character.isJavaIdentifierPart(ch))
        sb.append(ch);
    }

    varName = sb.toString();

    if (!_charArrayMap.values().contains(varName)) {
      _charArrayMap.put(string, varName);
      return varName;
    }

    String baseName = varName;

    int i = 0;
    do {
      varName = baseName + "_" + i++;
    } while (_charArrayMap.values().contains(varName));

    _charArrayMap.put(string, varName);

    return varName;
  }

  /**
   * Generates the tail.
   */
  public void generateCoda() throws IOException
  {
  }
}
