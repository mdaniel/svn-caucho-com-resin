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
* @author Adam Megacz
*/

package com.caucho.xml.stream;
import java.io.*;
import javax.xml.stream.*;
import javax.xml.stream.util.*;
import javax.xml.transform.*;
import com.caucho.vfs.*;

public class XMLOutputFactoryImpl extends XMLOutputFactory {

  public XMLOutputFactoryImpl()
  {
  }

  public XMLEventWriter createXMLEventWriter(OutputStream stream)
    throws XMLStreamException
  {
    return new StreamEventWriter(Vfs.openWrite(stream));
  }

  public XMLEventWriter createXMLEventWriter(OutputStream stream,
                                             String encoding)
    throws XMLStreamException
  {
    try {
      OutputStreamWriter osw = new OutputStreamWriter(stream, encoding);
      return new StreamEventWriter(Vfs.openWrite(osw));
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /**
   *  This method is optional.
   */
  public XMLEventWriter createXMLEventWriter(Result result)
    throws XMLStreamException
  {
    throw new JAXPNotSupportedInStAXException();
  }

  public XMLEventWriter createXMLEventWriter(Writer stream)
    throws XMLStreamException
  {
    return new StreamEventWriter(Vfs.openWrite(stream));
  }

  public XMLStreamWriter createXMLStreamWriter(OutputStream stream)
    throws XMLStreamException
  {
    return new StreamWriterImpl(Vfs.openWrite(stream));
  }

  public XMLStreamWriter createXMLStreamWriter(OutputStream stream,
                                               String encoding)
    throws XMLStreamException
  {
    try {
      OutputStreamWriter osw = new OutputStreamWriter(stream, encoding);
      return new StreamWriterImpl(Vfs.openWrite(osw));
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /**
   *  This method is optional.
   */
  public XMLStreamWriter createXMLStreamWriter(Result result)
    throws XMLStreamException
  {
    throw new JAXPNotSupportedInStAXException();
  }

  public XMLStreamWriter createXMLStreamWriter(Writer stream)
    throws XMLStreamException
  {
    return new StreamWriterImpl(Vfs.openWrite(stream));
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new IllegalArgumentException("property \""+name+"\" not supported");
  }

  public boolean isPropertySupported(String name)
  {
    return false;
  }

  public void setProperty(String name, Object value)
    throws IllegalArgumentException
  {
    throw new IllegalArgumentException("property \""+name+"\" not supported");
  }


}

