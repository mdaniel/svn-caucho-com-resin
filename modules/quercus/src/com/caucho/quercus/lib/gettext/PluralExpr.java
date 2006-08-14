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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext;

import com.caucho.quercus.UnimplementedException;

import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a plural expression of an MO file.
 */
class PluralExpr
{
  final static int INTEGER = 256;
  final static int LPAREN = INTEGER + 1;
  final static int RPAREN = INTEGER + 2;
  final static int SEMI = INTEGER + 3;
  final static int IF_QUESTION = INTEGER + 4;
  final static int IF_COLON = INTEGER + 5;

  final static int EQ = INTEGER + 6;
  final static int NEQ = INTEGER + 7;
  final static int LT = INTEGER + 8;
  final static int LE = INTEGER + 9;
  final static int GT = INTEGER + 10;
  final static int GE = INTEGER + 11;

  final static int ADD = INTEGER + 12;
  final static int SUB = INTEGER + 13;
  final static int MUL = INTEGER + 14;
  final static int DIV = INTEGER + 15;
  final static int MOD = INTEGER + 16;

  final static int AND = INTEGER + 17;
  final static int OR = INTEGER + 18;

  final static int UNKNOWN = INTEGER + 19;
  final static int UNSET = INTEGER + 20;

  private CharSequence _expr;
  private int _exprLength;
  private int _parseIndex;

  private int _nplurals = -1;
  private int _plural = -1;

  private int _peekToken;
  private int _integer;
  private int _n;

  private boolean _errorEncountered;

  private PluralExpr() {}

  private PluralExpr(CharSequence expr)
  {
    _expr = expr;
    _exprLength = expr.length();
  }

  private void init(int n)
  {
    _parseIndex = 0;
    _peekToken = UNSET;
    _n = n;
    _errorEncountered = false;
  }

  /**
   * Extracts plural expression from metadata and returns a PluralExpr for the
   * plural expression.
   *
   * @param metadata containing plural expression
   */
  public static PluralExpr getPluralExpr(String metaData)
  {
    String pluralForms = "Plural-Forms:";
    int i = metaData.indexOf(pluralForms);

    if (i < 0)
      return new PluralExpr("nplurals=2; plural=n!=1");

    i += pluralForms.length();
    int j = metaData.indexOf('\n', i);

    if (j < 0)
      return new PluralExpr(metaData.substring(i));
    else
      return new PluralExpr(metaData.substring(i, j));
  }

  /**
   * Returns evaluated plural expression
   *
   * @param expr
   * @param n number of items
   */
  public static int eval(CharSequence expr, int n)
  {
    PluralExpr pluralExpr = new PluralExpr(expr);

    return pluralExpr.eval(n);
  }

  /**
   * Evaluates this plural expression.
   */
  public int eval(int n)
  {
    init(n);

    evalAssignExpr();
    evalAssignExpr();

    return validate();
  }

  private int validate()
  {
    if (_errorEncountered) {
      if (_n == 1)
        return 0;
      else
        return 1;
    }

    if (_nplurals < 1 || _plural < 0)
    {
      if (_n == 1)
        return 0;
      else return 1;
    }

    if (_plural >= _nplurals)
      return 0;

    return _plural;
  }

  private void evalAssignExpr()
  {
    int ch = consumeWhiteSpace();

    boolean isNplurals;

    if (ch == 'n' &&
        read() == 'p' &&
        read() == 'l' &&
        read() == 'u' &&
        read() == 'r' &&
        read() == 'a' &&
        read() == 'l' &&
        read() == 's') {
      isNplurals = true;
    }
    else if (ch == 'p' &&
        read() == 'l' &&
        read() == 'u' &&
        read() == 'r' &&
        read() == 'a' &&
        read() == 'l') {
      isNplurals = false;
    }
    else
      return;

    ch = consumeWhiteSpace();

    if (ch != '=')
      return;

    if (isNplurals)
      _nplurals = evalIfExpr();
    else
      _plural = evalIfExpr();

    // Read semicolon
    parseToken();
  }

  private int evalLiteralExpr()
  {
    int token = parseToken();

    if (token == INTEGER)
      return _integer;
    else
      throw new RuntimeException("");
  }

  private int evalParenExpr()
  {
    int token = parseToken();

    if (token != LPAREN) {
      _peekToken = token;
      return evalLiteralExpr();
    }

    int expr = evalIfExpr();
    if (parseToken() != RPAREN)
      throw new RuntimeException("");

    return expr;
  }

  private int evalMulExpr()
  {
    int expr = evalParenExpr();

    while (true) {
      int token = parseToken();
      switch (token) {
        case MOD:
          expr = expr % evalParenExpr();
          break;
        case MUL:
          expr = expr * evalParenExpr();
          break;
        case DIV:
          expr = expr / evalParenExpr();
          break;
        default:
          _peekToken = token;
          return expr;
      }
    }
  }

  private int evalAddExpr()
  {
    int expr = evalMulExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
        case ADD:
          expr += evalMulExpr();
          break;
        case SUB:
          expr -= evalMulExpr();
          break;
        default:
          _peekToken = token;
          return expr;
      }
    }
  }

  private int evalCmpExpr()
  {
    int expr = evalAddExpr();

    while (true) {
      int token = parseToken();
      switch (token) {
        case GT:
          if (expr > evalAddExpr())
            expr = 1;
          else
            expr = 0;
          break;
        case GE:
          if (expr >= evalAddExpr())
            expr = 1;
          else
            expr = 0;
          break;
        case LT:
          if (expr < evalAddExpr())
            expr = 1;
          else
            expr = 0;
          break;
        case LE:
          if (expr <= evalAddExpr())
            expr = 1;
          else
            expr = 0;
          break;
        case EQ:
          if (expr == evalAddExpr())
            expr = 1;
          else
            expr = 0;
          break;
        case NEQ:
          if (expr != evalAddExpr())
            expr = 1;
          else
            expr = 0;
          break;
        default:
          _peekToken = token;
          return expr;
      }
    }
  }

  private int evalAndExpr()
  {
    int expr = evalCmpExpr();

    while (true) {
      int token = parseToken();
      if (token != AND) {
        _peekToken = token;
        return expr;
      }

      int expr2 = evalCmpExpr();

      if (expr != 0 && expr2 != 0)
        expr = 1;
      else
        expr = 0;
    }
  }

  private int evalOrExpr()
  {
    int expr = evalAndExpr();

    while (true) {
      int token = parseToken();
      if (token != OR) {
        _peekToken = token;
        return expr;
      }

      int expr2 = evalAndExpr();

      if (expr != 0 || expr2 != 0)
        expr = 1;
      else
        expr = 0;
    }
  }

  private int evalIfExpr()
  {
    int expr = evalOrExpr();

    int token = parseToken();
    if (token != IF_QUESTION) {
      _peekToken = token;
      return expr;
    }

    int trueExpr = evalIfExpr();

    token = parseToken();
    if (token != IF_COLON)
      throw new RuntimeException();

    int falseExpr = evalIfExpr();

    if (expr != 0)
      return trueExpr;

    return falseExpr;
  }

  private int parseToken()
  {

    if (_peekToken != UNSET) {
      int toReturn = _peekToken;
      _peekToken = UNSET;
      return toReturn;
    }

    int ch = consumeWhiteSpace();

    switch (ch) {
      case '(':
        return LPAREN;
      case ')':
        return RPAREN;
      case '?':
        return IF_QUESTION;
      case ':':
        return IF_COLON;
      case ';':
        return SEMI;
      case '+':
        return ADD;
      case '-':
        return SUB;
      case '%':
        return MOD;
      case '*':
        return MUL;
      case '/':
        return DIV;
      case '>':
        if (read() == '=')
          return GE;
        unread();
        return GT;
      case '<':
        if (read() == '=')
          return LE;
        unread();
        return LT;
      case '=':
        if (read() == '=')
          return EQ;
        return UNKNOWN;
      case '!':
        if (read() == '=')
          return NEQ;
        return UNKNOWN;
      case '&':
        if (read() == '&')
          return AND;
        return UNKNOWN;
      case '|':
        if (read() == '|')
          return OR;
        return UNKNOWN;
    }

    return parseIntegerToken(ch);
  }

  private int parseIntegerToken(int ch)
  {
    if ('0' <= ch && ch <= '9') {
      _integer = ch - '0';
      for (ch = read(); '0' <= ch && ch <= '9'; ch = read()) {
        _integer = _integer * 10 + ch - '0';
      }

      unread();
      return INTEGER;
    }

    else if (ch == 'n') {
      if (Character.isLetter(read()))
        return UNKNOWN;

      _integer = _n;

      unread();
      return INTEGER;
    }

    return UNKNOWN;
  }

  /**
   * Consumes whitespaces and returns the last non-whitespace character.
   */
  private int consumeWhiteSpace()
  {
    while (true) {
      int ch = read();
      switch (ch) {
        case ' ':
        case '\n':
        case '\t':
        case '\r':
          continue;
        default:
          return ch;
      }
    } 
  }

  private int read()
  {
    if (_parseIndex >= _exprLength)
      return -1;

    return _expr.charAt(_parseIndex++);
  }

  private void unread()
  {
    if (_parseIndex > 0 && _parseIndex < _exprLength)
      _parseIndex--;
  }
}
