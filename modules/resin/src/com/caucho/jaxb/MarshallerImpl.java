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

package com.caucho.jaxb;
import javax.xml.bind.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import java.io.*;
import java.math.*;
import org.xml.sax.*;
import javax.xml.bind.attachment.*;
import javax.xml.bind.helpers.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;
import java.net.*;
import com.caucho.jaxb.skeleton.*;
import com.caucho.jaxb.adapters.*;
import javax.xml.bind.MarshalException;

public class MarshallerImpl extends AbstractMarshallerImpl {

  private JAXBContextImpl _context;

  MarshallerImpl(JAXBContextImpl context)
  {
    this._context = context;
  }

  /**
   * Marshal the content tree rooted at jaxbElement into a .
   */
  public void marshal(Object jaxbElement, XMLStreamWriter writer)
    throws JAXBException
  {
    Class c = jaxbElement.getClass();

    if (!_context.createJAXBIntrospector().isElement(jaxbElement) &&
        !c.isAnnotationPresent(XmlRootElement.class))
      throw new MarshalException("JAXBIntrospector.isElement()==false");

    String name = null;

    XmlRootElement xre = (XmlRootElement)c.getAnnotation(XmlRootElement.class);
    if (xre != null)
      name = xre.name();

    XmlType xmlTypeAnnotation = (XmlType)c.getAnnotation(XmlType.class);
    if (name == null)
      name = xmlTypeAnnotation == null
        ? c.getName()
        : xmlTypeAnnotation.name();

    String encoding = getEncoding();
    if (encoding == null)
      encoding = "utf-8";

    try {
      if (!isFragment())
        writer.writeStartDocument("1.0", encoding);

      // XXX this needs to happen after the startElement is written
      // jaxb/5003
      /*
      if (getNoNSSchemaLocation() != null)
        writer.writeAttribute("xsi",
                              "http://www.w3.org/2001/XMLSchema-instance",
                              "noNamespaceSchemaLocation",
                              getNoNSSchemaLocation());
      */

      _context.getSkeleton(c).write(this, writer, jaxbElement, new QName(name));

    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public void marshal(Object obj, XMLEventWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void marshal(Object obj, Result result) throws JAXBException
  {
    try {
      XMLOutputFactory factory = XMLOutputFactory.newInstance();
      XMLStreamWriter out = factory.createXMLStreamWriter(result);

      marshal(obj, out);
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    A a = super.getAdapter(type);

    if (a == null)
      return (A)new BeanAdapter();

    return a;
  }
}

