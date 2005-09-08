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
import java.lang.reflect.Method;
import java.beans.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.vfs.*;
import com.caucho.util.*;

import com.caucho.jsp.*;
import com.caucho.jsp.cfg.TldAttribute;

import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

public class JspDirectiveAttribute extends JspNode {
  static L10N L = new L10N(JspDirectiveAttribute.class);

  private static final QName NAME = new QName("name");
  private static final QName TYPE = new QName("type");
  private static final QName REQUIRED = new QName("required");
  private static final QName FRAGMENT = new QName("fragment");
  private static final QName RTEXPRVALUE = new QName("rtexprvalue");
  private static final QName DESCRIPTION = new QName("description");

  private String _name;
  private String _type;
  private boolean _isRequired;
  private boolean _isFragment;
  private Boolean _isRtexprvalue;
  private String _description;

  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (NAME.equals(name))
      _name = value;
    else if (TYPE.equals(name))
      _type = value;
    else if (REQUIRED.equals(name))
      _isRequired = attributeToBoolean(name.getName(), value);
    else if (FRAGMENT.equals(name))
      _isFragment = attributeToBoolean(name.getName(), value);
    else if (RTEXPRVALUE.equals(name))
      _isRtexprvalue = attributeToBoolean(name.getName(), value);
    else if (DESCRIPTION.equals(name))
      _description = value;
    else {
      throw error(L.l("`{0}' is an unknown JSP attribute directive attributes.  See the JSP documentation for a complete list of page directive attributes.",
                      name.getName()));
    }
  }

  /**
   * When the element complets.
   */
  public void endElement()
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("`{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));
    
    if (_name == null)
      throw error(L.l("<{0}> needs a `name' attribute.",
                      getTagName()));

    JavaTagGenerator tagGen = (JavaTagGenerator) _gen;

    TldAttribute attr = new TldAttribute();
    attr.setName(_name);

    if (_type != null) {
      Class type = loadClass(_type);
      
      if (type == null)
        throw error(L.l("`{0}' is an unknown class for tag attribute {1}.",
                        _type, _name));
      
      attr.setType(type);
    }

    attr.setRequired(_isRequired);
    
    if (_isFragment && _isRtexprvalue != null)
      throw error(L.l("rtexprvalue cannot be set when fragment is true."));

    if (_isRtexprvalue == null || Boolean.TRUE.equals(_isRtexprvalue))
      attr.setRtexprvalue(Boolean.TRUE);
    
    if (_isFragment)
      attr.setType(JspFragment.class);

    tagGen.addAttribute(attr);
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    return true;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:directive.attribute name=\"" + _name + "\"");

    if (_type != null)
      os.print(" type=\"" + _type + "\"/>");
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
