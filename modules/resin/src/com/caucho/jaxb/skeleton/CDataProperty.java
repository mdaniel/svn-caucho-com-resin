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
 * @author Emil Ong, Adam Megacz
 */

package com.caucho.jaxb.skeleton;

import java.io.IOException;

import java.util.Iterator;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;

/**
 * helper class for properties that are represented as a "flat" CDATA block
 */
public abstract class CDataProperty extends Property {
  private static final L10N L = new L10N(CDataProperty.class);

  protected boolean _isNillable = true;

  protected abstract Object read(String in) 
    throws JAXBException;

  public String getMinOccurs()
  {
    if (_isNillable)
      return "0";

    return null;
  }

  public Object readAttribute(XMLStreamReader in, int i)
    throws JAXBException
  {
    return read(in.getAttributeValue(i));
  }

  public Object readAttribute(Attribute attribute)
    throws JAXBException
  {
    return read(attribute.getValue());
  }

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
      ret = read(in.getText());

      // essentially a nextTag() that handles end of document gracefully
      while (in.hasNext()) {
        in.next();

        if (in.getEventType() == in.END_ELEMENT)
          break;
      }
    }
    else
      ret = read(""); // Hack when we have something like <tag></tag>

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
    // XXX attributes 
    XMLEvent event = in.nextEvent(); // skip start element

    Object ret = null;

    if (event.isCharacters()) {
      ret = read(((Characters) event).getData());
      event = in.nextEvent();
    }
    else
      ret = read(""); // Hack when we have something like <tag></tag>

    while (in.hasNext()) {
      if (event.isEndElement())
        break;

      event = in.nextEvent();
    }

    if (in.hasNext())
      in.next();

    return ret;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws JAXBException
  {
    Node root = node.getNode();

    Object ret = read(root.getTextContent());

    binder.bind(ret, root);

    return ret;
  }

  protected abstract String write(Object in)
    throws JAXBException;

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

      out.writeCharacters(write(value));
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
      out.writeCharacters(write(value));
      writeQNameEndElement(out, qname);
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object value, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    write(m, out, value, qname, null);
  }

  public void write(Marshaller m, XMLEventWriter out, Object value, 
                    QName qname, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      out.add(JAXBUtil.EVENT_FACTORY.createStartElement(qname, null, null));
      out.add(JAXBUtil.EVENT_FACTORY.createCharacters(write(value)));
      out.add(JAXBUtil.EVENT_FACTORY.createEndElement(qname, null));
    }
  }

  public Node bindTo(BinderImpl binder, Node node, Object value, QName qname)
    throws JAXBException
  {
    QName name = JAXBUtil.qnameFromNode(node);

    if (! name.equals(qname)) {
      Document doc = node.getOwnerDocument(); 
      node = JAXBUtil.elementFromQName(qname, doc);
    }

    node.setTextContent(write(value));

    binder.bind(value, node);

    return node;
  }
}
