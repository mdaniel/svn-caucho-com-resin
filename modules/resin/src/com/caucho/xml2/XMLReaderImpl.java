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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xml2;

import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;

import org.xml.sax.Parser;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.caucho.util.L10N;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.TempCharBuffer;

import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;
import com.caucho.xml.ExtendedLocator;

/**
 * A fast XML parser.
 */
public class XMLReaderImpl implements XMLReader {
  private static final L10N L = new L10N(XMLReaderImpl.class);
  
  // Xerces uses the following
  public static final String XMLNS = "http://www.w3.org/2000/xmlns/";

  static final QName DOC_NAME = new QName(null, "#document", null);
  static final QName TEXT_NAME = new QName(null, "#text", null);
  static final QName JSP_NAME = new QName(null, "#jsp", null);
  static final QName WHITESPACE_NAME = new QName(null, "#whitespace", null);
  static final QName JSP_ATTRIBUTE_NAME = new QName("xtp", "jsp-attribute", null);
  static final String LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";

  private ContentHandler _contentHandler;
  private EntityResolver _entityResolver;
  private DTDHandler _dtdHandler;
  private ErrorHandler _errorHandler;

  private Reader _reader;

  private final AttributesImpl _attributes = new AttributesImpl();
  private final ExtendedLocator _locator = new LocatorImpl();

  private char []_valueBuf;
  private char []_inputBuf;
  private int _inputOffset;
  private int _inputLength;

  private String _filename;
  private String _systemId;
  private String _publicId;
  private int _line;

  /**
   * Returns a SAX feature.
   *
   * <p>All XMLReaders are required to recognize the
   * http://xml.org/sax/features/namespaces and the
   * http://xml.org/sax/features/namespace-prefixes feature names.</p>
   */
  public boolean getFeature(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    throw new SAXNotRecognizedException(name);
  }

  /**
   * Sets a SAX property.
   */
  public void setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    if (LEXICAL_HANDLER.equals(name)) {
    }
    else
      throw new SAXNotRecognizedException(name);
  }

  /**
   * Returns a SAX property.
   */
  public Object getProperty(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    throw new SAXNotRecognizedException(name);
  }

  /**
   * Sets a SAX feature.
   */
  public void setFeature(String name, boolean value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    throw new SAXNotRecognizedException(name);
  }
  
  /**
   * Sets the SAX entityResolver.
   *
   * @param resolver the entity resolver
   */
  public void setEntityResolver(EntityResolver resolver)
  {
    _entityResolver = resolver;
  }
  
  /**
   * Gets the SAX entityResolver.
   *
   * @return the entity resolver
   */
  public EntityResolver getEntityResolver()
  {
    return _entityResolver;
  }

  /**
   * Sets the SAX DTD handler
   *
   * @param handler the dtd handler
   */
  public void setDTDHandler(DTDHandler handler)
  {
    _dtdHandler = handler;
  }

  /**
   * Gets the SAX DTD handler
   *
   * @return the dtd handler
   */
  public DTDHandler getDTDHandler()
  {
    return _dtdHandler;
  }

  /**
   * Sets the SAX content handler
   *
   * @param handler the content handler
   */
  public void setContentHandler(ContentHandler handler)
  {
    _contentHandler = handler;
  }

  /**
   * Gets the SAX content handler
   *
   * @param handler the content handler
   */
  public ContentHandler getContentHandler()
  {
    return _contentHandler;
  }

  /**
   * Sets the SAX errorHandler.
   *
   * @param handler the error handler
   */
  public void setErrorHandler(ErrorHandler handler)
  {
    _errorHandler = handler;
  }

  /**
   * Gets the SAX errorHandler.
   *
   * @param handler the error handler
   */
  public ErrorHandler getErrorHandler()
  {
    return _errorHandler;
  }
  
  /**
   * parses the input source.
   *
   * @param source the source to parse from
   */
  public void parse(InputSource source)
    throws IOException, SAXException
  {
    InputStream is = source.getByteStream();
    if (is != null) {
      _systemId = source.getSystemId();
      
      if (is instanceof ReadStream) {
	_filename = ((ReadStream) is).getPath().getUserPath();
	if (_systemId == null)
	  _systemId = ((ReadStream) is).getPath().getURL();
      }
      else {
	_filename = _systemId;
      }

      _reader = new java.io.InputStreamReader(is);
      
      parseImpl();
    }
    else
      throw new IllegalArgumentException();
  }
  
  /**
   * Parses the file at the given string
   *
   * @param url the source url to parse from
   */
  public void parse(String systemId)
    throws IOException, SAXException
  {
    ReadStream is = Vfs.lookup(systemId).openRead();

    _reader = is.getReader();
    _systemId = systemId;
    _filename = systemId;
    try {
      parseImpl();
    } finally {
      _reader = null;
    }
  }
  
  /**
   * Parses the file at the given string
   *
   * @param url the source url to parse from
   */
  private void parseImpl()
    throws IOException, SAXException
  {
    TempCharBuffer inputBuffer = TempCharBuffer.allocate();
    TempCharBuffer valueBuffer = TempCharBuffer.allocate();
    try {
      _valueBuf = valueBuffer.getBuffer();
      _inputBuf = inputBuffer.getBuffer();
      _inputLength = 0;
      _inputOffset = 0;
      _line = 1;

      _contentHandler.setDocumentLocator(_locator);
      _contentHandler.startDocument();

      parseContent();
      
      _contentHandler.endDocument();
    } finally {
      _inputBuf = null;
      _valueBuf = null;
      
      TempCharBuffer.free(inputBuffer);
      TempCharBuffer.free(valueBuffer);
    }
  }

  /**
   * Parses XML content.
   */
  private void parseContent()
    throws IOException, SAXException
  {
    char []valueBuffer = _valueBuf;
    int valueLength = valueBuffer.length;
    int valueOffset = 0;

    boolean isWhitespace = true;
    boolean seenCr = false;

    while (true) {
      if (_inputLength == _inputOffset && ! fillBuffer()) {
	writeText(valueBuffer, valueOffset, isWhitespace);
	return;
      }

      char ch = _inputBuf[_inputOffset++];

      switch (ch) {
      case ' ': case '\t':
	if (valueOffset < valueLength)
	  valueBuffer[valueOffset++] = ch;
	else {
	  writeText(valueBuffer, valueOffset, isWhitespace);
	  valueOffset = 0;
	}
	break;

      case '\n':
	if (valueOffset < valueLength)
	  valueBuffer[valueOffset++] = ch;
	else {
	  writeText(valueBuffer, valueOffset, isWhitespace);
	  valueOffset = 0;
	}
	_line++;
	break;

      case '\r':
	if (valueOffset < valueLength)
	  valueBuffer[valueOffset++] = ch;
	else {
	  writeText(valueBuffer, valueOffset, isWhitespace);
	  valueOffset = 0;
	}

	addCarriageReturnLine();
	break;

      case '<':
	if (valueOffset > 0) {
	  writeText(valueBuffer, valueOffset, isWhitespace);
	  valueOffset = 0;
	}
	
	if (_inputLength == _inputOffset && ! fillBuffer())
	  error("XXX: unexpected eof");

	ch = _inputBuf[_inputOffset];
	switch (ch) {
	case '!':
	  break;
	case '?':
	  break;
	case '/':
	  _inputOffset++;
	  return;
	default:
	  parseElement();
	  break;
	}

	isWhitespace = true;
	break;

      case '&':
	if (valueOffset > 0) {
	  writeText(valueBuffer, valueOffset, isWhitespace);
	  valueOffset = 0;
	}
	isWhitespace = true;
	break;

      default:
	isWhitespace = false;
	if (valueOffset < valueLength)
	  valueBuffer[valueOffset++] = ch;
	else {
	  writeText(valueBuffer, valueOffset, false);
	  valueOffset = 0;
	}
	break;
      }
    }
  }

  /**
   * Parses the element.
   */
  private void parseElement()
    throws IOException, SAXException
  {
    String name = parseName();

    _attributes.clear();

    while (true) {
      int ch = read();

      switch (ch) {
      case -1:
	throw error("XXX: unexpected eof");

      case ' ': case '\t':
	break;

      case '\r':
	addCarriageReturnLine();
	break;

      case '\n':
	_line++;
	break;

      case '/':
	if ((ch = read()) != '>')
	  throw error("XXX: expected '>'");

	_contentHandler.startElement("", "", name, _attributes);
	_contentHandler.endElement("", "", name);

	return;
	
      case '>':
	_contentHandler.startElement("", "", name, _attributes);

	parseContent();

	String tailName = parseName();

	if ((ch = read()) != '>')
	  throw error("XXX: expected '>'");

	if (! name.equals(tailName))
	  throw error("XXX: mismatch name");

	_contentHandler.endElement("", "", name);
	
	return;

      default:
	if (XmlChar.isNameStart(ch)) {
	  unread();
	  
	  String attrName = parseName();
	  ch = skipWhitespace(read());

	  if (ch != '=')
	    throw error(L.l("Expected '=' for attribute value at {0}.",
			  badChar(ch)));

	  String attrValue = parseValue();

	  _attributes.add(new QName(attrName, null), attrValue);
	}
	else
	  throw error(L.l("{0} is an unexpected character in element.",
			  badChar(ch)));
      }
    }
  }

  /**
   * Parses a name.
   */
  private String parseName()
    throws IOException
  {
    int valueOffset = 0;

    while (true) {
      if (_inputLength == _inputOffset && ! fillBuffer()) {
	return new String(_valueBuf, 0, valueOffset);
      }

      char ch = _inputBuf[_inputOffset++];

      if (XmlChar.isNameChar(ch))
	_valueBuf[valueOffset++] = ch;
      else {
	_inputOffset--;

	return new String(_valueBuf, 0, valueOffset);
      }
    }
  }

  /**
   * Writes text data.
   */
  private void writeText(char []buffer, int length, boolean isWhitespace)
    throws SAXException
  {
  }

  /**
   * Adds the line for cr
   */
  private void addCarriageReturnLine()
    throws IOException
  {
    if (_inputLength == _inputOffset && ! fillBuffer())
      _line++;
    else if (_inputBuf[_inputOffset] != '\n')
      _line++;
  }

  /**
   * Parses an attribute value.
   */
  private String parseValue()
    throws IOException, SAXException
  {
    int end = skipWhitespace(read());

    if (end != '\'' && end != '"')
      throw error(L.l("expected quote at '{0}'", badChar(end)));

    int index = 0;
    char []valueBuf = _valueBuf;
    
    while (true) {
      if (_inputLength == _inputOffset && ! fillBuffer())
	throw error(L.l("Unexpected end of file in attribute value."));

      char ch = _inputBuf[_inputOffset++];
      
      switch (ch) {
      case '&':
	throw error(L.l("Can't handle entities yet."));

      case '\r':
	addCarriageReturnLine();
	ch = ' ';
	break;

      case '\n':
	_line++;
	ch = ' ';
	break;

      case '\'': case '"':
	if (ch == end)
	  return new String(valueBuf, 0, index);
	break;
      }

      valueBuf[index++] = ch;
    }
  }

  /**
   * Skips whitespace, returning the next character.
   */
  private int skipWhitespace(int ch)
    throws IOException
  {
    while (true) {
      switch (ch) {
      case -1:
	return -1;
	
      case ' ': case '\t':
	break;

      case '\r':
	addCarriageReturnLine();
	break;

      case '\n':
	_line++;
	break;
	
      default:
	return ch;
      }
      
      if (_inputLength == _inputOffset && ! fillBuffer())
	return -1;

      ch = _inputBuf[_inputOffset++];
    }
  }

  /**
   * Reads a character.
   */
  private int read()
    throws IOException
  {
    if (_inputLength == _inputOffset && ! fillBuffer())
      return -1;
    else
      return _inputBuf[_inputOffset++];
  }

  /**
   * Reads a character.
   */
  private void unread()
    throws IOException
  {
    _inputOffset--;
  }

  /**
   * Fills the input buffer.
   */
  private boolean fillBuffer()
    throws IOException
  {
    _inputOffset = 0;
    _inputLength = _reader.read(_inputBuf, 0, _inputBuf.length);

    return _inputLength > 0;
  }

  /**
   * Returns a string for a bad char.
   */
  private String badChar(int ch)
  {
    return "" + (char) ch;
  }

  /**
   * Returns an error.
   */
  private SAXException error(String msg)
  {
    return new SAXException(msg);
  }

  class LocatorImpl implements ExtendedLocator {
    /**
     * Returns the parser's system id.
     */
    public String getSystemId()
    {
      return _systemId;
      /*
      if (_parser._reader != null && _parser._reader.getSystemId() != null)
        return _parser._reader.getSystemId();
      else if (_parser.getSystemId() != null)
        return _parser.getSystemId();
      else if (_parser._reader != null && _parser._reader.getFilename() != null)
        return _parser._reader.getFilename();
      else if (_parser.getFilename() != null)
        return _parser.getFilename();
      else
        return null;
      */
    }

    /**
     * Returns the parser's filename.
     */
    public String getFilename()
    {
      return _filename;
      /*
      if (_parser._reader != null && _parser._reader.getFilename() != null)
        return _parser._reader.getFilename();
      else if (_parser.getFilename() != null)
        return _parser.getFilename();
      else if (_parser._reader != null && _parser._reader.getSystemId() != null)
        return _parser._reader.getSystemId();
      else if (_parser.getSystemId() != null)
        return _parser.getSystemId();
      else
        return null;
      */
    }

    /**
     * Returns the public id.
     */
    public String getPublicId()
    {
      return _publicId;
      /*
      if (_parser._reader != null)
        return _parser._reader.getPublicId();
      else
        return _parser.getPublicId();
      */
    }

    /**
     * Returns the line number.
     */
    public int getLineNumber()
    {
      return _line;
      /*
      if (_parser._reader != null)
        return _parser._reader.getLine();
      else
        return _parser.getLineNumber();
      */
    }

    /**
     * Returns the column.
     */
    public int getColumnNumber()
    {
      return -1;
    }
  }
}
