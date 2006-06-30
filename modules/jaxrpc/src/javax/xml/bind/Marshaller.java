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

/**
 * The Marshaller class is responsible for governing the process of serializing
 * Java content trees back into XML data. It provides the basic marshalling
 * methods: Assume the following setup code for all following code fragments:
 * Marshalling to a File: Marshalling to a SAX ContentHandler: Marshalling to a
 * DOM Node: Marshalling to a java.io.OutputStream: Marshalling to a
 * java.io.Writer: Marshalling to a javax.xml.transform.SAXResult: Marshalling
 * to a javax.xml.transform.DOMResult: Marshalling to a
 * javax.xml.transform.StreamResult: Marshalling to a
 * javax.xml.stream.XMLStreamWriter: Marshalling to a
 * javax.xml.stream.XMLEventWriter: Marshalling content tree rooted by a JAXB
 * element Encoding Validation and Well-Formedness Client applications are not
 * required to validate the Java content tree prior to calling any of the
 * marshal API's. Furthermore, there is no requirement that the Java content
 * tree be valid with respect to its original schema in order to marshal it
 * back into XML data. Different JAXB Providers will support marshalling
 * invalid Java content trees at varying levels, however all JAXB Providers
 * must be able to marshal a valid content tree back to XML data. A JAXB
 * Provider must throw a MarshalException when it is unable to complete the
 * marshal operation due to invalid content. Some JAXB Providers will fully
 * allow marshalling invalid content, others will fail on the first validation
 * error. Even when schema validation is not explictly enabled for the marshal
 * operation, it is possible that certain types of validation events will be
 * detected during the operation. Validation events will be reported to the
 * registered event handler. If the client application has not registered an
 * event handler prior to invoking one of the marshal API's, then events will
 * be delivered to a default event handler which will terminate the marshal
 * operation after encountering the first error or fatal error. Note that for
 * JAXB 2.0 and later versions, DefaultValidationEventHandler is no longer
 * used. Supported Properties All JAXB Providers are required to support the
 * following set of properties. Some providers may support additional
 * properties. jaxb.encoding - value must be a java.lang.String The output
 * encoding to use when marshalling the XML data. The Marshaller will use
 * "UTF-8" by default if this property is not specified. jaxb.formatted.output
 * - value must be a java.lang.Boolean This property controls whether or not
 * the Marshaller will format the resulting XML data with line breaks and
 * indentation. A true value for this property indicates human readable
 * indented xml data, while a false value indicates unformatted xml data. The
 * Marshaller will default to false (unformatted) if this property is not
 * specified. jaxb.schemaLocation - value must be a java.lang.String This
 * property allows the client application to specify an xsi:schemaLocation
 * attribute in the generated XML data. The format of the schemaLocation
 * attribute value is discussed in an easy to understand, non-normative form in
 * Section 5.6 of the W3C XML Schema Part 0: Primer and specified in Section
 * 2.6 of the W3C XML Schema Part 1: Structures. jaxb.noNamespaceSchemaLocation
 * - value must be a java.lang.String This property allows the client
 * application to specify an xsi:noNamespaceSchemaLocation attribute in the
 * generated XML data. The format of the schemaLocation attribute value is
 * discussed in an easy to understand, non-normative form in Section 5.6 of the
 * W3C XML Schema Part 0: Primer and specified in Section 2.6 of the W3C XML
 * Schema Part 1: Structures. jaxb.fragment - value must be a java.lang.Boolean
 * This property determines whether or not document level events will be
 * generated by the Marshaller. If the property is not specified, the default
 * is false. This property has different implications depending on which
 * marshal api you are using - when this property is set to true:
 * marshal(Object,ContentHandler) - the Marshaller won't invoke
 * ContentHandler.startDocument() and ContentHandler.endDocument().
 * marshal(Object,Node) - the property has no effect on this API.
 * marshal(Object,OutputStream) - the Marshaller won't generate an xml
 * declaration. marshal(Object,Writer) - the Marshaller won't generate an xml
 * declaration. marshal(Object,Result) - depends on the kind of Result object,
 * see semantics for Node, ContentHandler, and Stream APIs
 * marshal(Object,XMLEventWriter) - the Marshaller will not generate
 * XMLStreamConstants.START_DOCUMENT and XMLStreamConstants.END_DOCUMENT
 * events. marshal(Object,XMLStreamWriter) - the Marshaller will not generate
 * XMLStreamConstants.START_DOCUMENT and XMLStreamConstants.END_DOCUMENT
 * events. Marshal Event Callbacks Class defined event callback methods allow
 * any JAXB mapped class to specify its own specific callback methods by
 * defining methods with the following method signatures: The external listener
 * callback mechanism enables the registration of a Marshaller.Listener
 * instance with a setListener(Listener). The external listener receives all
 * callback events, allowing for more centralized processing than per class
 * defined callback methods. The 'class defined' and external listener event
 * callback methods are independent of each other, both can be called for one
 * event. The invocation ordering when both listener callback methods exist is
 * defined in Marshaller.Listener.beforeMarshal(Object) and
 * Marshaller.Listener.afterMarshal(Object). An event callback method throwing
 * an exception terminates the current marshal process. Since: JAXB1.0 Version:
 * $Revision: 1.19 $ $Date: 2006/03/08 16:54:42 $ Author: Kohsuke Kawaguchi,
 * Sun Microsystems, Inc.Ryan Shoemaker, Sun Microsystems, Inc.Joe Fialli, Sun
 * Microsystems, Inc. See Also:JAXBContext, Validator, Unmarshaller
 */
public interface Marshaller {

  /**
   * The name of the property used to specify the output encoding in the
   * marshalled XML data. See Also:Constant Field Values
   */
  static final String JAXB_ENCODING="jaxb.encoding";


  /**
   * The name of the property used to specify whether or not the marshalled XML
   * data is formatted with linefeeds and indentation. See Also:Constant Field
   * Values
   */
  static final String JAXB_FORMATTED_OUTPUT="jaxb.formatted.output";


  /**
   * The name of the property used to specify whether or not the marshaller
   * will generate document level events (ie calling startDocument or
   * endDocument). See Also:Constant Field Values
   */
  static final String JAXB_FRAGMENT="jaxb.fragment";


  /**
   * The name of the property used to specify the xsi:noNamespaceSchemaLocation
   * attribute value to place in the marshalled XML output. See Also:Constant
   * Field Values
   */
  static final String JAXB_NO_NAMESPACE_SCHEMA_LOCATION="jaxb.noNamespaceSchemaLocation";


  /**
   * The name of the property used to specify the xsi:schemaLocation attribute
   * value to place in the marshalled XML output. See Also:Constant Field Values
   */
  static final String JAXB_SCHEMA_LOCATION="jaxb.schemaLocation";


  /**
   * Gets the adapter associated with the specified type. This is the reverse
   * operation of the method.
   */
  abstract <A extends XmlAdapter> A getAdapter(Class<A> type);

  abstract AttachmentMarshaller getAttachmentMarshaller();


  /**
   * Return the current event handler or the default event handler if one
   * hasn't been set.
   */
  abstract ValidationEventHandler getEventHandler() throws JAXBException;


  /**
   * Return Marshaller.Listener registered with this Marshaller.
   */
  abstract Listener getListener();


  /**
   * Get a DOM tree view of the content tree(Optional). If the returned DOM
   * tree is updated, these changes are also visible in the content tree. Use
   * to force a deep copy of the content tree to a DOM representation.
   */
  abstract Node getNode(Object contentTree) throws JAXBException;


  /**
   * Get the particular property in the underlying implementation of
   * Marshaller. This method can only be used to get one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * get an undefined property will result in a PropertyException being thrown.
   * See .
   */
  abstract Object getProperty(String name) throws PropertyException;


  /**
   * Get the JAXP 1.3 object being used to perform marshal-time validation. If
   * there is no Schema set on the marshaller, then this method will return
   * null indicating that marshal-time validation will not be performed.
   */
  abstract Schema getSchema();


  /**
   * Marshal the content tree rooted at jaxbElement into SAX2 events.
   */
  abstract void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException;


  /**
   * Marshal the content tree rooted at jaxbElement into a DOM tree.
   */
  abstract void marshal(Object jaxbElement, Node node) throws JAXBException;


  /**
   * Marshal the content tree rooted at jaxbElement into an output stream.
   */
  abstract void marshal(Object jaxbElement, OutputStream os) throws JAXBException;


  /**
   * Marshal the content tree rooted at jaxbElement into the specified
   * javax.xml.transform.Result. All JAXB Providers must at least support
   * DOMResult, SAXResult, and StreamResult. It can support other derived
   * classes of Result as well.
   */
  abstract void marshal(Object jaxbElement, Result result) throws JAXBException;


  /**
   * Marshal the content tree rooted at jaxbElement into a Writer.
   */
  abstract void marshal(Object jaxbElement, Writer writer) throws JAXBException;


  /**
   * Marshal the content tree rooted at jaxbElement into a .
   */
  abstract void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException;


  /**
   * Marshal the content tree rooted at jaxbElement into a .
   */
  abstract void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException;


  /**
   * Associates a configured instance of with this marshaller. Every marshaller
   * internally maintains a MapClass,XmlAdapter>, which it uses for marshalling
   * classes whose fields/methods are annotated with XmlJavaTypeAdapter. This
   * method allows applications to use a configured instance of XmlAdapter.
   * When an instance of an adapter is not given, a marshaller will create one
   * by invoking its default constructor.
   */
  abstract <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter);


  /**
   * Associates a configured instance of with this marshaller. This is a
   * convenience method that invokes setAdapter(adapter.getClass(),adapter);.
   */
  abstract void setAdapter(XmlAdapter adapter);


  /**
   * Associate a context that enables binary data within an XML document to be
   * transmitted as XML-binary optimized attachment. The attachment is
   * referenced from the XML document content model by content-id URIs(cid)
   * references stored within the xml document.
   */
  abstract void setAttachmentMarshaller(AttachmentMarshaller am);


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
  abstract void setEventHandler(ValidationEventHandler handler) throws JAXBException;


  /**
   * Register marshal event callback Marshaller.Listener with this Marshaller.
   * There is only one Listener per Marshaller. Setting a Listener replaces the
   * previous set Listener. One can unregister current Listener by setting
   * listener to null.
   */
  abstract void setListener(Listener listener);


  /**
   * Set the particular property in the underlying implementation of
   * Marshaller. This method can only be used to set one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * set an undefined property will result in a PropertyException being thrown.
   * See .
   */
  abstract void setProperty(String name, Object value) throws PropertyException;


  /**
   * Specify the JAXP 1.3 object that should be used to validate subsequent
   * marshal operations against. Passing null into this method will disable
   * validation. This method allows the caller to validate the marshalled XML
   * as it's marshalled. Initially this property is set to null.
   */
  abstract void setSchema(Schema schema);


  /**
   * Since: JAXB2.0 See Also:Marshaller.setListener(Listener),
   * Marshaller.getListener()
   */
  public static abstract class Listener {
    public Listener()
    {
      throw new UnsupportedOperationException();
    }


    /**
     * Callback method invoked after marshalling source to XML. This method is
     * invoked after source and all its descendants have been marshalled. Note
     * that if the class of source defines its own afterMarshal method, the
     * class specific callback method is invoked just before this method is
     * invoked.
     */
    public void afterMarshal(Object source)
    {
      throw new UnsupportedOperationException();
    }


    /**
     * Callback method invoked before marshalling from source to XML. This
     * method is invoked just before marshalling process starts to marshal
     * source. Note that if the class of source defines its own beforeMarshal
     * method, the class specific callback method is invoked just before this
     * method is invoked.
     */
    public void beforeMarshal(Object source)
    {
      throw new UnsupportedOperationException();
    }

  }
}

