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

import java.io.IOException;
import java.io.InputStream;

import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;

/**
 * deals with an image
 */
public class PDFFileImage extends PDFObject {
  private int _id;
  
  private int _refId;
  private double _width;
  private double _height;

  public PDFFileImage(int refId, double width, double height)
    throws IOException
  {
    _refId = refId;
    _width = width;
    _height = height;
  }

  /**
   * Returns the object id.
   */
  @Override
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
  @Override
  public void writeObject(PDFWriter out)
    throws IOException
  {
    out.println("<< /Type /XObject");
    out.println("   /Subtype /Image");
    out.println("   /BitsPerComponent 8");
    out.println("   /ColorSpace /DeviceRGB");
    out.println("   /Width " + _width);
    out.println("   /Height " + _height);
    out.println("   /F " + _refId + " 0 R");
    out.println("   /Length " + 0);
    out.println(">>");
    out.println("stream");
    out.println("endstream");
  }
}
