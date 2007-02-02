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
 * @author Scott Ferguson
 */

package com.caucho.jaxb.skeleton;

import java.io.IOException;

import javax.xml.XMLConstants;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import javax.xml.namespace.QName;

import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;

/**
 * represents a property in a skeleton; requires an Accessor to access it
 */
public abstract class Property {
  public boolean isXmlPrimitiveType()
  {
    return true;
  }

  public String getMaxOccurs()
  {
    return null;
  }

  public abstract String getSchemaType();

  public abstract Object read(Unmarshaller u, XMLStreamReader in, QName name)
    throws IOException, XMLStreamException, JAXBException;
  
  public abstract Object read(Unmarshaller u, XMLEventReader in, QName name)
    throws IOException, XMLStreamException, JAXBException;
  
  public abstract Object bindFrom(BinderImpl binder, 
                                  NodeIterator node, 
                                  QName name)
    throws JAXBException;

  public abstract void write(Marshaller m, XMLStreamWriter out, 
                             Object obj, QName name)
    throws IOException, XMLStreamException, JAXBException;

  public abstract void write(Marshaller m, XMLEventWriter out, 
                             Object obj, QName name)
    throws IOException, XMLStreamException, JAXBException;

  public abstract Node bindTo(BinderImpl binder, Node node, 
                              Object obj, QName qname)
    throws JAXBException;

  protected void writeQNameStartElement(XMLStreamWriter out, QName name)
    throws IOException, XMLStreamException
  {
    if (name == null)
      return;

    if (name.getPrefix() != null && ! "".equals(name.getPrefix())) {
      out.writeStartElement(name.getPrefix(), 
                            name.getLocalPart(), 
                            name.getNamespaceURI());
    }
    else if (name.getNamespaceURI() != null && 
             ! "".equals(name.getNamespaceURI())) {
      out.writeStartElement(name.getNamespaceURI(), name.getLocalPart());
    }
    else
      out.writeStartElement(name.getLocalPart());
  }

  protected void writeQNameEndElement(XMLStreamWriter out, QName name)
    throws IOException, XMLStreamException
  {
    if (name == null)
      return;

    out.writeEndElement();
  }
}
