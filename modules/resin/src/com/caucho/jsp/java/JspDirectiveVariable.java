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
import com.caucho.jsp.cfg.TldVariable;
import com.caucho.jsp.cfg.TldAttribute;

import com.caucho.config.ConfigException;

import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

public class JspDirectiveVariable extends JspNode {
  private static final QName NAME_GIVEN = new QName("name-given");
  private static final QName NAME_FROM_ATTRIBUTE =
    new QName("name-from-attribute");
  private static final QName ALIAS = new QName("alias");
  private static final QName VARIABLE_CLASS = new QName("variable-class");
  private static final QName DECLARE = new QName("declare");
  private static final QName SCOPE = new QName("scope");
  private static final QName DESCRIPTION = new QName("description");
  
  static final L10N L = new L10N(JspDirectiveVariable.class);

  private String _nameGiven;
  private String _nameFromAttribute;
  private String _alias;
  private String _variableClass;
  private boolean _isDeclare = true;
  private String _scope;
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
    if (NAME_GIVEN.equals(name))
      _nameGiven = value;
    else if (NAME_FROM_ATTRIBUTE.equals(name))
      _nameFromAttribute = value;
    else if (ALIAS.equals(name))
      _alias = value;
    else if (VARIABLE_CLASS.equals(name))
      _variableClass = value;
    else if (DECLARE.equals(name))
      _isDeclare = attributeToBoolean(name.getName(), value);
    else if (SCOPE.equals(name)) {
      if (! "NESTED".equals(value) &&
	  ! "AT_BEGIN".equals(value) &&
	  ! "AT_END".equals(value))
	throw error(L.l("`{0}' is an illegal scope value.  NESTED, AT_BEGIN, and AT_END are the only accepted values.",
			value));

      _scope = value;
    }
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
      throw error(L.l("`{0}' is only allowed in .tag files.  Variable directives are not allowed in normal JSP files.",
                      getTagName()));
    
    if (_nameGiven == null && _nameFromAttribute == null)
      throw error(L.l("<{0}> needs a `name-given' or `name-from-attribute' attribute.",
                      getTagName()));

    if (_nameFromAttribute != null && _alias == null)
      throw error(L.l("<{0}> needs an `alias' attribute.  name-from-attribute requires an alias attribute.",
                      getTagName()));

    JavaTagGenerator tagGen = (JavaTagGenerator) _gen;

    TldVariable var = new TldVariable();
    var.setNameGiven(_nameGiven);
    var.setNameFromAttribute(_nameFromAttribute);
    var.setAlias(_alias);

    String name = _nameGiven;
    if (name == null)
      name = _nameFromAttribute;

    if (_variableClass != null)
      var.setVariableClass(_variableClass);

    var.setDeclare(_isDeclare);
    if (_scope != null)
      var.setScope(_scope);

    tagGen.addVariable(var);
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
    os.print("<jsp:directive.variable");

    if (_nameGiven != null)
      os.print(" name-given=\"" + _nameGiven + "\"");

    if (_nameFromAttribute != null)
      os.print(" name-from-attribute=\"" + _nameFromAttribute + "\"");

    if (_variableClass != null)
      os.print(" variable-class=\"" + _variableClass + "\"");

    os.print("/>");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    JavaTagGenerator gen = (JavaTagGenerator) _gen;

    if (_nameFromAttribute == null)
      return;

    ArrayList<TldAttribute> attributes = gen.getAttributes();
    for (int i = 0; i < attributes.size(); i++) {
      TldAttribute attr = attributes.get(i);

      if (! attr.getName().equals(_nameFromAttribute))
	continue;

      if (! String.class.equals(attr.getType()))
	throw error(L.l("name-from-attribute variable `{0}' needs a matching String attribute, not `{1}' .  name-from-attribute requires a matching String attribute.",
			_nameFromAttribute, attr.getType().getName()));

      return;
    }
    
    throw error(L.l("name-from-attribute variable `{0}' needs a matching String attribute.",
		    _nameFromAttribute));
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
