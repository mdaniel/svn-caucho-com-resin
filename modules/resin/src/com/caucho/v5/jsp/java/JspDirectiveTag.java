/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.jsp.java;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.jsp.JspParseException;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;

import java.io.IOException;
import java.util.Locale;

public class JspDirectiveTag extends JspNode {
  private static final NameCfg IMPORT = new NameCfg("import");
  private static final NameCfg DISPLAY_NAME = new NameCfg("display-name");
  private static final NameCfg BODY_CONTENT = new NameCfg("body-content");
  private static final NameCfg DYNAMIC_ATTRIBUTES
    = new NameCfg("dynamic-attributes");
  private static final NameCfg SMALL_ICON = new NameCfg("small-icon");
  private static final NameCfg LARGE_ICON = new NameCfg("large-icon");
  private static final NameCfg DESCRIPTION = new NameCfg("description");
  private static final NameCfg EXAMPLE = new NameCfg("example");
  private static final NameCfg LANGUAGE = new NameCfg("language");
  private static final NameCfg PAGE_ENCODING = new NameCfg("pageEncoding");
  private static final NameCfg IS_EL_IGNORED = new NameCfg("isELIgnored");
  private static final NameCfg DEFERRED_AS_LITERAL
    = new NameCfg("deferredSyntaxAllowedAsLiteral");
  private static final NameCfg TRIM_WHITESPACES
    = new NameCfg("trimDirectiveWhitespaces");
  
  private static final NameCfg IS_VELOCITY_ENABLED =
    new NameCfg("isVelocityEnabled");
  
  static final L10N L = new L10N(JspDirectiveTag.class);

  private String _import;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(NameCfg name, String value)
    throws JspParseException
  {
    if (! _gen.isTag())
      throw error(L.l("@tag directive is only allowed in tag files"));
    
    JavaTagGenerator gen = (JavaTagGenerator) _gen;

    if (IS_EL_IGNORED.equals(name)) {
      boolean isIgnored = value.equals("true");

      if (_parseState.isELIgnoredPageSpecified() &&
          isIgnored != _parseState.isELIgnored())
        throw error(L.l("isELIgnored values conflict"));

      _parseState.setELIgnored(isIgnored);
      _parseState.setELIgnoredPageSpecified(true);
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
        throw error(L.l("pageEncoding '{0}' conflicts with previous value of pageEncoding '{1}'.  Check the .jsp and any included .jsp files for conflicts.", value, oldEncoding));

      _parseState.setPageEncoding(value);
      _parseState.setCharEncoding(value);
    }
    else if (LANGUAGE.equals(name)) {
      if (! value.equals("java"))
        throw error(L.l("'{0}' is not supported as a JSP scripting language.",
                        value));
    }
    else if (IMPORT.equals(name)) {
      _import = value;
      
      _parseState.addImport(value);
    }
    else if (DISPLAY_NAME.equals(name)) {
      String oldValue = gen.getDisplayName();
      
      if (oldValue != null && ! oldValue.equals(value))
        throw error(L.l("@tag display-name '{0}' conflicts with previous value '{1}'.  The display-name attribute may only be specified once.",
                        value, oldValue));

      
      gen.setDisplayName(value);
    }
    else if (SMALL_ICON.equals(name)) {
      String oldValue = gen.getSmallIcon();
      
      if (oldValue != null && ! oldValue.equals(value))
        throw error(L.l("@tag small-icon '{0}' conflicts with previous value '{1}'.  The small-icon attribute may only be specified once.",
                        value, oldValue));

      
      gen.setSmallIcon(value);
    }
    else if (LARGE_ICON.equals(name)) {
      String oldValue = gen.getLargeIcon();
      
      if (oldValue != null && ! oldValue.equals(value))
        throw error(L.l("@tag large-icon '{0}' conflicts with previous value '{1}'.  The large-icon attribute may only be specified once.",
                        value, oldValue));

      
      gen.setLargeIcon(value);
    }
    else if (DESCRIPTION.equals(name)) {
      String oldValue = gen.getDescription();
      
      if (oldValue != null && ! oldValue.equals(value))
        throw error(L.l("@tag description '{0}' conflicts with previous value '{1}'.  The description attribute may only be specified once.",
                        value, oldValue));

      
      gen.setDescription(value);
    }
    else if (EXAMPLE.equals(name)) {
      String oldValue = gen.getExample();
      
      if (oldValue != null && ! oldValue.equals(value))
        throw error(L.l("@tag example '{0}' conflicts with previous value '{1}'.  The example attribute may only be specified once.",
                        value, oldValue));

      
      gen.setExample(value);
    }
    else if (DYNAMIC_ATTRIBUTES.equals(name)) {
      String oldValue = gen.getDynamicAttributes();
      
      if (oldValue != null && ! oldValue.equals(value))
        throw error(L.l("@tag dynamic-attributes '{0}' conflicts with previous value '{1}'.  The dynamic-attributes attribute may only be specified once.",
                        value, oldValue));
      else if (gen.findAttribute(value) != null) {
        throw error(L.l("@tag dynamic-attributes '{0}' conflicts with an attribute.",
                        value));
      }
      else if (gen.findVariable(value) != null) {
        throw error(L.l("@tag dynamic-attributes '{0}' conflicts with a variable.",
                        value));
      }
      
      gen.setDynamicAttributes(value);
    }
    else if (BODY_CONTENT.equals(name)) {
      String oldValue = gen.getBodyContent();
      
      if (oldValue != null && ! oldValue.equals(value)) {
        throw error(L.l("@tag body-content '{0}' conflicts with previous value '{1}'.  The body-content attribute may only be specified once.",
                        value, oldValue));
      }

      if (value.equals("scriptless")
          || value.equals("tagdependent")
          || value.equals("empty")) {
      }
      else
        throw error(L.l("'{0}' is an unknown body-content value for the JSP tag directive attribute.  'scriptless', 'tagdependent', and 'empty' are the allowed values.",
                      value));

      gen.setBodyContent(value);
    }
    else if (DEFERRED_AS_LITERAL.equals(name)) {
      _parseState.setDeferredSyntaxAllowedAsLiteral(value.equals("true"));
    }
    else if (TRIM_WHITESPACES.equals(name)) {
      if (value.equals("true"))
        _parseState.setTrimWhitespace(true);
      else if (value.equals("false"))
        _parseState.setTrimWhitespace(false);
      else
        throw error(L.l("trimDirectiveWhitespaces expects 'true' or 'false' at '{0}'",
                        value));
    }
    else {
      throw error(L.l("'{0}' is an unknown JSP tag directive attribute.  The valid attributes are: body-content, deferredSyntaxAllowedAsLiteral, display-name, dynamic-attributes, example, isELIgnored, language, large-icon, pageEncoding, small-icon, trimDirectiveWhitespaces.",
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
    type = type.toLowerCase(Locale.ENGLISH);
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
    os.print(" jsp:id=\"" + gen.generateJspId() + "\"");

    if (_parseState.isELIgnoredPageSpecified())
      os.print(" el-ignored='" + _parseState.isELIgnored() + "'");

    if (_import != null)
      os.print(" import='" + _import + "'");
    
    /*
    if (! _parseState.isScriptingEnabled())
      os.print(" scripting-enabled='false'");
    */
    String dynAttr = gen.getDynamicAttributes();
    if (dynAttr != null)
      os.print(" dynamic-attributes='" + dynAttr + "'");

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
