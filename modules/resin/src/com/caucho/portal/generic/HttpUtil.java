/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.Writer;
import java.io.IOException;

/**
 * HTTP utilities. 
 *
 * Encoding and decoding is for utf strings encoded with the rules defined for
 * the "application/x-www-form-urlencoded" MIME format.
 *
 */
public class HttpUtil
{
  /**
   * benchmarks 
   *    URLEncoder(717) encode(103) URLDecoder(165) decode(70)
   *    without _buffer: encode(153) decode(95) encodeJ(1034) decodeJ(229)
   */ 
  private static Object _bufferLock = new Integer(1);
  private static StringBuffer _buffer = new StringBuffer(256);

  static private StringBuffer getStringBuffer()
  {
    StringBuffer buf;

    synchronized (_bufferLock) {
      buf = _buffer;
      _buffer = null;
    }

    if (buf == null)
      buf = new StringBuffer(256);

    return buf;
  }

  static private void releaseStringBuffer(StringBuffer buf)
  {
    if (buf.capacity() <= 1024) {

      synchronized (_bufferLock) {
        if (_buffer == null || _buffer.capacity() < buf.capacity()) {
          buf.setLength(0);
          _buffer = buf;
        }
      }
    }
  }

  private static Pattern _headerPattern 
    = Pattern.compile("s/[,;\\s]*([^;,\\s]+)[^,]*//");

  /**
   * Return an ordered Set of header elements from an http header.  If there
   * are no header elements found, null is returned.
   *
   * <pre>
   *  text/html; q=1.0, text/*; q=0.8, image/gif; q=0.6, image/jpeg; q=0.8, image/*; q=0.5
   * </pre>
   *
   * returns:
   *
   * <ul> 
   * <li>text/html 
   * <li>text/* 
   * <li>image/gif 
   * <li>image/jpeg
   * <li>image/*
   * </ul> 
   *
   * Note that the  qs value is ignored.
   *
   * @return null or a Set with at least one elemement 
   */
  static public Set<String> getHeaderElements(String headerValue)
  {
    if (headerValue == null)
      return null;

    Matcher matcher = _headerPattern.matcher(headerValue);

    Set<String> resultSet = null;

    while (matcher.find()) {

      if (resultSet == null)
       return new LinkedHashSet<String>();

      resultSet.add(matcher.group(1));
    }

    return resultSet;
  }

  /**
   * Return only the first header element, null if headerValue is null or there
   * are no header elements.
   *
   * A headerValue with a String like "text/html; charset=xxx" returns
   * "text/html".
   *
   * A headerValue with a String like " en; q=1.0, fr; q=0.8 "
   * returns "en".
   */
  static public String getFirstHeaderElement(String headerValue)
  {
    if (headerValue == null)
      return null;

    Matcher matcher = _headerPattern.matcher(headerValue);

    if (matcher.find())
      return matcher.group(1);
    else
      return null;
  }

  /**
   * Extract and decode parameters out of the query string portion of the path
   * and add them to the map.  The parameters are found by looking for the '?'
   * character.
   *
   * @param map the Map to put the parameters in
   * @param url the url
   *
   * @returns the url without the query string
   */
  static public String extractParameters(Map<String, String[]> map, String url)
  {
    int beginIndex = url.indexOf('?');

    if (beginIndex == -1)
      return url;

    return extractParameters(map, url, beginIndex + 1);
  }

  /**
   * Extract and decode parameters out of the query string portion of the path
   * and add them to the map.
   *
   * @param map the Map to put the parameters in
   * @param url the url
   * @param beginIndex the index of the character that follows the '?' character
   *
   * @returns the url without the query string
   */
  static public String extractParameters(Map<String, String[]> map, 
                                         String url,
                                         int beginIndex)
  {
    if (beginIndex == -1)
      return url;

    String result = url.substring(0, beginIndex);

    StringBuffer buf = getStringBuffer();

    if (buf == null)
      buf = new StringBuffer(256);

    String name = null;
    String value = null;

    int len = url.length();

    do {
      int endIndex = url.indexOf('=', beginIndex);

      if (endIndex == -1) {
        endIndex = len;
      }
      else {
        buf.setLength(0);
        HttpUtil.decode(url, beginIndex, endIndex, buf);
        name = buf.toString();
      }

      if (endIndex == len) {
        value = "";
      }
      else {
        beginIndex = endIndex + 1;
        endIndex = url.indexOf('&', beginIndex);

        if (endIndex == -1)
          endIndex = len;

        buf.setLength(0);
        HttpUtil.decode(url, beginIndex, endIndex, buf);
        value = buf.toString();
      }

      String[] values = map.get(name);

      if (values == null) {
        map.put(name, new String[] { value });
      }
      else {
        int valuesLen = values.length;

        String[] newValues = new String[valuesLen + 1];

        for (int valuesIndex = 0; valuesIndex < valuesLen; valuesIndex++) {
          newValues[valuesIndex] = values[valuesIndex];
        }

        newValues[valuesLen] = value;

        map.put(name, newValues);
      }

      if (endIndex == len)
        beginIndex = -1;
      else
        beginIndex = url.indexOf('&', endIndex) + 1;

    } while (beginIndex > 0);

    releaseStringBuffer(buf);

    return result;
  }

  /**
   * Encode a string.
   *
   * @param source the String to encode
   *
   * @return the encoded String 
   */
  static public String encode(String source)
  {
    StringBuffer dest = getStringBuffer();

    encodeUri(source, 0, source.length(), dest);

    String result = dest.toString();

    releaseStringBuffer(dest);

    return result;
  }

  /**
   * Encode a string.
   *
   * @param source the String to encode
   * @param dest a StringBuffer that receives the encoded result
   */
  static public void encode(String source, StringBuffer dest)
  {
    encodeUri(source, 0, source.length(), dest);
  }

  /**
   * Extract and encode a portion of a String.
   *
   * @param source the String to encode
   * @param beginIndex the begin index, inclusive
   * @param endIndex the end index, exclusive
   * @param dest a StringBuffer that receives the encoded result
   */
  static public void encode(String source, 
                            int beginIndex, 
                            int endIndex, 
                            StringBuffer dest)
  {
    encodeUri(source, beginIndex, endIndex, dest);
  }

  /**
   * Extract and encode a portion of a StringBuffer.
   *
   * @param source the StringBuffer to encode
   * @param beginIndex the begin index, inclusive
   * @param endIndex the end index, exclusive
   * @param dest a StringBuffer that receives the encoded result
   */
  static public void encode(StringBuffer source, 
                            int beginIndex, 
                            int endIndex, 
                            StringBuffer dest)
  {
    encodeUri(source, beginIndex, endIndex, dest);
  }

  static public void encodeUri(CharSequence source, 
                               int beginIndex, 
                               int endIndex, 
                               StringBuffer dest)
  {
    for (int i = beginIndex; i < endIndex; i++) {
      char ch = source.charAt(i);
      if ( (ch >= 'a' && ch <= 'z')
          || (ch >= 'A' && ch <= 'z')
          || (ch >= '0' && ch <= '9')
          || ch == '-'
          || ch == '_'
          || ch == '.'
          || ch == '*')
      {
        dest.append(ch);
      }
      else if (ch == ' ') {
        dest.append('+');
      }
      else if (ch <= 0xff) {
        // 8 byte (utf-8)
        dest.append('%');
        dest.append(encodeHex(ch >> 4));
        dest.append(encodeHex(ch));
      }
      else {
        // 16 byte (utf-16)
        dest.append('%');
        dest.append('u');
        dest.append(encodeHex(ch >> 12));
        dest.append(encodeHex(ch >> 8));
        dest.append(encodeHex(ch >> 4));
        dest.append(encodeHex(ch));
      }
    }
  }

  /**
   * Encode a string.
   *
   * @param source the String to encode
   * @param dest a Writer that receives the encoded result
   *
   * @return the encoded String 
   */
  static public void encode(String source, Writer dest)
    throws IOException
  {
    encodeUri(source, 0, source.length(), dest);
  }

  /**
   * Extract and encode a portion of a String.
   *
   * @param source the String to encode
   * @param beginIndex the begin index, inclusive
   * @param endIndex the end index, exclusive
   * @param dest a Writer that receives the encoded result
   */
  static public void encode(String source, 
                            int beginIndex, 
                            int endIndex, 
                            Writer dest)
    throws IOException
  {
    encodeUri(source, beginIndex, endIndex, dest);
  }

  /**
   * Extract and encode a portion of a StringBuffer.
   *
   * @param source the StringBuffer to encode
   * @param beginIndex the begin index, inclusive
   * @param endIndex the end index, exclusive
   * @param dest a Writer that receives the encoded result
   */
  static public void encode(StringBuffer source, 
                            int beginIndex, 
                            int endIndex, 
                            Writer dest)
    throws IOException
  {
    encodeUri(source, beginIndex, endIndex, dest);
  }

  static public void encodeUri(CharSequence source, 
                               int beginIndex, 
                               int endIndex, 
                               Writer dest)
    throws IOException
  {
    for (int i = beginIndex; i < endIndex; i++) {
      char ch = source.charAt(i);
      if ( (ch >= 'a' && ch <= 'z')
          || (ch >= 'A' && ch <= 'z')
          || (ch >= '0' && ch <= '9')
          || ch == '-'
          || ch == '_'
          || ch == '.'
          || ch == '*')
      {
        dest.write(ch);
      }
      else if (ch == ' ') {
        dest.write('+');
      }
      else if (ch <= 0xff) {
        // 8 byte (utf-8)
        dest.write('%');
        dest.write(encodeHex(ch >> 4));
        dest.write(encodeHex(ch));
      }
      else {
        // 16 byte (utf-16)
        dest.write('%');
        dest.write('u');
        dest.write(encodeHex(ch >> 12));
        dest.write(encodeHex(ch >> 8));
        dest.write(encodeHex(ch >> 4));
        dest.write(encodeHex(ch));
      }
    }
  }

  /**
   * Decode a string.
   *
   * @param source the String to decode
   *
   * @return the decoded String
   */ 
  static public String decode(String source)
  {
    StringBuffer dest = getStringBuffer();

    decodeUri(source, 0, source.length(), dest);

    String result = dest.toString();

    releaseStringBuffer(dest);

    return result;
  }

  /**
   * Decode a string.
   *
   * @param source the String to decode
   * @param dest a StringBuffer that receives the decoded result
   */ 
  static public void decode(String source, StringBuffer dest)
  {
    decodeUri(source, 0, source.length(), dest);
  }

  /**
   * Extract and decode an encoded portion of a string.
   *
   * @param source the String to extract from
   * @param beginIndex the begin index, inclusive
   * @param endIndex the end index, exclusive
   * @param dest a StringBuffer that receives the decoded result
   */ 
  static public void decode(String source, 
                            int beginIndex, 
                            int endIndex,
                            StringBuffer dest)
  {
    decodeUri(source, beginIndex, endIndex, dest);
  }

  /**
   * Extract and decode an encoded portion of a StringBuffer.
   *
   * @param source the StringBuffer to extract from
   * @param beginIndex the begin index, inclusive
   * @param endIndex the end index, exclusive
   * @param dest a StringBuffer that receives the decoded result
   */ 
  static public void decode(StringBuffer source, 
                            int beginIndex, 
                            int endIndex,
                            StringBuffer dest)
  {
    decodeUri(source, beginIndex, endIndex, dest);
  }

  static private void decodeUri(CharSequence source, 
                                int beginIndex, 
                                int endIndex,
                                StringBuffer dest)
  {
    int i = beginIndex;

    while (i < endIndex) {
      char ch = source.charAt(i);
      if (ch == '%')
        i = scanUriEscape(source, i + 1, endIndex, dest);
      else if (ch == '+') {
        dest.append(' ');
        i++;
      } else {
        dest.append(ch);
        i++;
      }
    }
  }

  private static int scanUriEscape(CharSequence source, int i, int len,
                                   StringBuffer dest )
  {
    int ch1 = i < len ? ( ((int)source.charAt(i++)) & 0xff) : -1;

    if (ch1 == 'u') {
      ch1 = i < len ? ( ((int)source.charAt(i++)) & 0xff) : -1;
      int ch2 = i < len ? ( ((int)source.charAt(i++)) & 0xff) : -1;
      int ch3 = i < len ? ( ((int)source.charAt(i++)) & 0xff) : -1;
      int ch4 = i < len ? ( ((int)source.charAt(i++)) & 0xff) : -1;

      dest.append((char) ((decodeHex(ch1) << 12) +
                          (decodeHex(ch2) << 8) +
                          (decodeHex(ch3) << 4) +
                          (decodeHex(ch4))));
    }
    else {
      int ch2 = i < len ? ( ((int)source.charAt(i++)) & 0xff) : -1;

      int b = (decodeHex(ch1) << 4) + decodeHex(ch2);;

      dest.append( (char) b);
    }

    return i;
  }

  static char encodeHex(int ch)
  {
    ch &= 0xf;
    if (ch < 10)
      return (char) (ch + '0');
    else
      return (char) (ch + 'a' - 10);
  }
  
  private static int decodeHex(int ch)
  {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return ch - 'a' + 10;
    else if (ch >= 'A' && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }
}

