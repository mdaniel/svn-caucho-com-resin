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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import java.sql.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.caucho.quercus.module.Optional;

import com.caucho.vfs.Path;
import com.caucho.vfs.FilePath;

/**
 * XML object oriented API facade
 */


public class XmlClass {
  private static final Logger log = Logger.getLogger(XmlClass.class.getName());
  private static final L10N L = new L10N(XmlClass.class);

  private Env _env;
  private String _encoding;
  private Callback _startElementHandler;
  private Callback _endElementHandler;
  private Callback _characterDataHandler;

  private XmlHandler _handler = new XmlHandler();

  public XmlClass(Env env,
                  String encoding)
  {
    _env = env;
    _encoding = encoding;
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

  public boolean xml_parse(Path path)
    throws IOException, SAXException, ParserConfigurationException
  {
    System.out.println("HERE!!!!!!!!!! " + path);
    /*FilePath path = new FilePath(fileName);
    InputSource is = new InputSource(path.openRead());

    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(is, _handler);
*/
    return true;
  }

  class XmlHandler extends DefaultHandler {

    /**
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
      Value[] args = new Value[2];
      String eName = lName; // element name
      if ("".equals(eName)) eName = qName;
      args[0] = new StringValue(eName);

      // turn attrs into an array of name, value pairs
      args[1] = new ArrayValueImpl();
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name
        if ("".equals(aName)) aName = attrs.getQName(i);
        args[1].put(new StringValue(aName), new StringValue(attrs.getValue(i)));
      }

      try {
        _startElementHandler.eval(_env,args);
      } catch (Throwable t) {
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    public void endElement(String namespaceURI,
                           String sName,
                           String qName)
      throws SAXException
    {
      try {
        _endElementHandler.eval(_env, new StringValue(sName));
      } catch (Throwable t) {
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    public void characters(char[] ch,
                           int start,
                           int length)
      throws SAXException
    {
      String s = new String(ch,start,length);

      try {
        _characterDataHandler.eval(_env, new StringValue(s));
      } catch (Throwable t) {
        throw new SAXException(L.l(t.getMessage()));
      }
    }
  }
}
