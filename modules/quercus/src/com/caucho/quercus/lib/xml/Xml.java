/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.xml;

import com.caucho.quercus.env.*;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.util.L10N;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML object oriented API facade
 */
public class Xml {
  private static final Logger log = Logger.getLogger(Xml.class.getName());
  private static final L10N L = new L10N(Xml.class);

  /**
   * XML_OPTION_CASE_FOLDING is enabled by default
   *
   * only affects startElement (including attribute
   * names) and endElement handlers.
   */
  private boolean _xmlOptionCaseFolding = true;

  private String _xmlOptionTargetEncoding;

  /**
   *  XML_OPTION_SKIP_TAGSTART specifies how many chars
   *  should be skipped in the beginning of a tag name (default = 0)
   *
   *  XXX: Not yet implemented
   */
  private long _xmlOptionSkipTagstart = 0;

  /**
   *  XXX: _xmlOptionSkipWhite not yet implemented
   */
  private boolean _xmlOptionSkipWhite = false;

  private Env _env;

  /** XXX: _separator is set by xml_parse_create_ns but
   *  not yet used.  Default value is ":"
   *  Possibly should report error if user wants to use
   *  anything other than ":"
   */
  private String _separator;

  private int _errorCode = XmlModule.XML_ERROR_NONE;
  private String _errorString;

  private Callback _startElementHandler;
  private Callback _endElementHandler;
  private Callback _characterDataHandler;
  private Callback _processingInstructionHandler;
  private Callback _defaultHandler;
  private Callback _startNamespaceDeclHandler;
  private Callback _endNamespaceDeclHandler;
  private Callback _notationDeclHandler;
  private Callback _unparsedEntityDeclHandler;

  private Value _parser;
  private Value _obj;

  SAXParserFactory _factory = SAXParserFactory.newInstance();

  private StringBuilder _xmlString = new StringBuilder();

  public Xml(Env env,
             String outputEncoding,
             String separator)
  {
    _env = env;
    _xmlOptionTargetEncoding = outputEncoding;
    _parser = _env.wrapJava(this);
    _separator = separator;
  }

  public int getErrorCode()
  {
    return _errorCode;
  }

  public String getErrorString()
  {
    return _errorString;
  }

  /**
   * Sets the element handler functions for the XML parser.
   *
   * @param startElementHandler must exist when xml_parse is called
   * @param endElementHandler must exist when xml_parse is called
   * @return true always even if handlers are disabled
   */

  public boolean xml_set_element_handler(Value startElementHandler,
                                         Value endElementHandler)
  {
    if (_obj == null) {
      _startElementHandler = _env.createCallback(startElementHandler);
      _endElementHandler = _env.createCallback(endElementHandler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(startElementHandler);
      _startElementHandler = _env.createCallback(value);

      value = new ArrayValueImpl();
      value.put(_obj);
      value.put(endElementHandler);
      _endElementHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * Sets the character data handler function.
   *
   * @param handler can be empty string or FALSE
   * @return true always even if handler is disabled
   */
  public boolean xml_set_character_data_handler(Value handler)
  {
    if (_obj == null) {
      _characterDataHandler = _env.createCallback(handler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _characterDataHandler = _env.createCallback(value);
    }

    return true;
  }

  /**
   * The php documentation is very vague as to the purpose
   * of the default handler.
   *
   * We are interpreting it as an alternative to the character
   * data handler.
   *
   * If character handler is defined, then use that.  Otherwise,
   * use default handler, if it is defined.
   *
   * XXX: Need to confirm that this is appropriate
   *
   * @param handler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_default_handler(Value handler)
  {
    if (_obj == null) {
      _defaultHandler = _env.createCallback(handler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _defaultHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * Sets the processing instruction handler function
   *
   * @param processingInstructionHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_processing_instruction_handler(Value processingInstructionHandler)
  {
    if (_obj == null) {
      _processingInstructionHandler = _env.createCallback(processingInstructionHandler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(processingInstructionHandler);
      _processingInstructionHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * Sets the startPrefixMapping handler
   *
   * @param startNamespaceDeclHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_start_namespace_decl_handler(Value startNamespaceDeclHandler)
  {
    if (_obj == null) {
      _startNamespaceDeclHandler = _env.createCallback(startNamespaceDeclHandler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(startNamespaceDeclHandler);
      _startNamespaceDeclHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * Sets the unparsedEntityDecl handler
   *
   * @param handler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_unparsed_entity_decl_handler(Value handler)
  {
    if (_obj == null) {
      _unparsedEntityDeclHandler = _env.createCallback(handler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _unparsedEntityDeclHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * Sets the endPrefixMapping handler
   *
   * @param endNamespaceDeclHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_end_namespace_decl_handler(Value endNamespaceDeclHandler)
  {
    if (_obj == null) {
      _endNamespaceDeclHandler = _env.createCallback(endNamespaceDeclHandler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(endNamespaceDeclHandler);
      _endNamespaceDeclHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * Sets the notationDecl handler
   *
   * @param handler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_notation_decl_handler(Value handler)
  {
    if (_obj == null) {
      _notationDeclHandler = _env.createCallback(handler);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _notationDeclHandler = _env.createCallback(value);
    }
    return true;
  }

  /**
   * sets the object which houses all the callback functions
   *
   * @param obj
   * @return returns true unless obj == null
   */
  public boolean xml_set_object(Value obj)
  {
    if (obj == null)
      return false;

    _obj = obj;

    return true;
  }

  /**
   * xml_parse will keep accumulating "data" until
   * either is_final is true or omitted
   *
   * @param data
   * @param isFinal
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public boolean xml_parse(Env env, String data,
                           @Optional("true") boolean isFinal)
    throws Exception
  {
    _xmlString.append(data);

    if (isFinal) {
      InputSource is = new InputSource(new StringReader(_xmlString.toString()));
      if (_xmlOptionTargetEncoding == null)
        _xmlOptionTargetEncoding = is.getEncoding();
      
      try {
	_errorCode = XmlModule.XML_ERROR_NONE;
	_errorString = null;
	
        SAXParser saxParser = _factory.newSAXParser();
        saxParser.parse(is, new XmlHandler());
      } catch (Exception ex) {
	ArrayValue array = new ArrayValueImpl();
	_errorCode = XmlModule.XML_ERROR_SYNTAX;
	
        throw ex;
      }
    }

    return true;
  }

  /**
   * Parses data into 2 parallel array structures.
   *
   * @param data
   * @param valsV
   * @param indexV
   * @return 0 for failure, 1 for success
   */
  public int xml_parse_into_struct(String data,
                                   @Reference Value valsV,
                                   @Optional @Reference Value indexV)
    throws Exception
  {
    _xmlString.append(data);

    InputSource is = new InputSource(new StringReader(_xmlString.toString()));

    ArrayValueImpl valueArray = new ArrayValueImpl();
    ArrayValueImpl indexArray = new ArrayValueImpl();

    try {
      SAXParser saxParser = _factory.newSAXParser();
      saxParser.parse(is, new StructHandler(valueArray, indexArray));
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      throw new Exception(L.l(ex.getMessage()));
    }

    valsV.set(valueArray);
    indexV.set(indexArray);

    return 1;
  }

  /**
   *  sets one of the following:
   *  _xmlOptionCaseFolding (ENABLED / DISABLED)
   *  _xmlOptionTargetEncoding (String)
   *  _xmlOptionSkipTagstart (int)
   *  _xmlOptionSkipWhite (ENABLED / DISABLED)
   *
   * XXX: currently only _xmlOptionCaseFolding actually does something
   *
   * @param option
   * @param value
   * @return true unless value could not be set
   */
  public boolean xml_parser_set_option(int option,
                                       Value value)
  {
    switch(option) {
      case XmlModule.XML_OPTION_CASE_FOLDING:
        if (value instanceof BooleanValue) {
          _xmlOptionCaseFolding = value.toBoolean();
          return true;
        } else {
          return false;
        }
      case XmlModule.XML_OPTION_SKIP_TAGSTART:
        if (value instanceof LongValue) {
          _xmlOptionSkipTagstart = value.toLong();
          return true;
        } else {
          return false;
        }
      case XmlModule.XML_OPTION_SKIP_WHITE:
        if (value instanceof BooleanValue) {
          _xmlOptionSkipWhite = value.toBoolean();
          return true;
        } else {
          return false;
        }
      case XmlModule.XML_OPTION_TARGET_ENCODING:
        if (value instanceof StringValue) {
          _xmlOptionTargetEncoding = value.toString();
          return true;
        } else {
          return false;
        }
      default:
        return false;
    }
  }

  /**
   *
   * @param option
   * @return relevant value
   */
  public Value xml_parser_get_option(int option)
  {
    switch (option) {
      case XmlModule.XML_OPTION_CASE_FOLDING:
        return (_xmlOptionCaseFolding ? BooleanValue.TRUE : BooleanValue.FALSE);
      case XmlModule.XML_OPTION_SKIP_TAGSTART:
        return new LongValue(_xmlOptionSkipTagstart);
      case XmlModule.XML_OPTION_SKIP_WHITE:
        return (_xmlOptionSkipWhite ? BooleanValue.TRUE : BooleanValue.FALSE);
      case XmlModule.XML_OPTION_TARGET_ENCODING:
        return new StringValueImpl(_xmlOptionTargetEncoding);
      default:
        return BooleanValue.FALSE;
    }
  }

  public String toString()
  {
    return "Xml[]";
  }

  /**
   * handler solely for xml_parse_into_struct
   */
  class StructHandler extends DefaultHandler {
    private ArrayValueImpl _valueArray;
    private ArrayValueImpl _indexArray;

    //Keeps track of depth within tree;
    //startElement increments, endElement decrements
    private int _level = 1;

    private HashMap<Integer, String> _paramHashMap = new HashMap<Integer, String> ();
    private HashMap<StringValue, ArrayValueImpl> _indexArrayHashMap = new HashMap<StringValue, ArrayValueImpl>();
    private ArrayList<StringValue> _indexArrayKeys = new ArrayList<StringValue>();

    // Used to determine whether a given element has sub elements
    private boolean _isComplete = true;
    private boolean _isOutside = true;

    private int _valueArrayIndex = 0;

    public StructHandler(ArrayValueImpl valueArray,
                         ArrayValueImpl indexArray)
    {
      _valueArray = valueArray;
      _indexArray = indexArray;
    }

    /**
     * helper function to create an array of attributes for a tag
     * @param attrs
     * @return array of attributes
     */
    private ArrayValueImpl createAttributeArray(Attributes attrs)
    {
      ArrayValueImpl result = new ArrayValueImpl();

      // turn attrs into an array of name, value pairs
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name
        if ("".equals(aName)) aName = attrs.getQName(i);
        if (_xmlOptionCaseFolding) aName = aName.toUpperCase();
        result.put(new StringValueImpl(aName), new StringValueImpl(attrs.getValue(i)));
      }

      return result;
    }

    public void endDocument()
      throws SAXException
    {
      for(StringValue sv : _indexArrayKeys) {
        _indexArray.put(sv, _indexArrayHashMap.get(sv));
      }
    }

    public void startElement(String namespaceURI,
                             String lName,
                             String qName,
                             Attributes attrs)
      throws SAXException
    {
      Value elementArray = new ArrayValueImpl();

      String eName = lName; // element name
      if ("".equals(eName)) eName = qName;
      if (_xmlOptionCaseFolding) eName = eName.toUpperCase();

      elementArray.put(new StringValueImpl("tag"), new StringValueImpl(eName));
      elementArray.put(new StringValueImpl("type"), new StringValueImpl("open"));
      elementArray.put(new StringValueImpl("level"), new DoubleValue((double) _level));
      _paramHashMap.put(_level, eName);

      if (attrs.getLength() > 0) {
        elementArray.put(new StringValueImpl("attributes"), createAttributeArray(attrs));
      }

      _valueArray.put(new DoubleValue((double)_valueArrayIndex), elementArray);

      addToIndexArrayHashMap(eName);

      _valueArrayIndex++;
      _level++;
      _isComplete = true;
      _isOutside = false;
    }

    public void endElement(String namespaceURI,
                           String sName,
                           String qName)
      throws SAXException
    {
      Value elementArray;

      _level--;

      if (_isComplete) {
        elementArray = _valueArray.get(new DoubleValue((double) _valueArrayIndex - 1));
        elementArray.put(new StringValueImpl("type"), new StringValueImpl("complete"));
      } else {
        elementArray = new ArrayValueImpl();
        String eName = sName; // element name
        if ("".equals(sName)) eName = qName;
        if (_xmlOptionCaseFolding) eName = eName.toUpperCase();
        elementArray.put(new StringValueImpl("tag"), new StringValueImpl(eName));
        elementArray.put(new StringValueImpl("type"), new StringValueImpl("close"));
        elementArray.put(new StringValueImpl("level"), new DoubleValue((double) _level));
        _valueArray.put(new DoubleValue((double)_valueArrayIndex), elementArray);

        addToIndexArrayHashMap(eName);
        _valueArrayIndex++;
      }

      _isComplete = false;
      _isOutside = true;
    }

    private void addToIndexArrayHashMap(String eName)
    {
      StringValue key = new StringValueImpl(eName);
      ArrayValueImpl indexArray = _indexArrayHashMap.get(key);

      if (indexArray == null) {
        indexArray = new ArrayValueImpl();
        _indexArrayKeys.add(key);
      }

      indexArray.put(new DoubleValue((double) _valueArrayIndex));
      _indexArrayHashMap.put(key, indexArray);
    }

    public void characters(char[] ch,
                           int start,
                           int length)
      throws SAXException
    {
      String s = new String(ch, start, length);

      if (_isOutside) {
        Value elementArray = new ArrayValueImpl();
        elementArray.put(new StringValueImpl("tag"), new StringValueImpl(_paramHashMap.get(_level - 1)));
        elementArray.put(new StringValueImpl("value"), new StringValueImpl(s));
        elementArray.put(new StringValueImpl("type"), new StringValueImpl("cdata"));
        elementArray.put(new StringValueImpl("level"), new DoubleValue((double) _level - 1));
        _valueArray.put(new DoubleValue((double)_valueArrayIndex), elementArray);

        Value indexArray = _indexArray.get(new StringValueImpl(_paramHashMap.get(_level - 1)));
        indexArray.put(new DoubleValue((double) _valueArrayIndex));

        _valueArrayIndex++;
      } else {
        Value elementArray = _valueArray.get(new DoubleValue((double) _valueArrayIndex - 1));
        elementArray.put(new StringValueImpl("value"), new StringValueImpl(s));
      }
    }
  }

  class XmlHandler extends DefaultHandler {

    /**
     * wrapper for _startElementHandler.  creates Value[] args
     *
     * @param namespaceURI
     * @param lName
     * @param qName
     * @param attrs
     * @throws SAXException
     */
    public void startElement(String namespaceURI,
                             String lName,
                             String qName,
                             Attributes attrs)
      throws SAXException
    {
      /**
       *  args[0] reference to this parser
       *  args[1] name of element
       *  args[2] array of attributes
       *
       *  Typical call in PHP looks like:
       *
       *  function startElement($parser, $name, $attrs) {...}
       */
      Value[] args = new Value[3];

      args[0] = _parser;

      String eName = lName; // element name
      if ("".equals(eName)) eName = qName;
      if (_xmlOptionCaseFolding) eName = eName.toUpperCase();
      args[1] = new StringValueImpl(eName);

      // turn attrs into an array of name, value pairs
      args[2] = new ArrayValueImpl();
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name
        if ("".equals(aName)) aName = attrs.getQName(i);
        if (_xmlOptionCaseFolding) aName = aName.toUpperCase();
        args[2].put(new StringValueImpl(aName), new StringValueImpl(attrs.getValue(i)));
      }

      try {
        if (_startElementHandler != null)
          _startElementHandler.call(_env,args);
        else
          throw new Throwable("start element handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _endElementHandler
     *
     * @param namespaceURI
     * @param sName
     * @param qName
     * @throws SAXException
     */
    public void endElement(String namespaceURI,
                           String sName,
                           String qName)
      throws SAXException
    {
      try {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName;
        if (_xmlOptionCaseFolding) eName = eName.toUpperCase();

        if (_endElementHandler != null)
          _endElementHandler.call(_env, _parser, new StringValueImpl(eName));
        else
          throw new Throwable("end element handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _characterDataHandler
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    public void characters(char[] ch,
                           int start,
                           int length)
      throws SAXException
    {
      String s = new String(ch,start,length);

      try {
        if (_characterDataHandler != null)
          _characterDataHandler.call(_env, _parser, new StringValueImpl(s));
        else if (_defaultHandler != null)
          _defaultHandler.call(_env, _parser, new StringValueImpl(s));
        else
          throw new Throwable("neither character data handler nor default handler is set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _processingInstructionHandler
     * @param target
     * @param data
     * @throws SAXException
     */
    public void processingInstruction(String target,
                                      String data)
      throws SAXException
    {
      try {
        if (_processingInstructionHandler != null)
          _processingInstructionHandler.call(_env, _parser, new StringValueImpl(target), new StringValueImpl(data));
        else
          throw new Throwable("processing instruction handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _startNamespaceDeclHandler
     * @param prefix
     * @param uri
     * @throws SAXException
     */
    public void startPrefixMapping (String prefix,
                                    String uri)
      throws SAXException
    {
      try {
        if (_startNamespaceDeclHandler != null)
          _startNamespaceDeclHandler.call(_env, new StringValueImpl(prefix), new StringValueImpl(uri));
        else
          throw new Throwable("start namespace decl handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _endNamespaceDeclHandler
     *
     * @param prefix
     * @throws SAXException
     */
    public void endPrefixMapping(String prefix)
      throws SAXException
    {
      try {
        if (_endNamespaceDeclHandler != null)
          _endNamespaceDeclHandler.call(_env, new StringValueImpl(prefix));
        else
          throw new Throwable("end namespace decl handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    public void notationDecl(String name,
                             String publicId,
                             String systemId)
      throws SAXException
    {
      try {
        if (_notationDeclHandler != null)
          _notationDeclHandler.call(_env,
                                    _parser,
                                    new StringValueImpl(name),
                                    new StringValueImpl(""),
                                    new StringValueImpl(systemId),
                                    new StringValueImpl(publicId));
        else
          throw new Throwable("notation declaration handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    @Override
    public void unparsedEntityDecl(String name,
                                   String publicId,
                                   String systemId,
                                   String notationName)
      throws SAXException
    {
      /**
       * args[0] reference to this parser
       * args[1] name
       * args[2] base (always "")
       * args[3] systemId
       * args[4] publicId
       * args[5] notationName
       */
      Value[] args = new Value[6];

      args[0] = _parser;
      args[1] = new StringValueImpl(name);
      args[2] = new StringValueImpl("");
      args[3] = new StringValueImpl(systemId);
      args[4] = new StringValueImpl(publicId);
      args[5] = new StringValueImpl(notationName);

      try {
        if (_unparsedEntityDeclHandler != null)
          _unparsedEntityDeclHandler.call(_env, args);
        else
          throw new Exception("unparsed entity declaration handler is not set");
      } catch (Throwable t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }
  }
}
