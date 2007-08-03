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

package com.caucho.jsp.cfg;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.DependencyBean;
import com.caucho.jsp.JspParseException;
import com.caucho.util.L10N;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;
import javax.faces.component.UIComponent;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Configuration for the taglib tag in the .tld
 */
public class JsfTag extends TldTag {
  private Class _componentClass;
  
  public void setComponentClass(Class cl)
  {
    Config.validate(cl, UIComponent.class);
    
    _componentClass = cl;
  }
  
  public Class getComponentClass()
  {
    return _componentClass;
  }
  
  public String toString()
  {
    return "JsfTag[" + getName() + "]";
  }
}
