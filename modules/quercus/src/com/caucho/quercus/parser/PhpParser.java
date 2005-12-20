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

package com.caucho.quercus.parser;

import java.io.StringReader;
import java.io.Reader;
import java.io.IOException;

import java.util.ArrayList;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.expr.*;

import com.caucho.quercus.program.*;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.CallbackFunction;

import com.caucho.quercus.program.InterpretedClassDef;

import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.StringPath;
import com.caucho.vfs.ReadStream;

/**
 * Parses a PHP program.
 */
public class PhpParser {
  private final static L10N L = new L10N(PhpParser.class);
  
  private final static int IDENTIFIER = 256;
  private final static int STRING = 257;
  private final static int LONG = 258;
  private final static int DOUBLE = 259;
  private final static int LSHIFT = 260;
  private final static int RSHIFT = 261;
  private final static int PHP_END = 262;
  private final static int EQ = 263;
  private final static int DEREF = 264;
  private final static int THIS = 265;
  private final static int TRUE = 266;
  private final static int FALSE = 267;
  private final static int LEQ = 268;
  private final static int GEQ = 269;
  private final static int NEQ = 270;
  private final static int EQUALS = 271;
  private final static int NEQUALS = 272;
  private final static int C_AND = 273;
  private final static int C_OR = 274;
  private final static int XOR_RES = 275;
  private final static int AND_RES = 276;
  private final static int OR_RES = 277;
  
  private final static int PLUS_ASSIGN = 278;
  private final static int MINUS_ASSIGN = 279;
  private final static int APPEND_ASSIGN = 280;
  private final static int MUL_ASSIGN = 281;
  private final static int DIV_ASSIGN = 282;
  private final static int MOD_ASSIGN = 283;
  private final static int AND_ASSIGN = 284;
  private final static int OR_ASSIGN = 285;
  private final static int XOR_ASSIGN = 286;
  private final static int LSHIFT_ASSIGN = 287;
  private final static int RSHIFT_ASSIGN = 288;
  
  private final static int INCR = 289;
  private final static int DECR = 290;
  
  private final static int SCOPE = 291;
  private final static int ESCAPED_STRING = 292;
  private final static int HEREDOC = 293;
  private final static int ARRAY_RIGHT = 294;
  private final static int SIMPLE_STRING_ESCAPE = 295;
  private final static int COMPLEX_STRING_ESCAPE = 296;
  private final static int LIST = 297;
  
  private final static int ECHO = 512;
  private final static int NULL = 513;
  private final static int IF = 514;
  private final static int WHILE = 515;
  private final static int FUNCTION = 516;
  private final static int CLASS = 517;
  private final static int NEW = 518;
  private final static int RETURN = 519;
  private final static int VAR = 520;
  private final static int PRIVATE = 521;
  private final static int PROTECTED = 522;
  private final static int PUBLIC = 523;
  private final static int FOR = 524;
  private final static int DO = 525;
  private final static int BREAK = 526;
  private final static int CONTINUE = 527;
  private final static int ELSE = 528;
  private final static int EXTENDS = 529;
  private final static int STATIC = 530;
  private final static int INCLUDE = 531;
  private final static int REQUIRE = 532;
  private final static int INCLUDE_ONCE = 533;
  private final static int REQUIRE_ONCE = 534;
  private final static int UNSET = 535;
  private final static int FOREACH = 536;
  private final static int AS = 537;
  private final static int TEXT = 538;
  private final static int ISSET = 539;
  private final static int SWITCH = 540;
  private final static int CASE = 541;
  private final static int DEFAULT = 542;
  private final static int EXIT = 543;
  private final static int GLOBAL = 544;
  private final static int ELSEIF = 545;
  private final static int PRINT = 546;
  private final static int SYSTEM_STRING = 547;
  private final static int SIMPLE_SYSTEM_STRING = 548;
  private final static int COMPLEX_SYSTEM_STRING = 549;
  private final static int TEXT_ECHO = 550;

  private final static IntMap _insensitiveReserved = new IntMap();
  private final static IntMap _reserved = new IntMap();

  private Quercus _quercus;

  private Path _sourceFile;

  private String _file;
  private int _line;
  private boolean _hasCr;

  private int _peek = -1;
  private Reader _is;

  private CharBuffer _sb = new CharBuffer();

  private int _peekToken = -1;
  private String _lexeme = "";
  private String _heredocEnd = null;

  private GlobalScope _globalScope = new GlobalScope();

  private boolean _returnsReference = false;

  private Scope _scope = _globalScope;
  private InterpretedClassDef _quercusClass;

  private FunctionInfo _function;
  
  private boolean _isTop;

  PhpParser(Quercus quercus)
  {
    _quercus = quercus;
  }

  PhpParser(Quercus quercus, Path sourceFile, Reader is)
  {
    _quercus = quercus;

    init(sourceFile, is);
  }

  private void init(Path sourceFile)
    throws IOException
  {
    init(sourceFile, sourceFile.openRead().getReader());
  }

  private void init(Path sourceFile, Reader is)
  {
    _sourceFile = sourceFile;
    _is = is;

    _file = sourceFile.getPath();
    _line = 1;

    _peek = -1;
    _peekToken = -1;
  }
  
  public static PhpProgram parse(Quercus quercus, Path path)
    throws IOException
  {
    ReadStream is = path.openRead();

    try {
      PhpParser parser;
      parser = new PhpParser(quercus, path, is.getReader());

      return parser.parse();
    } finally {
      is.close();
    }
  }
  
  public static PhpProgram parse(Quercus quercus,
                                 Path path, Reader is)
    throws IOException
  {
    return new PhpParser(quercus, path, is).parse();
  }
  
  public static PhpProgram parseEval(Quercus quercus, String str)
    throws IOException
  {
    Path path = new StringPath(str);

    PhpParser parser = new PhpParser(quercus, path, path.openRead().getReader());

    return parser.parseCode();
  }
  
  public static PhpProgram parseEvalExpr(Quercus quercus, String str)
    throws IOException
  {
    Path path = new StringPath(str);

    PhpParser parser = new PhpParser(quercus, path, path.openRead().getReader());

    return parser.parseCode().createExprReturn();
  }
  
  public static Value parseFunction(Quercus quercus, String args, String code)
    throws IOException
  {
    Path argPath = new StringPath(args);
    Path codePath = new StringPath(code);

    PhpParser parser = new PhpParser(quercus);

    Function fun = parser.parseFunction(argPath, codePath);

    return new CallbackFunction(fun);
  }
  
  public static Expr parse(Quercus quercus, String str)
    throws IOException
  {
    Path path = Vfs.lookup("string:");
    
    return new PhpParser(quercus, path, new StringReader(str)).parseExpr();
  }
  
  public static Expr parseDefault(String str)
  {
    try {
      Path path = Vfs.lookup("string:");
    
      return new PhpParser(null, path, new StringReader(str)).parseExpr();
    } catch (IOException e) {
      e.printStackTrace();
      
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Returns the current filename.
   */
  public String getFileName()
  {
    if (_sourceFile == null)
      return null;
    else
      return _sourceFile.getPath();
  }

  /**
   * Returns the current line
   */
  public int getLine()
  {
    return _line;
  }

  PhpProgram parse()
    throws IOException
  {
    _function = new FunctionInfo(_quercus, "main");

    // quercus/0b0d
    _function.setVariableVar(true);
    _function.setUsesSymbolTable(true);
    
    Statement stmt = parseTop();

    PhpProgram program = new PhpProgram(_quercus, _sourceFile,
					_globalScope.getFunctionMap(),
					_globalScope.getClassMap(),
					_function,
					stmt);

    /*
    com.caucho.vfs.WriteStream out = com.caucho.vfs.Vfs.lookup("stdout:").openWrite();
    out.setFlushOnNewline(true);
    stmt.debug(new JavaWriter(out));
    */

    return program;
  }

  PhpProgram parseCode()
    throws IOException
  {
    _function = new FunctionInfo(_quercus, "eval");
    // XXX: need param or better function name for non-global?
    _function.setGlobal(false);

    ArrayList<Statement> stmtList = parseStatementList();
    
    return new PhpProgram(_quercus, _sourceFile,
			  _globalScope.getFunctionMap(),
			  _globalScope.getClassMap(),
			  _function,
			  BlockStatement.create(stmtList));
  }

  Function parseFunction(Path argPath, Path codePath)
    throws IOException
  {
    _function = new FunctionInfo(_quercus, "anonymous");
    // XXX: need param or better function name for non-global?
    _function.setGlobal(false);

    init(argPath);

    ArrayList<Arg> args = parseFunctionArgDefinition();
      
    init(codePath);
      
    ArrayList<Statement> statementList = null;

    statementList = parseStatementList();

    Function function = new Function("anonymous", _function, args, statementList);

    return function;
  }

  /**
   * Parses the top page.
   */
  Statement parseTop()
    throws IOException
  {
    _isTop = true;
    
    ArrayList<Statement> statements = new ArrayList<Statement>();

    int token = parsePhpText();

    if (_lexeme.length() > 0)
      statements.add(new TextStatement(_lexeme));

    if (token == TEXT_ECHO) {
      parseEcho(statements);
    }
    
    statements.addAll(parseStatementList());

    return BlockStatement.create(statements);
  }

  /**
   * Parses a statement list.
   */
  private ArrayList<Statement> parseStatementList()
    throws IOException
  {
    ArrayList<Statement> statements = new ArrayList<Statement>();

    while (true) {
      int token = parseToken();

      switch (token) {
      case -1:
	return statements;

      case ';':
	break;

      case ECHO:
	parseEcho(statements);
	break;

      case PRINT:
	statements.add(parsePrint());
	break;

      case UNSET:
	parseUnset(statements);
	break;

      case FUNCTION:
	{
	  Function fun = parseFunctionDefinition();

	  if (! _isTop) {
	    statements.add(new FunctionDefStatement(fun));
	  }
	}
	break;

      case CLASS:
	parseClassDefinition();
	// statements.add(new ClassDefStatement(parseClassDefinition()));
	break;

      case IF:
	statements.add(parseIf());
	break;

      case SWITCH:
	statements.add(parseSwitch());
	break;

      case WHILE:
	statements.add(parseWhile());
	break;

      case DO:
	statements.add(parseDo());
	break;

      case FOR:
	statements.add(parseFor());
	break;

      case FOREACH:
	statements.add(parseForeach());
	break;

      case PHP_END:
	return statements;

      case RETURN:
	statements.add(parseReturn());
	break;

      case BREAK:
	statements.add(BreakStatement.BREAK);
	break;

      case CONTINUE:
	statements.add(ContinueStatement.CONTINUE);
	break;

      case GLOBAL:
	statements.add(parseGlobal());
	break;
	
      case STATIC:
	statements.add(parseStatic());
	break;

      case '{':
	{
	  ArrayList<Statement> statementList = parseStatementList();

	  expect('}');

	  statements.addAll(statementList);
	}
	break;

      case '}':
      case CASE:
      case DEFAULT:
	_peekToken = token;
	return statements;

      case TEXT:
	if (_lexeme.length() > 0) {
	  statements.add(new TextStatement(_lexeme));
	}
	break;
	
      case TEXT_ECHO:
	if (_lexeme.length() > 0)
	  statements.add(new TextStatement(_lexeme));

	parseEcho(statements);

	break;

      default:
	_peekToken = token;
	
	statements.add(parseExprStatement());
	break;
      }
    }
  }

  private Statement parseStatement()
    throws IOException
  {
    int token = parseToken();
    switch (token) {
    case ';':
      return NullStatement.NULL;

    case '{':
      {
	ArrayList<Statement> statementList = parseStatementList();

	expect('}');

	return BlockStatement.create(statementList);
      }

    case IF:
      return parseIf();

    case SWITCH:
      return parseSwitch();

    case WHILE:
      return parseWhile();

    case DO:
      return parseDo();

    case FOR:
      return parseFor();

    case FOREACH:
      return parseForeach();

    case TEXT:
      if (_lexeme.length() > 0)
	return new TextStatement(_lexeme);
      else
	return parseStatement();
      
    default:
      Statement stmt = parseStatementImpl(token);

      token  = parseToken();
      if (token != ';')
	_peekToken = token;

      return stmt;
    }
  }

  /**
   * Parses statements that expect to be terminated by ';'.
   */
  private Statement parseStatementImpl(int token)
    throws IOException
  {
    switch (token) {
    case ECHO:
      {
	ArrayList<Statement> statementList = new ArrayList<Statement>();
	parseEcho(statementList);

	return BlockStatement.create(statementList);
      }

    case PRINT:
      return parsePrint();

    case UNSET:
      return parseUnset();

    case GLOBAL:
      return parseGlobal();

    case STATIC:
      return parseStatic();

    case BREAK:
      return BreakStatement.BREAK;

    case CONTINUE:
      return ContinueStatement.CONTINUE;

    case RETURN:
      return parseReturn();

    default:
      _peekToken = token;
      return parseExprStatement();

      /*
    default:
      throw error(L.l("unexpected token {0}.", tokenName(token)));
      */
    }
  }

  /**
   * Parses the echo statement.
   */
  private void parseEcho(ArrayList<Statement> statements)
    throws IOException
  {
    while (true) {
      Expr expr = parseTopExpr();

      createEchoStatements(statements, expr);

      int token = parseToken();

      if (token != ',') {
	_peekToken = token;
	return;
      }
    }
  }

  /**
   * Creates echo statements from an expression.
   */
  private void createEchoStatements(ArrayList<Statement> statements,
				    Expr expr)
  {
    if (expr == null) {
      // since AppendExpr.getNext() can be null.
    }
    else if (expr instanceof AppendExpr) {
      AppendExpr append = (AppendExpr) expr;

      // XXX: children of append print differently?

      createEchoStatements(statements, append.getValue());
      createEchoStatements(statements, append.getNext());
    }
    else if (expr instanceof StringLiteralExpr) {
      StringLiteralExpr string = (StringLiteralExpr) expr;
      
      Statement statement = new TextStatement(string.evalConstant().toString());

      statements.add(statement);
    }
    else {
      Statement statement = new EchoStatement(expr);

      statements.add(statement);
    }
  }

  /**
   * Parses the print statement.
   */
  private Statement parsePrint()
    throws IOException
  {
    int token = parseToken();

    ArrayList<Expr> args;

    if (token == '(') {
      // quercus/112z
      args = parseArgs();
    }
    else {
      _peekToken = token;

      args = new ArrayList<Expr>();
      args.add(parseTopExpr());
    }

    FunctionExpr expr = new FunctionExpr("print", args);
    expr.setLocation(getFileName(), getLine());

    return new ExprStatement(expr);
  }

  /**
   * Parses the global statement.
   */
  private Statement parseGlobal()
    throws IOException
  {
    ArrayList<Statement> statementList = new ArrayList<Statement>();
    
    while (true) {
      Expr expr = parseTopExpr();

      if (expr instanceof VarExpr) {
	VarExpr var = (VarExpr) expr;

	statementList.add(new GlobalStatement(var));
      }
      else
	throw error(L.l("unknown expr to global"));

      // statementList.add(new ExprStatement(expr));

      int token = parseToken();

      if (token != ',') {
	_peekToken = token;
	return BlockStatement.create(statementList);
      }
    }
  }

  /**
   * Parses the static statement.
   */
  private Statement parseStatic()
    throws IOException
  {
    ArrayList<Statement> statementList = new ArrayList<Statement>();
    
    while (true) {
      expect('$');

      String name = parseIdentifier();

      VarExpr var = new VarExpr(_function.createVar(name));

      Expr init = null;

      int token = parseToken();

      if (token == '=') {
	init = parseExpr();
	token = parseToken();
      }

      statementList.add(new StaticStatement(var, init));
      
      if (token != ',') {
	_peekToken = token;
	return BlockStatement.create(statementList);
      }
    }
  }

  /**
   * Parses the unset statement.
   */
  private Statement parseUnset()
    throws IOException
  {
    ArrayList<Statement> statementList = new ArrayList<Statement>();
    parseUnset(statementList);

    return BlockStatement.create(statementList);
  }

  /**
   * Parses the unset statement.
   */
  private void parseUnset(ArrayList<Statement> statementList)
    throws IOException
  {
    int token = parseToken();

    if (token != '(') {
      _peekToken = token;

      statementList.add(parseTopExpr().createUnset());

      return;
    }

    do {
      statementList.add(parseTopExpr().createUnset());
    } while ((token = parseToken()) == ',');

    _peekToken = token;
    expect(')');
  }

  /**
   * Parses the if statement
   */
  private Statement parseIf()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      expect('(');

      Expr test = parseExpr();

      expect(')');

      Statement trueBlock = parseStatement();
      Statement falseBlock = null;

      int token = parseToken();

      if (token == ELSEIF) {
	falseBlock = parseIf();
      }
      else if (token == ELSE) {
	falseBlock = parseStatement();
      }
      else
	_peekToken = token;

      Statement stmt = new IfStatement(test, trueBlock, falseBlock);

      return stmt;
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses the switch statement
   */
  private Statement parseSwitch()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      expect('(');

      Expr test = parseExpr();

      expect(')');

      expect('{');

      int token;

      ArrayList<Expr[]> caseList = new ArrayList<Expr[]>();
      ArrayList<BlockStatement> blockList = new ArrayList<BlockStatement>();

      ArrayList<Integer> fallThroughList = new ArrayList<Integer>();
      BlockStatement defaultBlock = null;

      while ((token = parseToken()) == CASE || token == DEFAULT) {
	ArrayList<Expr> valueList = new ArrayList<Expr>();
	boolean isDefault = false;
      
	while (token == CASE || token == DEFAULT) {
	  if (token == CASE) {
	    Expr value = parseExpr();

	    valueList.add(value);
	  }
	  else
	    isDefault = true;

	  token = parseToken();
	  if (token == ':') {
	  }
	  else if (token == ';') {
	    // XXX: warning?
	  }
	  else
	    throw error("expected ':' at " + tokenName(token));

	  token = parseToken();
	}

	_peekToken = token;

	Expr []values = new Expr[valueList.size()];
	valueList.toArray(values);

	ArrayList<Statement> newBlockList = parseStatementList();

	for (int fallThrough : fallThroughList) {
	  BlockStatement block = blockList.get(fallThrough);

	  boolean isDefaultBlock = block == defaultBlock;

	  block = block.append(newBlockList);

	  blockList.set(fallThrough, block);

	  if (isDefaultBlock)
	    defaultBlock = block;
	}
      
	BlockStatement block = new BlockStatement(newBlockList);

	if (values.length > 0) {
	  caseList.add(values);

	  blockList.add(block);
	}

	if (isDefault)
	  defaultBlock = block;

	if (blockList.size() > 0 &&
	    ! fallThroughList.contains(blockList.size() - 1)) {
	  fallThroughList.add(blockList.size() - 1);
      }

	  
	if (block.fallThrough() != Statement.FALL_THROUGH)
	  fallThroughList.clear();
      }

      _peekToken = token;

      expect('}');

      SwitchStatement stmt = new SwitchStatement(test,
						 caseList, blockList,
						 defaultBlock);

      return stmt;
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses the 'while' statement
   */
  private Statement parseWhile()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      expect('(');

      Expr test = parseExpr();

      expect(')');
    
      Statement block = parseStatement();

      return new WhileStatement(test, block);
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses the 'do' statement
   */
  private Statement parseDo()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      Statement block = parseStatement();

      expect(WHILE);
      expect('(');

      Expr test = parseExpr();

      expect(')');
    
      return new DoStatement(test, block);
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses the 'for' statement
   */
  private Statement parseFor()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      expect('(');

      Expr init = null;

      int token = parseToken();
      if (token != ';') {
	_peekToken = token;
	init = parseTopCommaExpr();
	expect(';');
      }

      Expr test = null;

      token = parseToken();
      if (token != ';') {
	_peekToken = token;
	test = parseTopExpr();
	expect(';');
      }

      Expr incr = null;

      token = parseToken();
      if (token != ')') {
	_peekToken = token;
	incr = parseTopCommaExpr();
	expect(')');
      }

      Statement block = parseStatement();

      Statement stmt = new ForStatement(init, test, incr, block);

      return stmt;
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses the 'foreach' statement
   */
  private Statement parseForeach()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      expect('(');

      Expr objExpr = parseTopExpr();

      expect(AS);

      boolean isRef = false;

      int token = parseToken();
      if (token == '&') {
	isRef = true;
	token = parseToken();
      }

      if (token != '$')
	throw error(L.l("expected variable in foreach"));

      String name = parseIdentifier();

      VarExpr keyVar = null;
      VarExpr valueVar = null;

      token = parseToken();

      if (token == ARRAY_RIGHT) {
	if (isRef)
	  throw error(L.l("key reference is forbidden in foreach"));
	
	keyVar = new VarExpr(_function.createVar(name));

	token = parseToken();

	if (token == '&') {
	  isRef = true;
	  token = parseToken();
	}

	if (token != '$')
	  throw error(L.l("expected variable in foreach"));

	name = parseIdentifier();

	token = parseToken();
      }

      if (token == ')') {
	valueVar = new VarExpr(_function.createVar(name));
      }
      else
	throw error(L.l("expected ')' in foreach"));
    
      Statement block = parseStatement();

      return new ForeachStatement(objExpr, keyVar, valueVar, isRef, block);
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses a function definition
   */
  private Function parseFunctionDefinition()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;
    
    boolean oldReturnsReference = _returnsReference;
    FunctionInfo oldFunction = _function;

    try {
      _returnsReference = false;
    
      int token = parseToken();

      if (token == '&')
	_returnsReference = true;
      else
	_peekToken = token;

      String name = parseIdentifier();

      _function = new FunctionInfo(_quercus, name);
      _function.setDeclaringClass(_quercusClass);
      
      _function.setReturnsReference(_returnsReference);

      expect('(');

      ArrayList<Arg> args = parseFunctionArgDefinition();
      
      expect(')');
      
      expect('{');
      
      ArrayList<Statement> statementList = null;

      statementList = parseStatementList();
    
      expect('}');

      Function function;

      if (_quercusClass != null)
	function = new ObjectMethod(_quercusClass,
				    name, _function,
				    args, statementList);
      else
	function = new Function(name, _function,
				args, statementList);

      function.setGlobal(oldTop);

      _scope.addFunction(name, function);

      /*
    com.caucho.vfs.WriteStream out = com.caucho.vfs.Vfs.lookup("stdout:").openWrite();
    out.setFlushOnNewline(true);
    function.debug(new JavaWriter(out));
      */

      return function;
    } finally {
      _returnsReference = oldReturnsReference;
      _function = oldFunction;
      _isTop = oldTop;
    }
  }

  private ArrayList<Arg> parseFunctionArgDefinition()
    throws IOException
  {
    ArrayList<Arg> args = new ArrayList<Arg>();
    int argIndex = 0;

    while (true) {
      int token = parseToken();
      boolean isReference = false;

      if (token == '&') {
	isReference = true;
	token = parseToken();
      }
	
      if (token != '$') {
	_peekToken = token;
	break;
      }
      
      String argName = parseIdentifier();
      Expr defaultExpr = null;

      token = parseToken();
      if (token == '=') {
	// XXX: actually needs to be primitive
	defaultExpr = parseTerm();
	
	token = parseToken();
      }

      Arg arg = new Arg(argName, defaultExpr, isReference);

      args.add(arg);

      VarInfo var = _function.createVar(argName);
      var.setArgument(true);
      var.setArgumentIndex(args.size() - 1);
      var.setRef(isReference);
      
      if (token != ',') {
	_peekToken = token;
	break;
      }
    }

    return args;
  }

  /**
   * Parses the 'return' statement
   */
  private Statement parseReturn()
    throws IOException
  {
    int token = parseToken();

    switch (token) {
    case ';':
      _peekToken = token;

      return new ReturnStatement(NullLiteralExpr.NULL);
      
    default:
      _peekToken = token;

      Expr expr = parseTopExpr();

      /*
      if (_returnsReference)
	expr = expr.createRef();
      else
	expr = expr.createCopy();
      */

      if (_returnsReference)
	return new ReturnRefStatement(expr);
      else
	return new ReturnStatement(expr);
    }
  }

  /**
   * Parses a class definition
   */
  private InterpretedClassDef parseClassDefinition()
    throws IOException
  {
    String name = parseIdentifier();

    String parentName = null;

    int token = parseToken();
    if (token == EXTENDS) {
      parentName = parseIdentifier();
    }
    else
      _peekToken = token;

    InterpretedClassDef oldClass = _quercusClass;
    Scope oldScope = _scope;

    try {
      _quercusClass = oldScope.addClass(name, parentName);
    
      _scope = new ClassScope(_quercusClass);

      expect('{');

      parseClassContents();

      expect('}');

      return _quercusClass;
    } finally {
      _quercusClass = oldClass;
      _scope = oldScope;
    }
  }

  /**
   * Parses a statement list.
   */
  private void parseClassContents()
    throws IOException
  {
    ArrayList<Statement> statements = new ArrayList<Statement>();

    while (true) {
      int token = parseToken();

      switch (token) {
      case ';':
	break;

      case FUNCTION:
	{
	  Function fun = parseFunctionDefinition();
	  fun.setStatic(false);
	  break;
	}

      case CLASS:
	parseClassDefinition();
	break;

	/* quercus/0260
      case VAR:
	parseClassVarDefinition(false);
	break;
	*/

      case PUBLIC:
      case PRIVATE:
      case PROTECTED:
      case STATIC:
	{
	  boolean isStatic = token == STATIC;
	  
	  int token2 = parseToken();

	  if (token2 == FUNCTION) {
	    Function fun = parseFunctionDefinition();

	    fun.setStatic(isStatic);
	  }
	  else {
	    _peekToken = token2;
	    
	    parseClassVarDefinition(isStatic);
	  }
	}
	break;

      case IDENTIFIER:
	if (_lexeme.equals("var")) {
	  parseClassVarDefinition(false);
	}
	else {
	  _peekToken = token;
	  return;
	}
	break;

      case -1:
      case '}':
      default:
	_peekToken = token;
	return;
      }
    }
  }

  /**
   * Parses a function definition
   */
  private void parseClassVarDefinition(boolean isStatic)
    throws IOException
  {
    int token;
    
    do {
      expect('$');
      String name = parseIdentifier();

      token = parseToken();

      Expr expr = null;

      if (token == '=') {
	expr = parseExpr();
      }
      else {
	_peekToken = token;
	expr = NullLiteralExpr.NULL;
      }

      if (isStatic)
	((ClassScope) _scope).addStaticVar(name, expr);
      else
	((ClassScope) _scope).addVar(name, expr);

      token = parseToken();
    } while (token == ',');

    _peekToken = token;
  }

  /**
   * Parses an expression statement.
   */
  private Statement parseExprStatement()
    throws IOException
  {
    Expr expr = parseTopExpr();

    Statement statement = new ExprStatement(expr);

    int token = parseToken();
    _peekToken = token;

    switch (token) {
    case -1:
    case ';':
    case '}':
    case PHP_END:
    case TEXT:
    case TEXT_ECHO:
      break;

    default:
      expect(';');
      break;
    }
    
    return statement;
  }

  /**
   * Parses a top expression.
   */
  private Expr parseTopExpr()
    throws IOException
  {
    Expr expr = parseExpr();

    return expr;
  }

  /**
   * Parses a top expression.
   */
  private Expr parseTopCommaExpr()
    throws IOException
  {
    return parseCommaExpr();
  }

  /**
   * Parses a comma expression.
   */
  private Expr parseCommaExpr()
    throws IOException
  {
    Expr expr = parseExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case ',':
	expr = new CommaExpr(expr, parseExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses an expression with optional '&'.
   */
  private Expr parseRefExpr()
    throws IOException
  {
    int token = parseToken();

    boolean isRef = token == '&';

    if (! isRef)
      _peekToken = token;
    
    Expr expr = parseExpr();

    if (isRef)
      expr = new RefExpr(expr);

    return expr;
  }

  /**
   * Parses an expression.
   */
  private Expr parseExpr()
    throws IOException
  {
    return parseWeakOrExpr();
  }

  /**
   * Parses a logical xor expression.
   */
  private Expr parseWeakOrExpr()
    throws IOException
  {
    Expr expr = parseWeakXorExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case OR_RES:
	expr = new OrExpr(expr, parseWeakXorExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a logical xor expression.
   */
  private Expr parseWeakXorExpr()
    throws IOException
  {
    Expr expr = parseWeakAndExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case XOR_RES:
	expr = new XorExpr(expr, parseWeakAndExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a logical and expression.
   */
  private Expr parseWeakAndExpr()
    throws IOException
  {
    Expr expr = parseAssignExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case AND_RES:
	expr = new AndExpr(expr, parseAssignExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses an assignment expression.
   */
  private Expr parseAssignExpr()
    throws IOException
  {
    Expr expr = parseConditionalExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '=':
	token = parseToken();
	
	try {
	  if (token == '&')
	    expr = expr.createAssignRef(this, parseAssignExpr());
	  else {
	    _peekToken = token;
	    expr = expr.createAssign(this, parseAssignExpr());
	  }
	} catch (PhpParseException e) {
	  throw e;
	} catch (IOException e) {
	  throw error(e.getMessage());
	}
	break;
	
      case PLUS_ASSIGN:
        expr = expr.createAssign(this, new AddExpr(expr, parseAssignExpr()));
	break;
	
      case MINUS_ASSIGN:
        expr = expr.createAssign(this, new SubExpr(expr, parseAssignExpr()));
	break;
	
      case APPEND_ASSIGN:
        expr = expr.createAssign(this, AppendExpr.create(expr, parseAssignExpr()));
	break;
	
      case MUL_ASSIGN:
        expr = expr.createAssign(this, new MulExpr(expr, parseAssignExpr()));
	break;
	
      case DIV_ASSIGN:
        expr = expr.createAssign(this, new DivExpr(expr, parseAssignExpr()));
	break;
	
      case MOD_ASSIGN:
        expr = expr.createAssign(this, new ModExpr(expr, parseAssignExpr()));
	break;
	
      case LSHIFT_ASSIGN:
        expr = expr.createAssign(this, new LeftShiftExpr(expr, parseAssignExpr()));
	break;
	
      case RSHIFT_ASSIGN:
        expr = expr.createAssign(this, new RightShiftExpr(expr, parseAssignExpr()));
	break;
	
      case AND_ASSIGN:
        expr = expr.createAssign(this, new BitAndExpr(expr, parseAssignExpr()));
	break;
	
      case OR_ASSIGN:
        expr = expr.createAssign(this, new BitOrExpr(expr, parseAssignExpr()));
	break;
	
      case XOR_ASSIGN:
        expr = expr.createAssign(this, new BitXorExpr(expr, parseAssignExpr()));
	break;
	
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a conditional expression.
   */
  private Expr parseConditionalExpr()
    throws IOException
  {
    Expr expr = parseOrExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '?':
	Expr trueExpr = parseExpr();
	expect(':');
	expr = new ConditionalExpr(expr, trueExpr, parseOrExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a logical or expression.
   */
  private Expr parseOrExpr()
    throws IOException
  {
    Expr expr = parseAndExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case C_OR:
	expr = new OrExpr(expr, parseAndExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a logical and expression.
   */
  private Expr parseAndExpr()
    throws IOException
  {
    Expr expr = parseBitOrExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case C_AND:
	expr = new AndExpr(expr, parseBitOrExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a bit or expression.
   */
  private Expr parseBitOrExpr()
    throws IOException
  {
    Expr expr = parseBitXorExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '|':
	expr = new BitOrExpr(expr, parseBitXorExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a bit xor expression.
   */
  private Expr parseBitXorExpr()
    throws IOException
  {
    Expr expr = parseBitAndExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '^':
	expr = new BitXorExpr(expr, parseBitAndExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a bit and expression.
   */
  private Expr parseBitAndExpr()
    throws IOException
  {
    Expr expr = parseEqExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '&':
	expr = new BitAndExpr(expr, parseEqExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a comparison expression.
   */
  private Expr parseEqExpr()
    throws IOException
  {
    Expr expr = parseCmpExpr();

    int token = parseToken();

    switch (token) {
    case EQ:
      return new EqExpr(expr, parseCmpExpr());
      
    case NEQ:
      return new NeqExpr(expr, parseCmpExpr());
      
    case EQUALS:
      return new EqualsExpr(expr, parseCmpExpr());
      
    case NEQUALS:
      return new NotExpr(new EqualsExpr(expr, parseCmpExpr()));
      
    default:
      _peekToken = token;
      return expr;
    }
  }

  /**
   * Parses a comparison expression.
   */
  private Expr parseCmpExpr()
    throws IOException
  {
    Expr expr = parseShiftExpr();

    int token = parseToken();

    switch (token) {
    case '<':
      return new LtExpr(expr, parseShiftExpr());

    case '>':
      return new GtExpr(expr, parseShiftExpr());

    case LEQ:
      return new LeqExpr(expr, parseShiftExpr());

    case GEQ:
      return new GeqExpr(expr, parseShiftExpr());
      
    default:
      _peekToken = token;
      return expr;
    }
  }

  /**
   * Parses a left/right shift expression.
   */
  private Expr parseShiftExpr()
    throws IOException
  {
    Expr expr = parseAddExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case LSHIFT:
	expr = new LeftShiftExpr(expr, parseAddExpr());
	break;
      case RSHIFT:
	expr = new RightShiftExpr(expr, parseAddExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses an add/substract expression.
   */
  private Expr parseAddExpr()
    throws IOException
  {
    Expr expr = parseMulExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '+':
	expr = new AddExpr(expr, parseMulExpr());
	break;
      case '-':
	expr = new SubExpr(expr, parseMulExpr());
	break;
      case '.':
	expr = AppendExpr.create(expr, parseMulExpr());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a multiplication/division expression.
   */
  private Expr parseMulExpr()
    throws IOException
  {
    Expr expr = parseTerm();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '*':
	expr = new MulExpr(expr, parseTerm());
	break;
      case '/':
	expr = new DivExpr(expr, parseTerm());
	break;
      case '%':
	expr = new ModExpr(expr, parseTerm());
	break;
      default:
	_peekToken = token;
	return expr;
      }
    }
  }

  /**
   * Parses a basic term.
   *
   * <pre>
   * term ::= termBase
   *      ::= term '[' index ']'
   *      ::= term '{' index '}'
   * </pre>
   */
  private Expr parseTerm()
    throws IOException
  {
    Expr term = parseTermBase();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '[':
        {
	  token = parseToken();
	  
	  if (token == ']') {
	    term = new ArrayTailExpr(term);
	  }
	  else {
	    _peekToken = token;
	    Expr index = parseExpr();
	    token = parseToken();

	    term = new ArrayGetExpr(term, index);
	  }

          if (token != ']')
            throw expect("']'", token);
        }
        break;
	
      case '{':
        {
          Expr index = null;
	  
	  index = parseExpr();

	  expect('}');

          term = new CharAtExpr(term, index);
        }
        break;

      case INCR:
	term = new PostIncrementExpr(term, 1);
	break;

      case DECR:
	term = new PostIncrementExpr(term, -1);
	break;

      case DEREF:
	term = parseDeref(term);
        break;

      case '(':
	_peek = token;
	term = parseFunction(term);
        break;

      case '=':
	token = parseToken();
	
	try {
	  if (token == '&')
	    return term.createAssignRef(this, parseAssignExpr());
	  else {
	    _peekToken = token;
	    return term.createAssign(this, parseAssignExpr());
	  }
	} catch (PhpParseException e) {
	  throw e;
	} catch (IOException e) {
	  throw error(e.getMessage());
	}

	// XXX: other assignment

      default:
        _peekToken = token;
        return term;
      }
    }
  }

  
  /**
   * Parses a deref
   *
   * <pre>
   * deref ::= term -> IDENTIFIER
   *       ::= term -> IDENTIFIER '(' args ')'
   * </pre>
   */
  private Expr parseDeref(Expr term)
    throws IOException
  {
    String name = null;
    Expr nameExpr = null;

    int token = parseToken();
    
    if (token == '$') {
      _peekToken = token;
      nameExpr = parseTermBase();
    }
    else if (token == '{') {
      nameExpr = parseExpr();
      expect('}');
    }
    else {
      _peekToken = token;
      name = parseIdentifier();
    }

    token = parseToken();
    if (token == '(') {
      ArrayList<Expr> args = new ArrayList<Expr>();

      parseFunctionArgs(args);

      if (nameExpr != null)
	return new VarMethodCallExpr(term, nameExpr, args);
      else if (term instanceof ThisExpr)
	return new ThisMethodCallExpr(term, name, args);
      else
	return new MethodCallExpr(term, name, args);
    }
    else if (nameExpr != null) {
      _peekToken = token;

      return term.createFieldGet(nameExpr);
    }
    else {
      _peekToken = token;

      return term.createFieldGet(name);
    }
  }
  
  /**
   * Parses a basic term.
   *
   * <pre>
   * term ::= STRING
   *      ::= LONG
   *      ::= DOUBLE
   * </pre>
   */
  private Expr parseTermBase()
    throws IOException
  {
    int token = parseToken();

    switch (token) {
    case STRING:
      return new StringLiteralExpr(_lexeme);
      
    case SYSTEM_STRING:
      {
	ArrayList<Expr> args = new ArrayList<Expr>();
	args.add(new StringLiteralExpr(_lexeme));
	return new FunctionExpr("system", args);
      }
      
    case SIMPLE_SYSTEM_STRING:
      {
	ArrayList<Expr> args = new ArrayList<Expr>();
	args.add(parseEscapedString(_lexeme, SIMPLE_STRING_ESCAPE, true));
	return new FunctionExpr("system", args);
      }

    case COMPLEX_SYSTEM_STRING:
      {
	ArrayList<Expr> args = new ArrayList<Expr>();
	args.add(parseEscapedString(_lexeme, COMPLEX_STRING_ESCAPE, true));
	return new FunctionExpr("system", args);
      }
      
    case SIMPLE_STRING_ESCAPE:
    case COMPLEX_STRING_ESCAPE:
      return parseEscapedString(_lexeme, token, false);
      
    case LONG:
      return new LiteralExpr(LongValue.create(Long.parseLong(_lexeme)));
      
    case DOUBLE:
      return new LiteralExpr(new DoubleValue(Double.parseDouble(_lexeme)));

    case NULL:
      return NullLiteralExpr.NULL;

    case TRUE:
      return new LiteralExpr(BooleanValue.TRUE);

    case FALSE:
      return new LiteralExpr(BooleanValue.FALSE);

    case '$':
      Expr var = parseVariable();

      return var;

      /* quercus/0211
    case '&':
      {
	Expr expr = parseTerm();
	
	return expr.createRef();
      }
      */

    case '-':
      {
        Expr expr = parseTerm();

        return new MinusExpr(expr);
      }

    case '+':
      {
        Expr expr = parseTerm();

        return new PlusExpr(expr);
      }

    case '!':
      {
	// XXX: quercus/03i3 vs quercus/03i4
	
        Expr expr = parseTerm();

	token = parseToken();

	if (token == '=') {
	  return new NotExpr(expr.createAssign(this, parseAssignExpr()));
	}
	else {
	  _peekToken = token;
	  
	  return new NotExpr(expr);
	}
      }

    case '~':
      {
        Expr expr = parseTerm();

        return new BitNotExpr(expr);
      }

    case '@':
      {
        Expr expr = parseTerm();

	return new SuppressErrorExpr(expr);
      }

    case INCR:
      {
        Expr expr = parseTerm();

        return new PreIncrementExpr(expr, 1);
      }

    case DECR:
      {
        Expr expr = parseTerm();

        return new PreIncrementExpr(expr, -1);
      }

    case NEW:
      return parseNew();

    case INCLUDE:
    case REQUIRE:
      return new IncludeExpr(_sourceFile, parseExpr());
    case INCLUDE_ONCE:
    case REQUIRE_ONCE:
      return new IncludeOnceExpr(_sourceFile, parseExpr());

    case LIST:
      return parseList();
      
    case EXIT:
      return parseExit();

    case IDENTIFIER:
      {
	if (_lexeme.equals("new"))
	  return parseNew();
	    
        token = parseToken();

        _peekToken = token;

	String className = null;
	String name = _lexeme;

	if (token == SCOPE) {
	  _peekToken = -1;
	  
	  className = name;

	  token = parseToken();
	  if (token == '$')
	    return parseStaticClassField(className);
	  else
	    _peekToken = token;
	  
	  name = parseIdentifier();

	  token = parseToken();
	  _peekToken = token;
	}
	  
        if (token == '(')
          return parseFunction(className, name);
        else
          return parseConstant(name);
      }

    case '(':
      {
	Expr expr = parseExpr();

	expect(')');

	if (expr instanceof ConstExpr) {
	  String type = ((ConstExpr) expr).getVar();
	  
	  if ("bool".equals(type) || "boolean".equals(type))
	    return new ToBooleanExpr(parseTerm());
	  else if ("int".equals(type) || "integer".equals(type))
	    return new ToLongExpr(parseTerm());
	  else if ("float".equals(type)
		   || "double".equals(type)
		   || "real".equals(type))
	    return new ToDoubleExpr(parseTerm());
	  else if ("string".equals(type))
	    return new ToStringExpr(parseTerm());
	  else if ("object".equals(type))
	    return new ToObjectExpr(parseTerm());
	  else if ("array".equalsIgnoreCase(type))
	    return new ToArrayExpr(parseTerm());
	}

	return expr;
      }

    default:
      throw error(L.l("{0} is an unexpected token, expected an expression.",
		      tokenName(token)));
    }
  }

  /**
   * Parses the next variable
   */
  private Expr parseVariable()
    throws IOException
  {
    int token = parseToken();

    if (token == THIS) {
      return new ThisExpr(_quercusClass);
    }
    else if (token == '$') {
      _peekToken = token;

      return new VarVarExpr(parseTermBase());
    }
    else if (_lexeme == null)
      throw error(L.l("Expected identifier at '{0}'", tokenName(token)));

    return new VarExpr(_function.createVar(_lexeme));
  }

  /**
   * Parses the next function
   */
  private Expr parseFunction(String className, String name)
    throws IOException
  {
    if (name.equalsIgnoreCase("array"))
      return parseArrayFunction();

    int token = parseToken();

    if (token != '(')
      throw error(L.l("Expected '('"));

    ArrayList<Expr> args = new ArrayList<Expr>();

    parseFunctionArgs(args);

    if (className == null) {
      if (name.equals("each")) {
	if (args.size() != 1)
	  throw error(L.l("each requires a single expression"));
      
	return new EachExpr(args.get(0));
      }

      FunctionExpr expr = new FunctionExpr(name, args);
      
      expr.setLocation(getFileName(), getLine());

      return expr;
    }
    else if (className.equals("parent")) {
      className = _quercusClass.getParentName();
      
      return new ClassMethodExpr(className, name, args);
    }
    else if (className.equals("self")) {
      className = _quercusClass.getName();
      
      return new ClassMethodExpr(className, name, args);
    }
    else {
      return new StaticMethodExpr(className, name, args);
    }
  }

  /**
   * Parses the next constant
   */
  private Expr parseConstant(String name)
    throws IOException
  {
    if (name.equals("__FILE__"))
      return new StringLiteralExpr(_file);
    else if (name.equals("__LINE__"))
      return new LongLiteralExpr(_line);
    else
      return new ConstExpr(name);
  }

  /**
   * Parses the next constant
   */
  private Expr parseStaticClassField(String className)
    throws IOException
  {
    String var = parseIdentifier();

    return new StaticFieldGetExpr(className, var);
  }
  
  private ArrayList<Expr> parseArgs()
    throws IOException
  {
    ArrayList<Expr> args = new ArrayList<Expr>();

    parseFunctionArgs(args);

    return args;
  }
  
  private void parseFunctionArgs(ArrayList<Expr> args)
    throws IOException
  {
    int token;
    
    while ((token = parseToken()) > 0 && token != ')') {
      boolean isRef = false;
      
      if (token == '&')
	isRef = true;
      else
	_peekToken = token;

      Expr expr = parseExpr();

      if (isRef)
	expr = expr.createRef();
      
      args.add(expr);

      token = parseToken();
      if (token == ')')
	break;
      else if (token != ',')
	throw expect("','", token);
    }
  }

  /**
   * Parses the next function
   */
  private Expr parseFunction(Expr name)
    throws IOException
  {
    expect('(');

    ArrayList<Expr> args = new ArrayList<Expr>();

    parseFunctionArgs(args);

    return new VarFunctionExpr(name, args);
  }

  /**
   * Parses the new expression
   */
  private Expr parseNew()
    throws IOException
  {
    int token = parseToken();

    String name = null;
    Expr nameExpr = null;

    if (token == IDENTIFIER)
      name = _lexeme;
    else {
      _peekToken = token;
      
      nameExpr = parseTermBase();
    }

    token = parseToken();
    
    ArrayList<Expr> args = new ArrayList<Expr>();

    if (token != '(')
      _peekToken = token;
    else {
      while ((token = parseToken()) > 0 && token != ')') {
	_peekToken = token;

	args.add(parseExpr());

	token = parseToken();
	if (token == ')')
	  break;
	else if (token != ',')
	  throw error(L.l("expected ','"));
      }
    }

    if (name != null)
      return new NewExpr(name, args);
    else
      return new VarNewExpr(nameExpr, args);
  }

  /**
   * Parses the include expression
   */
  private Expr parseInclude()
    throws IOException
  {
    Expr name = parseExpr();

    return new IncludeExpr(_sourceFile, name);
  }

  /**
   * Parses the list(...) = value expression
   */
  private Expr parseList()
    throws IOException
  {
    expect('(');

    int peek = parseToken();

    ArrayList<Expr> leftVars = new ArrayList<Expr>();

    while (peek > 0 && peek != ')') {
      if (peek != ',') {
	_peekToken = peek;

	Expr left = parseTerm();

	leftVars.add(left);

	peek = parseToken();
      }
      else {
	leftVars.add(null);
      }

      if (peek == ',')
	peek = parseToken();
      else
	break;
    }

    if (peek != ')')
      throw error(L.l("expected ')'"));

    expect('=');

    Expr value = parseAssignExpr();

    return ListExpr.create(this, leftVars, value);
  }

  /**
   * Parses the exit/die expression
   */
  private Expr parseExit()
    throws IOException
  {
    int token = parseToken();

    if (token == '(') {
      ArrayList<Expr> args = parseArgs();

      if (args.size() > 0)
	return new ExitExpr(args.get(0));
      else
	return new ExitExpr();
    }
    else {
      _peekToken = token;
      
      return new ExitExpr();
    }
  }

  /**
   * Parses the array() expression
   */
  private Expr parseArrayFunction()
    throws IOException
  {
    String name = _lexeme;

    int token = parseToken();

    if (token != '(')
      throw error(L.l("Expected '('"));

    ArrayList<Expr> keys = new ArrayList<Expr>();
    ArrayList<Expr> values = new ArrayList<Expr>();

    while ((token = parseToken()) > 0 && token != ')') {
      _peekToken = token;

      Expr value = parseRefExpr();

      token = parseToken();

      if (token == ARRAY_RIGHT) {
	Expr key = value;

	value = parseRefExpr();

	keys.add(key);
	values.add(value);

	token = parseToken();
      }
      else {
	keys.add(null);
	values.add(value);
      }
      
      if (token == ')')
	break;
      else if (token != ',')
	throw error(L.l("expected ','"));
    }

    return new ArrayFunExpr(keys, values);
  }

  private String parseIdentifier()
    throws IOException
  {
    int token = parseToken();

    switch (token) {
    case IDENTIFIER:
      return _lexeme;

    case DEFAULT:
    case CLASS:
    case PROTECTED:
    case PUBLIC:
    case FUNCTION:
    case LIST:
    case CASE:
    case TRUE:
    case FALSE:
    case RETURN:
      return _lexeme;
      
    default:
      throw error(L.l("expected identifier at {0}.", tokenName(token)));
    }
  }

  /**
   * Parses the next token.
   */
  private int parseToken()
    throws IOException
  {
    int peekToken = _peekToken;
    if (peekToken > 0) {
      _peekToken = 0;
      return peekToken;
    }

    while (true) {
      int ch = read();

      switch (ch) {
      case -1:
	return -1;

      case ' ': case '\t': case '\n': case '\r':
	break;

      case '#':
	while ((ch = read()) != '\n' && ch != '\r' && ch >= 0) {
	  if (ch != '?') {
	  }
	  else if ((ch = read()) != '>')
	    _peek = ch;
	  else {
	    ch = read();
	    if (ch == '\r')
	      ch = read();
	    if (ch != '\n')
	      _peek = ch;
    
	    return parsePhpText();
	  }
	}
	break;

      case '"':
	return parseEscapedString('"');

      case '`':
	{
	  int token = parseEscapedString('`');

	  switch (token) {
	  case STRING:
	    return SYSTEM_STRING;
	  case SIMPLE_STRING_ESCAPE:
	    return SIMPLE_SYSTEM_STRING;
	  case COMPLEX_STRING_ESCAPE:
	    return COMPLEX_SYSTEM_STRING;
	  default:
	    throw new IllegalStateException();
	  }
	}
	
      case '\'':
	parseStringToken('\'');
	return STRING;

      case ';': case '$': case '(': case ')': case '@':
      case '[': case ']': case ',': case '{': case '}':
      case '~':
	return ch;

      case '+':
	ch = read();
	if (ch == '=')
	  return PLUS_ASSIGN;
	else if (ch == '+')
	  return INCR;
	else
	  _peek = ch;

	return '+';

      case '-':
	ch = read();
	if (ch == '>')
	  return DEREF;
	else if (ch == '=')
	  return MINUS_ASSIGN;
	else if (ch == '-')
	  return DECR;
	else
	  _peek = ch;
	
	return '-';

      case '*':
	ch = read();
	if (ch == '=')
	  return MUL_ASSIGN;
	else
	  _peek = ch;

	return '*';

      case '/':
	ch = read();
	if (ch == '=')
	  return DIV_ASSIGN;
	else if (ch == '/') {
	  while ((ch = read()) != '\n' && ch != '\r' && ch >= 0) {
	  }
	  break;
	}
	else if (ch == '*') {
	  skipMultilineComment();
	  break;
	}
	else
	  _peek = ch;

	return '/';

      case '%':
	ch = read();
	if (ch == '=')
	  return MOD_ASSIGN;
	else
	  _peek = ch;

	return '%';

      case ':':
	ch = read();
	if (ch == ':')
	  return SCOPE;
	else
	  _peek = ch;
	
	return ':';
	
      case '=':
	ch = read();
	if (ch == '=') {
	  ch = read();
	  if (ch == '=')
	    return EQUALS;
	  else {
	    _peek = ch;
	    return EQ;
	  }
	}
	else if (ch == '>')
	  return ARRAY_RIGHT;
	else {
	  _peek = ch;
	  return '=';
	}
	
      case '!':
	ch = read();
	if (ch == '=') {
	  ch = read();
	  if (ch == '=')
	    return NEQUALS;
	  else {
	    _peek = ch;
	    return NEQ;
	  }
	}
	else {
	  _peek = ch;
	  return '!';
	}

      case '&':
	ch = read();
	if (ch == '&')
	  return C_AND;
	else if (ch == '=')
	  return AND_ASSIGN;
	else {
	  _peek = ch;
	  return '&';
	}

      case '^':
	ch = read();
	if (ch == '=')
	  return XOR_ASSIGN;
	else
	  _peek = ch;
	
	return '^';

      case '|':
	ch = read();
	if (ch == '|')
	  return C_OR;
	else if (ch == '=')
	  return OR_ASSIGN;
	else {
	  _peek = ch;
	  return '|';
	}

      case '<':
	ch = read();
	if (ch == '<') {
	  ch = read();

	  if (ch == '=')
	    return LSHIFT_ASSIGN;
	  else if (ch == '<') {
	    return parseHeredocToken();
	  }
	  else
	    _peek = ch;
	  
	  return LSHIFT;
	}
	else if (ch == '=')
	  return LEQ;
	else if (ch == '>')
	  return NEQ;
	else if (ch == '/') {
	  StringBuilder sb = new StringBuilder();
	    
	  if (! parseTextMatch(sb, "script"))
	    throw error(L.l("expected 'script' at '{0}'", sb));

	  expect('>');

	  return parsePhpText();
	}
	else
	  _peek = ch;

	return '<';

      case '>':
	ch = read();
	if (ch == '>') {
	  ch = read();

	  if (ch == '=')
	    return RSHIFT_ASSIGN;
	  else
	    _peek = ch;
	  
	  return RSHIFT;
	}
	else if (ch == '=')
	  return GEQ;
	else
	  _peek = ch;

	return '>';

      case '?':
	ch = read();
	if (ch == '>') {
	  ch = read();
	  if (ch == '\r')
	    ch = read();
	  if (ch != '\n')
	    _peek = ch;
    
	  return parsePhpText();
        }
	else
	  _peek = ch;

	return '?';

      case '.':
	ch = read();

	if (ch == '=')
	  return APPEND_ASSIGN;
	
	_peek = ch;
	
	if ('0' <= ch && ch <= '9')
	  return parseNumberToken('.');
	else
	  return '.';

      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
	return parseNumberToken(ch);
	
      default:
	if ('a' <= ch && ch <= 'z' ||
	    'A' <= ch && ch <= 'Z' ||
	    ch == '_') {
	  _sb.setLength(0);
	  _sb.append((char) ch);

	  for (ch = read();
	       ('a' <= ch && ch <= 'z' ||
		'A' <= ch && ch <= 'Z' ||
		'0' <= ch && ch <= '9' ||
		ch == '_');
	       ch = read()) {
	    _sb.append((char) ch);
	  }

	  _peek = ch;

	  _lexeme = _sb.toString();

	  int reserved = _reserved.get(_lexeme);

	  if (reserved > 0)
	    return reserved;

	  reserved = _insensitiveReserved.get(_lexeme.toLowerCase());
	  if (reserved > 0)
	    return reserved;
	  else
	    return IDENTIFIER;
	}
        
	throw error("unknown lexeme:" + (char) ch);
      }
    }
  }

  /**
   * Skips a multiline comment.
   */
  private void skipMultilineComment()
    throws IOException
  {
    int ch;

    while ((ch = read()) >= 0) {
      if (ch != '*') {
      }
      else if ((ch = read()) == '/')
	return;
      else
	_peek = ch;
    }
  }

  /**
   * Parses quercus text
   */
  private int parsePhpText()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    int ch = read();
    while (ch > 0) {
      if (ch == '<') {
	if ((ch = read()) == 's' || ch == 'S') {
	  _peek = ch;
	  if (parseScriptBegin(sb)) {
	    return TEXT;
	  }
	  ch = read();
	}
	else if (ch != '?') {
	  sb.append('<');
	}
	else if ((ch = read()) == '=') {
	  _lexeme = sb.toString();

	  return TEXT_ECHO;
	}
	else if (Character.isWhitespace(ch)) {
	  _lexeme = sb.toString();

	  return TEXT;
	}
	else if (ch != 'p') {
	  sb.append("<?");
	}
	else if ((ch = read()) != 'h') {
	  sb.append("<?p");
	}
	else if ((ch = read()) != 'p') {
	  sb.append("<?ph");
	}
	else if (! Character.isWhitespace((ch = read()))) {
	  sb.append("<?php");
	}
	else {
	  _lexeme = sb.toString();

	  return TEXT;
	}
      }
      else {
	sb.append((char) ch);

	ch = read();
      }
    }
    
    _lexeme = sb.toString();

    return TEXT;
  }

  /**
   * Parses the <script language="quercus"> opening
   */
  private boolean parseScriptBegin(StringBuilder sb)
    throws IOException
  {
    int begin = sb.length();
    
    sb.append('<');

    if (! parseTextMatch(sb, "script"))
      return false;

    parseWhitespace(sb);
    
    if (! parseTextMatch(sb, "language"))
      return false;
    
    parseWhitespace(sb);
    
    if (! parseTextMatch(sb, "=\"quercus\""))
      return false;

    parseWhitespace(sb);

    int ch = read();

    if (ch == '>') {
      sb.setLength(begin);
      return true;
    }
    else {
      _peek = ch;
      return false;
    }
  }
    
  private boolean parseTextMatch(StringBuilder sb, String text)
    throws IOException
  {
    int len = text.length();

    for (int i = 0; i < len; i++) {
      int ch = read();

      if (ch < 0)
	return false;

      if (Character.toLowerCase(ch) != text.charAt(i)) {
	_peek = ch;
	return false;
      }
      else
	sb.append((char) ch);
    }

    return true;
  }
    
  private void parseWhitespace(StringBuilder sb)
    throws IOException
  {
    int ch;

    while (Character.isWhitespace((ch = read()))) {
      sb.append((char) ch);
    }

    _peek = ch;
  }
  
  /**
   * Parses the next string token.
   */
  private void parseStringToken(int end)
    throws IOException
  {
    _sb.setLength(0);

    int ch;

    for (ch = read(); ch >= 0 && ch != end; ch = read()) {
      if (ch == '\\') {
        ch = read();

	if (end == '"') {
	  _sb.append('\\');

	  if (ch >= 0)
	    _sb.append((char) ch);
	}
	else {
	  switch (ch) {
	  case '\'': case '\\': case '\"':
	    _sb.append((char) ch);
	    break;
	  default:
	    _sb.append('\\');
	    _sb.append((char) ch);
	    break;
	  }
	}
      }
      else
        _sb.append((char) ch);
    }

    _lexeme = _sb.toString();
  }

  /**
   * Parses the next heredoc token.
   */
  private int parseHeredocToken()
    throws IOException
  {
    _sb.setLength(0);

    int ch;
    while ((ch = read()) >= 0 && ch != '\r' && ch != '\n') {
      _sb.append((char) ch);
    }

    _heredocEnd = _sb.toString();

    if (ch == '\n') {
    }
    else if (ch == '\r') {
      ch = read();
      if (ch != '\n')
	_peek = ch;
    }
    else
      _peek = ch;

    return parseEscapedString('"');
  }

  /**
   * Parses the next string
   */
  private Expr parseEscapedString(String prefix, int token, boolean isSystem)
    throws IOException
  {
    Expr expr = new StringLiteralExpr(prefix);

    while (true) {
      Expr tail;

      if (token == COMPLEX_STRING_ESCAPE) {
	tail = parseExpr();

	expect('}');
      }
      else if (token == SIMPLE_STRING_ESCAPE) {
	int ch = read();
	
	_sb.setLength(0);

	for (; ch > 0 && isIdentifierPart((char) ch); ch = read()) {
	  _sb.append((char) ch);
	}

	_peek = ch;

	String varName = _sb.toString();

	if (varName.equals("this"))
	  tail = new ThisExpr(_quercusClass);
	else
	  tail = new VarExpr(_function.createVar(varName));

	while (((ch = read()) == '[' ||  ch == '-')) {
	  if (ch == '[') {
	    tail = parseSimpleArrayTail(tail);
	  }
	  else if (ch == '-') {
	    if ((ch = read()) != '>') {
	      tail = AppendExpr.create(tail, new StringLiteralExpr("-"));
	    }
	    else if (isIdentifierPart((char) (ch = read()))) {
	      _sb.clear();
	      for (; isIdentifierPart((char) ch); ch = read()) {
		_sb.append((char) ch);
	      }

	      tail = tail.createFieldGet(_sb.toString());
	    }
	    else {
	      tail = AppendExpr.create(tail, new StringLiteralExpr("->"));
	    }

	    _peek = ch;
	  }
	}

	_peek = ch;
      }
      else
	throw new IllegalStateException();

      expr = AppendExpr.create(expr, tail);

      if (isSystem)
	token = parseEscapedString('`');
      else
	token = parseEscapedString('"');

      if (_sb.length() > 0)
	expr = AppendExpr.create(expr, new StringLiteralExpr(_sb.toString()));

      if (token == STRING)
	return expr;
    }
  }
  
  /**
   * Parses the next string
   */
  private Expr parseSimpleArrayTail(Expr tail)
    throws IOException
  {
    int ch = read();

    _sb.clear();

    if (ch == '$') {
      for (ch = read();
	   ch > 0 && isIdentifierPart((char) ch);
	   ch = read()) {
	_sb.append((char) ch);
      }

      VarExpr var = new VarExpr(_function.createVar(_sb.toString()));

      tail = new ArrayGetExpr(tail, var);
    }
    else if ('0' <= ch && ch <= '9') {
      long index = ch - '0';

      for (ch = read();
	   '0' <= ch && ch <= '9';
	   ch = read()) {
	index = 10 * index + ch - '0';
      }

      tail = new ArrayGetExpr(tail, new LongLiteralExpr(index));
    }
    else if (isIdentifierPart((char) ch)) {
      for (ch = read();
	   ch > 0 && isIdentifierPart((char) ch);
	   ch = read()) {
	_sb.append((char) ch);
      }

      Expr constExpr = new ConstExpr(_sb.toString());

      tail = new ArrayGetExpr(tail, constExpr);
    }
    else
      throw error(L.l("Unexpected character at {0}.",
		      String.valueOf((char) ch)));

    if (ch != ']')
      throw error(L.l("Expected ']' at {0}.",
		      String.valueOf((char) ch)));

    return tail;
  }

  /**
   * Parses the next string
   */
  private int parseEscapedString(char end)
    throws IOException
  {
    _sb.setLength(0);
    
    int ch;

    while ((ch = read()) > 0) {
      if (_heredocEnd == null && ch == end) {
	_lexeme = _sb.toString();
	return STRING;
      }
      else if (ch == '\\') {
        ch = read();

        switch (ch) {
        case '0': case '1': case '2': case '3':
          _sb.append((char) parseOctalEscape(ch));
          break;
        case 't':
          _sb.append((char) '\t');
          break;
        case 'r':
          _sb.append((char) '\r');
          break;
        case 'n':
          _sb.append((char) '\n');
          break;
        case '\\':
        case '$':
        case '"':
        case '`':
          _sb.append((char) ch);
          break;
        case 'x':
          _sb.append((char) parseHexEscape());
          break;
        default:
          _sb.append('\\');
          _sb.append((char) ch);
	  break;
        }
      }
      else if (ch == '$') {
	ch = read();

	if (ch == '{') {
	  _peek = '$';
	  _lexeme = _sb.toString();
	  return COMPLEX_STRING_ESCAPE;
	}
	else if (isIdentifierStart((char) ch)) {
	  _peek = ch;
	  _lexeme = _sb.toString();
	  return SIMPLE_STRING_ESCAPE;
	}
	else {
	  _sb.append('$');
	  _peek = ch;
	}
      }
      else if (ch == '{') {
	ch = read();

	if (ch == '$') {
	  _peek = ch;
	  _lexeme = _sb.toString();
	  return COMPLEX_STRING_ESCAPE;
	}
	else {
	  _peek = ch;
	  _sb.append('{');
	}
      }
      /* quercus/013c
      else if ((ch == '\r' || ch == '\n') && _heredocEnd == null)
	throw error(L.l("unexpected newline in string."));
      */
      else {
        _sb.append((char) ch);

	if (_heredocEnd == null || ! _sb.endsWith(_heredocEnd)) {
	}
	else if (_sb.length() == _heredocEnd.length() ||
		 _sb.charAt(_sb.length() - _heredocEnd.length() - 1) == '\n' ||
		 _sb.charAt(_sb.length() - _heredocEnd.length() - 1) == '\r') {
	  _sb.setLength(_sb.length() - _heredocEnd.length());

	  if (_sb.length() > 0 && _sb.charAt(_sb.length() - 1) == '\n')
	    _sb.setLength(_sb.length() - 1);
	  if (_sb.length() > 0 && _sb.charAt(_sb.length() - 1) == '\r')
	    _sb.setLength(_sb.length() - 1);

	  _heredocEnd = null;
	  _lexeme = _sb.toString();
	  return STRING;
	}
      }
    }

    _lexeme = _sb.toString();

    return STRING;
  }
 
  private boolean isIdentifierStart(char ch)
  {
    return (ch >= 'a' && ch <= 'z' ||
	    ch >= 'A' && ch <= 'Z' ||
	    ch == '_');
  }
 
  private boolean isIdentifierPart(char ch)
  {
    return (ch >= 'a' && ch <= 'z' ||
	    ch >= 'A' && ch <= 'Z' ||
	    ch >= '0' && ch <= '9' ||
	    ch == '_');
  }

  private int parseOctalEscape(int ch)
    throws IOException
  {
    int value = ch - '0';

    ch = read();
    if (ch < '0' || ch > '7') {
      _peek = ch;
      return value;
    }

    value = 8 * value + ch - '0';

    ch = read();
    if (ch < '0' || ch > '7') {
      _peek = ch;
      return value;
    }

    value = 8 * value + ch - '0';

    return value;
  }

  private int parseHexEscape()
    throws IOException
  {
    int value = 0;

    while (true) {
      int ch = read();
      
      if ('0' <= ch && ch <= '9') {
	value = 16 * value + ch - '0';
      }
      else if ('a' <= ch && ch <= 'f') {
	value = 16 * value + 10 + ch - 'a';
      }
      else if ('A' <= ch && ch <= 'F') {
	value = 16 * value + 10 + ch - 'A';
      }
      else {
	_peek = ch;
	return value;
      }
    }
  }

  /**
   * Parses the next number.
   */
  private int parseNumberToken(int ch)
    throws IOException
  {
    if (ch == '0') {
      ch = read();
      if (ch == 'x' || ch == 'X')
	return parseHex();
      else if ('0' <= ch && ch <= '7')
	return parseOctal(ch);
      else {
	_peek = ch;
	ch = '0';
      }
    }
    
    _sb.setLength(0);

    int token = LONG;

    for (; '0' <= ch && ch <= '9'; ch = read()) {
      _sb.append((char) ch);
    }

    if (ch == '.') {
      token = DOUBLE;
      
      _sb.append((char) ch);
      
      for (ch = read(); '0' <= ch && ch <= '9'; ch = read()) {
	_sb.append((char) ch);
      }
    }

    if (ch == 'e' || ch == 'E') {
      token = DOUBLE;

      _sb.append((char) ch);

      ch = read();
      if (ch == '+' || ch == '-') {
	_sb.append((char) ch);
	ch = read();
      }

      if ('0' <= ch && ch <= '9') {
	for (; '0' <= ch && ch <= '9'; ch = read()) {
	  _sb.append((char) ch);
	}
      }
      else
	throw error(L.l("illegal exponent"));
    }

    _peek = ch;

    _lexeme = _sb.toString();

    return token;
  }

  /**
   * Parses the next as hex
   */
  private int parseHex()
    throws IOException
  {
    long value = 0;
    double dValue = 0;

    while (true) {
      int ch = read();

      if ('0' <= ch && ch <= '9') {
	value = 16 * value + ch - '0';
	dValue = 16 * dValue + ch - '0';
      }
      else if ('a' <= ch && ch <= 'f') {
	value = 16 * value + ch - 'a' + 10;
	dValue = 16 * dValue + ch - 'a' + 10;
      }
      else if ('A' <= ch && ch <= 'F') {
	value = 16 * value + ch - 'A' + 10;
	dValue = 16 * dValue + ch - 'A' + 10;
      }
      else {
	_peek = ch;
	break;
      }
    }

    if (value == dValue) {
      _lexeme = String.valueOf(value);
      return LONG;
    }
    else {
      _lexeme = String.valueOf(dValue);

      return DOUBLE;
    }
  }

  /**
   * Parses the next as octal
   */
  private int parseOctal(int ch)
    throws IOException
  {
    long value = 0;
    double dValue = 0;

    while (true) {
      if ('0' <= ch && ch <= '7') {
	value = 8 * value + ch - '0';
	dValue = 8 * dValue + ch - '0';
      }
      else {
	_peek = ch;
	break;
      }

      ch = read();
    }

    if (value == dValue) {
      _lexeme = String.valueOf(value);

      return LONG;
    }
    else {
      _lexeme = String.valueOf(dValue);

      return DOUBLE;
    }
  }

  private void expect(int expect)
    throws IOException
  {
    int token = parseToken();

    if (token != expect)
      throw error(L.l("expected {0} at {1}",
		      tokenName(expect),
		      tokenName(token)));
  }

  /**
   * Reads the next character.
   */
  private int read()
    throws IOException
  {
    int peek = _peek;

    if (peek >= 0) {
      _peek = -1;
      return peek;
    }

    int ch = _is.read();

    if (ch == '\r') {
      _line++;
      _hasCr = true;
    }
    else if (ch == '\n' && ! _hasCr)
      _line++;
    else
      _hasCr = false;

    return ch;
  }

  /**
   * Returns an error.
   */
  private IOException expect(String expected, int token)
  {
    return error(L.l("expected {0} at {1}", expected, tokenName(token)));
  }

  /**
   * Returns an error.
   */
  public IOException error(String msg)
  {
    String []sourceLines = Env.getSourceLine(_sourceFile, _line - 1, 3);

    if (sourceLines != null &&
	sourceLines.length > 0 &&
	sourceLines[0] != null) {
      StringBuilder sb = new StringBuilder();

      String shortFile = _file;
      int p = shortFile.lastIndexOf('/');
      if (p > 0)
	shortFile = shortFile.substring(p + 1);

      sb.append(getLocation() + msg + " in");
      for (int i = 0; i < sourceLines.length && sourceLines[i] != null; i++) {
	sb.append("\n");
	sb.append(shortFile +  ":" + (_line - 1 + i) + ": " + sourceLines[i]);
      }
    
      return new PhpParseException(sb.toString());
    }
    else
      return new PhpParseException(getLocation() + msg);
  }

  /**
   * Returns the token name.
   */
  private String tokenName(int token)
  {
    switch (token) {
    case -1:
      return "end of file";
      
    case '\'':
      return "'";

    case AS: return "'as'";
      
    case TRUE: return "true";
    case FALSE: return "false";
      
    case IF: return "'if'";
    case ELSE: return "'else'";
      
    case LIST: return "'list'";
    case CASE: return "'case'";
      
    case DEFAULT: return "'default'";
    case CLASS: return "'class'";
    case RETURN: return "'return'";
      
    case SIMPLE_STRING_ESCAPE: return "string";
    case COMPLEX_STRING_ESCAPE: return "string";
      
    case REQUIRE: return "require";
    case REQUIRE_ONCE: return "require_once";
      
    case PRIVATE: return "private";
    case PROTECTED: return "protected";
    case PUBLIC: return "public";
      
    case FUNCTION: return "function";
      
    case ARRAY_RIGHT: return "'=>'";
    case LSHIFT: return "'<<'";
      
    case IDENTIFIER:
      return "'" + _lexeme + "'";

    case LONG:
      return "integer (" + _lexeme + ")";

    case TEXT:
      return "TEXT (token " + token + ")";

    case TEXT_ECHO:
      return "<?=";
      
    default:
      if (32 <= token && token < 127)
	return "'" + (char) token + "'";
      else
	return "(token " + token + ")";
    }
  }

  private String getLocation()
  {
    return _file + ":" + _line + ": ";
  }

  static {
    _insensitiveReserved.put("echo", ECHO);
    _insensitiveReserved.put("print", PRINT);
    _insensitiveReserved.put("if", IF);
    _insensitiveReserved.put("else", ELSE);
    _insensitiveReserved.put("elseif", ELSEIF);
    _insensitiveReserved.put("do", DO);
    _insensitiveReserved.put("while", WHILE);
    _insensitiveReserved.put("for", FOR);
    _insensitiveReserved.put("function", FUNCTION);
    _insensitiveReserved.put("class", CLASS);
    // quercus/0261
    // _insensitiveReserved.put("new", NEW);
    _insensitiveReserved.put("return", RETURN);
    _insensitiveReserved.put("break", BREAK);
    _insensitiveReserved.put("continue", CONTINUE);
    // quercus/0260
    //    _insensitiveReserved.put("var", VAR);
    _insensitiveReserved.put("this", THIS);
    _insensitiveReserved.put("private", PRIVATE);
    _insensitiveReserved.put("protected", PROTECTED);
    _insensitiveReserved.put("public", PUBLIC);
    _insensitiveReserved.put("and", AND_RES);
    _insensitiveReserved.put("xor", XOR_RES);
    _insensitiveReserved.put("or", OR_RES);
    _insensitiveReserved.put("extends", EXTENDS);
    _insensitiveReserved.put("static", STATIC);
    _insensitiveReserved.put("include", INCLUDE);
    _insensitiveReserved.put("require", REQUIRE);
    _insensitiveReserved.put("include_once", INCLUDE_ONCE);
    _insensitiveReserved.put("require_once", REQUIRE_ONCE);
    _insensitiveReserved.put("unset", UNSET);
    _insensitiveReserved.put("foreach", FOREACH);
    _insensitiveReserved.put("as", AS);
    _insensitiveReserved.put("switch", SWITCH);
    _insensitiveReserved.put("case", CASE);
    _insensitiveReserved.put("default", DEFAULT);
    _insensitiveReserved.put("die", EXIT);
    _insensitiveReserved.put("exit", EXIT);
    _insensitiveReserved.put("global", GLOBAL);
    _insensitiveReserved.put("list", LIST);
    
    _insensitiveReserved.put("true", TRUE);
    _insensitiveReserved.put("false", FALSE);
    _insensitiveReserved.put("null", NULL);
  }
}

