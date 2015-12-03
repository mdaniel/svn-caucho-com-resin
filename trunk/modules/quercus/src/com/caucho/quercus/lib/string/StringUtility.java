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

package com.caucho.quercus.lib.string;

import java.io.IOException;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Post;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.util.L10N;
import com.caucho.vfs.ByteToChar;

public class StringUtility
{
  private static final L10N L = new L10N(StringModule.class);

  public static Value parseStr(Env env,
                               CharSequence str,
                               ArrayValue result,
                               boolean isRef,
                               String encoding,
                               boolean isReplaceSpacesWithUnderscores)
  {
    return parseStr(env, str, result, isRef, encoding,
                    env.getIniBoolean("magic_quotes_gpc"),
                    isReplaceSpacesWithUnderscores,
                    Env.DEFAULT_QUERY_SEPARATOR_MAP);
  }

  public static Value parseStr(Env env,
                               CharSequence str,
                               ArrayValue result,
                               boolean isRef,
                               String encoding,
                               boolean isMagicQuotes,
                               boolean isReplaceSpacesWithUnderscores,
                               int []querySeparatorMap)
  {
    try {
      ByteToChar byteToChar = env.getByteToChar();

      if (encoding != null)
        byteToChar.setEncoding(encoding);

      int len = str.length();

      for (int i = 0; i < len; i++) {
        int ch = 0;
        byteToChar.clear();

        for (;
             i < len && isSeparator(querySeparatorMap, ch = str.charAt(i));
             i++) {
        }

        for (; i < len
               && (ch = str.charAt(i)) != '='
               && ! isSeparator(querySeparatorMap, ch);
            i++) {
          i = addQueryChar(byteToChar, str, len, i, ch, querySeparatorMap);
        }

        String key = byteToChar.getConvertedString();

        byteToChar.clear();

        String value;
        if (ch == '=') {
          for (i++; i < len
               && ! isSeparator(querySeparatorMap, (ch = str.charAt(i))); i++) {
            i = addQueryChar(byteToChar, str, len, i, ch, querySeparatorMap);
          }

          value = byteToChar.getConvertedString();
        }
        else {
          value = "";
        }

        if (key.length() == 0) {
          // php/php/080d
          // http://bugs.caucho.com/view.php?id=4998

          continue;
        }
        else if (isRef) {
          Post.addFormValue(env, result, key,
                            new String[] { value },
                            isMagicQuotes, isReplaceSpacesWithUnderscores);
        }
        else {
          // If key is an exsiting array, then append
          // this value to existing array
          // Only use extract(EXTR_OVERWRITE) on non-array variables or
          // non-existing arrays
          int openBracketIndex = key.indexOf('[');
          int closeBracketIndex = key.indexOf(']');
          if (openBracketIndex == 0) {
            // http://bugs.caucho.com/view.php?id=4998

            continue;
          }
          else if (openBracketIndex > 0) {
            String arrayName = key.substring(0, openBracketIndex);
            arrayName = arrayName.replace('.', '_');

            Value v = env.getVar(arrayName).getRawValue();
            if (v instanceof ArrayValue) {
              //Check to make sure valid string (ie: foo[...])
              if (closeBracketIndex < 0) {
                env.warning(L.l("invalid array {0}", key));
                return NullValue.NULL;
              }

              if (closeBracketIndex > openBracketIndex + 1) {
                String index = key.substring(key.indexOf('[') + 1,
                                             key.indexOf(']'));
                v.put(env.createString(index), env.createString(value));
              } else {
                v.put(env.createString(value));
              }
            } else {
              Post.addFormValue(env, result, key,
                                new String[] { value },
                                isMagicQuotes, isReplaceSpacesWithUnderscores);
            }
          } else {
            Post.addFormValue(env, result, key,
                              new String[] { value },
                              isMagicQuotes, isReplaceSpacesWithUnderscores);
          }
        }
      }

      if (! isRef) {
        ArrayModule.extract(env, result,
                            ArrayModule.EXTR_OVERWRITE,
                            null);
      }

      return NullValue.NULL;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  private static boolean isSeparator(int []sep, int ch)
  {
    return (ch < sep.length && sep[ch] > 0);
  }

  protected static int addQueryChar(ByteToChar byteToChar,
                                    CharSequence str,
                                    int len,
                                    int i,
                                    int ch,
                                    int[] querySeparatorMap)
    throws IOException
  {
    if (str == null)
      str = "";

    switch (ch) {
    case '+':
      byteToChar.addChar(' ');
      return i;

    case '%':
      char ch1;
      char ch2;

      if (i + 2 < len
          && (ch1 = str.charAt(i + 1)) != '='
          && ! isSeparator(querySeparatorMap, ch1)
          && (ch2 = str.charAt(i + 2)) != '='
          && ! isSeparator(querySeparatorMap, ch2)) {
        int d1 = StringModule.hexToDigit(ch1);
        int d2 = StringModule.hexToDigit(ch2);

        // XXX: d1 and d2 may be -1 if not valid hex chars
        byteToChar.addByte(d1 * 16 + d2);

        return i + 2;
      }
      else {
        byteToChar.addByte((byte) ch);
        return i;
      }

    default:
      byteToChar.addByte((byte) ch);
      return i;
    }
  }

  public static void addQueryValue(Env env, ArrayValue array,
                                   String key, String valueStr)
  {
    if (key == null)
      key = "";

    if (valueStr == null)
      valueStr = "";

    int p;

    Value value = env.createString(valueStr);

    if ((p = key.indexOf('[')) > 0 && key.endsWith("]")) {
      String index = key.substring(p + 1, key.length() - 1);
      key = key.substring(0, p);

      StringValue keyValue = env.createString(key);

      Value part;

      if (array != null)
        part = array.get(keyValue);
      else
        part = env.getVar(keyValue);

      if (! part.isArray())
        part = new ArrayValueImpl();

      if (index.equals(""))
        part.put(value);
      else
        part.put(env.createString(index), value);

      if (array != null)
        array.put(keyValue, part);
      else
        env.setVar(key, part);
    }
    else {
      if (array != null)
        array.put(env.createString(key), value);
      else
        env.setVar(key, value);
    }
  }
}
