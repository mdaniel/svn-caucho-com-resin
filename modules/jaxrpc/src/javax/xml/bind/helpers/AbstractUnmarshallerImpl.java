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

/** XXX */
public abstract class AbstractUnmarshallerImpl implements Unmarshaller {

  /** XXX */
  protected boolean validating;

  public AbstractUnmarshallerImpl()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected UnmarshalException createUnmarshalException(SAXException e)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    throw new UnsupportedOperationException();
  }

  public AttachmentUnmarshaller getAttachmentUnmarshaller()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Listener getListener()
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
  protected XMLReader getXMLReader() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public boolean isValidating() throws JAXBException
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
  public void setAttachmentUnmarshaller(AttachmentUnmarshaller au)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setEventHandler(ValidationEventHandler handler) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setListener(Listener listener)
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
  public void setValidating(boolean validating) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final Object unmarshal(File f) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final Object unmarshal(InputSource source) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final Object unmarshal(InputStream is) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final Object unmarshal(Reader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Object unmarshal(Source source) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public final Object unmarshal(URL url) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Object unmarshal(XMLEventReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                        Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  protected abstract Object unmarshal(XMLReader reader, InputSource source) throws JAXBException;


  /** XXX */
  public Object unmarshal(XMLStreamReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader,
                                        Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

}

