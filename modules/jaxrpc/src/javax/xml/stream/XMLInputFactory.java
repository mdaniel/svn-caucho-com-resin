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

package javax.xml.stream;
import javax.xml.transform.*;
import java.io.*;
import javax.xml.stream.util.*;

/**
 * Defines an abstract implementation of a factory for getting streams. The
 * following table defines the standard properties of this specification. Each
 * property varies in the level of support required by each implementation. The
 * level of support required is described in the 'Required' column.
 * Configuration parameters Property Name Behavior Return type Default Value
 * Required javax.xml.stream.isValidatingTurns on/off implementation specific
 * DTD validationBooleanFalseNo javax.xml.stream.isNamespaceAwareTurns on/off
 * namespace processing for XML 1.0 supportBooleanTrueTrue (required) / False
 * (optional) javax.xml.stream.isCoalescingRequires the processor to coalesce
 * adjacent character dataBooleanFalseYes
 * javax.xml.stream.isReplacingEntityReferencesreplace internal entity
 * references with their replacement text and report them as
 * charactersBooleanTrueYes
 * javax.xml.stream.isSupportingExternalEntitiesResolve external parsed
 * entitiesBooleanUnspecifiedYes javax.xml.stream.supportDTDUse this property
 * to request processors that do not support DTDsBooleanTrueYes
 * javax.xml.stream.reportersets/gets the impl of the XMLReporter
 * javax.xml.stream.XMLReporterNullYes javax.xml.stream.resolversets/gets the
 * impl of the XMLResolver interfacejavax.xml.stream.XMLResolverNullYes
 * javax.xml.stream.allocatorsets/gets the impl of the XMLEventAllocator
 * interfacejavax.xml.stream.util.XMLEventAllocatorNullYes Version: 1.0 Author:
 * Copyright (c) 2003 by BEA Systems. All Rights Reserved. See
 * Also:XMLOutputFactory, XMLEventReader, XMLStreamReader, EventFilter,
 * XMLReporter, XMLResolver, XMLEventAllocator
 */
public abstract class XMLInputFactory {

  /**
   * The property used to set/get the implementation of the allocator See
   * Also:Constant Field Values
   */
  public static final String ALLOCATOR="javax.xml.stream.allocator";


  /**
   * The property that requires the parser to coalesce adjacent character data
   * sections See Also:Constant Field Values
   */
  public static final String IS_COALESCING="javax.xml.stream.isCoalescing";


  /**
   * The property used to turn on/off namespace support, this is to support XML
   * 1.0 documents, only the true setting must be supported See Also:Constant
   * Field Values
   */
  public static final String IS_NAMESPACE_AWARE="javax.xml.stream.isNamespaceAware";


  /**
   * Requires the parser to replace internal entity references with their
   * replacement text and report them as characters See Also:Constant Field
   * Values
   */
  public static final String IS_REPLACING_ENTITY_REFERENCES="javax.xml.stream.isReplacingEntityReferences";


  /**
   * The property that requires the parser to resolve external parsed entities
   * See Also:Constant Field Values
   */
  public static final String IS_SUPPORTING_EXTERNAL_ENTITIES="javax.xml.stream.isSupportingExternalEntities";


  /**
   * The property used to turn on/off implementation specific validation See
   * Also:Constant Field Values
   */
  public static final String IS_VALIDATING="javax.xml.stream.isValidating";


  /**
   * The property used to set/get the implementation of the XMLReporter
   * interface See Also:Constant Field Values
   */
  public static final String REPORTER="javax.xml.stream.reporter";


  /**
   * The property used to set/get the implementation of the XMLResolver See
   * Also:Constant Field Values
   */
  public static final String RESOLVER="javax.xml.stream.resolver";


  /**
   * The property that requires the parser to support DTDs See Also:Constant
   * Field Values
   */
  public static final String SUPPORT_DTD="javax.xml.stream.supportDTD";

  protected XMLInputFactory()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a filtered event reader that wraps the filter around the event
   * reader
   */
  public abstract XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter) throws XMLStreamException;


  /**
   * Create a filtered reader that wraps the filter around the reader
   */
  public abstract XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from a java.io.InputStream
   */
  public abstract XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from a java.io.InputStream
   */
  public abstract XMLEventReader createXMLEventReader(InputStream stream, String encoding) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from a reader
   */
  public abstract XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from a JAXP source. Support of this method is
   * optional.
   */
  public abstract XMLEventReader createXMLEventReader(Source source) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from a java.io.InputStream
   */
  public abstract XMLEventReader createXMLEventReader(String systemId, InputStream stream) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from a reader
   */
  public abstract XMLEventReader createXMLEventReader(String systemId, Reader reader) throws XMLStreamException;


  /**
   * Create a new XMLEventReader from an XMLStreamReader. After being used to
   * construct the XMLEventReader instance returned from this method the
   * XMLStreamReader must not be used.
   */
  public abstract XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException;


  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   */
  public abstract XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException;


  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   */
  public abstract XMLStreamReader createXMLStreamReader(InputStream stream, String encoding) throws XMLStreamException;


  /**
   * Create a new XMLStreamReader from a reader
   */
  public abstract XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException;


  /**
   * Create a new XMLStreamReader from a JAXP source. This method is optional.
   */
  public abstract XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException;


  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   */
  public abstract XMLStreamReader createXMLStreamReader(String systemId, InputStream stream) throws XMLStreamException;


  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   */
  public abstract XMLStreamReader createXMLStreamReader(String systemId, Reader reader) throws XMLStreamException;


  /**
   * Gets the allocator used by streams created with this factory
   */
  public abstract XMLEventAllocator getEventAllocator();


  /**
   * Get the value of a feature/property from the underlying implementation
   */
  public abstract Object getProperty(String name) throws IllegalArgumentException;


  /**
   * The reporter that will be set on any XMLStreamReader or XMLEventReader
   * created by this factory instance.
   */
  public abstract XMLReporter getXMLReporter();


  /**
   * The resolver that will be set on any XMLStreamReader or XMLEventReader
   * created by this factory instance.
   */
  public abstract XMLResolver getXMLResolver();


  /**
   * Query the set of properties that this factory supports.
   */
  public abstract boolean isPropertySupported(String name);


  /**
   * Create a new instance of the factory. This static method creates a new
   * factory instance. This method uses the following ordered lookup procedure
   * to determine the XMLInputFactory implementation class to load: Use the
   * javax.xml.stream.XMLInputFactory system property. Use the properties file
   * "lib/stax.properties" in the JRE directory. This configuration file is in
   * standard java.util.Properties format and contains the fully qualified name
   * of the implementation class with the key being the system property defined
   * above. Use the Services API (as detailed in the JAR specification), if
   * available, to determine the classname. The Services API will look for a
   * classname in the file META-INF/services/javax.xml.stream.XMLInputFactory
   * in jars available to the runtime. Platform default XMLInputFactory
   * instance. Once an application has obtained a reference to a
   * XMLInputFactory it can use the factory to configure and obtain stream
   * instances.
   */
  public static XMLInputFactory newInstance() throws FactoryConfigurationError
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a new instance of the factory
   */
  public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set a user defined event allocator for events
   */
  public abstract void setEventAllocator(XMLEventAllocator allocator);


  /**
   * Allows the user to set specific feature/property on the underlying
   * implementation. The underlying implementation is not required to support
   * every setting of every property in the specification and may use
   * IllegalArgumentException to signal that an unsupported property may not be
   * set with the specified value.
   */
  public abstract void setProperty(String name, Object value) throws IllegalArgumentException;


  /**
   * The reporter that will be set on any XMLStreamReader or XMLEventReader
   * created by this factory instance.
   */
  public abstract void setXMLReporter(XMLReporter reporter);


  /**
   * The resolver that will be set on any XMLStreamReader or XMLEventReader
   * created by this factory instance.
   */
  public abstract void setXMLResolver(XMLResolver resolver);

}

