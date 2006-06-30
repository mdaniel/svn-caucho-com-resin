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

package javax.xml.stream.events;
import java.io.*;
import javax.xml.stream.*;
import javax.xml.namespace.*;

/**
 * This is the base event interface for handling markup events. Events are
 * value objects that are used to communicate the XML 1.0 InfoSet to the
 * Application. Events may be cached and referenced after the parse has
 * completed. Version: 1.0 Author: Copyright (c) 2003 by BEA Systems. All
 * Rights Reserved. See Also:XMLEventReader, Characters, ProcessingInstruction,
 * StartElement, EndElement, StartDocument, EndDocument, EntityReference,
 * EntityDeclaration, NotationDeclaration
 */
public interface XMLEvent extends XMLStreamConstants {

  /**
   * Returns this event as Characters, may result in a class cast exception if
   * this event is not Characters.
   */
  abstract Characters asCharacters();


  /**
   * Returns this event as an end element event, may result in a class cast
   * exception if this event is not a end element.
   */
  abstract EndElement asEndElement();


  /**
   * Returns this event as a start element event, may result in a class cast
   * exception if this event is not a start element.
   */
  abstract StartElement asStartElement();


  /**
   * Returns an integer code for this event.
   */
  abstract int getEventType();


  /**
   * Return the location of this event. The Location returned from this method
   * is non-volatile and will retain its information.
   */
  abstract Location getLocation();


  /**
   * This method is provided for implementations to provide optional type
   * information about the associated event. It is optional and will return
   * null if no information is available.
   */
  abstract QName getSchemaType();


  /**
   * A utility function to check if this event is an Attribute.
   */
  abstract boolean isAttribute();


  /**
   * A utility function to check if this event is Characters.
   */
  abstract boolean isCharacters();


  /**
   * A utility function to check if this event is an EndDocument.
   */
  abstract boolean isEndDocument();


  /**
   * A utility function to check if this event is a EndElement.
   */
  abstract boolean isEndElement();


  /**
   * A utility function to check if this event is an EntityReference.
   */
  abstract boolean isEntityReference();


  /**
   * A utility function to check if this event is a Namespace.
   */
  abstract boolean isNamespace();


  /**
   * A utility function to check if this event is a ProcessingInstruction.
   */
  abstract boolean isProcessingInstruction();


  /**
   * A utility function to check if this event is a StartDocument.
   */
  abstract boolean isStartDocument();


  /**
   * A utility function to check if this event is a StartElement.
   */
  abstract boolean isStartElement();


  /**
   * This method will write the XMLEvent as per the XML 1.0 specification as
   * Unicode characters. No indentation or whitespace should be outputted. Any
   * user defined event type SHALL have this method called when being written
   * to on an output stream. Built in Event types MUST implement this method,
   * but implementations MAY choose not call these methods for optimizations
   * reasons when writing out built in Events to an output stream. The output
   * generated MUST be equivalent in terms of the infoset expressed.
   */
  abstract void writeAsEncodedUnicode(Writer writer) throws XMLStreamException;

}

