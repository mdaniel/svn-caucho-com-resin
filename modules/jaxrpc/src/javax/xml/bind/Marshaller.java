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

package javax.xml.bind;
import javax.xml.stream.*;
import javax.xml.transform.*;
import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.bind.attachment.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.bind.Marshaller.*;

/** XXX */
public interface Marshaller {

  /** XXX */
  static final String JAXB_ENCODING="jaxb.encoding";


  /** XXX */
  static final String JAXB_FORMATTED_OUTPUT="jaxb.formatted.output";


  /** XXX */
  static final String JAXB_FRAGMENT="jaxb.fragment";


  /** XXX */
  static final String JAXB_NO_NAMESPACE_SCHEMA_LOCATION="jaxb.noNamespaceSchemaLocation";


  /** XXX */
  static final String JAXB_SCHEMA_LOCATION="jaxb.schemaLocation";


  /** XXX */
  abstract <A extends XmlAdapter> A getAdapter(Class<A> type);

  abstract AttachmentMarshaller getAttachmentMarshaller();


  /** XXX */
  abstract ValidationEventHandler getEventHandler() throws JAXBException;


  /** XXX */
  abstract Listener getListener();


  /** XXX */
  abstract Node getNode(Object contentTree) throws JAXBException;


  /** XXX */
  abstract Object getProperty(String name) throws PropertyException;


  /** XXX */
  abstract Schema getSchema();


  /** XXX */
  abstract void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException;


  /** XXX */
  abstract void marshal(Object jaxbElement, Node node) throws JAXBException;


  /** XXX */
  abstract void marshal(Object jaxbElement, OutputStream os) throws JAXBException;


  /** XXX */
  abstract void marshal(Object jaxbElement, Result result) throws JAXBException;


  /** XXX */
  abstract void marshal(Object jaxbElement, Writer writer) throws JAXBException;


  /** XXX */
  abstract void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException;


  /** XXX */
  abstract void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException;


  /** XXX */
  abstract <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter);


  /** XXX */
  abstract void setAdapter(XmlAdapter adapter);


  /** XXX */
  abstract void setAttachmentMarshaller(AttachmentMarshaller am);


  /** XXX */
  abstract void setEventHandler(ValidationEventHandler handler) throws JAXBException;


  /** XXX */
  abstract void setListener(Listener listener);


  /** XXX */
  abstract void setProperty(String name, Object value) throws PropertyException;


  /** XXX */
  abstract void setSchema(Schema schema);


  /** XXX */
  public static abstract class Listener {
    public Listener()
    {
      throw new UnsupportedOperationException();
    }


    /** XXX */
    public void afterMarshal(Object source)
    {
      throw new UnsupportedOperationException();
    }


    /** XXX */
    public void beforeMarshal(Object source)
    {
      throw new UnsupportedOperationException();
    }

  }
}

