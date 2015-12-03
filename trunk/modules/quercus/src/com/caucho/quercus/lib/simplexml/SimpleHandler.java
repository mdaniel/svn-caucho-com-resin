/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.simplexml;

import java.io.StringReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

import com.caucho.util.L10N;

public class SimpleHandler extends DefaultHandler2
{
  private static final Logger log
    = Logger.getLogger(SimpleHandler.class.getName());
  private static final L10N L = new L10N(SimpleHandler.class);

  private HashMap<String,String> _entityMap = new HashMap<String,String>();

  private StringBuilder _sb = new StringBuilder();

  private final DOMImplementation _impl;
  private Document _doc;
  private Node _node;

  private String _entityName;

  public SimpleHandler(DOMImplementation impl)
  {
    _impl = impl;
  }

  public Document getDocument()
  {
    return _doc;
  }

  //
  // ContentHandler start
  //

  @Override
  public void startDocument()
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".startDocument0");
    }

    _doc = _impl.createDocument(null, null, null);
    _node = _doc;
  }

  @Override
  public void endDocument()
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".endDocument0");
    }
  }

  @Override
  public void processingInstruction(String target, String data)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".processingInstruction0: " + target + " . " + data);
    }
  }

  @Override
  public void characters(char []ch, int start, int length)
    throws SAXException
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".characters0: " + new String(ch, start, length));
    }

    if (_entityName != null) {
      String entityName = _entityName;

      appendText();

      EntityReference ref = getDocument().createEntityReference(entityName);

      _node.appendChild(ref);
    }
    else {
      _sb.append(ch, start, length);
    }
  }

  @Override
  public void startElement(String uri,
                           String localName,
                           String qName,
                           Attributes attributes)
  {
    appendText();

    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".startElement0: " + uri + " . " + localName + " . " + qName);
    }

    Element e = getDocument().createElementNS(uri, qName);

    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".startElement1: " + e.getNamespaceURI());
    }

    for (int i = 0; i < attributes.getLength(); i++) {
      String attrName = attributes.getQName(i);
      String attrValue = attributes.getValue(i);

      e.setAttribute(attrName, attrValue);

      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, getClass().getSimpleName() + ".startElement2: " + attrName + " . " + attrValue);
      }
    }

    _node.appendChild(e);

    _node = e;
  }

  @Override
  public void endElement(String uri, String localName, String qName)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".endElement0: " + uri + " . " + localName + " . " + qName);
    }

    appendText();

    _node = _node.getParentNode();
  }

  private void appendText()
  {
    _entityName = null;

    if (_sb.length() > 0) {
      String str = _sb.toString();
      _sb.setLength(0);

      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, "SimpleHandler.appendText0: " + str + " . " + str.length());
      }

      Text text = getDocument().createTextNode(str);

      _node.appendChild(text);
    }
  }

  @Override
  public void elementDecl(String name, String model)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".elementDecl0: " + name + " . " + model);
    }
  }

  @Override
  public void attributeDecl(String eName, String aName, String type, String mode, String value)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".attributeDecl0: " + eName + " . " + aName + " . " + type + " . " + mode + " . " + value);
    }

    ((Element) _node).setAttribute(aName, value);
  }

  //
  // DTDHandler start
  //

  @Override
  public void notationDecl(String name, String publicId, String systemId)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".notationDecl0: " + name + " . " + publicId + " . " + systemId);
    }
  }

  @Override
  public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".unparsedEntityDecl0: " + name + " . " + publicId + " . " + systemId + " . " + notationName);
    }
  }

  // DTDHandler end

  @Override
  public void internalEntityDecl(String name, String value)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".internalEntityDecl0: " + name + " . " + value);
    }

    _entityMap.put(name, value);
  }

  @Override
  public void externalEntityDecl(String name, String publicId, String systemId)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".externalEntityDecl0: " + name + " . " + publicId + " . " + systemId);
    }
  }

  //
  // LexicalHandler start
  //

  @Override
  public void comment(char []ch, int start, int length)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".comment0: " + new String(ch, start, length));
    }

    appendText();

    String str = new String(ch, start, length);

    Comment comment = getDocument().createComment(str);

    _node.appendChild(comment);
  }

  @Override
  public void startCDATA()
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".startCDATA0");
    }
  }

  @Override
  public void endCDATA()
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".endCDATA0");
    }
  }

  @Override
  public void startDTD(String name, String publicId, String systemId)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".startDTD0: " + name + " . " + publicId + " . " + systemId);
    }

    DocumentType type = _impl.createDocumentType(name, publicId, systemId);

    getDocument().appendChild(type);
  }

  @Override
  public void endDTD()
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".endDTD0");
    }
  }

  @Override
  public void startEntity(String name)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".startEntity0: " + name);
    }

    _entityName = name;

    //_sb.setLength(0);
  }

  @Override
  public void endEntity(String name)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".endEntity0: " + name);
    }

    //_entityMap.put(name, _sb.toString());
  }

  // LexicalHandler end

  @Override
  public InputSource getExternalSubset(String name, String baseURI)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".getExternalSubset0: " + name + " . " + baseURI);
    }

    return null;
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".resolveEntity0: " + publicId + " . " + systemId);
    }


    InputSource is = new InputSource(new StringReader(""));

    return is;
  }

  @Override
  public InputSource resolveEntity(String name, String publicId,
                                   String baseURI, String systemId)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".resolveEntity1: " + name + " . " + publicId + " . " + baseURI + " . " + systemId);
    }

    InputSource is = new InputSource(new StringReader(""));

    return is;
  }

  @Override
  public void skippedEntity(String name)
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".skippedEntity0: " + name);
    }

    EntityReference ref = getDocument().createEntityReference(name);

    _node.appendChild(ref);
  }

  @Override
  public void warning(SAXParseException e)
    throws SAXException
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".warning0: " + e);
    }
  }

  @Override
  public void error(SAXParseException e)
    throws SAXException
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".error0: " + e);
    }
  }

  @Override
  public void fatalError(SAXParseException e)
    throws SAXException
  {
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, getClass().getSimpleName() + ".fatalError0: " + e);
    }
  }
}
