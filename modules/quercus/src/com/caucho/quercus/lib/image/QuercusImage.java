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
 */

package com.caucho.quercus.lib.image;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.OptionsModule;
import com.caucho.util.IntQueue;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;

public class QuercusImage extends ResourceValue
{
  private static LruCache<StringValue,Font> _fontMap
    = new LruCache<StringValue,Font>(1024);

  private static Font FONT_ZERO = new Font("sansserif", 0, 8);
  private static Font FONT_TWO = new Font("sansserif", 0, 10);
  private static Font FONT_THREE = new Font("sansserif", 0, 11);
  private static Font FONT_FOUR = new Font("sansserif", 0, 12);
  private static Font FONT_FIVE = new Font("sansserif", 0, 14);

  private BufferedImage _bufferedImage;
  private Graphics2D _graphics;
  private boolean _isInterlace;

  private BufferedImage _brush;
  private int[] _style;
  private int _thickness;

  private boolean _isToFill = false;
  private boolean _isBlankImage = false;

  public QuercusImage(int width, int height)
  {
    _bufferedImage = new BufferedImage(width, height,
                                       BufferedImage.TYPE_INT_RGB);
    _graphics = (Graphics2D)_bufferedImage.getGraphics();

    _isBlankImage = true;
  }

  public QuercusImage(InputStream inputStream)
  {
    try {
      _bufferedImage = ImageIO.read(inputStream);
      _graphics = (Graphics2D)_bufferedImage.getGraphics();
    }
    catch (IOException e) {
      throw new QuercusException(e);
    }
  }

  public QuercusImage(Env env, Path filename)
  {
    try {
      _bufferedImage = ImageIO.read(filename.openRead());
      _graphics = (Graphics2D)_bufferedImage.getGraphics();
    }
    catch (IOException e) {
      throw new QuercusException(e);
    }
  }

  public void setInterlace(boolean isInterlace)
  {
    _isInterlace = isInterlace;
  }

  public boolean isInterlace()
  {
    return _isInterlace;
  }

  public int getPixel(int x, int y)
  {
    return _bufferedImage.getRGB(x, y) & 0x00ffffff;
  }

  public void setPixel(int x, int y, int color)
  {
    _isBlankImage = false;

    _bufferedImage.setRGB(x, y, color);
  }

  public Graphics2D getGraphics()
  {
    return _graphics;
  }

  public Font getFont(int fontIndex)
  {
    switch (fontIndex) {
      case 0: case 1:
        return FONT_ZERO;
      case 2:
        return FONT_TWO;
      case 3:
        return FONT_THREE;
      case 4:
        return FONT_FOUR;
      default:
        return FONT_FIVE;
    }
  }

  public Font getTrueTypeFont(Env env, StringValue fontPath)
    throws FontFormatException,
           IOException
  {
    Font font = _fontMap.get(fontPath);

    if (font != null)
      return font;

    Path path = env.lookupPwd(fontPath);

    if (path.canRead()) {
      ReadStream is = path.openRead();

      try {
        font = Font.createFont(Font.TRUETYPE_FONT, is);
      } finally {
        is.close();
      }

      _fontMap.put(fontPath, font);

      return font;
    }

    if (fontPath.length() > 0 && fontPath.charAt(0) == '/')
      return null;

    StringValue gdFontPathKey = env.createString("GDFONTPATH");

    StringValue gdFontPath
      = OptionsModule.getenv(env, gdFontPathKey).toStringValue();

    int start = 0;
    int len = gdFontPath.length();

    while (start < len) {
      int i = gdFontPath.indexOf(':', start);

      if (i >= 0 && i + 1 < len && gdFontPath.charAt(i + 1) == ';') {
        StringValue item = gdFontPath.substring(start, i);

        path = env.lookupPwd(item);

        start = i + 2;
      }
      else {
        StringValue item = gdFontPath.substring(start);

        path = env.lookupPwd(item);

        start = len;
      }

      if (path.canRead()) {
        ReadStream is = path.openRead();

        try {
          font = Font.createFont(Font.TRUETYPE_FONT, is);
        } finally {
          is.close();
        }

        _fontMap.put(fontPath, font);

        return font;
      }

    }

    return null;
  }

  public BufferedImage getBufferedImage()
  {
    return _bufferedImage;
  }

  public int getWidth()
  {
    return _bufferedImage.getWidth();
  }

  public int getHeight()
  {
    return _bufferedImage.getHeight();
  }

  public void fill(Shape shape, int color)
  {
    _isBlankImage = false;

    _graphics.setColor(intToColor(color));
    _graphics.fill(shape);
  }

  protected static Color intToColor(int argb)
  {
    // don't forget: PHP alpha channel is only 7 bits
    int alpha = argb >> 24;
    alpha <<= 1;
    alpha |= ((alpha & 0x2) >> 1);  // copy bit #2 to LSB

    return new Color((argb >> 16) & 0xff,
                     (argb >>  8) & 0xff,
                     (argb >>  0) & 0xff,
                     alpha);
  }

  public void stroke(Shape shape, int color)
  {
    _isBlankImage = false;

    switch(color) {
      case ImageModule.IMG_COLOR_STYLED:
        strokeStyled(shape);
        break;
      case ImageModule.IMG_COLOR_BRUSHED:
        strokeBrushed(shape);
        break;
      default:
        _graphics.setColor(intToColor(color));
        _graphics.setStroke(new BasicStroke(_thickness));
        _graphics.draw(shape);
        break;
    }
  }

  private void strokeStyled(Shape shape)
  {
    _isBlankImage = false;

    for (int i = 0; i < _style.length; i++) {
      _graphics.setColor(intToColor(_style[i]));
      Stroke stroke =
        new BasicStroke(_thickness,
                        BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND, 1,
            new float[]{1, _style.length - 1},
                        i);
      _graphics.setStroke(stroke);
      _graphics.draw(shape);
    }
  }

  private void strokeBrushed(Shape shape)
  {
    _isBlankImage = false;

    // XXX: support "styled brushes" (see imagesetstyle() example on php.net)
    Graphics2D g = _graphics;
    FlatteningPathIterator fpi =
      new FlatteningPathIterator(shape.getPathIterator(g.getTransform()), 1);
    float[] floats = new float[6];
    fpi.currentSegment(floats);
    float last_x = floats[0];
    float last_y = floats[1];
    while (! fpi.isDone())
      {
        fpi.currentSegment(floats);
        int distance = (int) Math.sqrt(
            (floats[0] - last_x) * (floats[0] - last_x)
                + (floats[1] - last_y) * (floats[1] - last_y));
        if (distance <= 1) distance = 1;
        for (int i = 1; i <= distance; i++)
          {
            int x = (int)
                (floats[0] * i + last_x * (distance - i)) / distance;
            x -= _brush.getWidth() / 2;
            int y = (int)
                (floats[1] * i + last_y * (distance - i)) / distance;
            y -= _brush.getHeight() / 2;
            g.drawImage(_brush, x, y, null);
          }
        last_x = floats[0];
        last_y = floats[1];
        fpi.next();
      }
  }

  public void setThickness(int thickness)
  {
    _style = null;
    _thickness = thickness;
  }

  public void setStyle(Env env, ArrayValue colors)
  {
    _style = new int[colors.getSize()];

    Iterator<Value> iter = colors.getValueIterator(env);

    for (int i = 0; i < _style.length; i++) {
      _style[i] = iter.next().toInt();
    }
  }

  public void setBrush(QuercusImage image)
  {
    _brush = image._bufferedImage;
  }

  public BufferedImage getBrush()
  {
    return _brush;
  }

  public void setToFill(boolean isToFill)
  {
    _isToFill = isToFill;
  }

  public long allocateColor(int r, int g, int b)
  {
    int color = ((0x7f << 24)
        | ((r & 0xff) << 16)
        | ((g & 0xff) <<  8)
        | ((b & 0xff) <<  0));

    if (_isToFill) {
      _isToFill = false;
      flood(0, 0, color);
    }

    return color;
  }

  public void flood(int x, int y, int color)
  {
    flood(x, y, color, 0, false);
  }

  public void flood(int x, int y, int color, int border)
  {
    flood(x, y, color, border, true);
  }

  private void floodBlankImage(int color)
  {
    int width = getWidth();
    int height = getHeight();

    color &= 0x00ffffff;

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        setPixel(i, j, color);
      }
    }

    /*
    Rectangle rect = new Rectangle(width, height);

    _graphics.setColor(intToColor(color));
    _graphics.fill(rect);
    */
  }

  private void flood(int startX, int startY,
                     int color, int border, boolean isUseBorder)
  {
    boolean isBlank = _isBlankImage;

    if (isBlank) {
      floodBlankImage(color);

      return;
    }

    _isBlankImage = false;

    IntQueue q = new IntQueue();
    q.add(startX);
    q.add(startY);

    color &= 0x00ffffff;
    border &= 0x00ffffff;

    int width = getWidth();
    int height = getHeight();

    int startColor = getPixel(startX, startX) & 0x00ffffff;

    while (q.size() > 0) {
      int x = q.remove();
      int y = q.remove();
      int p = (getPixel(x, y) & 0x00ffffff);

      if (isUseBorder && (p == border || p == color)
          || (! isUseBorder) && p != startColor
          || p == color) {
        continue;
      }

      setPixel(x, y, color);

      if (y - 1 >= 0) {
        addPointIfValid(q, x, y - 1, color, border, startColor, isUseBorder);
      }

      if (y + 1 < height) {
        addPointIfValid(q, x, y + 1, color, border, startColor, isUseBorder);
      }

      if (x - 1 >= 0) {
        if (y - 1 >= 0) {
          addPointIfValid(q, x - 1, y - 1, color, border, startColor, isUseBorder);
        }

        addPointIfValid(q, x - 1, y, color, border, startColor, isUseBorder);

        if (y + 1 < height) {
          addPointIfValid(q, x - 1, y + 1, color, border, startColor, isUseBorder);
        }
      }

      if (x + 1 < width) {
        if (y - 1 >= 0) {
          addPointIfValid(q, x + 1, y - 1, color, border, startColor, isUseBorder);
        }

        addPointIfValid(q, x + 1, y, color, border, startColor, isUseBorder);

        if (y + 1 < height) {
          addPointIfValid(q, x + 1, y + 1, color, border, startColor, isUseBorder);
        }
      }

    }
  }

  private void addPointIfValid(IntQueue queue,
                               int x,
                               int y,
                               int fillColor,
                               int borderColor,
                               int startColor,
                               boolean isUseBorder)
  {
    int pixel = getPixel(x, y) & 0x00ffffff;

    if (isUseBorder && (pixel == borderColor || pixel == fillColor)) {
    }
    else if (! isUseBorder && pixel != startColor) {
    }
    else {
      queue.add(x);
      queue.add(y);
    }
  }
}
