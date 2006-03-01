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

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Optional;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DOMDocumentValue extends DOMNodeValue {

  private static final Logger log = Log.open(DOMDocumentValue.class);
  private static final L10N L = new L10N(DOMDocumentValue.class);

  private String _encoding;
  private BooleanValue _formatOutput;
  private DOMImplementationValue _DOMImplementationValue;
  private BooleanValue _recover;
  private BooleanValue _resolveExternals;
  private BooleanValue _substituteEntities;
  private String _version;

  private InputStream _is;
  private DocumentBuilderFactory _documentBuilderFactory;
  private Document _document;
  private DOMConfiguration _DOMConfig;

  public DOMDocumentValue()
  {
    _document = createDocument();
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
  public static Document createDocument()
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.newDocument();
    } catch (ParserConfigurationException ex) {
      log.log(Level.FINE, L.l(ex.toString()), ex);
      return null;
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

      return new DOMDocumentTypeValue(_document.getDoctype());

    else if ("_documentElementValue".equals(name))

      return new DOMElementValue(_document.getDocumentElement());

    else if ("documentURI".equals(name))

      return new StringValue(_document.getDocumentURI());

    else if ("encoding".equals(name)) //XXX: actualencoding vs. encoding vs. actualEncoding???

      return new StringValue(_document.getXmlEncoding());

    else if ("formatOutput".equals(name)) //XXX: what is formatOutput???

      return _formatOutput;

    else if ("implementation".equals(name)) {

      if (_DOMImplementationValue == null)
        _DOMImplementationValue = new DOMImplementationValue(_document.getImplementation());

      return _DOMImplementationValue;

    } else if ("preserveWhiteSpace".equals(name))

      return getPreserveWhiteSpace();

    else if ("recover".equals(name)) //XXX: what is recover???

      return _recover;

    else if ("resolveExternals".equals(name)) //XXX: what is resolveExternals

      return _resolveExternals;

    else if ("standalone".equals(name)) {

      if (_document.getXmlStandalone())
        return BooleanValue.TRUE;
      else
        return BooleanValue.FALSE;

    } else if ("strictErrorChecking".equals(name)) {

      if (_document.getStrictErrorChecking())
        return BooleanValue.TRUE;
      else
        return BooleanValue.FALSE;

    } else if ("substituteEntities".equals(name)) //XXX: what is substituteEntities

      return _substituteEntities;

    else if ("validateOnParse".equals(name))

      return getValidateOnParse();

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

  private Value getValidateOnParse()
  {
    if (_documentBuilderFactory == null)
      return BooleanValue.FALSE;

    return _documentBuilderFactory.isValidating()
           ? BooleanValue.TRUE
           : BooleanValue.FALSE;
  }

  private Value setValidateOnParse(Value value)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();

    _documentBuilderFactory.setValidating(value.toBoolean());

    return NullValue.NULL;
  }

  private Value getPreserveWhiteSpace()
  {
    if (_documentBuilderFactory == null)
      return BooleanValue.FALSE;

    //isIgnoringElementContentWhitespace is the opposite of
    //php preserveWhiteSpace
    return _documentBuilderFactory.isIgnoringElementContentWhitespace()
           ? BooleanValue.FALSE
           : BooleanValue.TRUE;

  }

  private Value setPreserveWhiteSpace(Value value)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();

    if (!value.toBoolean()) {
      _documentBuilderFactory.setValidating(true);
      _documentBuilderFactory.setIgnoringElementContentWhitespace(true);
    }

    return NullValue.NULL;
  }

  @Override
  public Value putField(Env env, String key, Value value)
  {
    if ("actualEncoding".equals(key))
      return errorReadOnly(key);
    else if ("config".equals(key))
      return errorReadOnly(key);
    else if ("doctype".equals(key))
      return errorReadOnly(key);
    else if ("documentElement".equals(key))
      return errorReadOnly(key);
    else if ("documentURI".equals(key)) {
      if (_document != null)
        _document.setDocumentURI(value.toString());
    } else if ("encoding".equals(key)) { //XXX: encoding vs. actualEncoding???
      _encoding = value.toString();
    } else if ("formatOutput".equals(key)) {
      _formatOutput = (BooleanValue) value;
    } else if ("implementation".equals(key))
      return errorReadOnly(key);
    else if ("preserveWhiteSpace".equals(key)) {
      return setPreserveWhiteSpace(value);
    } else if ("recover".equals(key)) { //XXX: recover

      if (value instanceof BooleanValue)
        _recover = (BooleanValue) value;

    } else if ("resolveExternals".equals(key)) { //XXX: resolveExternals

      if (value instanceof BooleanValue)
        _resolveExternals =  (BooleanValue) value;

    } else if ("standalone".equals(key)) {

      if (value instanceof BooleanValue) {
        if (_document != null)
          _document.setXmlStandalone(value.toBoolean());
      }

    } else if ("strictErrorChecking".equals(key)) {

      if (value instanceof BooleanValue) {
        if (_document != null)
          _document.setStrictErrorChecking(value.toBoolean());
      }
    } else if ("substituteEntities".equals(key)) {

      if (value instanceof BooleanValue)
      
        _substituteEntities = (BooleanValue) value;
      
    } else if ("validateOnParse".equals(key)) {
  
        return setValidateOnParse(value);
 
    } else if ("version".equals(key)) { //XXX: version vs. xmlVersion

      if (_document != null)
        _document.setXmlVersion(value.toString());

    } else if ("xmlEncoding".equals(key))
      return errorReadOnly(key);
    else if ("xmlStandalone".equals(key)) {

      if (_document != null)
        _document.setXmlStandalone(value.toBoolean());

    } else if ("xmlVersion".equals(key)) {

      if (_document != null)
        _document.setXmlVersion(value.toString());

    }

    return NullValue.NULL;
  }

  //Used if user trys to set a read-only property
  //in putField
  private Value errorReadOnly(String key)
  {
    return NullValue.NULL;
  }

  public Value createAttribute(Value name)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    //XXX: deal with DOMExceptions:
    //INVALID_CHARACTER_ERR
    return new DOMAttrValue(_document.createAttribute(name.toString()));
  }

  public Value createAttributeNS(Value namespaceURI,
                                 Value qualifiedName)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    //XXX: deal with DOMExpceitons:
    // INVALID_CHARACTER_ERR
    // NAMESPACE_ERR

    return new DOMAttrValue(_document.createAttributeNS(namespaceURI.toString(), qualifiedName.toString()));
  }

  public Value createCDATASection(Value data)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    //XXX: deal with DOMExceptions:
    // NOT_SUPPORTED_ERR

    return new DOMCDATASectionValue(_document.createCDATASection(data.toString()));
  }

  public Value createComment(Value data)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    return new DOMCommentValue(_document.createComment(data.toString()));
  }

 public Value createDocumentFragment()
 {
   if (_document == null)
     return NullValue.NULL;

   return new DOMDocumentFragmentValue(_document.createDocumentFragment());
 }

 public Value createElement(Value name,
                            @Optional Value value)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    //XXX: handle DOM_INVALID_CHARACTER_ERR

    DOMElementValue result = new DOMElementValue(_document.createElement(name.toString()));

    if (value != null)
      result.setNodeValue(value);

    return result;
  }

  public Value createElementNS(Value namespaceURI,
                               Value qualifiedName,
                               @Optional Value value)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    //XXX: handle INVALID_CHARACTER_ERR,
    //NAMESPACE_ERR, NOT_SUPPORTED_ERR

    DOMElementValue result = new DOMElementValue(_document.createElementNS(namespaceURI.toString(), qualifiedName.toString()));

    if (value != null)
      result.setNodeValue(value);

    return result;
  }

  public Value createEntityReference(Value name)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    //XXX: handle INVALID_CHARACTER_ERR,
    //NOT_SUPPORTED_ERR

    return new DOMEntityReferenceValue(_document.createEntityReference(name.toString()));
  }

  public Value createProcessingInstruction(Value target,
                                           @Optional Value data)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    if (data == null)
      return new DOMProcessingInstructionValue(_document.createProcessingInstruction(target.toString(), ""));
    else
      return new DOMProcessingInstructionValue(_document.createProcessingInstruction(target.toString(), data.toString()));
  }

  public Value createTextNode(Value data)
  {
    if (_document == null)
      return BooleanValue.FALSE;

    return new DOMTextValue(_document.createTextNode(data.toString()));
  }

  public Value getElementById(Value elementId)
  {
    if (_document == null)
      return NullValue.NULL;

    return new DOMElementValue(_document.getElementById(elementId.toString()));
  }

  public Value getElementsByTagName(Value tagname)
  {
    if (_document == null)
      return NullValue.NULL;

    return new DOMNodeListValue(_document.getElementsByTagName(tagname.toString()));
  }

  public Value getElementsByTagNameNS(Value namespaceURI,
                                      Value localName)
  {
    if (_document == null)
      return NullValue.NULL;

    return new DOMNodeListValue(_document.getElementsByTagNameNS(namespaceURI.toString(), localName.toString()));
  }

  public Value importNode(Value node,
                          @Optional Value deep)
  {
    boolean isDeep;

    if (!(node instanceof DOMNodeValue))
      return NullValue.NULL;
    
    if (_document == null)
      return NullValue.NULL;

    if (deep == null)
      isDeep = false;
    else
      isDeep = deep.toBoolean();

    return new DOMNodeValue(_document.importNode(((DOMNodeValue)node).getNode(), isDeep));
  }

  //XXX: need to implement static version which returns a DOMDocumentValue
  public Value load(Value filename,
                    @Optional Value options)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    try {
      DocumentBuilder builder = _documentBuilderFactory.newDocumentBuilder();
      _is = new BufferedInputStream(new FileInputStream(new File(filename.toString())));
      _document = builder.parse(_is);
      return BooleanValue.TRUE;
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return BooleanValue.FALSE;
    }
  }
  
  public Value loadXML(Value source,
                       @Optional Value options)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    try {
      DocumentBuilder builder = _documentBuilderFactory.newDocumentBuilder();
      _is = source.toInputStream();
      _document = builder.parse(_is);
      return BooleanValue.TRUE;
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return BooleanValue.FALSE;
    }
  }
  
  @Override
  public Value normalize()
  {
    if (_document != null)
      _document.normalizeDocument();

    return NullValue.NULL;
  }
  public Value relaxNGValidate(Value fileName)
  {
    try {
      InputStream is = new FileInputStream(new File(fileName.toString()));
      return validateInputStream(is, XMLConstants.RELAXNG_NS_URI);
    } catch (IOException e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return BooleanValue.FALSE;
    }
  }
  
  public Value relaxNGValidateSource(Value source)
  {
    return validateInputStream(source.toInputStream(), XMLConstants.RELAXNG_NS_URI);
  }
  
  public Value schemaValidate(Value fileName)
  {
    try {
      InputStream is = new FileInputStream(new File(fileName.toString()));
      return validateInputStream(is, XMLConstants.W3C_XML_SCHEMA_NS_URI);
    } catch (IOException e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return BooleanValue.FALSE;
    }
  }
  
  private Value validateInputStream(InputStream is,
                                    String xmlConstant)
  {
    if (_document == null)
      return BooleanValue.FALSE;
    
    SchemaFactory factory = SchemaFactory.newInstance(xmlConstant);
    
    try {
      Source schemaFile = new StreamSource(is);
      Schema schema = factory.newSchema(schemaFile);
      
      Validator validator = schema.newValidator();
      
      try {
        validator.validate(new DOMSource(_document));
        return BooleanValue.TRUE;
      } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
        return BooleanValue.FALSE;
      }
      
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return BooleanValue.FALSE;
    }
  }
  
  public Value schemaValidateSource(Value source)
  {
    return validateInputStream(source.toInputStream(), XMLConstants.W3C_XML_SCHEMA_NS_URI);
  }

  public Value validate()
  {
    if ((_document != null) && (_documentBuilderFactory != null) && (_documentBuilderFactory.isValidating()))
      return BooleanValue.TRUE;

    //Need to re-parse with a validating parser
    if (_is == null)
      return BooleanValue.FALSE;
    
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    _documentBuilderFactory.setValidating(true);
    
    try {
      DocumentBuilder builder = _documentBuilderFactory.newDocumentBuilder();
      _document = builder.parse(_is);
      return BooleanValue.TRUE;
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return BooleanValue.FALSE;
    } 
  }

  //XXX: does not yet support options (ie: LIBXML_NOEMPTYTAG)
  public Value save(Env env,
                    Value fileName,
                    @Optional Value options)
  {
    Value result = BooleanValue.FALSE;
    
    if (_document == null)
      return result;
    
    BufferedWriter bw = null;
    
    try {
      
      bw = new BufferedWriter (new FileWriter(new File(fileName.toString())));
      SimpleXMLElementValue simpleXML = new SimpleXMLElementValue(_document, _document.getDocumentElement());
      String asXML = simpleXML.asXML().toString();
      bw.write(asXML);
      result = new LongValue(asXML.length());
      
    } catch (IOException e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);  
    } finally {
      try {
        if (bw != null)
          bw.close();
      } catch (IOException e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
      }
    }
    
    return result;
  }

  /**
   * 
   * @param node
   * @param options Not yet supported LIBXML_NOEMPTYTAG
   * @return XML as string or FALSE
   */
  public Value saveXML(@Optional Value node,
                       @Optional Value options)
  {  
    if (_document == null)
      return BooleanValue.FALSE;
    
    SimpleXMLElementValue simpleXML;
  
    if ((node != null) && !(node instanceof DOMNodeValue))
      return BooleanValue.FALSE;
    
    if (node == null) {
      
      simpleXML = new SimpleXMLElementValue(_document, _document.getDocumentElement());   
      return new StringValue(simpleXML.asXML().toString());
      
    } else {
      
      simpleXML = new SimpleXMLElementValue(_document, (Element) ((DOMNodeValue) node).getNode());
      return new StringValue(simpleXML.generateXML().toString());
    }
  }
  
  public Value saveHTML()
  {
    return saveXML(null, LongValue.ZERO);
  }
  
  public Value saveHTMLFile(Env env, Value filename)
  {
    return save(env, filename, LongValue.ZERO);
  }


  @Override
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    if ("createDocumentFragment".equals(methodName))
      return createDocumentFragment();
    else if ("validate".equals(methodName))
      return validate();
    else if ("normalize".equals(methodName))
      return normalize();
    else if ("saveHTML".equals(methodName))
      return saveHTML();
    
    return super.evalMethod(env, methodName);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("createAttribute".equals(methodName))
      return createAttribute(a0);
    else if ("createCDATASection".equals(methodName))
      return createCDATASection(a0);
    else if ("createComment".equals(methodName))
      return createComment(a0);
    else if ("createEntityReference".equals(methodName))
      return createEntityReference(a0);
    else if ("createTextNode".equals(methodName))
      return createTextNode(a0);
    else if ("getElementById".equals(methodName))
      return getElementById(a0);
    else if ("getElementsByTagName".equals(methodName))
      return getElementsByTagName(a0);
    else if ("loadHTML".equals(methodName))
      return loadHTML(a0);
    else if ("loadHTMLFile".equals(methodName))
      return loadHTMLFile(a0);
    else if ("relaxNGValidate".equals(methodName))
      return relaxNGValidate(a0);
    else if ("relaxNGValidateSource".equals(methodName))
      return relaxNGValidateSource(a0);
    else if ("saveHTMLFile".equals(methodName))
      return saveHTMLFile(env, a0);
    else if ("schemaValidate".equals(methodName))
      return schemaValidate(a0);
    else if ("schemaValidateSource".equals(methodName))
      return schemaValidateSource(a0);
    else if ("xinclude".equals(methodName))
      return xinclude(a0);
    
    return super.evalMethod(env, methodName, a0);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    if ("createAttributeNS".equals(methodName))
      return createAttributeNS(a0,a1);
    else if ("createElement".equals(methodName))
      return createElement(a0, a1);
    else if ("createProcessingInstruction".equals(methodName))
      return createProcessingInstruction(a0, a1);
    else if ("getElementsByTagNameNS".equals(methodName))
      return getElementsByTagNameNS(a0, a1);
    else if ("importNode".equals(methodName))
      return importNode(a0, a1);
    else if ("load".equals(methodName))
      return load(a0, a1);
    else if ("loadXML".equals(methodName))
      return loadXML(a0, a1);
    else if ("save".equals(methodName))
      return save(env, a0, a1);
    else if ("saveXML".equals(methodName))
      return saveXML(a0, a1);
    
    return super.evalMethod(env, methodName, a0, a1);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1, Value a2)
    throws Throwable
  {
    if ("createElementNS".equals(methodName))
      return createElementNS(a0, a1, a2);
    
    return super.evalMethod(env, methodName, a0, a1, a2);
  }
  
  //@todo loadHTML
  public Value loadHTML(Value source)
  {
    throw new UnsupportedOperationException();
  }
  
  //@todo loadHTMLFile()  
  public Value loadHTMLFile(Value fileName)
  {
    throw new UnsupportedOperationException();
  }
  
  //@todo xinclude([int options])
  public Value xinclude(@Optional Value options)
  {
    throw new UnsupportedOperationException();
  }
  
  public Document getDocument()
  {
    return _document;
  }
}
