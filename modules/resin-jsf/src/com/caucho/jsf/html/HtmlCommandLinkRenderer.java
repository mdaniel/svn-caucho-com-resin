/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.html;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

/**
 * The HTML command/link renderer
 */
class HtmlCommandLinkRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlCommandLinkRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return true;
  }
  
  /**
   * Renders the open tag for the text.
   */
  @Override
  public void encodeBegin(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter out = context.getResponseWriter();

    String id = component.getId();

    String accesskey;
    String charset;
    String coords;
    String dir;
    boolean disabled;
    String hreflang;
    String lang;
    
    String onblur;
    String onclick;
    String ondblclick;
    String onfocus;
    
    String onkeydown;
    String onkeypress;
    String onkeyup;
    
    String onmousedown;
    String onmousemove;
    String onmouseout;
    String onmouseover;
    String onmouseup;
    
    String rel;
    String rev;
    String shape;
    String style;
    String styleClass;
    String tabindex;
    String target;
    String title;
    String type;
    
    Object value;
    
    if (component instanceof HtmlCommandLink) {
      HtmlCommandLink htmlCommandLink = (HtmlCommandLink) component;

      accesskey = htmlCommandLink.getAccesskey();
      charset = htmlCommandLink.getCharset();
      coords = htmlCommandLink.getCoords();
      dir = htmlCommandLink.getDir();
      disabled = htmlCommandLink.isDisabled();
      hreflang = htmlCommandLink.getHreflang();
      lang = htmlCommandLink.getLang();
      
      onblur = htmlCommandLink.getOnblur();
      onclick = htmlCommandLink.getOnclick();
      ondblclick = htmlCommandLink.getOndblclick();
      onfocus = htmlCommandLink.getOnfocus();
      
      onkeydown = htmlCommandLink.getOnkeydown();
      onkeypress = htmlCommandLink.getOnkeypress();
      onkeyup = htmlCommandLink.getOnkeyup();
      
      onmousedown = htmlCommandLink.getOnmousedown();
      onmousemove = htmlCommandLink.getOnmousemove();
      onmouseout = htmlCommandLink.getOnmouseout();
      onmouseover = htmlCommandLink.getOnmouseover();
      onmouseup = htmlCommandLink.getOnmouseup();
      
      rel = htmlCommandLink.getRel();
      rev = htmlCommandLink.getRev();
      shape = htmlCommandLink.getShape();
      style = htmlCommandLink.getStyle();
      styleClass = htmlCommandLink.getStyleClass();
      tabindex = htmlCommandLink.getTabindex();
      title = htmlCommandLink.getTitle();
      type = htmlCommandLink.getType();

      value = htmlCommandLink.getValue();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      accesskey = (String) attrMap.get("accesskey");
      charset = (String) attrMap.get("charset");
      coords = (String) attrMap.get("coords");
      dir = (String) attrMap.get("dir");
      disabled = (Boolean) attrMap.get("disabled");
      hreflang = (String) attrMap.get("hreflang");
      lang = (String) attrMap.get("lang");
      
      onblur = (String) attrMap.get("onblur");
      onclick = (String) attrMap.get("onclick");
      ondblclick = (String) attrMap.get("ondblclick");
      onfocus = (String) attrMap.get("onfocus");
      
      onkeydown = (String) attrMap.get("onkeydown");
      onkeypress = (String) attrMap.get("onkeypress");
      onkeyup = (String) attrMap.get("onkeyup");
      
      onmousedown = (String) attrMap.get("onmousedown");
      onmousemove = (String) attrMap.get("onmousemove");
      onmouseout = (String) attrMap.get("onmouseout");
      onmouseover = (String) attrMap.get("onmouseover");
      onmouseup = (String) attrMap.get("onmouseup");
      
      rel = (String) attrMap.get("rel");
      rev = (String) attrMap.get("rev");
      shape = (String) attrMap.get("shape");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      tabindex = (String) attrMap.get("tabindex");
      title = (String) attrMap.get("title");
      type = (String) attrMap.get("type");
      
      value = attrMap.get("value");
    }

    String hiddenFieldName = "jsf-link";
    for (UIComponent ptr = component.getParent();
	 ptr != null;
	 ptr = ptr.getParent()) {
      if (ptr instanceof UIForm) {
	hiddenFieldName = (ptr.getClientId(context)
			   + NamingContainer.SEPARATOR_CHAR
			   + "jsf-link");
	break;
      }
    }

    if (disabled)
      out.startElement("span", component);
    else
      out.startElement("a", component);

    out.writeAttribute("href", "#", "href");
      
    //out.writeAttribute("name", component.getClientId(context), "name");
    
    if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      out.writeAttribute("id", component.getClientId(context), "id");

    if (accesskey != null)
      out.writeAttribute("accesskey", accesskey, "accesskey");

    if (charset != null)
      out.writeAttribute("charset", charset, "charset");

    if (coords != null)
      out.writeAttribute("coords", coords, "coords");

    if (dir != null)
      out.writeAttribute("dir", dir, "dir");

    /*
    if (disabled)
      out.writeAttribute("disabled", "disabled", "disabled");
    */

    if (hreflang != null)
      out.writeAttribute("hreflang", hreflang, "hreflang");

    if (lang != null)
      out.writeAttribute("lang", lang, "lang");

    if (onblur != null)
      out.writeAttribute("onblur", onblur, "onblur");

    String clientId = component.getClientId(context);

    StringBuilder clickJs = new StringBuilder();
    clickJs.append("document.forms['");
    clickJs.append(clientId);
    clickJs.append("']['");
    clickJs.append(hiddenFieldName);
    clickJs.append("'].value='");
    clickJs.append(clientId);
    clickJs.append("';");

    // uiparam

    clickJs.append("return false;");

    if (disabled) {
    }
    else {
      if (onclick != null) {
	String code = ("var a = function(){" + onclick + "};"
		       + "var b = function() {" + clickJs + "};"
		       + "return a() && b();");
      
	out.writeAttribute("onclick", code, "onclick");
      }
      else
	out.writeAttribute("onclick", clickJs.toString(), "onclick");
    }

    if (ondblclick != null)
      out.writeAttribute("ondblclick", ondblclick, "ondblclick");

    if (onfocus != null)
      out.writeAttribute("onfocus", onfocus, "onfocus");

    if (onkeydown != null)
      out.writeAttribute("onkeydown", onkeydown, "onkeydown");

    if (onkeypress != null)
      out.writeAttribute("onkeypress", onkeypress, "onkeypress");

    if (onkeyup != null)
      out.writeAttribute("onkeyup", onkeyup, "onkeyup");

    if (onmousedown != null)
      out.writeAttribute("onmousedown", onmousedown, "onmousedown");

    if (onmousemove != null)
      out.writeAttribute("onmousemove", onmousemove, "onmousemove");

    if (onmouseout != null)
      out.writeAttribute("onmouseout", onmouseout, "onmouseout");

    if (onmouseover != null)
      out.writeAttribute("onmouseover", onmouseover, "onmouseover");

    if (onmouseup != null)
      out.writeAttribute("onmouseup", onmouseup, "onmouseup");

    if (rel != null)
      out.writeAttribute("rel", rel, "rel");

    if (rev != null)
      out.writeAttribute("rev", rev, "rev");

    if (shape != null)
      out.writeAttribute("shape", shape, "shape");

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (tabindex != null)
      out.writeAttribute("tabindex", tabindex, "tabindex");

    if (title != null)
      out.writeAttribute("title", title, "title");

    if (type != null)
      out.writeAttribute("type", type, "type");

    if (disabled) {
      if (value != null)
	out.writeAttribute("value", String.valueOf(value), "value");

      out.endElement("span");
      return;
    }
    int childCount = component.getChildCount();

    if (childCount > 0) {
      List<UIComponent> children = component.getChildren();

      for (int i = 0; i < childCount; i++) {
	UIComponent child = children.get(i);

	if (child instanceof UIParameter)
	  continue;
      
	if (child.isRendered()) {
	  child.encodeBegin(context);
	  child.encodeChildren(context);
	  child.encodeEnd(context);
	}
      }
    }

    out.endElement("a");
  }

  /**
   * Renders the content for the component.
   */
  @Override
  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  public String toString()
  {
    return "HtmlInputTextRenderer[]";
  }
}
