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
import java.util.logging.Logger;

import com.caucho.v5.util.L10N;

/**
 * font builder.
 */
public class FontPdf extends ObjectPdf {
  private static final Logger log
    = Logger.getLogger(FontPdf.class.getName());
  private static final L10N L = new L10N(FontPdf.class);

  private int _id;

  private final FontFacePdf _face;

  private final String _encoding;

  FontPdf(FontFacePdf face, String encoding)
  {
    _face = face;
    _encoding = encoding;
  }

  void setId(int id)
  {
    _id = id;
  }

  public int getId()
  {
    return _id;
  }

  public String getFontName()
  {
    return _face.getFontName();
  }

  public String getFontStyle()
  {
    return _face.getWeight();
  }

  public double getAscender()
  {
    return _face.getAscender();
  }

  public double getCapHeight()
  {
    return _face.getCapHeight();
  }
  
  public double getXHeight()
  {
    return _face.getXHeight();
  }

  public double getDescender()
  {
    return _face.getDescender();
  }

  public double stringWidth(String text)
  {
    return _face.stringWidth(text);
  }
  
  public double getAvgCharWidth()
  {
    return _face.getAvgCharWidth();
  }

  public double getMaxCharWidth()
  {
    return _face.getMaxCharWidth();
  }

  public String getPDFName()
  {
    return "F" + _id;
  }

  String getResourceName()
  {
    return "/Font";
    
  }
  
  String getResource()
  {
    return "<< /F" + _id + " " + _id + " 0 R >>";
  }

  @Override
  public void writeObject(OutPdf out)
    throws IOException
  {
    out.println("<< /Type /Font");
    out.println("   /Subtype /Type1");
    out.println("   /BaseFont /" + _face.getFontName());
    out.println("   /Encoding /MacRomanEncoding");
    out.println(">>");
  }

  public int hashCode()
  {
    int hash = 37;

    hash = 65521 * hash + _face.hashCode();
    hash = 65521 * hash + _encoding.hashCode();

    return hash;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof FontPdf))
      return false;

    FontPdf font = (FontPdf) o;

    return (_face == font._face &&
            _encoding.equals(font._encoding));
  }

  public String toString()
  {
    return "PDFFont[" + _face.getFontName() + "," + _encoding + "]";
  }
}
