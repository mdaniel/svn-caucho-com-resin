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

package com.caucho.v5.pdf.canvas;

import java.io.IOException;

import com.caucho.v5.pdf.PagePdf;
import com.caucho.v5.pdf.PdfDocument;
import com.caucho.v5.vfs.Path;

/**
 * pdf object oriented API facade
 */
public class CanvasPdf implements AutoCloseable 
{
  private static final TextBuilder _textStandard;
  
  private PdfDocument _pdf;
  private PagePdf _page;
  
  private double _pageWidth = 595;
  private double _pageHeight = 842;
  
  private double _marginLeft = 40;
  private double _marginRight = 40;
  private double _marginTop = 40;
  private double _marginBottom = 40;
  
  private String _sectionFont = "Helvetica";
  private double _sectionFontSize = 12;
  
  private String _headerFont = "Helvetica";
  private double _headerFontSize = 8;
  
  private String _textFont = "Helvetica";
  private double _textFontSize = 9;
  
  private String _dataFont = "Courier";
  private double _dataFontSize = 8;
  
  private String _headerLeft;
  private String _headerCenter;
  private String _headerRight;
  
  private String _footerLeft;
  private String _footerCenter;
  private String _footerRight;

  
  private int _pageNumber;
  
  private ModePdf _mode = ModePdf.TEXT;
  
  private double _xText;
  private double _yText;
  private double _yRow;
  
  private double _yLast;

  private String _sectionTitle;

  private Object _subsectionTitle;

  private boolean _isSectionPage;

  public CanvasPdf(Path path)
    throws IOException
  {
    _pdf = new PdfDocument(path);
    
    nextPage();
  }

  public double getX()
  {
    return _xText;
  }

  public double getY()
  {
    return invertY(_yText);
  }
  
  public CanvasPdf marginLeft(double margin)
  {
    _marginLeft = margin;
    
    return this;
  }
  
  public CanvasPdf marginRight(double margin)
  {
    _marginRight = margin;
    
    return this;
  }
  
  public CanvasPdf marginTop(double margin)
  {
    _marginTop = margin;
    
    return this;
  }
  
  public CanvasPdf marginBottom(double margin)
  {
    _marginBottom = margin;
    
    return this;
  }

  public double getMarginBottom()
  {
    return _marginBottom;
  }

  public PagePdf getPage()
  {
    return _page;
  }
  
  protected String getHeaderLeft()
  {
    return _headerLeft;
  }
  
  public void setHeaderLeft(String text)
  {
    _headerLeft = text;
  }
  
  protected String getHeaderCenter()
  {
    return _headerCenter;
  }
  
  public void setHeaderCenter(String text)
  {
    _headerCenter = text;
  }
  
  protected String getHeaderRight()
  {
    return _headerRight;
  }
  
  public void setHeaderRight(String text)
  {
    _headerRight = text;
  }
  
  protected String getFooterLeft()
  {
    return _footerLeft;
  }
  
  public void setFooterLeft(String text)
  {
    _footerLeft = text;
  }
  
  protected String getFooterCenter()
  {
    return _footerCenter;
  }
  
  public void setFooterCenter(String text)
  {
    _footerCenter = text;
  }
  
  protected String getFooterRight()
  {
    return _footerRight;
  }
  
  public void setFooterRight(String text)
  {
    _footerRight = text;
  }
  
  protected void setHeaderFont()
  {
    _page.font(_headerFontSize, _headerFont);
    _page.gray(0);
  }
  
  protected double getTextLineWidth()
  {
    return _pageWidth - _marginLeft - _marginRight;
  }
  
  //
  // text
  //
  
  public CanvasPdf font(FontPdf font)
  {
    switch (font) {
    case TEXT:
      _page.font(_textFontSize, _textFont);
      break;
      
    case DATA:
      _page.font(_dataFontSize, _dataFont);
      break;
    }
    
    return this;
  }
  
  public CanvasPdf font(double size, String name)
  {
    _page.font(size, name);
    
    return this;
  }
  
  public GraphBuilderPdf graphBuilder(String name, double width, double height)
  {
    return new GraphBuilderPdf(this, name, width, height);
  }
  
  public CanvasPdf text(String text)
  {
    text(text, _textStandard);
    
    return this;
  }

  public CanvasPdf textColumn(int x, String text)
  {
    double xNew = _marginLeft + x * _page.textWidth("m");
    
    if (_xText < xNew) {
      _xText = xNew;
    }
    
    text(text);
    
    return this;
  }

  public CanvasPdf textColumnRight(int x, int width, String text)
  {
    double xNew = _marginLeft + x * _page.textWidth("m");
    
    if (_xText < xNew) {
      _xText = xNew;
    }
    
    _page.posText(alignRight(_xText + width, text), invertY(_yText))
         .text(text);
    
    _xText += width;
    
    return this;
  }
  
  public CanvasPdf newline()
  {
    align();
    
    _yText += _page.getFontHeight();
    _xText = _marginLeft;
    
    return this;
  }
  
  public CanvasPdf textLeft(double x, String text)
  {
    if (_pageHeight - _marginBottom <= _yText) {
      nextPage();
    }
    
    writeText(calculateX(x), _yText, text);
    
    return this;
  }
  
  public CanvasPdf textRight(double x, String text)
  {
    if (_pageHeight - _marginBottom <= _yText) {
      nextPage();
    }
    
    writeTextRight(calculateX(x), _yText, text);
    
    return this;
  }
  
  public CanvasPdf text(double x, double y, String text)
  {
    _page.posText(x, y).text(text);
    
    return this;
  }
  
  public CanvasPdf textCenter(double x, double y, String text)
  {
    _page.posText(x - _page.textWidth(text) / 2, y)
         .text(text);
    
    return this;
  }
  
  public CanvasPdf textRight(double x, double y, String text)
  {
    _page.posText(x - _page.textWidth(text), y)
         .text(text);
    
    return this;
  }
  
  protected double calculateX(double x)
  {
    if (x >= 0) {
      return _marginLeft + x;
    }
    else {
      return _pageWidth - _marginRight + x;
    }
  }
  
  protected void text(String text, TextBuilder builder)
  {
    double width = getTextLineWidth();

    AlignPdf align = AlignPdf.LEFT;
    double indent = 0;
    
    String []lines = text.split("\\n");
    
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      
      if (i > 0) {
        _xText = _marginLeft + indent;
        _yText += _page.getFontHeight();
      }

      double x = _xText;
      double xPos;
    
      if (_pageHeight - _marginBottom <= _yText) {
        nextPage();
      }
    
      double lineWidth = _page.textWidth(line);
    
      switch (align) {
      case LEFT:
        writeText(x, _yText, line);
        _xText += indent + lineWidth;
        break;
      
      default:
        writeText(x, _yText, line);
        break;
      }
    }
  }
  
  public CanvasPdf hrule(double width)
  {
    return hrule(width, 0);
  }
  
  public CanvasPdf hrule(double linewidth, double indent)
  {
    _page.save();
    
    _page.linewidth(linewidth);
    
    double height = _page.getFontHeight() / 2;
    double y = _yText - height;
    
    _page.moveto(_marginLeft + indent, invertY(y))
         .lineto(_pageWidth - _marginRight - indent, invertY(y))
         .stroke();
    
    _page.restore();
    
    return this;
  }
  
  //
  // page formatting (headers, footers)
  //

  public CanvasPdf section(String title)
  {
    return section(title, false);
  }
  
  public CanvasPdf section(String title, boolean isNewPage)
  {
    _page.save();
    
    align();
    
    _sectionTitle = title;
    _subsectionTitle = null;
    
    if (_isSectionPage) {
      newline();
    }
    _isSectionPage = true;
    
    _page.outline(title);
    
    _page.font(_sectionFontSize, _sectionFont);
    text(title);
    newline();
    
    hrule(0, 2);
    newline();
    
    setHeaderRight(title);
    
    _page.restore();
    
    return this;
  }
  
  protected void writeHeaders()
  {
    _page.save();
    
    setHeaderFont();
    
    double line = (_marginTop - _page.getFontSize()) / 2;
    
    String headerLeft = getHeaderLeft();
    
    if (headerLeft != null) {
      writeText(_marginLeft, line, headerLeft);
    }
    
    String headerCenter = getHeaderCenter();
    
    if (headerCenter != null) {
      double center = (_pageWidth - _marginLeft - _marginRight) / 2;
      
      writeTextCenter(_marginLeft + center, line, headerCenter);
    }
    
    String headerRight = getHeaderRight();
    
    if (headerRight != null) {
      writeTextRight(_pageWidth - _marginRight, line, headerRight);
    }
    
    _page.restore();
  }
  
  protected void writeFooters()
  {
    _page.save();
    
    setHeaderFont();
    
    double line = _pageHeight - (_marginBottom + _page.getFontSize()) / 2;
    
    String footerLeft = getFooterLeft();
    
    if (footerLeft != null) {
      writeText(_marginLeft, line, footerLeft);
    }
    
    String footerCenter = getFooterCenter();
    
    if (footerCenter == null) {
      footerCenter = "Page " + _pageNumber;
    }
      
    double center = (_pageWidth - _marginLeft - _marginRight) / 2;
      
    writeTextCenter(_marginLeft + center, line, footerCenter);
    
    String footerRight = getFooterRight();
    
    if (footerRight != null) {
      writeTextRight(_pageWidth - _marginRight, line, footerRight);
    }
    
    _page.restore();
  }

  public void advance(double width, double height)
  {
    _yLast = _yText;
    _yText += height;
    _xText += width;
  }
  
  /**
   * Align to the beginning of the row after an advance. Used to handle
   * the left-to-right graph
   */
  public void align()
  {
    _xText = _marginLeft;
    _yLast = _yText;
  }
  
  public void alignXY(double width, double height)
  {
    if (_xText + width <= _pageWidth - _marginRight) {
      _yText = _yLast;
    }
    else {
      _xText = _marginLeft;
      _yLast = _yText;
    }
  }
  
  public void nextPage()
  {
    
    double fontSize = _textFontSize;
    String fontName = _textFont;
    
    if (_page != null) {
      fontName = _page.getFont().getFontName();
      fontSize = _page.getFontSize();
    }
    
    closePage();
    
    _page = _pdf.page(_pageWidth, _pageHeight);
    _page.font(fontSize, fontName);
    
    _pageNumber++;
    
    _isSectionPage = false;
    
    _yText = _marginTop;
    _yLast = _yText;
    _xText = _marginLeft;
  }
  
  private void writeText(double x, double y, String text)
  {
    _page.posText(x, invertY(y))
         .text(text);
  }
  
  private void writeTextCenter(double x, double y, String text)
  {
    _page.posText(alignCenter(x, text), invertY(y))
         .text(text);
  }
  
  private double writeTextRight(double x, double y, String text)
  {
    _page.posText(alignRight(x, text), invertY(y))
         .text(text);
    
    return _page.textWidth(text);
  }
  
  private double alignCenter(double x, String text)
  {
    return x - _page.textWidth(text) / 2;
  }
  
  private double alignRight(double x, String text)
  {
    return x - _page.textWidth(text);
  }
  
  private double invertY(double y)
  {
    return _mode == ModePdf.GRAPH ? y : (_pageHeight - y);
  }
  
  private void closePage()
  {
    PagePdf page = _page;
    
    if (_page != null) {
      writeHeaders();
      writeFooters();
      
      _page = null;
      page.close();
    }
  }

  @Override
  public void close()
    throws IOException
  {
    closePage();
    
    _pdf.close();
  }
  
  static class TextBuilder {
    public void text(CanvasPdf canvas, String text)
    {
      canvas.text(text, this);
    }
  }
  
  enum ModePdf {
    TEXT,
    GRAPH;
  }
  
  public enum AlignPdf {
    LEFT,
    CENTER,
    RIGHT;
  }
  
  public enum FontPdf {
    DATA,
    TEXT;
  }
  
  static {
    _textStandard = new TextBuilder();
  }
}
