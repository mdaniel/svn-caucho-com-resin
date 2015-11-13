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

package com.caucho.v5.el;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.L10N;

/**
 * Parses the expression.
 */
public class ELParser
{
  private static final Logger log = Logger.getLogger(ELParser.class.getName());
  private static final L10N L = new L10N(ELParser.class);

  // The expression string
  private String _string;
  // Current parse index into the string
  private int _index;
  // The peek token
  private int _peek = -1;

  private int _lastToken = -1;
  private int _lastIndexStart;
  private int _lastIndexEnd;

  // The current lexeme
  private String _lexeme;
  // Temporary buffer
  private CharBuffer _cb = new CharBuffer();

  private final ELContext _elContext;
  private final boolean _isMethodExpr;
  private boolean _isParsingArgs;

  private HashMap<String,Expr> _lambdaVars;

  private boolean _checkEscape = true;

  public ELParser(ELContext elContext, String string)
  {
    this(elContext, string, false);
  }

  public ELParser(ELContext elContext, String string, boolean isMethodExpr)
  {
    if (elContext == null) {
      throw new NullPointerException();
    }
    
    _elContext = elContext;
    _string = string;
    _isMethodExpr = isMethodExpr;
  }

  protected ELParser create(String string)
  {
    ELParser parser = new ELParser(_elContext, string, _isMethodExpr);

    copyTo(parser);

    return parser;
  }

  protected ELContext getELContext()
  {
    return _elContext;
  }

  protected boolean isMethodExpr()
  {
    return _isMethodExpr;
  }

  /**
   * Copy to the dest parser.
   */
  protected void copyTo(ELParser parser)
  {
  }

  /**
   * Set true if escapes are checked.
   */
  public void setCheckEscape(boolean checkEscape)
  {
    _checkEscape = checkEscape;
  }

  /**
   * Parses the expression string.
   */
  public Expr parse()
    throws ELParseException
  {
    return parseInterpolate();
  }

  /**
   * Parses interpolated code.
   */
  public Expr parseInterpolate()
    throws ELParseException
  {
    CharBuffer text = new CharBuffer();
    CharBuffer exprString = new CharBuffer();
    Expr expr = null;
    int ch;
    int exprToken = -1;

    while ((ch = read()) >= 0) {
      if (_checkEscape && ch == '\\') {
        ch = read();

        if (ch == '$' || ch == '#' || ch == '\\')
          text.append((char) ch);
        else {
          text.append('\\');
          unread();
        }
      }
      else if (ch == '$' || ch == '#') {
        int origChar = ch;

        ch = read();

        if (ch == '{') {
          if (expr != null && _isMethodExpr)
            throw new ELParseException(L.l("Invalid method expression '{0}'",
                                           _string));

          if (exprToken != -1 && exprToken != origChar)
            throw error(L.l("Mixed '#' and '$'. Expected '{0}' at '{1}'",
                            Character.toString((char) exprToken),
                            Character.toString((char) origChar)));

          exprToken = origChar;

          if (text.length() > 0) {
            StringLiteral right = new StringLiteral(text.toString());

            if (expr == null)
              expr = right;
            else
              expr = new InterpolateExpr(expr, right);

            text.clear();
          }

          exprString.clear();

          int depth = 0;

          for (ch = read();
               ch > 0 && ! (ch == '}' && depth == 0);
               ch = read()) {
            exprString.append((char) ch);

            switch (ch) {
            case '{':
              depth++;
              break;

            case '}':
              depth--;
              break;

            case '\'': case '"': {
              int end = ch;

              for (ch = read(); ch > 0 && ch != end; ch = read()) {
                exprString.append((char) ch);

                if (ch == '\\') {
                  ch = read();
                  if (ch > 0)
                    exprString.append((char) ch);
                }
              }

              if (ch > 0)
                exprString.append((char) ch);
            }
            break;
            }
          }

          if (ch != '}')
            throw error(L.l("expected '}' at end of EL expression",
                            exprString));

          Expr right = create(exprString.toString()).parseExpr();

          if (expr == null)
            expr = right;
          else
            expr = new InterpolateExpr(expr, right);
        }
        else {
          text.append((char) origChar);
          unread();
        }
      }
      else
        text.append((char) ch);
    }

    if (text.length() > 0) {
      StringLiteral right = new StringLiteral(text.toString());

      if (expr == null)
        expr = right;
      else
        expr = new InterpolateExpr(expr, right);
    }

    if (expr == null)
      expr = new StringLiteral("");

    return expr;
  }

  /**
   * expr ::= term
   */
  public Expr parseExpr()
    throws ELParseException
  {
    Expr left = parseTerm();

    while (true) {
      int token = scanToken();

      if (_isMethodExpr && ! _isParsingArgs) {
        switch (token) {
        case '?':
        case Expr.COND_BINARY:
        case Expr.OR: case Expr.AND:
        case Expr.EQ: case Expr.NE: case Expr.LT:
        case Expr.LE: case Expr.GT: case Expr.GE:
        case Expr.ADD: case Expr.SUB:
        case Expr.MUL: case Expr.DIV: case Expr.MOD:
        case Expr.MATCHES:
          throw new ELParseException(L.l("Invalid method expression `{0}'",
                                         _string));
        }
      }

      switch (token) {

        case Expr.SEMICOLON:
          left = new SemicolonExpr(left, parseExpr());
          break;

        case Expr.ASSIGN:
          left = parseAssignExpr(left, parseTerm());
          break;

        case Expr.LAMBDA:
          left = parseLambdaExpr(left);
          break;

        case '?':
          left = parseCondExpr(left);
          break;

        case Expr.COND_BINARY:
          {
            Expr defaultExpr = parseExpr();

            left = new ConditionalNullExpr(left, defaultExpr);
          }
          break;

        case Expr.OR:
          left = parseOrExpr(token, left, parseTerm());
          break;

        case Expr.AND:
          left = parseAndExpr(token, left, parseTerm());
          break;

        case Expr.EQ: case Expr.NE: case Expr.LT:
        case Expr.LE: case Expr.GT: case Expr.GE:
        case Expr.MATCHES:
          left = parseCmpExpr(token, left, parseTerm());
          break;

        case Expr.CONCAT:
          left = new ConcatExpr(left, parseExpr());
          break;

        case Expr.ADD: case Expr.SUB:
          left = parseAddExpr(token, left, parseTerm());
          break;

        case Expr.MUL: case Expr.DIV: case Expr.MOD:
          left = parseMulExpr(token, left, parseTerm());
          break;

        default:
          unreadToken();
          return left;
      }
    }
  }

  /**
   * assign-expr ::= assign-expr '=' expr
   *             ::= lambda-expr
   */
  private Expr parseAssignExpr(Expr left, Expr right)
    throws ELParseException
  {
    while (true) {
      int token = scanToken();

      switch (token) {

      case Expr.ASSIGN:
        right = parseAssignExpr(right, parseTerm());
        break;

      case Expr.LAMBDA:
        right = parseLambdaExpr(right);
        break;

      case '?':
        right = parseCondExpr(right);
        break;

      case Expr.OR:
        right = parseOrExpr(token, right, parseTerm());
        break;

      case Expr.AND:
        right = parseAndExpr(token, right, parseTerm());
        break;

      case Expr.EQ: case Expr.NE:
      case Expr.LT: case Expr.GT:
      case Expr.LE: case Expr.GE:
      case Expr.MATCHES:
        right = parseCmpExpr(token, right, parseTerm());
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm());
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        return new AssignExpr(left, right);
      }
    }
  }

  /**
   * lambda-expr ::= lambda-params '->' lambda-expr
   *             ::= cmp-expr
   */
  private Expr parseLambdaExpr(Expr left)
  {
    LambdaParamsExpr params = null;
    if (left instanceof LambdaParamsExpr) {
      params = (LambdaParamsExpr) left;
    }
    else {
      params = new LambdaParamsExpr(left);
    }

    HashMap<String,Expr> oldLambdaVars = _lambdaVars;
    _lambdaVars = new HashMap<String,Expr>();

    if (oldLambdaVars != null) {
      _lambdaVars.putAll(oldLambdaVars);
    }

    for (String paramName : params) {
      _lambdaVars.put(paramName, new LambdaVarExpr(paramName));
    }

    Expr right = parseTerm();

    while (true) {
      int token = scanToken();

      switch (token) {
      case Expr.LAMBDA:
        right = parseLambdaExpr(right);
        break;

      case '?':
        right = parseCondExpr(right);
        break;

      case Expr.OR:
        right = parseOrExpr(token, right, parseTerm());
        break;

      case Expr.AND:
        right = parseAndExpr(token, right, parseTerm());
        break;

      case Expr.EQ: case Expr.NE:
      case Expr.LT: case Expr.GT:
      case Expr.LE: case Expr.GE:
      case Expr.MATCHES:
        right = parseCmpExpr(token, right, parseTerm());
        break;

      case Expr.CONCAT:
        right = new ConcatExpr(right, parseExpr());
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm());
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        _lambdaVars = oldLambdaVars;
        return new LambdaExpr(params, right);
      }
    }
  }

  private Expr parseCondExpr(Expr left)
  {
    Expr testExpr = parseExpr();
    int token = scanToken();
    if (token != ':')
      throw error(L.l("Expected ':' at {0}.  Conditional syntax is 'expr ? expr : expr'.", badChar(token)));

    Expr right = parseTerm();

    while (true) {
      token = scanToken();

      switch (token) {
      case Expr.OR:
        right = parseOrExpr(token, right, parseTerm());
        break;

      case Expr.AND:
        right = parseAndExpr(token, right, parseTerm());
        break;

      case Expr.EQ: case Expr.NE:
      case Expr.LT: case Expr.GT:
      case Expr.LE: case Expr.GE:
      case Expr.MATCHES:
        right = parseCmpExpr(token, right, parseTerm());
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm());
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        return new ConditionalExpr(left, testExpr, right);
      }
    }

  }


  /**
   * or-expr ::= or-expr 'or' expr
   *         ::= and-expr
   */
  private Expr parseOrExpr(int code, Expr left, Expr right)
    throws ELParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.OR:
        left = new BooleanExpr(code, left, right);
        code = token;
        right = parseTerm();
        break;

      case Expr.AND:
        right = parseAndExpr(token, right, parseTerm());
        break;

      case Expr.EQ: case Expr.NE:
      case Expr.LT: case Expr.GT:
      case Expr.LE: case Expr.GE:
      case Expr.MATCHES:
        right = parseCmpExpr(token, right, parseTerm());
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm());
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        return new BooleanExpr(code, left, right);
      }
    }
  }

  /**
   * and-expr ::= and-expr 'and' expr
   *          ::= cmp-expr
   */
  private Expr parseAndExpr(int code, Expr left, Expr right)
    throws ELParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.AND:
        left = new BooleanExpr(code, left, right);
        code = token;
        right = parseTerm();
        break;

      case Expr.EQ: case Expr.NE:
      case Expr.LT: case Expr.GT:
      case Expr.LE: case Expr.GE:
      case Expr.MATCHES:
        right = parseCmpExpr(token, right, parseTerm());
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm());
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        return new BooleanExpr(code, left, right);
      }
    }
  }

  /**
   * cmp-expr ::= cmp-expr '=' expr
   *          ::= cmp-expr
   */
  private Expr parseCmpExpr(int code, Expr left, Expr right)
    throws ELParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.EQ: case Expr.NE:
      case Expr.GT: case Expr.LT:
      case Expr.LE: case Expr.GE:
      case Expr.MATCHES:
        left = CmpExpr.create(code, left, right);

        code = token;
        right = parseTerm();
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm());
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        return CmpExpr.create(code, left, right);
      }
    }
  }

  /**
   * add-expr ::= add-expr '+' expr
   *          ::= mul-expr
   */
  private Expr parseAddExpr(int code, Expr left, Expr right)
    throws ELParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.ADD: case Expr.SUB:
        left = BinaryExpr.create(code, left, right);
        code = token;
        right = parseTerm();
        break;

      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm());
        break;

      default:
        unreadToken();
        return BinaryExpr.create(code, left, right);
      }
    }
  }

  /**
   * mul-expr ::= mul-expr '*' expr
   *          ::= expr
   */
  private Expr parseMulExpr(int code, Expr left, Expr right)
    throws ELParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.MUL: case Expr.DIV: case Expr.MOD:
        left = BinaryExpr.create(code, left, right);
        right = parseTerm();
        code = token;
        break;

      default:
        unreadToken();
        return BinaryExpr.create(code, left, right);
      }
    }
  }

  /**
   * term ::= simple-term
   *      ::= term '[' expr ']'
   *      ::= term . identifier
   */
  private Expr parseTerm()
    throws ELParseException
  {
    Expr term = parseSimpleTerm();

    while (true) {
      int token = scanToken();

      switch (token) {
      case '[':
      {
        Expr expr = parseExpr();
        token = scanToken();
        if (token != ']')
          throw error(L.l("Expected `]' at {0}.  All open array braces must have matching closing brace.", badChar(token)));

        term = term.createField(expr);
        break;
      }

      case '(':
      {
        Expr []args = parseArgs();

        Expr expr = term.createMethod(args);

        if (expr == null)
          throw error(L.l("Method call not supported in this context `{0}'.",
                          term));
        term = expr;

        break;
      }

      case '.':
      {
        int ch = skipWhitespace(read());

        if (! Character.isJavaIdentifierStart((char) ch))
          throw error(L.l("Expected `]' at {0}.  Field references must be identifiers.", badChar(ch)));

        String field = readName(ch);

        term = term.createField(field);
        break;
      }

      case Expr.NOT: {
        if (Expr.NOT == token && term != null && term.isConstant())
          throw new ELParseException(L.l("invalid expression `{0}'", _string));

        unreadToken();
        return term;
      }

      default:
        unreadToken();
        return term;
      }
    }
  }

  private Expr []parseArgs()
  {
    ArrayList<Expr> argList = new ArrayList<>();

    int token = -1;

    int ch = skipWhitespace(read());

    while (ch > 0 && ch != ')') {
      unread();

      _isParsingArgs = true;

      argList.add(parseExpr());

      token = scanToken();

      if (token != ',') {
        ch = token;
        break;
      }

      ch = skipWhitespace(read());
    }

    if (ch != ')')
      throw error(L.l("Expected `)' at {0}.  All functions must have matching closing parenthesis.", badChar(ch)));

    _isParsingArgs = false;

    // token = scanToken();

    return argList.toArray(new Expr[argList.size()]);
  }

  /**
   * simple-term ::= number
   *             ::= '(' expr ')'
   *             ::= variable
   *             ::= '"' string '"'
   *             ::= true | false | null
   */
  private Expr parseSimpleTerm()
    throws ELParseException
  {
    int ch = read();

    ch = skipWhitespace(ch);

    switch (ch) {
    case '.':
    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9':
      {
        if (_isMethodExpr && ! _isParsingArgs)
          throw new ELParseException(L.l("Invalid method expression `{0}'",
                                         _string));

        long value = 0;
        double exp = 1;
        int digits = 0;

        for (; ch >= '0' && ch <= '9'; ch = read())
          value = 10 * value + ch - '0';

        if (ch != '.' && ch != 'e' && ch != 'E') {
          unread();
          return new LongLiteral(value);
        }

        if (ch == '.') {
          for (ch = read(); ch >= '0' && ch <= '9'; ch = read()) {
            value = 10 * value + ch - '0';
            exp *= 10;
            digits--;
          }
        }

        if (ch == 'e' || ch == 'E') {
          int sign = 1;
          int expValue = 0;

          ch = read();
          if (ch == '-') {
            sign = -1;
            ch = read();
          }
          else if (ch == '+')
            ch = read();

          for (; ch >= '0' && ch <= '9'; ch = read())
            expValue = 10 * expValue + ch - '0';

          exp = Math.pow(10, digits + sign * expValue);

          unread();

          return new DoubleLiteral((double) value * (double) exp);
        }

        unread();
        return new DoubleLiteral((double) value / (double) exp);
      }

    case '-': {
      if (_isMethodExpr && ! _isParsingArgs)
        throw new ELParseException(L.l("Invalid method expression `{0}'",
                                       _string));

      return new MinusExpr(parseTerm());
    }

    case '!': {
      if (_isMethodExpr &&  ! _isParsingArgs)
        throw new ELParseException(L.l("Invalid method expression `{0}'",
                                       _string));

      return UnaryExpr.create(Expr.NOT, parseTerm());
    }

    case '+': {
      if (_isMethodExpr && ! _isParsingArgs)
        throw new ELParseException(L.l("Invalid method expression `{0}'",
                                       _string));

      return parseTerm();
    }

    case '(':
      {
        int index = _index;

        ch = skipWhitespace(read());

        Expr expr;

        if (ch == ')') {
          // lambda empty params
          Expr []params = new Expr[0];

          LambdaParamsExpr paramsExpr = new LambdaParamsExpr(params);

          expr = paramsExpr;
        }
        else {
          _index = index;

          expr = parseExpr();
          ch = skipWhitespace(read());

          if (ch == ',') {
            Expr []args = parseArgs();

            Expr []params = new Expr[args.length + 1];
            params[0] = expr;
            System.arraycopy(args, 0, params, 1, args.length);

            LambdaParamsExpr paramsExpr = new LambdaParamsExpr(params);
            //parseLambdaExpr(paramsExpr);
            return paramsExpr;
          }
          else if (ch != ')') {
            throw error(L.l("Expected `)' at {0}.  All open parentheses must have matching closing parentheses.", badChar(ch)));
          }
        }

        return expr;
      }

    case '[':
      {
        return parseList();
      }

    case '{':
      {
        return parseMap();
      }

    case '\'': case '"':
      {
        int end = ch;
        CharBuffer cb = _cb;
        cb.clear();

        for (ch = read(); ch >= 0; ch = read()) {
          if (ch == '\\')
            cb.append((char) read());
          else if (ch != end)
            cb.append((char) ch);
          else if ((ch = read()) == end)
            cb.append((char) ch);
          else {
            unread();
            break;
          }
        }

        return new StringLiteral(cb.toString());
      }

    default:
      if (! Character.isJavaIdentifierStart((char) ch) && ch != ':') {
        throw error(L.l("Unexpected character at {0}.", badChar(ch)));
      }

      CharBuffer cb = _cb;
      cb.clear();

      for (;
           Character.isJavaIdentifierPart((char) ch) || ch == ':';
           ch = read())
        cb.append((char) ch);

      unread();

      if (cb.charAt(cb.length() - 1) == ':') {
        unread();

        cb.deleteCharAt(cb.length() - 1);
      }

      String name = cb.toString();

      if (name.equals("null"))
        return new NullLiteral();
      else if (name.equals("true"))
        return new BooleanLiteral(true);
      else if (name.equals("false"))
        return new BooleanLiteral(false);
      else if (name.equals("not"))
        return UnaryExpr.create(Expr.NOT, parseTerm());
      else if (name.equals("empty"))
        return UnaryExpr.create(Expr.EMPTY, parseTerm());
      else {
        if (_lambdaVars != null) {
          Expr var = _lambdaVars.get(name);

          if (var != null) {
            return var;
          }
        }

        VariableMapper varMapper = _elContext.getVariableMapper();

        ValueExpression valueExpr = null;

        if (varMapper != null) {
          valueExpr = varMapper.resolveVariable(name);
        }

        if (valueExpr != null) {
          return new ValueExpr(name, valueExpr);
        }

        Expr expr = createImplicitObjectExpr(name);

        if (expr != null) {
          return expr;
        }
      }

      try {
        Method method = getStaticMethod(name);

        if (method != null)
          return new StaticMethodExpr(method);
        else
          return new IdExpr(name);
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);

        return new IdExpr(name);
      }
    }
  }

  private Expr parseList()
  {
    ArrayList<Expr> list = new ArrayList<Expr>();

    int ch = skipWhitespace(read());

    if (ch == ']') {
      return new ListExpr(list);
    }

    unread();

    int token;

    do {
      list.add(parseExpr());
    } while ((token = scanToken()) == ',');

    if (token != ']') {
      throw error(L.l("Expected ']' at {0}.", badChar(token)));
    }

    return new ListExpr(list);
  }

  private Expr parseMap()
  {
    List<MapEntryExpr> mapEntries = new ArrayList<>();
    List<Expr> setEntries = new ArrayList<Expr>();

    int ch = skipWhitespace(read());

    if (ch == '}') {
      return new SetExpr(setEntries);
    }

    unread();

    int token;
    Boolean isMap = null;

    do {
      Expr key = parseExpr();
      token = scanToken();

      if (isMap == null) {
        isMap = (token == ':');
      }

      if (token == ':') {
        if (! isMap) {
          throw error(L.l("Unexpected ':' at {0}.", badChar(token)));
        }

        Expr value = parseExpr();
        mapEntries.add(new MapEntryExpr(key, value));

        token = scanToken();
      }
      else {
        if (isMap) {
          throw error(L.l("Expected ':' at {0}.", badChar(token)));
        }

        setEntries.add(key);
      }
    } while (token == ',');

    if (token != '}') {
      throw error(L.l("Expected '}' at {0}.", badChar(token)));
    }

    if (isMap)  {
      return new MapExpr(mapEntries);
    }
    else {
      return new SetExpr(setEntries);
    }
  }

  /**
   * Creates the implicit object for the name.
   */
  protected Expr createImplicitObjectExpr(String name)
  {
    return null;
  }

  /**
   * Creates the implicit object for the name.
   */
  protected Method getStaticMethod(String name)
    throws ELParseException
  {
    Method method = null;

    FunctionMapper funMapper = _elContext.getFunctionMapper();

    if (funMapper != null) {
      String prefix = "";
      String localName = name;

      int p = name.indexOf(':');
      if (p > 0) {
        prefix = name.substring(0, p);
        localName = name.substring(p + 1);
      }

      method = funMapper.resolveFunction(prefix, localName);
    }

    return method;
  }

  /**
   * Unreads the last read token and resets the read buffer.
   */
  private void unreadToken()
  {
    _index = _lastIndexStart;

    _peek = _lastToken;
  }

  /**
   * Scans the next token.
   *
   * @return token code, expressed as an Expr enumeration.
   */
  private int scanToken()
    throws ELParseException
  {
    _lastIndexStart = _index;

    int token = scanTokenImpl();

    _lastToken = token;
    _lastIndexEnd = _index;

    return token;
  }

  private int scanTokenImpl()
    throws ELParseException
  {
    if (_peek >= 0) {
      int value = _peek;
      _peek = -1;

      _index = _lastIndexEnd;

      return value;
    }

    int ch = skipWhitespace(read());

    switch (ch) {
    case '+':
      ch = read();
      if (ch == '=')
        return Expr.CONCAT;
      else {
        unread();
        return Expr.ADD;
      }
    case '-':
      ch = read();
      if (ch == '>')
        return Expr.LAMBDA;
      else {
        unread();
        return Expr.SUB;
      }
    case '*': return Expr.MUL;
    case '/': return Expr.DIV;
    case '%': return Expr.MOD;

    case '!':
      ch = read();
      if (ch == '=')
        return Expr.NE;
      else
        return Expr.NOT;

    case '=':
      ch = read();
      if (ch == '=')
        return Expr.EQ;
      else if (ch == '~')
        return Expr.MATCHES;
      else {
        unread();
        return Expr.ASSIGN;
        //throw error(L.l("expected '==' or '=~' at '={0}'", badChar(ch)));
      }

    case '&':
      ch = read();
      if (ch == '&')
        return Expr.AND;
      else
        throw error(L.l("expected '&&' at '&{0}'", badChar(ch)));

    case '|':
      ch = read();
      if (ch == '|')
        return Expr.OR;
      else
        throw error(L.l("expected '||' at '|{0}'", badChar(ch)));

    case '<':
      ch = read();
      if (ch == '=') {
        return Expr.LE;
      }
      else {
        unread();
        return Expr.LT;
      }

    case '>':
      ch = read();
      if (ch == '=') {
        return Expr.GE;
      }
      else {
        unread();
        return Expr.GT;
      }

    case ';':
      return Expr.SEMICOLON;

    case '[':
      return '[';

    case ']':
      return ']';

    case '{':
      return '{';

    case '}':
      return '}';

    case ')':
      return ')';

    case '(':
      return '(';

    case '.':
      return '.';

    case ',':
      return ',';

    case '?':
      ch = read();
      if (ch == ':') {
        return Expr.COND_BINARY;
      }
      else {
        unread();
        return '?';
      }

    case ':':
      return ch;

    default:
      if (Character.isJavaIdentifierStart((char) ch)) {
        String name = readName(ch);

        if (name.equals("div"))
          return Expr.DIV;
        else if (name.equals("mod"))
          return Expr.MOD;
        else if (name.equals("eq"))
          return Expr.EQ;
        else if (name.equals("ne"))
          return Expr.NE;
        else if (name.equals("lt"))
          return Expr.LT;
        else if (name.equals("le"))
          return Expr.LE;
        else if (name.equals("gt"))
          return Expr.GT;
        else if (name.equals("ge"))
          return Expr.GE;
        else if (name.equals("and"))
          return Expr.AND;
        else if (name.equals("or"))
          return Expr.OR;
        else if (name.equals("matches"))
          return Expr.MATCHES;
        else if (name.equals("cat"))
          return Expr.ADD;
        else {
          _lexeme = name;
          return Expr.IDENTIFIER;

          //throw error(L.l("expected binary operation at '{0}'", name));
        }
      }

      unread();
      return -1;
    }
  }

  private String readName(int ch)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (; Character.isJavaIdentifierPart((char) ch); ch = read())
      cb.append((char) ch);

    unread();

    return cb.toString();
  }

  /**
   * Skips whitespace, returning the next meaningful character.
   */
  private int skipWhitespace(int ch)
    throws ELParseException
  {
    for (; ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'; ch = read()) {
    }

    return ch;
  }

  /**
   * Reads the next character, returning -1 on end of file.
   */
  private int read()
  {
    _peek = -1;

    if (_index < _string.length())
      return _string.charAt(_index++);
    else {
      _index++;
      return -1;
    }
  }

  /**
   * Unread the last character.
   */
  private void unread()
  {
    _index--;
  }

  /**
   * Returns a readable version of the character.
   */
  private String badChar(int ch)
  {
    if (ch < 0)
      return L.l("end of file");
    else if (ch == '\n')
      return L.l("end of line");
    else
      return "`" + (char) ch + "'";
  }

  /**
   * Returns an new exception.
   */
  private ELParseException error(String message)
  {
    return new ELParseException(message + " in " + _string);
  }
}
