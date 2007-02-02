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

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

import org.w3c.dom.Node;

/**
 * a property referencing some other Skeleton
 */
public class SkeletonProperty extends Property {
  private static final L10N L = new L10N(SkeletonProperty.class);

  private Skeleton _skeleton;

  public SkeletonProperty(Skeleton skeleton)
  {
    _skeleton = skeleton;
  }

  public Object read(Unmarshaller u, XMLStreamReader in, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (in.getEventType() != in.START_ELEMENT || ! in.getName().equals(qname))
      return null;

    Object ret = _skeleton.read(u, in);

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
    XMLEvent event = in.peek();

    if (! event.isStartElement() || 
        ! qname.equals(((StartElement) event).getName()))
      return null;

    Object ret = _skeleton.read(u, in);

    while (! event.isEndElement())
      event = in.nextEvent();

    if (! ((EndElement) event).getName().equals(qname))
      throw new IOException(L.l("Expected </{0}>, not </{1}>", 
                                qname.getLocalPart(), 
                                ((EndElement) event).getName()));

    return ret;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, QName name)
    throws JAXBException
  {
    Node root = node.getNode();

    if (root.getNodeType() != Node.ELEMENT_NODE)
      return null;

    QName nodeName = JAXBUtil.qnameFromNode(root);

    if (! name.equals(nodeName))
      return null;

    Object old = binder.getJAXBNode(root);

    Object ret = _skeleton.bindFrom(binder, old, node);

    binder.bind(ret, root);

    return ret;
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX Subclassing/anyType
    //Skeleton skeleton = getAccessor().getContext().findSkeletonForObject(obj);
    _skeleton.write(m, out, obj, qname);
  }

  public void write(Marshaller m, XMLEventWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX Subclassing/anyType
    //Skeleton skeleton = getAccessor().getContext().findSkeletonForObject(obj);
    _skeleton.write(m, out, obj, qname);
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName qname)
    throws JAXBException
  {
    return _skeleton.bindTo(binder, node, obj, qname);
  }

  public String getSchemaType()
  {
    return JAXBUtil.qNameToString(_skeleton.getTypeName());
  }

  public boolean isXmlPrimitiveType()
  {
    return false;
  }

  public String toString()
  {
    return "SkeletonProperty[" + _skeleton + "]";
  }
}


