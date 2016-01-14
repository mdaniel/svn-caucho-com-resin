/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.cf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;

/**
 * Parses the baratine .cf file.
 * 
 * <code><pre>
 * tag ::= tag-name attr1=value1 ... attrn=valuen;
 * 
 * tag ::= tag-name attr1=value1 ... attrn=valuen {
 *   tags
 * }
 * </pre></code>
 */
public class ConfigFileParser
{
  private static final L10N L = new L10N(ConfigFileParser.class);
  private static final Logger log = Logger.getLogger(ConfigFileParser.class.getName());
  
  private static final HashMap<String,Token> _reservedMap = new HashMap<>();
  private static final HashMap<String,Token> _topReservedMap = new HashMap<>();
  private static final NameCfg IMPORT = new NameCfg("", "import",
                                                "urn:java:com.caucho.baratine");

  private PathImpl _path;
  private ReadStream _is;
  private ClassLoader _loader;
  
  private ConfigContext _config;
  
  private String _lexeme;
  
  private int _line;
  private int _peek;
  
  private Token _peekToken;

  private ProgramBeanContainer _topProgram;
  private ContainerProgram _program;
  
  private HashMap<String,String> _namespaceMap = new HashMap<>();

  // private ELContext _elContext;

  private ExprCfg _expr;

  private NameCfg _qName;
  
  public ConfigFileParser(ConfigContext config)
  {
    Objects.requireNonNull(config);
    
    _config = config;
    //_elContext = ConfigELContext.EL_CONTEXT;
  }
  
  private ConfigContext getConfig()
  {
    return _config;
  }

  public ConfigProgram parse(PathImpl path)
    throws IOException
  {
    _path = path;
    _topProgram = new ProgramBeanContainer();
    _program = _topProgram;
    
    _topProgram.setLocation(getLocation());
    _topProgram.setFile(_path);
    
    try (ReadStream is = path.openRead()) {
      _is = is;
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
      
      if (loader.getParent().equals(systemLoader)) {
        loader = systemLoader;
      }
      
      _loader = loader;
      
      parseTop(_program);
      
      return _program;
    }
  }
  
  private boolean isTop()
  {
    return _program == _topProgram;
  }
  
  private void parseTop(ContainerProgram program)
    throws IOException
  {
    while (parseStatement()) {
    }
  }
  
  private void parseStatementList()
    throws IOException
  {
    while (parseStatement()) {
    }
  }
  
  private boolean parseStatement()
    throws IOException
  {
    Token token = parseToken();
    
    switch (token) {
    case IDENTIFIER: {
      parseStatementCommand(_lexeme);
      
      return true;
    }
    
    case CLASS_NAME: {
      parseCommandClassName(_qName);
      
      return true;
    }
    
    case IMPORT: {
      parseCommandClassName(IMPORT);
      
      return true;
    }
    
    case VAR: {
      parseAssignVar(_lexeme);
      return true;
    }
    
    case RBRACE: {
      _peekToken = token;
      
      return false;
    }
    
    case SEMI_COLON:
      return true;
      
    case NAMESPACE:
      parseNamespace();
      return true;
      
    case EOF:
      return false;
      
    default:
      throw error("Unexpected token {0}", token);
    }
  }
  
  private void parseStatementCommand(String name)
    throws IOException
  {
    Token token = parseToken();
  
    switch (token) {
    case ASSIGN: {
      parseAssign(name);
      return;
    }
    
    default:
      _peekToken = token;
      parseCommand(name);
      break;
    }
  }
  
  private void parseCommand(String name)
    throws IOException
  {
    Arg arg = null;
    ArrayList<Arg> args = new ArrayList<>();
    int index = 0;
    
    while ((arg = parseArg(index)) != null) {
      index++;
      
      args.add(arg);
    }
    
    if (args.size() == 1 
        && peekToken() == Token.SEMI_COLON
        && args.get(0).isPosition()) {
      ConfigProgram program;
      
      program = args.get(0).toProgram(name);

      if (program != null) {
        _program.addProgram(program);
        return;
      }
    }

    ProgramCommand program = new ProgramCommand(name, args);
    program.setLocation(getLocation());
    program.setFile(_path);
    
    parseProgramValues(program);
    
    _program.addProgram(program.toProgram());
  }
  
  private void parseCommandClassName(NameCfg name)
    throws IOException
  {
    Arg arg = null;
    ArrayList<Arg> args = new ArrayList<>();
    int index = 0;
    
    while ((arg = parseArg(index)) != null) {
      index++;
      
      args.add(arg);
    }

    ProgramCommandContainer program = new ProgramCommandClassName(name, args);
    program.setLocation(getLocation());
    program.setFile(_path);

    _program.addProgram(program);
    
    parseProgramValues(program);
  }
  
  private void parseProgramValues(ContainerProgram program)
    throws IOException
  {
    Token token = peekToken();
    
    if (token == Token.LBRACE) {
      parseToken();
    
      ContainerProgram oldProgram = _program;
    
      try {
        _program = program;
        
        parseStatementList();
      } finally {
        _program = oldProgram;
      }
      
      token = parseToken();
      
      if (token != Token.RBRACE) {
        throw error("Expected '}' at {0}", token);
      }
    }
  }
  
  private Arg parseArg(int index)
    throws IOException
  {
    Token token = parseToken();
    
    switch (token) {
    case IDENTIFIER: {
      String id = _lexeme;
      
      token = parseToken();
      
      if (token != Token.ASSIGN) {
        _peekToken = token;
        return new ArgPositionString(index, id);
      }
      
      token = parseToken();
      
      switch (token) {
      case IDENTIFIER:
      case NUMBER:
      case STRING:
        return new ArgString(id, _lexeme);
      
      case EXPR: {
        return new ArgExpr(id, _expr, _expr.getExpressionString()); // , _expr.toTopExpression());
      }
      
      case VAR: {
        /*
        ELParser parser = new ELParser(getELContext(), "${" + _lexeme + "}");
        parser.setCheckEscape(true);
        Expr expr = parser.parse();
        */
        ExprCfg expr = ExprCfg.newParser("${" + _lexeme + "}").parse();
        
        return new ArgExpr(id, expr, "${" + _lexeme + "}");
        
      }
                             
      default:
        throw error(L.l("Unknown token {0} {1}", token, _lexeme));
      }
    }
    
    case CLASS_NAME: {
      token = parseToken();
      
      if (token != Token.ASSIGN) {
        throw error(L.l("Expected '=' at {0}", token));
      }

      /*
      Expr valueExpr = parseValueExpr();

      return new ArgClassExpr(_qName, valueExpr);
      */
      throw new UnsupportedOperationException();
    }
    
    case VAR: {
      /*
      ELParser parser = new ELParser(getELContext(), "${" + _lexeme + "}");
      parser.setCheckEscape(true);
      Expr expr = parser.parse();
      
      return new ArgPositionExpr(index, expr, "${" + _lexeme + "}");
      */
      ExprCfg expr = ExprCfg.newParser("${" + _lexeme + "}").parse();
      
      return new ArgPositionExpr(index, expr, "${" + _lexeme + "}");
    }
    
    case EXPR: {
      return new ArgPositionExpr(index, _expr, _expr.getExpressionString());
      // throw new UnsupportedOperationException();
    }
      
    case NUMBER:
      return new ArgPositionString(index, _lexeme);
      
    case STRING:
      return new ArgPositionString(index, _lexeme);
      
    case SEMI_COLON:
      _peekToken = token;
      return null;
      
    case LBRACE:
      _peekToken = token;
      return null;
      
    default:
      throw error(L.l("Unknown token {0} {1}", token, _lexeme));
    }
  }

  /*
  private Expr parseValueExpr()
      throws IOException
  {
    Token token = parseToken();
    
    switch (token) {
    case IDENTIFIER:
    case NUMBER:
    case STRING: {
      return new StringLiteral(_lexeme);
    }
      
    case EXPR: {
      return _expr;
    }
    
    case VAR: {
      ELParser parser = new ELParser(getELContext(), "${" + _lexeme + "}");
      parser.setCheckEscape(true);
      Expr expr = parser.parse();
      
      return expr;
    }
                           
    default:
      throw error(L.l("Unknown token {0} {1}", token, _lexeme));
    }
  }
  */
  
  private void parseAssign(String id)
      throws IOException
  {
    String value = parseValue();
    
    ProgramBase program = new ProgramAssign(_config, id, value);
    program.setLocation(getLocation());
    
    if (ConfigContext.getProperty(id) == null) {
      ConfigContext.setProperty(id, value, _loader);
    }
    
    _program.addProgram(program);
  }
  
  private void parseAssignVar(String name)
    throws IOException
  {
    Token token = parseToken();
  
    if (token != Token.ASSIGN) {
      throw error("Expected assignment at {0}", token);
    }
    
    token = parseToken();
    
    switch (token) {
    case IDENTIFIER:
    case STRING:
    case NUMBER: {
      ProgramBase program = new ProgramAssignVar(_config, name, _lexeme);
      program.setLocation(getLocation());
      
      _program.addProgram(program);
      return;
    }
    
    default:
      throw error("Unexpected token at var {0}", token);
    }
  }
  
  private void parseNamespace()
    throws IOException
  {
    String prefix = null;
    String packageName = null;
  
    Token token = parseToken();
    
    switch (token) {
    case IDENTIFIER:
    case STRING:
      prefix = _lexeme;
      break;
    
    default:
      throw error("Expected namespace prefix at {0}", token);
    }
    
    token = parseToken();
    
    switch (token) {
    case IDENTIFIER:
    case STRING:
      packageName = _lexeme;
      break;
    
    default:
      throw error("Expected namespace package at {0}", token);
    }
    
    _namespaceMap.put(prefix, packageName);
  }
  
  private String parseValue()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    int ch;
    
    while (((ch = read()) > 0) && ch != ';') {
      sb.append((char) ch);
    }
    
    String value = sb.toString().trim();
    
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    
    return value;
  }
  
  /*
  private Token parseToken()
  {
    String line;

    while ((line = _is.readLine()) != null) {
      while (line.endsWith("\\")) {
        line = line.substring(0, line.length() - 1);

        String contLine = _is.readLine();

        if (contLine != null) {
          line += contLine;
        }
      }

      line = line.trim();

      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      if (line.startsWith("#") || line.equals("")) {
        continue;
      }

      p = line.indexOf(':');
      int q = line.indexOf('=');

      if (p < 0 || q >= 0 && q < p)
        p = q;

      if (p < 0) {
        throw new ConfigException(L.l("invalid line in {0}\n  {1}",
                                      _path, line));
      }

      String key = line.substring(0, p).trim();
      String value = line.substring(p + 1).trim();

    }
  }
  */
  
  /*
  private ELContext getELContext()
  {
    return _elContext;
  }
  */
  
  private Token peekToken()
    throws IOException
  {
    Token token = parseToken();
    
    _peekToken = token;
    
    return token;
  }
  
  private Token parseToken()
    throws IOException
  {
    Token token = _peekToken;
    
    if (token != null) {
      _peekToken = null;
      return token;
    }
    
    while (true) {
      int ch;
      
      switch (ch = read()) {
      case ' ': 
      case '\t':
        break;
        
      case '\r':
        break;
      case '\n':
        _line++;
        break;
        
      case -1:
        return Token.EOF;
        
      case '=':
        return Token.ASSIGN;
        
      case ';':
        return Token.SEMI_COLON;
        
      case '{':
        return Token.LBRACE;
        
      case '}':
        return Token.RBRACE;
        
      case '#':
        skipComment();
        break;
        
      case '0': case '1': case '2': case '3': case '4': 
      case '5': case '6': case '7': case '8': case '9':
        // return parseNumber(ch);
        parseIdentifier(ch);
        return Token.IDENTIFIER;
        
      case '"':
        return parseString(ch);
        
      case '$': {
        ch = read();
        if (Character.isJavaIdentifierStart(ch)) {
          parseIdentifier(ch);
          return Token.VAR;
        }
        else if (ch == '{') {
          _expr = parseExpression();
          
          return Token.EXPR;
        }
        else {
          throw error("Unknown expression at ${0}", (char) ch);
        }
      }
        
        
      default:
        if (ch < 0) {
          return Token.EOF;
        }
        else if (Character.isJavaIdentifierStart(ch)) {
          return parseIdentifier(ch);
        }
        else {
          System.out.println("FOR_STREAM: " + _is);
          throw error("Unexpected character '{0}' 0x{1}",
                      (char) ch, Integer.toHexString(ch));
          //return Token.ERROR;
        }
      }
    }
  }
  
  private ExprCfg parseExpression()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    int ch;
    
    sb.append("${");
    
    while ((ch = read()) >= 0) {
      if (ch == '}') {
        sb.append("}");
        //ELParser parser = new ELParser(getELContext(), sb.toString());
        //parser.setCheckEscape(true);
        return ExprCfg.newParser(sb.toString()).parse();
      }
      else {
        sb.append((char) ch);
      }
    }
    
    throw error("Unexpected end of expression");
  }
  
  private Token parseIdentifier(int ch)
    throws IOException
  {
    String prefix = null;
    
    StringBuilder sb = new StringBuilder();
    
    sb.append((char) ch);
    
    boolean isIdentifier = Character.isJavaIdentifierStart(ch);
    
    while ((ch = read()) > 0) {
      if (Character.isJavaIdentifierPart(ch)) {
        sb.append((char) ch);
      }
      else if (ch == ':' && isIdentifier) {
        if (prefix == null) {
          prefix = sb.toString();
          sb.setLength(0);
        }
        else {
          sb.append((char) ch);
        }
      }
      else if (ch == '-') {
        sb.append((char) ch);
      }
      else if (ch == '.' || ch == ':') {
        isIdentifier = false;
        sb.append((char) ch);
      }
      else {
        _peek = ch;
        break;
      }
    }
    
    _lexeme = sb.toString();
    
    if (prefix == null) {
      Token token;
      
      if (isTop()) {
        token = _topReservedMap.get(_lexeme);
      }
      else {
        token = _topReservedMap.get(_lexeme);
      }
      
      if (token != null) {
        return token;
      }
      else {
        return Token.IDENTIFIER;
      }
    }
    else if (prefix.equals("")) {
      return Token.IDENTIFIER;
    }
    
    String className = _namespaceMap.get(prefix);
    
    if (className == null) {
      className = prefix;
    }
    
    _qName = new NameCfg(prefix, _lexeme, "urn:java:" + className);
    
    return Token.CLASS_NAME;
  }
  
  private Token parseNumber(int ch)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append((char) ch);
    
    while (! isDelimiter(ch = read())) {
      sb.append((char) ch);
    }

    _peek = ch;
    
    _lexeme = sb.toString();
    
    return Token.NUMBER;
  }
  
  private Token parseString(int ch)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    while ((ch = read()) > 0) {
      switch (ch) {
      case '"':
        _lexeme = sb.toString();
        
        if (_lexeme.contains("${")) {
          /*
          ELParser parser = new ELParser(getELContext(), _lexeme);
          parser.setCheckEscape(true);
          _expr = parser.parse();
          
          return Token.EXPR;
          */
          _expr = ExprCfg.newParser(_lexeme).parse();
          
          return Token.EXPR;
        }
        
        return Token.STRING;
        
      case '\r': case '\n':
        throw error("Newline is forbidden in single-line strings");
        
      case '\\':
        switch ((ch = read())) {
        case 'n':
          sb.append('\n');
          break;
          
        case 'r':
          sb.append('\r');
          break;
          
        case 't':
          sb.append('\t');
          break;
          
        default:
          sb.append((char) read());
        }
        break;
        
      default:
        sb.append((char) ch);
      }
    }
    
    throw error("Unexpected end of file in string");
  }
  
  private boolean isDelimiter(int ch)
  {
    switch (ch) {
    case ' ': case '\t': case '\n': case '\r':
    case '(': case ')': case '{': case '}':
    case '[': case ']':
    case ',': case ';': case '=': case ':':
    case -1:
      return true;
      
    default:
      return false;
    }
  }
  
  private void skipComment()
    throws IOException
  {
    int ch;
    
    while ((ch = read()) > 0 && ch != '\n') {
    }
    
    if (ch == '\n') {
      _line++;
    }
  }
  
  private int read()
    throws IOException
  {
    int peek = _peek;
    
    if (peek > 0) {
      _peek = 0;
      return peek;
    }
    
    return _is.read();
  }
  
  private ConfigException error(String msg, Object ...args)
  {
    return new ConfigException(getLocation() + L.l(msg, args)
                               + getSourceLines(_path, _line));
  }
  
  private String getLocation()
  {
    return _path + ":" + _line + ": ";
  }

  public String getSourceLines(PathImpl source, int errorLine)
  {
    if (source == null || errorLine < 1)
      return "";

    boolean hasLine = false;
    StringBuilder sb = new StringBuilder("\n\n");

    ReadStream is = null;
    try {
      is = source.openRead();
      // is.setEncoding(_parseState.getPageEncoding());

      int line = 0;
      String text;
      while ((text = is.readLine()) != null) {
        line++;

        if (errorLine - 2 <= line && line <= errorLine + 2) {
          sb.append(line);
          sb.append(":  ");
          sb.append(text);
          sb.append("\n");
          hasLine = true;
        }
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      is.close();
    }

    if (hasLine)
      return sb.toString();
    else
      return "";
  }

  enum Token {
    EOF,
    ERROR,
    IDENTIFIER,
    ASSIGN,
    STRING,
    NUMBER,
    
    VAR,
    EXPR,
    
    CLASS_NAME,
    
    SEMI_COLON,
    LBRACE,
    RBRACE,
    
    NAMESPACE,
    IMPORT,
    ;
  }
  
  static class Arg
  {
    private final String _id;
    
    Arg(String id)
    {
      _id = id;
    }

    String getId()
    {
      return _id;
    }
    
    boolean isPosition()
    {
      return false;
    }
    
    ConfigProgram toProgram()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
    
    ConfigProgram toProgram(String name)
    {
      return null;
    }
  }
  
  class ArgString extends Arg
  {
    private final String _value;
    
    ArgString(String id, String value)
    {
      super(id);
      
      _value = value;
    }

    @Override
    ConfigProgram toProgram()
    {
      return new ProgramPropertyString(getConfig(), getId(), _value);
    }
  }
  
  class ArgExpr extends Arg
  {
    private final ExprCfg _expr;
    private final String _text;
    
    ArgExpr(String id, ExprCfg expr, String text)
    {
      super(id);
      
      _expr = expr;
      _text = text;
    }

    @Override
    ConfigProgram toProgram()
    {
      ProgramPropertyExpr program = new ProgramPropertyExpr(getConfig(), 
                                                            getId(), 
                                                            _expr,
                                                            _text);
      program.setLocation(getLocation());
      
      return program;
    }

    @Override
    ConfigProgram toProgram(String name)
    {
      
      ProgramPropertyExpr program = new ProgramPropertyExpr(getConfig(),
                                                            name,
                                                            _expr,
                                                            _text);
      program.setLocation(getLocation());
      
      return program;
    }
  }
  
  class ArgClassExpr extends Arg
  {
    private final NameCfg _qName;
    private final ExprCfg _expr;
    
    ArgClassExpr(NameCfg qName, ExprCfg expr)
    {
      super(qName.getName());
      
      _qName = qName;
      _expr = expr;
    }

    @Override
    ConfigProgram toProgram()
    {
      ProgramCommandContainer program = new ProgramCommandClassName(_qName);
      program.setLocation(getLocation());
      program.setFile(_path);

      ProgramPropertyExpr valueProgram
        = new ProgramPropertyExpr(getConfig(), "value", _expr, _expr.getExpressionString());
      valueProgram.setLocation(getLocation());
      
      program.addProgram(valueProgram);

      return program;
    }

    @Override
    ConfigProgram toProgram(String name)
    {
      return toProgram();
    }
  }
  
  private class ArgPositionString extends Arg
  {
    private final String _value;
    
    ArgPositionString(int id, String value)
    {
      super("_p" + id);
      
      _value = value;
    }
    
    @Override
    boolean isPosition()
    {
      return true;
    }

    @Override
    ConfigProgram toProgram()
    {
      ProgramPropertyString program;
      
      program = new ProgramPropertyString(getConfig(), getId(), _value);
      
      program.setLocation(getLocation());
      
      return program;
    }

    @Override
    ConfigProgram toProgram(String name)
    {
      ProgramPropertyStringElement program;
      
      program = new ProgramPropertyStringElement(getConfig(), name, _value);
      
      program.setLocation(getLocation());
      
      return program;
    }
  }
  
  private class ArgPositionExpr extends Arg
  {
    private final ExprCfg _expr;
    private final String _text;
    
    ArgPositionExpr(int id, ExprCfg expr, String text)
    {
      super("_p" + id);
      
      Objects.requireNonNull(expr);
      
      _expr = expr;
      _text = text;
      
    }
    
    @Override
    boolean isPosition()
    {
      return true;
    }

    @Override
    ConfigProgram toProgram()
    {
      return new ProgramPropertyExpr(getConfig(), getId(), _expr, _text);
    }

    @Override
    ConfigProgram toProgram(String name)
    {
      if ("#text".equals(name)) {
        return toProgram();
      }
      
      return new ProgramPropertyExpr(getConfig(), name, _expr, _text);
    }
  }
  
  static {
    _topReservedMap.putAll(_reservedMap);
    
    _topReservedMap.put("namespace", Token.NAMESPACE);
    _topReservedMap.put("import", Token.IMPORT);
  }
}

