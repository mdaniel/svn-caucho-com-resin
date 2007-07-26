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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.i18n;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Unicode handling.  Also includes iconv, etc.
 */
public class UnicodeModule extends AbstractQuercusModule {
  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  private static final Logger log =
                       Logger.getLogger(UnicodeModule.class.getName());
  private static final L10N L = new L10N(UnicodeModule.class);

  public static final int U_INVALID_STOP = 0;
  public static final int U_INVALID_SKIP = 1;
  public static final int U_INVALID_SUBSTITUTE = 2;
  public static final int U_INVALID_ESCAPE = 3;

  public static final String ICONV_IMPL = "QuercusIconv";
  public static final String ICONV_VERSION = "1.0";
  
  public static final int ICONV_MIME_DECODE_STRICT = 1;
  public static final int ICONV_MIME_DECODE_CONTINUE_ON_ERROR = 2;

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  public static BooleanValue unicode_semantics(Env env)
  {
    return BooleanValue.TRUE;
  }

  public static UnicodeValue unicode_decode(Env env,
                              BinaryValue str,
                              String encoding)
  {
    return str.toUnicodeValue(env, encoding);
  }

  public static BinaryValue unicode_encode(Env env,
                              UnicodeValue str,
                              String encoding)
  {
    return str.toBinaryValue(env, encoding);
  }

  /**
   * Returns the current encoding.
   *
   * @param env
   * @param type setting to return
   * @return iconv environment settings
   */
  public static Value iconv_get_encoding(Env env,
                       @Optional("all") String type)
  {
    type = type.toLowerCase();

    if ("all".equals(type)) {
      ArrayValue array = new ArrayValueImpl();
      array.put("input_encoding",
          env.getIniString("iconv.input_encoding"));
      array.put("output_encoding",
          env.getIniString("iconv.output_encoding"));
      array.put("internal_encoding",
          env.getIniString("iconv.internal_encoding"));
      return array;
    }

    if ("input_encoding".equals(type))
      return new StringValueImpl(env.getIniString("iconv.input_encoding"));
    else if ("output_encoding".equals(type))
      return new StringValueImpl(env.getIniString("iconv.output_encoding"));
    else if ("internal_encoding".equals(type))
      return new StringValueImpl(env.getIniString("iconv.internal_encoding"));

    return BooleanValue.FALSE;
  }

  /**
   * Decodes all the headers and place them in an array.
   * Use iconv.internal_encoding.
   * XXX: mode, line-length, line-break-chars
   *
   * @param env
   * @param encoded_headers
   * @param mode controls error recovery
   * @param charset
   */
  public static Value iconv_mime_decode_headers(Env env,
                       StringValue encoded_headers,
                       @Optional() int mode,
                       @Optional() String charset)
  {
    if (charset.length() == 0)
      charset = env.getIniString("iconv.internal_encoding");

    try {
      return decodeMimeHeaders(env, encoded_headers, charset);
    }
    catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }
    catch (MessagingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }

    return BooleanValue.FALSE;
  }

  private static Value decodeMimeHeaders(Env env,
                              StringValue encodedHeaders,
                              String charset)
    throws MessagingException, UnsupportedEncodingException
  {
    ArrayValue headers = new ArrayValueImpl();

    Enumeration<Header> enumeration
        = new InternetHeaders(encodedHeaders.toInputStream()).getAllHeaders();

    while (enumeration.hasMoreElements()) {
      Header header = enumeration.nextElement();

      StringValue name
          = IconvUtility.decodeMime(env, header.getName(), charset);
      StringValue val
          = IconvUtility.decodeMime(env, header.getValue(), charset);

      Value headerName;
      if ((headerName = headers.containsKey(name)) == null) {
        headers.put(name, val);
        continue;
      }

      ArrayValue inner;
      if (headerName.isArray()) {
        inner = headerName.toArrayValue(env);
      }
      else {
        inner = new ArrayValueImpl();
        inner.put(headerName);
      }

      inner.put(val);
      headers.put(name, inner);
    }

    return headers;
  }

  /**
   * Uses iconv.internal_encoding.
   *
   * XXX: mode ignored
   *
   * @param env
   * @param encoded_header
   * @param mode controls error recovery
   * @param charset to encode resultant 
   */
  public static Value iconv_mime_decode(Env env,
                              StringValue encoded_header,
                              @Optional("1") int mode,
                              @Optional("") String charset)
  {
    if (charset.length() == 0)
      charset = env.getIniString("iconv.internal_encoding");

    try {
      Enumeration<String> enumeration = new InternetHeaders(
          encoded_header.toInputStream()).getAllHeaderLines();

      if (! enumeration.hasMoreElements()) {
        env.warning(L.l("Error parsing header."));
        return BooleanValue.FALSE;
      }

      return IconvUtility.decodeMime(env, enumeration.nextElement(), charset);

    }
    catch (MessagingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }
    catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }

    return BooleanValue.FALSE;
  }

  /**
   * Encodes a MIME header.
   *
   * XXX: preferences
   *
   * @param field_name header field name
   * @param field_value header field value
   * @param preferences
   */
  public static Value iconv_mime_encode(Env env,
                              StringValue field_name,
                              StringValue field_value,
                              @Optional() ArrayValue preferences)
  {
    try {
      String scheme = "B";
      String lineBreakChars = "\r\n";
      String inCharset = env.getIniString("iconv.internal_encoding");
      String outCharset = inCharset;
      int lineLength = 76;

      if (preferences != null) {
        Value tmp = new StringValueImpl("scheme");
        if ((tmp = preferences.get(tmp)).isset())
          scheme = tmp.toString();

        tmp = new StringValueImpl("line-break-chars");
        if ((tmp = preferences.get(tmp)).isset())
          lineBreakChars = tmp.toString();

        tmp = new StringValueImpl("input-charset");
        if ((tmp = preferences.get(tmp)).isset())
          inCharset = tmp.toString();

        tmp = new StringValueImpl("output-charset");
        if ((tmp = preferences.get(tmp)).isset())
          outCharset = tmp.toString();

        tmp = new StringValueImpl("line-length");
        if ((tmp = preferences.get(tmp)).isset()) {
        if (tmp.isLongConvertible())
          lineLength = (int)tmp.toLong();
        }
      }

      return IconvUtility.encodeMime(env,
                                     field_name,
                                     field_value,
                                     inCharset,
                                     outCharset,
                                     scheme,
                                     lineBreakChars,
                                     lineLength);

    }
    catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Sets the current encoding.
   * @param env
   * @param type
   * @param charset
   */
  public static BooleanValue iconv_set_encoding(Env env,
                       String type,
                       StringValue charset)
  {
    type = type.toLowerCase();

    if ("input_encoding".equals(type)) {
      env.setIni("iconv.input_encoding", charset);
      return BooleanValue.TRUE;
    }
    else if ("output_encoding".equals(type)) {
      env.setIni("iconv.output_encoding", charset);
      return BooleanValue.TRUE;
    }
    else if ("internal_encoding".equals(type)) {
      env.setIni("iconv.internal_encoding", charset);
      return BooleanValue.TRUE;
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns the length of the decoded string.
   * Uses iconv.internal_encoding.
   *
   * @param env
   * @param str
   * @param charset
   */
  public static Value iconv_strlen(Env env,
                       StringValue str,
                       @Optional("") String charset)
  {
    if (charset.length() == 0 )
      charset = env.getIniString("iconv.internal_encoding");

    return LongValue.create(str.toUnicodeValue(env, charset).length());
  }

  /**
   * Returns the first occurence of the substring in the haystack.
   * Uses iconv.internal_encoding.
   *
   * @param env
   * @param haystack
   * @param needle
   * @param offset
   * @param charset
   * @return first occurence of needle in haystack, FALSE otherwise
   */
  public static Value iconv_strpos(Env env,
                       StringValue haystack,
                       StringValue needle,
                       @Optional("0") int offset,
                       @Optional("") String charset)
  {
    if (charset.length() == 0)
      charset = env.getIniString("iconv.internal_encoding");

    haystack = haystack.toUnicodeValue(env, charset);
    needle = needle.toUnicodeValue(env, charset);

    int index = haystack.indexOf(needle, offset);

    if (index < 0)
      return BooleanValue.FALSE;

    return LongValue.create(index);
  }

  /**
   * Returns the last occurence of the substring in the haystack.
   * Uses iconv.internal_encoding.
   *
   * @param env
   * @param haystack
   * @param needle
   * @param charset encoding of StringValue arguments
   *
   * @return last occurence of needle in haystack, FALSE otherwise
   */
  public static Value iconv_strrpos(Env env,
                       StringValue haystack,
                       StringValue needle,
                       @Optional("") String charset)
  {
    if (charset.length() == 0)
      charset = env.getIniString("iconv.internal_encoding");

    haystack = haystack.toUnicodeValue(env, charset);
    needle = needle.toUnicodeValue(env, charset);

    int index = haystack.lastIndexOf(needle);

    if (index < 0)
      return BooleanValue.FALSE;

    return LongValue.create(index);
  }

  /**
   * Uses iconv.internal_encoding.
   *
   * @param env
   * @param str encoded string
   * @param offset of str after decoding
   * @param length of str after decoding
   * @param charset encoding of StringValue argument
   * @return substring of argument string.
   *
   */
  public static Value iconv_substr(Env env,
                       StringValue str,
                       int offset,
                       @Optional("7fffffff") int length,
                       @Optional("") String charset)
  {
    if (charset.length() == 0)
      charset = env.getIniString("iconv.internal_encoding");

    str = str.toUnicodeValue(env, charset);

    int tail;
    int strlen = str.length();

    // Imitating PHP5 behavior
    if (offset < 0)
      offset = strlen + offset;

    if (length < 0)
      tail = strlen + length;
    else if (length > strlen - offset)
      tail = strlen;
    else
      tail = offset + length;

    if (offset < 0 || tail < offset)
      return StringValue.EMPTY;

    str = str.substring(offset, tail);

    return str.toBinaryValue(env, charset);
  }

  /**
   * Returns encoded string from decoded argument string.
   *
   * @param env
   * @param in_charset charset to decode from
   * @param out_charset charset to decode to
   * @param str to decode and encode
   */
  public static Value iconv(Env env,
                       String in_charset,
                       String out_charset,
                       StringValue str)
  {
    if (out_charset.endsWith("//TRANSLIT")) {
      env.stub("Iconv TRANSLIT option not supported");

      int length = out_charset.length() - "//TRANSLIT".length();

      out_charset = out_charset.substring(0, length);
    }

    try {
      return IconvUtility.decodeEncode(str, in_charset, out_charset);
    }
    catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l("error converting {1} to {2}", in_charset, out_charset));

      return BooleanValue.FALSE;
    }
  }

  public static StringValue ob_iconv_handler(
                       StringValue contents,
                       int status)
  {
    throw new UnimplementedException("ob_iconv_handler");
  }

  static {
    addIni(_iniMap, "unicode.fallback_encoding", "utf-8", PHP_INI_ALL);
    addIni(_iniMap, "unicode.from_error_mode", "2", PHP_INI_ALL);
    addIni(_iniMap, "unicode.from_error_subst_char", "3f", PHP_INI_ALL);
    addIni(_iniMap, "unicode.http_input_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.output_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.runtime_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.script_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.semantics", "off", PHP_INI_SYSTEM);

    addIni(_iniMap, "iconv.input_encoding", "utf-8", PHP_INI_ALL);
    addIni(_iniMap, "iconv.output_encoding", "utf-8", PHP_INI_ALL);
    addIni(_iniMap, "iconv.internal_encoding", "utf-8", PHP_INI_ALL);
  }
}

// XXX: "//TRANSLIT" and "//IGNORE" charset suffixes
