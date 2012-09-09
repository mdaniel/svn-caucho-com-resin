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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class EmailSanitizeFilter
  extends AbstractFilter
  implements SanitizeFilter
{
  @Override
  public Value filter(Env env, Value value, Value flagsV)
  {
    StringValue sb = env.createStringBuilder();

    StringValue str = value.toStringValue(env);
    int len = str.length();

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z') {
        sb.append(ch);
      }
      else if ('A' <= ch && ch <= 'Z') {
        sb.append(ch);
      }
      else if ('0' <= ch && ch <= '9') {
        sb.append(ch);
      }
      else if (ch == '@'
               || ch == '.'
               || ch == '_'
               || ch == '!'
               || ch == '#'
               || ch == '$'
               || ch == '%'
               || ch == '&'
               || ch == '\''
               || ch == '*'
               || ch == '+'
               || ch == '-'
               // || ch == '/'
               || ch == '='
               || ch == '?'
               || ch == '^'
               || ch == '`'
               || ch == '{'
               || ch == '|'
               || ch == '}'
               || ch == '~'
               || ch == '['
               || ch == ']') {
        sb.append(ch);
      }
      else {
        // do nothing
      }
    }

    return sb;
  }
}
