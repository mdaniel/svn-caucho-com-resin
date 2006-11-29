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
import javax.xml.stream.*;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class XMLInputFactoryImpl extends XMLInputFactory {

  private XMLEventAllocator _allocator = new XMLEventAllocatorImpl();
  private XMLReporter _reporter;
  private XMLResolver _resolver;

  public XMLInputFactoryImpl()
  {
  }

  //
  // Filtered
  //
  
  public XMLEventReader 
    createFilteredReader(XMLEventReader reader, EventFilter filter)
    throws XMLStreamException
  {
    return new FilteredEventReader(reader, filter);
  }

  public XMLStreamReader 
    createFilteredReader(XMLStreamReader reader, StreamFilter filter)
    throws XMLStreamException
  {
    return new FilteredStreamReader(reader, filter);
  }

  //
  // Event reader
  //
  
  public XMLEventReader 
    createXMLEventReader(InputStream stream)
    throws XMLStreamException
  {
    return new XMLEventReaderImpl(createXMLStreamReader(stream));
  }

  public XMLEventReader 
    createXMLEventReader(InputStream stream, String encoding)
    throws XMLStreamException
  {
    return new XMLEventReaderImpl(createXMLStreamReader(stream, encoding));
  }

  public XMLEventReader 
    createXMLEventReader(Reader reader)
    throws XMLStreamException
  {
    return new XMLEventReaderImpl(createXMLStreamReader(reader));
  }

  /**
   *  "Support of this method is optional."
   */ 
  public XMLEventReader 
    createXMLEventReader(Source source)
    throws XMLStreamException
  {
    throw new JAXPNotSupportedInStAXException();
  }

  public XMLEventReader 
    createXMLEventReader(String systemId, InputStream stream)
    throws XMLStreamException
  {
    return new XMLEventReaderImpl(createXMLStreamReader(systemId, stream));
  }

  public XMLEventReader 
    createXMLEventReader(String systemId, Reader reader)
    throws XMLStreamException
  {
    return new XMLEventReaderImpl(createXMLStreamReader(systemId, reader));
  }

  public XMLEventReader 
    createXMLEventReader(XMLStreamReader reader)
    throws XMLStreamException
  {
    return new XMLEventReaderImpl(reader);
  }

  //
  // Stream reader
  //
  
  public XMLStreamReader 
    createXMLStreamReader(InputStream stream)
    throws XMLStreamException
  {
    return new XMLStreamReaderImpl(stream);
  }

  public XMLStreamReader 
    createXMLStreamReader(InputStream stream, String encoding)
    throws XMLStreamException
  {
    try {
      InputStreamReader isr = new InputStreamReader(stream, encoding);
      return new XMLStreamReaderImpl(isr);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public XMLStreamReader 
    createXMLStreamReader(Reader reader)
    throws XMLStreamException
  {
    return new XMLStreamReaderImpl(reader);
  }

  /**
   *  "Support of this method is optional."
   */ 
  public XMLStreamReader 
    createXMLStreamReader(Source source)
    throws XMLStreamException
  {
    throw new JAXPNotSupportedInStAXException();
  }

  public XMLStreamReader 
    createXMLStreamReader(String systemId, InputStream stream)
    throws XMLStreamException
  {
    return new XMLStreamReaderImpl(stream, systemId);
  }

  public XMLStreamReader 
    createXMLStreamReader(String systemId, Reader reader)
    throws XMLStreamException
  {
    return new XMLStreamReaderImpl(reader, systemId);
  }

  public XMLEventAllocator getEventAllocator()
  {
    return _allocator;
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new IllegalArgumentException("property \""+name+"\" not supported");
  }

  public XMLReporter getXMLReporter()
  {
    return _reporter;
  }

  public XMLResolver getXMLResolver()
  {
    return _resolver;
  }

  public boolean isPropertySupported(String name)
  {
    return false;
  }

  public void setEventAllocator(XMLEventAllocator allocator)
  {
    _allocator = allocator;
  }

  public void setProperty(String name, Object value)
    throws IllegalArgumentException
  {

    if ("javax.xml.stream.allocator".equals(name)) {
      setEventAllocator((XMLEventAllocator)value);
      return;
    }

    throw new IllegalArgumentException("property \""+name+"\" not supported");
  }

  public void setXMLReporter(XMLReporter reporter)
  {
    _reporter = reporter;
  }

  public void setXMLResolver(XMLResolver resolver)
  {
    _resolver = resolver;
  }

}

