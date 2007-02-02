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
import com.caucho.xml.QDocument;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DOMResultXMLStreamWriterImpl implements XMLStreamWriter {
  private static final Logger log
    = Logger.getLogger(DOMResultXMLStreamWriterImpl.class.getName());
  private static final L10N L
    = new L10N(DOMResultXMLStreamWriterImpl.class);

  private DOMResult _result;
  private Document _document;
  private Node _current;
  private boolean _currentIsEmpty = false;

  private SimpleNamespaceContext _context = new SimpleNamespaceContext(null);

  public DOMResultXMLStreamWriterImpl(DOMResult result)
    throws XMLStreamException
  {
    _result = result;

    _current = result.getNode();

    if (_current == null) {
      _current = _document = new QDocument();
      result.setNode(_document);
    }
    else {
      if (_current.getNodeType() == Node.DOCUMENT_NODE)
        _document = (Document) _current;
      else 
        _document = _current.getOwnerDocument();
    }
  }

  public void close() 
    throws XMLStreamException
  {
    writeEndDocument();
  }

  public void flush() 
    throws XMLStreamException
  {
  }

  public NamespaceContext getNamespaceContext()
  {
    return _context;
  }

  public String getPrefix(String uri)
    throws XMLStreamException
  {
    return _context.getPrefix(uri);
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new PropertyNotSupportedException(name);
  }

  public void setDefaultNamespace(String uri)
    throws XMLStreamException
  {
    _context.declare("", uri);
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
    _context.declare(prefix, uri);
  }

  public void writeAttribute(String localName, String value)
    throws XMLStreamException
  {
    try {
      ((Element) _current).setAttribute(localName, value);
    }
    catch (ClassCastException e) {
      throw new XMLStreamException(e);
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeAttribute(String namespaceURI, String localName,
                             String value)
    throws XMLStreamException
  {
    writeAttribute(_context.declare(namespaceURI),
                   namespaceURI,
                   localName,
                   value);
  }

  public void writeAttribute(String prefix, String namespaceURI,
                             String localName, String value)
    throws XMLStreamException
  {
    try {
      _context.declare(prefix, namespaceURI);
      ((Element) _current).setAttributeNS(namespaceURI, 
                                          prefix + ":" + localName, value);
    }
    catch (ClassCastException e) {
      throw new XMLStreamException(e);
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCData(String data)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createCDATASection(data));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException
  {
    writeCharacters(new String(text, start, len));
  }

  public void writeCharacters(String text)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createTextNode(text));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeComment(String data)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createComment(data));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException
  {
    _context.declare("", namespaceURI);
  }

  public void writeDTD(String dtd)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public void writeEmptyElement(String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    try {
      Node parent = _current;
      _current = _document.createElement(localName);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = true;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    writeEmptyElement(_context.declare(namespaceURI),
                      localName,
                      namespaceURI);
  }

  public void writeEmptyElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    try {
      Node parent = _current;
      _current = _document.createElementNS(namespaceURI, 
                                           prefix + ":" + localName);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = true;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEndDocument()
    throws XMLStreamException
  {
    while (_context != null)
      popContext();
  }

  public void writeEndElement()
    throws XMLStreamException
  {
    try {
      popContext();

      if (_currentIsEmpty)
        popContext();

      _currentIsEmpty = false;
    } 
    catch (ClassCastException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEntityRef(String name)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createEntityReference(name));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException
  {
    _context.declare(prefix, namespaceURI);
  }

  public void writeProcessingInstruction(String target)
    throws XMLStreamException
  {
    writeProcessingInstruction(target, "");
  }

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createProcessingInstruction(target, data));
    }
    catch (DOMException e) {
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
      _document.setXmlVersion(version);
      // XXX encoding
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      Node parent = _current;
      _current = _document.createElement(localName);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = false;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    writeStartElement(_context.declare(namespaceURI),
                      localName,
                      namespaceURI);
  }

  public void writeStartElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      Node parent = _current;
      _current = _document.createElementNS(namespaceURI, 
                                           prefix + ":" + localName);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = false;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  //////////////////////////////////////////////////////////////////////////

  private void pushContext()
  {
    _context = new SimpleNamespaceContext(_context);
  }

  private void popContext()
  {
    if (_current instanceof Element) {
      Element element = (Element) _current;
      
      for (Map.Entry<String,String> entry : 
           _context.getPrefixMap().entrySet()) {

        if ("".equals(entry.getKey()))
          element.setAttribute("xmlns", entry.getValue());
        else
          element.setAttributeNS("http://www.w3.org/2000/xmlns/", 
                                 "xmlns:" + entry.getKey(),
                                 entry.getValue());
      }
    }

    if (_context != null)
      _context = _context.getParent();

    _current = _current.getParentNode();
  }

  // XXX switch to NamespaceWriterContext
  private static class SimpleNamespaceContext implements NamespaceContext {
    private HashMap<String,String> _uris = new HashMap<String,String>();
    private HashMap<String,List<String>> _prefixes
      = new HashMap<String,List<String>>();
    private SimpleNamespaceContext _parent;
    private int _prefixCounter = 0;

    public SimpleNamespaceContext(SimpleNamespaceContext parent)
    {
      _parent = parent;
    }

    public String getNamespaceURI(String prefix)
    {
      return _uris.get(prefix);
    }

    public String getPrefix(String namespaceURI)
    {
      List<String> prefixes = _prefixes.get(namespaceURI);

      if (prefixes == null || prefixes.size() == 0)
        return null;

      return prefixes.get(0);
    }

    public Iterator getPrefixes(String namespaceURI)
    {
      List<String> prefixes = _prefixes.get(namespaceURI);

      if (prefixes == null) {
        prefixes = new ArrayList<String>();
        _prefixes.put(namespaceURI, prefixes);
      }

      return prefixes.iterator();
    }

    public HashMap<String,String> getPrefixMap()
    {
      return _uris;
    }

    public String declare(String namespaceURI)
    {
      String prefix = "ns" + _prefixCounter;
      declare(prefix, namespaceURI);
      _prefixCounter++;

      return prefix;
    }

    public void declare(String prefix, String namespaceURI)
    {
      _uris.put(prefix, namespaceURI);

      List<String> prefixes = _prefixes.get(namespaceURI);

      if (prefixes == null) {
        prefixes = new ArrayList<String>();
        _prefixes.put(namespaceURI, prefixes);
      }

      prefixes.add(prefix);
    }

    public SimpleNamespaceContext getParent()
    {
      return _parent;
    }
  }
}
