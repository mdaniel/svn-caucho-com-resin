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
 * achar with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;

/**
 * a property for serializing/deserializing arrays
 */
public class CharacterArrayProperty extends ArrayProperty {
  private static final L10N L = new L10N(CharacterArrayProperty.class);

  public static final CharacterArrayProperty PROPERTY 
    = new CharacterArrayProperty();

  public CharacterArrayProperty()
  {
    super(CharacterProperty.PRIMITIVE_PROPERTY);
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    char[] array = (char[]) previous;

    if (array == null)
      array = new char[1];
    else {
      char[] newArray = new char[array.length + 1];
      System.arraycopy(array, 0, newArray, 0, array.length);

      array = newArray;
    }

    array[array.length - 1] =
      ((Character) _componentProperty.read(u, in, null)).charValue();

    return array;
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    char[] array = (char[]) previous;

    if (array == null)
      array = new char[1];
    else {
      char[] newArray = new char[array.length + 1];
      System.arraycopy(array, 0, newArray, 0, array.length);

      array = newArray;
    }

    array[array.length - 1] =
      ((Character) _componentProperty.read(u, in, null)).charValue();

    return array;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException, JAXBException
  {
    char[] array = (char[]) previous;

    if (array == null)
      array = new char[1];
    else {
      char[] newArray = new char[array.length + 1];
      System.arraycopy(array, 0, newArray, 0, array.length);

      array = newArray;
    }

    Character b = (Character) _componentProperty.bindFrom(binder, node, null);

    array[array.length - 1] = b.charValue();

    return array;
  }

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object value, QName qname, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    //XXX wrapper
    
    if (value != null) {
      char[] array = (char[]) value;

      for (int i = 0; i < array.length; i++) {
        CharacterProperty.PRIMITIVE_PROPERTY.write(m, out, 
                                                   array[i], qname, obj);
      }
    }
  }

  public void write(Marshaller m, XMLEventWriter out, 
                    Object value, QName qname, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    //XXX wrapper
    
    if (value != null) {
      char[] array = (char[]) value;

      for (int i = 0; i < array.length; i++) {
        CharacterProperty.PRIMITIVE_PROPERTY.write(m, out, 
                                                   array[i], qname, obj);
      }
    }
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName qname)
    throws IOException, JAXBException
  {
    QName name = JAXBUtil.qnameFromNode(node);
    Document doc = node.getOwnerDocument(); 

    if (! name.equals(qname))
      node = JAXBUtil.elementFromQName(qname, doc);

    binder.bind(obj, node);

    if (obj != null) {
      char[] array = (char[]) obj;

      for (int i = 0; i < array.length; i++) {
        Node child = JAXBUtil.elementFromQName(qname, doc);
        node.appendChild(CharacterProperty.PRIMITIVE_PROPERTY.bindTo(binder, 
                                                                     child, 
                                                                     array[i], 
                                                                     qname));
      }
    }

    return node;
  }

}
