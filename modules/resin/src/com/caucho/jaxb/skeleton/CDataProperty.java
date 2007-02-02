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

import java.io.IOException;

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

  public Object read(Unmarshaller u, XMLStreamReader in, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (in.getEventType() != in.START_ELEMENT) {
      if (_isNillable)
        return null;
      else
        throw new IOException(L.l("Expected <{0}>", qname.toString()));
    }
    else if (! in.getName().equals(qname)) {
      if (_isNillable)
        return null;
      else
        throw new IOException(L.l("Expected <{0}>, not <{1}>", 
              qname.toString(), in.getName().toString()));
    }

    in.next();

    Object ret = null;

    if (in.getEventType() == in.CHARACTERS)
      ret = read(in.getText());
    else
      ret = read(""); // Hack when we have something like <tag></tag>

    while (in.getEventType() != in.END_ELEMENT)
      in.nextTag();

    if (! in.getName().equals(qname))
      throw new IOException(L.l("Expected </{0}>, not </{1}>", 
            qname.getLocalPart(), in.getLocalName()));

    // essentially a nextTag() that handles end of document gracefully
    while (in.hasNext()) {
      in.next();

      if (in.getEventType() == in.END_ELEMENT || 
          in.getEventType() == in.START_ELEMENT)
        break;
    }

    return ret;
  }

  public Object read(Unmarshaller u, XMLEventReader in, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    XMLEvent event = null;
    
    event = in.peek();

    if (! event.isStartElement()) {
      if (_isNillable)
        return null;
      else
        throw new IOException(L.l("Expected <{0}>", qname.toString()));
    }
    else if (! ((StartElement) event).getName().equals(qname)) {
      if (_isNillable)
        return null;
      else
        throw new IOException(L.l("Expected <{0}>, not <{1}>", 
                                  qname.toString(), 
                                  ((StartElement) event).getName().toString()));
    }

    event = in.nextEvent(); // skip start element

    event = in.peek();

    Object ret = null;

    if (event.isCharacters()) {
      ret = read(((Characters) event).getData());
      event = in.nextEvent();
    }
    else
      ret = read(""); // Hack when we have something like <tag></tag>

    while (! event.isEndElement())
      event = in.nextEvent();

    if (! ((EndElement) event).getName().equals(qname)) {
      String localName = ((EndElement) event).getName().getLocalPart();

      throw new IOException(L.l("Expected </{0}>, not </{1}>", 
                                qname.getLocalPart(), localName));
    }

    return ret;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, QName qname)
    throws JAXBException
  {
    Node root = node.getNode();

    if (root.getNodeType() != Node.ELEMENT_NODE) {
      if (_isNillable)
        return null;
      else
        throw new UnmarshalException(L.l("Expected <{0}>", qname));
    }
    else {
      QName nodeName = JAXBUtil.qnameFromNode(root);
      
      if (! nodeName.equals(qname)) {
        if (_isNillable)
          return null;
        else
          throw new UnmarshalException(L.l("Expected <{0}>, not <{1}>", 
                                      qname, nodeName));
      }
    }

    Object ret = read(root.getTextContent());

    binder.bind(ret, root);

    return ret;
  }

  protected abstract String write(Object in);

  public void write(Marshaller m, XMLStreamWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      writeQNameStartElement(out, qname);
      out.writeCharacters(write(obj));
      writeQNameEndElement(out, qname);
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      out.add(JAXBUtil.EVENT_FACTORY.createStartElement(qname, null, null));
      out.add(JAXBUtil.EVENT_FACTORY.createCharacters(write(obj)));
      out.add(JAXBUtil.EVENT_FACTORY.createEndElement(qname, null));
    }
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName qname)
    throws JAXBException
  {
    QName name = JAXBUtil.qnameFromNode(node);

    if (! name.equals(qname)) {
      Document doc = node.getOwnerDocument(); 
      node = JAXBUtil.elementFromQName(qname, doc);
    }

    node.setTextContent(write(obj));

    binder.bind(obj, node);

    return node;
  }
}
