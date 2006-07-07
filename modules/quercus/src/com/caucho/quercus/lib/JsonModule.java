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
import com.caucho.quercus.env.ArrayValueImpl;
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

import java.util.regex.Pattern;

public class JsonModule
    extends AbstractQuercusModule {

  private static final Logger log
    = Logger.getLogger(JsonModule.class.getName());
  private static final L10N L = new L10N(JsonModule.class);

  private int _lastOffset;
  private boolean _associative;
  private Env _env;

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
    _env = env;
    StringBuilderValue sb = new StringBuilderValue();
    jsonEncodeImpl(sb, val);
    return sb;
  }

  private void jsonEncodeImpl(StringBuilderValue sb, Value val)
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
      encodeArray(sb, (ArrayValue)val);

    else if (val instanceof ObjectValue)
      encodeObject(sb, (ObjectValue)val);

    else if (val == NullValue.NULL || val == null)
      sb.append("null");

    else {
      _env.warning(L.l("json_encode: type is unsupported; encoded as null."));
      sb.append("null");
    }
  }

  private void encodeArray(StringBuilderValue sb, ArrayValue val)
  {
    long length = 0;
    for (Value key : val.keySet()) {
      if ((! key.isLongConvertible()) || key.toLong() != length) {
        encodeArrayAsObject(sb, val);
        return;
      }
      length++;
    }

    sb.append('[');

    length = 0;
    for (Value value : ((ArrayValue)val).values()) {
      if (length > 0)
        sb.append(',');
      jsonEncodeImpl(sb, value);
      length++;
    }

    sb.append(']');
  }

  /**
   * Encodes an associative array as a JSON object.
   */
  private void encodeArrayAsObject(StringBuilderValue sb, ArrayValue val)
  {
      sb.append('{');

      int length = 0;
      for (Map.Entry<Value,Value> entry : val.entrySet()) {
        if (length > 0)
          sb.append(',');

        jsonEncodeImpl(sb, entry.getKey().toStringValue());
        sb.append(':');
        jsonEncodeImpl(sb, entry.getValue());
        length++;
      }

      sb.append('}');
  }

  private void encodeObject(StringBuilderValue sb, ObjectValue val)
  {
      sb.append('{');

      int length = 0;
      for (Map.Entry<String,Value> entry : val.entrySet()) {
        if (length > 0)
          sb.append(',');

        jsonEncodeImpl(sb, new StringValueImpl(entry.getKey()));
        sb.append(':');
        jsonEncodeImpl(sb, entry.getValue());
        length++;
      }

      sb.append('}');
  }

  /**
   * Escapes special/control characters.
   */
  private void encodeString(StringBuilderValue sb, StringValue val)
  {
    String s = val.toString();

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
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
          // Need to escape control chars in range 0-0x1F
          if (c > 0x1F) {
            sb.append(c);
            break;
          }

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
            c += 48;
          else
            c += 87;

          sb.append((char)c);

      } //switch
    } //for

  }



  /***********************    json_decode functions    ***********************/



  /**
   * Takes a JSON-encoded string and returns a PHP value.
   *
   * @param env
   * @param s JSON-encoded string.
   * @param assoc determines whether a generic PHP object or PHP associative
   *     array is be returned when decoding json objects.
   * @return decoded PHP value.
   */
  public Value json_decode(Env env,
                                              StringValue stringValue,
                                              @Optional("false") boolean assoc)
  {
    try {
      _associative = assoc;
      _env = env;

      String s = stringValue.toString();
      Value val = jsonDecodeImpl(s, 0);

      // Should now be at end of string or have only white spaces left.
      for (_lastOffset++; _lastOffset < s.length(); _lastOffset++) {
        char c = s.charAt(_lastOffset);
        switch (c) {
          case ' ':
          case '\n':
          case '\r':
          case '\t':
            continue;
          default:
            throw new Exception("Error parsing json text; null returned(1).");
        }
      }

      return val;
    } catch (Exception e) {
      env.warning(L.l(e.toString()));
      log.log(Level.FINE, e.toString(), e);
      return NullValue.NULL;
    }
  }

  /**
   * Determines if the JSON-encoded string looks like an array, literal,
   * number, object, or string.  If it is a literal or number, then it decodes
   * the value on the spot.  For arrays, objects, and strings, it offloads the
   * decoding work to subfunctions, but consumes the first type-identifying
   * character to make life a little easier for the subfunctions.
   *
   * @param s
   * @param offset into s
   * @return decoded PHP value 
   */
  private Value jsonDecodeImpl(String s, int offset)
    throws Exception
  {
    for (; offset < s.length(); offset++) {
      char c = s.charAt(offset);

      switch (c) {
        case '"':
          return decodeString(s, offset + 1);

        case 't':
          if (s.charAt(++offset) != 'r' || s.charAt(++offset) != 'u' ||
              s.charAt(++offset) != 'e')
            throw new Exception("Error parsing json text; null returned(21).");

          _lastOffset = offset;
          return BooleanValue.TRUE;

        case 'f':
          if (s.charAt(++offset) != 'a' || s.charAt(++offset) != 'l' ||
               s.charAt(++offset) != 's' || s.charAt(++offset) != 'e')
            throw new Exception("Error parsing json text; null returned(22).");

          _lastOffset = offset;
          return BooleanValue.FALSE;

        case 'n':
          if (s.charAt(++offset) != 'u' || s.charAt(++offset) != 'l' ||
              s.charAt(++offset) != 'l')
            throw new Exception("Error parsing json text; null returned(23).");

          _lastOffset = offset;
          return NullValue.NULL;

        case '[':
          return decodeArray(s, offset + 1);

        case '{':
          return decodeObject(s, offset + 1);

        case ' ':
        case '\n':
        case '\r':
        case '\t':
          continue;

        default:
          if (c >= '1' && c <= '9')
            return decodeNumber(s, offset + 1, 2);
          if (c == 0)
            return decodeNumber(s, offset + 1, 1);
          if (c == '-')
            return decodeNumber(s, offset + 1, 0);
          throw new Exception("Error parsing json text; null returned(24).");
      }
    }

    throw new Exception("Error parsing json text; null returned(25).");
  }

  /**
   * Lazily checks to see if it looks like a number before converting.
   * XXX: do additional checks.  See JSON Internet Draft.
   */
  private Value decodeNumber(String s, int offset, int state)
  {
    boolean isDouble = false;

    int i = offset;
    for(; i < s.length(); i++) {
      char c = s.charAt(i);

      if (c >= '0' && c <= '9')
        continue;
      if (c == 'e' || c == 'E' || c == '.') {
        isDouble = true;
        continue;
      }
      if (c == '+' || c == '-')
        continue;

      break;
    }

    NumberValue val;

    if (isDouble)
      val = new DoubleValue(Double.parseDouble(s.substring(offset-1, i)));
    else
      val = new LongValue(Long.parseLong(s.substring(offset-1, i)));

    _lastOffset = i-1;
    return val;
  }

  /**
   * Not meant to be called by itself.  Use jsonDecodeImpl(String, offset).
   */
  private Value decodeArray(String s, int offset)
    throws Exception
  {
    boolean commaSeen = false;

    ArrayValueImpl array = new ArrayValueImpl();

    for (; offset < s.length(); offset++) {
      char c = s.charAt(offset);

      switch (c) {
        case ',':
          if (commaSeen || array.getSize() == 0)
            throw new Exception("Error decoding array; null returned(31).");
          commaSeen = true;
        case ' ':
        case '\n':
        case '\r':
        case '\t':
          continue;

        case ']':
          if (commaSeen)
            throw new Exception("Error decoding array; null returned(32).");

          _lastOffset = offset;
          return array;

        // Decode and append a value to this array.
        default:
          if ((! commaSeen) && array.getSize() > 0)
            throw new Exception("Error decoding array; null returned(33).");

          commaSeen = false;
          array.append(jsonDecodeImpl(s, offset));
          offset = _lastOffset;
      }
    }

    throw new Exception("Error parsing json text; null returned(34).");
  }

  /**
   * Decodes into an stdObject (or an associative array).
   * Not meant to be called by itself.  Use jsonDecodeImpl(String, offset).
   */
  private Value decodeObject(String s, int offset)
    throws Exception
  {
    boolean commaSeen = false;
    boolean colonSeen = false;
    Value name = null;

    ArrayValue array = null;
    ObjectValue object = null;

    if (_associative)
      array = new ArrayValueImpl();
    else
      object = _env.createObject();

    int size = 0;
    for(; offset < s.length(); offset++) {
      char c = s.charAt(offset);

      switch (c) {
        case ',':
          if (commaSeen || size == 0 || name != null)
            throw new Exception("Error decoding object; null returned(41).");
          commaSeen = true;
          continue;
        case ':':
          if (name == null)
            throw new Exception("Error decoding object; null returned(42).");
          colonSeen = true;
        case ' ':
        case '\n':
        case '\r':
        case '\t':
          continue;

        case '}':
          if (commaSeen || colonSeen || name != null)
            throw new Exception("Error decoding object; null returned(43).");

          _lastOffset = offset;
          if (_associative)
            return array;
          return object;

        // Decodes a name or value.  If value, then append name-value pair.
        default:
          if ((! commaSeen) && size > 0)
            throw new Exception("Error decoding object; null returned(44).");

          if (name == null) {
            name = jsonDecodeImpl(s, offset);
            if (! (name instanceof StringValue))
              throw new Exception("Error decoding object; null returned(45).");
          }
          else if (colonSeen) {
            if (_associative)
              array.append(name, jsonDecodeImpl(s, offset));
            else
              object.putFieldInit(_env, name.toString(), jsonDecodeImpl(s, offset));
            commaSeen = false;
            colonSeen = false;
            size++;
            name = null;
          }
          else
            throw new Exception("Error decoding object; null returned(46).");

          offset = _lastOffset;
      }
    }

    throw new Exception("Error parsing json text; null returned(47).");
  }

  /**
   * Not meant to be called by itself.  Use jsonDecodeImpl(String, offset).
   */
  private Value decodeString(String s, int offset)
    throws Exception
  {
    StringBuilderValue sbv = new StringBuilderValue();

    for (; offset < s.length(); offset++) {
      char c = s.charAt(offset);

      switch (c) {
        // Escaped Characters
        case '\\':
          offset++;
          if (offset >= s.length())
            throw new Exception(
              "Error decoding escaped string; null returned(51).");
          c = s.charAt(offset);

          switch (c) {
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
              if (offset + 4 >= s.length())
                throw new Exception(
                    "Error decoding escaped string; null returned(52).");

              int hex = 0;
              for (int i = 1; i <= 4; i++) {
                hex = hex << 4;
                char hexChar = s.charAt(offset + i);
                if (hexChar >= '0' && hexChar <= '9')
                  hex += hexChar - 48;
                else if (hexChar >= 'a' && hexChar <= 'f')
                  hex += hexChar - 87;
                else if (hexChar >= 'A' && hexChar <= 'F')
                  hex += hexChar - 55;
                else
                  throw new Exception(
                      "Error decoding escaped string; null returned(52).");
              }
              offset += 4;
              sbv.append((char)hex);

          } //end inner switch
          break;

        // End of string
        case '"':
          _lastOffset = offset;
          return sbv;

        default:
          sbv.append(c);
      } //end switch
    } //end for

    throw new Exception("Error decoding json string; null returned(53).");
  }
}
