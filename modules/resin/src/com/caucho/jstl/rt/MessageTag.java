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

package com.caucho.jstl.rt;

import java.io.*;
import java.util.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.jstl.core.*;
import javax.servlet.jsp.jstl.fmt.*;

import com.caucho.vfs.*;
import com.caucho.jsp.PageContextImpl;
import com.caucho.jstl.ParamContainerTag;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class MessageTag extends BodyTagSupport implements ParamContainerTag {
  private String _key;
  private Object _bundle;

  private ArrayList<Object> _params;

  private String _var;
  private String _scope;

  /**
   * Sets the message key.
   *
   * @param key the key value.
   */
  public void setKey(String key)
  {
    _key = key;
  }

  /**
   * Sets the bundle.
   *
   * @param bundle the localication context
   */
  public void setBundle(Object bundle)
  {
    _bundle = bundle;
  }

  /**
   * Sets the variable.
   *
   * @param var the variable
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope.
   *
   * @param scope the scope
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Add a parameter value to the message.
   */
  public void addParam(Object value)
  {
    if (_params == null)
      _params = new ArrayList<Object>();

    _params.add(value);
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    Object []args = null;

    if (_params != null) {
      args = _params.toArray(new Object[_params.size()]);
      _params = null;
    }
    
    try {
      PageContextImpl pc = (PageContextImpl) pageContext;
      
      JspWriter out = pc.getOut();

      String key = _key;

      if (_key != null)
        key = _key;
      else
        key = getBodyContent().getString().trim();

      String msg;
      
      if (_bundle != null) {
        msg = pc.getLocalizedMessage(_bundle, key, args, null);
      }
      else {
        LocalizationContext lc;
        lc = (LocalizationContext) pageContext.getAttribute("caucho.bundle");

        if (lc == null)
          msg = pc.getLocalizedMessage(key, args, null);
        else
          msg = pc.getLocalizedMessage(lc, key, args, null);
      }

      if (_var != null)
        CoreSetTag.setValue(pc, _var, _scope, msg);
      else
        out.print(msg);
    } catch (IOException e) {
    }

    return EVAL_PAGE;
  }
}
