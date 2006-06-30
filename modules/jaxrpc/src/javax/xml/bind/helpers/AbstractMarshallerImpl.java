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

/**
 * Partial default Marshaller implementation. This class provides a partial
 * default implementation for the Marshaller interface. The only methods that a
 * JAXB Provider has to implement are marshal(Object,
 * javax.xml.transform.Result), marshal(Object,
 * javax.xml.stream.XMLStreamWriter), and marshal(Object,
 * javax.xml.stream.XMLEventWriter). Since: JAXB1.0 Version: $Revision: 1.7 $
 * $Date: 2006/03/08 17:00:39 $ Author: Kohsuke Kawaguchi, Sun Microsystems,
 * Inc. See Also:Marshaller
 */
public abstract class AbstractMarshallerImpl implements Marshaller {
  public AbstractMarshallerImpl()
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

  public AttachmentMarshaller getAttachmentMarshaller()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for getting the current output encoding.
   */
  protected String getEncoding()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the current event handler or the
   * default event handler if one hasn't been set.
   */
  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the corresponding Java encoding name from an IANA name. This method
   * is a helper method for the derived class to convert encoding names.
   */
  protected String getJavaEncoding(String encoding) throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return Marshaller.Listener registered
   * with this Marshaller.
   */
  public Listener getListener()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * By default, the getNode method is unsupported and throw an .
   * Implementations that choose to support this method must override this
   * method.
   */
  public Node getNode(Object obj) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for getting the current noNamespaceSchemaLocation.
   */
  protected String getNoNSSchemaLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Default implementation of the getProperty method handles the four defined
   * properties in Marshaller. If a provider needs to support additional
   * provider specific properties, it should override this method in a derived
   * class.
   */
  public Object getProperty(String name) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the JAXP 1.3 object being used to
   * perform marshal-time validation. If there is no Schema set on the
   * marshaller, then this method will return null indicating that marshal-time
   * validation will not be performed.
   */
  public Schema getSchema()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for getting the current schemaLocation.
   */
  protected String getSchemaLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for getting the formatted output flag.
   */
  protected boolean isFormattedOutput()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for getting the fragment flag.
   */
  protected boolean isFragment()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Marshal the content tree rooted at
   * jaxbElement into SAX2 events.
   */
  public final void marshal(Object obj, ContentHandler handler) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Marshal the content tree rooted at
   * jaxbElement into a DOM tree.
   */
  public final void marshal(Object obj, Node node) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Marshal the content tree rooted at
   * jaxbElement into an output stream.
   */
  public final void marshal(Object obj, OutputStream os) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Marshal the content tree rooted at
   * jaxbElement into a Writer.
   */
  public final void marshal(Object obj, Writer w) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Marshal the content tree rooted at
   * jaxbElement into a .
   */
  public void marshal(Object obj, XMLEventWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Marshal the content tree rooted at
   * jaxbElement into a .
   */
  public void marshal(Object obj, XMLStreamWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Associates a configured instance of
   * with this marshaller. Every marshaller internally maintains a
   * MapClass,XmlAdapter>, which it uses for marshalling classes whose
   * fields/methods are annotated with XmlJavaTypeAdapter. This method allows
   * applications to use a configured instance of XmlAdapter. When an instance
   * of an adapter is not given, a marshaller will create one by invoking its
   * default constructor.
   */
  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Associates a configured instance of
   * with this marshaller. This is a convenience method that invokes
   * setAdapter(adapter.getClass(),adapter);.
   */
  public void setAdapter(XmlAdapter adapter)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Associate a context that enables binary
   * data within an XML document to be transmitted as XML-binary optimized
   * attachment. The attachment is referenced from the XML document content
   * model by content-id URIs(cid) references stored within the xml document.
   */
  public void setAttachmentMarshaller(AttachmentMarshaller am)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for setting the output encoding.
   */
  protected void setEncoding(String encoding)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Allow an application to register a
   * validation event handler. The validation event handler will be called by
   * the JAXB Provider if any validation errors are encountered during calls to
   * any of the marshal API's. If the client application does not register a
   * validation event handler before invoking one of the marshal methods, then
   * validation events will be handled by the default event handler which will
   * terminate the marshal operation after the first error or fatal error is
   * encountered. Calling this method with a null parameter will cause the
   * Marshaller to revert back to the default default event handler.
   */
  public void setEventHandler(ValidationEventHandler handler) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for setting the formatted output flag.
   */
  protected void setFormattedOutput(boolean v)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for setting the fragment flag.
   */
  protected void setFragment(boolean v)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Register marshal event callback
   * Marshaller.Listener with this Marshaller. There is only one Listener per
   * Marshaller. Setting a Listener replaces the previous set Listener. One can
   * unregister current Listener by setting listener to null.
   */
  public void setListener(Listener listener)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for setting the noNamespaceSchemaLocation.
   */
  protected void setNoNSSchemaLocation(String location)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Default implementation of the setProperty method handles the four defined
   * properties in Marshaller. If a provider needs to handle additional
   * properties, it should override this method in a derived class.
   */
  public void setProperty(String name, Object value) throws PropertyException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Specify the JAXP 1.3 object that should
   * be used to validate subsequent marshal operations against. Passing null
   * into this method will disable validation. This method allows the caller to
   * validate the marshalled XML as it's marshalled. Initially this property is
   * set to null.
   */
  public void setSchema(Schema schema)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convenience method for setting the schemaLocation.
   */
  protected void setSchemaLocation(String location)
  {
    throw new UnsupportedOperationException();
  }

}

