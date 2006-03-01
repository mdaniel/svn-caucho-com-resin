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

package com.caucho.quercus.pdflib;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.TempBuffer;

/**
 * pdf object oriented API facade
 */
public class PDFStream {
  private static final Logger log
    = Logger.getLogger(PDFStream.class.getName());
  private static final L10N L = new L10N(PDFStream.class);

  private int _id;
  
  private TempStream _tempStream = new TempStream();
  private WriteStream _out = new WriteStream();

  private PDFProcSet _procSet;
  private PDFFont _font;
  private double _fontSize = 24;

  private boolean _inText;
  private boolean _hasFont;
  private boolean _hasTextPos = true;
  private double _textX = 0;
  private double _textY = 0;
  
  private double _x = 0;
  private double _y = 0;
  private boolean _hasGraphicsPos = true;

  PDFStream(int id)
  {
    _id = id;
    
    _tempStream = new TempStream();
    _tempStream.openWrite();
    _out.init(_tempStream);

    _procSet = new PDFProcSet();
    _procSet.add("/PDF");

    _font = null;
    _inText = false;
    _hasFont = false;
    _hasTextPos = true;
    _hasGraphicsPos = true;
  }

  public int getId()
  {
    return _id;
  }

  public void setFont(PDFFont font, double size)
  {
    _font = font;
    _fontSize = size;
    _hasFont = false;
  }

  public PDFFont getFont()
  {
    return _font;
  }

  public double getFontSize()
  {
    return _fontSize;
  }

  public void setTextPos(double x, double y)
  {
    _textX = x;
    _textY = y;
    _hasTextPos = false;
  }

  public void stroke()
    throws IOException
  {
    flushToGraph();
    
    _out.println("S");
  }

  public void closepath()
    throws IOException
  {
    flushToGraph();
    
    _out.println("h");
  }

  public void clip()
    throws IOException
  {
    flushToGraph();
    
    _out.println("W");
  }

  public void curveTo(double x1, double y1,
		      double x2, double y2,
		      double x3, double y3)
    throws IOException
  {
    flushToGraph();

    if (x1 == x2 && y1 == y2) {
      _out.println(x1 + " " + y1 + " " +
		   x3 + " " + y3 + " y");
    }
    else if (x2 == x3 && y2 == y3) {
      _out.println(x1 + " " + y1 + " " +
		   x2 + " " + y2 + " v");
    }
    else {
      _out.println(x1 + " " + y1 + " " +
		   x2 + " " + y2 + " " +
		   x3 + " " + y3 + " c");
    }

    _x = x3;
    _y = y3;
    _hasGraphicsPos = true;
  }

  public void endpath()
    throws IOException
  {
    flushToGraph();
    
    _out.println("n");
  }
  
  public void closepathStroke()
    throws IOException
  {
    flushToGraph();
    
    _out.println("s");
  }

  public void closepathFillStroke()
    throws IOException
  {
    flushToGraph();
    
    _out.println("b");
  }

  public void fill()
    throws IOException
  {
    flushToGraph();
    
    _out.println("f");
  }

  public void fillStroke()
    throws IOException
  {
    flushToGraph();
    
    _out.println("B");
  }

  public void lineTo(double x, double y)
    throws IOException
  {
    flushToGraph();

    if (x != _x || y != _y || ! _hasGraphicsPos)
      _out.println(x + " " + y + " l");

    _x = x;
    _y = y;
    _hasGraphicsPos = true;
  }

  public void rect(double x, double y, double w, double h)
    throws IOException
  {
    flushToGraph();

    _out.println(x + " " + y + " " + w + " " + h + " re");
  }

  public void moveTo(double x, double y)
    throws IOException
  {
    if (_x != x || _y != y) {
      _x = x;
      _y = y;
      _hasGraphicsPos = false;
    }
  }

  public static int STROKE = 1;
  public static int FILL = 2;
  public static int BOTH = 3;
  
  public boolean setcolor(String fstype, String colorspace,
			  double c1, double c2, double c3, double c4)
    throws IOException
  {
    flushToGraph();

    int type;
    if ("both".equals(fstype) || "fillstroke".equals(fstype))
      type = BOTH;
    else if ("fill".equals(fstype))
      type = FILL;
    else if ("stroke".equals(fstype))
      type = STROKE;
    else
      return false;
    
    if ("gray".equals(colorspace)) {
      if ((type & STROKE) != 0)
	_out.println(c1 + " G");
      if ((type & FILL) != 0)
	_out.println(c1 + " g");

      return true;
    }
    else if ("rgb".equals(colorspace)) {
      if ((type & STROKE) != 0)
	_out.println(c1 + " " + c2 + " " + c3 + " RG");
      if ((type & FILL) != 0)
	_out.println(c1 + " " + c2 + " " + c3 + " rg");

      return true;
    }
    else if ("cmyk".equals(colorspace)) {
      if ((type & STROKE) != 0)
	_out.println(c1 + " " + c2 + " " + c3 + " " + c4 + " K");
      if ((type & FILL) != 0)
	_out.println(c1 + " " + c2 + " " + c3 + " " + c4 + " k");

      return true;
    }
    else {
      // spot, pattern, iccbasedgray, iccbasedrgb, iccbasedcmyk, lab
      
      return false;
    }
  }
  
  public void setDash(double b, double w)
    throws IOException
  {
    _out.println("[" + b + " " + w + "] 0 d");
  }
  
  public boolean setlinewidth(double w)
    throws IOException
  {
    _out.println(w + " w");

    return true;
  }

  /**
   * Saves the graphics state
   */
  public boolean save()
    throws IOException
  {
    _out.println("q");

    return true;
  }

  /**
   * Restores the graphics state
   */
  public boolean restore()
    throws IOException
  {
    _out.println("Q");

    return true;
  }
  
  public boolean concat(double a, double b, double c,
			double d, double e, double f)
    throws IOException
  {
    _out.println(String.format("%.4f %.4f %.4f %.4f %.4f %.4f cm",
			       a, b, c, d, e, f));

    return true;
  }

  public void show(String text)
    throws IOException
  {
    _procSet.add("/Text");
    
    if (! _inText) {
      _out.println("BT");
      _inText = true;
    }

    if (! _hasFont && _font != null) {
      _out.println("/" + _font.getPDFName() + " " + _fontSize + " Tf");
      _hasFont = true;
    }

    if (! _hasTextPos) {
      _out.println(_textX + " " + _textY + " Td");
      _hasTextPos = true;
    }

    _out.print("(");
    _out.print(text);
    _out.println(") Tj");
  }

  public void continue_text(String text)
    throws IOException
  {
    _out.print("(");
    _out.print(text);
    _out.println(") T*");
  }

  public boolean fit_image(PDFImage img)
    throws IOException
  {
    _procSet.add("/ImageB");
    _procSet.add("/ImageC");
    _procSet.add("/ImageI");
    
    _out.println("/I" + img.getId() + " Do");

    return true;
  }

  public void flushToGraph()
    throws IOException
  {
    flush();
    
    if (! _hasGraphicsPos) {
      _out.println(_x + " " + _y + " m");
      _hasGraphicsPos = true;
    }
  }

  public void flush()
    throws IOException
  {
    if (_inText) {
      _out.println("ET");
      _inText = false;
    }
    
    _out.flush();
  }

  public PDFProcSet getProcSet()
  {
    return _procSet;
  }

  public int getLength()
    throws IOException
  {
    _out.flush();
    
    return _tempStream.getLength();
  }

  public void write(PDFWriter out)
    throws IOException
  {
    out.writeStream(getId(), this);
  }
	   
  public void writeToStream(WriteStream os)
    throws IOException
  {
    for (TempBuffer head = _tempStream.getHead();
	 head != null;
	 head = head.getNext()) {
      os.write(head.getBuffer(), 0, head.getLength());
    }

    TempBuffer.freeAll(_tempStream.getHead());
  }
}
