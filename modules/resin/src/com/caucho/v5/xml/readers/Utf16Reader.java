/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.xml.readers;

import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.xml.XmlParser;

import java.io.IOException;

/**
 * A fast reader to convert bytes to characters for parsing XML.
 */
public class Utf16Reader extends XmlReader {
  boolean isReverse;
  /**
   * Create a new reader.
   */
  public Utf16Reader()
  {
  }

  /**
   * Create a new reader with the given read stream.
   */
  public Utf16Reader(XmlParser parser, ReadStream is)
  {
    super(parser, is);
  }

  public void setReverse(boolean isReverse)
  {
    this.isReverse = isReverse;
  }

  public boolean getReverse()
  {
    return isReverse;
  }

  /**
   * Read the next character, returning -1 on end of file..
   */
  public int read()
    throws IOException
  {
    int ch1 = _is.read();
    int ch2 = _is.read();

    if (ch2 < 0) {
      return -1;
    }
    else if (isReverse)
      return (ch2 << 8) + ch1;
    else
      return (ch1 << 8) + ch2;
  }
}

