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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.awt.image.*;
import javax.imageio.*;

/**
 * PHP image
 */
public class ImageModule extends AbstractQuercusModule {
  private static final Logger log
    = Logger.getLogger(ImageModule.class.getName());
  private static final L10N L = new L10N(ImageModule.class);

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
    imageArray.put(new StringValueImpl("width=\"" + info._width +
				   "\" height=\"" + info._height + "\""));

    if (info._bits >= 0)
      imageArray.put(new StringValueImpl("bits"), new LongValue(info._bits));

    if (info._type == IMAGETYPE_JPEG)
      imageArray.put("channels", 3);

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
      if (is.read() != 'P' ||
	  is.read() != 'N' ||
	  is.read() != 'G' ||
	  is.read() != '\r' ||
	  is.read() != '\n' ||
	  is.read() != 26 ||
	  is.read() != '\n')
	return false;

      return parsePNGImageSize(is, info);
    }
    else if (ch == 'G') {
      // GIF
      if (is.read() != 'I' ||
	  is.read() != 'F' ||
	  is.read() != '8' ||
	  is.read() != '7' ||
	  is.read() != 'a')
	return false;

      return parseGIFImageSize(is, info);
    }
    else if (ch == 0xff) {
      // JPEG
      if (is.read() != 0xd8)
	return false;

      return parseJPEGImageSize(is, info);
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

  /**
   * Parses the image size from the PNG file.
   */
  private static boolean parseGIFImageSize(ReadStream is, ImageInfo info)
    throws IOException
  {
    int length;

    int width = (is.read() & 0xff) + 256 * (is.read() & 0xff);
    int height = (is.read() & 0xff) + 256 * (is.read() & 0xff);

    int flags = is.read() & 0xff;

    info._width = width;
    info._height = height;
    info._type = IMAGETYPE_GIF;

    info._bits = flags & 0x7;

    info._mime = "image/gif";

    return true;
  }

  /**
   * Parses the image size from the PNG file.
   */
  private static boolean parseJPEGImageSize(ReadStream is, ImageInfo info)
    throws IOException
  {
    int ch;

    while ((ch = is.read()) == 0xff) {
      ch = is.read();

      if (ch == 0xff) {
	is.unread();
      }
      else if (0xd0 <= ch && ch <= 0xd9) {
	// rst
      }
      else if (0x01 == ch) {
	// rst
      }
      else if (ch == 0xc0) {
	int len = 256 * is.read() + is.read();

	int bits = is.read();
	int height = 256 * is.read() + is.read();
	int width = 256 * is.read() + is.read();

	info._width = width;
	info._height = height;
	info._type = IMAGETYPE_JPEG;

	info._bits = bits;

	info._mime = "image/jpeg";

	return true;
      }
      else {
	int len = 256 * is.read() + is.read();

	is.skip(len - 2);
      }
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

  static class QuercusImage extends ResourceValue {
    private int _width;
    private int _height;
    BufferedImage _bufferedImage;

    public QuercusImage(int width, int height)
    {
      _width = width;
      _height = height;
      _bufferedImage =
	new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public QuercusImage(InputStream inputStream)
    {
      try {
	_bufferedImage = ImageIO.read(inputStream);
	_width = _bufferedImage.getWidth(null);
	_height = _bufferedImage.getHeight(null);
      }
      catch (IOException e) {
	throw new QuercusException(e);
      }
    }

    public QuercusImage(Env env, String filename)
    {
      try {
	_bufferedImage = ImageIO.read(env.getPwd().lookup(filename).openRead());
	_width = _bufferedImage.getWidth(null);
	_height = _bufferedImage.getHeight(null);
      }
      catch (IOException e) {
	throw new QuercusException(e);
      }
    }

    public String toString()
    {
      return "resource(Image)";
    }

    public int getPixel(int x, int y)
    {
      return _bufferedImage.getRGB(x, y);
    }
  }

  /** Retrieve information about the currently installed GD library */
  public static Value gd_info()
  {
    Value[] keys = new Value[] {
      StringValue.create("GD Version"), // ] => 2.0 or higher
      StringValue.create("FreeType Support"), // ] => 1
      StringValue.create("FreeType Linkage"), // ] => with freetype
      StringValue.create("T1Lib Support"), // ] => 1
      StringValue.create("GIF Read Support"), // ] => 1
      StringValue.create("GIF Create Support"), // ] => 1
      StringValue.create("JPG Support"), // ] => 1
      StringValue.create("PNG Support"), // ] => 1
      StringValue.create("WBMP Support"), // ] => 1
      StringValue.create("XPM Support"), // ] => 
      StringValue.create("XBM Support"), // ] => 
      StringValue.create("JIS-mapped Japanese Font Support"), // ] => 
    };
    Value[] vals = new Value[] {
      StringValue.create("2.0 or higher"),
      BooleanValue.TRUE,
      StringValue.create("with freetype"),
      BooleanValue.TRUE,
      BooleanValue.TRUE,
      BooleanValue.TRUE,
      BooleanValue.TRUE,
      BooleanValue.TRUE,
      BooleanValue.TRUE,
      BooleanValue.FALSE,
      BooleanValue.FALSE,
    };
    return new ArrayValueImpl(keys, vals);
  }

  /**  Get file extension for image type */
  public static Value image_type_to_extension(int imageType, boolean dot)
  {
    switch(imageType) {
      case IMAGETYPE_GIF:     return StringValue.create(dot ? ".gif" : "gif");
      case IMAGETYPE_JPG:     return StringValue.create(dot ? ".jpg" : "jpg");
      case IMAGETYPE_PNG:     return StringValue.create(dot ? ".png" : "png");
      case IMAGETYPE_SWF:     return StringValue.create(dot ? ".swf" : "swf");
      case IMAGETYPE_PSD:     return StringValue.create(dot ? ".psd" : "psd");
      case IMAGETYPE_BMP:     return StringValue.create(dot ? ".bmp" : "bmp");
      case IMAGETYPE_TIFF_II: return StringValue.create(dot ? ".tiff" : "tiff");
      case IMAGETYPE_TIFF_MM: return StringValue.create(dot ? ".tiff" : "tiff");
      case IMAGETYPE_JPC:     return StringValue.create(dot ? ".jpc" : "jpc");
      case IMAGETYPE_JP2:     return StringValue.create(dot ? ".jp2" : "jp2");
      case IMAGETYPE_JPX:     return StringValue.create(dot ? ".jpf" : "jpf");
      case IMAGETYPE_JB2:     return StringValue.create(dot ? ".jb2" : "jb2");
      case IMAGETYPE_SWC:     return StringValue.create(dot ? ".swc" : "swc");
      case IMAGETYPE_IFF:     return StringValue.create(dot ? ".iff" : "iff");
      case IMAGETYPE_WBMP:    return StringValue.create(dot ? ".wbmp" : "wbmp");
      case IMAGETYPE_XBM:     return StringValue.create(dot ? ".xbm" : "xbm");
    }
    throw new QuercusException("unknown imagetype " + imageType);
  }

  /** Get Mime-Type for image-type returned by getimagesize, exif_read_data,
   *  exif_thumbnail, exif_imagetype */
  public static Value image_type_to_mime_type(int imageType)
  {
    switch(imageType) {
      case IMAGETYPE_GIF:
	return StringValue.create("image/gif");
      case IMAGETYPE_JPG:
	return StringValue.create("image/jpeg");
      case IMAGETYPE_PNG:
	return StringValue.create("image/png");
      case IMAGETYPE_SWF:
	return StringValue.create("application/x-shockwave-flash");
      case IMAGETYPE_PSD:
	return StringValue.create("image/psd");
      case IMAGETYPE_BMP:
	return StringValue.create("image/bmp");
      case IMAGETYPE_TIFF_II:
	return StringValue.create("image/tiff");
      case IMAGETYPE_TIFF_MM:
	return StringValue.create("image/tiff");
      case IMAGETYPE_JPC:
	return StringValue.create("application/octet-stream");
      case IMAGETYPE_JP2:
	return StringValue.create("image/jp2");
      case IMAGETYPE_JPX:
	return StringValue.create("application/octet-stream");
      case IMAGETYPE_JB2:
	return StringValue.create("application/octet-stream");
      case IMAGETYPE_SWC:
	return StringValue.create("application/x-shockwave-flash");
      case IMAGETYPE_IFF:
	return StringValue.create("image/iff");
      case IMAGETYPE_WBMP:
	return StringValue.create("image/vnd.wap.wbmp");
      case IMAGETYPE_XBM:
	return StringValue.create("image/xbm");
    }
    throw new QuercusException("unknown imageType " + imageType);
  }


  /** Output a PNG image to either the browser or a file */
  public static void imagepng(Env env, QuercusImage image)
  {
    try {
      ImageIO.write(image._bufferedImage, "png", env.getOut());
    }
    catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /** Output image to browser or file */
  public static void imagejpeg(Env env, QuercusImage image)
  {
    try {
      ImageIO.write(image._bufferedImage, "jpeg", env.getOut());
    }
    catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /** Set the blending mode for an image */
  public static void imagealphablending()
  {
  }

  /**  Should antialias functions be used or not */
  public static void imageantialias()
  {
  }

  /** Draw a partial ellipse */
  public static void imagearc()
  {
  }

  /** Draw a character horizontally */
  public static void imagechar()
  {
  }

  /** Draw a character vertically */
  public static void imagecharup()
  {
  }

  /** Allocate a color for an image */
  public static Value imagecolorallocate(QuercusImage image,
					 int r, int g, int b)
  {
    return LongValue.create(( 0xff      << 24) |
			    ((r & 0xff) << 16) |
			    ((g & 0xff) <<  8) |
			    ((b & 0xff) <<  0) );
  }

  /** Allocate a color for an image */
  public static Value imagecolorallocatealpha(QuercusImage image,
					      int r, int g, int b, int a)
  {
    // don't forget: PHP alpha channel is only 7 bits!
    return LongValue.create(((a & 0xff) << 24) |
			    ((r & 0xff) << 16) |
			    ((g & 0xff) <<  8) |
			    ((b & 0xff) <<  0) );
  }

  /** De-allocate a color for an image */
  public static Value imagecolordeallocate(QuercusImage image, int rgb)
  {
    // no-op
    return BooleanValue.TRUE;
  }

  /** Get the index of the color of a pixel */
  public static Value imagecolorat(QuercusImage image, int x, int y)
  {
    return LongValue.create(image.getPixel(x, y));
  }

  /** Get the index of the closest color to the specified color */
  public static Value imagecolorclosest(QuercusImage image, int r, int g, int b)
  {
    return imagecolorallocate(image, r, g, b);
  }

  /** Get the index of the closest color to the specified color + alpha */
  public static Value imagecolorclosestalpha(QuercusImage image,
					     int r, int g, int b, int a)
  {
    return imagecolorallocatealpha(image, r, g, b, a);
  }

  /**  Get the index of the color which has the hue, white and blackness
   *   nearest to the given color */
  public static void imagecolorclosesthwb()
  {
    throw new QuercusException("imagecolorclosesthwb is not supported");
  }

  /** Get the index of the specified color */
  public static Value imagecolorexact(QuercusImage image, int r, int g, int b)
  {
    return imagecolorallocate(image, r, g, b);
  }

  /** Get the index of the specified color + alpha */
  public static Value imagecolorexactalpha(QuercusImage image,
					   int r, int g, int b, int a)
  {
    return imagecolorallocatealpha(image, r, g, b, a);
  }

  /**  Makes the colors of the palette version of an image more closely
   *   match the true color version */
  public static Value imagecolormatch(QuercusImage image1, QuercusImage image2)
  {
    // no-op
    return BooleanValue.TRUE;
  }

  /** Get the index of the specified color or its closest possible alternative*/
  public static Value imagecolorresolve(QuercusImage image, int r, int g, int b)
  {
    return imagecolorallocate(image, r, g, b);
  }

  /** Get the index of the specified color + alpha or its closest possible
   *  alternative */
  public static Value imagecolorresolvealpha(QuercusImage image,
					     int r, int g, int b, int a)
  {
    return imagecolorallocatealpha(image, r, g, b, a);
  }

  /** Set the color for the specified palette index */
  public static void imagecolorset()
  {
    throw new QuercusException("not implemented");
  }

  /** Get the colors for an index */
  public static void imagecolorsforindex()
  {
  }

  /** Find out the number of colors in an image's palette */
  public static Value imagecolorstotal()
  {
    return LongValue.create(0);
  }

  /** Define a color as transparent */
  public static void imagecolortransparent()
  {
  }

  /** Apply a 3x3 convolution matrix, using coefficient div and offset */
  public static void imageconvolution(QuercusImage image, ArrayValue matrix,
				      double div, double offset)
  {
    float[] kernelValues = new float[9];
    ArrayValue.Entry entry = matrix.getHead();
    for(int i=0; i<9; i++)
      {
	kernelValues[i] = (float)entry.getValue().toDouble();
	entry = entry.getNext();
      }
    ConvolveOp convolveOp = new ConvolveOp(new Kernel(3, 3, kernelValues));
    BufferedImage bufferedImage =
      convolveOp.filter(image._bufferedImage, null);
    // XXX: finish this
  }

  /** Copy part of an image */
  public static void imagecopy()
  {
  }

  /** Copy and merge part of an image */
  public static void imagecopymerge()
  {
  }

  /** Copy and merge part of an image with gray scale */
  public static void imagecopymergegray()
  {
  }

  /** Copy and resize part of an image with resampling */
  public static void imagecopyresampled()
  {
  }

  /** Copy and resize part of an image */
  public static void imagecopyresized()
  {
  }

  /** Create a new palette based image */
  public static Value imagecreate(int width, int height)
  {
    return new QuercusImage(width, height);
  }

  /** Create a new image from GD2 file or URL */
  public static void imagecreatefromgd2(String filename)
  {
    throw new QuercusException(".gd images are not supported");
  }

  /** Create a new image from a given part of GD2 file or URL */
  public static void imagecreatefromgd2part(String filename,
					    int srcX, int srcY,
					    int width, int height)
  {
    throw new QuercusException(".gd images are not supported");
  }

  /** Create a new image from GD file or URL */
  public static void imagecreatefromgd()
  {
    throw new QuercusException(".gd images are not supported");
  }

  /** Create a new image from file or URL */
  public static QuercusImage imagecreatefromgif(Env env, String filename)
  {
    return new QuercusImage(env, filename);
  }

  /** Create a new image from file or URL */
  public static QuercusImage imagecreatefromjpeg(Env env, String filename)
  {
    return new QuercusImage(env, filename);
  }

  /** Create a new image from file or URL */
  public static QuercusImage imagecreatefrompng(Env env, String filename)
  {
    return new QuercusImage(env, filename);
  }

  /** Create a new image from file or URL */
  public static Value imagecreatefromxbm(Env env, String filename)
  {
    return new QuercusImage(env, filename);
  }

  /** Create a new image from file or URL */
  public static QuercusImage imagecreatefromxpm(Env env, String filename)
  {
    return new QuercusImage(env, filename);
  }

  /** Create a new image from the image stream in the string */
  public static QuercusImage imagecreatefromstring(Env env, String data)
  {
    return new QuercusImage(new ByteArrayInputStream(data.getBytes()));
  }

  /** Create a new true color image */
  public static Value imagecreatetruecolor(int width, int height)
  {
    return new QuercusImage(width, height);
  }

  /** Destroy an image */
  public static void imagedestroy(QuercusImage image)
  {
    // no-op
  }

  // Shapes ///////////////////////////////////////////////////////////

  /** Draw a dashed line */
  public static void imagedashedline()
  {
  }

  /** Draw an ellipse */
  public static void imageellipse()
  {
  }

  /** Flood fill */
  public static void imagefill()
  {
  }

  /** Draw a partial ellipse and fill it */
  public static void imagefilledarc()
  {
  }

  /** Draw a filled ellipse */
  public static void imagefilledellipse()
  {
  }

  /** Draw a filled polygon */
  public static void imagefilledpolygon()
  {
  }

  /** Draw a filled rectangle */
  public static void imagefilledrectangle()
  {
  }

  /** Flood fill to specific color */
  public static void imagefilltoborder()
  {
  }

  /**  Applies a filter to an image */
  public static void imagefilter()
  {
  }

  // Text ///////////////////////////////////////////////////////////

  /** Get font height */
  public static void imagefontheight()
  {
  }

  /** Get font width */
  public static void imagefontwidth()
  {
  }

  /** Give the bounding box of a text using fonts via freetype2 */
  public static void imageftbbox()
  {
  }

  /** Write text to the image using fonts using FreeType 2 */
  public static void imagefttext()
  {
  }

  /** Apply a gamma correction to a GD image */
  public static void imagegammacorrect()
  {
  }

  // Output ///////////////////////////////////////////////////////////

  /** Output GD2 image to browser or file */
  public static void imagegd2()
  {
  }

  /** Output GD image to browser or file */
  public static void imagegd()
  {
  }

  /** Output image to browser or file */
  public static void imagegif()
  {
  }

  // Other ///////////////////////////////////////////////////////////

  /** Enable or disable interlace */
  public static void imageinterlace()
  {
  }

  /** Finds whether an image is a truecolor image */
  public static void imageistruecolor()
  {
  }

  /**  Set the alpha blending flag to use the bundled libgd layering effects */
  public static void imagelayereffect()
  {
  }

  /** Draw a line */
  public static void imageline()
  {
  }

  /** Load a new font */
  public static void imageloadfont()
  {
  }

  /** Copy the palette from one image to another */
  public static void imagepalettecopy()
  {
  }

  /** Draw a polygon */
  public static void imagepolygon()
  {
  }

  /**  Give the bounding box of a text rectangle using PostScript Type1 fonts */
  public static void imagepsbbox()
  {
  }

  /**  Make a copy of an already loaded font for further modification */
  public static void imagepscopyfont()
  {
  }

  /** Change the character encoding vector of a font */
  public static void imagepsencodefont()
  {
  }

  /** Extend or condense a font */
  public static void imagepsextendfont()
  {
  }

  /** Free memory used by a PostScript Type 1 font */
  public static void imagepsfreefont()
  {
  }

  /** Load a PostScript Type 1 font from file */
  public static void imagepsloadfont()
  {
  }

  /** Slant a font */
  public static void imagepsslantfont()
  {
  }

  /** To draw a text string over an image using PostScript Type1 fonts */
  public static void imagepstext()
  {
  }

  /** Draw a rectangle */
  public static void imagerectangle()
  {
  }

  /** Rotate an image with a given angle */
  public static void imagerotate()
  {
  }

  /** Set the flag to save full alpha channel information (as opposed to
   *  single-color transparency) when saving PNG images */
  public static void imagesavealpha()
  {
  }

  /** Set the brush image for line drawing */
  public static void imagesetbrush()
  {
  }

  /** Set a single pixel */
  public static void imagesetpixel()
  {
  }

  /** Set the style for line drawing */
  public static void imagesetstyle()
  {
  }

  /** Set the thickness for lineowser or file */
  public static void imagesetthickness()
  {
  }

  /** Embe into single tags. */
  public static void iptcembed()
  {
  }

  // stuff below is strictly for WAP

  /** Output image to browser or file */
  public static void image2wbmp()
  {
  }

  /** Create a new image from file or URL */
  public static void imagecreatefromwbmp()
  {
  }

  /** Convert JPEG image file to WBMP image file */
  public static void jpeg2wbmp()
  {
  }

  /** Convert PNG image file to WBM */
  public static void png2wbmp()
  {
  }
}

