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

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.NumberValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.module.Optional;

import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JsonModule
    extends AbstractQuercusModule {

  private static final Logger log
    = Logger.getLogger(JsonModule.class.getName());
  private static final L10N L = new L10N(JsonModule.class);

  public String []getLoadedExtensions()
  {
    return new String[] { "json" };
  }

  public Object json_decode(String s)
  {
    throw new UnimplementedException();
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
  public String json_encode(Env env, Value val)
  {
    if (val instanceof StringValue)
      return "\"" + escape(val.toString()) + "\"";

    if (val == BooleanValue.TRUE)
      return "true";
    if (val == BooleanValue.FALSE)
      return "false";

    if (val instanceof NumberValue)
      return val.toString();

    if (val instanceof ArrayValue)
      return encodeArray(env, (ArrayValue)val);

    if (val instanceof ObjectValue)
      return encodeObject(env, (ObjectValue)val);

    if (val == NullValue.NULL || val == null)
      return "null";

    env.warning("json_encode: type is unsupported; encoded as null." );
    return "null";
  }

  private String encodeArray(Env env, ArrayValue val)
  {
    long index = 0;
    for (Value key : val.keySet()) {
      if ((! key.isLongConvertible()) || key.toLong() != index)
        return encodeArrayAsObject(env, val);
      index++;
    }

    StringBuilder sb = new StringBuilder("[");

    for (Value value : ((ArrayValue)val).values()) {
      if (sb.length() > 1)
        sb.append(",");
      sb.append(json_encode(env, value));
    }

    sb.append("]");
    return sb.toString();
  }

  /**
   * Encodes an associative array as a JSON object.
   */
  private String encodeArrayAsObject(Env env, ArrayValue val)
  {
      StringBuilder sb = new StringBuilder("{");

      for (Map.Entry<Value,Value> entry : val.entrySet()) {
        if (sb.length() > 1)
          sb.append(",");

        sb.append(json_encode(env, entry.getKey().toStringValue()));
        sb.append(":");
        sb.append(json_encode(env, entry.getValue()));
      }

      sb.append("}");
      return sb.toString();
  }

  private String encodeObject(Env env, ObjectValue val)
  {
      StringBuilder sb = new StringBuilder("{");

      for (Map.Entry<String,Value> entry : val.entrySet()) {
        if (sb.length() > 1)
          sb.append(",");

        sb.append(json_encode(env, new StringValueImpl(entry.getKey())));
        sb.append(":");
        sb.append(json_encode(env, entry.getValue()));
      }

      sb.append("}");
      return sb.toString();
  }

  /**
   * Escapes special/control characters.
   */
  private String escape(String s)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '"':
          sb.append("\\\"");
          break;
        case '/':
          sb.append("\\/");
          break;
        default:
          if (c > 0x1F) {
            sb.append(c);
            break;
          }
          if (c > 0x0F)
              sb.append("\\u00" + Integer.toHexString(c));
          else
              sb.append("\\u000" + Integer.toHexString(c));
      } //switch
    } //for

    return sb.toString();
  }

}
