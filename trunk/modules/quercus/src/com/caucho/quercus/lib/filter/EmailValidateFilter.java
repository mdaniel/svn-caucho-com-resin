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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.filter;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * Coded to RFC822 specifications. XXX: RFC2822.
 */
public class EmailValidateFilter
  extends AbstractFilter
  implements ValidateFilter
{
  @Override
  public Value filter(Env env, Value value, Value flagV)
  {
    StringValue str = value.toStringValue(env);

    EmailParser parser = new EmailParser(str);

    boolean isValid = parser.parse();

    if (isValid) {
      return value;
    }
    else {
      return BooleanValue.FALSE;
    }
  }

  static class EmailParser
  {
    private final StringValue _s;
    private final int _length;

    private int _offset;

    public EmailParser(StringValue s)
    {
      _s = s;
      _length = s.length();
    }

    protected int getOffset()
    {
      return _offset;
    }

    public boolean parse()
    {
      int atSignPos = _s.indexOf('@');

      if (atSignPos < 0) {
        return false;
      }

      if (! parseLocalPart()) {
        return false;
      }

      if (read() != '@') {
        return false;
      }

      if (! parseDomain()) {
        return false;
      }

      return _length <= _offset;
    }

    private boolean parseLocalPart()
    {
      if (! parseWord()) {
        return false;
      }

      while (true) {
        int ch = read();

        if (ch == '.') {
          if (! parseWord()) {
            unread();
            break;
          }
        }
        else {
          unread();
          break;
        }
      }

      return true;
    }

    private boolean parseWord()
    {
      int ch = peek();

      if (ch == '"') {
        return parseQuotedString();
      }
      else {
        return parseAtom();
      }
    }

    private boolean parseAtom()
    {
      if (! parseAtomInner()) {
        return false;
      }

      while (true) {
        if (! parseAtomInner()) {
          break;
        }
      }

      return true;
    }

    private boolean parseAtomInner()
    {
      int ch = read();

      if (ch < 0) {
        unread();

        return false;
      }
      else if (isSpecial(ch)) {
        unread();

        return false;
      }
      else if (isCtrl(ch)) {
        unread();

        return false;
      }
      else if (ch == ' ') {
        unread();

        return false;
      }
      else {
        return true;
      }
    }

    private static boolean isSpecial(int ch)
    {
      return ch == '('
             || ch == ')'
             || ch == '<'
             || ch == '>'
             || ch == '@'
             || ch == ','
             || ch == ';'
             || ch == ':'
             || ch == '\\'
             || ch == '"'
             || ch == '.'
             || ch == '['
             || ch == ']';
    }

    private static boolean isCtrl(int ch)
    {
      return ch <= 0x1f || ch == 0xff;
    }

    private boolean parseQuotedString()
    {
      int ch = read();

      if (ch != '"') {
        unread();

        return false;
      }

      while (true) {
        ch = read();

        if (ch < 0
            || ch == '"') {
          unread();
          break;
        }
        else if (ch == '\\') {
          if (read() < 0) {
            unread();
            unread();

            break;
          }
        }
        else if (ch == '\r') {
          if (peek() == '\n') {
            unread();

            if (! parseLinearWhitespace()) {
              break;
            }
          }
          else {
            unread();

            break;
          }
        }
      }

      return read() == '"';
    }

    private boolean parseDomain()
    {
      if (! parseSubDomain()) {
        return false;
      }

      while (true) {
        int ch = read();

        if (ch == '.') {
          if (! parseSubDomain()) {
            unread();
            break;
          }
        }
        else {
          unread();
          break;
        }
      }

      return true;
    }

    private boolean parseSubDomain()
    {
      int ch = peek();

      if (ch == '[') {
        return parseDomainLiteral();
      }
      else {
        return parseDomainRef();
      }
    }

    private boolean parseDomainLiteral()
    {
      int ch = read();

      if (ch != '[') {
        return false;
      }

      while (true) {
        ch = read();

        if (ch == '\\') {
          if (read() < 0) {
            return false;
          }
        }
        else {
          unread();

          if (! parseDomainText()) {
            break;
          }
        }
      }

      return read() == ']';
    }

    private boolean parseDomainText()
    {
      int ch = read();

      if (ch < 0) {
        unread();

        return false;
      }
      else if (ch == '[' || ch == ']' || ch == '\\') {
        unread();

        return false;
      }
      else if (ch == '\r') {
        if (peek() == '\n') {
          unread();

          return parseLinearWhitespace();
        }
        else {
          unread();
          return false;
        }
      }
      else {
        return true;
      }
    }

    private boolean parseLinearWhitespace()
    {
      if (! parseLinearWhitespaceInner()) {
        return false;
      }

      while (true) {
        if (! parseLinearWhitespaceInner()) {
          break;
        }
      }

      return true;
    }

    private boolean parseLinearWhitespaceInner()
    {
      boolean isSawCrLf = false;
      int ch = read();

      if (ch == '\r') {
        if (read() == '\n') {
          ch = read();
          isSawCrLf =  true;
        }
        else {
          unread();
          unread();

          return false;
        }
      }

      if (ch == ' ' || ch == '\t') {
        return true;
      }
      else {
        unread();

        if (isSawCrLf) {
          unread();
          unread();
        }

        return false;
      }
    }

    private boolean parseDomainRef()
    {
      return parseAtom();
    }

    private int read()
    {
      int offset = _offset++;

      if (_length <= offset) {
        return -1;
      }
      else {
        return _s.charAt(offset);
      }
    }

    private int peek()
    {
      if (_length <= _offset) {
        return -1;
      }
      else {
        return _s.charAt(_offset);
      }
    }

    private void unread()
    {
      _offset--;
    }
  }
}
