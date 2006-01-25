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

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.util.ByteBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Zlib object oriented API facade
 */

public class ZlibClass {
  private static final Logger log = Logger.getLogger(ZlibClass.class.getName());
  private static final L10N L = new L10N(ZlibClass.class);

  private Deflater _deflater;
  private BufferedReader _bufferedReader;

  private Path _path;
  private Value _fileValue; // Created by fopen... can be BooleanValue.FALSE

  /**
   * Creates and sets _deflater.
   * Also creates _fileValue.  All write functions are wrappers around
   * the _fileValue functions using _deflater to compress the
   * byte stream.
   * 
   * All read functions use _bufferedReader;
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

    _path = env.getPwd().lookup(fileName);

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

    byte[] input = s.getBytes();

    _deflater.setInput(input);
    _deflater.finish();
    byte[] output = new byte[2 * input.length];
    int compressedDataLength = _deflater.deflate(output);

    try {
      fileValue.write(output, 0, compressedDataLength);
      fileValue.flush();
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

  /**
   * alias of gzwrite
   * @param env
   * @param s
   * @param length
   * @return # of uncompressed bytes
   */
  public int gzputs(Env env,
                    @NotNull String s,
                    @Optional("0") int length)
  {
    return gzwrite(env, s, length);
  }

  /**
   *
   * @return the next character or BooleanValue.FALSE
   */
  public Value gzgetc()
    throws IOException, DataFormatException
  {
    if (_bufferedReader == null) {
      getBufferedReader();
    }
    int ch = _bufferedReader.read();
    
    if (ch >= 0)
      return new StringValue(Character.toString((char) ch));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Gets a (uncompressed) string of up to length - 1 bytes read
   * from the given file pointer. Reading ends when length - 1 bytes
   * have been read, on a newline, or on EOF (whichever comes first).
   *
   * @param length
   * @return StringValue
   */
  public Value gzgets(int length)
    throws IOException, DataFormatException
  {
    if (_bufferedReader == null) {
      getBufferedReader();
    }
    
    StringBuffer sb = new StringBuffer();
    int readChar;
    for (int i=0; i < length - 1; i++) {
      readChar = _bufferedReader.read();
      if (readChar >= 0) {
        sb.append(Character.toString((char) readChar));
        if ((((char) readChar) == '\n') || (((char) readChar) == '\r'))
          break;
      } else
        break;
    }
    if (sb.length() > 0)
      return new StringValue(sb.toString());
    else
      return BooleanValue.FALSE;
  }

  /**
   * 
   * @return true if eof
   */
  public boolean gzeof()
    throws IOException
  {
    if (_bufferedReader == null) {
      getBufferedReader();
    }
    
    _bufferedReader.mark(1);
    int result = _bufferedReader.read();
    _bufferedReader.reset();
    
    return (result == -1);
  }

  /**
   * 
   * @param length
   * @param allowedTags
   * @return next line stripping tags
   * @throws IOException
   * @throws DataFormatException
   */
  public Value gzgetss(int length,
                       @Optional String allowedTags)
    throws IOException, DataFormatException
  {
    if (_bufferedReader == null) {
      getBufferedReader();
    }
    
    StringBuffer sb = new StringBuffer();
    int readChar;
    for (int i=0; i < length - 1; i++) {
      readChar = _bufferedReader.read();
      if (readChar >= 0) {
        sb.append(Character.toString((char) readChar));
        if ((((char) readChar) == '\n') || (((char) readChar) == '\r'))
          break;
      } else
        break;
    }
    if (sb.length() > 0)
      return new StringValue(QuercusStringModule.strip_tags(sb.toString(), allowedTags));
    else
      return BooleanValue.FALSE;
  }

  /**
   * helper function to open file for reading when necessary
   * 
   * @throws IOException
   */
  private void getBufferedReader()
    throws IOException
  {
    _bufferedReader = new BufferedReader(new InputStreamReader(new InflaterInputStream(_path.openRead(), new Inflater())));
  }
}
