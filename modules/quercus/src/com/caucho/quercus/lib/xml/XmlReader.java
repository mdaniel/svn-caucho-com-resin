package com.caucho.quercus.lib.xml;

import com.caucho.util.L10N;
import com.caucho.xml.stream.StreamReaderImpl;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.env.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.Location;

public class XmlReader {
	private static final Logger log = Logger.getLogger(XmlReader.class.getName());
	private static final L10N L = new L10N(XmlReader.class);


	private int _attributeCount;
	private String _baseURI;
	//private int _depth;
	private boolean _hasAttributes;
	private boolean _hasValue;
	private boolean _isDefault;
	private boolean _isEmptyElement;
	private String _localName;
	private String _name;
	private String _namespaceURI;
	private int _nodeType;
	private String _prefix;
	private String _value;
	private String _xmlLang;

	private int _depth;
	private boolean _lastNodeWasStart;

	private XMLStreamReader  _streamReader;

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
		_attributeCount = 0;
		_baseURI = "";
		//_depth = 0;
		_hasAttributes = false;
		_hasValue = false;
		_isDefault = false;
		_isEmptyElement = false;
		_localName = "";
		_name = "";
		_namespaceURI = "";
		_nodeType = 0;
		_prefix = "";
		_value = "";
		_xmlLang = "";

		_depth = 0;
		_lastNodeWasStart = false;

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
		if (streamIsOpen())
			return BooleanValue.create(_hasAttributes);

		return NullValue.NULL;
	}

	/**
	 * Determines whether this element has content.
	 *
	 * @return true if this element has content, false if not, otherwise null
	 */
	public Value getHasValue() {
		if (streamIsOpen())
			return BooleanValue.create(_hasValue);

		return NullValue.NULL;
	}

	/**
	 * Determines whether this element is default.
	 *
	 * XXX: Not sure what default means, yet.
	 *
	 * @return true if this element is default, false if not, otherwise null
	 */
	public Value getIsDefault() {
		if (streamIsOpen())
			return BooleanValue.create(_isDefault);

		return NullValue.NULL;
	}

	/**
	 * Determines whether this element is empty.
	 *
	 * @return true if this element is empty, false if not, otherwise null
	 */
	public Value getIsEmptyElement() {
		if (streamIsOpen())
			return BooleanValue.create(_isEmptyElement);

		return NullValue.NULL;
	}

	/**
	 * Determines whether this element has attributes.
	 *
	 * @return true if this element has attributes, false if not, otherwise null
	 */
	public Value getLocalName() {
		if (streamIsOpen())
			return StringValue.create(_localName);

		return NullValue.NULL;
	}

	/**
	 * Returns the name of the current element.
	 *
	 * @return the name, otherwise null
	 */
	public Value getName() {
		if (streamIsOpen())
			return StringValue.create(_name);

		return NullValue.NULL;
	}

	/**
	 * Returns the namespace uniform resource locator of the current element.
	 *
	 * @return the namespace URI, otherwise null
	 */
	public Value getNamespaceURI() {
		if (streamIsOpen())
			return StringValue.create(_namespaceURI);

		return NullValue.NULL;
	}

	/**
	 * Returns the node type of the current element.
	 *
	 * @return the node type, otherwise null
	 */
	public Value getNodeType() {
		if (streamIsOpen())
			return LongValue.create(_nodeType);

		return NullValue.NULL;
	}

	/**
	 * Returns the prefix of the current element.
	 *
	 * @return the prefix, otherwise null
	 */
	public Value getPrefix() {
		if (streamIsOpen())
			return StringValue.create(_prefix);

		return NullValue.NULL;
	}

	/**
	 * Returns the value of the current element.
	 *
	 * @return the value, otherwise null
	 */
	public Value getValue() {
		if (streamIsOpen())
			return StringValue.create(_value);

		return NullValue.NULL;
	}

	/**
	 * Returns the node type of the current element.
	 *
	 * @return the node type, otherwise null
	 */
	public Value getXmlLang() {
		if (streamIsOpen())
			return StringValue.create(_xmlLang);

		return NullValue.NULL;
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
	public BooleanValue open(Env env, String uri) {
		try {
			_streamReader = new StreamReaderImpl(new BufferedInputStream(
					new FileInputStream(uri)));
		}
		catch (XMLStreamException ex) {
			log.log(Level.WARNING, ex.toString(), ex);

			env.warning(L.l("Unable to open source data"));

			return BooleanValue.FALSE;
		}
		catch (FileNotFoundException ex) {
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
		_lastNodeWasStart = _streamReader.isStartElement();
	}

	/**
	 * Updates the depth.
	 *
	 */
	private void updateDepth() {
		if (_lastNodeWasStart)
			_depth++;
		else if (_streamReader.isEndElement())
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

			_streamReader.next();

			updateDepth();

		}
		catch (XMLStreamException ex) {
			log.log(Level.WARNING, ex.toString(), ex);

			env.warning(L.l("Unable to read"));

			return BooleanValue.FALSE;
		}

		return BooleanValue.TRUE;
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
}