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
 * @author Emil Ong
 */

package com.caucho.soap.marshall;

import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import java.util.*;

import java.lang.reflect.*;
import java.io.*;

import com.caucho.vfs.WriteStream;

/**
 * Marshalls data for a JAXB object
 */
public class JAXBMarshall extends Marshall {
  private JAXBContext _context;

  public JAXBMarshall(Class cl)
    throws JAXBException
  {
    _context = JAXBContext.newInstance(new Class[] { cl });
  }


  /**
   * Deserializes the data from the input.
   */
  public Object deserialize(XMLStreamReader in)
    throws IOException
  {
    try {
      Unmarshaller unmarshaller = _context.createUnmarshaller();
      return unmarshaller.unmarshal(in);
    } catch (JAXBException e) {
      IOException ioException = new IOException();
      ioException.initCause(e);
      throw ioException;
    }
  }

  /**
   * Serializes the data to the result
   */
  public void serialize(XMLStreamWriter out, Object obj, QName fieldName)
    throws IOException, XMLStreamException
  {
    out.writeStartElement(fieldName.toString());

    try {
      Marshaller marshaller = _context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
      marshaller.marshal(obj, out);
    } catch (JAXBException e) {
      IOException ioException = new IOException();
      ioException.initCause(e);
      throw ioException;
    }

    out.writeEndElement(); // fieldName
  }
}


