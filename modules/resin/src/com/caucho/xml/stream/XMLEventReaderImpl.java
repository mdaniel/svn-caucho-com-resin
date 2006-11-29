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
* @author Emil Ong
*/

package com.caucho.xml.stream;

import com.caucho.util.L10N;
import com.caucho.xml.stream.events.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import java.util.HashMap;

public class XMLEventReaderImpl implements XMLEventReader, XMLStreamConstants {
  public static final L10N L = new L10N(XMLEventReaderImpl.class);

  private final XMLStreamReader _in;
  private XMLEvent _current = null;
  private XMLEvent _next = null;

  public XMLEventReaderImpl(XMLStreamReader in)
    throws XMLStreamException
  {
    _in = in;
    _next = getEvent();
  }

  public void close() 
    throws XMLStreamException
  {
    _in.close();
  }

  public String getElementText() 
    throws XMLStreamException
  {
    return _in.getElementText();
  }

  public Object getProperty(String name) 
    throws IllegalArgumentException
  {
    throw new IllegalArgumentException(name);
  }

  public boolean hasNext()
  {
    try {
      return _next != null || _in.hasNext();
    } 
    catch (XMLStreamException e) {
      return false;
    }
  }

  public XMLEvent nextEvent() throws XMLStreamException
  {
    if (_next != null) {
      _current = _next;
      _next = null;
    }
    else {
      _in.next();
      _current = getEvent();
    }

    return _current;
  }

  public XMLEvent nextTag() throws XMLStreamException
  {
    if (_next != null) {
      _current = _next;
      _next = null;
    }
    else {
      _in.nextTag();
      _current = getEvent();
    }

    return _current;
  }

  public XMLEvent peek() throws XMLStreamException
  {
    if (_next == null) {
      _in.next();
      _next = getEvent();
    }

    return _next;
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }

  public XMLEvent next()
  {
    try {
      return nextEvent();
    }
    catch (XMLStreamException e) {
      return null;
    }
  }

  private XMLEvent getEvent()
    throws XMLStreamException
  {
    switch (_in.getEventType()) {
      case ATTRIBUTE: 
        // won't happen: our stream reader does not return attributes
        // independent of start elements/empty elements
        break;

      case CDATA:
        return new CharactersImpl(_in.getText(), true, false, false);

      case CHARACTERS: 
        return new CharactersImpl(_in.getText(), false, false, false);

      case COMMENT:
        return new CommentImpl(_in.getText());

      case DTD:
        // XXX
        break;

      case END_DOCUMENT:
        return new EndDocumentImpl();

      case END_ELEMENT:
        return new EndElementImpl(_in.getName());

      case ENTITY_DECLARATION:
        // XXX
        break;

      case ENTITY_REFERENCE:
        // XXX
        break;

      case NAMESPACE:
        // won't happen: our stream reader does not return attributes
        // independent of start elements/empty elements
        break;

      case NOTATION_DECLARATION:
        // XXX
        break;

      case PROCESSING_INSTRUCTION:
        return new ProcessingInstructionImpl(_in.getPIData(), 
                                             _in.getPITarget());

      case SPACE:
        return new CharactersImpl(_in.getText(), false, true, true);

      case START_DOCUMENT:
        boolean encodingSet = true;
        String encoding = _in.getCharacterEncodingScheme();

        if (encoding == null) {
          encoding = "utf-8"; // XXX
          encodingSet = false;
        }

        return new StartDocumentImpl(encodingSet, encoding, 
                                     null /* XXX: system id */, 
                                     _in.getVersion(), 
                                     _in.isStandalone(), _in.standaloneSet());

      case START_ELEMENT:
        HashMap<QName,Attribute> attributes = new HashMap<QName,Attribute>();

        for (int i = 0; i < _in.getAttributeCount(); i++) {
          Attribute attribute = new AttributeImpl(_in.getAttributeName(i),
                                                  _in.getAttributeValue(i));
          attributes.put(_in.getAttributeName(i), attribute);
        }

        HashMap<String,Namespace> namespaces= new HashMap<String,Namespace>();

        for (int i = 0; i < _in.getNamespaceCount(); i++) {
          Namespace namespace = new NamespaceImpl(_in.getNamespacePrefix(i),
                                                  _in.getNamespaceURI(i));
          namespaces.put(_in.getNamespacePrefix(i), namespace);
        }

        return new StartElementImpl(_in.getName(), attributes, namespaces,
                                    _in.getNamespaceContext());
    }

    throw new XMLStreamException("Event type = " + _in.getEventType());
  }
}

