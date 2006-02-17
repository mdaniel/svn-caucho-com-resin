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

package com.caucho.soap.wsdl;

import java.util.logging.Logger;

import java.io.InputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.w3c.dom.Document;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.config.NodeBuilder;
import com.caucho.config.Config;

/**
 * WSDL Parser
 */
public class WSDLParser {
  private final static Logger log = Log.open(WSDLParser.class);
  private final static L10N L = new L10N(WSDLParser.class);

  /**
   * Starts a WSDL element.
   */
  public static WSDLDefinitions parse(InputStream is, String systemId)
    throws IOException, SAXException
  {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();

      Document doc = builder.parse(is, systemId);

      WSDLDefinitions wsdl = new WSDLDefinitions();
      
      new Config().configure(wsdl, doc.getDocumentElement());

      return wsdl;
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw e;
    } catch (SAXException e) {
      throw e;
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }
}
