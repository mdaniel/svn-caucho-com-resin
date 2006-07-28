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
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;
import java.net.*;
import com.caucho.jaxb.adapters.*;

public class UnmarshallerImpl implements Unmarshaller {

  private JAXBContext _context;

  UnmarshallerImpl(JAXBContext context)
  {
    this._context = context;
  }

  /**
   * Gets the adapter associated with the specified type. This is the reverse
   * operation of the method.
   */
  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    throw new UnsupportedOperationException();
  }

  public AttachmentUnmarshaller getAttachmentUnmarshaller()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Return the current event handler or the default event handler if one
   * hasn't been set.
   */
  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Return Unmarshaller.Listener registered with this Unmarshaller.
   */
  public Listener getListener()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the particular property in the underlying implementation of
   * Unmarshaller. This method can only be used to get one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * get an undefined property will result in a PropertyException being thrown.
   * See .
   */
  public Object getProperty(String name) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the JAXP 1.3 object being used to perform unmarshal-time validation.
   * If there is no Schema set on the unmarshaller, then this method will
   * return null indicating that unmarshal-time validation will not be
   * performed. This method provides replacement functionality for the
   * deprecated isValidating() API as well as access to the Schema object. To
   * determine if the Unmarshaller has validation enabled, simply test the
   * return type for null: boolean isValidating = u.getSchema()!=null;
   */
  public Schema getSchema()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Get an unmarshaller handler object that can be used as a component in an
   * XML pipeline. The JAXB Provider can return the same handler object for
   * multiple invocations of this method. In other words, this method does not
   * necessarily create a new instance of UnmarshallerHandler. If the
   * application needs to use more than one UnmarshallerHandler, it should
   * create more than one Unmarshaller.
   */
  public UnmarshallerHandler getUnmarshallerHandler()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deprecated. Indicates whether or not the Unmarshaller is configured to
   * validate during unmarshal operations. This API returns the state of the
   * JAXB Provider's default unmarshal-time validation mechanism. This method
   * is deprecated as of JAXB 2.0 - please use the new getSchema() API.
   */
  public boolean isValidating() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Associates a configured instance of with this unmarshaller. Every
   * unmarshaller internally maintains a MapClass,XmlAdapter>, which it uses
   * for unmarshalling classes whose fields/methods are annotated with
   * XmlJavaTypeAdapter. This method allows applications to use a configured
   * instance of XmlAdapter. When an instance of an adapter is not given, an
   * unmarshaller will create one by invoking its default constructor.
   */
  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Associates a configured instance of with this unmarshaller. This is a
   * convenience method that invokes setAdapter(adapter.getClass(),adapter);.
   */
  public void setAdapter(XmlAdapter adapter)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Associate a context that resolves cid's, content-id URIs, to binary data
   * passed as attachments. Unmarshal time validation, enabled via
   * setSchema(Schema), must be supported even when unmarshaller is performing
   * XOP processing.
   */
  public void setAttachmentUnmarshaller(AttachmentUnmarshaller au)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Allow an application to register a ValidationEventHandler. The
   * ValidationEventHandler will be called by the JAXB Provider if any
   * validation errors are encountered during calls to any of the unmarshal
   * methods. If the client application does not register a
   * ValidationEventHandler before invoking the unmarshal methods, then
   * ValidationEvents will be handled by the default event handler which will
   * terminate the unmarshal operation after the first error or fatal error is
   * encountered. Calling this method with a null parameter will cause the
   * Unmarshaller to revert back to the default event handler.
   */
  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Register unmarshal event callback Unmarshaller.Listener with this
   * Unmarshaller. There is only one Listener per Unmarshaller. Setting a
   * Listener replaces the previous set Listener. One can unregister current
   * Listener by setting listener to null.
   */
  public void setListener(Listener listener)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Set the particular property in the underlying implementation of
   * Unmarshaller. This method can only be used to set one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * set an undefined property will result in a PropertyException being thrown.
   * See .
   */
  public void setProperty(String name, Object value) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Specify the JAXP 1.3 object that should be used to validate subsequent
   * unmarshal operations against. Passing null into this method will disable
   * validation. This method replaces the deprecated setValidating(boolean)
   * API. Initially this property is set to null.
   */
  public void setSchema(Schema schema)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deprecated. Specifies whether or not the default validation mechanism of
   * the Unmarshaller should validate during unmarshal operations. By default,
   * the Unmarshaller does not validate. This method may only be invoked before
   * or after calling one of the unmarshal methods. This method only controls
   * the JAXB Provider's default unmarshal-time validation mechanism - it has
   * no impact on clients that specify their own validating SAX 2.0 compliant
   * parser. Clients that specify their own unmarshal-time validation mechanism
   * may wish to turn off the JAXB Provider's default validation mechanism via
   * this API to avoid "double validation". This method is deprecated as of
   * JAXB 2.0 - please use the new setSchema(javax.xml.validation.Schema) API.
   */
  public void setValidating(boolean validating) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified file and return the resulting
   * content tree. Implements Unmarshal Global Root Element.
   */
  public Object unmarshal(File f) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified SAX InputSource and return the
   * resulting content tree. Implements Unmarshal Global Root Element.
   */
  public Object unmarshal(InputSource source) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified InputStream and return the resulting
   * content tree. Validation event location information may be incomplete when
   * using this form of the unmarshal API. Implements Unmarshal Global Root
   * Element.
   */
  public Object unmarshal(InputStream is) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal global XML data from the specified DOM tree and return the
   * resulting content tree. Implements Unmarshal Global Root Element.
   */
  public Object unmarshal(Node node) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data by JAXB mapped declaredType and return the resulting
   * content tree. Implements Unmarshal by Declared Type
   */
  public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified Reader and return the resulting
   * content tree. Validation event location information may be incomplete when
   * using this form of the unmarshal API, because a Reader does not provide
   * the system ID. Implements Unmarshal Global Root Element.
   */
  public Object unmarshal(Reader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified XML Source and return the resulting
   * content tree. Implements Unmarshal Global Root Element. SAX 2.0 Parser
   * Pluggability A client application can choose not to use the default parser
   * mechanism supplied with their JAXB provider. Any SAX 2.0 compliant parser
   * can be substituted for the JAXB provider's default mechanism. To do so,
   * the client application must properly configure a SAXSource containing an
   * XMLReader implemented by the SAX 2.0 parser provider. If the XMLReader has
   * an org.xml.sax.ErrorHandler registered on it, it will be replaced by the
   * JAXB Provider so that validation errors can be reported via the
   * ValidationEventHandler mechanism of JAXB. If the SAXSource does not
   * contain an XMLReader, then the JAXB provider's default parser mechanism
   * will be used. This parser replacement mechanism can also be used to
   * replace the JAXB provider's unmarshal-time validation engine. The client
   * application must properly configure their SAX 2.0 compliant parser to
   * perform validation (as shown in the example above). Any
   * SAXParserExceptions encountered by the parser during the unmarshal
   * operation will be processed by the JAXB provider and converted into JAXB
   * ValidationEvent objects which will be reported back to the client via the
   * ValidationEventHandler registered with the Unmarshaller. Note: specifying
   * a substitute validating SAX 2.0 parser for unmarshalling does not
   * necessarily replace the validation engine used by the JAXB provider for
   * performing on-demand validation. The only way for a client application to
   * specify an alternate parser mechanism to be used during unmarshal is via
   * the unmarshal(SAXSource) API. All other forms of the unmarshal method
   * (File, URL, Node, etc) will use the JAXB provider's default parser and
   * validator mechanisms.
   */
  public Object unmarshal(Source source) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified XML Source by declaredType and
   * return the resulting content tree. Implements Unmarshal by Declared Type
   * See SAX 2.0 Parser Pluggability
   */
  public <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified URL and return the resulting content
   * tree. Implements Unmarshal Global Root Element.
   */
  public Object unmarshal(URL url) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified pull parser and return the resulting
   * content tree. This method is an Unmarshal Global Root method. This method
   * assumes that the parser is on a START_DOCUMENT or START_ELEMENT event.
   * Unmarshalling will be done from this start event to the corresponding end
   * event. If this method returns successfully, the reader will be pointing at
   * the token right after the end event.
   */
  public Object unmarshal(XMLEventReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal root element to JAXB mapped declaredType and return the
   * resulting content tree. This method implements unmarshal by declaredType.
   * This method assumes that the parser is on a START_DOCUMENT or
   * START_ELEMENT event. Unmarshalling will be done from this start event to
   * the corresponding end event. If this method returns successfully, the
   * reader will be pointing at the token right after the end event.
   */
  public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                        Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal XML data from the specified pull parser and return the resulting
   * content tree. Implements Unmarshal Global Root Element. This method
   * assumes that the parser is on a START_DOCUMENT or START_ELEMENT event.
   * Unmarshalling will be done from this start event to the corresponding end
   * event. If this method returns successfully, the reader will be pointing at
   * the token right after the end event.
   */
  public Object unmarshal(XMLStreamReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshal root element to JAXB mapped declaredType and return the
   * resulting content tree. This method implements unmarshal by declaredType.
   * This method assumes that the parser is on a START_DOCUMENT or
   * START_ELEMENT event. Unmarshalling will be done from this start event to
   * the corresponding end event. If this method returns successfully, the
   * reader will be pointing at the token right after the end event.
   */
  public <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader,
                                        Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

}
