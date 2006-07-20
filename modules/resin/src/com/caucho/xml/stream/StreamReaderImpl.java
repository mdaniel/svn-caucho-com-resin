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
 * @author Scott Ferguson, Adam Megacz
 */

package com.caucho.xml.stream;

import java.io.*;
import java.util.logging.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * XML pull-parser interface.
 */
public class StreamReaderImpl implements XMLStreamReader {
  private static final Logger log
    = Logger.getLogger(StreamReaderImpl.class.getName());
  private static final L10N L = new L10N(StreamReaderImpl.class);

  private static final boolean []IS_XML_NAME = new boolean[65536];
  
  private ReadStream _is;

  private int _col = 1;
  private int _row = 1;

  private String _version;
  private String _encoding;

  private int _token;
  private int _current;
  private int _state;
  private boolean _isShortTag;
  private boolean _eofEncountered = false;
  
  private RawName _rawTagName = new RawName();
  private QName _name;

  private RawName []_attrRawNames = new RawName[16];
  private QName []_attrNames = new QName[16];
  private String []_attrValues = new String[16];
  private int _attrCount;
  private final StringBuilder _sb = new StringBuilder();

  private char []_cBuf = new char[4096];
  private int _cBufLength;

  public StreamReaderImpl(InputStream is)
    throws XMLStreamException
  {
    _is = Vfs.openRead(is);

    readHeader();

    _token = 0;
    _current = START_DOCUMENT;
  }
  
  public int getAttributeCount()
  {
    return _attrCount;
  }
  
  public String getAttributeLocalName(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
					     _attrCount, index));
    
    return _attrNames[index].getLocalPart();
  }
  
  public QName getAttributeName(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
					     _attrCount, index));
    
    return _attrNames[index];
  }
  
  public String getAttributeNamespace(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
					     _attrCount, index));
    
    String ret = _attrNames[index].getNamespaceURI();

    // API quirk
    if ("".equals(ret))
      return null;
    
    return ret;
  }
  
  public String getAttributePrefix(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
					     _attrCount, index));
    
    String ret = _attrNames[index].getPrefix();
    
    // API quirk
    if ("".equals(ret))
      return null;

    return ret;
  }
  
  public String getAttributeType(int index)
  {
    return "CDATA";
  }
  
  public String getAttributeValue(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
					     _attrCount, index));
    
    return _attrValues[index];
  }
  
  public String getAttributeValue(String namespaceURI, String localName)
  {
    for (int i = _attrCount - 1; i >= 0; i--) {
      QName name = _attrNames[i];

      if (name.getLocalPart().equals(localName) &&
	  name.getNamespaceURI().equals(namespaceURI))
	return _attrValues[i];
    }
    
    return null;
  }
  
  public String getCharacterEncodingScheme()
  {
    return null;
  }
  
  public String getElementText() throws XMLStreamException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getEncoding()
  {
    return null;
  }
  
  public int getEventType()
  {
    return _current;
  }
  
  public String getLocalName()
  {
    return _name.getLocalPart();
  }
  
  public Location getLocation()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public QName getName()
  {
    if (_name != null)
      return _name;
    else
      /*
      throw new IllegalStateException(L.l("getName() must be called only on a START_ELEMENT or END_ELEMENT event"));
      */
      return null;
  }
  
  public NamespaceContext getNamespaceContext()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public int getNamespaceCount()
  {
    return 0;
  }
  
  public String getNamespacePrefix(int index)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getNamespaceURI()
  {
    String uri = _name.getNamespaceURI();

    // lame API quirk
    if ("".equals(uri))
      return null;

    return uri;
  }
  
  public String getNamespaceURI(int index)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getPIData()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getPITarget()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getPrefix()
  {
    String prefix = _name.getPrefix();

    // lame API quirk
    if ("".equals(prefix))
      return null;

    return prefix;
  }
  
  public Object getProperty(String name) throws IllegalArgumentException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public String getText()
  {
    return new String(_cBuf, 0, _cBufLength);
  }
  
  public char[] getTextCharacters()
  {
    return _cBuf;
  }
  
  public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public int getTextLength()
  {
    return _cBufLength;
  }
  
  public int getTextStart()
  {
    return 0;
  }
  
  public String getVersion()
  {
    return _version;
  }
  
  public boolean hasName()
  {
    return _name != null;
  }

  public boolean hasText()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isAttributeSpecified(int index)
  {
    return true;
  }
  
  public boolean isCharacters()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isEndElement()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isStandalone()
  {
    return false;
  }
  
  public boolean isStartElement()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isWhiteSpace()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public int nextTag() throws XMLStreamException
  {
    int tag;

    while ((tag = next()) != START_ELEMENT && tag != END_ELEMENT) {
      switch (tag) {
      case SPACE:
      case START_DOCUMENT:
      case COMMENT:
	break;

	// XXX: is this right?  BEA's reader appears to skip anything
	/*
      default:
	throw new IllegalStateException("in nextTag(), encountered " + tag);
	*/
      }
    }

    return tag;
  }
  
  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean standaloneSet()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean hasNext() throws XMLStreamException
  {
    if (_is == null)
      return false;

    if (_token == 0) {
      try {
	_token = readNext();
      } catch (IOException e) {
	throw new XMLStreamException(e);
      }
    }

    return !_eofEncountered || _token > 0;
  }
  
  public int next() throws XMLStreamException
  {
    int token = _token;
    _token = 0;

    if (token == 0) {
      try {
	token = readNext();
      } catch (IOException e) {
	throw new XMLStreamException(e);
      }
    }

    if (token > 0)
      return _current = token;
    else {
      if (_eofEncountered)
	return _current = -1;

      _eofEncountered = true;
      return _current = END_DOCUMENT;
    }
  }

  private int readNext()
    throws IOException, XMLStreamException
  {
    if (_isShortTag) {
      _isShortTag = false;

      return END_ELEMENT;
    }

    _name = null;
    
    int ch = read();

    if (ch == '<') {
      ch = read();

      if (ch == '/') {
	readRawName(_rawTagName);

	expect('>');

	_name = new QName(_rawTagName.getLocalName());

	return END_ELEMENT;

      } else if (ch == '!') {
	expect('-');
	expect('-');
	return readComment();
	
      } else {
	unread();

	readElementBegin();

	return START_ELEMENT;
      }
    }
    else if (ch < 0)
      return -1;
    else {
      unread();
      return readData();
    }
  }

  private void readElementBegin()
    throws IOException, XMLStreamException
  {
    readRawName(_rawTagName);

    int ch = readAttributes();

    if (ch == '>') {
    }
    else if (ch == '/') {
      _isShortTag = true;
      
      expect('>');
    }
    else
      throw error(L.l("Expected {0} at {1}", ">", charName(ch)));

    _name = new QName(_rawTagName.getLocalName());

    for (int i = _attrCount - 1; i >= 0; i--) {
      _attrNames[i] = new QName(_attrRawNames[i].getLocalName());
    }
  }

  private int readAttributes()
    throws IOException, XMLStreamException
  {
    int ch;
    int attrCount = 0;

    while ((ch = skipWhitespace()) >= 0 && IS_XML_NAME[ch]) {
      unread();

      if (_attrRawNames.length <= attrCount)
	extendAttrs();

      RawName rawName = _attrRawNames[attrCount];
      
      if (rawName == null) {
	rawName = new RawName();
	_attrRawNames[attrCount] = rawName;
      }

      readRawName(rawName);

      ch = skipWhitespace();

      if (ch != '=')
	throw error(L.l("attribute expects '=' at {0}", charName(ch)));

      ch = skipWhitespace();

      if (ch == '\'' || ch == '"') {
	if (rawName.getPrefix()==null &&
	    "xmlns".equals(rawName.getLocalName())) {
	  // XXX
	  readValue(ch);
	} else if ("xmlns".equals(rawName.getPrefix())) {
	  // XXX
	  readValue(ch);
	} else {
	  _attrValues[attrCount] = readValue(ch);
	}
      }
      else
	throw error(L.l("attribute expects value at {0}", charName(ch)));

      attrCount++;
    }

    _attrCount = attrCount;

    return ch;
  }

  private String readValue(int end)
    throws IOException, XMLStreamException
  {
    int ch;

    StringBuilder sb = _sb;
    sb.setLength(0);
    while ((ch = read()) > 0 && ch != end) {
      sb.append((char) ch);
    }

    return sb.toString();
  }

  private void extendAttrs()
  {
    int length = _attrRawNames.length;

    RawName []attrRawNames = new RawName[length + 16];
    System.arraycopy(_attrRawNames, 0, attrRawNames, 0, length);
    _attrRawNames = attrRawNames;

    QName []attrNames = new QName[length + 16];
    System.arraycopy(_attrNames, 0, attrNames, 0, length);
    _attrNames = attrNames;

    String []attrValues = new String[length + 16];
    System.arraycopy(_attrValues, 0, attrValues, 0, length);
    _attrValues = attrValues;
  }

  private int readData()
    throws IOException, XMLStreamException
  {
    int ch = 0;
    boolean isWhitespace = true;

    int index = 0;
    char []cBuf = _cBuf;
    int length = cBuf.length;
    boolean entity = false;
    loop:
    for (; index < length && (ch = read()) >= 0; index++) {
      switch (ch) {
      case '<':
	unread();
	break loop;
	
      case '&':
	if (index > 0) {
	  unread();
	  break loop;
	}
	cBuf[index] = (char) ch;
	entity = true;
	break;

      case '\r':
	ch = read();
	if (ch != '\n') { ch = '\r'; unread(); }
      case ' ': case '\t': case '\n':
	cBuf[index] = (char) ch;
	break;

      case ';':
	if (entity) {
	  String resolved = resolveEntity(new String(cBuf, 1, index-1));
	  resolved.getChars(0, resolved.length(), cBuf, 0);
	  index = resolved.length()-1;
	  entity = false;
	  break;
	}
      default:
	isWhitespace = false;
	cBuf[index] = (char) ch;
	break;
      }
    }

    if (entity)
      throw new Error("XXX: bad");

    _cBufLength = index;

    // whitespace surrounding the root element is "ignorable" per the XML spec
    boolean isIgnorableWhitespace =
      isWhitespace && ((ch == -1) || (_current == START_DOCUMENT));

    return isIgnorableWhitespace ? SPACE : CHARACTERS;
  }

  private String resolveEntity(String s)
    throws XMLStreamException
  {
    if ("amp".equals(s))    return "&";
    if ("apos".equals(s))   return "\'";
    if ("quot".equals(s))   return "\"";
    if ("lt".equals(s))     return "<";
    if ("gt".equals(s))     return ">";
    if (s.startsWith("#x"))
      return ""+((char)Integer.parseInt(s.substring(2), 16));
    if (s.startsWith("#"))
      return ""+((char)Integer.parseInt(s.substring(2)));

    throw new XMLStreamException("unknown entity: \"" + s + "\"");
  }

  private int readComment()
    throws IOException
  {
    int ch = 0;
    int index = 0;
    char []cBuf = _cBuf;
    int length = cBuf.length;
    loop:
    for (; index < length && (ch = read()) >= 0; index++) {
      cBuf[index] = (char) ch;
      if (index > 3
	  && cBuf[index-2] == '-'
	  && cBuf[index-1] == '-'
	  && cBuf[index-0] == '>') {
	index -= 2;
	break;
      }
    }

    _cBufLength = index;

    return COMMENT;
  }

  private void readRawName(RawName name)
    throws IOException, XMLStreamException
  {
    int length = 0;
    char []nameBuffer = name._buffer;
    int bufferLength = nameBuffer.length;
    int prefix = -1;

    int ch;

    while ((ch = read()) >= 0 && IS_XML_NAME[ch]) {
      if (bufferLength <= length) {
	name.expandCapacity();
	nameBuffer = name._buffer;
	bufferLength = nameBuffer.length;
      }

      if (ch == ':' && prefix < 0)
	prefix = length;
      
      nameBuffer[length++] = (char) ch;
    }
    unread();

    name._length = length;
    name._prefix = prefix;
  }

  private static boolean isXmlName(int ch)
  {
    return ('a' <= ch && ch <= 'z'
	    || 'A' <= ch && ch <= 'Z'
	    || 'A' <= ch && ch <= '9'
	    || ch == ':'
	    || ch == '+'
	    || ch == '_'
	    || ch == '-');
  }

  private int skipWhitespace()
    throws IOException
  {
    int ch;

    while ((ch = read()) >= 0 &&
	   (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')) {
    }

    return ch;
  }
  
  /**
   * Reads the <?xml ... ?> declaraction
   */
  private void readHeader()
    throws XMLStreamException
  {
    // The reading at this point must use the underlying stream because
    // the encoding is not determined until the end of the declaration
    try {
      int ch;

      ch = read();

      if (ch == (char)0xFE) {
	if (read() != (char)0xFF) {
	  throw new XMLStreamException("found unrecognized BOM");
	}
	ch = read();
      } else if (ch == (char)0xFF) {
	if (read() != (char)0xFE) {
	  throw new UnsupportedOperationException("found byte-swapped BOM");
	} else {
	  throw new XMLStreamException("found unrecognized BOM");
	}
      }

      if (ch != '<') {
	unread();
      }
      else if ((ch = read()) != '?') {
	unread();
	unread();
      }
      else if ((ch = read()) != 'x') {
	unread();
	unread();
	unread();
      }
      else if ((ch = read()) != 'm') {
	unread();
	unread();
	unread();
	unread();
      }
      else if ((ch = read()) != 'l') {
	unread();
	unread();
	unread();
	unread();
	unread();
      }
      else {
	while ((ch = read()) >= 0 && ch != '?') {
	}

	ch = read();
	if (ch != '>')
	  throw error(L.l("Expected '>' at end of '<?xml' declaration at {0}",
			  charName(ch)));
      }
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /**
   * Reads and validate next character.
   */
  private void expect(int expect)
    throws IOException, XMLStreamException
  {
    int ch = read();

    if (ch != expect)
      throw error(L.l("expected {0} at {1}", charName(expect), charName(ch)));
  }

  /**
   * Reads the next character.
   */
  private int read()
    throws IOException
  {
    // XXX: need to add buffer
    
    int i = _is.readChar();

    if (i == '\n') {
      _col = 1;
      _row++;
    } else if (i != -1) {
      _col++;
    }
    return i;
  }

  /**
   * Unreads the last character.
   */
  private void unread()
    throws IOException
  {
    _is.unread();
    int i = _is.read();
    _is.unread();

    if (i == -1)
      return;
    
    if (i == '\n') {
      _row--;
      _col = 1; // XXX
    } else {
      _col--;
    }
      
  }

  private String charName(int ch)
  {
    if (ch > 0x20 && ch <= 0x7f)
      return "'" + (char) ch + "'";
    else
      return "0x" + Integer.toHexString(ch);
  }

  private XMLStreamException error(String s)
  {
    return new XMLStreamException(s + " " + location());
  }

  private String location()
  {
    return "(line " + _row + ", col " + _col +")";
  }
  
  public void close() throws XMLStreamException
  {
    ReadStream is = _is;
    _is = null;

    if (is != null) {
      try {
	is.close();
      } catch (IOException e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  static class RawName {
    char []_buffer = new char[64];
    int _prefix;
    int _length;

    public String toString()
    {
      return new String(_buffer, 0, _length);
    }

    String getLocalName()
    {
      return new String(_buffer, _prefix + 1, _length - _prefix - 1);
    }

    String getPrefix()
    {
      if (_prefix==-1) return null;
      return new String(_buffer, 0, _prefix);
    }
    
    void expandCapacity()
    {
      char []newBuffer = new char[_buffer.length + 64];
      
      System.arraycopy(_buffer, 0, newBuffer, 0, _buffer.length);

      _buffer = newBuffer;
    }
  }
  /*
  static class NSContext {

    NSContext _parent;

    public NSContext(NSContext parent)
    {
      _parent = parent;
    }
  }
  */
  static {
    for (int i = 0; i < IS_XML_NAME.length; i++)
      IS_XML_NAME[i] = isXmlName(i);
  }
}
