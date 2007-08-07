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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.jsf;

import java.util.*;

import javax.servlet.*;
import javax.servlet.jsp.tagext.*;
import javax.faces.context.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.webapp.*;

/**
 * Utilities for JsfTags
 */
public class JsfTagUtil {
  public static UIComponent addTransient(ServletRequest req,
					 UIComponent parent,
					 String prevId,
					 Class childClass)
    throws Exception
  {
    FacesContext context = FacesContext.getCurrentInstance();

    if (parent == null) {
      UIComponentClassicTagBase parentTag
	= (UIComponentClassicTagBase) req.getAttribute("caucho.jsf.parent");
    
      parent = parentTag.getComponentInstance();
    
      BodyContent body = parentTag.getBodyContent();

      if (body != null)
	addVerbatim(parent, body);
    }

    UIComponent child = (UIComponent) childClass.newInstance();
    child.setTransient(true);

    addChild(parent, prevId, child);

    return child;
  }
  
  public static UIComponent addVerbatim(UIComponent parent,
					String prevId,
					String text)
    throws Exception
  {
    HtmlOutputText verbatim = new HtmlOutputText();

    verbatim.setTransient(true);
    verbatim.setValue(text);
    verbatim.setEscape(false);

    addChild(parent, prevId, verbatim);

    return verbatim;
  }
  
  public static UIComponent addVerbatim(UIComponent parent,
					String text)
    throws Exception
  {
    HtmlOutputText verbatim = new HtmlOutputText();

    verbatim.setTransient(true);
    verbatim.setValue(text);
    verbatim.setEscape(false);

    parent.getChildren().add(verbatim);

    return verbatim;
  }

  private static void addChild(UIComponent parent,
			       String prevId,
			       UIComponent child)
  {
    if (parent != null) {
      List<UIComponent> children = parent.getChildren();
      int size = children.size();
      boolean hasPrev = prevId == null;

      int i = 0;
      for (; i < size; i++) {
	UIComponent oldChild = children.get(i);

	if (hasPrev && oldChild.getId() != null) {
	  children.add(i, child);

	  return;
	}
	else if (prevId != null && prevId.equals(oldChild.getId()))
	  hasPrev = true;
      }
      
      parent.getChildren().add(child);
    }
  }
  
  public static UIComponent addPersistent(ServletRequest req,
					  UIComponent parent,
					  Class childClass)
    throws Exception
  {
    FacesContext context = FacesContext.getCurrentInstance();

    if (parent == null) {
      UIComponentClassicTagBase parentTag
	= (UIComponentClassicTagBase) req.getAttribute("caucho.jsf.parent");

      parent = parentTag.getComponentInstance();
      
      BodyContent body = parentTag.getBodyContent();
    
      addVerbatim(parent, body);
    }

    UIComponent child = (UIComponent) childClass.newInstance();

    if (parent != null)
      parent.getChildren().add(child);

    return child;
  }
  
  public static UIComponent findPersistent(ServletRequest req,
					   UIComponent parent,
					   String id)
    throws Exception
  {
    FacesContext context = FacesContext.getCurrentInstance();
    
    BodyContent body = null;

    if (parent == null) {
      UIComponentClassicTagBase parentTag
	= (UIComponentClassicTagBase) req.getAttribute("caucho.jsf.parent");
    
      parent = parentTag.getComponentInstance();
      
      body = parentTag.getBodyContent();
    }

    if (parent != null) {
      List<UIComponent> children = parent.getChildren();
      int size = children.size();

      String prevId = null;
      for (int i = 0; i < size; i++) {
	UIComponent child = children.get(i);

	if (id.equals(child.getId())) {
	  if (body != null)
	    addVerbatim(parent, prevId, body);
	  
	  return child;
	}

	if (child.getId() != null)
	  prevId = child.getId();
      }
    }

    return null;
  }
  
  public static UIComponent addVerbatim(UIComponent parent,
					String prevId,
					BodyContent body)
    throws Exception
  {
    String value = body.getString();
    body.clear();

    if (! isWhitespace(value))
      return addVerbatim(parent, prevId, value);
    else
      return null;
  }
  
  public static UIComponent addVerbatim(UIComponent parent,
					BodyContent body)
    throws Exception
  {
    String value = body.getString();
    body.clear();

    if (! isWhitespace(value))
      return addVerbatim(parent, value);
    else
      return null;
  }

  private static boolean isWhitespace(String text)
  {
    if (text == null)
      return true;

    boolean isWhitespace = true;

    for (int i = text.length() - 1; i >= 0; i--) {
      char ch = text.charAt(i);

      if (! Character.isWhitespace(ch)) {
	// check for comment
	if (ch == '>' && text.indexOf("-->") + 2 == i) {
	  int head = text.indexOf("<!--");

	  if (head >= 0) {
	    for (int j = 0; j < head; j++) {
	      if (! Character.isWhitespace(text.charAt(j))) {
		return false;
	      }
	    }

	    return true;
	  }
	}

	return false;
      }
    }

    return true;
  }
}
