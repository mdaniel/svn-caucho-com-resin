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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;

import com.caucho.util.L10N;
import com.caucho.util.Log;

public class DOMDocumentValue extends DOMNodeValue {

  private static final Logger log = Log.open(DOMDocumentValue.class);
  private static final L10N L = new L10N(DOMDocumentValue.class);

  private String _encoding;
  private BooleanValue _formatOutput;
  private DOMImplementationValue _DOMImplementationValue;
  private BooleanValue _recover;
  private BooleanValue _resolveExternals;
  private BooleanValue _strictErrorChecking = BooleanValue.TRUE;
  private BooleanValue _substituteEntities;
  private String _version;
  
  private Document _document;
  private DOMConfiguration _DOMConfig;

  public DOMDocumentValue()
  {
    createDocument();
  }

  public DOMDocumentValue(String version)
  {
    _version = version;
    createDocument();
    _document.setXmlVersion(version);
  }

  public DOMDocumentValue(String version,
                          String encoding)
  {
    _version = version;
    _encoding = encoding; //Used when writing XML to a file
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
  
  @Override
  public Value getField(String name)
  {
    if ("actualEncoding".equals(name))
    
      return new StringValue(_document.getXmlEncoding());
    
    else if ("config".equals(name)) {
    
      if (_DOMConfig == null)
        _DOMConfig = _document.getDomConfig();
      
      return new DOMConfigurationValue(_DOMConfig);
    
    } else if ("doctype".equals(name))
    
      return new DOMDocumentType(_document.getDoctype());
    
    else if ("_documentElementValue".equals(name))
    
      return new DOMElementValue(_document.getDocumentElement());
    
    else if ("documentURI".equals(name))
    
      return new StringValue(_document.getDocumentURI());
    
    else if ("encoding".equals(name)) //XXX: encoding vs. actualEncoding???
    
      return new StringValue(_document.getXmlEncoding());
    
    else if ("formatOutput".equals(name)) //XXX: what is formatOutput???
    
      return _formatOutput;
    
    else if ("implementation".equals(name)) {
      
      if (_DOMImplementationValue == null)
        _DOMImplementationValue = new DOMImplementationValue(_document.getImplementation());
    
      return _DOMImplementationValue;
    
    } else if ("preserveWhiteSpace".equals(name))
    
      return checkPreserveWhiteSpace();
    
    else if ("recover".equals(name)) //XXX: what is recover???
    
      return _recover;
    
    else if ("resolveExternals".equals(name)) //XXX: what is resolveExternals
    
      return _resolveExternals;
    
    else if ("standalone".equals(name)) {
    
      if (_document.getXmlStandalone())
        return BooleanValue.TRUE;
      else 
        return BooleanValue.FALSE;
    
    } else if ("strictErrorChecking".equals(name)) //XXX: throws DOMException on Errors
    
      return _strictErrorChecking;
    
    else if ("substituteEntities".equals(name))
    
      return _substituteEntities;
    
    else if ("validateOnParse".equals(name))
    
      return checkValidateOnParse();
    
    else if ("version".equals(name)) //XXX: version vs. xmlVersion
    
      return new StringValue(_document.getXmlVersion());
    
    else if ("xmlEncoding".equals(name))
    
      return new StringValue(_document.getXmlEncoding());
    
    else if ("xmlStandalone".equals(name)) {
    
      if (_document.getXmlStandalone())
        return BooleanValue.TRUE;
      else
        return BooleanValue.FALSE;
    
    } else if ("xmlVersion".equals(name))
    
     return new StringValue(_document.getXmlVersion());
    
    else
    
      return NullValue.NULL;
  }

  private Value checkValidateOnParse()
  {
    if (_DOMConfig == null)
      _DOMConfig = _document.getDomConfig();

    return _DOMConfig.getParameter("validate").equals(true)
           ? BooleanValue.TRUE
           : BooleanValue.FALSE;
  }

  private Value checkPreserveWhiteSpace()
  {
    if (_DOMConfig == null)
      _DOMConfig = _document.getDomConfig();

    return _DOMConfig.getParameter("element-content-whitespace").equals(true)
           ? BooleanValue.TRUE
           : BooleanValue.FALSE;

  }

  @Override
  public Value putField(Env env, String key, Value value)
  {
    return NullValue.NULL;
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
