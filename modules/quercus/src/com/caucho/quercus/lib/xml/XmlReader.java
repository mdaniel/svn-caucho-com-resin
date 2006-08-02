package com.caucho.quercus.lib.xml;

import com.caucho.util.L10N;
import com.caucho.xml.stream.StreamReaderImpl;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.env.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.PathWrapper;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.Location;
import javax.xml.namespace.QName;

public class XmlReader {
  private static final Logger log = Logger.getLogger(XmlReader.class.getName());
  private static final L10N L = new L10N(XmlReader.class);

  private int _depth;
  private int _lastNodeType;

  private int _nextType;

  private XMLStreamReader  _streamReader;

  private static final HashMap<Integer, Integer> _constConvertMap =
  	new HashMap<Integer, Integer>();

  public static final int NONE = 0;
  public static final int ELEMENT = 1;
  public static final int ATTRIBUTE = 2;
  public static final int TEXT = 3;
  public static final int CDATA = 4;
  public static final int ENTITY_REF = 5;
  public static final int ENTITY = 6;
  public static final int PI = 7;
  public static final int COMMENT = 8;
  public static final int DOC = 9;
  public static final int DOC_TYPE = 10;
  public static final int DOC_FRAGMENT = 11;
  public static final int NOTATION = 12;
  public static final int WHITESPACE = 13;
  public static final int SIGNIFICANT_WHITESPACE = 14;
  public static final int END_ELEMENT = 15;
  public static final int END_ENTITY = 16;
  public static final int XML_DECLARATION = 17;

  public static final int LOADDTD = 1;
  public static final int DEFAULTATTRS = 2;
  public static final int VALIDATE = 3;
  public static final int SUBST_ENTITIES = 4;

  /**
   * Default constructor.
   *
   * XXX: Not completely sure what the passed in string(s) does.
   *
   * @param string not used
   */
  public XmlReader(@Optional String[] string) {
    _depth = 0;
    _lastNodeType = -1;
    _nextType = XMLStreamConstants.START_DOCUMENT;

    _streamReader = null;
  }

  /**
   * Determines if the stream has been opened and produces a warning if not.
   *
   * @param env
   * @param operation name of the operation being performed (i.e. read, etc.)
   * @return true if the stream is open, false otherwise
   */
  private boolean streamIsOpen(Env env, @ReadOnly String operation) {
    if (! streamIsOpen()) {
      env.warning(L.l("Load Data before trying to " + operation));

      return false;
   }

    return true;
   }

  /**
   * Determines if the stream has been opened.
   *
   * @return true if the stream is open, false otherwise
   */
  private boolean streamIsOpen() {
     return _streamReader != null;
   }

  /**
   * Returns the number of attributes of the current element.
   *
   * @return the count if it exists, otherwise null
   */
  public Value getAttributeCount()  {
    if (! streamIsOpen())
      return NullValue.NULL;

    try {
      return LongValue.create(_streamReader.getAttributeCount());
    }
    catch (IllegalStateException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return NullValue.NULL;
    }
  }

  /**
   * Returns the base uniform resource locator of the current element.
   *
   * @return the URI, otherwise null
   */
  public Value getBaseURI() {
    if (! streamIsOpen())
      return NullValue.NULL;

    // XXX: Not sure how to do this one.  StreamReaderImpl.getLocation() is
    // unsupported and Location.getLocationURI() doesn't exist either though
    // it's in the web documentation.

    // return StringValue.create(_streamReader.getLocation().getLocationURI());

    return NullValue.NULL;
  }

  /**
   * Returns the depth of the current element.
   *
   * @return the depth if it exists, otherwise null
   */
  public Value getDepth() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return LongValue.create(_depth);
  }

  /**
   * Determines whether this element has attributes.
   *
   * @return true if this element has attributes, false if not, otherwise null
   */
  public Value getHasAttributes() {
    if (! streamIsOpen())
      return NullValue.NULL;

    try {
      return BooleanValue.create(_streamReader.getAttributeCount() > 0);
    }
    catch (IllegalStateException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return NullValue.NULL;
    }
  }

  /**
   * Determines whether this element has content.
   *
   * @return true if this element has content, false if not, otherwise null
   */
  public Value getHasValue() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return BooleanValue.create(_streamReader.hasText());
  }

  /**
   * Determines whether this element is default.
   *
   * @return true if this element is default, false if not, otherwise null
   */
  public Value getIsDefault() {
    if (! streamIsOpen())
      return NullValue.NULL;

    // XXX:  StreamReaderImpl.isAttributeSpecified() only checks for
    // attribute existence
    return BooleanValue.create(_streamReader.isAttributeSpecified(0));
  }

  /**
   * Determines whether this element is empty.
   *
   * @return true if this element is empty, false if not, otherwise null
   */
  public Value getIsEmptyElement() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return BooleanValue.create(! _streamReader.hasText());
  }

  /**
   * Determines whether this element has attributes.
   *
   * @return true if this element has attributes, false if not, otherwise null
   */
  public Value getLocalName() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getLocalName());
  }

  /**
   * Returns the name of the current element.
   *
   * @return the name, otherwise null
   */
  public Value getName(Env env) {
    if (! streamIsOpen())
      return NullValue.NULL;

    try {
      return StringValue.create(_streamReader.getName().toString());
    }
    catch (IllegalStateException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return NullValue.NULL;
    }
  }

  /**
   * Returns the namespace uniform resource locator of the current element.
   *
   * @return the namespace URI, otherwise null
   */
  public Value getNamespaceURI() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getNamespaceURI());
  }

  /**
   * Returns the node type of the current element.
   *
   * @return the node type, otherwise null
   */
  public Value getNodeType() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return getConvertedNextType();
  }

  /**
   * Returns the prefix of the current element.
   *
   * @return the prefix, otherwise null
   */
  public Value getPrefix() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getPrefix());
  }

  /**
   * Returns the value of the current element.
   *
   * @return the value, otherwise null
   */
  public Value getValue() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getText());
  }

  /**
   * Returns the node type of the current element.
   *
   * @return the node type, otherwise null
   */
  public Value getXmlLang() {
    if (! streamIsOpen())
      return NullValue.NULL;

    // XXX: Not sure if this is the proper implementation or not.
    return StringValue.create(_streamReader.getEncoding());
  }

  /**
   * Closes the reader.
   *
   * @return true if success, false otherwise
   */
  public BooleanValue close() {
    if (! streamIsOpen())
      return BooleanValue.TRUE;

    try {
      _streamReader.close();
    }
    catch (XMLStreamException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return BooleanValue.FALSE;
    }

    return BooleanValue.TRUE;
  }

  /**
   *
   * @return
   */
  public Value expand() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param name
   * @return
   */
  public StringValue getAttribute(String name) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param index
   * @return
   */
  public StringValue getAttributeNo(int index) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param localName
   * @param namespaceURI
   * @return
   */
  public StringValue getAttributeNS(String localName, String namespaceURI) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param property
   * @return
   */
  public BooleanValue getParserProperty(int property) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue isValid() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param prefix
   * @return
   */
  public BooleanValue lookupNamespace(String prefix) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param name
   * @return
   */
  public BooleanValue moveToAttribute(String name) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param index
   * @return
   */
  public BooleanValue moveToAttributeNo(int index) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param localName
   * @param namespaceURI
   * @return
   */
  public BooleanValue moveToAttributeNs(String localName, String namespaceURI) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue moveToElement() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue moveToFirstAttribute() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue moveToNextAttribute() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param localname
   * @return
   */
  public BooleanValue next(@Optional String localname) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Opens a stream using the uniform resource locator.
   *
   * @param uri uniform resource locator to open
   * @return true if success, false otherwise
   */
  public BooleanValue open(Env env, Path path) {
    try {
      _streamReader = new StreamReaderImpl(path.openRead());
    }
    catch (XMLStreamException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      env.warning(L.l("Unable to open source data"));

      return BooleanValue.FALSE;
    }
    catch (IOException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      env.warning(L.l("Unable to open source data"));

      return BooleanValue.FALSE;
    }

    return BooleanValue.TRUE;
  }

  /**
   * Records the last node type.
   *
   */
  private void updateLastNode() {
    _lastNodeType = _nextType;
  }

  /**
   * Updates the depth.
   *
   */
  private void updateDepth(Env env) {
    if (_lastNodeType != XMLStreamConstants.START_DOCUMENT &&
    		_streamReader.isStartElement())
      _depth++;
    else if (_lastNodeType != XMLStreamConstants.START_ELEMENT &&
    		_streamReader.isEndElement())
      _depth--;
  }

  /**
   * Moves the cursor to the next node.
   *
   * @return true if success, false otherwise
   */
  public BooleanValue read(Env env) {
    if (! streamIsOpen(env, "read"))
      return BooleanValue.FALSE;

    try {
      if (! _streamReader.hasNext())
        return BooleanValue.FALSE;


      updateLastNode();

      _nextType = _streamReader.next();

      if (_nextType == XMLStreamConstants.END_DOCUMENT)
      	return BooleanValue.FALSE;

      updateDepth(env);

    }
    catch (XMLStreamException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      env.warning(L.l("Unable to read"));

      return BooleanValue.FALSE;
    }

    return BooleanValue.TRUE;
  }

  public LongValue getNextType() {
    return LongValue.create(_nextType);
  }

  public LongValue getConvertedNextType() {
  	Integer convertedInteger = _constConvertMap.get(_nextType);

  	int convertedInt = convertedInteger.intValue();

  	return LongValue.create(convertedInt);
  }

  /**
   *
   * @param property
   * @param value
   * @return
   */
  public BooleanValue setParserProperty(int property, boolean value) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param filename
   * @return
   */
  public BooleanValue setRelaxNGSchema(String filename) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param source
   * @return
   */
  public BooleanValue setRelaxNGSchemaSource(String source) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param source
   * @return
   */
  public BooleanValue XML(String source) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  static {
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.ATTRIBUTE),
  			new Integer(ATTRIBUTE));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.CDATA),
  			new Integer(CDATA));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.CHARACTERS),
  			new Integer(TEXT));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.COMMENT),
  			new Integer(COMMENT));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.END_ELEMENT),
  			new Integer(END_ELEMENT));
        /*
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.END_ENTITY),
  			new Integer(END_ENTITY));
        */
  	// XXX: XMLStreamConstants.ENTITY_DECLARATION is 17 in the Sun Docs
  	// but is 15 in the Resin implementation.
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.ENTITY_DECLARATION),
  			new Integer(ENTITY)); // ENTITY used twice
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.ENTITY_REFERENCE),
  			new Integer(ENTITY_REF));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.NOTATION_DECLARATION),
  			new Integer(NOTATION));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.PROCESSING_INSTRUCTION),
  			new Integer(PI));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.SPACE),
  			new Integer(WHITESPACE));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.START_ELEMENT),
  			new Integer(ELEMENT));
        /*
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.START_ENTITY),
  			new Integer(ENTITY));
        */
  	// Following constants did not match
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.DTD),
  			new Integer(NONE));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.END_DOCUMENT),
  			new Integer(NONE));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.NAMESPACE),
  			new Integer(NONE));
  	_constConvertMap.put(
  			new Integer(XMLStreamConstants.START_DOCUMENT),
  			new Integer(NONE));
  	_constConvertMap.put(
  			new Integer(0),
  			new Integer(NONE)); // Pre-Read
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(NONE));
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(DOC));
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(DOC_TYPE));
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(DOC_FRAGMENT));
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(DOC_TYPE));
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(SIGNIFICANT_WHITESPACE));
  	_constConvertMap.put(
  			new Integer(-1),
  			new Integer(XML_DECLARATION));
  }
}
