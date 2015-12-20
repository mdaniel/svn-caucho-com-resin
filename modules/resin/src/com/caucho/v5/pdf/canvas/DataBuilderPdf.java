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
import java.util.Objects;

/**
 * pdf object oriented API facade
 */
public class DataBuilderPdf
{
  private final GraphBuilderPdf _graph;
  
  private String _name = "";
  
  private double _minX = Double.MAX_VALUE;
  private double _maxX = -Double.MAX_VALUE;
  
  private double _minY = Double.MAX_VALUE;
  private double _maxY = -Double.MAX_VALUE;
  
  private final ArrayList<ItemGraphPdf> _items = new ArrayList<>();
  
  DataBuilderPdf(GraphBuilderPdf graph)
  {
    Objects.requireNonNull(graph);
    
    _graph = graph;
  }
  
  DataBuilderPdf name(String name)
  {
    _name = name;
    
    return this;
  }
  
  public String getName()
  {
    return _name;
  }
  
  public DataBuilderPdf point(double x, double y)
  {
    ItemGraphPdf item = new ItemGraphPdf(x, y);
    
    _minX = Math.min(x, _minX);
    _maxX = Math.max(x, _maxX);
    
    _minY = Math.min(y, _minY);
    _maxY = Math.max(y, _maxY);
    
    _items.add(item);
    
    return this;
  }
  
  public double getWidth()
  {
    return _maxX - _minX;
  }
  
  public double getHeight()
  {
    return _maxY - _minY;
  }
  
  ArrayList<ItemGraphPdf> getItems()
  {
    return _items;
  }
  
  public void build()
  {
    if (_items.size() == 0) {
      return;
    }
    
    _graph.addData(this);
  }

  public double getXMin()
  {
    return _minX;
  }

  public double getXMax()
  {
    return _maxX;
  }

  public double getYMin()
  {
    return _minY;
  }

  public double getYMax()
  {
    return _maxY;
  }
}
