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

package com.caucho.quercus.lib.i18n;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.VariableArguments;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.MailModule;
import com.caucho.quercus.lib.regexp.RegexpModule;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Logger;

public class MbstringModule
  extends AbstractQuercusModule
{
  private static final IniDefinitions _iniDefinitions = new IniDefinitions();


  private static final Logger log =
                       Logger.getLogger(MbstringModule.class.getName());
  private static final L10N L = new L10N(MbstringModule.class);

  public static final int MB_CASE_UPPER = 0;
  public static final int MB_CASE_LOWER = 1;
  public static final int MB_CASE_TITLE = 2;

  /**
   * Returns the extensions implemented by the module.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "mbstring" };
  }

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  /**
   * Upper-cases, lower-cases, or capitalizes first letter of words.
   */
  public static StringValue mb_convert_case(Env env,
                              StringValue str,
                              int mode,
                              @Optional("") String encoding)
  {
    if (mode == MB_CASE_TITLE) {
      encoding = getEncoding(env, encoding);

      StringValue unicodeStr = str.convertToUnicode(env, encoding);
      unicodeStr = toUpperCaseTitle(env, unicodeStr);

      return str.create(env, unicodeStr, encoding);
    }
    else if (mode == MB_CASE_LOWER)
      return mb_strtolower(env, str, encoding);
    else if (mode == MB_CASE_UPPER)
      return mb_strtoupper(env, str, encoding);
    else
      return str;
  }

  /**
   * Converts string of one encoding to another.
   */
  public static StringValue mb_convert_encoding(Env env,
                              StringValue str,
                              String destEncoding,
                              @Optional() String fromEncodings)
  {
    // XXX: fallback encoding
    int tail = fromEncodings.indexOf(',', 1);

    String srcEncoding;

    if (tail < 0)
      srcEncoding = fromEncodings;
    else
      srcEncoding = getEncoding(env, fromEncodings.substring(0, tail).trim());

    return decodeEncode(env, str, srcEncoding, destEncoding);
  }

  /**
   * Performs Japanese specific charset conversion.
   */
  public static StringValue mb_convert_kana(Env env,
                              StringValue str,
                              @Optional("") String option,
                              @Optional("") String encoding)
  {
    throw new UnimplementedException("mb_convert_kana");
  }

  /**
   * Decodes and then encodes variables.
   *
   * XXX: variable arguments to convert.
   */
  @VariableArguments
  public static StringValue mb_convert_variables(Env env,
                              String toEncoding,
                              String fromEncodings,
                              @Reference Value vars)
  {
    // XXX: fallback encoding
    int tail = fromEncodings.indexOf(',', 1);

    if (tail < 0)
      tail = fromEncodings.length();

    String srcEncoding;

    if (tail < 0)
      srcEncoding = fromEncodings;
    else
      srcEncoding = getEncoding(env, fromEncodings.substring(0, tail).trim());

    Value decoded = decodeAll(env, vars, srcEncoding);

    vars.set(encodeAll(env, decoded, toEncoding));

    return env.createString(srcEncoding);
  }

  /**
   * Decodes mime field.
   */
  public static Value mb_decode_mimeheader(Env env,
                              StringValue str)
  {
    String encoding = getEncoding(env);

    try {
      return IconvUtility.decodeMime(env, str, encoding);

    } catch (UnsupportedEncodingException e) {
      throw new QuercusModuleException(e.getMessage());
    }
  }

  /**
   * Decodes HTML numeric entity.
   */
  public static StringValue mb_decode_numericentity(Env env,
                              StringValue str,
                              ArrayValue convmap,
                              @Optional String encoding)
  {
    throw new UnimplementedException("mb_decode_numericentity");
  }

  /**
   * Detects encoding of string.
   */
  public static StringValue mb_detect_encoding(Env env,
                              StringValue str,
                              @Optional Value encoding_list,
                              @Optional boolean strict)
  {
    throw new UnimplementedException("mb_detect_encoding");
  }

  /**
   * Specifies order of charsets to test when detecting encoding.
   */
  public static Value mb_detect_order(Env env,
                              Value encoding_list)
  {
    throw new UnimplementedException("mb_detect_order");
  }

  /**
   * Encodes a string into mime.
   */
  public static StringValue mb_encode_mimeheader(Env env,
                              StringValue str,
                              @Optional("") String charset,
                              @Optional("B") String transfer_encoding,
                              @Optional("") String linefeed)
  {
    charset = getEncoding(env, charset);

    try {
      String mime = IconvUtility.encodeMimeWord(str.toString(),
                                                charset,
                                                transfer_encoding,
                                                linefeed,
                                                76);
      return env.createString(mime);

    } catch (UnsupportedEncodingException e) {
      throw new QuercusModuleException(e.getMessage());
    }

  }

  /**
   * Encodes HTML numeric string entity.
   */
  public static StringValue mb_encode_numericentity(Env env,
                              StringValue str,
                              ArrayValue convmap,
                              @Optional String encoding)
  {
    throw new UnimplementedException();
  }

  /**
   * Returns true if pattern matches a part of string.
   */
  public static BooleanValue mb_ereg_match(Env env,
                              StringValue pattern,
                              StringValue string,
                              @Optional String option)
  {
    String encoding = getEncoding(env);

    pattern = pattern.convertToUnicode(env, encoding);
    string = string.convertToUnicode(env, encoding);

    // XXX: option

    Value val = RegexpModule.ereg(env, pattern, string, null);

    if (val == BooleanValue.FALSE)
      return BooleanValue.FALSE;
    else
      return BooleanValue.TRUE;
  }

  /**
   * Multibyte version of ereg_replace.
   */
  public static Value mb_ereg_replace(Env env,
                              StringValue pattern,
                              StringValue replacement,
                              StringValue subject,
                              @Optional String option)
  {
    String encoding = getEncoding(env);

    pattern = pattern.convertToUnicode(env, encoding);
    replacement = replacement.convertToUnicode(env, encoding);
    subject = subject.convertToUnicode(env, encoding);

    //XXX: option

    Value val = RegexpModule.ereg_replace(env, pattern, replacement, subject);

    return encodeAll(env, val, encoding);
  }

  /**
   * Multibyte version of ereg.
   */
  public static Value mb_ereg(Env env,
                              StringValue pattern,
                              StringValue string,
                              @Optional ArrayValue regs)
  {
    return eregImpl(env, pattern, string, regs, true);
  }

  /**
   * Multibyte version of eregi_replace.
   */
  public static Value mb_eregi_replace(Env env,
                              StringValue pattern,
                              StringValue replacement,
                              StringValue subject,
                              @Optional String option)
  {
    String encoding = getEncoding(env);

    pattern = pattern.convertToUnicode(env, encoding);
    replacement = replacement.convertToUnicode(env, encoding);
    subject = subject.convertToUnicode(env, encoding);

    //XXX: option

    Value val = RegexpModule.eregi_replace(env, pattern, replacement, subject);

    return encodeAll(env, val, encoding);
  }

  /**
   * Multibyte version of eregi.
   */
  public static Value mb_eregi(Env env,
                              StringValue pattern,
                              StringValue string,
                              @Optional ArrayValue regs)
  {
    return eregImpl(env, pattern, string, regs, false);
  }

  private static Value eregImpl(Env env,
                              StringValue pattern,
                              StringValue string,
                              ArrayValue regs,
                              boolean isCaseSensitive)
  {
    String encoding = getEncoding(env);

    pattern = pattern.convertToUnicode(env, encoding);
    string = string.convertToUnicode(env, encoding);

    if (regs == null) {
      if (isCaseSensitive)
        return RegexpModule.ereg(env, pattern, string, null);
      else
        return RegexpModule.eregi(env, pattern, string, null);
    }

    Value val;
    Var regVar = new Var();

    if (isCaseSensitive)
      val = RegexpModule.ereg(env, pattern, string, regVar);
    else
      val = RegexpModule.eregi(env, pattern, string, regVar);

    if (regVar.isset()) {
      regs.clear();
      ArrayValue results = regVar.toArrayValue(env);

      for (Map.Entry<Value,Value> entry : results.entrySet()) {

        Value bytes = encodeAll(env, entry.getValue(), encoding);
        regs.put(entry.getKey(), bytes);
      }

      val = LongValue.create(
              regs.get(LongValue.ZERO).toStringValue().length());
    }

    return val;
  }

  /**
   * Gets current position of ereg state object.
   */
  public static LongValue mb_ereg_search_getpos(Env env)
  {
    EregSearch ereg = getEreg(env);

    if (ereg == null)
      return LongValue.ZERO;

    return LongValue.create(ereg._position);
  }

  /**
   * Gets the last match of ereg state object from previous matching.
   */
  public static Value mb_ereg_search_getregs(Env env)
  {
    EregSearch ereg = getEreg(env);

    if (ereg == null || ereg._lastMatch == null)
      return BooleanValue.FALSE;

    return ereg._lastMatch;
  }

  /**
   * Initializes a ereg state object.
   */
  public static BooleanValue mb_ereg_search_init(Env env,
                              StringValue string,
                              @Optional Value pattern,
                              @Optional Value option)
  {
    EregSearch ereg = new EregSearch(env, string, pattern, option);
    env.setSpecialValue("mb.search", ereg);

    return BooleanValue.TRUE;
  }

  /**
   * Returns index and position after matching.
   */
  public static Value mb_ereg_search_pos(Env env,
                              @Optional Value pattern,
                              @Optional Value option)
  {
    EregSearch ereg = getEreg(env, pattern, option);

    if (ereg == null) {
      env.warning(L.l("Regular expression not set"));
      return BooleanValue.FALSE;
    }

    return ereg.search(env, true);
  }

  /**
   * Returns match array after matching.
   */
  public static Value mb_ereg_search_regs(Env env,
                              @Optional Value pattern,
                              @Optional Value option)
  {
    EregSearch ereg = getEreg(env, pattern, option);

    if (ereg == null) {
      env.warning(L.l("Regular expression not set"));
      return BooleanValue.FALSE;
    }

    if (ereg.search(env, false) == BooleanValue.FALSE)
      return BooleanValue.FALSE;

    return ereg._lastMatch;
  }

  /**
   * Sets the position of the ereg state object.
   */
  public static BooleanValue mb_ereg_search_setpos(Env env,
                              int position)
  {
    EregSearch ereg = getEreg(env);

    if (ereg == null)
      return BooleanValue.FALSE;

    ereg._position = position;
    return BooleanValue.TRUE;
  }

  /**
   * Returns whether or not pattern matches string.
   */
  public static BooleanValue mb_ereg_search(Env env,
                              @Optional Value pattern,
                              @Optional Value option)
  {
    EregSearch ereg = getEreg(env, pattern, option);

    if (ereg == null) {
      env.warning(L.l("Regular expression not set"));
      return BooleanValue.FALSE;
    }

    Value result = ereg.search(env, false);

    return BooleanValue.create(result.toBoolean());
  }

  /**
   * Returns the ereg state object from the environment.
   */
  private static EregSearch getEreg(Env env)
  {
    Object obj = env.getSpecialValue("mb.search");

    if (obj == null)
      return null;

    return (EregSearch)obj;
  }

  /**
   * Returns the ereg state object from the environment iff the ereg object
   * is a valid one.
   */
  private static EregSearch getEreg(Env env,
                              Value pattern,
                              Value option)
  {
    Object obj = env.getSpecialValue("mb.search");

    if (obj != null) {
      EregSearch ereg = (EregSearch)obj;

      ereg.init(env, pattern, option);

      if (ereg._isValidRegexp)
        return ereg;
      else
        return null;
    }
    else
      return null;
  }

  /**
   * Returns current mb settings.
   */
  public static Value mb_get_info(Env env,
                              @Optional("") String type)
  {
    if (type.length() == 0) {
      ArrayValue array = new ArrayValueImpl();

      array.put(env.createString("internal_encoding"),
                env.createString(getEncoding(env)));

      return array;
    }

    else if (type.equals("internal_encoding")) {
      return env.createString(getEncoding(env));

    } else {
      throw new UnimplementedException("mb_get_info");
    }
  }

  /**
   * Returns and/or sets the http input encoding
   */
  public static Value mb_http_input(Env env,
                              @Optional String type)
  {
    throw new UnimplementedException("mb_http_input");
  }
 
  /**
   * Returns and/or sets the http output encoding
   */
  public static Value mb_http_output(Env env,
                              @Optional String encoding)
  {
    throw new UnimplementedException("mb_http_output");
/*
    if (encoding.length() == 0)
      return new StringValueImpl(getEncoding(env));
    else
      return BooleanValue.FALSE;
*/
  }

  /**
   * Returns and/or sets the internal encoding.
   */
  public static Value mb_internal_encoding(Env env,
                              @Optional String encoding)
  {
    if (encoding.length() == 0)
      return env.createString(getEncoding(env));
    else {
      setEncoding(env, encoding);
      return BooleanValue.TRUE;
    }
  }

  /**
   * Returns and/or sets the encoding for mail.
   */
  public static Value mb_language(Env env,
                              @Optional String language)
  {
    String encoding = getEncoding(env);

    if (language.length() == 0) {
      if (encoding.equalsIgnoreCase("ISO-2022-JP"))
        return env.createString("Japanese");
      else if (encoding.equalsIgnoreCase("ISO-8859-1"))
        return env.createString("English");
      else if (encoding.equalsIgnoreCase("UTF-8"))
        return env.createString("uni");
    }
    else if (language.equals("Japanese") || language.equals("ja"))
      setEncoding(env, "ISO-2022-JP");
    else if (language.equals("English") || language.equals("en"))
      setEncoding(env, "ISO-8859-1");
    else if (language.equals("uni"))
      setEncoding(env, "UTF-8");
    else
      return BooleanValue.FALSE;

    return BooleanValue.TRUE;
  }

  /**
   * XXX: get all supported encodings
   */
  public static ArrayValue mb_list_encodings(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    array.put(env.createString("ASCII"));
    array.put(env.createString("UTF-8"));
    array.put(env.createString("UTF-16"));
    array.put(env.createString("ISO-8859-1"));
    array.put(env.createString("ISO-8859-2"));
    array.put(env.createString("ISO-8859-15"));
    array.put(env.createString("ISO-2022-JP"));
    array.put(env.createString("EUC-KR"));
    array.put(env.createString("EUC-CN"));
    array.put(env.createString("EUC-TW"));
    array.put(env.createString("EUC-JP"));
    array.put(env.createString("JIS"));

    return array;
  }

  /**
   * ob_start() handler
   */
  public static StringValue mb_output_handler(Env env,
                              StringValue contents,
                              int value)
  {
    throw new UnimplementedException("mb_output_handler");
  }

  /**
   * Multibyte version of parse_str.
   */
  public static BooleanValue mb_parse_str(Env env,
                              StringValue strValue,
                              @Optional @Reference Value result)
  {
    String encoding = getEncoding(env);
    StringModule.parse_str(env,strValue.toString(), result);

    if (result == null) {
      // XXX: encode newly added global variables
      return BooleanValue.TRUE;
    }
    else {
      Value array = encodeAll(env, result, encoding);
      result.set(array);

      return BooleanValue.TRUE;
    }
  }

  /**
   * Returns the preferred mime name of this encoding.
   */
  public static StringValue mb_preferred_mime_name(Env env,
                              StringValue encoding)
  {
    String mimeName = Encoding.getMimeName(encoding.toString());

    return env.createString(mimeName);
  }

  /**
   * Returns and/or sets encoding for mb regular expressions.
   */
  public static Value mb_regex_encoding(Env env,
                              @Optional("") String encoding)
  {
    return mb_internal_encoding(env, encoding);
  }

  /**
   * XXX: what does this actually do?
   */
  public static StringValue mb_regex_set_options(Env env,
                              @Optional String options)
  {
    throw new UnimplementedException("mb_regex_set_options");
  }

  /**
   * Multibyte version of mail.
   */
  public static BooleanValue mb_send_mail(Env env,
                              StringValue to,
                              StringValue subject,
                              StringValue message,
                              @Optional StringValue additionalHeaders,
                              @Optional StringValue additionalParameters)
  {
    String encoding = getEncoding(env);

    subject = subject.toBinaryValue(env, encoding);
    message = message.toBinaryValue(env, encoding);
    additionalHeaders = additionalHeaders.toBinaryValue(env, encoding);

    boolean result = MailModule.mail(env,
                                     to.toString(),
                                     subject.toString(),
                                     message,
                                     additionalHeaders.toString(),
                                     additionalParameters.toString());

    return BooleanValue.create(result);
  }

  /**
   * Multibyte version of split.
   */
  public static Value mb_split(Env env,
                              StringValue pattern,
                              StringValue string,
                              @Optional("-1") long limit)
  {
    String encoding = getEncoding(env);

    pattern = pattern.convertToUnicode(env, encoding);
    string = string.convertToUnicode(env, encoding);

    Value val = RegexpModule.split(env, pattern, string, limit);

    return encodeAll(env, val, encoding);
  }

  /**
   * Similar to substr except start index is at the beginning of char
   * boundaries.
   */
  public static StringValue mb_strcut(Env env,
                              StringValue str,
                              int start,
                              @Optional("7fffffff") int length,
                              @Optional String encoding)
  {
    encoding = getEncoding(env, encoding);

    str = str.convertToUnicode(env, encoding);

    int len = str.length();
    int end = start + length;

    if (end > len)
      end = len;

    if (start < 0 || start > end)
      return StringValue.EMPTY;

    // XXX: not quite exactly the same behavior as PHP
    if (Character.isHighSurrogate(str.charAt(start)))
      start--;

    str = str.substring(start, end);

    return str.toBinaryValue(env, encoding);
  }

  /**
   * Truncates the string.
   */
  public static StringValue mb_strimwidth(Env env,
                              StringValue str,
                              int start,
                              int width,
                              @Optional() StringValue trimmarker,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    StringValue unicodeStr = str.convertToUnicode(env, encoding);

    int len = unicodeStr.length();
    int end = start + width;

    if (end > len)
      end = len;

    if (start < 0 || start > end)
      return str.getEmptyString();

    unicodeStr = unicodeStr.substring(start, end);

    if (end < len && trimmarker.length() > 0) {
      StringValue sb = env.createUnicodeBuilder();

      sb.append(unicodeStr);
      sb.append(trimmarker.convertToUnicode(env, encoding));

      unicodeStr = sb;
    }

    return str.create(env, unicodeStr, encoding);
  }

  /**
   * Multibyte version of strlen.
   */
  public static LongValue mb_strlen(Env env,
                              StringValue str,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    str = str.convertToUnicode(env, encoding);

    return LongValue.create(str.length());
  }

  /**
   * Multibyte version of strpos.
   */
  public static Value mb_strpos(Env env,
                              StringValue haystack,
                              StringValue needle,
                              @Optional("0") int offset,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    haystack = haystack.convertToUnicode(env, encoding);
    needle = needle.convertToUnicode(env, encoding);

   return StringModule.strpos(haystack, needle, offset);
  }

  /**
   * Multibyte version of strrpos.
   */
  public static Value mb_strrpos(Env env,
                              StringValue haystack,
                              StringValue needle,
                              @Optional Value offsetV,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    haystack = haystack.convertToUnicode(env, encoding);
    needle = needle.convertToUnicode(env, encoding);

    return StringModule.strrpos(haystack, needle, offsetV);
  }

  /**
   * Converts all characters to lower-case.
   */
  public static StringValue mb_strtolower(Env env,
                              StringValue str,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    StringValue unicodeStr = str.convertToUnicode(env, encoding);
    unicodeStr = StringModule.strtolower(str);

    return str.create(env, unicodeStr, encoding);
  }

  /**
   * Converts all characters to upper-case.
   */
  public static StringValue mb_strtoupper(Env env,
                              StringValue str,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    StringValue unicodeStr = str.convertToUnicode(env, encoding);
    unicodeStr = StringModule.strtoupper(str);

    return str.create(env, unicodeStr, encoding);
  }

  /**
   * Returns the width of this multibyte string.
   */
  public static LongValue mb_strwidth(Env env,
                              StringValue str,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    str = str.convertToUnicode(env, encoding);

    return LongValue.create(str.length());

/*
    int width = 0;
    int len = string.length();

    // Per PHP manual
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (ch <= 0x19)
        continue;
      else if (ch <= 0x1fff)
        width += 1;
      else if (ch <= 0xff60)
        width += 2;
      else if (ch <= 0xff9f)
        width += 1;
      else
        width += 2;
    }

    return LongValue.create(width);
*/
  }

  /**
   * Sets the character to use when decoding/encoding fails on a character.
   */
  public static Value mb_substitute_character(Value substrchar)
  {
    throw new UnimplementedException("mb_substitute_character");
  }

  public static LongValue mb_substr_count(Env env,
                              StringValue haystack,
                              StringValue needle,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    haystack = haystack.convertToUnicode(env, encoding);
    needle = needle.convertToUnicode(env, encoding);

    int count = 0;
    int sublen = needle.length();

    int i = haystack.indexOf(needle);

    while (i >= 0) {
      i = haystack.indexOf(needle, i + sublen);
      count++;
    }

    return LongValue.create(count);
  }

  /**
   * Multibyte version of substr.
   */
  public static StringValue mb_substr(Env env,
                              StringValue str,
                              int start,
                              @Optional Value lengthV,
                              @Optional String encoding)
  {
    encoding = getEncoding(env, encoding);

    str = str.convertToUnicode(env, encoding);

    Value val = StringModule.substr(env, str, start, lengthV);

    if (val == BooleanValue.FALSE)
      return StringValue.EMPTY;

    return val.toStringValue().toBinaryValue(env, encoding);
  }


  // Private helper functions

  /**
   * Returns string with words capitalized and intermediate letters are
   * made lower-case.
   */
  private static StringValue toUpperCaseTitle(Env env, StringValue string)
  {
    StringValue sb = string.createEmptyStringBuilder();

    int strLen = string.length();
    boolean isWordStart = true;

    for (int i = 0; i < strLen; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case ' ': case '\t': case '\r': case '\n':
        isWordStart = true;
        sb.append(ch);
        break;
      default:
        if (isWordStart) {
          sb.append(Character.toUpperCase(ch));
          isWordStart = false;
        }
        else
          sb.append(Character.toLowerCase(ch));
        break;
      }
    }

    return sb;
  }

  private static String getEncoding(Env env)
  {
    return env.getRuntimeEncoding().toString();
  }

  private static String getEncoding(Env env, String encoding)
  {
    if (encoding == null || encoding.length() == 0)
      return getEncoding(env);
    else
      return encoding;
  }

  private static void setEncoding(Env env, String encoding)
  {
    env.setRuntimeEncoding(encoding);
  }

  /**
   * Recursively decodes objects and arrays.
   */
  private static Value decodeAll(Env env,
                              Value val,
                              String encoding)
  {
    val = val.toValue();

    if (val instanceof StringValue)
      return ((StringValue)val).convertToUnicode(env, encoding);

    else if (val instanceof ArrayValue) {
      ArrayValue array = new ArrayValueImpl();

      for (Map.Entry<Value,Value> entry : ((ArrayValue)val).entrySet()) {
        array.put(entry.getKey(),
                  decodeAll(env, entry.getValue(), encoding));
      }

      return array;
    } else if (val instanceof ObjectValue) {

      ObjectValue obj = (ObjectValue)val;

      for (Map.Entry<String,Value> entry : obj.entrySet()) {
        obj.putField(env,
                     entry.getKey(),
                     decodeAll(env, entry.getValue(), encoding));
      }

      return obj;
    } else
      return val;
  }

  /**
   * Recursively encodes objects and arrays.
   */
  private static Value encodeAll(Env env,
                                 Value val,
                                 String encoding)
  {
    val = val.toValue();

    if (val instanceof StringValue) {
      StringValue sb = env.createBinaryBuilder();
      
      sb.append(env, (StringValue) val, encoding);
      
      return sb;
    }
    else if (val instanceof ArrayValue) {
      ArrayValue array = new ArrayValueImpl();

      for (Map.Entry<Value,Value> entry : ((ArrayValue)val).entrySet()) {
        array.put(entry.getKey(),
                  encodeAll(env, entry.getValue(), encoding));
      }

      return array;
    } else if (val instanceof ObjectValue) {

      ObjectValue obj = (ObjectValue)val;

      for (Map.Entry<String,Value> entry : obj.entrySet()) {
        obj.putField(env,
                     entry.getKey(),
                     encodeAll(env, entry.getValue(), encoding));
      }

      return obj;
    } else
      return val;
  }

  private static StringValue decodeEncode(Env env,
                              StringValue val,
                              String srcEncoding,
                              String destEncoding)
  {
    try {
      return IconvUtility.decodeEncode(env, val, srcEncoding, destEncoding);
    } catch (UnsupportedEncodingException e) {
      throw new QuercusModuleException(e.getMessage());
    }
  }

  /**
   * ereg state object (saves previous match and other info)
   *
   * XXX: option
   */
  static class EregSearch {
    private StringValue _string;
    private StringValue _pattern;
    private Value _option;
    private int _length;

    ArrayValue _lastMatch;
    int _position;
    boolean _isValidRegexp;

    EregSearch(Env env,
                 StringValue string,
                 Value pattern,
                 Value option)
    {
      _string = string.convertToUnicode(env, getEncoding(env));
      _position = 0;
      _length = _string.length();

      init(env, pattern, option);
    }

    void init(Env env, Value pattern, Value option)
    {
      _option = option;
      initPattern(env, pattern);
    }

    void initPattern(Env env, Value pattern)
    {
      if (pattern instanceof StringValue) {
        _pattern = pattern.toStringValue();
        _isValidRegexp = true;
      }
      else
        _isValidRegexp = (_pattern != null);
    }

    StringValue getString(Env env)
    {
      if (_position == 0)
        return _string;
      else if (_position < _length)
        return _string.substring(_position);
      else
        return StringValue.EMPTY;
    }

    Value search(Env env, boolean isArrayReturn)
    {
      StringValue string = getString(env);

      ArrayValue regs = new ArrayValueImpl();
      Value val = eregImpl(env, _pattern, string, regs, true);

      if (val == BooleanValue.FALSE)
        return BooleanValue.FALSE;

      StringValue match = regs.get(LongValue.ZERO).toStringValue();
      int matchIndex = _string.indexOf(match, _position);
      int matchLength = match.length();

      _position = matchIndex + matchLength;
      _lastMatch = regs;

      if (isArrayReturn) {
        ArrayValue array = new ArrayValueImpl();

        array.put(LongValue.create(matchIndex));
        array.put(LongValue.create(matchLength));

        return array;

      } else
        return BooleanValue.TRUE;
    }
  }

  static final IniDefinition INI_MBSTRING_HTTP_INPUT
    = _iniDefinitions.add("mbstring.http_input", "pass", PHP_INI_ALL);
  static final IniDefinition INI_MBSTRING_HTTP_OUTPUT
    = _iniDefinitions.add("mbstring.http_output", "pass", PHP_INI_ALL);
}
