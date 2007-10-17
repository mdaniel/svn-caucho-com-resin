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

package com.caucho.quercus.lib.json;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class JsonModule
    extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(JsonModule.class.getName());
  private static final L10N L = new L10N(JsonModule.class);

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
  public StringValue json_encode(Env env, Value val)
  {
    StringValue sb = env.createUnicodeBuilder();

    jsonEncodeImpl(env, sb, val);
    return sb;
  }

  private void jsonEncodeImpl(Env env, StringValue sb, Value val)
  {
    if (val instanceof StringValue) {
      sb.append('"');
      encodeString(sb, (StringValue)val);
      sb.append('"');
    }

    else if (val == BooleanValue.TRUE)
      sb.append("true");
    else if (val == BooleanValue.FALSE)
      sb.append("false");

    else if (val instanceof NumberValue)
      sb.append(val.toStringValue());

    else if (val instanceof ArrayValue)
      encodeArray(env, sb, (ArrayValue)val);

    else if (val instanceof ObjectValue)
      encodeObject(env, sb, (ObjectValue)val);

    else if (val == NullValue.NULL || val == null)
      sb.append("null");

    else {
      env.warning(L.l("type is unsupported; encoded as null"));
    }
  }

  private void encodeArray(Env env, StringValue sb, ArrayValue val)
  {
    long length = 0;
    
    Iterator<Value> keyIter = val.getKeyIterator(env);
    
    while (keyIter.hasNext()) {
      Value key = keyIter.next();
      
      if ((! key.isLongConvertible()) || key.toLong() != length) {
        encodeArrayToObject(env, sb, val);
        return;
      }
      length++;
    }

    sb.append('[');

    length = 0;
    for (Value value : ((ArrayValue)val).values()) {
      if (length > 0)
        sb.append(',');
      jsonEncodeImpl(env, sb, value);
      length++;
    }

    sb.append(']');
  }

  /**
   * Encodes an associative array into a JSON object.
   */
  private void encodeArrayToObject(Env env, StringValue sb, ArrayValue val)
  {
      sb.append('{');

      int length = 0;
      for (Map.Entry<Value,Value> entry : val.entrySet()) {
        if (length > 0)
          sb.append(',');

        jsonEncodeImpl(env, sb, entry.getKey().toStringValue());
        sb.append(':');
        jsonEncodeImpl(env, sb, entry.getValue());
        length++;
      }

      sb.append('}');
  }

  /**
   * Encodes an PHP object into a JSON object.
   */
  private void encodeObject(Env env, StringValue sb, ObjectValue val)
  {
      sb.append('{');

      int length = 0;
      for (Map.Entry<Value,Value> entry : val.entrySet()) {
        if (length > 0)
          sb.append(',');

        jsonEncodeImpl(env, sb, entry.getKey().toStringValue());
        sb.append(':');
        jsonEncodeImpl(env, sb, entry.getValue());
        length++;
      }

      sb.append('}');
  }

  /**
   * Escapes special/control characters.
   */
  private void encodeString(StringValue sb, StringValue val)
  {
    int len = val.length();
    for (int i = 0; i < len; i++) {
      char c = val.charAt(i);

      switch (c) {
        case '\b':
          sb.append('\\');
          sb.append('b');
          break;
        case '\f':
          sb.append('\\');
          sb.append('f');
          break;
        case '\n':
          sb.append('\\');
          sb.append('n');
          break;
        case '\r':
          sb.append('\\');
          sb.append('r');
          break;
        case '\t':
          sb.append('\\');
          sb.append('t');
          break;
        case '\\':
          sb.append('\\');
          sb.append('\\');
          break;
        case '"':
          sb.append('\\');
          sb.append('"');
          break;
        case '/':
          sb.append('\\');
          sb.append('/');
          break;
        default:
          if (c > 0x1F) {
            sb.append(c);
            break;
          }

          // Need to escape control chars in range 0-0x1F
          sb.append('\\');
          sb.append('u');
          sb.append('0');
          sb.append('0');

          if (c <= 0x0F)
            sb.append('0');
          else {
            sb.append('1');
            c &= 0x0F;
          }

          if (c <= 0x09)
            c += '0';
          else
            c += 'a' - 10;

          sb.append(c);
      }
    }
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
  public Value json_decode(Env env,
                          StringValue s,
                          @Optional("false") boolean assoc)
  {
    return (new JsonDecoder()).jsonDecode(env, s, assoc);
  }

}
