/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.pdf;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.vfs.PathImpl;

/**
 * deals with an image
 */
public class EmbeddedFilePdf extends ObjectPdf {
  private PathImpl _path;
  
  private int _id;

  public EmbeddedFilePdf(PathImpl path)
    throws IOException
  {
    _path = path;
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

  /**
   * Writes the object to the stream
   */
  public void writeObject(OutPdf out)
    throws IOException
  {
    long length = _path.length();

    out.println("<< /Type /EmbeddedFile");
    out.println("   /Length " + length);
    out.println(">>");
    out.println("stream");

    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.buffer();
    int sublen;
    
    InputStream is = _path.openRead();
    
    while ((sublen = is.read(buffer, 0, buffer.length)) > 0) {
      out.write(buffer, 0, sublen);
    }
    
    out.println();
    out.println("endstream");
  }
}
