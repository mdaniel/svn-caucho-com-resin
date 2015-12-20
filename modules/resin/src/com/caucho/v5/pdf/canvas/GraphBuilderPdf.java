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

import java.util.ArrayList;

import com.caucho.v5.pdf.PagePdf;
import com.caucho.v5.util.QDate;

/**
 * pdf object oriented API facade
 */
public class GraphBuilderPdf
{
  private final CanvasPdf _canvas;
  private final String _name;
  
  private double _width;
  private double _height;
  
  private double _x;
  private double _y;
  
  private double _xMin = Double.MAX_VALUE;
  private double _xMax = -Double.MAX_VALUE;
  
  private double _yMin = 0; // Double.MAX_VALUE;
  private double _yMax = -Double.MAX_VALUE;
  
  private ArrayList<DataBuilderPdf> _dataList = new ArrayList<>();
  
  // view data
  private double _xTop;
  private double _yLeft;
  private double _xLeft;
  private double _yTop;
  private double _xGraph;
  private double _widthGraph;
  private double _yGraph;
  private double _heightGraph;
  private double _unitsHeight;
  private double _unitsWidth;
  
  private boolean _isDate = true;
  private double _heightTick;
  private double _widthTick;
  
  GraphBuilderPdf(CanvasPdf canvas, String name, double width, double height)
  {
    _canvas = canvas;
    _name = name;
    
    _width = width;
    _height = height;
  }
  
  public GraphBuilderPdf range(double xMin, double xMax, double yMin, double yMax)
  {
    _xMin = xMin;
    _xMax = xMax;
    _yMin = yMin;
    _yMax = yMax;
    
    return this;
  }
  
  public DataBuilderPdf dataBuilder(String name)
  {
    DataBuilderPdf builder = new DataBuilderPdf(this);
    
    builder.name(name);
    
    return builder;
  }
  
  public GraphBuilderPdf date(boolean isDate)
  {
    _isDate = isDate;
    
    return this;
  }
  
  public void build()
  {
    PagePdf page = _canvas.getPage();
    
    page.save();
    
    _canvas.font(8, "Courier");
    
    double titleHeight = page.getFontHeight();
    
    _unitsWidth = page.textWidth("00.0");
    _unitsHeight = page.getFontHeight();
    
    _widthGraph = _width - _unitsWidth;
    
    double legendHeight = getLegendHeight();
    
    _heightGraph = _height - titleHeight - _unitsHeight - legendHeight;
    
    _canvas.alignXY(_widthGraph, _heightGraph);
    
    if (_canvas.getY() - _height < _canvas.getMarginBottom()) {
      _canvas.nextPage();
    }
    
    _xLeft = _canvas.getX();
    _yTop = _canvas.getY();
    
    _xGraph = _xLeft + _unitsWidth;
    _yGraph = _yTop - _height + _unitsHeight + legendHeight;
    
    double yTitle = _yTop - page.getFontAscender();
    
    _canvas.textCenter(_xLeft + _width / 2, yTitle, _name);
    
    calculateRangeXDate();
    calculateRangeY();
    
    if (_yMin <= _yMax) {
      drawTicks();
      drawBorders();
      
      drawLegend();
    
      drawGraph();
    }
    else {
      drawBorders();
      
      double yCenter = _yTop - _height / 2;
      
      _canvas.textCenter(_xLeft + _width / 2, yCenter, "no data");
    }
    
    page.restore();
    
    _canvas.advance(_width, _height);
  }
  
  private void calculateRangeY()
  {
    double height = _yMax - _yMin;
    
    if (height <= 0) {
      height = 1;
    }
    
    double heightLog = Math.log10(height);
    double tail = heightLog - Math.floor(heightLog);
    
    if (tail < 0.5) {
      _heightTick = Math.pow(10.0, Math.floor(heightLog) - 1);
    }
    else {
      _heightTick = Math.pow(10.0, Math.floor(heightLog));
    }
    
    int ticks = (int) (height / _heightTick);
    
    if (ticks >= 20) {
      _heightTick *= 5;
    }
    else if (ticks > 10) {
      _heightTick *= 2;
    }
    
    _yMax = _heightTick * Math.floor((_yMax + _heightTick) / _heightTick);
  }
  
  private void calculateRangeXDate()
  {
    double time = _xMax - _xMin;
    
    if (time <= 0) {
      _widthTick = 60000.0;
    }
    else {
      double minutes = time / 60000;
      
      if (minutes >= 2 * 24 * 60) {
        _widthTick = 24 * 60 * 60000.0;
      }
      else if (minutes >= 24 * 60) {
        _widthTick = 6 * 60 * 60000.0;
      }
      else if (minutes >= 4 * 60) {
        _widthTick = 60 * 60000;
      }
      else if (minutes >= 60) {
        _widthTick = 15 * 60000;
      }
      else if (minutes >= 15) {
        _widthTick = 5 * 60000;
      }
      else {
        _widthTick = 60000;
      }
      
      //_xMin = _widthTick * Math.floor(_xMin / _widthTick);
      //_xMax = _widthTick * Math.floor(_xMax / _widthTick + 0.99);
    }
  }
  
  private void drawBorders()
  {
    PagePdf page = _canvas.getPage();
    
    //page.hsb(20.0/360, 0.5, 0.5)
    
    page.hsb(0.0/360, 0, 0.5)
        .linewidth(1.0)
        .rect(_xGraph, _yGraph, _widthGraph, _heightGraph)
        .stroke();
    
    // page.hsb(0.5, 1, 1).rect(_xLeft, _yTop, _width, - _height).stroke();
  }
  
  /**
   * Draws the grid tick marks.
   */
  private void drawTicks()
  {
    PagePdf page = _canvas.getPage();
    
    //page.hsb(20.0/360, 0.1, 0.95)
    page.hsb(0, 0, 0.95)
        .linewidth(1.0);
    
    double width = _xMax - _xMin;
    
    double xFirst = _widthTick * Math.floor((_xMin + _widthTick) / _widthTick);
    //double xLast = _widthTick * Math.floor(_xMax / _widthTick); 
    double xLast = _xMax;
    
    for (double x = xFirst; x < xLast; x += _widthTick) {
      double xPoint = _xGraph + _widthGraph * (x - _xMin) / width;
      
      page.moveto(xPoint, _yGraph)
          .lineto(xPoint, _yGraph + _heightGraph)
          .stroke();
    }
    
    double height = _yMax - _yMin;
    
    double yFirst = _heightTick * Math.floor((_yMin + _heightTick) / _heightTick);
    double yLast = _heightTick * Math.floor(_yMax / _heightTick); 
    
    for (double y = yFirst; y < yLast; y += _heightTick) {
      double yPoint = _yGraph + _heightGraph * (y - _yMin) / height;
      
      page.moveto(_xGraph, yPoint)
          .lineto(_xGraph + _widthGraph, yPoint)
          .stroke();
    }
  }
  
  private void drawLegend()
  {
    PagePdf page = _canvas.getPage();
    
    page.gray(0);
    
    double ascender = page.getFontAscender();
    
    String xMin;
    
    if (_isDate) {
      xMin = unitDate(_xMin, _xMin, _xMax);
    }
    else {
      xMin = unit(_xMin, _xMin, _xMax);
    }
    _canvas.text(_xGraph, _yGraph - ascender - 2, xMin);
    
    String xMax;
    
    if (_isDate) {
      xMax = unitDate(_xMax, _xMin, _xMax);
    }
    else {
      xMax = unit(_xMax, _xMin, _xMax);
    }
    
    _canvas.textRight(_xGraph + _widthGraph, _yGraph - ascender - 2, xMax);
    
    String yMin = unit(_yMin, _yMin, _yMax);
    _canvas.textRight(_xGraph - 2, _yGraph, yMin);
    
    String yMax = unit(_yMax, _yMin, _yMax);
    _canvas.textRight(_xGraph - 2, _yGraph + _heightGraph - ascender, yMax);
    
    _canvas.font(8, "Courier");
    double legendHeight = 8;
    
    for (int i = 0; i < _dataList.size(); i++) {
      DataBuilderPdf data = _dataList.get(i);
      
      double y = _yGraph - _unitsHeight - legendHeight * (i + 1);
      
      String name = data.getName();
      
      setColor(page, i);
      
      page.moveto(_xGraph + 10, y + legendHeight / 2 - 2)
          .lineto(_xGraph + 25, y + legendHeight / 2 - 2)
          .stroke();
      
      _canvas.text(_xGraph + 30, y, name);
      
      page.gray(0);
    }
  }
  
  private double getLegendHeight()
  {
    double size = _dataList.size();
    
    double legendHeight = 8;
    
    return size * legendHeight + legendHeight / 2;
  }
  
  private String unit(double value, double min, double max)
  {
    double minLog = Math.log10(Math.abs(min));
    double maxLog = Math.log10(Math.abs(max));
    
    if (maxLog >= 12) {
      return String.format("%1.1fT", value / 1e12);
    }
    else if (maxLog >= 9) {
      return String.format("%1.1fG", value / 1e9);
    }
    else if (maxLog >= 6) {
      return String.format("%1.1fM", value / 1e6);
    }
    else if (maxLog >= 3) {
      return String.format("%1.1fk", value / 1e3);
    }
    else {
      return String.format("%1.1f", value);
    }
  }
  
  private String unitDate(double value, double min, double max)
  {
    boolean isDay = (max - min) >= 24 * 3600 * 1000.0;
    
    if (isDay) {
      return QDate.formatLocal((long) value, "%m-%d");
    }
    else {
      return QDate.formatLocal((long) value, "%H:%M");
    }
  }
  
  private void drawGraph()
  {
    if (_dataList.size() == 0) {
      return;
    }
    
    double width = _xMax - _xMin;
    double height = _yMax - _yMin;
    
    PagePdf page = _canvas.getPage();
    
    page.save();
    
    page.linewidth(0);
    
    page.translate(_xGraph, _yGraph);
    page.scale(_widthGraph / 10.0, _heightGraph / 10.0);
    
    for (int i = 0; i < _dataList.size(); i++) {
      DataBuilderPdf data = _dataList.get(i);
      
      setColor(page, i);
      
      boolean isFirst = true;
      for (ItemGraphPdf item : data.getItems()) {
        double x = 10.0 * (item.getX() - _xMin) / width; 
        double y = 10.0 * (item.getY() - _yMin) / height;
        
        if (isFirst) {
          page.moveto(x, y);
          
          isFirst = false;
        }
        else {
          page.lineto(x, y);
        }
      }
      
      page.stroke();
    }
    
    page.restore();
  }
  
  void setColor(PagePdf page, int index)
  {
    ColorGraph []colors = ColorGraph.values();
    
    ColorGraph color = colors[index % colors.length];
    
    page.hsb(color.getH(), color.getS(), color.getB());
  }

  void addData(DataBuilderPdf data)
  {
    _xMin = Math.min(data.getXMin(), _xMin);
    _xMax = Math.max(data.getXMax(), _xMax);
    
    _yMin = Math.min(data.getYMin(), _yMin);
    _yMax = Math.max(data.getYMax(), _yMax);
    
    _dataList.add(data);
  }
  
  static enum ColorGraph {
    AZURE(220.0 / 360, 0.8, 0.8),
    RED(0.0 / 360, 0.8, 0.8),
    GREEN(120.0 / 360, 0.8, 0.5),
    ORANGE(40.0 / 360, 1, 0.8),
    BLUE(240.0 / 360, 0.8, 0.8),
    PURPLE(300.0 / 360, 0.8, 0.8),
    BLACK(0, 0, 0),
    CYAN(180.0 / 360, 0.8, 0.8),
    ;
    
    private final double _hue;
    private final double _sat;
    private final double _bright;
    
    ColorGraph(double hue, double sat, double bright)
    {
      _hue = hue;
      _sat = sat;
      _bright = bright;
    }
    
    double getH()
    {
      return _hue;
    }
    
    double getS()
    {
      return _sat;
    }
    
    double getB()
    {
      return _bright;
    }
    
  }
}
