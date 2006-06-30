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
import javax.xml.stream.util.*;
import javax.xml.namespace.*;
import javax.xml.stream.events.*;

/**
 * This is the top level interface for writing XML documents. Instances of this
 * interface are not required to validate the form of the XML. Version: 1.0
 * Author: Copyright (c) 2003 by BEA Systems. All Rights Reserved. See
 * Also:XMLEventReader, XMLEvent, Characters, ProcessingInstruction,
 * StartElement, EndElement
 */
public interface XMLEventWriter extends XMLEventConsumer {

  /**
   * Add an event to the output stream Adding a START_ELEMENT will open a new
   * namespace scope that will be closed when the corresponding END_ELEMENT is
   * written.
   */
  abstract void add(XMLEvent event) throws XMLStreamException;


  /**
   * Adds an entire stream to an output stream, calls next() on the inputStream
   * argument until hasNext() returns false This should be treated as a
   * convenience method that will perform the following loop over all the
   * events in an event reader and call add on each event.
   */
  abstract void add(XMLEventReader reader) throws XMLStreamException;


  /**
   * Frees any resources associated with this stream
   */
  abstract void close() throws XMLStreamException;


  /**
   * Writes any cached events to the underlying output mechanism
   */
  abstract void flush() throws XMLStreamException;


  /**
   * Returns the current namespace context.
   */
  abstract NamespaceContext getNamespaceContext();


  /**
   * Gets the prefix the uri is bound to
   */
  abstract String getPrefix(String uri) throws XMLStreamException;


  /**
   * Binds a URI to the default namespace This URI is bound in the scope of the
   * current START_ELEMENT / END_ELEMENT pair. If this method is called before
   * a START_ELEMENT has been written the uri is bound in the root scope.
   */
  abstract void setDefaultNamespace(String uri) throws XMLStreamException;


  /**
   * Sets the current namespace context for prefix and uri bindings. This
   * context becomes the root namespace context for writing and will replace
   * the current root namespace context. Subsequent calls to setPrefix and
   * setDefaultNamespace will bind namespaces using the context passed to the
   * method as the root context for resolving namespaces.
   */
  abstract void setNamespaceContext(NamespaceContext context) throws XMLStreamException;


  /**
   * Sets the prefix the uri is bound to. This prefix is bound in the scope of
   * the current START_ELEMENT / END_ELEMENT pair. If this method is called
   * before a START_ELEMENT has been written the prefix is bound in the root
   * scope.
   */
  abstract void setPrefix(String prefix, String uri) throws XMLStreamException;

}

