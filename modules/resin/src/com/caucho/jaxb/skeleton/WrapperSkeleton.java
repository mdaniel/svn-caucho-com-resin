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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import com.caucho.jaxb.JAXBContextImpl;

public class WrapperSkeleton extends Skeleton {
  private static final Logger log 
    = Logger.getLogger(WrapperSkeleton.class.getName());

  public WrapperSkeleton(JAXBContextImpl context, 
                         QName typeName, 
                         List<QName> names,
                         List<Class> wrapped)
    throws JAXBException
  {
    super(context);

    try {
      _typeName = typeName;

      for (int i = 0; i < wrapped.size(); i++) {
        Accessor a = new Accessor.WrapperListAccessor(context, 
                                                      names.get(i), 
                                                      wrapped.get(i));
        Property p = _context.createProperty(a);
        
        _elementProperties.put(p.getName(), p);
      }
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    Map<String,Object> ret = new HashMap<String,Object>();

    in.next();

    while (in.getEventType() != -1) {
      if (in.getEventType() == in.START_ELEMENT) {
        Property prop = getProperty(in.getName());
        Object val = prop.read(u, in);
        prop.set(ret, val);
      } 
      else if (in.getEventType() == in.END_ELEMENT) {
        in.next();
        break;
      }
      in.next();
    }

    return ret;
  }
  
  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, QName fieldName)
    throws IOException, XMLStreamException, JAXBException
    {
      // ignore fieldName... we know what we're doing.

      if (_typeName.getNamespaceURI() == null ||
          _typeName.getNamespaceURI().equals(""))
        out.writeStartElement(_typeName.getLocalPart());
      else
        out.writeStartElement(_typeName.getNamespaceURI(),
                              _typeName.getLocalPart());

      for(Property p : _elementProperties.values())
        p.write(m, out, p.get(obj));

      out.writeEndElement();
    }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    out.writeStartElement(XML_SCHEMA_PREFIX, "complexType", XML_SCHEMA_NS);
    out.writeAttribute("name", _typeName.toString());

    out.writeStartElement(XML_SCHEMA_PREFIX, "sequence", XML_SCHEMA_NS);

    for (Property property : _elementProperties.values())
      property.generateSchema(out);

    out.writeEndElement(); // sequence

    out.writeEndElement(); // complexType
  }
}
