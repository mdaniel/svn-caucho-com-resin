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

import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.Path;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.TempBufferStringValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;

/**
 * pdf object oriented API facade
 */
public class PDF {
  private static final Logger log = Logger.getLogger(PDF.class.getName());
  private static final L10N L = new L10N(PDF.class);

  private static final double KAPPA = 0.5522847498;

  private static HashMap<String,Font> _faceMap = new HashMap<String,Font>();
  
  private HashMap<PDFFont,PDFFont> _fontMap
    = new HashMap<PDFFont,PDFFont>();
  
  private HashMap<PDFProcSet,PDFProcSet> _procSetMap
    = new HashMap<PDFProcSet,PDFProcSet>();

  private TempStream _tempStream;
  private WriteStream _os;
  private PDFWriter _out;

  private int _pageId;
  
  private PDFPage _page;
  private PDFStream _stream = new PDFStream();

  public PDF(Env env)
    throws IOException
  {
    _out = new PDFWriter(env.getOut());
  }

  public boolean begin_document(String fileName, @Optional String optList)
    throws IOException
  {
    _tempStream = new TempStream();
    _tempStream.openWrite();
    _os = new WriteStream(_tempStream);
    
    _out = new PDFWriter(_os);
    _out.beginDocument();

    _pageId = _out.writeHeader();

    return true;
  }

  public boolean begin_page(double width, double height)
  {
    _page = new PDFPage(_pageId - 1, _pageId, width, height);
    _stream.begin();

    return true;
  }

  public boolean begin_page_ext(double width, double height, String opt)
  {
    return begin_page(width, height);
  }

  public boolean set_info(String key, String value)
  {
    if ("Author".equals(key)) {
      _out.setAuthor(key);
      return true;
    }
    else if ("Title".equals(key)) {
      _out.setTitle(key);
      return true;
    }
    else if ("Creator".equals(key)) {
      _out.setCreator(key);
      return true;
    }
    else
      return false;
  }

  /**
   * Returns the result as a string.
   */
  public Value get_buffer()
  {
    TempStream ts = _tempStream;
    _tempStream = null;

    if (ts == null)
      return BooleanValue.FALSE;

    return new TempBufferStringValue(ts.getHead());
  }

  /**
   * Returns the value for a parameter.
   */
  public String get_parameter(String name)
  {
    if ("fontname".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
	return font.getFontName();
      else
	return null;
    }
    else
      return null;
  }

  /**
   * Returns the value for a parameter.
   */
  public double get_value(String name)
  {
    if ("ascender".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
	return font.getAscender();
      else
	return 0;
    }
    else if ("capheight".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
	return font.getCapHeight();
      else
	return 0;
    }
    else if ("descender".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
	return font.getDescender();
      else
	return 0;
    }
    else if ("fontsize".equals(name)) {
      return _stream.getFontSize();
    }
    else
      return 0;
  }

  /**
   * Loads a font for later use.
   *
   * @param name the font name, e.g. Helvetica
   * @param encoding the font encoding, e.g. winansi
   * @param opt any options
   */
  public PDFFont load_font(String name, String encoding, String opt)
    throws IOException
  {
    Font face = loadFont(name);
    
    PDFFont font = new PDFFont(face, encoding, opt);

    PDFFont oldFont = _fontMap.get(font);

    if (oldFont != null)
      return oldFont;

    font.setId(_out.allocateId(1));

    _fontMap.put(font, font);

    _out.addPendingObject(font);

    return font;
  }

  private Font loadFont(String name)
    throws IOException
  {
    synchronized (_faceMap) {
      Font face = _faceMap.get(name);

      if (face == null) {
	face = new AfmParser().parse(name);

	_faceMap.put(name, face);
      }

      return face;
    }
  }

  /**
   * Sets the current font
   *
   * @param name the font name, e.g. Helvetica
   * @param encoding the font encoding, e.g. winansi
   * @param opt any options
   */
  public boolean setfont(@NotNull PDFFont font, double size)
    throws IOException
  {
    if (font == null)
      return false;
    
    _stream.setFont(font, size);

    _page.addResource(font.getResource());

    return true;
  }

  /**
   * Sets generic informatino.
   */
  public boolean set_info(String key, String value)
  {
    return true;
  }

  /**
   * Returns the length of a string for a font.
   */
  public double stringwidth(String string, @NotNull PDFFont font, double size)
  {
    if (font == null)
      return 0;

    return size * font.stringWidth(string) / 1000.0;
  }
  

  /**
   * Sets the text position.
   */
  public boolean set_text_pos(double x, double y)
  {
    _stream.setTextPos(x, y);

    return true;
  }

  /**
   * Fills
   */
  public boolean fill()
    throws IOException
  {
    _stream.fill();

    return true;
  }

  /**
   * Closes the path
   */
  public boolean closepath()
    throws IOException
  {
    _stream.closepath();

    return true;
  }

  /**
   * Appends the current path to the clipping path.
   */
  public boolean clip()
    throws IOException
  {
    _stream.clip();

    return true;
  }

  /**
   * Closes the path strokes
   */
  public boolean closepath_stroke()
    throws IOException
  {
    _stream.closepathStroke();

    return true;
  }

  /**
   * Closes the path strokes
   */
  public boolean closepath_fill_stroke()
    throws IOException
  {
    _stream.closepathFillStroke();

    return true;
  }

  /**
   * Fills
   */
  public boolean fill_stroke()
    throws IOException
  {
    _stream.fillStroke();

    return true;
  }

  /**
   * Ends the path
   */
  public boolean endpath()
    throws IOException
  {
    _stream.endpath();

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto(double x1, double y1,
			 double x2, double y2,
			 double x3, double y3)
    throws IOException
  {
    _stream.curveTo(x1, y1, x2, y2, x3, y3);

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto_b(double x1, double y1,
			   double x2, double y2)
    throws IOException
  {
    _stream.curveTo(x1, y1, x1, y1, x2, y2);

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto_e(double x1, double y1,
			   double x2, double y2)
    throws IOException
  {
    _stream.curveTo(x1, y1, x2, y2, x2, y2);

    return true;
  }

  /**
   * Creates a counterclockwise arg
   */
  public boolean arc(double x1, double y1, double r, double a, double b)
    throws IOException
  {
    a = a % 360;
    if (a < 0)
      a += 360;
    
    b = b % 360;
    if (b < 0)
      b += 360;
    
    if (b < a)
      b += 360;

    int aQuarter = (int) (a / 90);
    int bQuarter = (int) (b / 90);

    if (aQuarter == bQuarter) {
      clockwiseArc(x1, y1, r, a, b);
    }
    else {
      clockwiseArc(x1, y1, r, a, (aQuarter + 1) * 90);

      for (int q = aQuarter + 1; q < bQuarter; q++)
	clockwiseArc(x1, y1, r, q * 90, (q + 1) * 90);
	
      clockwiseArc(x1, y1, r, bQuarter * 90, b);
    }
    
    return true;
  }

  /**
   * Creates a clockwise arc
   */
  public boolean arcn(double x1, double y1, double r, double a, double b)
    throws IOException
  {
    a = a % 360;
    if (a < 0)
      a += 360;
    
    b = b % 360;
    if (b < 0)
      b += 360;
    
    if (a < b)
      a += 360;

    int aQuarter = (int) (a / 90);
    int bQuarter = (int) (b / 90);

    if (aQuarter == bQuarter) {
      counterClockwiseArc(x1, y1, r, a, b);
    }
    else {
      counterClockwiseArc(x1, y1, r, a, aQuarter * 90);

      for (int q = aQuarter - 1; bQuarter < q; q--)
	counterClockwiseArc(x1, y1, r, (q + 1) * 90, q * 90);
	
      counterClockwiseArc(x1, y1, r, (bQuarter + 1) * 90, b);
    }
    
    return true;
  }

  /**
   * Creates an arc from 0 to pi/2
   */
  private boolean clockwiseArc(double x, double y, double r,
			       double aDeg, double bDeg)
    throws IOException
  {
    double a = aDeg * Math.PI / 180.0;
    double b = bDeg * Math.PI / 180.0;
    
    double cos_a = Math.cos(a);
    double sin_a = Math.sin(a);
    
    double x1 = x + r * cos_a;
    double y1 = y + r * sin_a;
    
    double cos_b = Math.cos(b);
    double sin_b = Math.sin(b);
    
    double x2 = x + r * cos_b;
    double y2 = y + r * sin_b;

    double l = KAPPA * r * 2 * (b - a) / Math.PI;

    lineto(x1, y1);
    curveto(x1 - l * sin_a, y1 + l * cos_a,
	    x2 + l * sin_b, y2 - l * cos_b,
	    x2, y2);
    
    return true;
  }

  /**
   * Creates an arc from 0 to pi/2
   */
  private boolean counterClockwiseArc(double x, double y, double r,
				      double aDeg, double bDeg)
    throws IOException
  {
    double a = aDeg * Math.PI / 180.0;
    double b = bDeg * Math.PI / 180.0;
    
    double cos_a = Math.cos(a);
    double sin_a = Math.sin(a);
    
    double x1 = x + r * cos_a;
    double y1 = y + r * sin_a;
    
    double cos_b = Math.cos(b);
    double sin_b = Math.sin(b);
    
    double x2 = x + r * cos_b;
    double y2 = y + r * sin_b;

    double l = KAPPA * r * 2 * (a - b) / Math.PI;

    lineto(x1, y1);
    curveto(x1 + l * sin_a, y1 - l * cos_a,
	    x2 - l * sin_b, y2 + l * cos_b,
	    x2, y2);
    
    return true;
  }

  /**
   * Creates a circle
   */
  public boolean circle(double x1, double y1, double r)
    throws IOException
  {
    double l = r * KAPPA;
    
    moveto(x1, y1 + r);

    curveto(x1 - l, y1 + r, x1 - r, y1 + l, x1 - r, y1);
    
    curveto(x1 - r, y1 - l, x1 - l, y1 - r, x1, y1 - r);
    
    curveto(x1 + l, y1 - r, x1 + r, y1 - l, x1 + r, y1);
    
    curveto(x1 + r, y1 + l, x1 + l, y1 + r, x1, y1 + r);
    
    return true;
  }

  /**
   * Sets the graphics position.
   */
  public boolean lineto(double x, double y)
    throws IOException
  {
    _stream.lineTo(x, y);

    return true;
  }

  /**
   * Sets the graphics position.
   */
  public boolean moveto(double x, double y)
    throws IOException
  {
    _stream.moveTo(x, y);

    return true;
  }

  /**
   * Creates a rectangle
   */
  public boolean rect(double x, double y, double width, double height)
    throws IOException
  {
    _stream.rect(x, y, width, height);

    return true;
  }

  /**
   * Sets the color to a grayscale
   */
  public boolean setgray_stroke(double g)
    throws IOException
  {
    return _stream.setcolor("stroke", "gray", g, 0, 0, 0);
  }

  /**
   * Sets the color to a grayscale
   */
  public boolean setgray_fill(double g)
    throws IOException
  {
    return _stream.setcolor("fill", "gray", g, 0, 0, 0);
  }

  /**
   * Sets the color to a grayscale
   */
  public boolean setgray(double g)
    throws IOException
  {
    return _stream.setcolor("both", "gray", g, 0, 0, 0);
  }

  /**
   * Sets the color to a rgb
   */
  public boolean setrgbcolor_stroke(double r, double g, double b)
    throws IOException
  {
    return _stream.setcolor("stroke", "rgb", r, g, b, 0);
  }

  /**
   * Sets the fill color to a rgb
   */
  public boolean setrgbcolor_fill(double r, double g, double b)
    throws IOException
  {
    return _stream.setcolor("fill", "rgb", r, g, b, 0);
  }

  /**
   * Sets the color to a rgb
   */
  public boolean setrgbcolor(double r, double g, double b)
    throws IOException
  {
    return _stream.setcolor("both", "rgb", r, g, b, 0);
  }

  /**
   * Sets the color
   */
  public boolean setcolor(String fstype, String colorspace,
			  double c1, double c2, double c3, double c4)
    throws IOException
  {
    return _stream.setcolor(fstype, colorspace, c1, c2, c3, c4);
  }

  /**
   * Sets the line width
   */
  public boolean setlinewidth(double w)
    throws IOException
  {
    return _stream.setlinewidth(w);
  }

  /**
   * Concatenates the matrix
   */
  public boolean concat(double a, double b, double c,
			double d, double e, double f)
    throws IOException
  {
    return _stream.concat(a, b, c, d, e, f);
  }

  /**
   * open image
   */
  public PDFImage open_image_file(String type, Path file,
				  @Optional String stringParam,
				  @Optional int intParam)
    throws IOException
  {
    PDFImage img = new PDFImage(file);
    
    img.setId(_out.allocateId(1));

    _out.addPendingObject(img);

    return img;
  }

  public boolean fit_image(PDFImage img, double x, double y,
			   @Optional String opt)
    throws IOException
  {
    _page.addResource(img.getResource());

    _stream.save();

    concat(img.getWidth(), 0, 0, img.getHeight(), x, y);
    
    _stream.fit_image(img);

    _stream.restore();
    
    return true;
  }

  /**
   * Skews the coordinates
   *
   * @param a degrees to skew the x axis
   * @param b degrees to skew the y axis
   */
  public boolean skew(double aDeg, double bDeg)
    throws IOException
  {
    double a = aDeg * Math.PI / 180;
    double b = bDeg * Math.PI / 180;
    
    return _stream.concat(1, Math.tan(a), Math.tan(b), 1, 0, 0);
  }
  
  /**
   * scales the coordinates
   *
   * @param sx amount to scale the x axis
   * @param sy amount to scale the y axis
   */
  public boolean scale(double sx, double sy)
    throws IOException
  {
    return _stream.concat(sx, 0, 0, sy, 0, 0);
  }
  
  /**
   * translates the coordinates
   *
   * @param tx amount to translate the x axis
   * @param ty amount to translate the y axis
   */
  public boolean translate(double tx, double ty)
    throws IOException
  {
    return _stream.concat(1, 0, 0, 1, tx, ty);
  }
  
  /**
   * rotates the coordinates
   *
   * @param p amount to rotate
   */
  public boolean rotate(double pDeg)
    throws IOException
  {
    double p = pDeg * Math.PI / 180;
    
    return _stream.concat(Math.cos(p), Math.sin(p),
			  -Math.sin(p), Math.cos(p),
			  0, 0);
  }

  /**
   * Saves the graphics state.
   */
  public boolean save()
    throws IOException
  {
    return _stream.save();
  }

  /**
   * Restores the graphics state.
   */
  public boolean restore()
    throws IOException
  {
    return _stream.restore();
  }
  
  /**
   * Displays text
   */
  public void show(String text)
    throws IOException
  {
    _stream.show(text);
  }

  /**
   * Draws the graph
   */
  public boolean stroke()
    throws IOException
  {
    _stream.stroke();

    return true;
  }

  /**
   * Displays text
   */
  public boolean continue_text(String text)
    throws IOException
  {
    _stream.continue_text(text);

    return true;
  }

  public boolean end_page()
    throws IOException
  {
    _stream.flush();

    PDFProcSet procSet = _stream.getProcSet();

    _page.addResource(procSet.getResource());
    
    int streamId = _out.allocateId(1);

    _page.write(_out, streamId);

    _out.writeStream(streamId, _stream);

    return true;
  }
  
  public boolean end_page_ext(String optlist)
    throws IOException
  {
    return end_page();
  }
  
  public boolean end_document(Env env, @Optional String optList)
    throws IOException
  {
    _out.endDocument();

    _os.close();
    _out = null;

    return true;
  }

  public boolean close(Env env)
    throws IOException
  {
    return end_document(env, "");
  }

  public boolean delete()
    throws IOException
  {
    return true;
  }

  public String toString()
  {
    return "PDF[]";
  }
}
