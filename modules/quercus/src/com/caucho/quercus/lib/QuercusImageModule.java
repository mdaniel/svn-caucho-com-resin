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

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;

import com.caucho.quercus.env.*;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

/**
 * PHP image
 */
public class QuercusImageModule extends AbstractQuercusModule {
  private static final Logger log
    = Logger.getLogger(QuercusImageModule.class.getName());
  private static final L10N L = new L10N(QuercusImageModule.class);

  public static final int IMAGETYPE_GIF = 1;
  public static final int IMAGETYPE_JPG = 2;
  public static final int IMAGETYPE_JPEG = 2;
  public static final int IMAGETYPE_PNG = 3;
  public static final int IMAGETYPE_SWF = 4;
  public static final int IMAGETYPE_PSD = 5;
  public static final int IMAGETYPE_BMP = 6;
  public static final int IMAGETYPE_TIFF_II = 7;
  public static final int IMAGETYPE_TIFF_MM = 8;
  public static final int IMAGETYPE_JPC = 9;
  public static final int IMAGETYPE_JP2 = 10;
  public static final int IMAGETYPE_JPX = 11;
  public static final int IMAGETYPE_JB2 = 12;
  public static final int IMAGETYPE_SWC = 13;
  public static final int IMAGETYPE_IFF = 14;
  public static final int IMAGETYPE_WBMP = 15;
  public static final int IMAGETYPE_XBM = 16;

  private static final int PNG_IHDR = pngCode("IHDR");

  /**
   * Returns the environment value.
   */
  public Value getimagesize(Env env,
			    Path file,
			    @Optional ArrayValue imageArray)
  {
    if (! file.canRead())
      return BooleanValue.FALSE;

    ImageInfo info = new ImageInfo();
    
    ReadStream is = null;
    
    try {
      is = file.openRead();

      if (! parseImageSize(is, info))
	return BooleanValue.FALSE;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    } finally {
      try { is.close(); } catch (IOException e) {}
    }

    if (imageArray == null)
      imageArray = new ArrayValueImpl();

    imageArray.put(new LongValue(info._width));
    imageArray.put(new LongValue(info._height));
    imageArray.put(new LongValue(info._type));
    imageArray.put(new StringValue("width=\"" + info._width +
				   "\" height=\"" + info._height + "\""));

    if (info._bits >= 0)
      imageArray.put(new StringValue("bits"), new LongValue(info._bits));

    if (info._mime != null)
      imageArray.put("mime", info._mime);
    
    return imageArray;
  }

  /**
   * Parses the image size from the file.
   */
  private static boolean parseImageSize(ReadStream is, ImageInfo info)
    throws IOException
  {
    int ch;

    ch = is.read();

    if (ch == 137) {
      // PNG - http://www.libpng.org/pub/png/spec/iso/index-object.html
      if (is.read() != 80 ||
	  is.read() != 78 ||
	  is.read() != 71 ||
	  is.read() != 13 ||
	  is.read() != 10 ||
	  is.read() != 26 ||
	  is.read() != 10)
	return false;

      return parsePNGImageSize(is, info);
    }
    else
      return false;
  }

  /**
   * Parses the image size from the PNG file.
   */
  private static boolean parsePNGImageSize(ReadStream is, ImageInfo info)
    throws IOException
  {
    int length;

    while ((length = readInt(is)) > 0) {
      int type = readInt(is);

      if (type == PNG_IHDR) {
	int width = readInt(is);
	int height = readInt(is);
	int depth = is.read() & 0xff;
	int color = is.read() & 0xff;
	int compression = is.read() & 0xff;
	int filter = is.read() & 0xff;
	int interlace = is.read() & 0xff;

	info._width = width;
	info._height = height;
	info._type = IMAGETYPE_PNG;
	
	info._bits = depth;

	info._mime = "image/png";

	return true;
      }
      else {
	for (int i = 0; i < length; i++) {
	  if (is.read() < 0)
	    return false;
	}
      }

      int crc = readInt(is);
    }
    
    return false;
  }

  private static int pngCode(String code)
  {
    return ((code.charAt(0) << 24) |
	    (code.charAt(1) << 16) |
	    (code.charAt(2) << 8) |
	    (code.charAt(3)));
  }

  private static int readInt(ReadStream is)
    throws IOException
  {
    return (((is.read() & 0xff) << 24) |
	    ((is.read() & 0xff) << 16) |
	    ((is.read() & 0xff) << 8) |
	    ((is.read() & 0xff)));
  }

  static class ImageInfo {
    int _width;
    int _height;
    int _type;
    
    int _bits;

    String _mime;
  }
}

