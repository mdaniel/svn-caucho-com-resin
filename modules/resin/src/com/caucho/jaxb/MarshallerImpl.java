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
import javax.xml.bind.Unmarshaller.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;
import java.net.*;
import com.caucho.jaxb.marshall.*;
import com.caucho.jaxb.adapters.*;

public class MarshallerImpl implements Marshaller {

  private static HashMap<Class<?>, Class<? extends XmlAdapter>> _adapters =
    new HashMap<Class<?>, Class<? extends XmlAdapter>>();

  static {
    _adapters.put(HashMap.class, HashMapAdapter.class);
  }

  private JAXBContext _context;
  private Listener _listener = null;

  MarshallerImpl(JAXBContext context)
  {
    this._context = context;
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    Class<? extends XmlAdapter> c = _adapters.get(type);

    // XXX: try superclasses/interfaces?
    if (c == null)
      return (A)new BeanAdapter();

    try {
      return (A)c.newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public AttachmentMarshaller getAttachmentMarshaller()
  {
    throw
      new UnsupportedOperationException("binary attachments not yet supported");
  }

  public ValidationEventHandler getEventHandler()
    throws JAXBException
  {
    throw
      new UnsupportedOperationException("schema validation not yet supported");
  }

  public Listener getListener()
  {
    return _listener;
  }

  /**
   * Get a DOM tree view of the content tree(Optional). If the returned DOM
   * tree is updated, these changes are also visible in the content tree. Use
   * to force a deep copy of the content tree to a DOM representation.
   */
  public Node getNode(Object contentTree)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Object getProperty(String name)
    throws PropertyException
  {
    // Caucho does not define any properties
    return null;
  }

  public Schema getSchema()
  {
    throw
      new UnsupportedOperationException("schema validation not yet supported");
  }

  /**
   * Marshal the content tree rooted at jaxbElement into SAX2 events.
   */
  public void marshal(Object jaxbElement, org.xml.sax.ContentHandler handler)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Marshal the content tree rooted at jaxbElement into a DOM tree.
   */
  public void marshal(Object jaxbElement, Node node)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Marshal the content tree rooted at jaxbElement into an output stream.
   */
  public void marshal(Object jaxbElement, OutputStream os)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Marshal the content tree rooted at jaxbElement into the specified
   * javax.xml.transform.Result. All JAXB Providers must at least support
   * DOMResult, SAXResult, and StreamResult. It can support other derived
   * classes of Result as well.
   */
  public void marshal(Object jaxbElement, Result result)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Marshal the content tree rooted at jaxbElement into a Writer.
   */
  public void marshal(Object jaxbElement, Writer writer)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Marshal the content tree rooted at jaxbElement into a .
   */
  public void marshal(Object jaxbElement, XMLEventWriter writer)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Marshal the content tree rooted at jaxbElement into a .
   */
  public void marshal(Object jaxbElement, XMLStreamWriter writer)
    throws JAXBException
  {
    Class c = jaxbElement.getClass();
    XmlType xmlTypeAnnotation = (XmlType)c.getAnnotation(XmlType.class);
    String name = xmlTypeAnnotation == null
      ? c.getName()
      : xmlTypeAnnotation.name();
    Marshall marshall = getMarshall(c);
    try {
      marshall.serialize(writer, jaxbElement, new QName(name));
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public static Marshall getMarshall(Class type)
  {
    if (String.class.equals(type))
      return StringMarshall.MARSHALL;

    if (Map.class.equals(type))
      return MapMarshall.MARSHALL;

    if (Double.class.equals(type) || Double.TYPE.equals(type))
      return DoubleMarshall.MARSHALL;

    if (Float.class.equals(type) || Float.TYPE.equals(type))
      return FloatMarshall.MARSHALL;

    if (Integer.class.equals(type) || Integer.TYPE.equals(type))
      return IntMarshall.MARSHALL;

    if (Long.class.equals(type) || Long.TYPE.equals(type))
      return LongMarshall.MARSHALL;

    if (BigDecimal.class.equals(type))
      return BigDecimalMarshall.MARSHALL;

    if (List.class.equals(type))
      return ListMarshall.MARSHALL;

    if (Date.class.equals(type))
      return DateMarshall.MARSHALL;

    if (byte[].class.equals(type))
      return ByteArrayMarshall.MARSHALL;

    if (Object[].class.isAssignableFrom(type))
      return ArrayMarshall.MARSHALL;

    throw new UnsupportedOperationException(type.getName());
  }

  /**
   * Associates a configured instance of with this marshaller. Every marshaller
   * internally maintains a MapClass,XmlAdapter>, which it uses for marshalling
   * classes whose fields/methods are annotated with XmlJavaTypeAdapter. This
   * method allows applications to use a configured instance of XmlAdapter.
   * When an instance of an adapter is not given, a marshaller will create one
   * by invoking its default constructor.
   */
  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Associates a configured instance of with this marshaller. This is a
   * convenience method that invokes setAdapter(adapter.getClass(),adapter);.
   */
  public void setAdapter(XmlAdapter adapter)
  {
    throw new UnsupportedOperationException();
  }

  public void setAttachmentMarshaller(AttachmentMarshaller am)
  {
    throw
      new UnsupportedOperationException("binary attachments not yet supported");
  }

  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    throw
      new UnsupportedOperationException("schema validation not yet supported");
  }

  public void setListener(Listener listener)
  {
    _listener = listener;
  }

  public void setProperty(String name, Object value)
    throws PropertyException
  {
    // Caucho does not define any properties
  }

  public void setSchema(Schema schema)
  {
    throw
      new UnsupportedOperationException("schema validation not yet supported");
  }

}

