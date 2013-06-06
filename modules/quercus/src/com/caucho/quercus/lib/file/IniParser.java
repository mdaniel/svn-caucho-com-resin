/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   @author Nam Nguyen
 */

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeBuilderValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.ReadStream;

import java.io.IOException;

public class IniParser {
  public static ArrayValue parse(Env env, ReadStream is, boolean isProcessSections)
    throws IOException
  {
    ArrayValue top = new ArrayValueImpl();
    ArrayValue section = top;

    while (true) {
      int ch = skipWhitespaces(is);

      if (ch < 0) {
        break;
      }
      else if (ch == ';') {
        skipToNewline(is);
      }
      else if (ch == '[') {
        StringValue key = parseBracketKey(env, is);

        if (isProcessSections) {
          section = new ArrayValueImpl();

          top.put(key, section);
        }
      }
      else {
        is.unread();
        parseIniLine(env, is, section);
      }
    }

    return top;
  }

  private static StringValue parseBracketKey(Env env, ReadStream is)
    throws IOException
  {
    StringValue sb = env.createStringBuilder();

    int ch = skipSpacesAndTabs(is);

    int quoteChar = -1;
    if (ch == '"' || ch == '\'') {
      quoteChar = ch;
      ch = is.read();
    }

    while (true) {
      if (ch < 0
          || ch == quoteChar
          || ch == ']' && quoteChar < 0) {
        break;
      }
      else if (ch == '\r' || ch == '\n') {
        throw new IOException("expected ']' but saw end of line");
      }

      sb.append((char) ch);
      ch = is.read();
    }

    if (ch == quoteChar) {
      for (ch = is.read(); ch >= 0 && ch != ']'; ch = is.read()) {
      }
    }

    return sb;
  }

  private static void parseIniLine(Env env, ReadStream is, ArrayValue section)
    throws IOException
  {
    StringValue name = parseIniName(env, is);
    StringValue key = null;
    Value value;

    int ch = skipSpacesAndTabs(is);

    if (ch == '[') {
      key = parseBracketKey(env, is);

      ch = skipSpacesAndTabs(is);
    }

    if (ch != '=') {
      throw new IOException("expected '=', but saw '" + ((char) ch) + "' (" + ch + ")");
    }

    value = parseIniValue(env, is);

    if (key != null) {
      Value array = section.get(name);

      if (array == UnsetValue.UNSET) {
        array = new ArrayValueImpl();

        section.put(name, array);
      }

      array.put(key, value);
    }
    else {
      section.put(name, value);

    }
  }

  private static StringValue parseIniName(Env env, ReadStream is)
    throws IOException
  {
    StringValue sb = env.createStringBuilder();

    int ch = skipSpacesAndTabs(is);

    int quoteChar = -1;
    if (ch == '"' || ch == '\'') {
      quoteChar = ch;
      ch = is.read();
    }

    while (true) {
      if (ch < 0
          || ch == quoteChar
          || ch == '\r' || ch == '\n'
          || quoteChar < 0 && ! isValidIniKeyChar(ch)) {
        break;
      }

      sb.append((char) ch);
      ch = is.read();
    }

    if (ch == quoteChar) {
      for (ch = is.read(); ch >= 0 && ch != ']'; ch = is.read()) {
      }
    }

    is.unread();

    int len = sb.length();
    for (; len > 0; len--) {
      ch = sb.charAt(len - 1);

      if (! Character.isWhitespace(ch)) {
        break;
      }
    }

    if (len != sb.length()) {
      if (sb instanceof StringBuilderValue) {
        ((StringBuilderValue) sb).setLength(len);
      }
      else {
        ((UnicodeBuilderValue) sb).setLength(len);
      }
    }

    return sb;
  }

  private static Value parseIniValue(Env env, ReadStream is)
    throws IOException
  {
    int ch = skipSpacesAndTabs(is);

    if (ch == '\r' || ch == '\n')
      return NullValue.NULL;

    if (ch == '"' || ch == '\'') {
      int quoteChar = ch;

      StringValue sb = env.createStringBuilder();

      for (ch = is.read(); ch >= 0 && ch != quoteChar; ch = is.read()) {
        sb.append((char) ch);
      }

      skipToNewline(is);

      return sb;
    }
    else {
      StringBuilder sb = new StringBuilder();

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
        if (ch == ';') {
          skipToNewline(is);
          break;
        }
        else if (ch == '$') {
          int peek = is.read();

          if (peek == '{') {
            StringBuilder var = new StringBuilder();

            for (ch = is.read();
                 ch >= 0 && ch != '\r' && ch != '\n' && ch != '}';
                 ch = is.read()) {
              var.append((char) ch);
            }

            Value value = env.getIni(var.toString());

            if (value != null) {
              sb.append(value);
            }
          }
          else {
            sb.append('$');
            is.unread();
          }

        }
        else if (ch == '"') {
          StringValue result = env.createStringBuilder();

          String value = sb.toString().trim();

          result.append(getIniConstant(env, value));

          for (ch = is.read(); ch >= 0 && ch != '"'; ch = is.read()) {
            result.append((char) ch);
          }

          skipToNewline(is);

          return result;
        }
        else {
          sb.append((char) ch);
        }
      }

      String value = sb.toString().trim();

      return getIniConstant(env, value);
    }
  }

  private static Value getIniConstant(Env env, String value)
  {
    if (value.equalsIgnoreCase("null")) {
      return env.getEmptyString();
    }
    else if (value.equalsIgnoreCase("true")
             || value.equalsIgnoreCase("yes")) {
      return env.createString("1");
    }
    else if (value.equalsIgnoreCase("false")
             || value.equalsIgnoreCase("no")) {
      return env.getEmptyString();
    }
    else if (env.isDefined(value)) {
      return env.getConstant(value);
    }
    else {
      return env.createString(value);
    }
  }

  private static boolean isValidIniKeyChar(int ch)
  {
    if (ch <= 0
        || ch == '='
        || ch == ';'
        || ch == '{'
        || ch == '}'
        || ch == '|'
        || ch == '&'
        || ch == '~'
        || ch == '!'
        || ch == '['
        || ch == '('
        || ch == ')'
        || ch == '"')
      return false;
    else
      return true;
  }

  private static int skipWhitespaces(ReadStream is)
    throws IOException
  {
    int ch;

    for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
    }

    return ch;
  }

  private static int skipSpacesAndTabs(ReadStream is)
    throws IOException
  {
    int ch;

    while (true) {
      ch = is.read();

      if (ch != ' ' && ch != '\t') {
        break;
      }
    }

    return ch;
  }

  private static int skipToNewline(ReadStream is)
    throws IOException
  {
    int ch;

    while (true) {
      ch = is.read();

      if (ch < 0 || ch == '\r' || ch == '\n') {
        break;
      }
    }

    return ch;
  }
}
