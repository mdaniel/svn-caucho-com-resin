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
import com.caucho.quercus.env.StringInputStream;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Optional;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

public class DOMDocument extends DOMNode {

  private static final Logger log = Log.open(DOMDocument.class);
  private static final L10N L = new L10N(DOMDocument.class);

  private String _encoding;
  private boolean _formatOutput;
  private DOMImplementationClass _domImplementation;
  private boolean _recover;
  private boolean _resolveExternals;
  private boolean _substituteEntities;

  private InputStream _is;
  private DocumentBuilderFactory _documentBuilderFactory;
  
  private Env _env;
  private Document _document;

  public DOMDocument(Env env)
  {
    this(env,"","");
  }

  public DOMDocument(Env env,
                     String version)
  {
    this(env,version,"");
  }

  public DOMDocument(Env env,
                     String version,
                     String encoding)
  {
    _env = env;
    _document = createDocument();
    //_document.setXmlVersion(version);
    _encoding = encoding; //Used when writing XML to a file
  }

  public DOMDocument(Env env,
                     Document document)
  {
    _env = env;
    _document = document;
  }

  public Document getNode()
  {
    return _document;
  }
  
  public Env getEnv()
  {
    return _env;
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

  public String getActualEncoding()
  {
    return _document.getXmlEncoding();
  }
  
  public DOMConfiguration getConfig()
  {
    return _document.getDomConfig();
  }
  
  public DOMNode getDoctype()
  {
    return DOMNodeFactory.createDOMNode(_env, _document.getDoctype());
  }
  
  public DOMNode getDocumentElement()
  {
    return DOMNodeFactory.createDOMNode(_env, _document.getDocumentElement());
  }
  
  public String getDocumentURI()
  {
    return _document.getDocumentURI();
  }
  
  public void setDocumentURI(String documentURI)
  {
    _document.setDocumentURI(documentURI);
  }
  
  // XXX: encoding vs. xmlEncoding ???
  public String getEncoding()
  {
    return _encoding;
  }
  
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }
  
  // XXX: figure out what formatOutput does
  public boolean getFormatOutput()
  {
    return _formatOutput;
  }
  
  public void setFormatOutput(boolean value)
  {
    _formatOutput = value;
  }
  
  public DOMImplementationClass getImplementation()
  {
    if (_domImplementation == null)
      _domImplementation = new DOMImplementationClass(_env, _document.getImplementation());
    
    return _domImplementation;
  }

  /**
   *  java isIgnoringElementContentWhitespace is the opposite of
   *  php preserveWhiteSpace
   */
  public boolean getPreserveWhiteSpace()
  {
    //isIgnoringElementContentWhitespace is the opposite of
    //php preserveWhiteSpace
    if (_documentBuilderFactory == null)
      return false;
    
    return !_documentBuilderFactory.isIgnoringElementContentWhitespace();
  }
  
  public void setPreserveWhiteSpace(boolean value)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    if (!value) {
      _documentBuilderFactory.setValidating(true);
      _documentBuilderFactory.setIgnoringElementContentWhitespace(true);
    }
  }
  
  // XXX: what is recover ???
  public boolean getRecover()
  {
    return _recover;
  }
  
  public void setRecover(boolean value)
  {
    _recover = value;
  }
  
  // XXX: implement resolveExternals properly
  public boolean getResolveExternals()
  {
    return _resolveExternals;
  }
  
  public void setResolveExternals(boolean value)
  {
    _resolveExternals = value;
  }
  
  public boolean getStandalone()
  {
    return _document.getXmlStandalone();
  }
  
  public void setStandalone(boolean xmlStandalone)
  {
    _document.setXmlStandalone(xmlStandalone);
  }
  
  public boolean getStrictErrorChecking()
  {
    return _document.getStrictErrorChecking();
  }
  
  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    _document.setStrictErrorChecking(strictErrorChecking);
  }
  
  // XXX: what is substituteEntities ???
  public boolean getSubstituteEntities()
  {
    return _substituteEntities;
  }
  
  public void setSubstituteEntities(boolean value)
  {
    _substituteEntities = value;
  }
  
  public boolean getValidateOnParse()
  {
    if (_documentBuilderFactory == null)
      return false;
    
    return _documentBuilderFactory.isValidating();
  }
  
  public void setValidateOnParse(boolean validating)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    _documentBuilderFactory.setValidating(validating);
  }
  
  public String getVersion()
  {
    return _document.getXmlVersion();
  }
  
  public void setVersion(String xmlVersion)
  {
    _document.setXmlVersion(xmlVersion);
  }
  
  public String getXmlEncoding()
  {
    return _document.getXmlEncoding();
  }
  
  public boolean getXmlStandalone()
  {
    return _document.getXmlStandalone();
  }
  
  public void setXmlStandalone(boolean xmlStandalone)
  {
    _document.setXmlStandalone(xmlStandalone);
  }
  
  public String getXmlVersion()
  {
    return _document.getXmlVersion();
  }
  
  public void setXmlVersion(String xmlVersion)
  {
    _document.setXmlVersion(xmlVersion);
  }

  public DOMAttr createAttribute(String name)
  {
    return new DOMAttr(_env, _document.createAttribute(name));
  }

  public DOMAttr createAttributeNS(String namespaceURI,
                                   String qualifiedName)
  {
    return new DOMAttr(_env, _document.createAttributeNS(namespaceURI, qualifiedName));
  }

  public DOMNode createCDATASection(String data)
  {
    return new DOMCDATASection(_env, _document.createCDATASection(data));
  }

  public DOMComment createComment(String data)
  {
    return new DOMComment(_env, _document.createComment(data));
  }

 public DOMDocumentFragment createDocumentFragment()
 {
   return new DOMDocumentFragment(_env, _document.createDocumentFragment());
 }

 public DOMElement createElement(String tagName,
                                 @Optional String value)
  {
    DOMElement result = new DOMElement(_env,_document.createElement(tagName));

    if (value != null)
      result.setNodeValue(value);

    return result;
  }

  public DOMElement createElementNS(String namespaceURI,
                                    String qualifiedName,
                                    @Optional String value)
  {
    return new DOMElement(_env, qualifiedName, value, namespaceURI);
  }

  public DOMEntityReference createEntityReference(String name)
  {
    return new DOMEntityReference(_env, _document.createEntityReference(name));
  }

  public DOMProcessingInstruction createProcessingInstruction(String target,
                                                              @Optional String data)
  {
    if (data == null)
      data = "";
    
    return new DOMProcessingInstruction(_env, _document.createProcessingInstruction(target, data));
  }

  public DOMText createTextNode(String data)
  {
    return new DOMText(_env, _document.createTextNode(data));
  }

  public DOMNode getElementById(String elementId)
  {
    return DOMNodeFactory.createDOMNode(_env, _document.getElementById(elementId));
  }

  public DOMNodeListValue getElementsByTagName(String tagname)
  {
    NodeList elements = _document.getElementsByTagName(tagname);
    DOMNodeListValue result = new DOMNodeListValue(_env, elements);
    int length = elements.getLength();
    
    for (int i=0; i < length; i++) {
      result.put(_env.wrapJava(DOMNodeFactory.createDOMNode(_env, elements.item(i))));
    }
    
    return result;
  }

  public DOMNodeListValue getElementsByTagNameNS(String namespaceURI,
                                                 String localName)
  {
    NodeList elements = _document.getElementsByTagNameNS(namespaceURI, localName);
    DOMNodeListValue result = new DOMNodeListValue(_env, elements);
    int length = elements.getLength();

    for (int i=0; i < length; i++) {
      result.put(_env.wrapJava(DOMNodeFactory.createDOMNode(_env, elements.item(i))));
    }

    return result;
  }

  public DOMNode importNode(DOMNode node,
                            @Optional("false") boolean deep)
  {
    return DOMNodeFactory.createDOMNode(_env, _document.importNode(node.getNode(), deep));
  }

  //XXX: need to implement static version which returns a DOMDocument
  public DOMDocument load(String filename,
                          @Optional Value options)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    try {
      DocumentBuilder builder = _documentBuilderFactory.newDocumentBuilder();
      _is = new BufferedInputStream(new FileInputStream(new File(filename)));
      _document = builder.parse(_is);
      return this;
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return null;
    }
  }
  
  public DOMDocument loadXML(String source,
                             @Optional Value options)
  {
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    try {
      DocumentBuilder builder = _documentBuilderFactory.newDocumentBuilder();
      _is = new StringInputStream(source);
      _document = builder.parse(_is);
      return this;
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return null;
    }
  }
  
  public void normalize()
  {
      _document.normalizeDocument();
  }
  
  public boolean relaxNGValidate(String fileName)
  {
    try {
      InputStream is = new FileInputStream(new File(fileName));
      return validateInputStream(is, XMLConstants.RELAXNG_NS_URI);
    } catch (IOException e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return false;
    }
  }
  
  public boolean relaxNGValidateSource(String source)
  {
    return validateInputStream(new StringInputStream(source), XMLConstants.RELAXNG_NS_URI);
  }
  
  public boolean schemaValidate(String fileName)
  {
    try {
      InputStream is = new FileInputStream(new File(fileName));
      return validateInputStream(is, XMLConstants.W3C_XML_SCHEMA_NS_URI);
    } catch (IOException e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return false;
    }
  }
  
  private boolean validateInputStream(InputStream is,
                                      String xmlConstant)
  {
    SchemaFactory factory = SchemaFactory.newInstance(xmlConstant);
    
    try {
      Source schemaFile = new StreamSource(is);
      Schema schema = factory.newSchema(schemaFile);
      
      Validator validator = schema.newValidator();
      
      try {
        validator.validate(new DOMSource(_document));
        return true;
      } catch (Exception e) {
        log.log(Level.FINE, L.l(e.getMessage()), e);
        return false;
      }
      
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return false;
    }
  }
  
  public boolean schemaValidateSource(InputStream source)
  {
    return validateInputStream(source, XMLConstants.W3C_XML_SCHEMA_NS_URI);
  }

  public boolean validate()
  {
    if ((_document != null) && (_documentBuilderFactory != null) && (_documentBuilderFactory.isValidating()))
      return true;

    //Need to re-parse with a validating parser
    if (_is == null)
      return false;
    
    if (_documentBuilderFactory == null)
      _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    _documentBuilderFactory.setValidating(true);
    
    try {
      DocumentBuilder builder = _documentBuilderFactory.newDocumentBuilder();
      _document = builder.parse(_is);
      return true;
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
      return false;
    } 
  }

  //XXX: does not yet support options (ie: LIBXML_NOEMPTYTAG)
  public Value save(Env env,
                    String fileName,
                    @Optional int options)
  {
    Value result = BooleanValue.FALSE;
    
    BufferedWriter bw = null;
    
    try {
      
      bw = new BufferedWriter (new FileWriter(new File(fileName)));
      SimpleXMLElement simpleXML = new SimpleXMLElement(env, _document, _document.getDocumentElement());
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
   * @param domNode
   * @param options Not yet supported LIBXML_NOEMPTYTAG
   * @return XML as string or FALSE
   */
  public Value saveXML(@Optional DOMNode domNode,
                       @Optional int options)
  {  
    SimpleXMLElement simpleXML;
    Node node = null;
    
    if (domNode != null)
      node = domNode.getNode();
    
    if (node == null) {
      simpleXML = new SimpleXMLElement(_env, _document, _document.getDocumentElement());   
      return new StringValueImpl(simpleXML.asXML().toString());
      
    } else {
      simpleXML = new SimpleXMLElement(_env, _document, ((Element) node));
      return new StringValueImpl(simpleXML.generateXML().toString());
    }
  }
  
  public Value saveHTML()
  {
    return saveXML(null, 0);
  }
  
  public Value saveHTMLFile(Env env,
                            String filename)
  {
    return save(env, filename, 0);
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
}
