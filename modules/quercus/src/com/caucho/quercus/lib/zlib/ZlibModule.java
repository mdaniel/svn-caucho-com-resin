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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.zlib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.*;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.TempBufferStringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.lib.zlib.Zlib;
import com.caucho.util.ByteBuffer;

import com.caucho.vfs.*;

/**
 * PHP ZLib
 */
public class ZlibModule extends AbstractQuercusModule {

 // private static final Logger log = Log.open(QuercusZlibModule.class);
  //private static final L10N L = new L10N(QuercusZlibModule.class);

  public static final int FORCE_GZIP = 0x1;
  public static final int FORCE_DEFLATE = 0x2;

  public String []getLoadedExtensions()
  {
    return new String[] { "zlib" };
  }

  /**
   *
   * @param env
   * @param fileName
   * @param mode
   * @param useIncludePath always on
   * @return Zlib
   */
  public Zlib gzopen(Env env,
		     String fileName,
		     String mode,
		     @Optional("false") boolean useIncludePath)
  {
    return new Zlib(env, fileName, mode, useIncludePath);
  }

  /**
   *
   * @param env
   * @param fileName
   * @param useIncludePath
   * @return array of uncompressed lines from fileName
   */
  public Value gzfile(Env env,
		     String fileName,
		     @Optional("false") boolean useIncludePath)
  {
    return (new Zlib(env, fileName, "r", useIncludePath)).gzfile();
  }

  /**
   * outputs uncompressed bytes directly to browser
   */
  public boolean readgzfile(Env env,
                            String fileName,
                            @Optional("false") boolean useIncludePath)
  {
    try {
      Zlib zlib = new Zlib(env, fileName,"r",useIncludePath);
      env.getOut().writeStream(zlib.readgzfile());
      
      return true;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  public int gzwrite(Env env,
                     @NotNull Zlib zp,
                     InputStream is,
                     @Optional("0") int length)
  {
    if (zp == null)
      return 0;

    return zp.gzwrite(env, is,length);
  }

  /**
   * alias of gzwrite
   */
  public int gzputs(Env env,
                    @NotNull Zlib zp,
                    InputStream is,
                    @Optional("0") int length)
  {
    return gzwrite(env, zp, is, length);
  }

  public boolean gzclose(@NotNull Zlib zp)
  {
    if (zp == null)
      return false;

    return zp.gzclose();
  }

  public boolean gzeof(@NotNull Zlib zp)
  {
    if (zp == null)
      return false;

    return zp.gzeof();
  }

  public Value gzgetc(@NotNull Zlib zp)
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgetc();
  }

  public Value gzread(@NotNull Zlib zp,
                      int length)
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzread(length);
  }

  public Value gzgets(@NotNull Zlib zp,
                      int length)
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgets(length);
  }

  public Value gzgetss(@NotNull Zlib zp,
                       int length,
                       @Optional String allowedTags)
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgetss(length,allowedTags);
  }

  public boolean gzrewind(@NotNull Zlib zp)
  {
    if (zp == null)
      return false;

    return zp.gzrewind();
  }

  /**
   * compresses data using zlib
   *
   * @param data
   * @param level (default is Deflater.DEFAULT_COMPRESSION)
   * @return compressed string
   */
  public Value gzcompress(InputStream data,
                          @Optional("6") int level)
  {
    try {
      Deflater deflater = new Deflater(level, true);
      Adler32 crc = new Adler32();

      TempBuffer buf = TempBuffer.allocate();
      byte []buffer = buf.getBuffer();
      boolean isFinished = false;
      TempStream out = new TempStream();

      buffer[0] = (byte) 0x78;

      if (level <= 1)
	buffer[1] = (byte) 0x01;
      else if (level < 6)
	buffer[1] = (byte) 0x5e;
      else if (level == 6)
	buffer[1] = (byte) 0x9c;
      else
	buffer[1] = (byte) 0xda;

      out.write(buffer, 0, 2, false);

      while (! isFinished) {
	while (! isFinished && deflater.needsInput()) {
	  int len = data.read(buffer, 0, buffer.length);

	  if (len > 0) {
	    crc.update(buffer, 0, len);
	    deflater.setInput(buffer, 0, len);
	  }
	  else {
	    isFinished = true;
	    deflater.finish();
	  }
	}

	int len;

	while ((len = deflater.deflate(buffer, 0, buffer.length)) > 0) {
	  out.write(buffer, 0, len, false);
	}
      }

      long value = crc.getValue();
    
      buffer[0] = (byte) (value >> 24);
      buffer[1] = (byte) (value >> 16);
      buffer[2] = (byte) (value >> 8);
      buffer[3] = (byte) (value >> 0);
    
      out.write(buffer, 0, 4, true);
    
      TempBuffer.free(buf);

      return new TempBufferStringValue(out.getHead());
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   *
   * @param data
   * @param length (maximum length of string returned)
   * @return uncompressed string
   */
  public Value gzuncompress(InputStream is,
                            @Optional("0") long length)
  {
    try {
      if (length == 0)
	length = Long.MAX_VALUE;

      //    is.skip(2);

      InflaterInputStream in = new InflaterInputStream(is);

      TempStream out = new TempStream();

      TempBuffer tempBuf = TempBuffer.allocate();
      byte []buffer = tempBuf.getBuffer();

      int len;
      while ((len = in.read(buffer, 0, buffer.length)) >= 0) {
	out.write(buffer, 0, len, false);
      }

      TempBuffer.free(tempBuf);
      in.close();

      return new TempBufferStringValue(out.getHead());
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   *
   * @param level
   * @return compressed using DEFLATE algorithm
   */
  public Value gzdeflate(InputStream data,
                         @Optional("6") int level)
  {
    try {
      Deflater deflater = new Deflater(level, true);

      TempBuffer buf = TempBuffer.allocate();
      byte []buffer = buf.getBuffer();
      boolean isFinished = false;
      TempStream out = new TempStream();

      while (! isFinished) {
	while (! isFinished && deflater.needsInput()) {
	  int len = data.read(buffer, 0, buffer.length);

	  if (len > 0) {
	    deflater.setInput(buffer, 0, len);
	  }
	  else {
	    isFinished = true;
	    deflater.finish();
	  }
	}

	int len;

	while ((len = deflater.deflate(buffer, 0, buffer.length)) > 0) {
	  out.write(buffer, 0, len, false);
	}
      }

      deflater.end();

      TempBuffer.free(buf);

      return new TempBufferStringValue(out.getHead());
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * @param data compressed using Deflate algorithm
   * @param length (maximum length of string returned)
   *
   * @return uncompressed string
   */
  public Value gzinflate(Env env,
			 InputStream data,
                         @Optional("0") long length)
  {
    try {
      Inflater inflater = new Inflater(true);

      TempBuffer buf = TempBuffer.allocate();
      byte []buffer = buf.getBuffer();
      boolean isFinished = false;
      TempStream out = new TempStream();

      while (! isFinished) {
	while (! isFinished && inflater.needsInput()) {
	  int len = data.read(buffer, 0, buffer.length);

	  if (len > 0) {
	    inflater.setInput(buffer, 0, len);
	  }
	  else {
	    isFinished = true;
	  }
	}

	int len;

	while ((len = inflater.inflate(buffer, 0, buffer.length)) > 0) {
	  out.write(buffer, 0, len, false);
	}
      }

      inflater.end();

      TempBuffer.free(buf);

      return new TempBufferStringValue(out.getHead());
    } catch (Exception e) {
      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   *
   * XXX: treated as a wrapper for gzcompress
   *
   * @param data
   * @param level
   * @param encodingMode XXX:ignored for now
   * @return gzcompress
   */
  public Value gzencode(InputStream data,
                        @Optional("-1") int level,
                        @Optional int encodingMode)
  {
    return gzcompress(data, level);
  }

  // @todo gzpassthru()
  // @todo gzseek()
  // @todo gztell()
  // @todo zlib_get_coding_type()
}
