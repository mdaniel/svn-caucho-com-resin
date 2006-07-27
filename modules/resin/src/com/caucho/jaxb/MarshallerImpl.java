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
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.bind.attachment.*;
import javax.xml.bind.Unmarshaller.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.adapters.*;
import java.net.*;
import com.caucho.jaxb.adapters.*;

public class MarshallerImpl implements Marshaller {

  private static HashMap<Class<?>, Class<? extends XmlAdapter>> _adapters =
    new HashMap<Class<?>, Class<? extends XmlAdapter>>();

  static {
    _adapters.put(HashMap.class, HashMapAdapter.class);
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
    throw new UnsupportedOperationException();
  }

  public ValidationEventHandler getEventHandler()
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Listener getListener()
  {
    throw new UnsupportedOperationException();
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

  /**
   * Get the particular property in the underlying implementation of
   * Marshaller. This method can only be used to get one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * get an undefined property will result in a PropertyException being thrown.
   * See .
   */
  public Object getProperty(String name)
    throws PropertyException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the JAXP 1.3 object being used to perform marshal-time validation. If
   * there is no Schema set on the marshaller, then this method will return
   * null indicating that marshal-time validation will not be performed.
   */
  public Schema getSchema()
  {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
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

  /**
   * Associate a context that enables binary data within an XML document to be
   * transmitted as XML-binary optimized attachment. The attachment is
   * referenced from the XML document content model by content-id URIs(cid)
   * references stored within the xml document.
   */
  public void setAttachmentMarshaller(AttachmentMarshaller am)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Allow an application to register a validation event handler. The
   * validation event handler will be called by the JAXB Provider if any
   * validation errors are encountered during calls to any of the marshal
   * API's. If the client application does not register a validation event
   * handler before invoking one of the marshal methods, then validation events
   * will be handled by the default event handler which will terminate the
   * marshal operation after the first error or fatal error is encountered.
   * Calling this method with a null parameter will cause the Marshaller to
   * revert back to the default default event handler.
   */
  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Register marshal event callback Marshaller.Listener with this Marshaller.
   * There is only one Listener per Marshaller. Setting a Listener replaces the
   * previous set Listener. One can unregister current Listener by setting
   * listener to null.
   */
  public void setListener(Listener listener)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the particular property in the underlying implementation of
   * Marshaller. This method can only be used to set one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * set an undefined property will result in a PropertyException being thrown.
   * See .
   */
  public void setProperty(String name, Object value)
    throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Specify the JAXP 1.3 object that should be used to validate subsequent
   * marshal operations against. Passing null into this method will disable
   * validation. This method allows the caller to validate the marshalled XML
   * as it's marshalled. Initially this property is set to null.
   */
  public void setSchema(Schema schema)
  {
    throw new UnsupportedOperationException();
  }

}

