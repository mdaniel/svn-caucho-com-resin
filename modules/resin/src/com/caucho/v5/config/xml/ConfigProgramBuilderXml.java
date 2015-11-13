/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.cf.ProgramCommand;
import com.caucho.v5.config.cf.ProgramCommandClassName;
import com.caucho.v5.config.cf.ProgramPropertyExpr;
import com.caucho.v5.config.cf.ProgramPropertyString;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.el.ELParser;
import com.caucho.v5.xml.QAbstractNode;
import com.caucho.v5.xml.QAttr;
import com.caucho.v5.xml.QElement;
import com.caucho.v5.xml.QText;

/**
 * Builds a config program from XML.
 */
public class ConfigProgramBuilderXml
{
  private ConfigXml _config;
  
  ConfigProgramBuilderXml(ConfigXml config)
  {
    _config = config;
  }
  
  public ConfigProgram buildTop(Node node)
  {    
    ContainerProgram program = new ContainerProgram();
    
    String ns = node.getNamespaceURI();
    
    // jsp/0135
    if (node.getNodeName().equals("web-app") && (ns == null || ns.equals(""))) {
      ProgramPropertyString prop = new ProgramPropertyString(_config, "pre23Config", "true");
            
      program.addProgram(prop);
    }
    
    return buildElementChildren(program, (QElement) node);
  }
  
  public ConfigProgram build(Node node)
  {
    switch (node.getNodeType()) {
    case Node.ELEMENT_NODE:
      return buildElement((QElement) node);
      
    case Node.ATTRIBUTE_NODE:
      return buildAttribute((QAttr) node);
      
    case Node.COMMENT_NODE:
      return null;
      
    case Node.TEXT_NODE:
      return buildText((QText) node);
      
    case Node.PROCESSING_INSTRUCTION_NODE:
      return null;
      
    default:
      throw new UnsupportedOperationException(String.valueOf(node));
    }
  }
  
  public ConfigProgram buildElement(QElement element)
  {
    String ns = getNamespace(element);
    String name = element.getLocalName();
    
    NameCfg qName = new NameCfg(name, ns);
    
    String value = getElementValue(element);
    
    if (value != null) {
      return buildProperty(element, name, ns, value);
    }
    
    if (ns != null && ns.startsWith("urn:java:")) {
      // String pkg = ns.substring("urn:java:".length());
      
      ProgramCommandClassName program
        = new ProgramCommandClassName(qName);
      
      program.setLocation(element.getFilename(), element.getLine());
      
      buildElementChildren(program, element);
      
      return program;
    }
    else {
      ProgramCommand program = new ProgramCommand(qName);
      
      program.setLocation(element.getFilename(), element.getLine());
      
      buildElementChildren(program, element);
      
      return program;
    }
  }
  
  private String getNamespace(Node node)
  {
    return node.getNamespaceURI();
  }
    
  public ConfigProgram buildAttribute(QAttr attr)
  {
    String ns = attr.getNamespaceURI();
    String name = attr.getLocalName();
    
    if (name.startsWith("xmlns") 
        || "http://www.w3.org/2000/xmlns/".equals(ns)) {
      return null;
    }
    else if ("xml".equals(attr.getPrefix()) && name.equals("space")) {
      return null;
    }
    
    return buildProperty(attr, name, ns, attr.getValue());
  }
  
  public ConfigProgram buildText(QText text)
  {
    String data = text.getData();
    
    data = data.trim();
    
    if (data.equals("")) {
      return null;
    }
    
    return buildProperty(text, "#text", "", data);
  }
  
  private ConfigProgram buildProperty(QAbstractNode node, String name, String ns, String value)
  {
    NameCfg qName = new NameCfg(name, ns);
    
    if (ns != null && ns.startsWith("urn:java:")) {
      // String pkg = ns.substring("urn:java:".length());
      
      ProgramCommandClassName program
        = new ProgramCommandClassName(qName);
      
      program.setLocation(node.getFilename(), node.getLine());
      
      if (!value.trim().equals("")) {
        program.addProgram(buildProperty(node, "#text", "", value));
      }
      
      return program;
    }
    else if (value.indexOf("${") >= 0) {
      ProgramPropertyExpr program;
      
      ExprCfg expr = ExprCfg.newParser(value).parse();
      
      program = new ProgramPropertyExpr(_config, qName, expr, value);
      
      program.setLocation(node.getFilename(), node.getLine());
      
      return program;
    }
    else {
      ProgramPropertyString program
        = new ProgramPropertyString(_config, qName, value);
      
      program.setLocation(node.getFilename(), node.getLine());
      
      return program;
    }
  }
  
  /**
   * Returns the value for an element with only a solo text. 
   */
  private String getElementValue(QElement element)
  {
    boolean isTrim = true;
    
    for (Attr attr = element.getFirstAttribute(); 
        attr != null; 
        attr = (Attr) attr.getNextSibling()) {
      if ("xml:space".equals(attr.getName())) {
        isTrim = false;
      }
      else {
        return null;
      }
    }
    
    Node node = element.getFirstChild();
    
    if (node == null) {
      return "";
    }
    
    if (! (node instanceof Text) || node.getNextSibling() != null) {
      return null;
    }
    
    Text text = (Text) node;
  
    if (isTrim) {
      return text.getData().trim();
    }
    else {
      return text.getData();
    }
  }
  
  public ConfigProgram buildElementChildren(ContainerProgram program,
                                            QElement element)
  {
    for (Attr attr = element.getFirstAttribute();
        attr != null;
        attr = (Attr) attr.getNextSibling()) {
      ConfigProgram childProgram = build(attr);
      
      if (childProgram != null) {
        program.addProgram(childProgram);
      }
    }
    
    for (Node node = element.getFirstChild();
        node != null;
        node = node.getNextSibling()) {
      ConfigProgram childProgram = build(node);
      
      if (childProgram != null) {
        program.addProgram(childProgram);
      }
    }
    
    return program;
  }
}

