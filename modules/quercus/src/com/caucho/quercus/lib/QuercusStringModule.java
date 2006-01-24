/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.util.Map;
import java.util.Iterator;
import java.util.Locale;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.CRC32;

import java.io.InputStream;
import java.io.IOException;

import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.LocaleInfo;
import com.caucho.quercus.env.Post;

import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.UsesSymbolTable;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.AbstractQuercusModule;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import com.caucho.vfs.ByteToChar;
import com.caucho.vfs.Path;

/**
 * PHP functions implemented from the string module
 */
public class QuercusStringModule extends AbstractQuercusModule {
  private static final Logger log =
    Logger.getLogger(QuercusStringModule.class.getName());

  private static final L10N L = new L10N(QuercusStringModule.class);

  public static final int CRYPT_SALT_LENGTH = 2;
  public static final int CRYPT_STD_DES = 0;
  public static final int CRYPT_EXT_DES = 0;
  public static final int CRYPT_MD5 = 0;
  public static final int CRYPT_BLOWFISH = 0;

  public static final int CHAR_MAX = 1;

  public static final int LC_CTYPE = 1;
  public static final int LC_NUMERIC = 2;
  public static final int LC_TIME = 3;
  public static final int LC_COLLATE = 4;
  public static final int LC_MONETARY = 5;
  public static final int LC_ALL = 6;
  public static final int LC_MESSAGES = 7;

  public static final int STR_PAD_LEFT = 1;
  public static final int STR_PAD_RIGHT = 0;
  public static final int STR_PAD_BOTH = 2;

  private static final DecimalFormatSymbols DEFAULT_DECIMAL_FORMAT_SYMBOLS ;

  /**
   * Escapes a string using C syntax.
   *
   * @see #stripcslashes
   *
   * @param source the source string to convert
   * @param characters the set of characters to convert
   * @return the escaped string
   */
  public static String addcslashes(String source, String characters)
  {
    boolean []bitmap = parseCharsetBitmap(characters);

    int length = source.length();

    StringBuilder sb = new StringBuilder(length * 5 / 4);

    for (int i = 0; i < length; i++) {
      char ch = source.charAt(i);

      if (ch >= 256 || ! bitmap[ch]) {
        sb.append(ch);
        continue;
      }

      switch (ch) {
      case 0x07:
        sb.append("\\a");
        break;
      case '\b':
        sb.append("\\b");
        break;
      case '\t':
        sb.append("\\t");
        break;
      case '\n':
        sb.append("\\n");
        break;
      case 0xb:
        sb.append("\\v");
        break;
      case '\f':
        sb.append("\\f");
        break;
      case '\r':
        sb.append("\\r");
        break;
      default:
        if (ch < 0x20 || ch >= 0x7f) {
          // save as octal
          sb.append("\\");
          sb.append((char) ('0' + ((ch >> 6) & 7)));
          sb.append((char) ('0' + ((ch >> 3) & 7)));
          sb.append((char) ('0' + ((ch) & 7)));
          break;
        }
        else {
          sb.append("\\");
          sb.append(ch);
          break;
        }
      }
    }

    return sb.toString();
  }

  /**
   * Parses the cslashes bitmap returning an actual bitmap.
   *
   * @param charset the bitmap string
   * @return  the actual bitmap
   */
  private static boolean []parseCharsetBitmap(String charset)
  {
    boolean []bitmap = new boolean[256];

    int length = charset.length();
    for (int i = 0; i < length; i++) {
      char ch = charset.charAt(i);

      // XXX: the bitmap eventual might need to deal with unicode
      if (ch >= 256)
        continue;

      bitmap[ch] = true;

      if (length <= i + 3)
        continue;

      if (charset.charAt(i + 1) != '.' || charset.charAt(i + 2) != '.')
        continue;

      char last = charset.charAt(i + 3);

      if (last < ch) {
        // XXX: exception type
        throw new RuntimeException(L.l("Invalid range."));
      }

      i += 3;
      for (; ch <= last; ch++) {
        bitmap[ch] = true;
      }

      // XXX: handling of '@'?
    }

    return bitmap;
  }

  /**
   * Escapes a string for db characters.
   *
   * @param source the source string to convert
   * @return the escaped string
   */
  public static String addslashes(String source)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();
    int length = source.length();
    for (int i = 0; i < length; i++) {
      char ch = source.charAt(i);

      switch (ch) {
      case 0x0:
        sb.append("\\0");
        break;
      case '\'':
        sb.append("\\'");
        break;
      case '\"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    return sb.toString();
  }

  /**
   * Converts a binary value to a hex value.
   */
  public static String bin2hex(String strValue)
    throws Throwable
  {
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < strValue.length(); i++) {
      char ch = strValue.charAt(i);

      int d = (ch >> 4) & 0xf;

      if (d < 10)
        sb.append((char) (d + '0'));
      else
        sb.append((char) (d + 'a' - 10));

      d = (ch) & 0xf;

      if (d < 10)
        sb.append((char) (d + '0'));
      else
        sb.append((char) (d + 'a' - 10));
    }

    return sb.toString();
  }

  /**
   * Alias of rtrim.  Removes trailing whitespace.
   *
   * @param env the quercus environment
   * @param str the string to be trimmed
   * @param charset optional set of characters to trim
   * @return the trimmed string
   */
  public static String chop(Env env, String str,
                            @Optional String charset)
    throws Throwable
  {
    return rtrim(env, str, charset);
  }

  /**
   * converts a number to its character equivalent
   *
   * @param value the integer value
   *
   * @return the string equivalent
   */
  public static String chr(long value)
  {
    return String.valueOf((char) value);
  }

  /**
   * Splits a string into chunks
   *
   * @param body the body string
   * @param chunkLen the optional chunk length, defaults to 76
   * @param end the optional end value, defaults to "\r\n"
   */
  public static String chunk_split(String body,
                                   @Optional("76") int chunkLen,
                                   @Optional("\"\\r\\n\"") String end)
    throws Throwable
  {
    if (chunkLen < 1) // XXX: real exn
      throw new IllegalArgumentException(L.l("bad value {0}", chunkLen));

    StringBuilder sb = new StringBuilder();

    int i = 0;

    for (; i + chunkLen <= body.length(); i += chunkLen) {
      sb.append(body.substring(i, i + chunkLen));
      sb.append(end);
    }

    if (i < body.length()) {
      sb.append(body.substring(i));
      sb.append(end);
    }

    return sb.toString();
  }

  /**
   * Converts from one cyrillic set to another.
   *
   * This implementation does nothing, because quercus stores strings as
   * 16 bit unicode.
   */
  public static Value convert_cyr_string(Value str,
                                         Value from,
                                         Value to)
  {
    return str;
  }

  public static Value convert_uudecode(Env env, String source)
    throws java.io.IOException
  {
    if (source == null || source.length() == 0)
      return BooleanValue.FALSE;

    ByteToChar byteToChar = env.getByteToChar();

    int length = source.length();

    int i = 0;
    while (i < length) {
      int ch1 = source.charAt(i++);

      if (ch1 == 0x60 || ch1 == 0x20)
        break;
      else if (ch1 < 0x20 || 0x5f < ch1)
        continue;

      int sublen = ch1 - 0x20;

      while (sublen > 0) {
        int code;

        code = ((source.charAt(i++) - 0x20) & 0x3f) << 18;
        code += ((source.charAt(i++) - 0x20) & 0x3f) << 12;
        code += ((source.charAt(i++) - 0x20) & 0x3f) << 6;
        code += ((source.charAt(i++) - 0x20) & 0x3f);

        byteToChar.addByte(code >> 16);

        if (sublen > 1)
          byteToChar.addByte(code >> 8);

        if (sublen > 2)
          byteToChar.addByte(code);

        sublen -= 3;
      }
    }

    return new StringValue(byteToChar.getConvertedString());
  }

  /**
   * uuencode a string.
   */
  public static Value convert_uuencode(String source)
  {
    if (source == null || source.length() == 0)
      return BooleanValue.FALSE;

    StringBuilder result = new StringBuilder();

    int i = 0;
    int length = source.length();
    while (i < length) {
      int sublen = length - i;

      if (45 < sublen)
        sublen = 45;

      result.append((char) (sublen + 0x20));

      int end = i + sublen;

      while (i < end) {
        int code = source.charAt(i++) << 16;

        if (i < length)
          code += source.charAt(i++) << 8;

        if (i < length)
          code += source.charAt(i++);

        result.append(toUUChar(((code >> 18) & 0x3f)));
        result.append(toUUChar(((code >> 12) & 0x3f)));
        result.append(toUUChar(((code >> 6) & 0x3f)));
        result.append(toUUChar(((code) & 0x3f)));
      }

      result.append('\n');
    }

    result.append((char) 0x60);
    result.append('\n');

    return new StringValue(result.toString());
  }
  /**
   * Returns an array of information about the characters.
   */
  public static Value count_chars(String data,
                                  @Optional("0") int mode)
  {
    if (data == null)
      data = "";

    int []count = new int[256];

    int length = data.length();

    for (int i = 0; i < length; i++) {
      count[data.charAt(i) & 0xff] += 1;
    }

    switch (mode) {
    case 0:
      {
        ArrayValue result = new ArrayValueImpl();

        for (int i = 0; i < count.length; i++) {
          result.put(new LongValue(i), new LongValue(count[i]));
        }

        return result;
      }

    case 1:
      {
        ArrayValue result = new ArrayValueImpl();

        for (int i = 0; i < count.length; i++) {
          if (count[i] > 0)
            result.put(new LongValue(i), new LongValue(count[i]));
        }

        return result;
      }

    case 2:
      {
        ArrayValue result = new ArrayValueImpl();

        for (int i = 0; i < count.length; i++) {
          if (count[i] == 0)
            result.put(new LongValue(i), new LongValue(count[i]));
        }

        return result;
      }

    case 3:
      {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count.length; i++) {
          if (count[i] > 0)
            sb.append((char) i);
        }

        return new StringValue(sb.toString());
      }

    case 4:
      {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count.length; i++) {
          if (count[i] == 0)
            sb.append((char) i);
        }

        return new StringValue(sb.toString());
      }

    default:
      return BooleanValue.FALSE;
    }
  }

  /**
   * Calculates the crc32 value for a string
   *
   * @param str the string value
   *
   * @return the crc32 hash
   */
  public static int crc32(String str)
    throws Throwable
  {
    CRC32 crc = new CRC32();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      crc.update((byte) ch);
    }

    return (int) crc.getValue();
  }

  public static String crypt(String string, @Optional String salt)
    throws Exception
  {
    if (salt == null || salt.equals("")) {
      salt = ("" + Crypt.resultToChar(RandomUtil.nextInt(0x40)) +
              Crypt.resultToChar(RandomUtil.nextInt(0x40)));
    }
    return Crypt.crypt(string, salt);
  }

  /**
   * Explodes a string into an array
   *
   * @param separator the separator string
   * @param string the string to be exploded
   * @param limit the max number of elements
   * @return an array of exploded values
   */
  public static Value explode(String separator,
                              String string,
                              @Optional("0x7fffffff") long limit)
    throws Throwable
  {
    if (separator.equals(""))
      return BooleanValue.FALSE;

    ArrayValue array = new ArrayValueImpl();

    int head = 0;
    int tail;

    int i = 0;
    while ((tail = string.indexOf(separator, head)) >= 0) {
      if (limit <= i + 1)
        break;

      LongValue key = new LongValue(i++);

      StringValue chunk = new StringValue(string.substring(head, tail));

      array.put(key, chunk);

      head = tail + separator.length();
    }

    LongValue key = new LongValue(i + 1);

    StringValue chunk = new StringValue(string.substring(head));

    array.put(key, chunk);

    return array;
  }

  /**
   * Use printf style formatting to write a string to a file.
   * @param fd the file to write to
   * @param format the format string
   * @param args the valujes to apply to the format string
   */
  public static Value fprintf(Env env,
                              Value fd,
                              String format,
                              Value []args)
    throws Throwable
  {
    String value = sprintf(format, args);

    return QuercusFileModule.fwrite(env, fd, value, -1);
  }

  /**
   * implodes an array into a string
   *
   * @param glueV the separator string
   * @param piecesV the array to be imploded
   *
   * @return a string of imploded values
   */
  public static Value implode(Env env,
                              Value glueV,
                              Value piecesV)
    throws Throwable
  {
    String glue;
    ArrayValue pieces;

    if (piecesV instanceof ArrayValue) {
      pieces = (ArrayValue) piecesV;
      glue = glueV.toString();
    }
    else if (glueV instanceof ArrayValue) {
      pieces = (ArrayValue) glueV;
      glue = piecesV.toString();
    }
    else {
      env.error(L.l("neither argument to implode is an array: {0}, {1}",
		    glueV.getClass().getName(), piecesV.getClass().getName()));

      return BooleanValue.FALSE;
    }

    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;

    for (Map.Entry<Value,Value> entry : pieces.entrySet()) {
      if (! isFirst)
        sb.append(glue);
      isFirst = false;

      sb.append(entry.getValue().toString());
    }

    return new StringValue(sb.toString());
  }

  /**
   * implodes an array into a string
   *
   * @param glueV the separator string
   * @param piecesV the array to be imploded
   *
   * @return a string of imploded values
   */
  public static Value join(Env env,
                           Value glueV,
                           Value piecesV)
    throws Throwable
  {
    return implode(env, glueV, piecesV);
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static String md5(String source,
                           @Optional boolean rawOutput)
    throws Throwable
  {
    MessageDigest md = MessageDigest.getInstance("MD5");

    // XXX: iso-8859-1

    for (int i = 0; i < source.length(); i++) {
      char ch = source.charAt(i);

      md.update((byte) ch);
    }

    byte []digest = md.digest();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < digest.length; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = (digest[i] & 0xf);

      sb.append(toHexChar(d1));
      sb.append(toHexChar(d2));
    }

    return sb.toString();
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static Value md5_file(Path source,
                               @Optional boolean rawOutput)
    throws Throwable
  {
    MessageDigest md = MessageDigest.getInstance("MD5");
    InputStream is = null;

    try {
      is = source.openRead();
      int d;

      while ((d = is.read()) >= 0) {
        md.update((byte) d);
      }

      return digestToString(md.digest());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException e) {
      }
    }
  }

  private static Value digestToString(byte []digest)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < digest.length; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = (digest[i] & 0xf);

      sb.append(toHexChar(d1));
      sb.append(toHexChar(d2));
    }

    return new StringValue(sb.toString());
  }

  /**
   * Returns a formatted money value.
   *
   * @param format the format
   * @param value the value
   *
   * @return a string of formatted values
   */
  public static String money_format(Env env, String format, double value)
  {
    Locale monetaryLocale = env.getLocaleInfo().getMonetary();

    return NumberFormat.getCurrencyInstance(monetaryLocale).format(value);
  }

  /**
   * Returns the metaphone of a string.
   * This implentation produces identical results to the php version, which does contain some bugs.
   */
  public static String metaphone(String string)
  {
    int length = string.length();
    int index = 0;
    char ch = 0;

    // ignore everything up until first letter
    for (; index < length; index++) {
      ch = toUpperCase(string.charAt(index));

      if ('A' <= ch && ch <= 'Z')
        break;
    }

    if (index == length)
      return "";

    int lastIndex = length - 1;

    StringBuilder result = new StringBuilder(length);

    // special case first letter

    char nextCh
      = index < lastIndex
      ? toUpperCase(string.charAt(index + 1))
      : 0;

    switch (ch) {
      case 'A':
        if (nextCh == 'E') {
          result.append('E');
          index += 2;
        }
        else {
          result.append('A');
          index += 1;
        }

        break;

      case 'E':
      case 'I':
      case 'O':
      case 'U':
        result.append(ch);
        index += 1;
        break;

      case 'G':
      case 'K':
      case 'P':
        if (nextCh == 'N') {
          result.append('N');
          index += 2;
        }

        break;

      case 'W':
        if (nextCh == 'H' || nextCh == 'R') {
          result.append(nextCh);
          index += 2;
        }
        else {
          switch (nextCh) {
            case 'A':
            case 'E':
            case 'I':
            case 'O':
            case 'U':
              result.append('W');
              index += 2;
              break;
            default:
              break;
          }
        }

        break;

      case 'X':
        result.append('S');
        index += 1;
        break;

      default:
        break;
    }

    // the rest of the letters

    char prevCh;

    for (; index < length; index++) {

      if (index > 0)
        prevCh = toUpperCase(string.charAt(index - 1));
      else
        prevCh = 0;

      ch = toUpperCase(string.charAt(index));

      if (ch < 'A' || ch > 'Z')
        continue;

      if (ch == prevCh && ch != 'C')
        continue;

      if (index + 1 < length)
        nextCh = toUpperCase(string.charAt(index + 1));
      else
        nextCh = 0;

      char nextnextCh;

      if (index + 2 < length)
        nextnextCh = toUpperCase(string.charAt(index + 2));
      else
        nextnextCh = 0;


      switch (ch) {
        case 'B':
          if (prevCh != 'M')
            result.append('B');
          break;

        case 'C':
            switch (nextCh) {
              case 'E':
              case 'I':
              case 'Y':
                // makesoft
                if (nextCh == 'I' && nextnextCh == 'A') {
                  result.append('X');
                }
                else if (prevCh == 'S') {
                }
                else {
                  result.append('S');
                }
                break;
              default:
                if (nextCh == 'H') {
                  result.append('X');
                  index++;
                }
                else {
                  result.append('K');
                }
                break;
            }

          break;

        case 'D':
          if (nextCh == 'G') {
            switch (nextnextCh) {
              case 'E':
              case 'I':
              case 'Y':
                // makesoft
                result.append('J');
                index++;
                break;
              default:
                result.append('T');
                break;
            }
          }
          else
            result.append('T');

          break;

        case 'G':
          if (nextCh == 'H') {
            boolean isSilent = false;

            if (index - 3 >= 0) {
              char prev3Ch = toUpperCase(string.charAt(index - 3));
              switch (prev3Ch) {
                // noghtof
                case 'B':
                case 'D':
                case 'H':
                  isSilent = true;
                  break;
                default:
                  break;
              }
            }

            if (!isSilent) {
              if (index - 4 >= 0) {
                char prev4Ch = toUpperCase(string.charAt(index - 4));

                isSilent = (prev4Ch == 'H');
              }
            }

            if (!isSilent) {
              result.append('F');
              index++;
            }
          }
          else if (nextCh == 'N') {
            char nextnextnextCh;

            if (index + 3 < length)
              nextnextnextCh = toUpperCase(string.charAt(index + 3));
            else
              nextnextnextCh = 0;

            if (nextnextCh < 'A' || nextnextCh > 'Z') {
            }
            else if (nextnextCh == 'E' && nextnextnextCh == 'D') {
            }
            else
              result.append('K');
          }
          else if (prevCh == 'G') {
            result.append('K');
          }
          else {
            switch (nextCh) {
              case 'E':
              case 'I':
              case 'Y':
                // makesoft
                result.append('J');
                break;
              default:
                result.append('K');
                break;
            }
          }

          break;

        case 'H':
        case 'W':
        case 'Y':
          switch (nextCh) {
            case 'A':
            case 'E':
            case 'I':
            case 'O':
            case 'U':
              // followed by a vowel

              if (ch == 'H') {
                switch (prevCh) {
                  case 'C':
                  case 'G':
                  case 'P':
                  case 'S':
                  case 'T':
                    // affecth
                    break;
                  default:
                    result.append('H');
                    break;
                }
              }
              else
                result.append(ch);

              break;
            default:
              // not followed by a vowel
              break;
          }

          break;

        case 'K':
          if (prevCh != 'C')
            result.append('K');

          break;

        case 'P':
          if (nextCh == 'H')
            result.append('F');
          else
            result.append('P');

          break;

        case 'Q':
          result.append('K');
          break;

        case 'S':
          if (nextCh == 'I' && (nextnextCh == 'O' || nextnextCh == 'A')) {
            result.append('X');
          }
          else if (nextCh == 'H') {
            result.append('X');
            index++;
          }
          else
            result.append('S');

          break;

        case 'T':
          if (nextCh == 'I' && (nextnextCh == 'O' || nextnextCh == 'A')) {
            result.append('X');
          }
          else if (nextCh == 'H') {
            result.append('0');
            index++;
          }
          else
            result.append('T');

          break;

        case 'V':
          result.append('F');

          break;

        case 'X':
          result.append('K');
          result.append('S');
          break;

        case 'Z':
          result.append('S');
          break;

        case 'F':
        case 'J':
        case 'L':
        case 'M':
        case 'N':
        case 'R':
          result.append(ch);
          break;

        default:
          break;
      }
    }

    return result.toString();
  }

  /**
   * Returns a formatted number.
   *
   * @param value the value
   * @param decimals the number of decimals
   * @param pointValue the decimal point string
   * @param groupValue the thousands separator
   *
   * @return a string of the formatted number
   */
  public static String number_format(Env env,
                                     double value,
                                     @Optional int decimals,
                                     @Optional Value pointValue,
                                     @Optional Value groupValue)
  {
    boolean isGroupDefault = (groupValue instanceof DefaultValue);
    boolean isPointDefault = (pointValue instanceof DefaultValue);

    if  (!isPointDefault && isGroupDefault) {
      env.warning(L.l("wrong parameter count"));
      return null;
    }

    String pattern;

    char point = '.';

    if (!pointValue.isNull()) {
      String pointString = pointValue.toString();

      point =  (pointString.length() == 0) ? 0 : pointString.charAt(0);
    }

    char group = ',';

    if (!groupValue.isNull()) {
      String groupString = groupValue.toString();

      group = (groupString.length() == 0) ? 0 : groupString.charAt(0);
    }

    if (decimals > 0) {
      StringBuilder patternBuilder = new StringBuilder(6 + decimals);

      patternBuilder.append(group == 0 ? "###0." : "#,##0.");

      for (int i = 0; i < decimals; i++)
        patternBuilder.append('0');

      pattern = patternBuilder.toString();
    }
    else {
      pattern = group == 0 ? "###0" : "#,##0";
    }

    DecimalFormatSymbols decimalFormatSymbols;

    if (point == '.' && group == ',')
      decimalFormatSymbols = DEFAULT_DECIMAL_FORMAT_SYMBOLS;
    else {
      decimalFormatSymbols = new DecimalFormatSymbols();
      decimalFormatSymbols.setDecimalSeparator(point);
      decimalFormatSymbols.setGroupingSeparator(group);
      decimalFormatSymbols.setZeroDigit('0');
    }

    DecimalFormat format = new DecimalFormat(pattern, decimalFormatSymbols);

    String result = format.format(value);

    if (point == 0 && decimals > 0) {
      // no way to get DecimalFormat to output nothing for the point,
      // so remove it here
      int i = result.lastIndexOf(point);

      return result.substring(0, i) + result.substring(i + 1, result.length());
    }
    else
      return result;
  }

 /**
   * Converts the first character to an integer.
   *
   * @param string the string to be converted
   *
   * @return the integer value
   */
  public static long ord(String string)
    throws Throwable
  {
    if (string.length() == 0)
      return 0;
    else
      return string.charAt(0);
  }

  /**
   * Parses the string as a query string.
   *
   * @param env the calling environment
   * @param str the query string
   * @param array the optional result array
   */
  @UsesSymbolTable
  public static Value parse_str(Env env, String str,
                                @Optional ArrayValue array)
    throws IOException
  {
    ByteToChar byteToChar = env.getByteToChar();
    int len = str.length();

    ArrayValue result = array;

    if (result == null)
      result = new ArrayValueImpl();

    for (int i = 0; i < len; i++) {
      int ch = 0;

      byteToChar.clear();

      for (; i < len && (ch = str.charAt(i)) != '='; i++) {
        i = addQueryChar(byteToChar, str, len, i, ch);
      }

      String key = byteToChar.getConvertedString();
      byteToChar.clear();

      String value;
      if (ch == '=') {
        for (i++; i < len && (ch = str.charAt(i)) != '&'; i++) {
          i = addQueryChar(byteToChar, str, len, i, ch);
        }

        value = byteToChar.getConvertedString();
      }
      else
        value = "";

      Post.addFormValue(result, key, new String[] { value } );
    }

    if (array == null) {
      QuercusArrayModule.extract(env, result,
                                 QuercusArrayModule.EXTR_OVERWRITE,
                                 null);
    }

    return NullValue.NULL;
  }

  private static int addQueryChar(ByteToChar byteToChar, String str, int len,
                                  int i, int ch)
    throws IOException
  {
    switch (ch) {
    case '+':
      byteToChar.addChar(' ');
      return i;

    case '%':
      if (i + 2 < len) {
        int d1 = hexToDigit(str.charAt(i + 1));
        int d2 = hexToDigit(str.charAt(i + 2));

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
    int p;

    Value value = new StringValue(valueStr);

    if ((p = key.indexOf('[')) > 0 && key.endsWith("]")) {
      String index = key.substring(p + 1, key.length() - 1);
      key = key.substring(0, p);

      Value keyValue = new StringValue(key);

      Value part;

      if (array != null)
        part = array.get(keyValue);
      else
        part = env.getVar(key);

      if (! part.isArray())
        part = new ArrayValueImpl();

      if (index.equals(""))
        part.put(value);
      else
        part.put(new StringValue(index), value);

      if (array != null)
        array.put(keyValue, part);
      else
        env.setVar(key, part);
    }
    else {
      if (array != null)
        array.put(new StringValue(key), value);
      else
        env.setVar(key, value);
    }
  }

  /**
   * Prints the string.
   *
   * @param env the quercus environment
   * @param value the string to print
   */
  public static long print(Env env, Value value)
    throws Throwable
  {
    env.getOut().print(value.toString(env));

    return 1;
  }

  /**
   * Escapes meta characters.
   *
   * @param string the string to be quoted
   *
   * @return the quoted
   */
  public static Value quotemeta(String string)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();

    int len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '.': case '\\': case '+': case '*': case '?':
      case '[': case '^': case ']': case '(': case ')': case '$':
        sb.append("\\");
        sb.append(ch);
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    return new StringValue(sb.toString());
  }

  private static final boolean[]TRIM_WHITESPACE = new boolean[256];

  static {
    TRIM_WHITESPACE['\0'] = true;
    TRIM_WHITESPACE['\b'] = true;
    TRIM_WHITESPACE[' '] = true;
    TRIM_WHITESPACE['\t'] = true;
    TRIM_WHITESPACE['\r'] = true;
    TRIM_WHITESPACE['\n'] = true;
  }

  /**
   * Removes leading whitespace.
   *
   * @param string the string to be trimmed
   * @param characters optional set of characters to trim
   * @return the trimmed string
   */
  public static String ltrim(Env env,
                             String string,
                             @Optional String characters)
    throws Throwable
  {
    boolean []trim;

    if (characters.equals(""))
      trim = TRIM_WHITESPACE;
    else
      trim = parseCharsetBitmap(characters);

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      if (ch >= 256 || ! trim[ch]) {
        if (i == 0)
          return string;
        else
          return string.substring(i);
      }
    }

    return "";
  }

  /**
   * Removes trailing whitespace.
   *
   * @param env the quercus environment
   * @param string the string to be trimmed
   * @param characters optional set of characters to trim
   * @return the trimmed string
   */
  public static String rtrim(Env env,
                             String string,
                             @Optional String characters)
    throws Throwable
  {
    boolean []trim;

    if (characters.equals(""))
      trim = TRIM_WHITESPACE;
    else
      trim = parseCharsetBitmap(characters);

    for (int i = string.length() - 1; i >= 0; i--) {
      char ch = string.charAt(i);

      if (ch >= 256 || ! trim[ch]) {
        if (i == string.length())
          return string;
        else
          return string.substring(0, i + 1);
      }
    }

    return "";
  }

  /**
   * Sets locale configuration.
   */
  public static Value setlocale(Env env,
                                int category,
                                Value localeArg,
                                Value []fallback)
  {
    LocaleInfo localeInfo = env.getLocaleInfo();

    if (localeArg instanceof ArrayValue) {
      for (Value value : ((ArrayValue) localeArg).values()) {
        Locale locale = setLocale(localeInfo, category, value.toString());

        if (locale != null)
          return new StringValue(locale.toString());
      }
    }
    else {
      Locale locale = setLocale(localeInfo, category, localeArg.toString());

      if (locale != null)
        return new StringValue(locale.toString());
    }

    for (int i = 0; i < fallback.length; i++) {
      Locale locale = setLocale(localeInfo, category, fallback[i].toString());

      if (locale != null)
        return new StringValue(locale.toString());
    }

    return BooleanValue.FALSE;
  }

  /**
   * Sets locale configuration.
   */
  private static Locale setLocale(LocaleInfo localeInfo,
                                  int category,
                                  String localeName)
  {
    String language;
    String country;
    String variant;

    int p = localeName.indexOf('_');
    int p1 = localeName.indexOf('-');

    if (p1 > 0 && (p1 < p || p < 0))
      p = p1;

    Locale locale;

    if (p > 0) {
      language = localeName.substring(0, p);

      int q = localeName.indexOf('-', p + 1);
      int q1 = localeName.indexOf('.', p + 1);
      // XXX: '.' should be charset?

      if (q1 > 0 && (q1 < q || q < 0))
        q = q1;

      q1 = localeName.indexOf('@', p + 1);
      // XXX: '@' is ??

      if (q1 > 0 && (q1 < q || q < 0))
        q = q1;

      q1 = localeName.indexOf('_', p + 1);

      if (q1 > 0 && (q1 < q || q < 0))
        q = q1;

      if (q > 0) {
        country = localeName.substring(p + 1, q);
        variant = localeName.substring(q + 1);

        locale = new Locale(language, country, variant);
      }
      else {
        country = localeName.substring(p + 1);

        locale = new Locale(language, country);
      }
    }
    else
      locale = new Locale(localeName);

    if (! isValidLocale(locale))
      return null;

    switch (category) {
    case LC_ALL:
      localeInfo.setAll(locale);
      return localeInfo.getMessages();
    case LC_COLLATE:
      localeInfo.setCollate(locale);
      return localeInfo.getCollate();
    case LC_CTYPE:
      localeInfo.setCtype(locale);
      return localeInfo.getCtype();
    case LC_MONETARY:
      localeInfo.setMonetary(locale);
      return localeInfo.getMonetary();
    case LC_NUMERIC:
      localeInfo.setNumeric(locale);
      return localeInfo.getNumeric();
    case LC_TIME:
      localeInfo.setTime(locale);
      return localeInfo.getTime();
    case LC_MESSAGES:
      localeInfo.setMessages(locale);
      return localeInfo.getMessages();
    default:
      return null;
    }
  }

  /**
   * Returns true if the locale is supported.
   */
  private static boolean isValidLocale(Locale locale)
  {
    Locale []validLocales = Locale.getAvailableLocales();

    for (int i = 0; i < validLocales.length; i++) {
      if (validLocales[i].equals(locale)) {
        return true;
      }
    }

    return false;
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static String sha1(String source,
                            @Optional boolean rawOutput)
    throws Throwable
  {
    MessageDigest md = MessageDigest.getInstance("SHA1");

    // XXX: iso-8859-1

    for (int i = 0; i < source.length(); i++) {
      char ch = source.charAt(i);

      md.update((byte) ch);
    }

    byte []digest = md.digest();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < digest.length; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = (digest[i] & 0xf);

      sb.append(toHexChar(d1));
      sb.append(toHexChar(d2));
    }

    return sb.toString();
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static Value sha1_file(Path source,
                                @Optional boolean rawOutput)
    throws Throwable
  {
    MessageDigest md = MessageDigest.getInstance("SHA1");
    InputStream is = null;

    try {
      is = source.openRead();
      int d;

      while ((d = is.read()) >= 0) {
        md.update((byte) d);
      }

      return digestToString(md.digest());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * scans a string
   *
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static Value sscanf(String string,
                             String format,
                             @Optional Value []args)
  {
    // quercus/113-

    int fmtLen = format.length();
    int strlen = string.length();

    int sIndex = 0;
    int fIndex = 0;

    ArrayValue array = new ArrayValueImpl();

    while (fIndex < fmtLen && sIndex < strlen) {
      char ch = format.charAt(fIndex++);

      if (isWhitespace(ch)) {
        for (;
             (fIndex < fmtLen &&
              isWhitespace(ch = format.charAt(fIndex)));
             fIndex++) {
        }

        ch = string.charAt(sIndex);
        if (! isWhitespace(ch)) {
          return array; // XXX: return false?
        }

        for (sIndex++;
             sIndex < strlen && isWhitespace(string.charAt(sIndex));
             sIndex++) {
        }
      }
      else if (ch == '%') {
        int maxLen = -1;
        boolean suppressAssignment = false;

        loop:
        while (fIndex < fmtLen) {
          ch = format.charAt(fIndex++);

          switch (ch) {
          case '%':
            if (string.charAt(sIndex) != '%')
              return array;
            else
              break loop;

          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
            if (maxLen < 0)
              maxLen = 0;

            maxLen = 10 * maxLen + ch - '0';
            break;

          case 's':
            sIndex = sscanfString(string, sIndex, maxLen, array);
            break loop;

          default:
            log.fine(L.l("'{0}' is a bad sscanf string.", format));
            return array;
          }
        }
      }
      else if (ch == string.charAt(sIndex)) {
        sIndex++;
      }
      else
        return array;
    }

    return array;
  }

  /**
   * Scans a string with a given length.
   */
  private static int sscanfString(String string, int sIndex, int maxLen,
                                  ArrayValue array)
  {
    // quercus/1131
    int strlen = string.length();

    if (maxLen < 0)
      maxLen = Integer.MAX_VALUE;

    StringBuilder sb = new StringBuilder();

    for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
      char ch = string.charAt(sIndex);

      if (isWhitespace(ch)) {
        array.append(new StringValue(sb.toString()));
        return sIndex;
      }
      else
        sb.append(ch);
    }

    array.append(new StringValue(sb.toString()));

    return sIndex;
  }

  /**
   * print to the output with a formatter
   *
   * @param env the quercus environment
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static int printf(Env env, String format, Value []args)
    throws Throwable
  {
    String str = sprintf(format, args);

    env.getOut().print(str);

    return str.length();
  }

  private static final char[] SOUNDEX_VALUES = "01230120022455012623010202".toCharArray();

  public static Value soundex(String string)
  {
    int length = string.length();

    if (length == 0)
      return BooleanValue.FALSE;

    StringBuilder result = new StringBuilder(4);

    int count = 0;
    char lastCode = 0;


    for (int i = 0; i < length && count < 4; i++) {
      char ch = toUpperCase(string.charAt(i));

      if ('A' <= ch  && ch <= 'Z') {
        char code = SOUNDEX_VALUES[ch - 'A'];

        if (count == 0) {
          result.append(ch);
          count++;
        }
        else if (code != '0' && code != lastCode) {
          result.append(code);
          count++;
        }

        lastCode = code;
      }
    }

    for (; count < 4; count++) {
      result.append('0');
    }

    return new StringValue(result.toString());
  }

  /**
   * Print to a string with a formatter
   *
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static String sprintf(String format, Value []args)
    throws Throwable
  {
    ArrayList<PrintfSegment> segments = parsePrintfFormat(format);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < segments.size(); i++)
      segments.get(i).apply(sb, args);

    return sb.toString();
  }

  private static ArrayList<PrintfSegment> parsePrintfFormat(String format)
  {
    ArrayList<PrintfSegment> segments = new ArrayList<PrintfSegment>();

    StringBuilder sb = new StringBuilder();
    StringBuilder flags = new StringBuilder();

    int length = format.length();
    int index = 0;

    for (int i = 0; i < length; i++) {
      char ch = format.charAt(i);

      if (i + 1 < length && ch == '%') {
        // The C printf silently ignores invalid flags, so we need to
        // remove them if present.

        sb.append(ch);

        boolean isLeft = false;
        boolean isAlt = false;
        boolean isZero = false;

        flags.setLength(0);

        int j = i + 1;

        loop:
        for (; j < length; j++) {
          ch = format.charAt(j);

          switch (ch) {
          case '-':
            isLeft = true;
            break;
          case '#':
            isAlt = true;
            break;
          case '0':
            isZero = true;
            flags.append(ch);
            break;
          case '+': case ' ': case ',': case '(':
            flags.append(ch);
            break;
          default:
            break loop;
          }
        }

        int head = j;
        loop:
        for (; j < length; j++) {
          ch = format.charAt(j);

          switch (ch) {
          case '%':
            i = j - 1;
            break loop;

          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
          case '.':
            break;

          case 'b': case 'B':
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(format, head, j);
            sb.append(ch);
            i = j;
            break loop;

          case 's': case 'S':
            sb.setLength(sb.length() - 1);
            segments.add(new StringPrintfSegment(sb,
                                                 isLeft || isAlt,
                                                 isZero,
                                                 ch == 'S',
                                                 format.substring(head, j),
                                                 index++));
            sb.setLength(0);
            i = j;
            break loop;

          case 'c': case 'C':
            sb.setLength(sb.length() - 1);
            segments.add(new CharPrintfSegment(sb,
                                               isLeft || isAlt,
                                               isZero,
                                               ch == 'C',
                                               format.substring(head, j),
                                               index++));
            sb.setLength(0);
            i = j;
            break loop;

          case 'd': case 'x': case 'o': case 'X':
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(flags);
            sb.append(format, head, j);
            sb.append(ch);

            segments.add(new LongPrintfSegment(sb.toString(), index++));
            sb.setLength(0);
            i = j;
            break loop;

          case 'e': case 'E': case 'f': case 'g': case 'G':
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(flags);
            sb.append(format, head, j);
            sb.append(ch);

            segments.add(new DoublePrintfSegment(sb.toString(), index++));
            sb.setLength(0);
            i = j;
            break loop;

          default:
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(flags);
            sb.append(format, head, j);
            sb.append(ch);
            i = j;
            break loop;
          }
        }
      }
      else
        sb.append(ch);
    }

    if (sb.length() > 0)
      segments.add(new TextPrintfSegment(sb));

    return segments;
  }

  // XXX: dead code?
  private static String cleanPrintfFormat(String format)
  {
    StringBuilder sb = new StringBuilder();
    StringBuilder flags = new StringBuilder();

    int length = format.length();

    for (int i = 0; i < length; i++) {
      char ch = format.charAt(i);

      if (i + 1 < length && ch == '%') {
        // The C printf silently ignores invalid flags, so we need to
        // remove them if present.

        sb.append(ch);

        boolean isLeft = false;
        boolean isAlt = false;

        flags.setLength(0);

        int j = i + 1;

        loop:
        for (; j < length; j++) {
          ch = format.charAt(j);

          switch (ch) {
          case '-':
            isLeft = true;
            break;
          case '#':
            isAlt = true;
            break;
          case '+': case ' ': case '0': case ',': case '(':
            flags.append(ch);
            break;
          default:
            break loop;
          }
        }

        int head = j;
        loop:
        for (; j < length; j++) {
          ch = format.charAt(j);

          switch (ch) {
          case '%':
            sb.append(ch);
            break loop;

          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
          case '.':
            break;

          case 'b': case 'B': case 's': case 'S': case 'c':
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(format, head, j);
            sb.append(ch);
            i = j;
            break loop;

          default:
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(flags);
            sb.append(format, head, j);
            sb.append(ch);
            i = j;
            break loop;
          }
        }
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  // XXX: dead code?
  private static void parsePrintfFormat(String format,
                                        Value []args,
                                        Object []values)
  {
    int k = 0;
    int strlen = format.length();

    for (int i = 0; i < strlen; i++) {
      char ch = format.charAt(i);

      if (ch == '%') {
        loop:
        for (i++; i < strlen; i++) {
          ch = format.charAt(i);

          switch (ch) {
          case '%':
            break loop;
          case 's': case 'c':
            values[k] = args[k].toString();
            k++;
            break loop;
          case 'd': case 'x': case 'o': case 'X':
            values[k] = args[k].toLong();
            k++;
            break loop;
          case 'e': case 'f': case 'g':
            values[k] = args[k].toDouble();
            k++;
            break loop;
          default:
            break;
          }
        }
      }
    }
  }

  /**
   * Removes leading and trailing whitespace.
   *
   * @param string the string to be trimmed
   * @param characters optional set of characters to trim
   * @return the trimmed string
   */
  public static Value trim(String string, @Optional String characters)
    throws Throwable
  {
    boolean []trim;

    if (characters == null || characters.equals(""))
      trim = TRIM_WHITESPACE;
    else
      trim = parseCharsetBitmap(characters.toString());

    int len = string.length();

    int head = 0;
    for (; head < len; head++) {
      char ch = string.charAt(head);

      if (ch >= 256 || ! trim[ch]) {
        break;
      }
    }

    int tail = len - 1;
    for (; tail >= 0; tail--) {
      char ch = string.charAt(tail);

      if (ch >= 256 || ! trim[ch]) {
        break;
      }
    }

    if (tail < head)
      return StringValue.EMPTY;
    else {
      return new StringValue(string.substring(head, tail + 1));
    }
  }

  // XXX: str_ireplace

  /**
   * Pads strings
   *
   * @param string string
   * @param length length
   * @param pad padding string
   * @param type padding type
   */
  public static String str_pad(String string,
                               int length,
                               @Optional("' '") String pad,
                               @Optional("STR_PAD_RIGHT") int type)
    throws Throwable
  {
    int strLen = string.length();
    int padLen = length - strLen;

    if (padLen <= 0)
      return string;

    if (pad.length() == 0)
      pad = " ";

    int leftPad = 0;
    int rightPad = 0;

    switch (type) {
    case STR_PAD_LEFT:
      leftPad = padLen;
      break;
    case STR_PAD_RIGHT:
    default:
      rightPad = padLen;
      break;
    case STR_PAD_BOTH:
      leftPad = padLen / 2;
      rightPad = padLen - leftPad;
      break;
    }

    StringBuilder sb = new StringBuilder();

    int padStringLen = pad.length();

    for (int i = 0; i < leftPad; i++)
      sb.append(pad.charAt(i % padStringLen));

    sb.append(string);

    for (int i = 0; i < rightPad; i++)
      sb.append(pad.charAt(i % padStringLen));

    return sb.toString();
  }

  /**
   * repeats a string
   *
   * @param string string to repeat
   * @param count number of times to repeat
   */
  public static Value str_repeat(String string, int count)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < count; i++)
      sb.append(string);

    return new StringValue(sb.toString());
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param count return value
   */
  public static Value str_replace(Value search,
                                  Value replace,
                                  Value subject,
                                  @Reference @Optional Value count)
    throws Throwable
  {
    count.set(LongValue.ZERO);

    if (subject.isNull())
      return StringValue.EMPTY;

    if (search.isNull())
      return subject;

    if (subject instanceof ArrayValue) {
      ArrayValue subjectArray = (ArrayValue) subject;
      ArrayValue resultArray = new ArrayValueImpl();

      for (Value value : subjectArray.values()) {
        Value result = str_replace_impl(search, replace,
                                        value.toString(), count);

        resultArray.append(result);
      }

      return resultArray;
    }
    else {
      String subjectString = subject.toString();

      if (subjectString.length() == 0)
        return StringValue.EMPTY;

      return str_replace_impl(search, replace, subjectString, count);
    }
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param count return value
   */
  private static Value str_replace_impl(Value search,
                                        Value replace,
                                        String subject,
                                        Value count)
    throws Throwable
  {
    if (! search.isArray()) {
      String searchString = search.toString();

      if (searchString.length() == 0)
        return new StringValue(subject);

      subject = str_replace_impl(searchString,
                                 replace.toString(),
                                 subject,
                                 count);
    }
    else if (replace instanceof ArrayValue) {
      ArrayValue searchArray = (ArrayValue) search;
      ArrayValue replaceArray = (ArrayValue) replace;

      Iterator<Value> searchIter = searchArray.values().iterator();
      Iterator<Value> replaceIter = replaceArray.values().iterator();

      while (searchIter.hasNext()) {
        Value searchItem = searchIter.next();
        Value replaceItem = replaceIter.next();

        if (replaceItem == null)
          replaceItem = NullValue.NULL;

        subject = str_replace_impl(searchItem.toString(),
                                   replaceItem.toString(),
                                   subject,
                                   count);
      }
    }
    else {
      ArrayValue searchArray = (ArrayValue) search;

      Iterator<Value> searchIter = searchArray.values().iterator();

      while (searchIter.hasNext()) {
        Value searchItem = searchIter.next();

        subject = str_replace_impl(searchItem.toString(),
                                   replace.toString(),
                                   subject,
                                   count);
      }
    }

    return new StringValue(subject);
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param countV return value
   */
  private static String str_replace_impl(String search,
                                         String replace,
                                         String subject,
                                         Value countV)
  {
    long count = countV.toLong();

    int head = 0;
    int next;

    int searchLen = search.length();

    StringBuilder result = new StringBuilder();

    while ((next = subject.indexOf(search, head)) >= head) {
      result.append(subject, head, next);
      result.append(replace);

      if (head < next + searchLen)
        head = next + searchLen;
      else
        head += 1;

      count++;
    }

    if (count != 0) {
      countV.set(new LongValue(count));

      if (head > 0 && head < subject.length())
        result.append(subject, head, subject.length());

      return result.toString();
    }
    else
      return subject;
  }

  /**
   * rot13 conversion
   *
   * @param string string to convert
   */
  public static Value str_rot13(String string)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();

    int len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if ('a' <= ch && ch <= 'z') {
        int off = ch - 'a';

        sb.append((char) ('a' + (off + 13) % 26));
      }
      else if ('A' <= ch && ch <= 'Z') {
        int off = ch - 'A';

        sb.append((char) ('A' + (off + 13) % 26));
      }
      else {
        sb.append(ch);
      }
    }

    return new StringValue(sb.toString());
  }

  /**
   * shuffles a string
   */
  public static String str_shuffle(String string)
  {
    char []chars = string.toCharArray();

    int length = chars.length;

    for (int i = 0; i < length; i++) {
      int rand = RandomUtil.nextInt(length);

      char temp = chars[rand];
      chars[rand] = chars[i];
      chars[i] = temp;
    }

    return new String(chars);
  }

  /**
   * split into an array
   *
   * @param string string to split
   * @param chunk chunk size
   */
  public static Value str_split(String string,
                                @Optional("1") int chunk)
    throws Throwable
  {
    ArrayValue array = new ArrayValueImpl();

    int strLen = string.length();

    for (int i = 0; i < strLen; i += chunk) {
      Value value;

      if (i + chunk <= strLen) {
        value = new StringValue(string.substring(i, i + chunk));
      } else {
        value = new StringValue(string.substring(i));
      }

      array.put(new LongValue(i), value);
    }

    return array;
  }

  public static Value str_word_count(String string,
                                     @Optional int format,
                                     @Optional String additionalWordCharacters)
    throws Throwable
  {
    if (format < 0 || format > 2)
      return NullValue.NULL;

    int strlen = string.length();
    boolean isAdditionalWordCharacters = additionalWordCharacters.length() > 0;

    ArrayValueImpl resultArray = null;

    if (format > 0)
      resultArray = new ArrayValueImpl();

    boolean isBetweenWords = true;

    int wordCount = 0;

    int lastWordStart = 0;

    for (int i = 0; i <= strlen; i++) {
      boolean isWordCharacter;

      if (i < strlen) {
        int ch = string.charAt(i);

        isWordCharacter = Character.isLetter(ch)
                          || ch == '-'
                          || ch == '\''
                          || (isAdditionalWordCharacters
                              && additionalWordCharacters.indexOf(ch) > -1);
      }
      else
        isWordCharacter = false;

      if (isWordCharacter) {
        if (isBetweenWords) {
          // starting a word
          isBetweenWords = false;

          lastWordStart = i;
          wordCount++;
        }
      }
      else {
        if (!isBetweenWords) {
          // finished a word
          isBetweenWords = true;

          if (format > 0) {
            StringValue word = new StringValue(string.substring(lastWordStart, i));

            if (format == 1)
              resultArray.append(word);
            else if (format == 2)
              resultArray.put(new LongValue(lastWordStart), word);
          }
        }
      }
    }

    return resultArray == null ? new LongValue(wordCount) : resultArray;
  }

  /**
   * Case-insensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strcasecmp(String a, String b)
    throws Throwable
  {
    int aLen = a.length();
    int bLen = b.length();

    for (int i = 0; i < aLen && i < bLen; i++) {
      char chA = a.charAt(i);
      char chB = b.charAt(i);

      if (chA == chB)
        continue;

      if (Character.isUpperCase(chA))
        chA = Character.toLowerCase(chA);

      if (Character.isUpperCase(chB))
        chB = Character.toLowerCase(chB);

      if (chA == chB)
        continue;
      else if (chA < chB)
        return -1;
      else
        return 1;
    }

    if (aLen == bLen)
      return 0;
    else if (aLen < bLen)
      return -1;
    else
      return 1;
  }

  /**
   * Case-sensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strcmp(String a, String b)
    throws Throwable
  {
    int cmp = a.compareTo(b);

    if (cmp == 0)
      return 0;
    else if (cmp < 0)
      return -1;
    else
      return 1;
  }

  /**
   * Finds the index of a substring
   *
   * @param env the calling environment
   */
  public static Value strchr(Env env, String haystack, Value needle)
    throws Throwable
  {
    return strstr(env, haystack, needle);
  }

  /**
   * Locale-based comparison
   * XXX: i18n
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static Value strcoll(String a, String b)
    throws Throwable
  {
    int cmp = a.compareTo(b);

    if (cmp == 0)
      return LongValue.ZERO;
    else if (cmp < 0)
      return LongValue.MINUS_ONE;
    else
      return LongValue.ONE;
  }

  /**
   * Finds the number of initial characters in <i>string</i> that do not match
   * one of the characters in <i>characters</i>
   *
   * @param string the string to search in
   * @param characters the character set
   * @param offset the starting offset
   * @param length the length
   *
   * @return the length of the match or FALSE if the offset or length are invalid
   */
  public static Value strcspn(String string,
                              String characters,
                              @Optional("0") int offset,
                              @Optional("-2147483648") int length)
  {
    return strspnImpl(string, characters, offset, length, false);
  }


  /**
   * Removes tags from a string.
   *
   * @param string the string to remove
   * @param allowTags the allowable tags
   */
  public static String strip_tags(String string, @Optional String allowTags)
  {
    // XXX: allowTags is stubbed

    StringBuilder result = new StringBuilder();

    int len = string.length();

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (ch != '<') {
        result.append(ch);
        continue;
      }

      for (i++; i < len; i++) {
        ch = string.charAt(i);

        if (ch == '>')
          break;
      }
    }

    return result.toString();
  }

  /**
   * Returns the length of a string.
   *
   * @param value the argument value
   */
  public static long strlen(Value value)
  {
    return value.strlen();
  }

  /**
   * Case-insensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strnatcasecmp(String a, String b)
    throws Throwable
  {
    return strcasecmp(a, b);
  }

  /**
   * Case-sensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strnatcmp(String a, String b)
    throws Throwable
  {
    return strcmp(a, b);
  }

  /**
   * Case-sensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strncmp(String a, String b, int length)
    throws Throwable
  {
    if (length < a.length())
      a = a.substring(0, length);

    if (length < b.length())
      b = b.substring(0, length);

    int cmp = a.compareTo(b);

    if (cmp == 0)
      return 0;
    else if (cmp < 0)
      return -1;
    else
      return 1;
  }

  /**
   * Returns the position of a substring.
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   */
  public static Value strpos(String haystack,
                             Value needleV,
                             @Optional Value offsetV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue)
      needle = needleV.toString();
    else
      needle = String.valueOf((char) needleV.toInt());

    int offset = offsetV.toInt();

    int pos = haystack.indexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return new LongValue(pos);
  }

  /**
   * Returns the position of a substring, testing case insensitive.
   *
   * @param haystack the full argument to check
   * @param needleV the substring argument to check
   * @param offsetV optional starting position
   */
  public static Value stripos(String haystack,
                              Value needleV,
                              @Optional Value offsetV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue)
      needle = needleV.toString();
    else
      needle = String.valueOf((char) needleV.toInt());

    int offset = offsetV.toInt();

    haystack = haystack.toLowerCase();
    needle = needle.toLowerCase();

    int pos = haystack.indexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return new LongValue(pos);
  }

  /**
   * Strip out the backslashes, recognizing the escape sequences, octal,
   * and hexadecimal representations.
   *
   * @param source the string to clean
   * @see #addcslashes
   */
  public static String stripcslashes(String source)
  {
    StringBuilder result = new StringBuilder(source.length() * 5 / 4);

    int length = source.length();

    for (int i = 0; i < length; i++) {
      int ch = source.charAt(i);

      if (ch == '\\') {
        i++;

        if (i == length)
          ch = '\\';
        else {
          ch = source.charAt(i);

          switch (ch) {
          case 'a':
            ch = 0x07;
            break;
          case 'b':
            ch = '\b';
            break;
          case 't':
            ch = '\t';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'v':
            ch = 0xb;
            break;
          case 'f':
            ch = '\f';
            break;
          case 'r':
            ch = '\r';
            break;
          case 'x':
            // up to two digits for a hex number
            if (i + 1 == length)
              break;

            int digitValue = hexToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = digitValue;
            i++;

            if (i + 1 == length)
              break;

            digitValue = hexToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = ((ch << 4) | digitValue);
            i++;

            break;
          default:
            // up to three digits from 0 to 7 for an octal number
            digitValue = octToDigit((char) ch);

            if (digitValue < 0)
              break;

            ch = digitValue;

            if (i + 1 == length)
              break;

            digitValue = octToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = ((ch << 3) | digitValue);
            i++;

            if (i + 1 == length)
              break;

            digitValue = octToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = ((ch << 3) | digitValue);
            i++;
          }
        }
      } // if ch == '/'

      result.append((char) ch);
    }

    return result.toString();
  }

  /**
   * Strips out the backslashes.
   *
   * @param string the string to clean
   */
  public static String stripslashes(String string)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();
    int len = string.length();

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (ch == '\\' && i + 1 < len) {
        sb.append(string.charAt(i + 1));
        i++;
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Finds the first instance of a substring, testing case insensitively
   *
   * @param env the calling environment
   * @param haystack the string to search in
   * @param needleV the string to search for
   * @return the trailing match or FALSE
   */
  public static Value stristr(String haystack,
                              Value needleV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue) {
      needle = needleV.toString();
    }
    else {
      needle = String.valueOf((char) needleV.toLong());
    }

    String haystackLower = haystack.toLowerCase();
    String needleLower = needle.toLowerCase();

    int i = haystackLower.indexOf(needleLower);

    if (i > 0)
      return new StringValue(haystack.substring(i));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Finds the last instance of a substring
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   * @return the trailing match or FALSE
   */
  public static Value strrchr(String haystack,
                              Value needleV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue) {
      needle = needleV.toString();
    }
    else {
      needle = String.valueOf((char) needleV.toLong());
    }

    int i = haystack.lastIndexOf(needle);

    if (i > 0)
      return new StringValue(haystack.substring(i));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Reverses a string.
   *
   * @param env the calling environment
   */
  public static Value strrev(String string)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();

    for (int i = string.length() - 1; i >= 0; i--) {
      sb.append(string.charAt(i));
    }

    return new StringValue(sb.toString());
  }

  /**
   * Returns the position of a substring.
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   */
  public static Value strrpos(String haystack,
                              Value needleV,
                              @Optional Value offsetV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue)
      needle = needleV.toString();
    else
      needle = String.valueOf((char) needleV.toInt());

    int offset;

    if (offsetV instanceof DefaultValue)
      offset = haystack.length();
    else
      offset = offsetV.toInt();

    int pos = haystack.lastIndexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return new LongValue(pos);
  }

  /**
   * Returns the position of a substring, testing case-insensitive.
   *
   * @param env the calling environment
   * @param haystack the full string to test
   * @param needleV the substring string to test
   * @param offsetV the optional offset to start searching
   */
  public static Value strripos(String haystack,
                               Value needleV,
                               @Optional Value offsetV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue)
      needle = needleV.toString();
    else
      needle = String.valueOf((char) needleV.toInt());

    int offset;

    if (offsetV instanceof DefaultValue)
      offset = haystack.length();
    else
      offset = offsetV.toInt();

    haystack = haystack.toLowerCase();
    needle = needle.toLowerCase();

    int pos = haystack.lastIndexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return new LongValue(pos);
  }

  /**
   * Finds the number of initial characters in <i>string</i> that match one of
   * the characters in <i>characters</i>
   *
   * @param string the string to search in
   * @param characters the character set
   * @param offset the starting offset
   * @param length the length
   *
   * @return the length of the match or FALSE if the offset or length are invalid
   */
  public static Value strspn(String string,
                             String characters,
                             @Optional int offset,
                             @Optional("-2147483648") int length)
    throws Throwable
  {
    return strspnImpl(string, characters, offset, length, true);
  }

  private static Value strspnImpl(String string,
                                  String characters,
                                  int offset,
                                  int length,
                                  boolean isMatch)
  {
    int strlen = string.length();

    // see also strcspn which uses the same procedure for determining
    // effective offset and length
    if (offset < 0) {
      offset += strlen;

      if (offset < 0)
        offset = 0;
    }

    if (offset > strlen)
      return BooleanValue.FALSE;

    if (length ==  -2147483648)
      length = strlen;
    else if (length < 0) {
      length += (strlen - offset);

      if (length < 0)
        length = 0;
    }

    int end = offset + length;

    if (strlen < end)
      end = strlen;

    int count = 0;

    for (; offset < end; offset++) {
      char ch = string.charAt(offset);

      boolean isPresent = characters.indexOf(ch) > -1;

      if (isPresent == isMatch)
        count++;
      else
        return new LongValue(count);
    }

    return new LongValue(count);
  }

  /**
   * Finds the first instance of a needle in haystack and returns
   * the portion of haystack from the beginning of needle to the end of haystack.
   *
   * @param env the calling environment
   * @param haystack the string to search in
   * @param needleV the string to search for, or the oridinal value of a character
   * @return the trailing match or FALSE if needle is not found
   */
  public static Value strstr(Env env,
                             String haystack,
                             Value needleV)
    throws Throwable
  {
    String needle;

    if (needleV instanceof StringValue) {
      needle = needleV.toString();
    }
    else {
      needle = String.valueOf((char) needleV.toLong());
    }

    if (needle.length() == 0) {
      env.warning("empty needle");
      return BooleanValue.FALSE;
    }

    int i = haystack.indexOf(needle);

    if (i >= 0)
      return new StringValue(haystack.substring(i));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Split a string into tokens using any character in another string as a delimiter.
   *
   * The first call establishes the string to search and the characters to use as tokens,
   * the first token is returned:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   * </pre>
   *
   * Subsequent calls pass only the token characters, the next token is returned:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   *   strtok(", ")
   *     => "world"
   * </pre>
   *
   * False is returned if there are no more tokens:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   *   strtok(", ")
   *     => "world"
   *   strtok(", ")
   *     => false
   * </pre>
   *
   * Calls that pass two arguments reset the search string:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   *   strtok("goodbye, world", ", ")
   *     => "goodbye"
   *   strtok("world")
   *     => false
   *   strtok(", ")
   *     => false
   * </pre>
   */
  public static Value strtok(Env env, String string1, @Optional Value string2)
  {
    String string;
    String characters;
    int offset;

    if (string2.isNull()) {
      String savedString = (String) env.getSpecialValue("caucho.strtok_string");
      Integer savedOffset = (Integer) env.getSpecialValue("caucho.strtok_offset");

      string = savedString == null ? "" : savedString;
      offset = savedOffset == null ? 0 : savedOffset;
      characters = string1;
    }
    else {
      string = string1;
      offset = 0;
      characters = string2.toString();

      env.setSpecialValue("caucho.strtok_string", string);
    }

    int strlen = string.length();

    // skip any at beginning
    for (; offset < strlen; offset++) {
      char ch = string.charAt(offset);

      if (characters.indexOf(ch) < 0)
        break;
    }

    Value result;

    if (offset == strlen)
      result = BooleanValue.FALSE;
    else {
      int start = offset;

      offset++;

      // find end
      for (; offset < strlen; offset++) {
        char ch = string.charAt(offset);

        if (characters.indexOf(ch) > -1)
          break;
      }

      result = new StringValue(string.substring(start, offset));
    }

    env.setSpecialValue("caucho.strtok_offset", offset);

    return result;
  }

  /**
   * Converts to lower case.
   *
   * @param string the input string
   */
  public static String strtolower(String string)
  {
    return string.toLowerCase();
  }

  /**
   * Converts to upper case.
   *
   * @param string the input string
   */
  public static String strtoupper(String string)
  {
    return string.toUpperCase();
  }

  /**
   * Translates characters in a string to target values.
   *
   * @param string the source string
   * @param fromV the from characters
   * @param to the to character map
   */
  public static String strtr(Env env,
                             String string,
                             Value fromV,
                             @Optional String to)
  {
    if (fromV instanceof ArrayValue)
      return strtr_array(string, (ArrayValue) fromV);

    String from = fromV.toString();

    int len = from.length();

    if (to.length() < len)
      len = to.length();

    char []map = new char[256];
    for (int i = len - 1; i >= 0; i--)
      map[from.charAt(i)] = to.charAt(i);

    StringBuilder sb = new StringBuilder();

    len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (map[ch] != 0)
        sb.append(map[ch]);
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Translates characters in a string to target values.
   *
   * @param string the source string
   * @param map the character map
   */
  private static String strtr_array(String string, ArrayValue map)
  {
    int size = map.getSize();

    String []from = new String[size];
    String []to = new String[size];
    int k = 0;

    for (Map.Entry<Value,Value> entry : map.entrySet()) {
      from[k] = entry.getKey().toString();
      to[k] = entry.getValue().toString();

      k++;
    }

    StringBuilder result = new StringBuilder();
    int len = string.length();
    int head = 0;

    while (head < len) {
      int bestHead = len;
      int bestI = -1;
      int bestLength = 0;

      for (int i = 0; i < from.length; i++) {
        int p = string.indexOf(from[i], head);

        if (p >= 0 && (p < bestHead ||
                       p == bestHead && bestLength < from[i].length())) {
          bestHead = p;
          bestI = i;
          bestLength = from[i].length();
        }
      }

      if (head != bestHead)
        result.append(string.substring(head, bestHead));

      if (bestI >= 0)
        result.append(to[bestI]);

      head = bestHead + bestLength;
    }

    return result.toString();
  }

  /**
   * Returns a substring
   *
   * @param env the calling environment
   * @param string the string
   * @param start the start offset
   * @param lenV the optional length
   */
  public static Value substr(Env env,
                             String string,
                             int start,
                             @Optional Value lenV)
    throws Throwable
  {
    int strLen = string.length();
    if (start < 0)
      start = strLen + start;

    if (start < 0 || strLen < start)
      return BooleanValue.FALSE;

    if (lenV instanceof DefaultValue) {
      return new StringValue(string.substring(start));
    }
    else {
      int len = lenV.toInt();
      int end;

      if (len < 0)
        end = strLen + len;
      else
        end = start + len;

      if (end <= start)
        return StringValue.EMPTY;
      else if (strLen <= end)
        return new StringValue(string.substring(start));
      else
        return new StringValue(string.substring(start, end));
    }
  }

  public static Value substr_count(Env env,
                                   String haystack,
                                   String needle,
                                   @Optional("0") int offset,
                                   @Optional("-1") int length)
  {
    if (needle.length() == 0) {
      env.warning(L.l("empty substr"));
      return BooleanValue.FALSE;
    }

    int haystackLength = haystack.length();

    if (offset < 0 || offset > haystackLength) {
      env.warning(L.l("offset `{0}' out of range", offset));
      return BooleanValue.FALSE;
    }

    if (length > -1) {
      if (offset + length > haystackLength) {
        env.warning(L.l("length `{0}' out of range", length));
        return BooleanValue.FALSE;
      }
      else
        haystackLength = offset + length;
    }

    int needleLength = needle.length();

    int count = 0;

    int end = haystackLength - needleLength + 1;

    for (int i = offset; i < end; i++) {
      if (haystack.startsWith(needle, i)) {
        count++;
        i += needleLength;
      }
    }

    return new LongValue(count);
  }

  /**
   * Replaces a substring with a replacement
   *
   * @param subjectV a string to modify, or an array of strings to modify
   * @param replacement the replacement string
   * @param startV the start offset
   * @param lengthV the optional length
   */
  public static Value substr_replace(Value subjectV,
                                     String replacement,
                                     Value startV,
                                     @Optional Value lengthV)
  {
    int start = 0;
    int length = Integer.MAX_VALUE / 2;

    if ( !(lengthV.isNull() || lengthV.isArray()) )
      length = lengthV.toInt();

    if ( !(startV.isNull() || startV.isArray()) )
      start = startV.toInt();

    Iterator<Value> startIterator =
      startV.isArray()
      ? ((ArrayValue) startV).values().iterator()
      : null;

    Iterator<Value> lengthIterator =
      lengthV.isArray()
      ? ((ArrayValue) lengthV).values().iterator()
      : null;

    if (subjectV.isArray()) {
      ArrayValue resultArray = new ArrayValueImpl();

      ArrayValue subjectArray = (ArrayValue) subjectV;

      for (Value value : subjectArray.values()) {

        if (lengthIterator != null && lengthIterator.hasNext())
          length = lengthIterator.next().toInt();

        if (startIterator != null && startIterator.hasNext())
          start = startIterator.next().toInt();

        Value result = substr_replace_impl(value.toString(), replacement, start, length);

        resultArray.append(result);
      }

      return resultArray;
    }
    else {
      if (lengthIterator != null && lengthIterator.hasNext())
        length = lengthIterator.next().toInt();

      if (startIterator != null && startIterator.hasNext())
        start = startIterator.next().toInt();

      return substr_replace_impl(subjectV.toString(), replacement, start, length);
    }
  }

  private static Value substr_replace_impl(String string, String replacement, int start, int len)
  {
    int strLen = string.length();

    if (start > strLen)
      start = strLen;
    else if (start < 0)
      start = Math.max(strLen + start, 0);

    int end;

    if (len < 0)
      end = Math.max(strLen + len, start);
    else
      end = Math.min(start + len, strLen);

    String result;

    result = string.substring(0, start) + replacement +  string.substring(end);

    return new StringValue(result);
  }

  /**
   * Uppercases the first character
   *
   * @param string the input string
   */
  public static String ucfirst(String string)
    throws Throwable
  {
    if (string.length() == 0)
      return string;

    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }

  /**
   * Uppercases the first character of each word
   *
   * @param string the input string
   */
  public static String ucwords(String string)
    throws Throwable
  {
    int strLen = string.length();

    boolean isStart = true;
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < strLen; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case ' ': case '\t': case '\r': case '\n':
        isStart = true;
        sb.append(ch);
        break;
      default:
        if (isStart)
          sb.append(Character.toUpperCase(ch));
        else
          sb.append(ch);
        isStart = false;
        break;
      }
    }

    return sb.toString();
  }

  /**
   * Formatted strings with array arguments
   *
   * @param format the format string
   * @param array the arguments to apply to the format string
   */
  public static int vprintf(Env env,
                            String format,
                            @NotNull ArrayValue array)
    throws Throwable
  {
    Value []args;

    if (array != null) {
      args = new Value[array.getSize()];
      int i = 0;
      for (Value value : array.values())
        args[i++] = value;
    }
    else
      args = new Value[0];

    return printf(env, format, args);
  }

  /**
   * Formatted strings with array arguments
   *
   * @param format the format string
   * @param array the arguments to apply to the format string
   */
  public static String vsprintf(String format,
                                @NotNull ArrayValue array)
    throws Throwable
  {
    Value []args;

    if (array != null) {
      args = new Value[array.getSize()];
      int i = 0;
      for (Value value : array.values())
        args[i++] = value;
    }
    else
      args = new Value[0];

    return sprintf(format, args);
  }

  /**
   * Wraps a string to the given number of characters.
   *
   * @param string the input string
   * @param width the width
   * @param breakString the break string
   * @param cut if true, break on exact match
   */
  public static String wordwrap(String string,
                                @Optional("75") int width,
                                @Optional("'\n'") String breakString,
                                @Optional boolean cut)
  {
    int len = string.length();
    int head = 0;

    StringBuilder sb = new StringBuilder();
    while (head + width < len) {
      int tail = head + width;

      if (! cut) {
        for (;
             head < tail && ! Character.isWhitespace(string.charAt(tail));
             tail--) {
        }

        if (head == tail)
          tail = head + width;
      }

      if (sb.length() > 0)
        sb.append(breakString);

      sb.append(string.substring(head, tail));

      head = tail;

      if (! cut && head < len && Character.isWhitespace(string.charAt(head)))
        head++;
    }

    if (head < len) {
      if (sb.length() > 0)
        sb.append(breakString);

      sb.append(string.substring(head));
    }

    return sb.toString();
  }

  /**
   * Returns true if the character is a whitespace character.
   */
  private static boolean isWhitespace(char ch)
  {
    return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
  }

  /**
   * Returns the uppercase equivalent of the caharacter
   */
  private static char toUpperCase(char ch)
  {
    if (ch >= 'a' && ch <= 'z')
      return (char) ('A' + (ch - 'a'));
    else
      return ch;
  }

  /**
   * Converts an integer digit to a uuencoded char.
   */
  private static char toUUChar(int d)
  {
    if (d == 0)
      return (char) 0x60;
    else
      return (char) (0x20 + (d & 0x3f));
  }

  private static char toHexChar(int d)
  {
    if (d < 10)
      return (char) (d + '0');
    else
      return (char) (d - 10 + 'a');
  }

  private static int hexToDigit(char ch)
  {
    if ('0' <= ch && ch <= '9')
      return ch - '0';
    else if ('a' <= ch && ch <= 'f')
      return ch - 'a' + 10;
    else if ('A' <= ch && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }

  private static int octToDigit(char ch)
  {
    if ('0' <= ch && ch <= '7')
      return ch - '0';
    else
      return -1;
  }

  abstract static class PrintfSegment {
    abstract public void apply(StringBuilder sb, Value []args);
  }

  static class TextPrintfSegment extends PrintfSegment {
    private final char []_text;

    TextPrintfSegment(StringBuilder text)
    {
      _text = new char[text.length()];

      text.getChars(0, _text.length, _text, 0);
    }

    public void apply(StringBuilder sb, Value []args)
    {
      sb.append(_text, 0, _text.length);
    }
  }

  static class LongPrintfSegment extends PrintfSegment {
    private final String _format;
    private final int _index;

    LongPrintfSegment(String format, int index)
    {
      _format = format;
      _index = index;
    }

    public void apply(StringBuilder sb, Value []args)
    {
      long value;

      if (_index < args.length)
        value = args[_index].toLong();
      else
        value = 0;

      sb.append(String.format(_format, value));
    }
  }

  static class DoublePrintfSegment extends PrintfSegment {
    private final String _format;
    private final int _index;

    DoublePrintfSegment(String format, int index)
    {
      _format = format;
      _index = index;
    }

    public void apply(StringBuilder sb, Value []args)
    {
      double value;

      if (_index < args.length)
        value = args[_index].toDouble();
      else
        value = 0;

      sb.append(String.format(_format, value));
    }
  }

  static class StringPrintfSegment extends PrintfSegment {
    private final char []_prefix;
    private final int _min;
    private final int _max;
    private final boolean _isLeft;
    private final boolean _isUpper;
    private final char _pad;
    protected final int _index;

    StringPrintfSegment(StringBuilder prefix,
                        boolean isLeft, boolean isZero, boolean isUpper,
                        String format, int index)
    {
      _prefix = new char[prefix.length()];

      _isLeft = isLeft;
      _isUpper = isUpper;

      _pad = isZero ? '0' : ' ';

      prefix.getChars(0, _prefix.length, _prefix, 0);

      int i = 0;
      int len = format.length();

      int min = 0;
      int max = Integer.MAX_VALUE;
      char ch = ' ';

      for (; i < len && '0' <= (ch = format.charAt(i)) && ch <= '9'; i++) {
        min = 10 * min + ch - '0';
      }

      if (ch == '.') {
        max = 0;

        for (i++; i < len && '0' <= (ch = format.charAt(i)) && ch <= '9'; i++) {
          max = 10 * max + ch - '0';
        }
      }

      _min = min;
      _max = max;

      _index = index;
    }

    public void apply(StringBuilder sb, Value []args)
    {
      sb.append(_prefix, 0, _prefix.length);

      String value = toValue(args);

      int len = value.length();

      if (_max < len) {
        value = value.substring(0, _max);
        len = _max;
      }

      if (_isUpper)
        value = value.toUpperCase();

      if (! _isLeft) {
        for (int i = len; i < _min; i++) {
          sb.append(_pad);
        }
      }

      sb.append(value);

      if (_isLeft) {
        for (int i = len; i < _min; i++) {
          sb.append(_pad);
        }
      }
    }

    String toValue(Value []args)
    {
      if (_index < args.length)
        return args[_index].toString();
      else
        return "";
    }
  }

  static class CharPrintfSegment extends StringPrintfSegment {
    CharPrintfSegment(StringBuilder prefix,
                      boolean isLeft, boolean isZero, boolean isUpper,
                      String format, int index)
    {
      super(prefix, isLeft, isZero, isUpper, format, index);
    }

    String toValue(Value []args)
    {
      if (args.length <= _index)
        return "";

      Value v = args[_index];

      if (v.isLong())
        return String.valueOf((char) v.toLong());
      else
        return v.charAt(0).toString();
    }
  }

  static {
    DEFAULT_DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols();
    DEFAULT_DECIMAL_FORMAT_SYMBOLS.setDecimalSeparator('.');
    DEFAULT_DECIMAL_FORMAT_SYMBOLS.setGroupingSeparator(',');
    DEFAULT_DECIMAL_FORMAT_SYMBOLS.setZeroDigit('0');
  }

}

