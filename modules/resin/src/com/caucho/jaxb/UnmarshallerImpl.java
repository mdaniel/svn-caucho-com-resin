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

package com.caucho.jaxb;
import javax.xml.bind.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.bind.attachment.*;
import javax.xml.bind.Unmarshaller.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;
import java.net.*;
import com.caucho.jaxb.skeleton.*;
import com.caucho.jaxb.adapters.*;
import javax.xml.bind.helpers.*;
import javax.xml.namespace.*;

public class UnmarshallerImpl extends AbstractUnmarshallerImpl {

  private JAXBContextImpl _context;

  UnmarshallerImpl(JAXBContextImpl context)
  {
    this._context = context;
  }

  public Object unmarshal(Node node) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  ////////////////////////////////////////////////////////////////////////

  protected Object unmarshal(XMLReader reader, InputSource source)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Object unmarshal(XMLEventReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                      Class<T> declaredType)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Object unmarshal(XMLStreamReader reader)
    throws JAXBException
  {
    try {
      while(reader.getEventType() != XMLStreamReader.START_ELEMENT)
        reader.next();
      
      return _context.getRootElement(reader.getName()).read(this, reader);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <T> JAXBElement<T> unmarshal(XMLStreamReader reader,
                                      Class<T> declaredType)
      throws JAXBException
  {
    try {

      while(reader.getEventType() != XMLStreamReader.START_ELEMENT)
        reader.next();

      QName name = reader.getName();
      T val = (T)_context.getSkeleton(declaredType).read(this, reader);
      return new JAXBElement<T>(name, declaredType, val);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    A a = super.getAdapter(type);

    if (a == null)
      return (A)new BeanAdapter();

    return a;
  }
}
