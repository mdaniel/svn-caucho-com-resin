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
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.bind.attachment.*;
import javax.xml.bind.Unmarshaller.*;
import javax.xml.validation.*;
import javax.xml.bind.annotation.adapters.*;
import java.net.*;

/** XXX */
public interface Unmarshaller {

  /** XXX */
  abstract <A extends XmlAdapter> A getAdapter(Class<A> type);

  abstract AttachmentUnmarshaller getAttachmentUnmarshaller();


  /** XXX */
  abstract ValidationEventHandler getEventHandler() throws JAXBException;


  /** XXX */
  abstract Listener getListener();


  /** XXX */
  abstract Object getProperty(String name) throws PropertyException;


  /** XXX */
  abstract Schema getSchema();


  /** XXX */
  abstract UnmarshallerHandler getUnmarshallerHandler();


  /** XXX */
  abstract boolean isValidating() throws JAXBException;


  /** XXX */
  abstract <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter);

  /** XXX */
  abstract void setAdapter(XmlAdapter adapter);


  /** XXX */
  abstract void setAttachmentUnmarshaller(AttachmentUnmarshaller au);


  /** XXX */
  abstract void setEventHandler(ValidationEventHandler handler) throws JAXBException;


  /** XXX */
  abstract void setListener(Listener listener);


  /** XXX */
  abstract void setProperty(String name, Object value) throws PropertyException;


  /** XXX */
  abstract void setSchema(Schema schema);


  /** XXX */
  abstract void setValidating(boolean validating) throws JAXBException;


  /** XXX */
  abstract Object unmarshal(File f) throws JAXBException;


  /** XXX */
  abstract Object unmarshal(InputSource source) throws JAXBException;


  /** XXX */
  abstract Object unmarshal(InputStream is) throws JAXBException;


  /** XXX */
  abstract Object unmarshal(Node node) throws JAXBException;


  /** XXX */
  abstract <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException;


  /** XXX */
  abstract Object unmarshal(Reader reader) throws JAXBException;


  /** XXX */
  abstract Object unmarshal(Source source) throws JAXBException;


  /** XXX */
  abstract <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException;

  /** XXX */
  abstract Object unmarshal(URL url) throws JAXBException;


  /** XXX */
  abstract Object unmarshal(XMLEventReader reader) throws JAXBException;


  /** XXX */
  abstract <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                        Class<T> declaredType)
      throws JAXBException;


  /** XXX */
  abstract Object unmarshal(XMLStreamReader reader) throws JAXBException;


  /** XXX */
  abstract <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader,
                                        Class<T> declaredType)
      throws JAXBException;

  /** XXX */
  public static abstract class Listener {
    public Listener()
    {
      throw new UnsupportedOperationException();
    }


    /** XXX */
    public void afterUnmarshal(Object target, Object parent)
    {
      throw new UnsupportedOperationException();
    }


    /** XXX */
    public void beforeUnmarshal(Object target, Object parent)
    {
      throw new UnsupportedOperationException();
    }

  }
}

