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
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.TempBufferStringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.util.ByteBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImplOutputStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.VfsStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * PHP ZLib
 */
public class QuercusZlibModule extends AbstractQuercusModule {

 // private static final Logger log = Log.open(QuercusZlibModule.class);
  //private static final L10N L = new L10N(QuercusZlibModule.class);

  public static final int FORCE_GZIP = 0x1;
  public static final int FORCE_DEFLATE = 0x2;

  /**
   * Returns true for the Zlib extension.
   */
  public boolean isExtensionLoaded(String name)
  {
    return "zlib".equals(name);
  }

  /**
   *
   * @param env
   * @param fileName
   * @param mode
   * @param useIncludePath always on
   * @return Zlib
   */
  public Value gzopen(Env env,
                      String fileName,
                      String mode,
                      @Optional("0") int useIncludePath)
  {
    if (fileName == null)
      return BooleanValue.FALSE;

    Zlib zlib = new Zlib(env, fileName, mode, useIncludePath);
    return env.wrapJava(zlib);
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
                      @Optional("0") int useIncludePath)
    throws IOException, DataFormatException
  {
    if (fileName == null)
      return BooleanValue.FALSE;

    Zlib zlib = new Zlib(env,fileName,"r",useIncludePath);
    return zlib.gzfile();
  }

  /**
   * outputs uncompressed bytes directly to browser
   * 
   * @param env
   * @param fileName
   * @param useIncludePath
   * @return
   * @throws IOException
   * @throws DataFormatException
   */
  public boolean readgzfile(Env env,
                            String fileName,
                            @Optional("0") int useIncludePath)
    throws IOException, DataFormatException
  {
    if (fileName == null)
      return false;

    Zlib zlib = new Zlib(env, fileName,"r",useIncludePath);
    env.getOut().writeStream(zlib.readgzfile());
    return true;
  }

  public int gzwrite(Env env,
                     @NotNull Zlib zp,
                     String s,
                     @Optional("0") int length)
  {
    if ((zp == null) || (s == null) || ("".equals(s)))
      return 0;

    return zp.gzwrite(env, s,length);
  }

  /**
   *
   * @param env
   * @param zp
   * @param s
   * @param length
   * @return alias of gzwrite
   */
  public int gzputs(Env env,
                    @NotNull Zlib zp,
                    String s,
                    @Optional("0") int length)
  {
    return gzwrite(env, zp, s, length);
  }

  public boolean gzclose(@NotNull Zlib zp)
  {
    if (zp == null)
      return false;

    return zp.gzclose();
  }

  public boolean gzeof(@NotNull Zlib zp)
    throws IOException
  {
    if (zp == null)
      return false;

    return zp.gzeof();
  }

  public Value gzgetc(@NotNull Zlib zp)
    throws IOException, DataFormatException
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgetc();
  }

  public Value gzread(@NotNull Zlib zp,
                      int length)
    throws IOException, DataFormatException
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzread(length);
  }

  public Value gzgets(@NotNull Zlib zp,
                      int length)
    throws IOException, DataFormatException
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgets(length);
  }

  public Value gzgetss(@NotNull Zlib zp,
                       int length,
                       @Optional String allowedTags)
    throws IOException, DataFormatException
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgetss(length,allowedTags);
  }

  public boolean gzrewind(@NotNull Zlib zp)
    throws IOException
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
  public Value gzcompress(String data,
                          @Optional("-1") int level)
    throws DataFormatException, IOException
  {
    if (level == -1)
      level = Deflater.DEFAULT_COMPRESSION;

    Deflater deflater = new Deflater(level);
    byte[] input = data.getBytes();
    deflater.setInput(input);
    deflater.finish();

    byte[] output = new byte[input.length];
    ByteBuffer buf = new ByteBuffer();
    int compressedDataLength;
    int fullCompressedDataLength = 0;
    while (!deflater.finished()) {
      compressedDataLength = deflater.deflate(output);
      fullCompressedDataLength += compressedDataLength;
      buf.append(output,0,compressedDataLength);
    }

    ByteArrayInputStream result = new ByteArrayInputStream(buf.getBuffer(),0,fullCompressedDataLength);
    ReadStream readStream = new ReadStream(new VfsStream(result,null));
    return new TempBufferStringValue(TempBuffer.copyFromStream(readStream));
  }

  /**
   * 
   * @param data
   * @param length (maximum length of string returned)
   * @return uncompressed string
   */
  public Value gzuncompress(InputStream is,
                            @Optional("0") long length)
    throws DataFormatException, IOException
  {
    if (length == 0)
      length = Long.MAX_VALUE;
    
    InflaterInputStream iis = new InflaterInputStream(is, new Inflater());
    
    StringBuilder uncompressed = new StringBuilder();
    int numChars = 0;
    int ch;
    
    while ((numChars < length) && ((ch = iis.read()) != -1)) {
      numChars++;
      uncompressed.append((char) ch);
    }
    
    return new StringValueImpl(uncompressed.toString());
  }

  /**
   * 
   * @param level
   * @return compressed using DEFLATE algorithm
   */
  public Value gzdeflate(InputStream is,
                         @Optional("-1") int level)
   throws DataFormatException, IOException
  {
    if (level == -1)
      level = Deflater.DEFAULT_COMPRESSION;

    TempStream ts = new TempStream();
    OutputStream os = new StreamImplOutputStream(ts);
    
    //Deflater deflater = new Deflater(level, true);
    
    //DeflaterOutputStream out = new DeflaterOutputStream(os, deflater);
    DeflaterOutputStream out = new DeflaterOutputStream(os);

    TempBuffer temp = TempBuffer.allocate();
    byte []buf = temp.getBuffer();
    int len;

    while ((len = is.read(buf, 0, buf.length)) > 0) {
      out.write(buf, 0, len);
    }

    out.close();

    TempBuffer.free(temp);

    return new TempBufferStringValue(ts.getHead());
  }

  /**
   * 
   * @param data compressed using Deflate algorithm
   * @param length (maximum length of string returned)
   * @return uncompressed string
   */
  public Value gzinflate(Env env,
			 Value data,
                         @Optional("0") long length)
    throws DataFormatException, IOException
  {
    try {
      InputStream is = data.toInputStream();

      InflaterInputStream in = new InflaterInputStream(is);

      TempStream os = new TempStream();
    
      TempBuffer temp = TempBuffer.allocate();
      byte []buf = temp.getBuffer();
      int len;

      while ((len = in.read(buf, 0, buf.length)) > 0) {
	      os.write(buf, 0, len, false);
      }

      os.close();

      TempBuffer.free(temp);

      return new TempBufferStringValue(os.getHead());
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
  public Value gzencode(String data,
                        @Optional("-1") int level,
                        @Optional int encodingMode)
    throws DataFormatException, IOException
  {
    return gzcompress(data, level);
  }

  // @todo gzpassthru()
  // @todo gzseek()
  // @todo gztell()
  // @todo zlib_get_coding_type()
}
