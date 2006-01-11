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

package com.caucho.quercus.lib;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.StringReader;

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.caucho.quercus.module.Optional;

/**
 * XML object oriented API facade
 */
public class XmlClass {
  private static final Logger log = Logger.getLogger(XmlClass.class.getName());
  private static final L10N L = new L10N(XmlClass.class);

  public static final int XML_OPTION_CASE_FOLDING = 0x1;
  public static final int XML_OPTION_TARGET_ENCODING = 0x2;
  public static final int XML_OPTION_SKIP_TAGSTART = 0x3;
  public static final int XML_OPTION_SKIP_WHITE = 0x4;

  private Env _env;
  private String _encoding;
  private Callback _startElementHandler;
  private Callback _endElementHandler;
  private Callback _characterDataHandler;
  private Value _parser;

  private StringBuffer _xmlString = new StringBuffer();

  public XmlClass(Env env,
                  String encoding)
  {
    _env = env;
    _encoding = encoding;
    _parser = _env.wrapJava(this);
  }

  /**
   * Sets the element handler functions for the XML parser.
   *
   * @param start_element_handler must exist when xml_parse is called
   * @param end_element_handler must exist when xml_parse is called
   * @return true always even if handlers are disabled
   */

  public boolean xml_set_element_handler(Value start_element_handler,
                                         Value end_element_handler)
  {
    _startElementHandler = _env.createCallback(start_element_handler);
    _endElementHandler = _env.createCallback(end_element_handler);
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
    _characterDataHandler = _env.createCallback(handler);
    return true;
  }

  /**
   * xml_parse will keep accumulating "data" until
   * either is_final is true or omitted
   *
   * @param data
   * @param is_final
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public boolean xml_parse(String data,
                           @Optional("true") boolean is_final)
    throws IOException, SAXException, ParserConfigurationException
  {
    _xmlString.append(data);

    if (is_final) {
      InputSource is = new InputSource(new StringReader(_xmlString.toString()));

      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(is, new XmlHandler());
    }

    return true;
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
      args[1] = new StringValue(eName);

      // turn attrs into an array of name, value pairs
      args[2] = new ArrayValueImpl();
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name
        if ("".equals(aName)) aName = attrs.getQName(i);
        args[2].put(new StringValue(aName), new StringValue(attrs.getValue(i)));
      }

      try {
        _startElementHandler.eval(_env,args);
      } catch (Throwable t) {
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
        _endElementHandler.eval(_env, _parser, new StringValue(sName));
      } catch (Throwable t) {
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
        _characterDataHandler.eval(_env, _parser, new StringValue(s));
      } catch (Throwable t) {
        throw new SAXException(L.l(t.getMessage()));
      }
    }
  }
}
