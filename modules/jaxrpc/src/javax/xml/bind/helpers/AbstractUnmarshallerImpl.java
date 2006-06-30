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

/**
 * Partial default Unmarshaller implementation. This class provides a partial
 * default implementation for the Unmarshallerinterface. A JAXB Provider has to
 * implement five methods (getUnmarshallerHandler, unmarshal(Node),
 * unmarshal(XMLReader,InputSource), unmarshal(XMLStreamReader), and
 * unmarshal(XMLEventReader). Since: JAXB1.0 Version: $Revision: 1.14 $ $Date:
 * 2006/03/08 17:01:00 $ Author: Kohsuke Kawaguchi, Sun Microsystems, Inc. See
 * Also:Unmarshaller
 */
public abstract class AbstractUnmarshallerImpl implements Unmarshaller {

  /**
   * whether or not the unmarshaller will validate
   */
  protected boolean validating;

  public AbstractUnmarshallerImpl()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates an UnmarshalException from a SAXException. This is an utility
   * method provided for the derived classes. When a provider-implemented
   * ContentHandler wants to throw a JAXBException, it needs to wrap the
   * exception by a SAXException. If the unmarshaller implementation blindly
   * wrap SAXException by JAXBException, such an exception will be a
   * JAXBException wrapped by a SAXException wrapped by another JAXBException.
   * This is silly. This method checks the nested exception of SAXException and
   * reduce those excessive wrapping.
   */
  protected UnmarshalException createUnmarshalException(SAXException e)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Gets the adapter associated with the
   * specified type. This is the reverse operation of the method.
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
   * Description copied from interface: Return Unmarshaller.Listener registered
   * with this Unmarshaller.
   */
  public Listener getListener()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Default implementation of the getProperty method always throws
   * PropertyException since there are no required properties. If a provider
   * needs to handle additional properties, it should override this method in a
   * derived class.
   */
  public Object getProperty(String name) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the JAXP 1.3 object being used to
   * perform unmarshal-time validation. If there is no Schema set on the
   * unmarshaller, then this method will return null indicating that
   * unmarshal-time validation will not be performed. This method provides
   * replacement functionality for the deprecated Unmarshaller.isValidating()
   * API as well as access to the Schema object. To determine if the
   * Unmarshaller has validation enabled, simply test the return type for null:
   * boolean isValidating = u.getSchema()!=null;
   */
  public Schema getSchema()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Obtains a configured XMLReader. This method is used when the
   * client-specified object doesn't have XMLReader. is not re-entrant, so we
   * will only use one instance of XMLReader.
   */
  protected XMLReader getXMLReader() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Indicates whether or not the Unmarshaller is configured to validate during
   * unmarshal operations. Note: I named this method isValidating() to stay
   * in-line with JAXP, as opposed to naming it getValidating().
   */
  public boolean isValidating() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Associates a configured instance of
   * with this unmarshaller. Every unmarshaller internally maintains a
   * MapClass,XmlAdapter>, which it uses for unmarshalling classes whose
   * fields/methods are annotated with XmlJavaTypeAdapter. This method allows
   * applications to use a configured instance of XmlAdapter. When an instance
   * of an adapter is not given, an unmarshaller will create one by invoking
   * its default constructor.
   */
  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Associates a configured instance of
   * with this unmarshaller. This is a convenience method that invokes
   * setAdapter(adapter.getClass(),adapter);.
   */
  public void setAdapter(XmlAdapter adapter)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Associate a context that resolves
   * cid's, content-id URIs, to binary data passed as attachments. Unmarshal
   * time validation, enabled via Unmarshaller.setSchema(Schema), must be
   * supported even when unmarshaller is performing XOP processing.
   */
  public void setAttachmentUnmarshaller(AttachmentUnmarshaller au)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Allow an application to register a validation event handler. The
   * validation event handler will be called by the JAXB Provider if any
   * validation errors are encountered during calls to any of the unmarshal
   * methods. If the client application does not register a validation event
   * handler before invoking the unmarshal methods, then all validation events
   * will be silently ignored and may result in unexpected behaviour.
   */
  public void setEventHandler(ValidationEventHandler handler) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Register unmarshal event callback
   * Unmarshaller.Listener with this Unmarshaller. There is only one Listener
   * per Unmarshaller. Setting a Listener replaces the previous set Listener.
   * One can unregister current Listener by setting listener to null.
   */
  public void setListener(Listener listener)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Default implementation of the setProperty method always throws
   * PropertyException since there are no required properties. If a provider
   * needs to handle additional properties, it should override this method in a
   * derived class.
   */
  public void setProperty(String name, Object value) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Specify the JAXP 1.3 object that should
   * be used to validate subsequent unmarshal operations against. Passing null
   * into this method will disable validation. This method replaces the
   * deprecated setValidating(boolean) API. Initially this property is set to
   * null.
   */
  public void setSchema(Schema schema)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Specifies whether or not the Unmarshaller should validate during unmarshal
   * operations. By default, the Unmarshaller does not validate. This method
   * may only be invoked before or after calling one of the unmarshal methods.
   */
  public void setValidating(boolean validating) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * file and return the resulting content tree. Implements Unmarshal Global
   * Root Element.
   */
  public final Object unmarshal(File f) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * SAX InputSource and return the resulting content tree. Implements
   * Unmarshal Global Root Element.
   */
  public final Object unmarshal(InputSource source) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * InputStream and return the resulting content tree. Validation event
   * location information may be incomplete when using this form of the
   * unmarshal API. Implements Unmarshal Global Root Element.
   */
  public final Object unmarshal(InputStream is) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data by JAXB mapped
   * declaredType and return the resulting content tree. Implements Unmarshal
   * by Declared Type
   */
  public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * Reader and return the resulting content tree. Validation event location
   * information may be incomplete when using this form of the unmarshal API,
   * because a Reader does not provide the system ID. Implements Unmarshal
   * Global Root Element.
   */
  public final Object unmarshal(Reader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * XML Source and return the resulting content tree. Implements Unmarshal
   * Global Root Element. SAX 2.0 Parser Pluggability A client application can
   * choose not to use the default parser mechanism supplied with their JAXB
   * provider. Any SAX 2.0 compliant parser can be substituted for the JAXB
   * provider's default mechanism. To do so, the client application must
   * properly configure a SAXSource containing an XMLReader implemented by the
   * SAX 2.0 parser provider. If the XMLReader has an org.xml.sax.ErrorHandler
   * registered on it, it will be replaced by the JAXB Provider so that
   * validation errors can be reported via the ValidationEventHandler mechanism
   * of JAXB. If the SAXSource does not contain an XMLReader, then the JAXB
   * provider's default parser mechanism will be used. This parser replacement
   * mechanism can also be used to replace the JAXB provider's unmarshal-time
   * validation engine. The client application must properly configure their
   * SAX 2.0 compliant parser to perform validation (as shown in the example
   * above). Any SAXParserExceptions encountered by the parser during the
   * unmarshal operation will be processed by the JAXB provider and converted
   * into JAXB ValidationEvent objects which will be reported back to the
   * client via the ValidationEventHandler registered with the Unmarshaller.
   * Note: specifying a substitute validating SAX 2.0 parser for unmarshalling
   * does not necessarily replace the validation engine used by the JAXB
   * provider for performing on-demand validation. The only way for a client
   * application to specify an alternate parser mechanism to be used during
   * unmarshal is via the unmarshal(SAXSource) API. All other forms of the
   * unmarshal method (File, URL, Node, etc) will use the JAXB provider's
   * default parser and validator mechanisms.
   */
  public Object unmarshal(Source source) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * XML Source by declaredType and return the resulting content tree.
   * Implements Unmarshal by Declared Type See SAX 2.0 Parser Pluggability
   */
  public <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * URL and return the resulting content tree. Implements Unmarshal Global
   * Root Element.
   */
  public final Object unmarshal(URL url) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * pull parser and return the resulting content tree. This method is an
   * Unmarshal Global Root method. This method assumes that the parser is on a
   * START_DOCUMENT or START_ELEMENT event. Unmarshalling will be done from
   * this start event to the corresponding end event. If this method returns
   * successfully, the reader will be pointing at the token right after the end
   * event.
   */
  public Object unmarshal(XMLEventReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal root element to JAXB mapped
   * declaredType and return the resulting content tree. This method implements
   * unmarshal by declaredType. This method assumes that the parser is on a
   * START_DOCUMENT or START_ELEMENT event. Unmarshalling will be done from
   * this start event to the corresponding end event. If this method returns
   * successfully, the reader will be pointing at the token right after the end
   * event.
   */
  public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                        Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Unmarshals an object by using the specified XMLReader and the InputSource.
   * The callee should call the setErrorHandler method of the XMLReader so that
   * errors are passed to the client-specified ValidationEventHandler.
   */
  protected abstract Object unmarshal(XMLReader reader, InputSource source) throws JAXBException;


  /**
   * Description copied from interface: Unmarshal XML data from the specified
   * pull parser and return the resulting content tree. Implements Unmarshal
   * Global Root Element. This method assumes that the parser is on a
   * START_DOCUMENT or START_ELEMENT event. Unmarshalling will be done from
   * this start event to the corresponding end event. If this method returns
   * successfully, the reader will be pointing at the token right after the end
   * event.
   */
  public Object unmarshal(XMLStreamReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Unmarshal root element to JAXB mapped
   * declaredType and return the resulting content tree. This method implements
   * unmarshal by declaredType. This method assumes that the parser is on a
   * START_DOCUMENT or START_ELEMENT event. Unmarshalling will be done from
   * this start event to the corresponding end event. If this method returns
   * successfully, the reader will be pointing at the token right after the end
   * event.
   */
  public <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader,
                                        Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

}

