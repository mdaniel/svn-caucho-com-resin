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

package javax.xml.stream.util;
import javax.xml.stream.*;
import javax.xml.namespace.*;

/**
 * This is the base class for deriving an XMLStreamReader filter This class is
 * designed to sit between an XMLStreamReader and an application's
 * XMLStreamReader. By default each method does nothing but call the
 * corresponding method on the parent interface. Version: 1.0 Author: Copyright
 * (c) 2003 by BEA Systems. All Rights Reserved. See Also:XMLStreamReader,
 * EventReaderDelegate
 */
public class StreamReaderDelegate implements XMLStreamReader {

  /**
   * Construct an empty filter with no parent.
   */
  public StreamReaderDelegate()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an filter with the specified parent. Parameters:reader - the
   * parent
   */
  public StreamReaderDelegate(XMLStreamReader reader)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Frees any resources associated with
   * this Reader. This method does not close the underlying input source.
   */
  public void close() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the count of attributes on this
   * START_ELEMENT, this method is only valid on a START_ELEMENT or ATTRIBUTE.
   * This count excludes namespace definitions. Attribute indices are
   * zero-based.
   */
  public int getAttributeCount()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the localName of the attribute
   * at the provided index
   */
  public String getAttributeLocalName(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the qname of the attribute at
   * the provided index
   */
  public QName getAttributeName(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the namespace of the attribute
   * at the provided index
   */
  public String getAttributeNamespace(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the prefix of this attribute at
   * the provided index
   */
  public String getAttributePrefix(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the XML type of the attribute
   * at the provided index
   */
  public String getAttributeType(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the value of the attribute at
   * the index
   */
  public String getAttributeValue(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the normalized attribute value
   * of the attribute with the namespace and localName If the namespaceURI is
   * null the namespace is not checked for equality
   */
  public String getAttributeValue(String namespaceUri, String localName)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the character encoding declared
   * on the xml declaration Returns null if none was declared
   */
  public String getCharacterEncodingScheme()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Reads the content of a text-only
   * element, an exception is thrown if this is not a text-only element.
   * Regardless of value of javax.xml.stream.isCoalescing this method always
   * returns coalesced content. Precondition: the current event is
   * START_ELEMENT. Postcondition: the current event is the corresponding
   * END_ELEMENT. The method does the following (implementations are free to
   * optimized but must do equivalent processing): if(getEventType() !=
   * XMLStreamConstants.START_ELEMENT) { throw new XMLStreamException( "parser
   * must be on START_ELEMENT to read next text", getLocation()); } int
   * eventType = next(); StringBuffer content = new StringBuffer();
   * while(eventType != XMLStreamConstants.END_ELEMENT ) { if(eventType ==
   * XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA ||
   * eventType == XMLStreamConstants.SPACE || eventType ==
   * XMLStreamConstants.ENTITY_REFERENCE) { buf.append(getText()); } else
   * if(eventType == XMLStreamConstants.PROCESSING_INSTRUCTION || eventType ==
   * XMLStreamConstants.COMMENT) { // skipping } else if(eventType ==
   * XMLStreamConstants.END_DOCUMENT) { throw new XMLStreamException(
   * "unexpected end of document when reading element text content", this); }
   * else if(eventType == XMLStreamConstants.START_ELEMENT) { throw new
   * XMLStreamException( "element text content may not contain START_ELEMENT",
   * getLocation()); } else { throw new XMLStreamException( "Unexpected event
   * type "+eventType, getLocation()); } eventType = next(); } return
   * buf.toString();
   */
  public String getElementText() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return input encoding if known or null
   * if unknown.
   */
  public String getEncoding()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns an integer code that indicates
   * the type of the event the cursor is pointing to.
   */
  public int getEventType()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the (local) name of the current
   * event. For START_ELEMENT or END_ELEMENT returns the (local) name of the
   * current element. For ENTITY_REFERENCE it returns entity name. The current
   * event must be START_ELEMENT or END_ELEMENT, or ENTITY_REFERENCE
   */
  public String getLocalName()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the current location of the
   * processor. If the Location is unknown the processor should return an
   * implementation of Location that returns -1 for the location and null for
   * the publicId and systemId. The location information is only valid until
   * next() is called.
   */
  public Location getLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns a QName for the current
   * START_ELEMENT or END_ELEMENT event
   */
  public QName getName()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns a read only namespace context
   * for the current position. The context is transient and only valid until a
   * call to next() changes the state of the reader.
   */
  public NamespaceContext getNamespaceContext()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the count of namespaces
   * declared on this START_ELEMENT or END_ELEMENT, this method is only valid
   * on a START_ELEMENT, END_ELEMENT or NAMESPACE. On an END_ELEMENT the count
   * is of the namespaces that are about to go out of scope. This is the
   * equivalent of the information reported by SAX callback for an end element
   * event.
   */
  public int getNamespaceCount()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the prefix for the namespace
   * declared at the index. Returns null if this is the default namespace
   * declaration
   */
  public String getNamespacePrefix(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: If the current event is a START_ELEMENT
   * or END_ELEMENT this method returns the URI of the prefix or the default
   * namespace. Returns null if the event does not have a prefix.
   */
  public String getNamespaceURI()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the uri for the namespace
   * declared at the index.
   */
  public String getNamespaceURI(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the uri for the given prefix.
   * The uri returned depends on the current state of the processor. NOTE:The
   * 'xml' prefix is bound as defined in Namespaces in XML specification to
   * "http://www.w3.org/XML/1998/namespace". NOTE: The 'xmlns' prefix must be
   * resolved to following namespace http://www.w3.org/2000/xmlns/
   */
  public String getNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Get the parent of this instance.
   */
  public XMLStreamReader getParent()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the data section of a processing
   * instruction
   */
  public String getPIData()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the target of a processing
   * instruction
   */
  public String getPITarget()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the prefix of the current event
   * or null if the event does not have a prefix
   */
  public String getPrefix()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the value of a feature/property
   * from the underlying implementation
   */
  public Object getProperty(String name)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the current value of the parse
   * event as a string, this returns the string value of a CHARACTERS event,
   * returns the value of a COMMENT, the replacement value for an
   * ENTITY_REFERENCE, the string value of a CDATA section, the string value
   * for a SPACE event, or the String value of the internal subset of the DTD.
   * If an ENTITY_REFERENCE has been resolved, any character data will be
   * reported as CHARACTERS events.
   */
  public String getText()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns an array which contains the
   * characters from this event. This array should be treated as read-only and
   * transient. I.e. the array will contain the text characters until the
   * XMLStreamReader moves on to the next event. Attempts to hold onto the
   * character array beyond that time or modify the contents of the array are
   * breaches of the contract for this interface.
   */
  public char[] getTextCharacters()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Gets the the text associated with a
   * CHARACTERS, SPACE or CDATA event. Text starting a "sourceStart" is copied
   * into "target" starting at "targetStart". Up to "length" characters are
   * copied. The number of characters actually copied is returned. The
   * "sourceStart" argument must be greater or equal to 0 and less than or
   * equal to the number of characters associated with the event. Usually, one
   * requests text starting at a "sourceStart" of 0. If the number of
   * characters actually copied is less than the "length", then there is no
   * more text. Otherwise, subsequent calls need to be made until all text has
   * been retrieved. For example: int length = 1024; char[] myBuffer = new
   * char[ length ]; for ( int sourceStart = 0 ; ; sourceStart += length ) {
   * int nCopied = stream.getTextCharacters( sourceStart, myBuffer, 0, length
   * ); if (nCopied < length) break; } XMLStreamException may be thrown if
   * there are any XML errors in the underlying source. The "targetStart"
   * argument must be greater than or equal to 0 and less than the length of
   * "target", Length must be greater than 0 and "targetStart + length" must be
   * less than or equal to length of "target".
   */
  public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the length of the sequence of
   * characters for this Text event within the text character array.
   */
  public int getTextLength()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns the offset into the text
   * character array where the first character (of this text event) is stored.
   */
  public int getTextStart()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the xml version declared on the xml
   * declaration Returns null if none was declared
   */
  public String getVersion()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: returns true if the current event has a
   * name (is a START_ELEMENT or END_ELEMENT) returns false otherwise
   */
  public boolean hasName()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns true if there are more parsing
   * events and false if there are no more events. This method will return
   * false if the current state of the XMLStreamReader is END_DOCUMENT
   */
  public boolean hasNext() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return true if the current event has
   * text, false otherwise The following events have text: CHARACTERS,DTD
   * ,ENTITY_REFERENCE, COMMENT, SPACE
   */
  public boolean hasText()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns a boolean which indicates if
   * this attribute was created by default
   */
  public boolean isAttributeSpecified(int index)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns true if the cursor points to a
   * character data event
   */
  public boolean isCharacters()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns true if the cursor points to an
   * end tag (otherwise false)
   */
  public boolean isEndElement()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the standalone declaration from the
   * xml declaration
   */
  public boolean isStandalone()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns true if the cursor points to a
   * start tag (otherwise false)
   */
  public boolean isStartElement()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Returns true if the cursor points to a
   * character data event that consists of all whitespace
   */
  public boolean isWhiteSpace()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get next parsing event - a processor
   * may return all contiguous character data in a single chunk, or it may
   * split it into several chunks. If the property
   * javax.xml.stream.isCoalescing is set to true element content must be
   * coalesced and only one CHARACTERS event must be returned for contiguous
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
  public int next() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Skips any white space (isWhiteSpace()
   * returns true), COMMENT, or PROCESSING_INSTRUCTION, until a START_ELEMENT
   * or END_ELEMENT is reached. If other than white space characters, COMMENT,
   * PROCESSING_INSTRUCTION, START_ELEMENT, END_ELEMENT are encountered, an
   * exception is thrown. This method should be used when processing
   * element-only content seperated by white space. Precondition: none
   * Postcondition: the current event is START_ELEMENT or END_ELEMENT and
   * cursor may have moved over any whitespace event. Essentially it does the
   * following (implementations are free to optimized but must do equivalent
   * processing): int eventType = next(); while((eventType ==
   * XMLStreamConstants.CHARACTERS isWhiteSpace()) // skip whitespace ||
   * (eventType == XMLStreamConstants.CDATA isWhiteSpace()) // skip whitespace
   * || eventType == XMLStreamConstants.SPACE || eventType ==
   * XMLStreamConstants.PROCESSING_INSTRUCTION || eventType ==
   * XMLStreamConstants.COMMENT ) { eventType = next(); } if (eventType !=
   * XMLStreamConstants.START_ELEMENT eventType !=
   * XMLStreamConstants.END_ELEMENT) { throw new String
   * XMLStreamException("expected start or end tag", getLocation()); } return
   * eventType;
   */
  public int nextTag() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Test if the current event is of the
   * given type and if the namespace and name match the current namespace and
   * name of the current event. If the namespaceURI is null it is not checked
   * for equality, if the localName is null it is not checked for equality.
   */
  public void require(int type, String namespaceURI, String localName) throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the parent of this instance.
   */
  public void setParent(XMLStreamReader reader)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Checks if standalone was set in the
   * document
   */
  public boolean standaloneSet()
  {
    throw new UnsupportedOperationException();
  }

}

