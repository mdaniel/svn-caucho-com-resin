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

package com.caucho.quercus.pdflib;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Path;

/**
 * deals with an image
 */
public class PDFImage {
  private static final Logger log
    = Logger.getLogger(PDFImage.class.getName());
  private static final L10N L = new L10N(PDFImage.class);

  private ReadStream _is;
  
  public PDFImage(Path path)
    throws IOException
  {
    _is = path.openRead();

    try {
      parseImage();
    } finally {
      _is.close();
    }
  }

  private boolean parseImage()
    throws IOException
  {
    int ch = _is.read();

    if (ch == 'G') {
      if (_is.read() != 'I' ||
	  _is.read() != 'F' ||
	  _is.read() != '8' ||
	  _is.read() != '7' ||
	  _is.read() != 'a')
	return false;

      return parseGIF();
    }

    return false;
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
    System.out.println("CH: " + (char) ch + " " + ch);
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
