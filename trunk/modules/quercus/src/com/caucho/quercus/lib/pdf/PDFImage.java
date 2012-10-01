/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.pdf;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * deals with an image
 */
public class PDFImage extends PDFObject {
  private static final Logger log
    = Logger.getLogger(PDFImage.class.getName());
  private static final L10N L = new L10N(PDFImage.class);

  private Path _path;
  private ReadStream _is;

  private BufferedImage _image;
  
  private int _id;

  private String _type;
  private int _width;
  private int _height;
  private int _bits;
  private TempBuffer _jpegHead;

  public PDFImage(Path path)
    throws IOException
  {
    _path = path;
    _is = path.openRead();

    try {
      parseImage();
    } finally {
      _is.close();
    }
  }

  /**
   * Returns the object id.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Sets the object id.
   */
  public void setId(int id)
  {
    _id = id;
  }

  public double get_width()
  {
    return _width;
  }

  public double get_height()
  {
    return _height;
  }

  private boolean parseImage()
    throws IOException
  {
    _image = ImageIO.read(_is);
    
    _width = _image.getWidth();
    _height = _image.getHeight();

    TempStream ts = new TempStream();
    WriteStream os = new WriteStream(ts);
    
    try {
      ImageIO.write(_image, "jpeg", os);
    } finally {
      os.close();
    }
    
    /*
    os = Vfs.openWrite("file:/tmp/caucho/qa/test.jpg");
    try {
      ImageIO.write(_image, "jpeg", os);
    } finally {
      os.close();
    }
    
    os = Vfs.openWrite("file:/tmp/caucho/qa/test.png");
    try {
      ImageIO.write(_image, "png", os);
    } finally {
      os.close();
    }
    */

    return parseImageJpeg(ts.openRead());
    
  }

  private boolean parseImageJpeg(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0xff)
      return false;
    
    if (is.read() != 0xd8)
      return false;

      TempStream ts = new TempStream();

      WriteStream ws = new WriteStream(ts);
      ws.write(0xff);
      ws.write(0xd8);
      is.writeToStream(ws);
      ws.close();

      // XXX: issues with _jpegHead vs ts.openReadAndSaveBuffer()
      _jpegHead = ts.getHead();
      is.close();

      _is = new ReadStream();
      ts.openRead(_is);

      parseJPEG();

      return true;
  }

  private boolean parseGIF()
    throws IOException
  {
    int width = (_is.read() & 0xff) + 256 * (_is.read() & 0xff);
    int heigth = (_is.read() & 0xff) + 256 * (_is.read() & 0xff);
    int flags = _is.read();
    int background = _is.read();
    int pad = _is.read();

    int depth = (flags & 0x7) + 1;

    int []colorMap = null;

    if ((flags & 0x80) != 0) {
      colorMap = parseGIFColorMap(depth);
    }
    else {
      System.out.println("GIF: can't cope with local");
      return false;
    }

    int ch = _is.read();
    if (ch != ',')
      return false;

    int imgLeft = (_is.read() & 0xff) + 256 * (_is.read() & 0xff);
    int imgTop = (_is.read() & 0xff) + 256 * (_is.read() & 0xff);
    int imgWidth = (_is.read() & 0xff) + 256 * (_is.read() & 0xff);
    int imgHeight = (_is.read() & 0xff) + 256 * (_is.read() & 0xff);
    flags = _is.read() & 0xff;

    if ((flags & 0x80) != 0) {
      System.out.println("GIF: can't cope with local");
      return false;
    }
    if ((flags & 0x40) != 0) {
      System.out.println("GIF: can't cope with interlaced");
      return false;
    }

    parseGIFData(colorMap);

    return false;
  }

  private int []parseGIFColorMap(int depth)
    throws IOException
  {
    int []values = new int[1 << depth];

    for (int i = 0; i < values.length; i++) {
      int value = (0x10000 * (_is.read() & 0xff) +
                   0x100 * (_is.read() & 0xff) +
                   0x1 * (_is.read() & 0xff));

      values[i] = value;
    }

    return values;
  }

  private void parseGIFData(int []colorMap)
    throws IOException
  {
    /*
    System.out.println("CS: " + codeSize);

    int []strings = new int[4096];

    for (int i = 0; i < clearCode; i++)
      strings[i] = i;

    while ((blockCount = _is.read()) > 0) {
      int offset = 0;
      int prev = 0;

      for (int i = 0; i < blockCount; i++) {
        int data = _is.read();

        if (i == 0) {
          System.out.println("C: " + data);
          System.out.println("C: " + (data & _codeMask));
        }
      }
    }
    */
  }

  private boolean parseJPEG()
    throws IOException
  {
    if (_is.read() != 0xff ||
        _is.read() != 0xd8)
      return false;

    int ch;

    while ((ch = _is.read()) == 0xff) {
      ch = _is.read();

      if (ch == 0xff) {
        _is.unread();
      }
      else if (0xd0 <= ch && ch <= 0xd9) {
        // rst
      }
      else if (0x01 == ch) {
        // rst
      }
      else if (ch == 0xc0) {
        int len = 256 * _is.read() + _is.read();

        _bits = _is.read();
        _height = 256 * _is.read() + _is.read();
        _width = 256 * _is.read() + _is.read();
        _type = "jpeg";

        return true;
      }
      else {
        int len = 256 * _is.read() + _is.read();

        _is.skip(len - 2);
      }
    }

    return false;
  }

  String getResourceName()
  {
    return "/XObject";
  }
  
  String getResource()
  {
    return ("<< /I" + _id + " " + _id + " 0 R >>");
  }

  /**
   * Writes the object to the stream
   */
  public void writeObjectNew(PDFWriter out)
    throws IOException
  {
    long length = _path.getLength();

    out.println("<< /Type /XObject");
    out.println("   /Subtype /Image");
    out.println("   /Width " + _width);
    out.println("   /Height " + _height);
    out.println("   /ColorSpace /DeviceRGB");
    out.println("   /BitsPerComponent " + _bits);
    // out.println("   /Filter /DCTDecode");
    out.println("   /Length " + length);
    out.println(">>");
    out.println("stream");

    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();
    int sublen;
    
    InputStream is = _path.openRead();
    
    while ((sublen = is.read(buffer, 0, buffer.length)) > 0) {
      out.write(buffer, 0, sublen);
    }
    
    out.println();
    out.println("endstream");
  }

  public void writeObject(PDFWriter out)
    throws IOException
  {
    int length = 0;

    for (TempBuffer ptr = _jpegHead; ptr != null; ptr = ptr.getNext())
      length += ptr.getLength();

    out.println("<< /Type /XObject");
    out.println("   /Subtype /Image");
    out.println("   /Width " + _width);
    out.println("   /Height " + _height);
    out.println("   /ColorSpace /DeviceRGB");
    out.println("   /BitsPerComponent " + _bits);
    out.println("   /Filter /DCTDecode");
    out.println("   /Length " + length);
    out.println(">>");
    out.println("stream");

    for (TempBuffer ptr = _jpegHead; ptr != null; ptr = ptr.getNext()) {
      out.write(ptr.getBuffer(), 0, ptr.getLength());
    }
    out.println();
    out.println("endstream");
  }

  static class GIFDecode {
    private final int _codeSize;
    private final int _clearCode;
    private final int _endOfCode;

    private ReadStream _is;
    private int _blockSize;

    GIFDecode(ReadStream is)
      throws IOException
    {
      _is = is;

      _codeSize = _is.read();
      _clearCode = 1 << _codeSize;
      _endOfCode = _clearCode + 1;
    }

    int readByte()
      throws IOException
    {
      if (_blockSize < 0)
        return -1;
      else if (_blockSize == 0) {
        _blockSize = _is.read();

        if (_blockSize == 0) {
          _blockSize = -1;
          return -1;
        }
      }

      _blockSize--;

      return _is.read();
    }
  }
}
