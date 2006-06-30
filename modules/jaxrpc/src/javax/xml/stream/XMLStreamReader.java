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
 * The XMLStreamReader interface allows forward, read-only access to XML. It is
 * designed to be the lowest level and most efficient way to read XML data. The
 * XMLStreamReader is designed to iterate over XML using next() and hasNext().
 * The data can be accessed using methods such as getEventType(),
 * getNamespaceURI(), getLocalName() and getText(); The next() method causes
 * the reader to read the next parse event. The next() method returns an
 * integer which identifies the type of event just read. The event type can be
 * determined using getEventType(). Parsing events are defined as the XML
 * Declaration, a DTD, start tag, character data, white space, end tag,
 * comment, or processing instruction. An attribute or namespace event may be
 * encountered at the root level of a document as the result of a query
 * operation. For XML 1.0 compliance an XML processor must pass the identifiers
 * of declared unparsed entities, notation declarations and their associated
 * identifiers to the application. This information is provided through the
 * property API on this interface. The following two properties allow access to
 * this information: javax.xml.stream.notations and javax.xml.stream.entities.
 * When the current event is a DTD the following call will return a list of
 * Notations List l = (List) getProperty("javax.xml.stream.notations"); The
 * following call will return a list of entity declarations: List l = (List)
 * getProperty("javax.xml.stream.entities"); These properties can only be
 * accessed during a DTD event and are defined to return null if the
 * information is not available. The following table describes which methods
 * are valid in what state. If a method is called in an invalid state the
 * method will throw a java.lang.IllegalStateException. Valid methods for each
 * state Event Type Valid Methods All States getProperty(), hasNext(),
 * require(), close(), getNamespaceURI(), isStartElement(), isEndElement(),
 * isCharacters(), isWhiteSpace(), getNamespaceContext(),
 * getEventType(),getLocation(), hasText(), hasName() START_ELEMENT next(),
 * getName(), getLocalName(), hasName(), getPrefix(), getAttributeXXX(),
 * isAttributeSpecified(), getNamespaceXXX(), getElementText(), nextTag()
 * ATTRIBUTE next(), nextTag() getAttributeXXX(), isAttributeSpecified(),
 * NAMESPACE next(), nextTag() getNamespaceXXX() END_ELEMENT next(), getName(),
 * getLocalName(), hasName(), getPrefix(), getNamespaceXXX(), nextTag()
 * CHARACTERS next(), getTextXXX(), nextTag() CDATA next(), getTextXXX(),
 * nextTag() COMMENT next(), getTextXXX(), nextTag() SPACE next(),
 * getTextXXX(), nextTag() START_DOCUMENT next(), getEncoding(), getVersion(),
 * isStandalone(), standaloneSet(), getCharacterEncodingScheme(), nextTag()
 * END_DOCUMENT close() PROCESSING_INSTRUCTION next(), getPITarget(),
 * getPIData(), nextTag() ENTITY_REFERENCE next(), getLocalName(), getText(),
 * nextTag() DTD next(), getText(), nextTag() Version: 1.0 Author: Copyright
 * (c) 2003 by BEA Systems. All Rights Reserved. See Also:XMLEvent,
 * XMLInputFactory, XMLStreamWriter
 */
public interface XMLStreamReader extends XMLStreamConstants {

  /**
   * Frees any resources associated with this Reader. This method does not
   * close the underlying input source.
   */
  abstract void close() throws XMLStreamException;


  /**
   * Returns the count of attributes on this START_ELEMENT, this method is only
   * valid on a START_ELEMENT or ATTRIBUTE. This count excludes namespace
   * definitions. Attribute indices are zero-based.
   */
  abstract int getAttributeCount();


  /**
   * Returns the localName of the attribute at the provided index
   */
  abstract String getAttributeLocalName(int index);


  /**
   * Returns the qname of the attribute at the provided index
   */
  abstract QName getAttributeName(int index);


  /**
   * Returns the namespace of the attribute at the provided index
   */
  abstract String getAttributeNamespace(int index);


  /**
   * Returns the prefix of this attribute at the provided index
   */
  abstract String getAttributePrefix(int index);


  /**
   * Returns the XML type of the attribute at the provided index
   */
  abstract String getAttributeType(int index);


  /**
   * Returns the value of the attribute at the index
   */
  abstract String getAttributeValue(int index);


  /**
   * Returns the normalized attribute value of the attribute with the namespace
   * and localName If the namespaceURI is null the namespace is not checked for
   * equality
   */
  abstract String getAttributeValue(String namespaceURI, String localName);


  /**
   * Returns the character encoding declared on the xml declaration Returns
   * null if none was declared
   */
  abstract String getCharacterEncodingScheme();


  /**
   * Reads the content of a text-only element, an exception is thrown if this
   * is not a text-only element. Regardless of value of
   * javax.xml.stream.isCoalescing this method always returns coalesced
   * content. Precondition: the current event is START_ELEMENT. Postcondition:
   * the current event is the corresponding END_ELEMENT. The method does the
   * following (implementations are free to optimized but must do equivalent
   * processing): if(getEventType() != XMLStreamConstants.START_ELEMENT) {
   * throw new XMLStreamException( "parser must be on START_ELEMENT to read
   * next text", getLocation()); } int eventType = next(); StringBuffer content
   * = new StringBuffer(); while(eventType != XMLStreamConstants.END_ELEMENT )
   * { if(eventType == XMLStreamConstants.CHARACTERS || eventType ==
   * XMLStreamConstants.CDATA || eventType == XMLStreamConstants.SPACE ||
   * eventType == XMLStreamConstants.ENTITY_REFERENCE) { buf.append(getText());
   * } else if(eventType == XMLStreamConstants.PROCESSING_INSTRUCTION ||
   * eventType == XMLStreamConstants.COMMENT) { // skipping } else if(eventType
   * == XMLStreamConstants.END_DOCUMENT) { throw new XMLStreamException(
   * "unexpected end of document when reading element text content", this); }
   * else if(eventType == XMLStreamConstants.START_ELEMENT) { throw new
   * XMLStreamException( "element text content may not contain START_ELEMENT",
   * getLocation()); } else { throw new XMLStreamException( "Unexpected event
   * type "+eventType, getLocation()); } eventType = next(); } return
   * buf.toString();
   */
  abstract String getElementText() throws XMLStreamException;


  /**
   * Return input encoding if known or null if unknown.
   */
  abstract String getEncoding();


  /**
   * Returns an integer code that indicates the type of the event the cursor is
   * pointing to.
   */
  abstract int getEventType();


  /**
   * Returns the (local) name of the current event. For START_ELEMENT or
   * END_ELEMENT returns the (local) name of the current element. For
   * ENTITY_REFERENCE it returns entity name. The current event must be
   * START_ELEMENT or END_ELEMENT, or ENTITY_REFERENCE
   */
  abstract String getLocalName();


  /**
   * Return the current location of the processor. If the Location is unknown
   * the processor should return an implementation of Location that returns -1
   * for the location and null for the publicId and systemId. The location
   * information is only valid until next() is called.
   */
  abstract Location getLocation();


  /**
   * Returns a QName for the current START_ELEMENT or END_ELEMENT event
   */
  abstract QName getName();


  /**
   * Returns a read only namespace context for the current position. The
   * context is transient and only valid until a call to next() changes the
   * state of the reader.
   */
  abstract NamespaceContext getNamespaceContext();


  /**
   * Returns the count of namespaces declared on this START_ELEMENT or
   * END_ELEMENT, this method is only valid on a START_ELEMENT, END_ELEMENT or
   * NAMESPACE. On an END_ELEMENT the count is of the namespaces that are about
   * to go out of scope. This is the equivalent of the information reported by
   * SAX callback for an end element event.
   */
  abstract int getNamespaceCount();


  /**
   * Returns the prefix for the namespace declared at the index. Returns null
   * if this is the default namespace declaration
   */
  abstract String getNamespacePrefix(int index);


  /**
   * If the current event is a START_ELEMENT or END_ELEMENT this method returns
   * the URI of the prefix or the default namespace. Returns null if the event
   * does not have a prefix.
   */
  abstract String getNamespaceURI();


  /**
   * Returns the uri for the namespace declared at the index.
   */
  abstract String getNamespaceURI(int index);


  /**
   * Return the uri for the given prefix. The uri returned depends on the
   * current state of the processor. NOTE:The 'xml' prefix is bound as defined
   * in Namespaces in XML specification to
   * "http://www.w3.org/XML/1998/namespace". NOTE: The 'xmlns' prefix must be
   * resolved to following namespace http://www.w3.org/2000/xmlns/
   */
  abstract String getNamespaceURI(String prefix);


  /**
   * Get the data section of a processing instruction
   */
  abstract String getPIData();


  /**
   * Get the target of a processing instruction
   */
  abstract String getPITarget();


  /**
   * Returns the prefix of the current event or null if the event does not have
   * a prefix
   */
  abstract String getPrefix();


  /**
   * Get the value of a feature/property from the underlying implementation
   */
  abstract Object getProperty(String name) throws IllegalArgumentException;


  /**
   * Returns the current value of the parse event as a string, this returns the
   * string value of a CHARACTERS event, returns the value of a COMMENT, the
   * replacement value for an ENTITY_REFERENCE, the string value of a CDATA
   * section, the string value for a SPACE event, or the String value of the
   * internal subset of the DTD. If an ENTITY_REFERENCE has been resolved, any
   * character data will be reported as CHARACTERS events.
   */
  abstract String getText();


  /**
   * Returns an array which contains the characters from this event. This array
   * should be treated as read-only and transient. I.e. the array will contain
   * the text characters until the XMLStreamReader moves on to the next event.
   * Attempts to hold onto the character array beyond that time or modify the
   * contents of the array are breaches of the contract for this interface.
   */
  abstract char[] getTextCharacters();


  /**
   * Gets the the text associated with a CHARACTERS, SPACE or CDATA event. Text
   * starting a "sourceStart" is copied into "target" starting at
   * "targetStart". Up to "length" characters are copied. The number of
   * characters actually copied is returned. The "sourceStart" argument must be
   * greater or equal to 0 and less than or equal to the number of characters
   * associated with the event. Usually, one requests text starting at a
   * "sourceStart" of 0. If the number of characters actually copied is less
   * than the "length", then there is no more text. Otherwise, subsequent calls
   * need to be made until all text has been retrieved. For example: int length
   * = 1024; char[] myBuffer = new char[ length ]; for ( int sourceStart = 0 ;
   * ; sourceStart += length ) { int nCopied = stream.getTextCharacters(
   * sourceStart, myBuffer, 0, length ); if (nCopied < length) break; }
   * XMLStreamException may be thrown if there are any XML errors in the
   * underlying source. The "targetStart" argument must be greater than or
   * equal to 0 and less than the length of "target", Length must be greater
   * than 0 and "targetStart + length" must be less than or equal to length of
   * "target".
   */
  abstract int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException;


  /**
   * Returns the length of the sequence of characters for this Text event
   * within the text character array.
   */
  abstract int getTextLength();


  /**
   * Returns the offset into the text character array where the first character
   * (of this text event) is stored.
   */
  abstract int getTextStart();


  /**
   * Get the xml version declared on the xml declaration Returns null if none
   * was declared
   */
  abstract String getVersion();


  /**
   * returns true if the current event has a name (is a START_ELEMENT or
   * END_ELEMENT) returns false otherwise
   */
  abstract boolean hasName();


  /**
   * Returns true if there are more parsing events and false if there are no
   * more events. This method will return false if the current state of the
   * XMLStreamReader is END_DOCUMENT
   */
  abstract boolean hasNext() throws XMLStreamException;


  /**
   * Return true if the current event has text, false otherwise The following
   * events have text: CHARACTERS,DTD ,ENTITY_REFERENCE, COMMENT, SPACE
   */
  abstract boolean hasText();


  /**
   * Returns a boolean which indicates if this attribute was created by default
   */
  abstract boolean isAttributeSpecified(int index);


  /**
   * Returns true if the cursor points to a character data event
   */
  abstract boolean isCharacters();


  /**
   * Returns true if the cursor points to an end tag (otherwise false)
   */
  abstract boolean isEndElement();


  /**
   * Get the standalone declaration from the xml declaration
   */
  abstract boolean isStandalone();


  /**
   * Returns true if the cursor points to a start tag (otherwise false)
   */
  abstract boolean isStartElement();


  /**
   * Returns true if the cursor points to a character data event that consists
   * of all whitespace
   */
  abstract boolean isWhiteSpace();


  /**
   * Get next parsing event - a processor may return all contiguous character
   * data in a single chunk, or it may split it into several chunks. If the
   * property javax.xml.stream.isCoalescing is set to true element content must
   * be coalesced and only one CHARACTERS event must be returned for contiguous
   * element content or CDATA Sections. By default entity references must be
   * expanded and reported transparently to the application. An exception will
   * be thrown if an entity reference cannot be expanded. If element content is
   * empty (i.e. content is "") then no CHARACTERS event will be reported.
   * Given the following XML: foo>!--description-->content
   * text![CDATA[greeting>Hello/greeting>]]>other content/foo> The behavior of
   * calling next() when being on foo will be: 1- the comment (COMMENT) 2- then
   * the characters section (CHARACTERS) 3- then the CDATA section (another
   * CHARACTERS) 4- then the next characters section (another CHARACTERS) 5-
   * then the END_ELEMENT NOTE: empty element (such as tag/>) will be reported
   * with two separate events: START_ELEMENT, END_ELEMENT - This preserves
   * parsing equivalency of empty element to tag>/tag>. This method will throw
   * an IllegalStateException if it is called after hasNext() returns false.
   */
  abstract int next() throws XMLStreamException;


  /**
   * Skips any white space (isWhiteSpace() returns true), COMMENT, or
   * PROCESSING_INSTRUCTION, until a START_ELEMENT or END_ELEMENT is reached.
   * If other than white space characters, COMMENT, PROCESSING_INSTRUCTION,
   * START_ELEMENT, END_ELEMENT are encountered, an exception is thrown. This
   * method should be used when processing element-only content seperated by
   * white space. Precondition: none Postcondition: the current event is
   * START_ELEMENT or END_ELEMENT and cursor may have moved over any whitespace
   * event. Essentially it does the following (implementations are free to
   * optimized but must do equivalent processing): int eventType = next();
   * while((eventType == XMLStreamConstants.CHARACTERS isWhiteSpace()) // skip
   * whitespace || (eventType == XMLStreamConstants.CDATA isWhiteSpace()) //
   * skip whitespace || eventType == XMLStreamConstants.SPACE || eventType ==
   * XMLStreamConstants.PROCESSING_INSTRUCTION || eventType ==
   * XMLStreamConstants.COMMENT ) { eventType = next(); } if (eventType !=
   * XMLStreamConstants.START_ELEMENT eventType !=
   * XMLStreamConstants.END_ELEMENT) { throw new String
   * XMLStreamException("expected start or end tag", getLocation()); } return
   * eventType;
   */
  abstract int nextTag() throws XMLStreamException;


  /**
   * Test if the current event is of the given type and if the namespace and
   * name match the current namespace and name of the current event. If the
   * namespaceURI is null it is not checked for equality, if the localName is
   * null it is not checked for equality.
   */
  abstract void require(int type, String namespaceURI, String localName) throws XMLStreamException;


  /**
   * Checks if standalone was set in the document
   */
  abstract boolean standaloneSet();

}

