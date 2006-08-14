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
import java.util.*;
import javax.xml.bind.attachment.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.bind.*;
import javax.xml.bind.Marshaller.*;

public abstract class AbstractMarshallerImpl implements Marshaller {

  private AttachmentMarshaller _attachmentMarshaller;
  private String _encoding;
  private ValidationEventHandler _validationEventHandler;
  private Listener _listener;
  private boolean _formattedOutput = false;
  private boolean _fragment = false;
  private String _noNSSchemaLocation;
  private Schema _schema;
  private String _schemaLocation;
  private HashMap<String,Object> _properties =
    new HashMap<String,Object>();
  private HashMap<Class,XmlAdapter> _adapters =
    new HashMap<Class,XmlAdapter>();

  public AbstractMarshallerImpl()
  {
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    return (A)_adapters.get(type);
  }

  public AttachmentMarshaller getAttachmentMarshaller()
  {
    return _attachmentMarshaller;
  }

  protected String getEncoding()
  {
    return _encoding;
  }

  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    return _validationEventHandler;
  }

  protected String getJavaEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    return encoding;
  }

  public Listener getListener()
  {
    return _listener;
  }

  public Node getNode(Object obj) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  protected String getNoNSSchemaLocation()
  {
    return _noNSSchemaLocation;
  }

  public Object getProperty(String name)
    throws PropertyException
  {
    return _properties.get(name);
  }

  public Schema getSchema()
  {
    return _schema;
  }

  protected String getSchemaLocation()
  {
    return _schemaLocation;
  }

  protected boolean isFormattedOutput()
  {
    return _formattedOutput;
  }

  protected boolean isFragment()
  {
    return _fragment;
  }

  public final void marshal(Object obj, ContentHandler handler)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public final void marshal(Object obj, Node node) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public final void marshal(Object obj, OutputStream os) throws JAXBException
  {
    try {
      XMLOutputFactory factory = XMLOutputFactory.newInstance();
      marshal(obj, factory.createXMLStreamWriter(os));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public final void marshal(Object obj, Writer w) throws JAXBException
  {
    try {
      XMLOutputFactory factory = XMLOutputFactory.newInstance();
      marshal(obj, factory.createXMLStreamWriter(w));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    _adapters.put(type, adapter);
  }

  public void setAdapter(XmlAdapter adapter)
  {
    _adapters.put((Class)adapter.getClass(), adapter);
  }

  public void setAttachmentMarshaller(AttachmentMarshaller am)
  {
    _attachmentMarshaller = am;
  }

  protected void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    _validationEventHandler = handler;
  }

  protected void setFormattedOutput(boolean v)
  {
    _formattedOutput = v;
  }

  protected void setFragment(boolean v)
  {
    _fragment = v;
  }

  public void setListener(Listener listener)
  {
    _listener = listener;
  }

  protected void setNoNSSchemaLocation(String location)
  {
    _noNSSchemaLocation = location;
  }

  public void setProperty(String name, Object value)
    throws PropertyException
  {
    _properties.put(name, value);
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;
  }

  protected void setSchemaLocation(String location)
  {
    _schemaLocation = location;
  }

  public void marshal(Object obj, XMLEventWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public void marshal(Object obj, XMLStreamWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }
}

