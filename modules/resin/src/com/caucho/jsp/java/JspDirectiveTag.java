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
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

public class JspDirectiveTag extends JspNode {
  private static final QName IS_EL_IGNORED = new QName("isELIgnored");
  private static final QName IS_VELOCITY_ENABLED =
    new QName("isVelocityEnabled");
  private static final QName PAGE_ENCODING = new QName("pageEncoding");
  private static final QName LANGUAGE = new QName("language");
  private static final QName IMPORT = new QName("import");
  private static final QName DISPLAY_NAME = new QName("display-name");
  private static final QName SMALL_ICON = new QName("small-icon");
  private static final QName LARGE_ICON = new QName("large-icon");
  private static final QName DESCRIPTION = new QName("description");
  private static final QName EXAMPLE = new QName("example");
  private static final QName DYNAMIC_ATTRIBUTES =
    new QName("dynamic-attributes");
  private static final QName BODY_CONTENT = new QName("body-content");
  
  static final L10N L = new L10N(JspDirectiveTag.class);

  private Boolean _isElIgnored;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (IS_EL_IGNORED.equals(name)) {
      boolean isIgnored = value.equals("true");

      _parseState.setELIgnored(isIgnored);
      
      if (_isElIgnored != null && _isElIgnored.booleanValue() != isIgnored)
	throw error(L.l("isELIgnored values conflict"));

      _isElIgnored = new Boolean(isIgnored);
    }
    /*
    else if (name.equals("isScriptingInvalid"))
      _parseState.setScriptingInvalid(value.equals("true"));
    */
    else if (IS_VELOCITY_ENABLED.equals(name))
      _parseState.setVelocityEnabled(value.equals("true"));
    else if (PAGE_ENCODING.equals(name)) {
      String oldEncoding = _parseState.getPageEncoding();
      
      if (oldEncoding != null && ! value.equals(oldEncoding))
        throw error(L.l("pageEncoding `{0}' conflicts with previous value of pageEncoding `{1}'.  Check the .jsp and any included .jsp files for conflicts.", value, oldEncoding));
      
      _parseState.setPageEncoding(value);
    }
    else if (LANGUAGE.equals(name)) {
      if (! value.equals("java"))
        throw error(L.l("`{0}' is not supported as a JSP scripting language.",
                        value));
    }
    else if (IMPORT.equals(name)) {
      _parseState.addImport(value);
    }
    else if (DISPLAY_NAME.equals(name)) {
    }
    else if (SMALL_ICON.equals(name)) {
    }
    else if (LARGE_ICON.equals(name)) {
    }
    else if (DESCRIPTION.equals(name)) {
    }
    else if (EXAMPLE.equals(name)) {
    }
    else if (DYNAMIC_ATTRIBUTES.equals(name)) {
      JavaTagGenerator gen = (JavaTagGenerator) _gen;

      gen.setDynamicAttributes(value);
    }
    else if (BODY_CONTENT.equals(name)) {
      if (value.equals("scriptless") ||
	  value.equals("tagdependent") ||
	  value.equals("empty")) {
      }
      else
	throw error(L.l("`{0}' is an unknown body-content value for the JSP tag directive attribute.  'scriptless', 'tagdependent', and 'empty' are the allowed values.",
                      value));

      ((JavaTagGenerator) _gen).setBodyContent(value);
    }
    else {
      throw error(L.l("`{0}' is an unknown JSP tag directive attribute.  See the JSP documentation for a complete list of tag directive attributes.",
                      name.getName()));
    }
  }
  
  protected String getRelativeUrl(String value)
  {
    if (value.length() > 0 && value.charAt(0) == '/')
      return value;
    else
      return _parseState.getUriPwd() + value;
  }

  /**
   * Charset can be specific as follows:
   *  test/html; z=9; charset=utf8; w=12
   */
  static String parseCharEncoding(String type)
    throws JspParseException
  {
    type = type.toLowerCase();
    int i;
    char ch;
    while ((i = type.indexOf(';')) >= 0 && i < type.length()) {
      i++;
      while (i < type.length() && ((ch = type.charAt(i)) == ' ' || ch == '\t'))
	i++;

      if (i >= type.length())
	return null;

      type = type.substring(i);
      i = type.indexOf('=');
      if (i >= 0) {
	if (! type.startsWith("charset"))
	  continue;

	for (i++;
	     i < type.length() && ((ch = type.charAt(i)) == ' ' || ch == '\t');
	     i++) {
	}

	type = type.substring(i);
      }
      
      for (i = 0;
	   i < type.length() && (ch = type.charAt(i)) != ';' && ch != ' ';
	   i++) {
      }

      return type.substring(0, i);
    }

    return null;
  }
  
  /**
   * Called when the tag ends.
   */
  public void endAttributes()
    throws JspParseException
  {
    if (! _gen.isTag())
      throw error(L.l("tag directive is only allowed in tag files."));
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
    JavaTagGenerator gen = (JavaTagGenerator) _gen;
    
    os.print("<jsp:directive.tag");
    if (! _parseState.isELIgnored())
      os.print(" el-ignored='false'");
    /*
    if (! _parseState.isScriptingEnabled())
      os.print(" scripting-enabled='false'");
    */
    String dynAttr = gen.getDynamicAttributes();
    if (dynAttr != null)
      os.print("dynamic-attributes='" + dynAttr + "'");

    os.print("/>");
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
