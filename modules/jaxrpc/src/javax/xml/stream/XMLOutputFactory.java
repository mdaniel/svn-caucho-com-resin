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

/**
 * Defines an abstract implementation of a factory for getting XMLEventWriters
 * and XMLStreamWriters. The following table defines the standard properties of
 * this specification. Each property varies in the level of support required by
 * each implementation. The level of support required is described in the
 * 'Required' column. Configuration parameters Property Name Behavior Return
 * type Default Value Required javax.xml.stream.isRepairingNamespacesdefaults
 * prefixes on the output sideBooleanFalseYes The following paragraphs describe
 * the namespace and prefix repair algorithm: The property can be set with the
 * following code line:
 * setProperty("javax.xml.stream.isRepairingNamespaces",new
 * Boolean(true|false)); This property specifies that the writer default
 * namespace prefix declarations. The default value is false. If a writer
 * isRepairingNamespaces it will create a namespace declaration on the current
 * StartElement for any attribute that does not currently have a namespace
 * declaration in scope. If the StartElement has a uri but no prefix specified
 * a prefix will be assigned, if the prefix has not been declared in a parent
 * of the current StartElement it will be declared on the current StartElement.
 * If the defaultNamespace is bound and in scope and the default namespace
 * matches the URI of the attribute or StartElement QName no prefix will be
 * assigned. If an element or attribute name has a prefix, but is not bound to
 * any namespace URI, then the prefix will be removed during serialization. If
 * element and/or attribute names in the same start or empty-element tag are
 * bound to different namespace URIs and are using the same prefix then the
 * element or the first occurring attribute retains the original prefix and the
 * following attributes have their prefixes replaced with a new prefix that is
 * bound to the namespace URIs of those attributes. If an element or attribute
 * name uses a prefix that is bound to a different URI than that inherited from
 * the namespace context of the parent of that element and there is no
 * namespace declaration in the context of the current element then such a
 * namespace declaration is added. If an element or attribute name is bound to
 * a prefix and there is a namespace declaration that binds that prefix to a
 * different URI then that namespace declaration is either removed if the
 * correct mapping is inherited from the parent context of that element, or
 * changed to the namespace URI of the element or attribute using that prefix.
 * Version: 1.0 Author: Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 * See Also:XMLInputFactory, XMLEventWriter, XMLStreamWriter
 */
public abstract class XMLOutputFactory {

  /**
   * Property used to set prefix defaulting on the output side See
   * Also:Constant Field Values
   */
  public static final String IS_REPAIRING_NAMESPACES="javax.xml.stream.isRepairingNamespaces";

  protected XMLOutputFactory()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a new XMLEventWriter that writes to a stream
   */
  public abstract XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException;


  /**
   * Create a new XMLEventWriter that writes to a stream
   */
  public abstract XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException;


  /**
   * Create a new XMLEventWriter that writes to a JAXP result. This method is
   * optional.
   */
  public abstract XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException;


  /**
   * Create a new XMLEventWriter that writes to a writer
   */
  public abstract XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException;


  /**
   * Create a new XMLStreamWriter that writes to a stream
   */
  public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException;


  /**
   * Create a new XMLStreamWriter that writes to a stream
   */
  public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException;


  /**
   * Create a new XMLStreamWriter that writes to a JAXP result. This method is
   * optional.
   */
  public abstract XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException;


  /**
   * Create a new XMLStreamWriter that writes to a writer
   */
  public abstract XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException;


  /**
   * Get a feature/property on the underlying implementation
   */
  public abstract Object getProperty(String name) throws IllegalArgumentException;


  /**
   * Query the set of properties that this factory supports.
   */
  public abstract boolean isPropertySupported(String name);


  /**
   * Create a new instance of the factory.
   */
  public static XMLOutputFactory newInstance() throws FactoryConfigurationError
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
   * Allows the user to set specific features/properties on the underlying
   * implementation.
   */
  public abstract void setProperty(String name, Object value) throws IllegalArgumentException;

}

