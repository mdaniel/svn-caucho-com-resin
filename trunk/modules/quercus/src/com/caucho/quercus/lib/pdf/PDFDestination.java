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

import java.util.*;

/**
 * pdf object oriented API facade
 */
public class PDFDestination 
{
  private int _id;
  private int _pageId;
  private String _title;
  private double _pos;
  private int _parentId;
  private List<PDFDestination> _children = 
    new ArrayList<PDFDestination>();

  PDFDestination(int id, 
                 String title, 
                 int pageId,
                 double pos)
  {
    _id = id;
    _title = title;
    _pageId = pageId;
    _pos = pos;
  }
  
  public int getId()
  {
    return _id;
  }

  public String getTitle()
  {
    return _title;
  }

  public int getPageId()
  {
    return _pageId;
  }
  
  public double getPos()
  {
    return _pos;
  }
  
  public void setParentId(int parentId)
  {
    _parentId = parentId;
  }
  
  public int getParentId()
  {
    return _parentId;
  }
  
  public void addChild(PDFDestination child)
  {
    child.setParentId(_id);
    _children.add(child);
  }
  
  public List<PDFDestination> getChildren()
  {
    return _children;
  }
  
  public boolean hasChildren()
  {
    return ! _children.isEmpty();
  }
}
