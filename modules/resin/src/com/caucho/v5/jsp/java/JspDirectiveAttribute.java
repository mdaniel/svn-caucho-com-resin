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
import com.caucho.v5.config.types.Signature;
import com.caucho.v5.jsp.JspParseException;
import com.caucho.v5.jsp.cfg.TldAttribute;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;

import javax.servlet.jsp.tagext.JspFragment;

import java.io.IOException;

public class JspDirectiveAttribute extends JspNode {
  static L10N L = new L10N(JspDirectiveAttribute.class);

  private static final NameCfg NAME = new NameCfg("name");
  private static final NameCfg REQUIRED = new NameCfg("required");
  private static final NameCfg FRAGMENT = new NameCfg("fragment");
  private static final NameCfg RTEXPRVALUE = new NameCfg("rtexprvalue");
  private static final NameCfg TYPE = new NameCfg("type");
  private static final NameCfg DESCRIPTION = new NameCfg("description");
  private static final NameCfg DEFERRED_VALUE
    = new NameCfg("deferredValue");
  private static final NameCfg DEFERRED_VALUE_TYPE
    = new NameCfg("deferredValueType");
  private static final NameCfg DEFERRED_METHOD
    = new NameCfg("deferredMethod");
  private static final NameCfg DEFERRED_METHOD_SIGNATURE
    = new NameCfg("deferredMethodSignature");

  private String _name;
  private String _type;
  private boolean _isRequired;
  private boolean _isFragment;
  private Boolean _isRtexprvalue;
  private String _description;

  private Boolean _deferredValue;
  private String _deferredValueType;
  private Boolean _deferredMethod;
  private String _deferredMethodSignature;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(NameCfg name, String value)
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("'{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));
    
    JavaTagGenerator gen = (JavaTagGenerator) _gen;
    
    if (NAME.equals(name)) {
      if (gen.findVariable(value) != null) {
        throw error(L.l("@attribute name '{0}' is already used by a variable.",
                        value));
      }
      else if (gen.findAttribute(value) != null) {
        throw error(L.l("@attribute name '{0}' is already used by another attribute.",
                        value));
      }
      
      _name = value;
    }
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
    else if (DEFERRED_VALUE.equals(name)) {
      if (_gen.isPre21())
        throw error("deferredValue requires JSP 2.1 or later tag file");
      
      _deferredValue = attributeToBoolean(name.getName(), value);
      if (_deferredValue)
        _type = "javax.el.ValueExpression";
    }
    else if (DEFERRED_VALUE_TYPE.equals(name)) {
      if (_gen.isPre21())
        throw error("deferredValueType requires JSP 2.1 or later tag file");
      
      _type = "javax.el.ValueExpression";
      _deferredValueType = value;
    }
    else if (DEFERRED_METHOD.equals(name)) {
      if (_gen.isPre21())
        throw error("deferredMethod requires JSP 2.1 or later tag file");
      
      _deferredMethod = attributeToBoolean(name.getName(), value);
      if (Boolean.TRUE.equals(_deferredMethod))
        _type = "javax.el.MethodExpression";
    }
    else if (DEFERRED_METHOD_SIGNATURE.equals(name)) {
      if (_gen.isPre21())
        throw error("deferredMethodSignature requires JSP 2.1 or later tag file");
      
      try {
        new Signature(value);
      } catch (Exception e) {
        throw error(e.getMessage());
      }

      _type = "javax.el.MethodExpression";
      _deferredMethodSignature = value;
    }
    else {
      throw error(L.l("'{0}' is an unknown JSP attribute directive attributes.  The valid attributes are: deferredMethod, deferredMethodSignature, deferredValue, deferredValueType, description, fragment, name, rtexprvalue, type.",
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
      throw error(L.l("'{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));
    
    if (_name == null)
      throw error(L.l("<{0}> needs a 'name' attribute.",
                      getTagName()));

    JavaTagGenerator tagGen = (JavaTagGenerator) _gen;

    TldAttribute attr = new TldAttribute();
    attr.setName(_name);

    if (_type == null) {
      attr.setType(String.class);
    }
    else {
      Class type = loadClass(_type);

      if (type == null)
        throw error(L.l("type '{0}' is an unknown class for tag attribute {1}.",
                        _type, _name));

      if (type.isPrimitive())
        throw error(L.l(
          "attribute type '{0}' cannot be a Java primitive for {1}.",
          _type,
          _name));

      attr.setType(type);
    }

    attr.setRequired(_isRequired);
    
    if (_isFragment && _isRtexprvalue != null)
      throw error(L.l("@attribute rtexprvalue cannot be set when fragment is true."));
    
    if (_isFragment && _type != null)
      throw error(L.l("@attribute type cannot be set when fragment is true."));

    if (_isRtexprvalue == null || Boolean.TRUE.equals(_isRtexprvalue))
      attr.setRtexprvalue(Boolean.TRUE);
    
    if (_isFragment)
      attr.setType(JspFragment.class);
    
    if (_deferredValue != null && _deferredValueType != null)
      throw error(L.l("@attribute deferredValue and deferredValueType may not both be specified"));
    
    if (_deferredMethod != null && _deferredMethodSignature != null)
      throw error(L.l("@attribute deferredMethod and deferredMethodSignature may not both be specified"));
    
    if ((_deferredValue != null || _deferredValueType != null)
        && (_deferredMethod != null || _deferredMethodSignature != null))
      throw error(L.l("@attribute deferredValue and deferredMethod may not both be specified"));

    if (Boolean.TRUE.equals(_deferredValue) || _deferredValueType != null) {
      attr.setDeferredValue(new TldAttribute.DeferredValue());

      if (_deferredValueType != null)
        attr.getDeferredValue().setType(_deferredValueType);
    }

    if (Boolean.TRUE.equals(_deferredMethod)
        || _deferredMethodSignature != null) {
      attr.setDeferredMethod(new TldAttribute.DeferredMethod());

      if (_deferredMethodSignature != null)
        attr.getDeferredMethod().setMethodSignature(new Signature(_deferredMethodSignature));
    }

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
    os.print("<jsp:directive.attribute");
    os.print(" jsp:id=\"" + _gen.generateJspId() + "\"");
    os.print(" name=\"" + _name + "\"");

    if (_type != null)
      os.print(" type=\"" + _type + "\"");

    os.println("/>");
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
