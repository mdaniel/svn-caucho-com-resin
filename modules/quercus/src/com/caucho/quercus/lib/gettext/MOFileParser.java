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

package com.caucho.quercus.lib.gettext;

import com.caucho.quercus.UnimplementedException;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.lib.i18n.UnicodeModule;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses Gettext MO files.
 */
class MOFileParser
{
  private static final Logger log
    = Logger.getLogger(MOFileParser.class.getName());
  private static final L10N L = new L10N(MOFileParser.class);

  private byte[] _tmpBuf = new byte[4];

  private boolean _isLittleEndian;
  private ReadStream _in;

  private int _numberOfStrings;
  private int _offsetOriginal;
  private int _offsetTranslation;

  private PluralExpr _pluralExpr;
  private String _charset;

  private MOFileParser() {}

  private boolean init(Path path)
    throws IOException
  {
    close();

    _isLittleEndian = true;
    _in = path.openRead();

    int magic = readInt();

    if (magic == 0xde120495)
      _isLittleEndian = false;
    else if (magic != 0x950412de)
      return false;

    // Ignore file format revision
    readInt();

    _numberOfStrings = readInt();
    _offsetOriginal = readInt();
    _offsetTranslation = readInt();

    if (_numberOfStrings < 0 || _offsetOriginal < 0 || _offsetTranslation < 0)
      return false;

    String metaData = getMetaData();
    _pluralExpr = PluralExpr.getPluralExpr(metaData);
    _charset = getCharset(metaData);

    return true;
  }

  private static String getCharset(String metaData)
  {
    String header = "charset=";
    int i = metaData.indexOf(header);

    if (i < 0)
      return "UTF-8";

    i = i + header.length();
    int len = metaData.length();

    int j = i + 1;
    for (; j < len; j++) {
      char ch = metaData.charAt(j);

      switch (ch) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          return metaData.substring(i, j);
        default:
          continue; 
      }
    }

    return metaData.substring(i, j);
  }

  /**
   * Finds translated string and it.
   *
   * @param env
   * @param path
   * @param message untranslated string
   *
   * @return translated string, or original message if translation not found.
   */
  public static StringValue search(Env env,
                       Path path,
                       StringValue message)
  {
    MOFileParser parser = new MOFileParser();

    try {
      if (parser.init(path) == false) {
        env.warning(L.l("Error reading MO file: {1}", path));
        return null;
      }

      return parser.binarySearch(
                  env,
                  message.toString(),
                  0,
                  parser._numberOfStrings,
                  0);

    } catch (IOException e) {
      env.warning(L.l(e.getMessage()));
      log.log(Level.FINE, e.toString(), e);
      return null;

    } finally {
      parser.close();
    }
  }

  /**
   * Finds translated plural string and returns it.
   *
   * @param env
   * @param path
   * @param message untranslated string
   * @param n get plural form for this quantity
   *
   * @return translated string, or original message if translation not found.
   */
  public static StringValue search(Env env,
                          Path path,
                          StringValue msgid1,
                          StringValue msgid2,
                          int n)
  {
    MOFileParser parser = new MOFileParser();

    try {
      if (parser.init(path) == false) {
        env.warning(L.l("Error reading MO file: {1}", path));
        return null;
      }

      return parser.binarySearch(
                  env,
                  msgid1.toString(),
                  0,
                  parser._numberOfStrings,
                  parser._pluralExpr.eval(n));

    } catch (IOException e) {
      env.warning(L.l(e.getMessage()));
      log.log(Level.FINE, e.toString(), e);
      return null;

    } finally {
      parser.close();
    }
  }

  /**
   * Strings are ordered lexicographically, so use binary search.
   * Converts strings to system encoding for comparison.
   * (Documentation do not mandate encoding of original strings)
   */
  private StringValue binarySearch(Env env,
                          String message,
                          int left,
                          int right,
                          int pluralForm)
    throws IOException
  {
    if (right < left)
      return null;

    int middle = (right - left) / 2 + left;
    int result = message.compareTo(getOriginal(middle));

    if (result > 0)
      return binarySearch(env,
                  message,
                  middle + 1,
                  right,
                  pluralForm);

    else if (result < 0)
      return binarySearch(env,
                  message,
                  left,
                  middle - 1,
                  pluralForm);

    StringValue translation = readTranslation(env, middle, pluralForm);

    if (translation != null)
      return translation;

    return readTranslation(env, middle, 0);
  }

  /**
   * XXX: implement charset decoding after 2nd pass of iconv is done.
   * Returns the desired plural form of the translated string.
   */
  private StringValue readTranslation(Env env, int pos, int pluralForm)
    throws IOException
  {
    int len = seek(_offsetTranslation, pos);

    while (pluralForm > 0) {
      int ch = _in.read();

      if (ch < 0)
        break;
      if (ch == 0)
        pluralForm--;

      len--;
    }

    if (len < 0)
      return null;

    if (len == 0 && pluralForm > 0)
      return null;

    BinaryBuilderValue bbv = new BinaryBuilderValue();

    for (int i = 0; i < len; i++) {
      int ch = _in.read();

      if (ch <= 0)
        break;

      bbv.append(ch);
    }

    return bbv;

/*
    Value val = UnicodeModule.iconv(env, _charset, "UTF-16", bbv);
    
    if (val == BooleanValue.FALSE)
      return bbv;

    return val.toStringValue();
*/
  }

  /**
   * Seeks to specified position and returns length of string at this position.
   */
  private int seek(int offset, int pos)
    throws IOException
  {
    _in.setPosition(offset + pos * 8);
    int len = readInt();

    _in.setPosition(readInt());

    return len;
  }

  private int readInt()
    throws IOException
  {
    int len = _in.read(_tmpBuf);

    if (len != 4)
      return -1;

    if (_isLittleEndian) {
      return (_tmpBuf[0] & 0xff) |
              (_tmpBuf[1] & 0xff) << 8 |
              (_tmpBuf[2] & 0xff) << 16 |
              _tmpBuf[3] << 24;
    }
    else {
      return _tmpBuf[0] << 24 |
              (_tmpBuf[1] & 0xff) << 16 |
              (_tmpBuf[2] & 0xff) << 8 |
              (_tmpBuf[3] & 0xff);
    }
  }

  /**
   * Returns original string at this position
   */
  private String getOriginal(int pos)
    throws IOException
  {
    return getString(_offsetOriginal, pos);
  }

  private String getMetaData()
    throws IOException
  {
    return getString(_offsetTranslation, 0);
  }

  /**
   * Returns a String in the MO file in the default encoding.
   */
  private String getString(int offset, int pos)
    throws IOException
  {
    int len = seek(offset, pos);
    byte[] buffer = new byte[len];

    int i = 0;
    for (; len > 0; len--) {
      int ch = _in.read();

      if (ch == 0)
        break;

      if (ch < 0)
        break;
      buffer[i++] = (byte)ch;
    }

    return new String(buffer, 0, i);
  }

  public void close()
  {
    try {
      if (_in != null)
        _in.close();
    } catch (IOException e) {}
  }
}
