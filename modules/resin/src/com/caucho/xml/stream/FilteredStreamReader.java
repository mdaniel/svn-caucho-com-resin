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
 * @author Adam Megacz
 */

package com.caucho.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class FilteredStreamReader implements XMLStreamReader {

  public FilteredStreamReader(XMLStreamReader reader,
                              StreamFilter filter)
  {
    throw new UnsupportedOperationException();
  }

  public void close() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public int getAttributeCount()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAttributeLocalName(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public QName getAttributeName(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAttributeNamespace(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAttributePrefix(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAttributeType(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAttributeValue(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAttributeValue(String namespaceURI, String localName)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getCharacterEncodingScheme()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getElementText() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
  
  public String getEncoding()
  {
    throw new UnsupportedOperationException();
  }
  
  public int getEventType()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getLocalName()
  {
    throw new UnsupportedOperationException();
  }
  
  public Location getLocation()
  {
    throw new UnsupportedOperationException();
  }
  
  public QName getName()
  {
    throw new UnsupportedOperationException();
  }
  
  public NamespaceContext getNamespaceContext()
  {
    throw new UnsupportedOperationException();
  }
  
  public int getNamespaceCount()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getNamespacePrefix(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getNamespaceURI()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getNamespaceURI(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException();
  }
  
  public String getPIData()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getPITarget()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getPrefix()
  {
    throw new UnsupportedOperationException();
  }
  
  public Object getProperty(String name) throws IllegalArgumentException
  {
    throw new UnsupportedOperationException();
  }
  
  public String getText()
  {
    throw new UnsupportedOperationException();
  }
  
  public char[] getTextCharacters()
  {
    throw new UnsupportedOperationException();
  }
  
  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public int getTextLength()
  {
    throw new UnsupportedOperationException();
  }
  
  public int getTextStart()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getVersion()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean hasName()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean hasNext() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasText()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isAttributeSpecified(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isCharacters()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isEndElement()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isStandalone()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isStartElement()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isWhiteSpace()
  {
    throw new UnsupportedOperationException();
  }
  
  public int next() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
  
  public int nextTag() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
  
  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean standaloneSet()
  {
    throw new UnsupportedOperationException();
  }

}
