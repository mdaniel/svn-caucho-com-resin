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
  private int _ofs = 1;

  private NamespaceTracker _namespaceTracker = new NamespaceTracker();

  private String _version;
  private String _encoding;

  private String _publicId;
  private String _systemId;

  private int _token;
  private int _current;
  private int _state;
  private boolean _isShortTag;
  private boolean _isWhitespace = false;

  private boolean _eofEncountered = false;

  private String _processingInstructionTarget;
  private String _processingInstructionData;

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
    this(Vfs.openRead(is));
  }

  public StreamReaderImpl(Reader r)
    throws XMLStreamException
  {
    this(Vfs.openRead(r));
  }

  public StreamReaderImpl(InputStream is, String systemId)
    throws XMLStreamException
  {
    this(Vfs.openRead(is));
    _systemId = systemId;
  }

  public StreamReaderImpl(Reader r, String systemId)
    throws XMLStreamException
  {
    this(Vfs.openRead(r));
    _systemId = systemId;
  }

  public StreamReaderImpl(ReadStream is)
    throws XMLStreamException
  {
    _is = is;

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
    return _encoding;
  }

  public String getElementText() throws XMLStreamException
  {
    return getText();
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
    return new StreamReaderLocation(_ofs, _row, _col);
  }

  public QName getName()
  {
    if (_name != null)
      return _name;
    else
      return null;
  }

  public NamespaceContext getNamespaceContext()
  {
    return _namespaceTracker;
  }

  public int getNamespaceCount()
  {
    return _namespaceTracker.getNumDecls();
  }

  public String getNamespacePrefix(int index)
  {
    return _namespaceTracker.getPrefix(index);
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
    return _namespaceTracker.getUri(index);
  }

  public String getNamespaceURI(String prefix)
  {
    return _namespaceTracker.getUri(prefix);
  }

  public String getPIData()
  {
    if (_current != PROCESSING_INSTRUCTION)
      return null;
    return _processingInstructionData;
  }

  public String getPITarget()
  {
    if (_current != PROCESSING_INSTRUCTION)
      return null;
    return _processingInstructionTarget;
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
    if ("javax.xml.stream.notations".equals(name)) {
      throw new UnsupportedOperationException(getClass().getName());
    }
    else if ("javax.xml.stream.entities".equals(name)) {
      throw new UnsupportedOperationException(getClass().getName());
    }
    else {
      throw
        new IllegalArgumentException("property \""+name+"+\" not supported");
    }
  }

  public String getText()
  {
    return new String(_cBuf, 0, _cBufLength);
  }

  public char[] getTextCharacters()
  {
    return _cBuf;
  }

  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException
  {
    getText().getChars(sourceStart,
                       sourceStart + length,
                       target,
                       targetStart);
    return length;
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
    switch(getEventType()) {
    case CHARACTERS:
    case DTD:
    case ENTITY_REFERENCE:
    case COMMENT:
    case SPACE:
      return true;
    default:
      return false;
    }
  }

  public boolean isAttributeSpecified(int index)
  {
    return index < _attrCount;
  }

  public boolean isCharacters()
  {
    return _current == CHARACTERS;
  }

  public boolean isEndElement()
  {
    return _current == END_ELEMENT;
  }

  public boolean isStandalone()
  {
    return false;
  }

  public boolean isStartElement()
  {
    return _current == START_ELEMENT;
  }

  public boolean isWhiteSpace()
  {
    return
      _isWhitespace &&
      (_current == CHARACTERS || _current == SPACE);
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
    if (type != _current)
      throw new XMLStreamException("expected " + constantToString(type) + ", "+
                                   "found " + constantToString(_current) +
                                   " at " + getLocation());

    if (localName != null && !localName.equals(getLocalName()))
      throw new XMLStreamException("expected <"+localName+">, found " +
                                   "<"+getLocalName()+"> at " + getLocation());

    if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI()))
      throw new XMLStreamException("expected xmlns="+namespaceURI+
                                   ", found xmlns="+getNamespaceURI() +
                                   " at " + getLocation());
  }

  public boolean standaloneSet()
  {
    return isStandalone();
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

    // we pop the namespace context when the user is finished
    // working with the END_ELEMENT event
    if (_current == END_ELEMENT)
        _namespaceTracker.pop();

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

        _name = _rawTagName.resolve(_namespaceTracker);

        return END_ELEMENT;

      } else if (ch == '!') {
        expect('-');
        expect('-');
        return readComment();

      } else if (ch == '?') {
        readProcessingDirective();
        return PROCESSING_INSTRUCTION;

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
    _namespaceTracker.push(null);

    int ch = readAttributes();

    _namespaceTracker.setTagName(_rawTagName.resolve(_namespaceTracker));

    if (ch == '>') {
    }
    else if (ch == '/') {
      _isShortTag = true;

      expect('>');
    }
    else
      throw error(L.l("Expected {0} at {1}", ">", charName(ch)));

    for (int i = _attrCount - 1; i >= 0; i--) {
      _attrNames[i] = _attrRawNames[i].resolve(_namespaceTracker);
    }
    _name = _rawTagName.resolve(_namespaceTracker);
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

          _namespaceTracker.declare(null, readValue(ch));
                            
        } else if ("xmlns".equals(rawName.getPrefix())) {

          _namespaceTracker.declare(rawName.getLocalName(),
                                    readValue(ch));

        } else {
          _attrValues[attrCount++] = readValue(ch);
        }
      }
      else
        throw error(L.l("attribute expects value at {0}", charName(ch)));
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
    _isWhitespace = true;

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
        _isWhitespace = false;
        cBuf[index] = (char) ch;
        break;
      }
    }

    if (entity)
      throw new Error("XXX: bad");

    _cBufLength = index;

    // whitespace surrounding the root element is "ignorable" per the XML spec
    boolean isIgnorableWhitespace =
      _isWhitespace &&
      ((ch == -1) ||
       (_current == START_DOCUMENT) ||
       (_current == PROCESSING_INSTRUCTION));

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

  private void readProcessingDirective()
    throws IOException
  {
    CharBuffer target = new CharBuffer();
    CharBuffer data   = null;

    while(true) {
      int ch = read();

      if (ch == -1)
        return;  /* XXX: error? */

      if (ch == '?') {
        int next = read();
        if (next == '>') {
          _processingInstructionTarget = target.toString();
          _processingInstructionData = data.toString();
          return;
        }
        unread();
      }

      if (data == null && (ch == ' ' || ch == '\r' || ch == '\n')) {
        data = new CharBuffer();
        continue;
      }

      if (data != null)
        data.append((char)ch);
      else
        target.append((char)ch);
    }
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

        CharBuffer directive = new CharBuffer();
        while ((ch = read()) >= 0 && ch != '?') {
          directive.append((char)ch);
        }

        String data = directive.toString().trim();
        if (data.startsWith("version")) {
          data = data.substring(7).trim();
          data = data.substring(1).trim();  // remove "="
          char quot = data.charAt(0);
          _version = data.substring(1, data.indexOf(quot, 1));
          data = data.substring(data.indexOf(quot, 1)+1).trim();
        }
        if (data.startsWith("encoding")) {
          data = data.substring(8).trim();
          data = data.substring(1).trim();  // remove "="
          char quot = data.charAt(0);
          _encoding = data.substring(1, data.indexOf(quot, 1));
          data = data.substring(data.indexOf(quot, 1)+1).trim();
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
    _ofs++;
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
    _ofs--;

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

    public QName resolve(NamespaceContext nsc)
    {
      if (getPrefix() == null)
        return new QName(nsc.getNamespaceURI(null),
                         getLocalName());
      return new QName(nsc.getNamespaceURI(getPrefix()),
                       getLocalName(),
                       getPrefix());
    }

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

  private class StreamReaderLocation implements Location {

    private int _ofs;
    private int _row;
    private int _col;

    public StreamReaderLocation(int ofs, int row, int col)
    {
      this._ofs = ofs;
      this._row = row;
      this._col = col;
    }

    public int getCharacterOffset()
    {
      return _ofs;
    }

    public int getColumnNumber()
    {
      return _col;
    }

    public int getLineNumber()
    {
      return _row;
    }

    public String getPublicId()
    {
      return _publicId;
    }

    public String getSystemId()
    {
      return _systemId;
    }

    public String toString() {
      return _row+":"+_col;
    }

  }

  private static String constantToString(int constant) {

    switch(constant) {

    case ATTRIBUTE: return "ATTRIBUTE";
    case CDATA: return "CDATA";
    case CHARACTERS: return "CHARACTERS";
    case COMMENT: return "COMMENT";
    case DTD: return "DTD";
    case END_DOCUMENT: return "END_DOCUMENT";
    case END_ELEMENT: return "END_ELEMENT";
    case ENTITY_DECLARATION: return "ENTITY_DECLARATION";
    case ENTITY_REFERENCE: return "ENTITY_REFERENCE";
    case NAMESPACE: return "NAMESPACE";
    case NOTATION_DECLARATION: return "NOTATION_DECLARATION";
    case PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
    case SPACE: return "SPACE";
    case START_DOCUMENT: return "START_DOCUMENT";
    case START_ELEMENT: return "START_ELEMENT";
    default:
      throw new RuntimeException("constantToString("+constant+") unknown");
    }

  }

}
