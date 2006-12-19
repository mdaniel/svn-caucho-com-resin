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

package com.caucho.jaxb.skeleton;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import com.caucho.util.L10N;

/**
 * helper class for properties that are represented as a "flat" CDATA block
 */
public abstract class CDataProperty extends Property {
  private static final L10N L = new L10N(CDataProperty.class);

  protected abstract Object read(String in)
    throws IOException, XMLStreamException;

  public Object read(Unmarshaller u, XMLStreamReader in, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (in.getEventType() != in.START_ELEMENT)
      throw new IOException(L.l("Expected <{0}>", qname.toString()));
    else if (! in.getName().equals(qname))
      throw new IOException(L.l("Expected <{0}>, not <{1}>", 
                                qname.toString(), in.getName().toString()));

    in.next();

    Object ret = null;

    if (in.getEventType() == in.CHARACTERS)
      ret = read(in.getText());

    while (in.getEventType() != in.END_ELEMENT)
      in.nextTag();

    if (! in.getName().equals(qname))
      throw new IOException(L.l("Expected </{0}>, not </{1}>", 
                                qname.getLocalPart(), in.getLocalName()));

    in.nextTag();

    return ret;
  }

  protected abstract String write(Object in)
    throws IOException, XMLStreamException;

  public void write(Marshaller m, XMLStreamWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      writeQNameStartElement(out, qname);
      out.writeCharacters(write(obj));
      writeQNameEndElement(out, qname);
    }
  }
}
