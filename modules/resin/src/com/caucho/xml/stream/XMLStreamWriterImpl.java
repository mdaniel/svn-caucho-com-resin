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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

public class XMLStreamWriterImpl implements XMLStreamWriter {
  private static final Logger log
    = Logger.getLogger(XMLStreamReaderImpl.class.getName());

  private WriteStream _ws;
  private NamespaceContextImpl _tracker = new NamespaceContextImpl();

  private QName _pendingTagName = null;
  private boolean _shortTag = false;
  private ArrayList<QName> _pendingAttributeNames = new ArrayList<QName>();
  private ArrayList<String> _pendingAttributeValues = new ArrayList<String>();

  public XMLStreamWriterImpl(WriteStream ws)
  {
    _ws = ws;
  }

  public void close() throws XMLStreamException
  {
    try {
      flushPending();
      _ws.close();
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void flush() throws XMLStreamException
  {
    try {
      _ws.flush();
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public NamespaceContext getNamespaceContext()
  {
    return _tracker;
  }

  public String getPrefix(String uri)
    throws XMLStreamException
  {
    return _tracker.getPrefix(uri);
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new PropertyNotSupportedException(name);
  }

  public void setDefaultNamespace(String uri)
    throws XMLStreamException
  {
    _tracker.declare("", uri);
  }

  public void setNamespaceContext(NamespaceContext context)
    throws XMLStreamException
  {
    String message = "please do not set the NamespaceContext";
    throw new UnsupportedOperationException(message);
  }

  public void setPrefix(String prefix, String uri)
    throws XMLStreamException
  {
    _tracker.declare(prefix, uri);
  }

  public void writeAttribute(String localName, String value)
    throws XMLStreamException
  {
    _pendingAttributeNames.add(new QName(localName));
    _pendingAttributeValues.add(value);
  }

  public void writeAttribute(String namespaceURI, String localName,
                             String value)
    throws XMLStreamException
  {
    writeAttribute(_tracker.declare(namespaceURI),
                   namespaceURI,
                   localName,
                   value);
  }

  public void writeAttribute(String prefix, String namespaceURI,
                             String localName, String value)
    throws XMLStreamException
  {
    _tracker.declare(prefix, namespaceURI);
    _pendingAttributeNames.add(new QName(namespaceURI, localName, prefix));
    _pendingAttributeValues.add(value);
  }

  public void writeCData(String data)
    throws XMLStreamException
  {
    flushPending();
    try {
      _ws.print("<![CDATA[");
      _ws.print(data);
      _ws.print("]]>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException
  {
    flushPending();
    try {
      Escapifier.escape(text, start, len, _ws);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(String text)
    throws XMLStreamException
  {
    flushPending();
    try {
      Escapifier.escape(text, _ws);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeComment(String data)
    throws XMLStreamException
  {
    flushPending();
    try {
      _ws.print("<!--");
      _ws.print(data);
      _ws.print("-->");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException
  {
    _tracker.declare("", namespaceURI);
  }

  public void writeDTD(String dtd)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public void writeEmptyElement(String localName)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(localName);
      pushContext(qname);
      _pendingTagName = qname;
      _shortTag = true;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    writeEmptyElement(_tracker.declare(namespaceURI),
                      localName,
                      namespaceURI);
  }

  public void writeEmptyElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(namespaceURI, localName, prefix);
      pushContext(qname);
      _pendingTagName = qname;
      _shortTag = true;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEndDocument()
    throws XMLStreamException
  {
  }

  public void writeEndElement()
    throws XMLStreamException
  {
    flushPending();
    try {
      QName name = popContext();
      _ws.print("</");
      _ws.print(printQName(name));
      _ws.print(">");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  private static String printQName(QName name) {

    if (name.getPrefix() == null || name.getPrefix().equals(""))
      return name.getLocalPart();

    return name.getPrefix() + ":" + name.getLocalPart();
  }

  public void writeEntityRef(String name)
    throws XMLStreamException
  {
    flushPending();
    try {
      _ws.print("&");
      _ws.print(name);
      _ws.print(";");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException
  {
    _tracker.declare(prefix, namespaceURI);
  }

  public void writeProcessingInstruction(String target)
    throws XMLStreamException
  {
    flushPending();
    try {
      _ws.print("<? ");
      _ws.print(target);
      _ws.print(" ?>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    flushPending();
    try {
      _ws.print("<? ");
      _ws.print(target);
      _ws.print(" ");
      _ws.print(data);
      _ws.print(" ?>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartDocument()
    throws XMLStreamException
  {
    writeStartDocument("1.0");
  }

  public void writeStartDocument(String version)
    throws XMLStreamException
  {
    writeStartDocument(version, "utf-8");
  }

  public void writeStartDocument(String version, String encoding)
    throws XMLStreamException
  {
    try {
      _ws.print("<?xml version='"+version+"' encoding='"+encoding+"'?>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String localName)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(localName);
      pushContext(qname);
      _pendingTagName = qname;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    writeStartElement(_tracker.declare(namespaceURI),
                      localName,
                      namespaceURI);
  }

  public void writeStartElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(namespaceURI, localName, prefix);
      pushContext(qname);
      _pendingTagName = qname;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private boolean _flushed = true;

  private void pushContext(QName elementName)
    throws IOException
  {
    flushContext();
    _tracker.push(elementName);
    _flushed = false;
  }

  private QName popContext()
    throws IOException
  {
    flushContext();
    QName name = _tracker.getTagName();
    _tracker.pop();
    return name;
  }

  private void flushContext()
    throws IOException
  {
    if (_flushed) return;
    _tracker.emitDeclarations(_ws);
    _flushed = true;
  }

  private void flushPending()
    throws XMLStreamException
  {
    try {
      if (_pendingTagName == null) return;
      
      _ws.print("<");
      _ws.print(printQName(_pendingTagName));
      
      for(int i=0; i<_pendingAttributeNames.size(); i++) {
        _ws.print(" ");
        _ws.print(printQName(_pendingAttributeNames.get(i)));
        _ws.print("='");
        Escapifier.escape(_pendingAttributeValues.get(i), _ws);
        _ws.print("'");
      }
      flushContext();

      if (_shortTag) {
        _ws.print("/>");
        popContext();
      } else {
        _ws.print(">");
      }
      
      _pendingTagName = null;
      _pendingAttributeNames.clear();
      _pendingAttributeValues.clear();
      _shortTag = false;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }
}
