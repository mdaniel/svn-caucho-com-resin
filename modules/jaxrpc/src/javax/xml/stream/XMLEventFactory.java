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
import javax.xml.namespace.*;
import javax.xml.stream.events.*;
import java.util.*;

/**
 * This interface defines a utility class for creating instances of XMLEvents
 * Version: 1.0 Author: Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 * See Also:StartElement, EndElement, ProcessingInstruction, Comment,
 * Characters, StartDocument, EndDocument, DTD
 */
public abstract class XMLEventFactory {
  protected XMLEventFactory()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a new Attribute
   */
  public abstract Attribute createAttribute(QName name, String value);


  /**
   * Create a new Attribute
   */
  public abstract Attribute createAttribute(String localName, String value);


  /**
   * Create a new Attribute
   */
  public abstract Attribute createAttribute(String prefix, String namespaceURI, String localName, String value);


  /**
   * Create a Characters event with the CData flag set to true
   */
  public abstract Characters createCData(String content);


  /**
   * Create a Characters event, this method does not check if the content is
   * all whitespace. To create a space event use #createSpace(String)
   */
  public abstract Characters createCharacters(String content);


  /**
   * Create a comment
   */
  public abstract Comment createComment(String text);


  /**
   * Create a document type definition event This string contains the entire
   * document type declaration that matches the doctypedecl in the XML 1.0
   * specification
   */
  public abstract DTD createDTD(String dtd);


  /**
   * Creates a new instance of an EndDocument event
   */
  public abstract EndDocument createEndDocument();


  /**
   * Create a new EndElement
   */
  public abstract EndElement createEndElement(QName name, Iterator namespaces);


  /**
   * Create a new EndElement
   */
  public abstract EndElement createEndElement(String prefix, String namespaceUri, String localName);


  /**
   * Create a new EndElement
   */
  public abstract EndElement createEndElement(String prefix, String namespaceUri, String localName, Iterator namespaces);


  /**
   * Creates a new instance of a EntityReference event
   */
  public abstract EntityReference createEntityReference(String name, EntityDeclaration declaration);


  /**
   * Create an ignorable space
   */
  public abstract Characters createIgnorableSpace(String content);


  /**
   * Create a new default Namespace
   */
  public abstract Namespace createNamespace(String namespaceURI);


  /**
   * Create a new Namespace
   */
  public abstract Namespace createNamespace(String prefix, String namespaceUri);


  /**
   * Create a processing instruction
   */
  public abstract ProcessingInstruction createProcessingInstruction(String target, String data);


  /**
   * Create a Characters event with the isSpace flag set to true
   */
  public abstract Characters createSpace(String content);


  /**
   * Creates a new instance of a StartDocument event
   */
  public abstract StartDocument createStartDocument();


  /**
   * Creates a new instance of a StartDocument event
   */
  public abstract StartDocument createStartDocument(String encoding);


  /**
   * Creates a new instance of a StartDocument event
   */
  public abstract StartDocument createStartDocument(String encoding, String version);


  /**
   * Creates a new instance of a StartDocument event
   */
  public abstract StartDocument createStartDocument(String encoding, String version, boolean standalone);


  /**
   * Create a new StartElement. Namespaces can be added to this StartElement by
   * passing in an Iterator that walks over a set of Namespace interfaces.
   * Attributes can be added to this StartElement by passing an iterator that
   * walks over a set of Attribute interfaces.
   */
  public abstract StartElement createStartElement(QName name, Iterator attributes, Iterator namespaces);


  /**
   * Create a new StartElement. This defaults the NamespaceContext to an empty
   * NamespaceContext. Querying this event for its namespaces or attributes
   * will result in an empty iterator being returned.
   */
  public abstract StartElement createStartElement(String prefix, String namespaceUri, String localName);


  /**
   * Create a new StartElement. Namespaces can be added to this StartElement by
   * passing in an Iterator that walks over a set of Namespace interfaces.
   * Attributes can be added to this StartElement by passing an iterator that
   * walks over a set of Attribute interfaces.
   */
  public abstract StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes, Iterator namespaces);


  /**
   * Create a new StartElement. Namespaces can be added to this StartElement by
   * passing in an Iterator that walks over a set of Namespace interfaces.
   * Attributes can be added to this StartElement by passing an iterator that
   * walks over a set of Attribute interfaces.
   */
  public abstract StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes, Iterator namespaces, NamespaceContext context);


  /**
   * Create a new instance of the factory
   */
  public static XMLEventFactory newInstance() throws FactoryConfigurationError
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a new instance of the factory
   */
  public static XMLEventFactory newInstance(String factoryId, ClassLoader classLoader) throws FactoryConfigurationError
  {
    throw new UnsupportedOperationException();
  }


  /**
   * This method allows setting of the Location on each event that is created
   * by this factory. The values are copied by value into the events created by
   * this factory. To reset the location information set the location to null.
   */
  public abstract void setLocation(Location location);

}

