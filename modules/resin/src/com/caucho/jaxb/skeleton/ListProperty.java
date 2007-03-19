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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// XXX: Make generic?
/**
 * a List Property
 */
public class ListProperty extends IterableProperty {
  public ListProperty(Property componentProperty)
  {
    _componentProperty = componentProperty;
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    ArrayList<Object> list = (ArrayList<Object>) previous;

    if (list == null)
      list = new ArrayList<Object>();

    list.add(_componentProperty.read(u, in, null));

    return list;
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    ArrayList<Object> list = (ArrayList<Object>) previous;

    if (list == null)
      list = new ArrayList<Object>();

    list.add(_componentProperty.read(u, in, null));

    return list;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException, JAXBException
  {
    Node child = node.getNode();

    ArrayList<Object> list = new ArrayList<Object>();

    list.add(_componentProperty.bindFrom(binder, node, null));

    return list;
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object value, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      if (value instanceof List) {
        List list = (List) value;

        for (Object o : list)
          _componentProperty.write(m, out, o, qname);
      }
      else
        throw new ClassCastException("Argument not a List");
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object value, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      if (value instanceof List) {
        List list = (List) value;

        for (Object o : list)
          _componentProperty.write(m, out, o, qname);
      }
      else
        throw new ClassCastException("Argument not a List");
    }
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object value, QName name)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX
  }

  public void write(Marshaller m, XMLEventWriter out,
                    Object value, QName name)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName qname)
    throws IOException, JAXBException
  {
    if (obj != null) {
      if (obj instanceof List) {
        List list = (List) obj;

        Node child = node.getFirstChild();
        for (Object o : list) {
          if (child != null) {
            // try to reuse as many of the child nodes as possible
            Node newNode =
              _componentProperty.bindTo(binder, child, o, qname);

            if (newNode != child) {
              node.replaceChild(child, newNode);
              binder.invalidate(child);
            }

            child = child.getNextSibling();
            node = JAXBUtil.skipIgnorableNodes(node);
          }
          else {
            Node newNode = JAXBUtil.elementFromQName(qname, node);
            newNode = _componentProperty.bindTo(binder, newNode, o, qname);

            node.appendChild(newNode);
          }
        }
      }
      else
        throw new ClassCastException("Argument not a List");
    }

    return node;
  }

  public String getSchemaType()
  {
    return _componentProperty.getSchemaType();
  }

  public boolean isXmlPrimitiveType()
  {
    return getComponentProperty().isXmlPrimitiveType();
  }

  public String getMaxOccurs()
  {
    return "unbounded";
  }

  public boolean isNillable()
  {
    return true;
  }
}
