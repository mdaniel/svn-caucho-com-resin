/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.util.L10N;
import com.caucho.util.Log;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DOMDocumentValue extends DOMNodeValue {

  private static final Logger log = Log.open(DOMDocumentValue.class);
  private static final L10N L = new L10N(DOMDocumentValue.class);

  public String actualEncoding;
  public DOMConfiguration config;
  public DOMDocumentType doctype;
  public DOMElement documentElement;
  public String documentURI;
  public String encoding;
  public boolean formatOutput;
  public DOMImplementation implementation;
  public boolean preserveWhiteSpace;
  public boolean recover;
  public boolean resolveExternals;
  public boolean standalone;
  public boolean strictErrorChecking;
  public boolean substituteEntities;
  public boolean validateOnParse;
  public String version;
  public String xmlEncoding;
  public boolean xmlStandalone;
  public String xmlVersion;

  private Document _document;

  public DOMDocumentValue()
  {
    createDocument();
  }

  public DOMDocumentValue(String version)
  {
    this.version = version;
    createDocument();
    _document.setXmlVersion(version);
  }

  public DOMDocumentValue(String version,
                          String encoding)
  {
    this.version = version;
    this.encoding = encoding; //Used when writing XML to a file
    createDocument();
    _document.setXmlVersion(version);
  }

  public DOMDocumentValue(Document document)
  {
    _document = document;
  }

  //helper for constructor
  private void createDocument()
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      _document = builder.newDocument();
    } catch (ParserConfigurationException ex) {
      log.log(Level.FINE, L.l(ex.toString()), ex);
    }
  }
  
  //METHODS
  //@todo createAttribute()
  //@todo createAttributeNS()
  //@todo createCDATASection()
  //@todo createComment()
  //@todo createDocumentFragment()
  //@todo createElement()
  //@todo createElementNS()
  //@todo createEntityReference()
  //@todo createProcessingInstruction()
  //@todo createTextNode()
  //@todo getElementById()
  //@todo getElementsByTagName()
  //@todo getElementsByTagNameNS()
  //@todo importNode()
  //@todo load()
  //@todo loadHTML()
  //@todo loadHTMLFile()
  //@todo loadXML()
  //@todo normalize()
  //@todo relaxNGValidate()
  //@todo relaxNGValidateSource()
  //@todo save()
  //@todo saveHTML()
  //@todo saveHTMLFile()
  //@todo saveXML()
  //@todo schemaValidate()
  //@todo schemaValidateSource()
  //@todo validate()
  //@todo xinclude()
}
