/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.json;

import com.caucho.quercus.env.*;
import com.caucho.util.L10N;

class JsonDecoder {
  private static final L10N L = new L10N(JsonDecoder.class);

  private StringValue _string;
  private int _len;
  private int _offset;

  private boolean _isAssociative;

  public Value jsonDecode(Env env,
                          StringValue s,
                          boolean assoc)
  {
    _string = s;
    _len = _string.length();
    _offset = 0;

    _isAssociative = assoc;

    Value val = jsonDecodeImpl(env);

    // Should now be at end of string or have only white spaces left.
    if (skipWhitespace() >= 0)
      return errorReturn(env, "expected no more input");

    return val;
  }

  /**
   * Entry point to decode a JSON value.
   *
   * @return decoded PHP value 
   */
  private Value jsonDecodeImpl(Env env)
  {
    for (int ch = skipWhitespace(); ch >= 0; ch = read()) {

      switch (ch) {
        case '"':
          return decodeString(env);

        case 't':
          if (read() == 'r' &&
              read() == 'u' &&
              read() == 'e')
            return BooleanValue.TRUE;
          else
            return errorReturn(env, "expected 'true'");

        case 'f':
          if (read() == 'a' &&
              read() == 'l' &&
              read() == 's' &&
              read() == 'e')
            return BooleanValue.FALSE;
          else
            return errorReturn(env, "expected 'false'");

        case 'n':
          if (read() == 'u' &&
              read() == 'l' &&
              read() == 'l')
            return NullValue.NULL;
          else
            return errorReturn(env, "expected 'null'");

        case '[':
          return decodeArray(env);

        case '{':
          return decodeObject(env);

        default:
          if (ch == '-' || ('0' <= ch && ch <= '9'))
            return decodeNumber(env, ch);
          else
            return errorReturn(env);
      }
    }

    return errorReturn(env);
  }

  /**
   * Checks to see if there is a valid number per JSON Internet Draft.
   */
  private Value decodeNumber(Env env, int ch)
  {
    StringBuilder sb = new StringBuilder();

    // (-)?
    if (ch == '-') {
      sb.append((char)ch);
      ch = read();
    }

    // (0) | ([1-9] [0-9]+)
    if (ch >= 0) {
      if (ch == '0') {
        sb.append((char)ch);
        ch = read();
      }
      else if ('1' <= ch && ch <= '9') {
        sb.append((char)ch);
        ch = read();

        while ('0' <= ch && ch <= '9') {
          sb.append((char)ch);
          ch = read();
        }
      }
      else
        return errorReturn(env, "expected 1-9");
    }

    int integerEnd = sb.length();

    // ((decimalPoint) [0-9]+)?
    if (ch == '.') {
      sb.append((char)ch);
      ch = read();

      while ('0' <= ch && ch <= '9') {
        sb.append((char)ch);
        ch = read();
      }
    }

    // ((e | E) (+ | -)? [0-9]+)
    if (ch == 'e' || ch == 'E') {
      sb.append((char)ch);
      ch = read();

      if (ch == '+' || ch == '-') {
        sb.append((char)ch);
        ch = read();
      }

      if ('0' <= ch && ch <= '9') {
        sb.append((char)ch);
        ch = read();

        while ('0' <= ch && ch <= '9') {
          sb.append((char)ch);
          ch = read();
        }
      }
      else
        return errorReturn(env, "expected 0-9 exponent");
    }

    unread();

    if (integerEnd != sb.length())
      return new DoubleValue(Double.parseDouble(sb.toString()));
    else
      return new LongValue(Long.parseLong(sb.toString()));
  }

  /**
   * Returns a non-associative PHP array.
   */
  private Value decodeArray(Env env)
  {
    ArrayValueImpl array = new ArrayValueImpl();

    while (true) {
      int ch = skipWhitespace();

      if (ch == ']')
        break;

      unread();

      array.append(jsonDecodeImpl(env));

      ch = skipWhitespace();

      if (ch == ',') {
      }
      else if (ch == ']')
        break;
      else
        errorReturn(env, "expected either ',' or ']'");
    }

    return array;
  }

  private Value decodeObject(Env env)
  {
    if (_isAssociative)
      return decodeObjectToArray(env);
    else
      return decodeObjectToObject(env);
  }

  /**
   * Returns a PHP associative array of JSON object.
   */
  private Value decodeObjectToArray(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    while (true) {
      int ch = skipWhitespace();

      if (ch == '}')
        break;

      unread();

      Value name = jsonDecodeImpl(env);

      ch = skipWhitespace();

      if (ch != ':')
        return errorReturn(env, "expected ':'");

      array.append(name, jsonDecodeImpl(env));

      ch = skipWhitespace();

      if (ch == ',') {
      }
      else if (ch == '}')
        break;
      else
        return errorReturn(env, "expected either ',' or '}'");
    }

    return array;
  }

  /**
   * Returns a PHP stdObject of JSON object.
   */
  private Value decodeObjectToObject(Env env)
  {
    ObjectValue object = env.createObject();

    while (true) {
      int ch = skipWhitespace();

      if (ch == '}')
        break;

      unread();

      Value name = jsonDecodeImpl(env);

      ch = skipWhitespace();

      if (ch != ':')
        return errorReturn(env, "expected ':'");

      object.putField(env, name.toString(), jsonDecodeImpl(env));

      ch = skipWhitespace();

      if (ch == ',') {
      }
      else if (ch == '}')
        break;
      else
        return errorReturn(env, "expected either ',' or '}'");
    }

    return object;
  }

  /**
   * Returns a PHP string.
   */
  private Value decodeString(Env env)
  {
    StringValue sbv = env.createUnicodeBuilder();

    for (int ch = read(); ch >= 0; ch = read()) {

      switch (ch) {
        // Escaped Characters
        case '\\':
          ch = read();
          if (ch < 0)
            return errorReturn(env, "invalid escape character");

          switch (ch) {
            case '"':
              sbv.append('"');
              break;
            case '\\':
              sbv.append('\\');
              break;
            case '/':
              sbv.append('/');
              break;
            case 'b':
              sbv.append('\b');
              break;
            case 'f':
              sbv.append('\f');
              break;
            case 'n':
              sbv.append('\n');
              break;
            case 'r':
              sbv.append('\r');
              break;
            case 't':
              sbv.append('\t');
              break;
            case 'u':
            case 'U':
              int hex = 0;

              for (int i = 0; i < 4; i++) {
                hex = hex << 4;
                ch = read();

                if ('0' <= ch && ch <= '9')
                  hex += ch - '0';
                else if (ch >= 'a' && ch <= 'f')
                  hex += ch - 'a' + 10;
                else if (ch >= 'A' && ch <= 'F')
                  hex += ch - 'A' + 10;
                else
                  return errorReturn(env, "invalid escaped hex character");
              }

              sbv.append((char)hex);

          }
          break;

        case '"':
          return sbv;

        default:
          sbv.append((char)ch);
      }
    }

    return errorReturn(env, "error decoding string");
  }

  private Value errorReturn(Env env)
  {
    return errorReturn(env, null);
  }

  private Value errorReturn(Env env, String message)
  {
    int start;
    int end;

    if (_offset < _len) {
      start = _offset - 1;
      end = _offset;
    }
    else {
      start = _len - 1;
      end = _len;
    }

    String token = _string.substring(start, end).toString();

    if (message != null)
      env.warning(L.l("error parsing '{0}': {1}", token, message));
    else
      env.warning(L.l("error parsing '{0}'", token));

    return NullValue.NULL;
  }

  private void unread()
  {
    if (_offset > 0)
      _offset--;
  }

  private int peek(int index)
  {
    if (0 <= index && index < _len)
      return _string.charAt(index);
    else
      return -1;
  }

  private int read()
  {
    if (_offset < _len)
      return _string.charAt(_offset++);
    else
      return -1;
  }

  private int skipWhitespace()
  {
    int ch = read();
    for (; ch >= 0; ch = read()) {
      if (ch != ' ' &&
          ch != '\n' &&
          ch != '\r' &&
          ch != '\t')
        break;
    }

    return ch;
  }
}
