/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.image;

public class ImageInfo
{
  private int _width;
  private int _height;
  private int _type;

  private int _bits;

  private String _mimeType;

  public ImageInfo(int width, int height, int type, int bits, String mimeType)
  {
    _width = width;
    _height = height;

    _type = type;
    _bits = bits;
    _mimeType = mimeType;
  }

  public ImageInfo()
  {
  }

  public int getWidth()
  {
    return _width;
  }

  public void setWidth(int width)
  {
    _width = width;
  }

  public int getHeight()
  {
    return _height;
  }

  public void setHeight(int height)
  {
    _height = height;
  }

  public int getType()
  {
    return _type;
  }

  public void setType(int type)
  {
    _type = type;
  }

  public int getBits()
  {
    return _bits;
  }

  public void setBits(int bits)
  {
    _bits = bits;
  }

  public String getMimeType()
  {
    return _mimeType;
  }

  public void setMimeType(String mimeType)
  {
    _mimeType = mimeType;
  }
}
