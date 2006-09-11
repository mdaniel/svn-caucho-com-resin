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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.lib.i18n.IconvUtility;

import com.caucho.quercus.lib.gettext.expr.PluralExpr;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Parses Gettext MO files.
 */
class MOFileParser extends GettextParser
{
  private static final Logger log
    = Logger.getLogger(MOFileParser.class.getName());
  private static final L10N L = new L10N(MOFileParser.class);

  private ReadStream _in;
  private byte[] _tmpBuf = new byte[4];

  private boolean _isLittleEndian;

  private int _numberOfStrings;
  private int _offsetOriginal;
  private int _offsetTranslation;

  private String _charset;

  MOFileParser(Path path)
    throws IOException
  {
    init(path);
  }

  private void init(Path path)
    throws IOException
  {
    _in = path.openRead();

    _isLittleEndian = true;
    int magic = readInt();

    if (magic == 0xde120495)
      _isLittleEndian = false;
    else if (magic != 0x950412de)
      return;

    // Ignore file format revision
    readInt();

    _numberOfStrings = readInt();
    _offsetOriginal = readInt();
    _offsetTranslation = readInt();

    if (_numberOfStrings < 0 || _offsetOriginal < 0 || _offsetTranslation < 0)
      return;

    StringValue metadata = getMetadata();
    _pluralExpr = PluralExpr.getPluralExpr(metadata);
    _charset = getCharset(metadata);
  }

  /**
   * Returns the gettext metadata.
   */
  private StringValue getMetadata()
    throws IOException
  {
    _in.setPosition(_offsetTranslation);
    int length = readInt();
    _in.setPosition(readInt());

    return readPluralForms(length).get(0);
  }

  /**
   * Returns the gettext translations.
   *
   * @return translations from file, or null on error
   */
  HashMap<StringValue, ArrayList<StringValue>> readTranslations()
    throws IOException
  {
    int[] originalOffsets = new int[_numberOfStrings];
    int[] translatedOffsets = new int[_numberOfStrings];
    int[] translatedLengths = new int[_numberOfStrings];
    StringValue[] originals = new StringValue[_numberOfStrings];

    _in.setPosition(_offsetOriginal);

    // Read in offsets of the original strings
    for (int i = 0; i < _numberOfStrings; i++) {
      // XXX: length of original strings not needed?
      readInt();

      originalOffsets[i] = readInt();

      if (originalOffsets[i] <= 0)
        return null;
    }

    _in.setPosition(_offsetTranslation);

    // Read in lengths and offsets of the translated strings
    for (int i = 0; i < _numberOfStrings; i++) {
      translatedLengths[i] = readInt();

      translatedOffsets[i] = readInt();

      if (translatedLengths[i] < 0 || translatedOffsets[i] <= 0)
        return null;
    }

    _in.setEncoding(_charset);

    // Read in the original strings
    for (int i = 0; i < _numberOfStrings; i++) {
      _in.setPosition(originalOffsets[i]);

      originals[i] = readOriginalString();
    }

    HashMap<StringValue, ArrayList<StringValue>> map =
            new HashMap<StringValue, ArrayList<StringValue>>();

    // Read translated strings into the HashMap
    for (int i = 0; i < _numberOfStrings; i++) {
      _in.setPosition(translatedOffsets[i]);

      map.put(originals[i], readPluralForms(translatedLengths[i]));
    }

    return map;
  }

  /**
   * Reads in a string until NULL or EOF encountered.
   */
  private StringValue readOriginalString()
    throws IOException
  {
    StringBuilderValue sb = new StringBuilderValue();

    for (int ch = _in.readChar(); ch > 0; ch = _in.readChar()) {
      sb.append((char)ch);
    }

    return sb;
  }

  /**
   * Reads in translated plurals forms that are separated by NULL.
   */
  private ArrayList<StringValue> readPluralForms(int length)
    throws IOException
  {
    ArrayList<StringValue> list = new ArrayList<StringValue>();
    StringBuilderValue sb = new StringBuilderValue();

    for (; length > 0; length--) {
      int ch = _in.readChar();

      if (ch > 0)
        sb.append((char)ch);

      else if (ch == 0) {
        list.add(sb);
        sb = new StringBuilderValue();
      }
      else
        break;
    }

    list.add(sb);
    return list;
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

  void close()
  {
    try {
      if (_in != null)
        _in.close();
    } catch (IOException e) {}
  }
}
