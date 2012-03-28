/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.*;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.CompilingStatement;
import com.caucho.quercus.statement.Statement;
import com.caucho.quercus.statement.StatementGenerator;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.*;

/**
 * Represents sequence of statements.
 */
public class ProFunction extends Function
  implements CompilingFunction
{
  private static final Logger log
    = Logger.getLogger(ProFunction.class.getName());
  private static final L10N L = new L10N(ProFunction.class);

  public ProFunction(ExprFactory exprFactory,
                     Location location,
                     String name,
                     FunctionInfo info,
                     Arg []argList,
                     Statement []statementList)
  {
    super(exprFactory, location, name, info, argList, statementList);
  }

  public FunctionGenerator getGenerator()
  {
    return GENERATOR;
  }

  private FunctionGenerator GENERATOR = new FunctionGenerator() {
    private StatementGenerator getStatement()
    {
      return ((CompilingStatement) _statement).getGenerator();
    }

    /**
     * Analyzes the arguments for read-only and reference.
     */
    @Override
    public void analyzeArguments(Expr []args, AnalyzeInfo info)
    {
      for (int i = 0; i < args.length; i++) {
        ExprPro arg = (ExprPro) args[i];

        if (i < _args.length) {
          if (_args[i].isReference()) {
            arg.getGenerator().analyzeSetModified(info);
            arg.getGenerator().analyzeSetReference(info);
          }
          else {
            arg.getGenerator().analyze(info);
          }
        }
        else {
          arg.getGenerator().analyzeSetModified(info);
          arg.getGenerator().analyzeSetReference(info);
        }
      }
    }

    /**
     * Analyzes the function.
     */
    @Override
    public void analyze(QuercusProgram program)
    {
      AnalyzeInfo info = new AnalyzeInfo(program, _info);
      info.setInitialBlock(true);

      for (int i = 0; i < _args.length; i++) {
        Arg arg = _args[i];

        InfoVarPro varInfo = (InfoVarPro) _info.createVar(arg.getName());
        VarExprPro var = new VarExprPro(varInfo);
        var.setVarState(VarState.VALID);

        varInfo.setArgumentIndex(i);
        varInfo.setArgument(true);
        if (arg.isReference())
          varInfo.setRefArgument();
        varInfo.setExpectedClass(arg.getExpectedClass());
        varInfo.setDefaultArg(arg.getDefault() != null);

        info.addVar(var);
      }

      Arg []useArgs = getClosureUseArgs();
      if (useArgs != null) {
        for (int i = 0; i < useArgs.length; i++) {
          Arg arg = useArgs[i];

          InfoVarPro varInfo = (InfoVarPro) _info.createVar(arg.getName());
          VarExprPro var = new VarExprPro(varInfo);
          var.setVarState(VarState.VALID);

          varInfo.setArgumentIndex(i);
          varInfo.setArgument(true);
          if (arg.isReference())
            varInfo.setRefArgument();
          varInfo.setExpectedClass(arg.getExpectedClass());
          varInfo.setDefaultArg(arg.getDefault() != null);

          info.addVar(var);
        }
      }

      _hasReturn = ! getStatement().analyze(info);
    }

    /**
     * Returns true if the function can generate the call directly.
     */
    @Override
    public boolean canGenerateCall(Expr []args)
    {
      // can only handle case where the arg length matches
      return args.length <= _args.length;
    }

    /**
     * Generates code to calluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generate(PhpWriter out, ExprGenerator funExpr, Expr []args)
    throws IOException
    {
      generateImpl(out, funExpr, args, false);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateRef(PhpWriter out, ExprGenerator funExpr, Expr []args)
    throws IOException
    {
      generateImpl(out, funExpr, args, true);
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    private void generateImpl(PhpWriter out,
                              ExprGenerator funExpr,
                              Expr []args, boolean isRef)
      throws IOException
    {
      String ref;

      if (_isReturnsReference)
        ref = "Ref";
      else
        ref = "";

      String funName = ProFunction.this.getName();

      out.print("env._fun[" + out.addFunctionId(funName) + "]");
      // out.print("fun_" + getCompilationName());

      out.print(".call" + ref + "(env");

      if (! isVariableArgs()) {
      }
      else if (args.length == 0 && _args.length == 0)
        out.print(", Value.NULL_ARGS");
      else
        out.print(", new Value[] {");

      for (int i = 0; i < args.length; i++) {
        if (i != 0 || ! isVariableArgs())
          out.print(", ");

        ExprPro arg = (ExprPro) args[i];
        ExprGenerator argGen = arg.getGenerator();

        if (_args[i].isReference())
          argGen.generateRef(out);
        else
          argGen.generateValueArg(out);
      }

      for (int i = _args.length; i < args.length; i++) {
        ExprPro arg = (ExprPro) args[i];

        if (i != 0)
          out.print(", ");

        arg.getGenerator().generateArg(out, true);
      }

      if (isVariableArgs() && (_args.length > 0 || args.length > 0))
        out.print("}");

      out.print(")");

      //if (_isReturnsReference && ! isRef)
      //  out.print(".toValue()");

      /* php/3a70 - should only be generated from generateCopy
	   if (! (isRef && _isReturnsReference))
	   out.print(".copyReturn()");
       */
    }

    private boolean isVariableArgs()
    {
      return _info.isVariableArgs() || _args.length > 5;
    }

    private boolean isVariableMap()
    {
      // return _info.isVariableVar();
      // php/3254
      return _info.isUsesSymbolTable() || _info.isVariableVar();

      // XXX: this can't be right
      // || ! _info.isGlobal() && _info.isUsesGlobal();
    }

    /**
     * Generates the code for the class component.
     *
     * @param out the writer to the output stream.
     */
    @Override
    public void generate(PhpWriter out)
    throws IOException
    {
      if (_info.getDeclaringClass() != null) {
        out.println();
        out.print("private static LazyMethod");
        out.println(" fun_" + getCompilationName());
        out.print("  = new LazyMethod(" + out.getCurrentClassName() +".class, ");
        out.print("\"" + ProFunction.this.getName() + "\", ");
        out.println("\"fun_" + getCompilationName() + "\");");
      }
      else {
        out.print("private static AbstractFunction");
        out.println(" fun_" + getCompilationName() + ";");
      }

      if (isVariableArgs())
        generateVariableArgs(out);
      else
        generateFixedArgs(out);

      if (isVariableMap()) {
        out.addSymbolMap(getCompilationName(), _info);
      }
    }

    /**
     * Generates the code for the class component.
     *
     * @param out the writer to the output stream.
     */
    public void generateFixedArgs(PhpWriter out)
    throws IOException
    {
      String ref = _isReturnsReference ? "Ref" : "";
      Arg []useArgs = getClosureUseArgs();

      out.println();
      out.print("public final static class fun_" + getCompilationName() + " extends ");

      if (! isStatic() || getDeclaringClassName() != null)
        out.print("CompiledMethod" + ref + "_" + _args.length);
      else
        out.print("CompiledFunction" + ref + "_" + _args.length);

      out.println(" {");
      out.pushDepth();
      
      if (useArgs != null && useArgs.length > 0) {
        for (int i = 0; i < useArgs.length; i++) {
          out.println("private Value p_" + useArgs[i].getName() + ";");
        }
        out.println();
      }

      out.print("public fun_" + getCompilationName() + "(");

      if (useArgs != null) {
        for (int i = 0; i < useArgs.length; i++) {
          if (i != 0)
            out.print(", ");
        
          out.print("Value p_" + useArgs[i].getName());
        }
      }
      out.println(")");
      
      out.println("{");
      out.pushDepth();
      
      out.print("super(\"");
      out.printJavaString(_name);
      out.print("\"");

      for (int i = 0; i < _args.length; i++) {
        out.print(", ");

        ExprPro defaultExpr = (ExprPro) _args[i].getDefault();

        defaultExpr.getGenerator().generateExpr(out);
      }
      out.println(");");
      
      if (useArgs != null) {
        out.println();
        
        for (int i = 0; i < useArgs.length; i++) {
          out.println("this.p_" + useArgs[i].getName()
                      + " = p_" + useArgs[i].getName() + ";");
        }
      }

      out.popDepth();
      out.println("}");

      out.println();
      if (! isStatic() || getDeclaringClassName() != null) {
	out.println("@Override");
        out.print("public final Value callMethod" + ref
		  + "(Env env, QuercusClass qClass, Value q_this");
      }
      else {
	out.println("@Override");
        out.print("public final Value call" + ref + "(Env env");
      }

      for (int i = 0; i < _args.length; i++) {
        out.print(", ");

        out.print("Value p_");
        out.print(_args[i].getName().toString());
      }
      out.println(")");

      out.println("{");

      out.pushDepth();
      
      if (isMethod() && isStatic()) {
        // php/395y
        out.println("q_this = qClass;");
      }

      generateBody(out);

      out.popDepth();

      out.println("}");

      generateProperties(out);

      out.popDepth();

      out.println("}");

      /*
        if (! isGlobal() || isFinal() || ! isPublic() || getDeclaringClassName() != null) {
          out.println();
          out.println("static {");
          out.pushDepth();

          if (! isGlobal())
            out.println("fun_" + getCompilationName() + ".setGlobal(false);");

          if (isFinal())
            out.println("fun_" + getCompilationName() + ".setFinal(true);");

          if (isProtected())
            out.println("fun_" + getCompilationName() + ".setVisibility(Visibility.PROTECTED);");
          else if (isPrivate())
            out.println("fun_" + getCompilationName() + ".setVisibility(Visibility.PRIVATE);");

          out.println("fun_" + getCompilationName() + ".setDeclaringClassName(\"" + getDeclaringClassName() +  "\");");

          out.popDepth();

          out.println("}");
        }
       */
    }

    /**
     * Generates the code for the class component.
     *
     * @param out the writer to the output stream.
     */
    public void generateVariableArgs(PhpWriter out)
    throws IOException
    {
      out.println();
      out.print("public static final class fun_" + getCompilationName() + " extends ");

      String ref = _isReturnsReference ? "Ref" : "";

      if (! isStatic() || getDeclaringClassName() != null)
        out.println(" CompiledMethod" + ref + "_N {");
      else
        out.println(" CompiledFunction" + ref + "_N {");   

      out.pushDepth();
      out.println("public fun_" + getCompilationName() + "()");
      out.println("{");
      out.print("  super(\"");
      out.printJavaString(getCompilationName());
      out.print("\", new Expr[] {");

      for (int i = 0; i < _args.length; i++) {
        if (i != 0)
          out.print(", ");

        ExprPro defaultExpr = (ExprPro) _args[i].getDefault();

        defaultExpr.getGenerator().generateExpr(out);
      }

      out.println("});");
      out.println("}");

      out.println();
      if (! isStatic() || getDeclaringClassName() != null)
        out.println("public Value callMethod" + ref + "Impl(Env env, QuercusClass qClass, Value q_this, Value []args)");
      else
        out.println("public Value call" + ref + "Impl(Env env, Value []args)");

      out.println("{");
      out.pushDepth();

      if (_info.isVariableArgs()) {
        out.println("Value []quercus_oldArgs = env.setFunctionArgs(args);");
        out.println("try {");
        out.pushDepth();
      }

      generateBody(out);

      if (_info.isVariableArgs()) {
        out.popDepth();
        out.println("} finally {");
        out.pushDepth();
        out.println("env.restoreFunctionArgs(quercus_oldArgs);");
        out.popDepth();
        out.println("}");
      }

      out.popDepth();
      out.println("}");

      generateProperties(out);

      out.popDepth();
      out.println("}");
    }

      private void generateBody(PhpWriter out)
      throws IOException
      {
        out.println("env.checkTimeout();");
        out.println();
        
        if (isMethod() && isStatic()) {
          // php/396n
          out.println("q_this = q_this.getQuercusClass();");
        }

        if (isVariableMap()) {
          int len = _info.getVariables().size();

          out.println("Var []_v = new Var[" + len + "];");

          out.println("java.util.Map<StringValue,EnvVar> _quercus_map = new ProSymbolMap(sym_" + getCompilationName() + ", _v);");
          out.println("java.util.Map<StringValue,EnvVar> _quercus_oldMap = env.pushEnv(_quercus_map);");
        }

        int index = 0;

        for (VarInfo varInfo : _info.getVariables()) {
          InfoVarPro var = (InfoVarPro) varInfo;

          if (isVariableMap())
            var.setSymbolName(out.addLocal(var.getName(), index++));

          String varName = "v_" + var.getName();
          String argName = "p_" + var.getName();

          var.printInitType(out, isVariableArgs());

          String initName = varName;

          if (isVariableMap())
            initName = "_v[" + var.getSymbolName() + "]";

          if (isVariableArgs()) {
            argName = "(args.length <= " + var.getArgumentIndex();
            argName += " ? _defaultArgs[" + var.getArgumentIndex() + "].eval(env)";
            argName += " : args[" + var.getArgumentIndex() + "])";
          }

          var.generateInit(out, initName, argName);

          if (log.isLoggable(Level.FINEST)) {
            log.finest(this + " " + var + " (arg=" + var.isArgument()
                       + ", ass=" + var.isAssigned()
                       + ", var=" + var.isVar()
                       + ", refarg=" + var.isRefArgument() + ")");
          }

          if (var.isArgument()) {
            String expectedClass = var.getExpectedClass();

            if (expectedClass != null
                && ! var.isDefaultArg()) {
              out.println("env.checkTypeHint(" + argName
                          + ",\"" + expectedClass + "\""
                          + ",\"" + argName + "\""
                          + ",\"" + getName() + "\");");
            }
          }
        }

        for (String var : _info.getTempVariables()) {
          out.println("Value " + var + ";");
        }

        //generateVisibilityCheck(out);

        if (isVariableMap()) {
          if (! isStatic() || getDeclaringClass() != null)
            out.println("Value q_oldThis = env.setThis(q_this);");

          out.println("try {");
          out.pushDepth();

          /*
	  for (int i = 0; i < _args.length; i++) {
	    out.print("_quercus_map.put(\"");
	    out.printJavaString(_args[i].getName());
	    out.println("\", (Var) v_" + _args[i].getName() + ");");
	  }
           */
        }

        CompilingStatement statement = (CompilingStatement) _statement;
        statement.getGenerator().generate(out);

        if (isVariableMap()) {
          out.popDepth();
          out.println("} finally {");
          out.pushDepth();
          // out.println("env.setFunctionArgs(_quercus_oldArgs);");
          out.println("env.popEnv(_quercus_oldMap);");

          if (! isStatic() || getDeclaringClass() != null)
            out.println("env.setThis(q_oldThis);");

          out.popDepth();
          out.println("}");
        }

        if (statement.getGenerator().fallThrough() != Statement.RETURN) {
          if (_isReturnsReference)
            out.println("return new Var();");
          else
            out.println("return NullValue.NULL;");
        }
      }

      private void generateVisibilityCheck(PhpWriter out)
      throws IOException
      {
        if (isPublic()) {
        }
        else if (isProtected()) {
          out.println("if (q_oldThis != null)");
          out.println("  q_oldThis.checkProtected(env, getDeclaringClassName());");
        }
        else {
          // private

          out.println("if (q_oldThis != null)");
          out.println("  q_oldThis.checkPrivate(env, getDeclaringClassName());");
        }
      }

      /**
       * Generates the code for the initialization component.
       *
       * @param out the writer to the output stream.
       */
      @Override
      public void generateInit(PhpWriter out)
      throws IOException
      {
        if (getInfo().isClosure())
          return;
        
        if (out.isProfile()) {
          out.print("fun_" + getCompilationName());
          out.print(" = new ProfileFunction(new fun_" + getCompilationName());
          out.print("(), ((com.caucho.quercus.ProfileQuercus) quercus).getProfileIndex(\"");
          out.printJavaString(_name.toLowerCase(Locale.ENGLISH));
          out.println("\"));");
        }
        else {
          out.print("fun_" + getCompilationName());
          out.print(" = new LazyFunction(quercus, \"");
          out.printJavaString(_name.toLowerCase(Locale.ENGLISH));
          out.print("\", ");
          out.print(out.getClassName());
          out.println(".class, \"fun_" + getCompilationName() + "\");");
        }

        out.print("addFunction(\"");
        out.printJavaString(_name.toLowerCase(Locale.ENGLISH));
        out.print("\"");
        out.println(", fun_" + getCompilationName() + ");");
      }

      private void generateProperties(PhpWriter out)
      throws IOException
      {
        if (getDeclaringClassName() != null) {
          out.println();
          out.println("@Override");
          out.println("public String getDeclaringClassName()");
        out.println("{");
        
        out.pushDepth();
        out.print("return \"");
        out.printJavaString(getDeclaringClassName());
        out.println("\";");
        out.popDepth();
        
        out.println("}");
      }
      
      if (isStatic()) {
        out.println();
        out.println("@Override");
        out.println("public boolean isStatic()");
        out.println("{");
        
        out.pushDepth();
        out.println("return " + isStatic() + ";");
        out.popDepth();
        
        out.println("}");
      }
      else {
        if (isFinal()) {
          out.println();
          out.println("@Override");
          out.println("public boolean isFinal()");
          out.println("{");
          
          out.pushDepth();
          out.println("return " + isFinal() + ";");
          out.popDepth();
          
          out.println("}");
        }
        
        if (! isPublic()) {
          out.println();
          out.println("@Override");
          out.println("public boolean isPublic()");
          out.println("{");
          
          out.pushDepth();
          out.println("return " + isPublic() + ";");
          out.popDepth();
          
          out.println("}");
        }
        
        if (isProtected()) {
          out.println();
          out.println("@Override");
          out.println("public boolean isProtected()");
          out.println("{");
          
          out.pushDepth();
          out.println("return " + isProtected() + ";");
          out.popDepth();
          
          out.println("}");
        }
        else if (isPrivate()) {
          out.println();
          out.println("@Override");
          out.println("public boolean isPrivate()");
          out.println("{");
          
          out.pushDepth();
          out.println("return " + isPrivate() + ";");
          out.popDepth();
          
          out.println("}");
        }
      }
      
      if (getComment() != null) {
        out.println();
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
      
    public String toString()
    {
      return getClass().getSimpleName() + "[" + getName() + "]";
    }
  };
}

