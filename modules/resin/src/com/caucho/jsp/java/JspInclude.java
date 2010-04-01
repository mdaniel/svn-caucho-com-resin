/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.jsp.java;

import com.caucho.config.Module;
import com.caucho.jsp.JspParseException;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a Java scriptlet.
 */
@Module
public class JspInclude extends JspNode {
  private static final QName PAGE = new QName("page");
  private static final QName FLUSH = new QName("flush");
  
  private String _page;
  private boolean _flush = false; // jsp/15m4
  
  private String _text;
  
  private ArrayList<JspParam> _params;

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (PAGE.equals(name))
      _page = value;
    else if (FLUSH.equals(name))
      _flush = value.equals("true");
    else
      throw error(L.l("'{0}' is an invalid attribute in <jsp:include>",
                      name.getName()));
  }
  
  /**
   * True if the node has scripting
   */
  public boolean hasScripting()
  {
    if (_params == null)
      return false;
    
    for (int i = 0; i < _params.size(); i++) {
      if (_params.get(i).hasScripting())
	return true;
    }

    return false;
  }

  /**
   * True if the jsf-parent setting is required.
   */
  @Override
  public boolean isJsfParentRequired()
  {
    return true;
  }

  /**
   * Adds text to the scriptlet.
   */
  public JspNode addText(String text)
  {
    _text = text;

    return null;
  }

  /**
   * Adds a parameter.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspParam) {
      JspParam param = (JspParam) node;

      if (_params == null)
        _params = new ArrayList<JspParam>();

      _params.add(param);
    }
    else {
      super.addChild(node);
    }
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:include");

    printXmlAttribute(os, "page", _page);

    os.print("></jsp:include>");
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_page == null)
      throw error(L.l("<jsp:include> expects a 'page' attribute.  'page' specifies the path to include."));

    if (hasRuntimeAttribute(_page)) {
      out.print("pageContext.include(");
      out.print(getRuntimeAttribute(_page));
    }
    else if (_params == null) {
      out.print("pageContext.include(");
      out.print(generateParameterValue(String.class, _page));
      out.println(", " + _flush + ");");
    }
    else {
      out.print("pageContext.include(");
      
      for (int i = 0; i < _params.size(); i++) {
        out.print("pageContext.encode(");
      }
      
      out.print("pageContext.encode(");
      out.print(generateParameterValue(String.class, _page));
      out.print(")");
      
      for (int i = 0; i < _params.size(); i++) {
        if (i > 0)
          out.print(".append('&')");
        
        out.print(", ");
        
        generateIncludeParam(out, _params.get(i));
        
        out.print(")");
      }
      
      out.println(".toString(), " + _flush + ");");
    }
  }
}
