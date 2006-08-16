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
import com.caucho.jaxb.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

import java.io.*;

import javax.xml.stream.*;

/**
 * represents a property in a skeleton; requires an Accessor to access it
 */
public abstract class Property {

  protected Accessor _accessor;

  public Property(Accessor accessor) {
    this._accessor = accessor;
  }

  public abstract Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException;
  
  public abstract void write(Marshaller m, XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException;

  public void writeStartElement(XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {

    XmlElementWrapper wrapper =
      (XmlElementWrapper)_accessor.getAnnotation(XmlElementWrapper.class);
    XmlElement element =
      (XmlElement)_accessor.getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (obj == null && !wrapper.nillable())
        return;
      if (wrapper.name().equals("##default"))
        out.writeStartElement(getName());
      else if (wrapper.namespace().equals("##default"))
        out.writeStartElement(wrapper.name());
      else
        out.writeStartElement(wrapper.namespace(), wrapper.name());
      if (obj == null)
        out.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance",
                           "nil", "true");
    }
    else if (element != null) {
      if (obj == null && !element.nillable())
        return;
      if (element.name().equals("##default"))
        out.writeStartElement(getName());
      else if (element.namespace().equals("##default"))
        out.writeStartElement(element.name());
      else
        out.writeStartElement(element.namespace(), element.name());
      if (obj == null)
        out.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance",
                           "nil", "true");
    }
    else {
      if (obj == null) return;
      out.writeStartElement(getQName().getLocalPart());
    }
  }

  public void writeEndElement(XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {

    XmlElementWrapper wrapper =
      (XmlElementWrapper)_accessor.getAnnotation(XmlElementWrapper.class);
    XmlElement element =
      (XmlElement)_accessor.getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (obj == null && !wrapper.nillable())
        return;
    } else if (element != null) {
      if (obj == null && !element.nillable())
        return;
    } else {
      if (obj == null) return;
    }
    out.writeEndElement();
  }

  public QName getQName()
  {
    XmlElementWrapper wrapper =
      (XmlElementWrapper)_accessor.getAnnotation(XmlElementWrapper.class);
    XmlElement element =
      (XmlElement)_accessor.getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (wrapper.name().equals("##default"))
        return _accessor.getQName();
      else if (wrapper.namespace().equals("##default"))
        return new QName(wrapper.name());
      else
        return new QName(wrapper.namespace(), wrapper.name());
    }
    else if (element != null) {
      if (element.name().equals("##default"))
        return _accessor.getQName();
      else if (element.namespace().equals("##default"))
        return new QName(element.name());
      else
        return new QName(element.namespace(), element.name());
    }

    return _accessor.getQName();
  }

  public Object get(Object target)
    throws JAXBException
  {
    return _accessor.get(target);
  }

  public void set(Object target, Object value)
    throws JAXBException
  {
    _accessor.set(target, value);
  }

  public String getName()
  {
    return _accessor.getName();
  }
}
