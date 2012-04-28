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

package com.caucho.quercus.lib.json;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JsonEncodeContext;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;

public class JsonModule extends AbstractQuercusModule
{
  public static final int JSON_ERROR_NONE = 0;
  public static final int JSON_ERROR_DEPTH = 1;
  public static final int JSON_ERROR_STATE_MISMATCH = 2;
  public static final int JSON_ERROR_CTRL_CHAR = 3;
  public static final int JSON_ERROR_SYNTAX = 4;
  public static final int JSON_ERROR_UTF8 = 5;

  public static final int JSON_HEX_TAG = 1;
  public static final int JSON_HEX_AMP = 2;
  public static final int JSON_HEX_APOS = 4;
  public static final int JSON_HEX_QUOT = 8;
  public static final int JSON_FORCE_OBJECT = 16;
  public static final int JSON_NUMERIC_CHECK = 32;
  public static final int JSON_BIGINT_AS_STRING = 64;
  public static final int JSON_PRETTY_PRINT = 128;
  public static final int JSON_UNESCAPED_SLASHES = 256;
  public static final int JSON_UNESCAPED_UNICODE = 512;

  public String []getLoadedExtensions()
  {
    return new String[] { "json" };
  }

  /**
   * Returns a JSON-encoded String.
   *
   * JSON strings can be in any Unicode format (UTF-8, UTF-16, UTF-32).
   * Therefore need to pay special attention to multi-char characters.
   *
   * @param env
   * @param val to encode into json format
   * @return String JSON-encoded String
   */
  public static StringValue json_encode(Env env,
                                        Value val,
                                        @Optional int options)
  {
    boolean isEscapeTag = (options & JSON_HEX_TAG) > 0;
    boolean isEscapeAmp = (options & JSON_HEX_AMP) > 0;
    boolean isEscapeApos = (options & JSON_HEX_APOS) > 0;
    boolean isEscapeQuote = (options & JSON_HEX_QUOT) > 0;
    boolean isNumericCheck = (options & JSON_NUMERIC_CHECK) > 0;
    boolean isBigIntAsString = (options & JSON_BIGINT_AS_STRING) > 0;

    JsonEncodeContext context = new JsonEncodeContext(isEscapeTag,
                                                      isEscapeAmp,
                                                      isEscapeApos,
                                                      isEscapeQuote,
                                                      isNumericCheck,
                                                      isBigIntAsString);

    StringValue sb = env.createStringBuilder();

    val.jsonEncode(env, context, sb);

    return sb;
  }

  /**
   * Takes a JSON-encoded string and returns a PHP value.
   *
   * @param env
   * @param s JSON-encoded string.
   * @param assoc determines whether a generic PHP object or PHP associative
   *     array should be returned when decoding json objects.
   * @return decoded PHP value.
   */
  public static Value json_decode(Env env,
                                  StringValue s,
                                  @Optional("false") boolean assoc)
  {
    if (s.length() == 0)
      return new ArrayValueImpl();

    return (new JsonDecoder()).jsonDecode(env, s, assoc);
  }

  public static int json_last_error(Env env)
  {
    Object obj = env.getSpecialValue("json.last_error");

    if (obj == null) {
      return JSON_ERROR_NONE;
    }
    else {
      return ((Integer) obj).intValue();
    }
  }
}
