/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.GZIPInputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.regex.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;

import com.caucho.util.L10N;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.vfs.Path;

/**
 * Zlib object oriented API facade
 */

public class ZlibClass {
  private static final Logger log = Logger.getLogger(ZlibClass.class.getName());
  private static final L10N L = new L10N(ZlibClass.class);

  private Deflater _deflater;
  private Inflater _inflater;
  private Value _fileValue; // Created by fopen... can be BooleanValue.FALSE

  /**
   * Creates and sets _deflater.
   * Also creates _fileValue.  All functions are wrappers around
   * the _fileValue functions using _deflater to compress the
   * byte stream or _inflater to decompress the byte stream.
   *
   * @param fileName
   * @param mode (ie: "w9" or "r7f")
   * @param useIncludePath is always on
   */
  public ZlibClass(Env env,
                   String fileName,
                   String mode,
                   int useIncludePath)
  {
    _inflater = new Inflater();

    // Set level
    Pattern pattern = Pattern.compile("[0-9]");
    Matcher matcher = pattern.matcher(mode);
    if (matcher.find())
      _deflater = new Deflater((int) mode.charAt(matcher.start()) - (int) '0');
    else
      _deflater = new Deflater();

    /**
     * XXX: Skipping strategy for now because it breaks
     * _deflater.deflate later (ie: makes it always return
     * 0 for bytes compressed).
     *
     * // Set Strategy (ie: filtered)
     * if (mode.indexOf("f") != -1)
     *   _deflater.setStrategy(Deflater.FILTERED);
     *
     * // Set Strategy Huffman only
     * if (mode.indexOf("h") != -1)
     *   _deflater.setStrategy(Deflater.HUFFMAN_ONLY);
     */

    // Strip everything to the right of the level
    // before sending mode to fopen
    _fileValue = QuercusFileModule.fopen(env, fileName, mode.substring(0,matcher.start()), true, null);

  }

  /**
   *
   * @param s
   * @param length # of bytes to compress
   * @return # of uncompressed bytes
   */
  public int gzwrite(Env env,
                     @NotNull String s,
                     @Optional("0") int length)
  {
    if ((_fileValue == null) || (_fileValue == BooleanValue.FALSE)) {
      env.warning(L.l("file could not be open for writing"));
      return -1;
    }

    FileValue fileValue = (FileValue) _fileValue;

    if (length == 0)
      length = s.length();
    else
      length = Math.min(length, s.length());
    byte[] input;
    try {
      input = s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      input = s.getBytes();
    }
    _deflater.setInput(input);
    _deflater.finish();
    byte[] output = new byte[2 * input.length];
    int compressedDataLength = _deflater.deflate(output);

    try {
      fileValue.write(output, 0, compressedDataLength);
    } catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }

    return length;
  }

  public boolean gzclose()
  {
    if ((_fileValue == null) || (_fileValue == BooleanValue.FALSE)) {
      return false;
    }

    FileValue fv = (FileValue) _fileValue;

    fv.close();

    return true;
  }
}
