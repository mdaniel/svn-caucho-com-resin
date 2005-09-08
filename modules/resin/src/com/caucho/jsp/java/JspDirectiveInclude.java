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

import javax.servlet.jsp.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.xml.sax.*;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.jsp.*;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;
import com.caucho.xml.Xml;

public class JspDirectiveInclude extends JspNode {
  static L10N L = new L10N(JspDirectiveInclude.class);

  static private final QName FILE = new QName("file");

  private String _file;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (FILE.equals(name))
      _file = value;
    else {
      throw error(L.l("'{0}' is an unknown JSP include directive attributes.  Compile-time includes need a 'file' attribute.",
                      name.getName()));
    }
  }

  /**
   * When the element completes.
   */
  public void endElement()
    throws JspParseException
  {
    if (_file == null)
      throw error(L.l("<{0}> needs a 'file' attribute.",
                      getTagName()));

    try {
      ParseState parseState = _gen.getParseState();
      
      if (parseState.isXml()) {
	Xml xml = new Xml();
	xml.setContentHandler(new JspContentHandler(parseState.getBuilder()));
	Path path = parseState.resolvePath(_file);
	
	path.setUserPath(_file);
	xml.setNamespaceAware(true);
	xml.parse(path);
      }
      else
	_gen.getJspParser().pushInclude(_file);
    } catch (SAXException e) {
      throw error(e);
    } catch (IOException e) {
      throw error(e);
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
    // jsp/0354
    // os.print("<jsp:directive.include file=\"" + _file + "\"/>");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
  }
}
