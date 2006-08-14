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
import javax.xml.transform.*;
import org.w3c.dom.*;
import java.io.*;
import javax.xml.bind.attachment.*;
import org.xml.sax.*;
import javax.xml.bind.Unmarshaller.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.bind.*;
import java.net.*;
import java.util.*;

public abstract class AbstractUnmarshallerImpl implements Unmarshaller {

  protected boolean validating;

  private AttachmentUnmarshaller _attachmentUnmarshaller = null;
  private ValidationEventHandler _validationEventHandler = null;
  private Listener _listener = null;
  private HashMap<String,Object> _properties = new HashMap<String,Object>();
  private Schema _schema = null;
  private XMLReader _xmlreader = null;
  private XmlAdapter _adapter = null;
  private UnmarshallerHandler _unmarshallerHandler = null;
  private HashMap<Class,XmlAdapter> _adapters =
    new HashMap<Class,XmlAdapter>();

  public AbstractUnmarshallerImpl()
  {
  }

  public UnmarshallerHandler getUnmarshallerHandler()
  {
    return _unmarshallerHandler;
  }

  public void setUnmarshallerHandler(UnmarshallerHandler u)
  {
    _unmarshallerHandler = u;
  }

  protected UnmarshalException createUnmarshalException(SAXException e)
  {
    return new UnmarshalException(e);
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    return (A)_adapters.get(type);
  }

  public AttachmentUnmarshaller getAttachmentUnmarshaller()
  {
    return _attachmentUnmarshaller;
  }

  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    return _validationEventHandler;
  }

  public Listener getListener()
  {
    return _listener;
  }

  public Object getProperty(String name) throws PropertyException
  {
    return _properties.get(name);
  }

  public Schema getSchema()
  {
    return _schema;
  }

  protected XMLReader getXMLReader() throws JAXBException
  {
    return _xmlreader;
  }

  public boolean isValidating() throws JAXBException
  {
    return validating;
  }

  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    _adapters.put(type, adapter);
  }

  public void setAdapter(XmlAdapter adapter)
  {
    setAdapter((Class)adapter.getClass(), adapter);
  }

  public void setAttachmentUnmarshaller(AttachmentUnmarshaller au)
  {
    _attachmentUnmarshaller = au;
  }

  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    _validationEventHandler = handler;
  }

  public void setListener(Listener listener)
  {
    _listener = listener;
  }

  public void setProperty(String name, Object value) throws PropertyException
  {
    _properties.put(name, value);
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;
  }

  public void setValidating(boolean validating) throws JAXBException
  {
    this.validating = validating;
  }

  public final Object unmarshal(File f) throws JAXBException
  {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(f);
      return unmarshal(fis);
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
    finally {
      try {
        if (fis != null)
          fis.close();
      }
      catch (IOException e) {
        throw new JAXBException(e);
      }
    }
  }

  public final Object unmarshal(InputSource source) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }
  
  public final Object unmarshal(InputStream is) throws JAXBException
  {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      return unmarshal(factory.createXMLStreamReader(is));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public final Object unmarshal(Reader reader) throws JAXBException
  {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      return unmarshal(factory.createXMLStreamReader(reader));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public Object unmarshal(Source source) throws JAXBException
  {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      return unmarshal(factory.createXMLStreamReader(source));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException
  {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      return unmarshal(factory.createXMLStreamReader(node), declaredType);
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public final Object unmarshal(URL url) throws JAXBException
  {
    try {
      return unmarshal(url.openStream());
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
  }

  public Object unmarshal(XMLEventReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                      Class<T> declaredType)
    throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader,
                                      Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public Object unmarshal(XMLStreamReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  protected abstract Object unmarshal(XMLReader reader, InputSource source)
    throws JAXBException;
}

