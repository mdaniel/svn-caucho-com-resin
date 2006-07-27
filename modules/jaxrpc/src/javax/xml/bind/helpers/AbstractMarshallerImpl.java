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

package javax.xml.bind.helpers;
import javax.xml.stream.*;
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.bind.attachment.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.bind.*;
import javax.xml.bind.Marshaller.*;

/** XXX */
public abstract class AbstractMarshallerImpl implements Marshaller {
  public AbstractMarshallerImpl()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    throw new UnsupportedOperationException();
  }

  public AttachmentMarshaller getAttachmentMarshaller()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected String getEncoding()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected String getJavaEncoding(String encoding) throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Listener getListener()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Node getNode(Object obj) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected String getNoNSSchemaLocation()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Object getProperty(String name) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Schema getSchema()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected String getSchemaLocation()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected boolean isFormattedOutput()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected boolean isFragment()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final void marshal(Object obj, ContentHandler handler) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final void marshal(Object obj, Node node) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final void marshal(Object obj, OutputStream os) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final void marshal(Object obj, Writer w) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void marshal(Object obj, XMLEventWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void marshal(Object obj, XMLStreamWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setAdapter(XmlAdapter adapter)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setAttachmentMarshaller(AttachmentMarshaller am)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected void setEncoding(String encoding)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setEventHandler(ValidationEventHandler handler) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected void setFormattedOutput(boolean v)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected void setFragment(boolean v)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setListener(Listener listener)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected void setNoNSSchemaLocation(String location)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setProperty(String name, Object value) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setSchema(Schema schema)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected void setSchemaLocation(String location)
  {
    throw new UnsupportedOperationException();
  }

}

