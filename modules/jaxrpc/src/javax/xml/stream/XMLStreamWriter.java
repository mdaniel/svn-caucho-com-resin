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

/**
 * The XMLStreamWriter interface specifies how to write XML. The
 * XMLStreamWriter does not perform well formedness checking on its input.
 * However the writeCharacters method is required to escape , and For attribute
 * values the writeAttribute method will escape the above characters plus to
 * ensure that all character content and attribute values are well formed. Each
 * NAMESPACE and ATTRIBUTE must be individually written. If
 * javax.xml.stream.isPrefixDefaulting is set to false it is a fatal error if
 * an element is written with namespace URI that has not been bound to a
 * prefix. If javax.xml.stream.isPrefixDefaulting is set to true the
 * XMLStreamWriter implementation must write a prefix for each unbound URI that
 * it encounters in the current scope. Version: 1.0 Author: Copyright (c) 2003
 * by BEA Systems. All Rights Reserved. See Also:XMLOutputFactory,
 * XMLStreamReader
 */
public interface XMLStreamWriter {

  /**
   * Close this writer and free any resources associated with the writer. This
   * must not close the underlying output stream.
   */
  abstract void close() throws XMLStreamException;


  /**
   * Write any cached data to the underlying output mechanism.
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
   * Get the value of a feature/property from the underlying implementation
   */
  abstract Object getProperty(String name) throws IllegalArgumentException;


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
   * method as the root context for resolving namespaces. This method may only
   * be called once at the start of the document. It does not cause the
   * namespaces to be declared. If a namespace URI to prefix mapping is found
   * in the namespace context it is treated as declared and the prefix may be
   * used by the StreamWriter.
   */
  abstract void setNamespaceContext(NamespaceContext context) throws XMLStreamException;


  /**
   * Sets the prefix the uri is bound to. This prefix is bound in the scope of
   * the current START_ELEMENT / END_ELEMENT pair. If this method is called
   * before a START_ELEMENT has been written the prefix is bound in the root
   * scope.
   */
  abstract void setPrefix(String prefix, String uri) throws XMLStreamException;


  /**
   * Writes an attribute to the output stream without a prefix.
   */
  abstract void writeAttribute(String localName, String value) throws XMLStreamException;


  /**
   * Writes an attribute to the output stream
   */
  abstract void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException;


  /**
   * Writes an attribute to the output stream
   */
  abstract void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException;


  /**
   * Writes a CData section
   */
  abstract void writeCData(String data) throws XMLStreamException;


  /**
   * Write text to the output
   */
  abstract void writeCharacters(char[] text, int start, int len) throws XMLStreamException;


  /**
   * Write text to the output
   */
  abstract void writeCharacters(String text) throws XMLStreamException;


  /**
   * Writes an xml comment with the data enclosed
   */
  abstract void writeComment(String data) throws XMLStreamException;


  /**
   * Writes the default namespace to the stream
   */
  abstract void writeDefaultNamespace(String namespaceURI) throws XMLStreamException;


  /**
   * Write a DTD section. This string represents the entire doctypedecl
   * production from the XML 1.0 specification.
   */
  abstract void writeDTD(String dtd) throws XMLStreamException;


  /**
   * Writes an empty element tag to the output
   */
  abstract void writeEmptyElement(String localName) throws XMLStreamException;


  /**
   * Writes an empty element tag to the output
   */
  abstract void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException;


  /**
   * Writes an empty element tag to the output
   */
  abstract void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException;


  /**
   * Closes any start tags and writes corresponding end tags.
   */
  abstract void writeEndDocument() throws XMLStreamException;


  /**
   * Writes an end tag to the output relying on the internal state of the
   * writer to determine the prefix and local name of the event.
   */
  abstract void writeEndElement() throws XMLStreamException;


  /**
   * Writes an entity reference
   */
  abstract void writeEntityRef(String name) throws XMLStreamException;


  /**
   * Writes a namespace to the output stream If the prefix argument to this
   * method is the empty string, "xmlns", or null this method will delegate to
   * writeDefaultNamespace
   */
  abstract void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException;


  /**
   * Writes a processing instruction
   */
  abstract void writeProcessingInstruction(String target) throws XMLStreamException;


  /**
   * Writes a processing instruction
   */
  abstract void writeProcessingInstruction(String target, String data) throws XMLStreamException;


  /**
   * Write the XML Declaration. Defaults the XML version to 1.0, and the
   * encoding to utf-8
   */
  abstract void writeStartDocument() throws XMLStreamException;


  /**
   * Write the XML Declaration. Defaults the XML version to 1.0
   */
  abstract void writeStartDocument(String version) throws XMLStreamException;


  /**
   * Write the XML Declaration. Note that the encoding parameter does not set
   * the actual encoding of the underlying output. That must be set when the
   * instance of the XMLStreamWriter is created using the XMLOutputFactory
   */
  abstract void writeStartDocument(String encoding, String version) throws XMLStreamException;


  /**
   * Writes a start tag to the output. All writeStartElement methods open a new
   * scope in the internal namespace context. Writing the corresponding
   * EndElement causes the scope to be closed.
   */
  abstract void writeStartElement(String localName) throws XMLStreamException;


  /**
   * Writes a start tag to the output
   */
  abstract void writeStartElement(String namespaceURI, String localName) throws XMLStreamException;


  /**
   * Writes a start tag to the output
   */
  abstract void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException;

}

