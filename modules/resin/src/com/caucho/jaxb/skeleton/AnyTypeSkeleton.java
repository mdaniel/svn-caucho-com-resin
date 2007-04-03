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

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import java.io.IOException;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 *
 **/
public class AnyTypeSkeleton extends ClassSkeleton<Object> {
  private static final Logger log 
    = Logger.getLogger(AnyTypeSkeleton.class.getName());
  private static final L10N L = new L10N(AnyTypeSkeleton.class);

  public AnyTypeSkeleton(JAXBContextImpl context)
    throws JAXBException
  {
    super(context);
  }

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    DOMResult result = new DOMResult();
    XMLOutputFactory factory = _context.getXMLOutputFactory();
    XMLStreamWriter out = factory.createXMLStreamWriter(result);

    StaxUtil.copyReaderToWriter(in, out);

    Node node = result.getNode();

    if (node.getNodeType() == Node.DOCUMENT_NODE)
      node = ((Document) node).getDocumentElement();

    return node;

    /*
    Skeleton skeleton = _context.getRootElement(in.getName());

    if (skeleton != null)
      return skeleton.read(u, in);

    // skip everything in this subtree
    int depth = 0;

    in.nextTag();

    do {
      if (in.getEventType() == XMLStreamConstants.START_ELEMENT)
        depth++;
      else if (in.getEventType() == XMLStreamConstants.END_ELEMENT)
        depth--;

      if (depth < 0)
        break;

      in.next();
    } 
    while (depth > 0);

    return null;*/
  }

  public Object read(Unmarshaller u, XMLEventReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    // skip everything in this subtree
    int depth = 0;

    XMLEvent event = in.nextTag();

    do {
      if (event.getEventType() == XMLStreamConstants.START_ELEMENT)
        depth++;
      else if (event.getEventType() == XMLStreamConstants.END_ELEMENT)
        depth--;

      if (depth < 0)
        break;

      event = in.nextEvent();
    } 
    while (depth > 0);

    return null;
  }

  public Object bindFrom(BinderImpl binder, Object existing, NodeIterator node)
    throws JAXBException
  {
    // skipping a subtree is much easier in DOM =D
    return null;
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, QName fieldName, Iterator attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      Skeleton skeleton = _context.findSkeletonForObject(obj);

      if (skeleton != null) {
        String typeName = null;//StaxUtil.qnameToString(out, skeleton.getTypeName());

        // XXX if (typeName == null) {}

        XmlInstanceWrapper instanceWrapper = 
          new XmlInstanceWrapper(typeName, attributes);

        Iterator extendedAttributes = instanceWrapper.getExtendedIterator();
        skeleton.write(m, out, obj, null, attributes);
        return;
      }

      Property property = _context.getSimpleTypeProperty(obj.getClass());

      if (property != null) {
        XmlInstanceWrapper instanceWrapper = 
          new XmlInstanceWrapper(property.getSchemaType());

        Iterator extendedAttributes = instanceWrapper.getExtendedIterator();

        // XXX the second obj here is a hack: we just need it not to be null.
        // Figure out if the api is wrong or there is a reasonable value for
        // the fifth argument instead of this second obj.
        property.write(m, out, obj, fieldName, obj, extendedAttributes);
        return;
      }

      if (obj instanceof Node) {
        XMLInputFactory factory = _context.getXMLInputFactory();
        DOMSource source = new DOMSource((Node) obj);
        XMLStreamReader in = factory.createXMLStreamReader(source);

        StaxUtil.copyReaderToWriter(in, out);
        
        return;
      }

      if (fieldName.getNamespaceURI() == null)
        out.writeEmptyElement(fieldName.getLocalPart());
      else if (fieldName.getPrefix() == null)
        out.writeEmptyElement(fieldName.getNamespaceURI(), 
                              fieldName.getLocalPart());
      else
        out.writeEmptyElement(fieldName.getPrefix(), 
                              fieldName.getLocalPart(), 
                              fieldName.getNamespaceURI()); 
    }
  }

  public void write(Marshaller m, XMLEventWriter out,
                    Object obj, QName fieldName, Iterator attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    out.add(JAXBUtil.EVENT_FACTORY.createStartElement(fieldName, null, null));
    out.add(JAXBUtil.EVENT_FACTORY.createEndElement(fieldName, null));
  }

  public Node bindTo(BinderImpl binder, Node node, 
                     Object obj, QName fieldName)
    throws JAXBException
  {
    return node;
  }

  public String toString()
  {
    return "AnyTypeSkeleton[]";
  }
}
