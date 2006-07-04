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
 */
public interface XMLStreamReader extends XMLStreamConstants {
  abstract void close() throws XMLStreamException;

  abstract int getAttributeCount();
  
  abstract String getAttributeLocalName(int index);
  
  abstract QName getAttributeName(int index);
  
  abstract String getAttributeNamespace(int index);
  
  abstract String getAttributePrefix(int index);
  
  abstract String getAttributeType(int index);
  
  abstract String getAttributeValue(int index);
  
  abstract String getAttributeValue(String namespaceURI, String localName);
  
  abstract String getCharacterEncodingScheme();
  
  abstract String getElementText() throws XMLStreamException;
  
  abstract String getEncoding();
  
  abstract int getEventType();
  
  abstract String getLocalName();
  
  abstract Location getLocation();
  
  abstract QName getName();
  
  abstract NamespaceContext getNamespaceContext();
  
  abstract int getNamespaceCount();
  
  abstract String getNamespacePrefix(int index);
  
  abstract String getNamespaceURI();
  
  abstract String getNamespaceURI(int index);
  
  abstract String getNamespaceURI(String prefix);
  
  abstract String getPIData();
  
  abstract String getPITarget();
  
  abstract String getPrefix();
  
  abstract Object getProperty(String name) throws IllegalArgumentException;
  
  abstract String getText();
  
  abstract char[] getTextCharacters();
  
  abstract int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException;

  abstract int getTextLength();
  
  abstract int getTextStart();
  
  abstract String getVersion();
  
  abstract boolean hasName();
  
  abstract boolean hasNext() throws XMLStreamException;

  abstract boolean hasText();
  
  abstract boolean isAttributeSpecified(int index);
  
  abstract boolean isCharacters();
  
  abstract boolean isEndElement();
  
  abstract boolean isStandalone();
  
  abstract boolean isStartElement();
  
  abstract boolean isWhiteSpace();
  
  abstract int next() throws XMLStreamException;
  
  abstract int nextTag() throws XMLStreamException;
  
  abstract void require(int type, String namespaceURI, String localName) throws XMLStreamException;
  
  abstract boolean standaloneSet();
}

