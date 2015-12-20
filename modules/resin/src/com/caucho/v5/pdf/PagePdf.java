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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * pdf object oriented API facade
 */
public class PagePdf implements AutoCloseable
{
  private static final Logger log
    = Logger.getLogger(PagePdf.class.getName());
  private static final L10N L = new L10N(PagePdf.class);

  private static final double KAPPA = 0.5522847498;

  private int _parent;
  private int _id;

  private StreamPdf _stream;

  private double _width;
  private double _height;

  private HashMap<String,String> _resources = new HashMap<String,String>();
  private PdfDocument _pdf;

  PagePdf(PdfDocument pdf, OutPdf out, int parent, double width, double height)
  {
    _pdf = pdf;
    _parent = parent;
    _id = out.allocateId(1);
    _width = width;
    _height = height;
    _stream = new StreamPdf(out.allocateId(1));
  }

  /**
   * Returns the id.
   */
  public int getId()
  {
    return _id;
  }
  
  public double getWidth()
  {
    return _width;
  }
  
  public double getHeight()
  {
    return _height;
  }

  /**
   * Returns the stream.
   */
  public StreamPdf getStream()
  {
    return _stream;
  }
  
  public FontPdf getFont()
  {
    return _stream.getFont();
  }
  
  public double getFontSize()
  {
    return _stream.getFontSize();
  }

  public double getFontHeight()
  {
    FontPdf font = _stream.getFont();
    
    double charHeight = font.getCapHeight() + font.getAscender() + font.getDescender();
    
    return getFontSize() * charHeight / 1000.0;
  }

  public double getFontDescender()
  {
    FontPdf font = _stream.getFont();
    
    double height = font.getDescender();
    
    return getFontSize() * height / 1000.0;
  }

  public double getFontAscender()
  {
    FontPdf font = _stream.getFont();
    
    double height = font.getAscender();
    
    return getFontSize() * height / 1000.0;
  }

  /**
   * Creates a counterclockwise arg
   * 
   * @param x1 center x
   * @param y1 center y
   * @param r radius
   * @param a degree start
   * @param b degree end
   */
  public PagePdf arc(double x1, double y1, double r, double a, double b)
  {
    arcImpl(x1, y1, r, a, b, true);
    
    return this;
  }
  
  public PagePdf arcto(double x1, double y1, double r, double a, double b)
  {
    arcImpl(x1, y1, r, a, b, false);
    
    return this;
  }
  
  private void arcImpl(double x1, double y1, double r, double a, double b, boolean isMove)
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
      clockwiseArc(x1, y1, r, a, b, isMove);
    }
    else {
      clockwiseArc(x1, y1, r, a, (aQuarter + 1) * 90, isMove);

      for (int q = aQuarter + 1; q < bQuarter; q++) {
        clockwiseArc(x1, y1, r, q * 90, (q + 1) * 90, false);
      }

      clockwiseArc(x1, y1, r, bQuarter * 90, b, false);
    }
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto(double x1, double y1,
                         double x2, double y2,
                         double x3, double y3)
  {
    _stream.curveTo(x1, y1, x2, y2, x3, y3);

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto_b(double x1, double y1,
                           double x2, double y2)
  {
    _stream.curveTo(x1, y1, x1, y1, x2, y2);

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto_e(double x1, double y1,
                           double x2, double y2)
  {
    _stream.curveTo(x1, y1, x2, y2, x2, y2);

    return true;
  }
  
  public PagePdf movetoArc(double x, double y, double r, double deg)
  {
    double a = deg * Math.PI / 180.0;

    double cos_a = Math.cos(a);
    double sin_a = Math.sin(a);

    double x1 = x + r * cos_a;
    double y1 = y + r * sin_a;

    moveto(x1, y1);
    
    return this;
  }
  
  public PagePdf linetoArc(double x, double y, double r, double deg)
  {
    double a = deg * Math.PI / 180.0;

    double cos_a = Math.cos(a);
    double sin_a = Math.sin(a);

    double x1 = x + r * cos_a;
    double y1 = y + r * sin_a;

    lineto(x1, y1);
    
    return this;
  }

  /**
   * Creates an arc from 0 to pi/2
   */
  private boolean clockwiseArc(double x, double y, double r,
                               double aDeg, double bDeg,
                               boolean isMove)
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

    // lineto(x1, y1);
    if (isMove) {
      moveto(x1, y1);
    }
    else {
      lineto(x1, y1);
    }
    
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
   * gray color assignment for lines and fill
   */
  public PagePdf gray(double g)
  {
    _stream.setcolor("both", "gray", g, 0, 0, 0);
    
    return this;
  }
  
  /**
   * gray color assignment for lines
   */
  public PagePdf grayStroke(double g)
  {
    _stream.setcolor("stroke", "gray", g, 0, 0, 0);
    
    return this;
  }
  
  /**
   * gray color assignment for fill
   */
  public PagePdf grayFill(double g)
  {
    _stream.setcolor("fill", "gray", g, 0, 0, 0);
    
    return this;
  }

  /**
   * Concatenates the matrix
   */
  public PagePdf concat(double a, double b, double c,
                        double d, double e, double f)
  {
    _stream.concat(a, b, c, d, e, f);
    
    return this;
  }

  /**
   * Sets the color
   */
  public PagePdf color(String colorspace,
                       double c1,
                       double c2,
                       double c3,
                       double c4)
  {
    _stream.setcolor("both", colorspace, c1, c2, c3, c4);
    
    return this;
  }

  /**
   * Sets the color
   */
  public PagePdf colorStroke(String colorspace,
                       double c1,
                       double c2,
                       double c3,
                       double c4)
  {
    _stream.setcolor("stroke", colorspace, c1, c2, c3, c4);
    
    return this;
  }

  /**
   * Sets the color
   */
  public PagePdf colorFill(String colorspace,
                       double c1,
                       double c2,
                       double c3,
                       double c4)
  {
    _stream.setcolor("fill", colorspace, c1, c2, c3, c4);
    
    return this;
  }

  /**
   * CMYK color for both lines and fill
   */
  public PagePdf cmyk(double c, double m, double y, double k)
  {
    _stream.setcolor("both", "DeviceCMYK", c, m, y, k);
    
    return this;
  }

  /**
   * CMYK color for lines
   */
  public PagePdf cmykStroke(double c, double m, double y, double k)
  {
    _stream.setcolor("stroke", "cmyk", c, m, y, k);
    
    return this;
  }

  /**
   * CMYK color for fill
   */
  public PagePdf cmykFill(double c, double m, double y, double k)
  {
    _stream.setcolor("fill", "cmyk", c, m, y, k);
    
    return this;
  }

  /**
   * Sets the dashing
   *
   * @param b black length
   * @param w which length
   */
  public PagePdf dash(double b, double w)
  {
    _stream.setDash(b, w);

    return this;
  }

  /**
   * Removes dashing
   */
  public PagePdf solid()
  {
    _stream.setSolid();

    return this;
  }

  /**
   * rgb color for both lines and fill
   */
  public PagePdf rgb(double r, double g, double b)
  {
    _stream.setcolor("both", "rgb", r, g, b, 0);
    
    return this;
  }

  /**
   * rgb color for lines
   */
  public PagePdf rgbStroke(double r, double g, double b)
  {
    _stream.setcolor("stroke", "rgb", r, g, b, 0);
    
    return this;
  }

  /**
   * rgb color for fill
   */
  public PagePdf rgbFill(double r, double g, double b)
  {
    _stream.setcolor("fill", "rgb", r, g, b, 0);
    
    return this;
  }

  /**
   * HSB color for both lines and fill
   */
  public PagePdf hsb(double h, double s, double b)
  {
    Color color = Color.getHSBColor((float) h, (float) s, (float) b);
    
    float []comp = new float[3];
    
    color.getRGBColorComponents(comp);
    
    _stream.setcolor("both", "rgb", comp[0], comp[1], comp[2], 0);
    
    return this;
  }

  /**
   * HSB color for lines
   */
  public PagePdf hsbStroke(double h, double s, double b)
  {
    Color color = Color.getHSBColor((float) h, (float) s, (float) b);
    
    float []comp = new float[3];
    
    color.getRGBColorComponents(comp);
    
    _stream.setcolor("stroke", "rgb", comp[0], comp[1], comp[2], 0);
    
    return this;
  }

  /**
   * HSB color for fill
   */
  public PagePdf hsbFill(double h, double s, double b)
  {
    Color color = Color.getHSBColor((float) h, (float) s, (float) b);
    
    float []comp = new float[3];
    
    color.getRGBColorComponents(comp);
    
    _stream.setcolor("fill", "rgb", comp[0], comp[1], comp[2], 0);
    
    return this;
  }

  /**
   * font: sets the current font
   *
   * @param size size in points of the text.
   * @param fontName name of the font typeface
   * @param encoding character encoding
   */
  public PagePdf font(double size, String fontName)
  {
    return font(size, fontName, "");
  }

  /**
   * font: sets the current font
   *
   * @param size size in points of the text.
   * @param fontName name of the font typeface
   * @param encoding character encoding
   */
  public PagePdf font(double size, String fontName, String encoding)
  {
    FontPdf font = _pdf.font(fontName, encoding);
    
    Objects.requireNonNull(font);

    _stream.setFont(font, size);

    addResource(font.getResourceName(), font.getResource());
    
    return this;
  }
  
  public FontPdf fontLoad(String fontName, String encoding)
  {
    return _pdf.font(fontName, encoding);
  }
  
  public FontPdf fontLoad(String fontName)
  {
    return fontLoad(fontName, "");
  }

  /**
   * fontSize: sets the current font size without changing the typeface.
   *
   * @param size size in points of the text.
   */
  public PagePdf fontSize(double size)
    throws IOException
  {
    FontPdf font = _stream.getFont();
    
    _stream.setFont(font,  size);

    return this;
  }

  public ImagePdf imageLoad(Path path)
    throws IOException
  {
    return _pdf.load_image(path);
  }

  public PagePdf image(Path path, double x, double y)
    throws IOException
  {
    ImagePdf img = _pdf.load_image(path);
    
    addResource(img.getResourceName(), img.getResource());

    _stream.save();

    concat(img.getWidth(), 0, 0, img.getHeight(), x, y);

    _stream.fit_image(img);

    _stream.restore();

    return this;
  }

  /**
   * open image
   */
  public PagePdf embed(Path path,
                       double x, double y,
                       double width, double height)
    throws IOException
  {
    ImageFilePdf img = _pdf.embedLoad(path, width, height);

    addResource(img.getResourceName(), img.getResource());

    _stream.save();

    // concat(img.get_width(), 0, 0, img.get_height(), x, y);
    concat(width, 0, 0, height, x, y);

    _stream.fit_file_image(img);

    _stream.restore();

    return this;
  }

  /**
   * Sets the line width
   */
  public PagePdf linewidth(double w)
  {
    _stream.setlinewidth(w);
    
    return this;
  }

  /**
   * lineto: draws a line to a new position
   */
  public PagePdf lineto(double x, double y)
  {
    _stream.lineTo(x, y);

    return this;
  }

  /**
   * moveto: moves to a new graphics position.
   */
  public PagePdf moveto(double x, double y)
  {
    _stream.moveTo(x, y);

    return this;
  }
  
  public PagePdf outline(String title)
  {
    _pdf.add_page_to_outline(title, getHeight());
    
    return this;
  }
  
  public PagePdf outline(String title, String parent)
  {
    _pdf.addOutlinePage(title, getHeight(), parent);
    
    return this;
  }
  
  public PagePdf outline(String title, String parent, double pos)
  {
    _pdf.addOutlinePage(title, pos, parent);
    
    return this;
  }


  /**
   * Creates a rectangle
   */
  public PagePdf rect(double x, double y, double width, double height)
  {
    _stream.rect(x, y, width, height);

    return this;
  }

  /**
   * Restores the graphics state.
   */
  public PagePdf restore()
  {
    _stream.restore();
    
    return this;
  }

  /**
   * Saves the graphics state.
   */
  public PagePdf save()
  {
    _stream.save();
    
    return this;
  }

  /**
   * Skews the coordinates
   *
   * @param aDeg degrees to skew the x axis
   * @param bDeg degrees to skew the y axis
   */
  public PagePdf skew(double aDeg, double bDeg)
  {
    double a = aDeg * Math.PI / 180;
    double b = bDeg * Math.PI / 180;

    _stream.concat(1, Math.tan(a), Math.tan(b), 1, 0, 0);
    
    return this;
  }

  /**
   * scales the coordinates
   *
   * @param sx amount to scale the x axis
   * @param sy amount to scale the y axis
   */
  public PagePdf scale(double sx, double sy)
  {
    _stream.concat(sx, 0, 0, sy, 0, 0);
    
    return this;
  }

  /**
   * translates the coordinates
   *
   * @param tx amount to translate the x axis
   * @param ty amount to translate the y axis
   */
  public PagePdf translate(double tx, double ty)
  {
    _stream.concat(1, 0, 0, 1, tx, ty);
    
    return this;
  }

  /**
   * rotates the coordinates
   *
   * @param p amount to rotate in degrees counterclockwise
   */
  public PagePdf rotate(double pDeg)
  {
    double p = pDeg * Math.PI / 180;

    _stream.concat(Math.cos(p), Math.sin(p),
                   -Math.sin(p), Math.cos(p),
                   0, 0);
    
    return this;
  }

  /**
   * Fills
   */
  public PagePdf fill()
  {
    _stream.fill();

    return this;
  }

  /**
   * Closes the path
   */
  public PagePdf closepath()
  {
    _stream.closepath();

    return this;
  }

  /**
   * Appends the current path to the clipping path.
   */
  public PagePdf clip()
  {
    _stream.clip();

    return this;
  }

  /**
   * Closes the path strokes
   */
  public PagePdf closepathStroke()
  {
    _stream.closepathStroke();

    return this;
  }

  /**
   * Closes the path strokes
   */
  public PagePdf closepathFillStroke()
  {
    _stream.closepathFillStroke();

    return this;
  }

  /**
   * Fills and lines
   */
  public PagePdf fillStroke()
  {
    _stream.fillStroke();

    return this;
  }

  /**
   * Ends the path
   */
  public PagePdf endpath()
  {
    _stream.endpath();

    return this;
  }

  /**
   * posText: position of the text cursor
   */
  public PagePdf posText(double x, double y)
  {
    _stream.setTextPos(x, y);

    return this;
  }

  /**
   * stroke: draws the current line graph
   */
  public PagePdf stroke()
  {
    _stream.stroke();

    return this;
  }

  /**
   * text: text display
   */
  public PagePdf text(String text)
  {
    _stream.show(text);

    return this;
  }

  /**
   * textCont: continuation text, set after the previous text
   */
  public PagePdf textCont(String text)
  {
    _stream.continue_text(text);

    return this;
  }
  
  /**
   * Returns the length of a string for a font.
   */
  public double textWidth(String string)
  {
    FontPdf font = _stream.getFont();
    
    if (font == null) {
      return 0;
    }

    return _stream.getFontSize() * font.stringWidth(string) / 1000.0;
  }
  
  /**
   * Returns the length of a string for a font.
   */
  public double textWidth(String string, double size)
  {
    FontPdf font = _stream.getFont();
    
    if (font == null) {
      return 0;
    }

    return size * font.stringWidth(string) / 1000.0;
  }

  /**
   * Returns the length of a string for a font.
   */
  public double textWidth(String string, double size, String fontName)
  {
    FontPdf font = fontLoad(fontName);
    
    if (font == null) {
      return 0;
    }

    return size * font.stringWidth(string) / 1000.0;
  }

  public double stringheight(String string, FontPdf font, double size)
  {
    boolean hasCap = false;

    for (char c : string.toCharArray()) {
      if (Character.isUpperCase(c)) {
        hasCap = true;
        break;
      }
    }

    double fontHeight = hasCap ? font.getCapHeight() : font.getXHeight();

    return size * fontHeight / 1000.0;
  }

  /*
   * An ESTIMATE of the number of chars that will fit in the space based
   * on the avg glyph size.  This only works properly with fixed-width fonts!
   *
   */
  public int charCount(double width, double size, String fontName)
  {
    FontPdf font = fontLoad(fontName);
    
    if (font == null)
      return 0;

    return (int) Math.round(width / (font.getAvgCharWidth() / 1000 * size));
  }
  
  public int charCount(double width)
  {
    FontPdf font = _stream.getFont();
    
    if (font == null)
      return 0;

    double size = _stream.getFontSize();
    
    return (int) Math.round(width / (font.getAvgCharWidth() / 1000 * size));
  }

  void addResource(String name, String value)
  {
    String oldValue = _resources.get(name);
    
    if (oldValue == null || oldValue.equals(value)) {
      _resources.put(name, value);
      return;
    }
    
    if (oldValue.startsWith("<<")) {
      String oldValueStrip = oldValue.substring(2, oldValue.length() - 2);
      String valueStrip = value.substring(2, value.length() - 2);
      
      ArrayList<String> oldValueList = new ArrayList<String>();
      
      for (String elt : oldValueStrip.split("\n")) {
        oldValueList.add(elt);
      }
      
      for (String elt : valueStrip.split("\n")) {
        if (! oldValueList.contains(elt))
          oldValueList.add(elt);
      }
      
      StringBuilder sb = new StringBuilder();
      sb.append("<<");
      for (int i = 0; i < oldValueList.size(); i++) {
        if (i != 0)
          sb.append("\n");
        
        sb.append(oldValueList.get(i));
      }
      sb.append(">>");
      
      _resources.put(name, sb.toString());
    }
  }

  void write(OutPdf out)
    throws IOException
  {
    out.beginObject(_id);
    out.println("  << /Type /Page");
    out.println("     /Parent " + _parent + " 0 R");
    out.println("     /MediaBox [0 0 " + _width + " " + _height + "]");
    out.println("     /Contents " + _stream.getId() + " 0 R");
    out.println("     /Resources <<");

    for (Map.Entry<String,String> entry : _resources.entrySet()) {
      out.println("     " + entry.getKey() + " " + entry.getValue());
    }

    out.println("     >>");
    out.println("  >>");
    out.endObject();

    _stream.write(out);
  }
  
  public void close()
  {
    _pdf.pageClose(this);
  }
}
