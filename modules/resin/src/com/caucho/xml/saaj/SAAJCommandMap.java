/*
* Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.xml.saaj;

import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.activation.*;
import javax.imageio.*;
import javax.xml.namespace.*;
import javax.xml.soap.*;
import javax.xml.transform.stream.*;

public class SAAJCommandMap extends MailcapCommandMap
                            implements DataContentHandlerFactory
{
  private static final DataContentHandler _plainDataContentHandler 
    = new PlainDataContentHandler();

  private static final DataContentHandler _xmlDataContentHandler 
    = new XmlDataContentHandler();

  private static final DataContentHandler _imageDataContentHandler 
    = new ImageDataContentHandler();

  public static final SAAJCommandMap COMMAND_MAP = new SAAJCommandMap();

  public DataContentHandler createDataContentHandler(String mimeType)
  {
    if ("text/plain".equals(mimeType))
      return _plainDataContentHandler;
    else if ("text/xml".equals(mimeType))
      return _xmlDataContentHandler;
    else if ("image/gif".equals(mimeType))
      return _imageDataContentHandler;
    else if ("image/jpeg".equals(mimeType))
      return _imageDataContentHandler;
    else return super.createDataContentHandler(mimeType);
  }

  static class PlainDataContentHandler implements DataContentHandler
  {
    static final DataFlavor[] flavors = 
      new DataFlavor[] { new DataFlavor(String.class, "text/plain") };

    public Object getContent(DataSource ds)
      throws IOException
    {
      InputStream is = ds.getInputStream();
      ByteArrayOutputStream os = new ByteArrayOutputStream();

      for (int b = is.read(); b >= 0; b = is.read())
        os.write(b);

      return new String(os.toByteArray());
    }

    public Object getTransferData(DataFlavor df, DataSource ds)
      throws UnsupportedFlavorException, IOException
    {
      if (df.equals(flavors[0]))
        return getContent(ds);

      throw new UnsupportedFlavorException(df);
    }

    public DataFlavor[] getTransferDataFlavors()
    {
      return flavors;
    }

    public void writeTo(Object obj, String mimeType, OutputStream os) 
      throws IOException
    {
      os.write(obj.toString().getBytes());
    }
  }

  static class XmlDataContentHandler implements DataContentHandler
  {
    static final DataFlavor[] flavors = 
      new DataFlavor[] { new DataFlavor(StreamSource.class, "text/xml") };

    public Object getContent(DataSource ds)
      throws IOException
    {
      return new StreamSource(ds.getInputStream());
    }

    public Object getTransferData(DataFlavor df, DataSource ds)
      throws UnsupportedFlavorException, IOException
    {
      if (df.equals(flavors[0]))
        return getContent(ds);

      throw new UnsupportedFlavorException(df);
    }

    public DataFlavor[] getTransferDataFlavors()
    {
      return flavors;
    }

    public void writeTo(Object obj, String mimeType, OutputStream os) 
      throws IOException
    {
      StreamSource source = (StreamSource) obj;

      InputStream is = source.getInputStream();

      for (int b = is.read(); b >= 0; b = is.read())
        os.write(b);
    }
  }

  static class ImageDataContentHandler implements DataContentHandler
  {
    static final DataFlavor[] flavors 
      = new DataFlavor[] { new DataFlavor(BufferedImage.class, "image/gif"), 
                           new DataFlavor(BufferedImage.class, "image/jpeg") };

    public Object getContent(DataSource ds)
      throws IOException
    {
      return ImageIO.read(ds.getInputStream());
    }

    public Object getTransferData(DataFlavor df, DataSource ds)
      throws UnsupportedFlavorException, IOException
    {
      if (df.equals(flavors[0]) || df.equals(flavors[1]))
        return getContent(ds);

      throw new UnsupportedFlavorException(df);
    }

    public DataFlavor[] getTransferDataFlavors()
    {
      return flavors;
    }

    public void writeTo(Object obj, String mimeType, OutputStream os) 
      throws IOException
    {
      int slash = mimeType.indexOf('/');

      if (slash < 0)
        throw new IOException("Unrecognized MIME type : " + mimeType);

      String formatName = mimeType.substring(slash + 1);
      Iterator writers = ImageIO.getImageWritersByFormatName(formatName);

      if (writers.hasNext())
        throw new IOException("Unsupported image type : " + mimeType);

      ImageWriter writer = (ImageWriter) writers.next();

      writer.setOutput(os);
      writer.write((BufferedImage) obj);
    }
  }
}
