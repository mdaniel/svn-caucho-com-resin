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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.java;

import java.io.*;
import java.util.*;

import java.lang.reflect.*;
import java.beans.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.xml.QName;

import com.caucho.jsp.*;

/**
 * Represents a custom tag.
 */
public class CustomSimpleTag extends GenericTag {
  JspBody _body;
  private boolean _oldScriptingInvalid;

  /**
   * Simple tags can't be reused.
   */
  public boolean isReuse()
  {
    return false;
  }
  
  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    super.endAttributes();
    
    _oldScriptingInvalid = _parseState.isScriptingInvalid();
    _parseState.setScriptingInvalid(true);
  }
  
  /**
   * Adds a child node.
   */
  public void endElement()
    throws Exception
  {
    super.endElement();
    
    _parseState.setScriptingInvalid(_oldScriptingInvalid);
    
    if (_children == null || _children.size() == 0)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode node = (JspNode) _children.get(i);

      if (node instanceof JspBody) {
        _body = (JspBody) node;
        _children.remove(i);
        return;
      }
    }

    _body = new JspBody();
    _body.setParent(this);
    _body.setGenerator(_gen);
    _body.endAttributes();
    
    for (int i = 0; i < _children.size(); i++) {
      JspNode node = _children.get(i);

      if (! (node instanceof JspAttribute))
        _body.addChild(node);
    }
    _body.endElement();
    _children = null;
  }
  
  /**
   * Set true if the node contains a child tag.
   */
  public boolean hasCustomTag()
  {
    if (_body != null && _body.hasCustomTag())
      return true;
    else
      return super.hasCustomTag();
  }
  
  /**
   * Generates code before the actual JSP.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    super.generatePrologue(out);

    if (_body != null) {
      _body.setJspFragment(true);
      _body.generateFragmentPrologue(out);
    }
  }
  
  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String name = _tag.getId();
    String className = _tagInfo.getTagClassName();
    Class cl = _tagClass;

    if (! isReuse()) {
      generateTagInit(out);
    }
    else if (! isDeclared()) {
      out.println("if (" + name + " == null) {");
      out.pushDepth();
      generateTagInit(out);
      out.popDepth();
      out.println("}");
      out.println();
    }

    fillAttributes(out, name);

    if (_body != null) {
      out.print(name + ".setJspBody(");
      generateFragment(out, _body, "pageContext");
      out.println(");");
    }

    out.println(name + ".doTag();");

    printVarDeclaration(out, VariableInfo.AT_END);
  }

  /**
   * Generates the initialization code for the tag.
   *
   * @param out the output stream
   */
  private void generateTagInit(JspJavaWriter out)
    throws Exception
  {
    TagInstance parent = _tag.getParent();

    String var = _tag.getId();
    String className = _tag.getTagClass().getName();
      
    out.print(var + " = new ");
    out.printClass(_tag.getTagClass());
    out.println("();");

    out.println(var + ".setJspContext(pageContext);");
    JspNode parentNode = getParent().getParentTagNode();
    if (parentNode != null) {
      out.println(var + ".setParent(" + parentNode.getCustomTagName() + ");");
    }

    if (hasCustomTag()) {
      out.println(var + "_adapter = new javax.servlet.jsp.tagext.TagAdapter(" + var + ");");
    }

    ArrayList<QName> names = _tag.getAttributeNames();
    for (int i = 0; i < names.size(); i++) {
      QName name = names.get(i);

      String value = _tag.getAttribute(name);
      if (value == null)
        continue;

      generateSetAttribute(out, var, name, value, false, false);
    }
  }
}
