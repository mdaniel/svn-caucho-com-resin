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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class StringSanitizeFilter
  extends AbstractFilter
  implements SanitizeFilter
{
  @Override
  protected Value filterImpl(Env env, Value value,
                             int flags, ArrayValue options)
  {
    StringValue str = value.toStringValue(env);
    StringValue sb = str.createStringBuilder();

    int len = str.length();

    boolean isEncodeLow = (flags & FilterModule.FILTER_FLAG_ENCODE_LOW) > 0;
    boolean isEncodeHigh = (flags & FilterModule.FILTER_FLAG_ENCODE_HIGH) > 0;
    boolean isStripLow = (flags & FilterModule.FILTER_FLAG_STRIP_LOW) > 0;
    boolean isStripHigh = (flags & FilterModule.FILTER_FLAG_STRIP_HIGH) > 0;

    boolean isEncodeAmp = (flags & FilterModule.FILTER_FLAG_ENCODE_AMP) > 0;
    boolean isNoEncodeQuotes = (flags & FilterModule.FILTER_FLAG_NO_ENCODE_QUOTES) > 0;

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if (0x00 <= ch && ch <= 0x1f) {
        if (isStripLow) {
        }
        else if (isEncodeLow) {
          appendEncoded(sb, ch);
        }
        else if (ch == 0x00) {
        }
        else {
          sb.append(ch);
        }
      }
      else if (0x80 <= ch) {
        if (isStripHigh) {
        }
        else if (isEncodeHigh) {
          appendEncoded(sb, ch);
        }
        else {
          sb.append(ch);
        }
      }
      else if (isEncodeAmp && ch == '&') {
        appendEncoded(sb, ch);
      }
      else if (ch == '"' || ch == '\'') {
        if (isNoEncodeQuotes) {
          sb.append(ch);
        }
        else {
          appendEncoded(sb, ch);
        }
      }
      else if (ch == '<') {
        while (++i < len) {
          if (str.charAt(i) == '>') {
            break;
          }
        }
      }
      else {
        sb.append(ch);
      }
    }

    return sb;
  }

  private static void appendEncoded(StringValue sb, char ch)
  {
    sb.append('&');
    sb.append('#');

    sb.append((int) ch);

    sb.append(';');
  }
}
