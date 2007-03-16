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
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.BinderImpl;

import com.caucho.util.L10N;
import com.caucho.util.Base64;

/**
 * DataHandler property.
 *
 * Note that DataHandler fields/properties are not affected by XmlMimeType
 * annotations.
 */
public class DataHandlerProperty extends Property {
  public static final DataHandlerProperty PROPERTY = new DataHandlerProperty();
  private static final String DEFAULT_DATA_HANDLER_MIME_TYPE 
    = "application/octet-stream";

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    return read(u, in, previous, null, null);
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous, 
                     ClassSkeleton attributed, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    if (attributed != null) {
      for (int i = 0; i < in.getAttributeCount(); i++) {
        QName attributeName = in.getAttributeName(i);
        Accessor a = attributed.getAttributeAccessor(attributeName);

        if (a == null)
          throw new UnmarshalException(L.l("Attribute {0} not found in {1}", 
                                           attributeName, 
                                           attributed.getType()));

        a.set(parent, a.readAttribute(in, i));
      }
    }

    in.next();

    Object ret = null;

    if (in.getEventType() == in.CHARACTERS) {
      byte[] buffer = Base64.decodeToByteArray(in.getText());
      ByteArrayDataSource bads = 
        new ByteArrayDataSource(buffer, DEFAULT_DATA_HANDLER_MIME_TYPE);
      ret = new DataHandler(bads);

      // essentially a nextTag() that handles end of document gracefully
      while (in.hasNext()) {
        in.next();

        if (in.getEventType() == in.END_ELEMENT)
          break;
      }
    }

    while (in.hasNext()) {
      in.next();

      if (in.getEventType() == in.START_ELEMENT || 
          in.getEventType() == in.END_ELEMENT)
        break;
    }

    return ret;
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void write(Marshaller m, XMLStreamWriter out, Object value, 
                    QName qname, Object obj, Iterator attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      writeQNameStartElement(out, qname);

      if (attributes != null) {
        while (attributes.hasNext()) {
          Accessor a = (Accessor) attributes.next();
          a.write(m, out, obj);
        }
      }

      if (value != null) {
        DataHandler handler = (DataHandler) value;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        handler.writeTo(baos);

        out.writeCharacters(Base64.encodeFromByteArray(baos.toByteArray()));
      }

      writeQNameEndElement(out, qname);
    }
  }

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object value, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    write(m, out, value, qname, null);
  }

  public void write(Marshaller m, XMLStreamWriter out, Object value,
                    QName qname, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      writeQNameStartElement(out, qname);

      DataHandler handler = (DataHandler) value;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      handler.writeTo(baos);

      out.writeCharacters(Base64.encodeFromByteArray(baos.toByteArray()));

      writeQNameEndElement(out, qname);
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object value, QName name)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Node bindTo(BinderImpl binder, Node node, Object value, QName qname)
    throws IOException,JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public String getSchemaType()
  {
    return "xsd:base64Binary";
  }
}
