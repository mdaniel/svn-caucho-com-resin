package com.caucho.quercus.lib;

import org.w3c.dom.DOMConfiguration;

/**
 * Created by IntelliJ IDEA. User: Charles Reich Date: Feb 15, 2006 Time:
 * 2:56:27 PM To change this template use File | Settings | File Templates.
 */
public class DOMDocument extends DOMNode {

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

  public DOMDocument()
  {
  }
  
  public DOMDocument(String version)
  {
    this.version = version;
  }
  
  public DOMDocument(String version,
                     String encoding)
  {
    this.version = version;
    this.encoding = encoding;
  }
  

  //METHODS
  //@todo construct()
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
