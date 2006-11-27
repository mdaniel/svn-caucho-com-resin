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
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

/**
 * The HTML panel/grid renderer
 */
class HtmlPanelGridRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlPanelGridRenderer();

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

    String bgcolor = null;
    int border = -1;
    String cellpadding = null;
    String cellspacing = null;
    String dir = null;
    String frame = null;
    String lang = null;
    String onclick = null;
    String ondblclick = null;
    String onkeydown = null;
    String onkeypress = null;
    String onkeyup = null;
    String onmousedown = null;
    String onmousemove = null;
    String onmouseout = null;
    String onmouseover = null;
    String onmouseup = null;
    String rules = null;
    String style = null;
    String styleClass = null;
    String summary = null;
    String title = null;
    String width = null;
    
    if (component instanceof HtmlPanelGrid) {
      HtmlPanelGrid html = (HtmlPanelGrid) component;

      bgcolor = html.getBgcolor();
      border = html.getBorder();
      cellpadding = html.getCellpadding();
      cellspacing = html.getCellspacing();
      dir = html.getDir();
      frame = html.getFrame();
      lang = html.getLang();
      onclick = html.getOnclick();
      ondblclick = html.getOndblclick();
      onkeydown = html.getOnkeydown();
      onkeypress = html.getOnkeypress();
      onkeyup = html.getOnkeyup();
      onmousedown = html.getOnmousedown();
      onmousemove = html.getOnmousemove();
      onmouseout = html.getOnmouseout();
      onmouseover = html.getOnmouseover();
      onmouseup = html.getOnmouseup();
      rules = html.getRules();
      style = html.getStyle();
      styleClass = html.getStyleClass();
      summary = html.getSummary();
      title = html.getTitle();
      width = html.getWidth();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      bgcolor = (String) attrMap.get("bgcolor");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
    }

    out.startElement("table", component);

    if (bgcolor != null)
      out.writeAttribute("bgcolor", bgcolor, "bgcolor");

    if (border >= 0)
      out.writeAttribute("border", border, "border");

    if (cellpadding != null)
      out.writeAttribute("cellpadding", cellpadding, "cellpadding");

    if (cellspacing != null)
      out.writeAttribute("cellspacing", cellspacing, "cellspacing");
    
    if (dir != null)
      out.writeAttribute("dir", dir, "dir");
    
    if (frame != null)
      out.writeAttribute("frame", frame, "frame");
    
    if (lang != null)
      out.writeAttribute("lang", lang, "lang");
    
    if (onclick != null)
      out.writeAttribute("onclick", onclick, "onclick");
    
    if (ondblclick != null)
      out.writeAttribute("ondblclick", ondblclick, "ondblclick");
    
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
    
    if (rules != null)
      out.writeAttribute("rules", rules, "rules");
    
    if (style != null)
      out.writeAttribute("style", style, "style");
    
    if (styleClass != null)
      out.writeAttribute("class", styleClass, "styleClass");
    
    if (summary != null)
      out.writeAttribute("summary", summary, "summary");
    
    if (title != null)
      out.writeAttribute("title", title, "title");
    
    if (width != null)
      out.writeAttribute("width", width, "width");
  }


  /**
   * Renders the content for the component.
   */
  @Override
  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter out = context.getResponseWriter();

    int columns = 0;
    
    if (component instanceof HtmlPanelGrid) {
      HtmlPanelGrid html = (HtmlPanelGrid) component;

      columns = html.getColumns();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      columns = (Integer) attrMap.get("columns");
    }

    int size = component.getChildCount();
    if (size == 0)
      return;
    
    if (columns < 1)
      columns = 1;

    List<UIComponent> children = component.getChildren();
    int count = 0;

    for (int i = 0; i < size; i++) {
      UIComponent child = children.get(i);

      if (! child.isRendered())
	continue;

      if (count % columns == 0) {
	if (count > 0) {
	  out.endElement("tr");
	}

	out.startElement("tr", child);
      }

      count++;

      out.startElement("td", child);
      child.encodeBegin(context);
      child.encodeChildren(context);
      child.encodeEnd(context);
      out.endElement("td");
    }

    if (count > 0)
      out.endElement("tr");
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter out = context.getResponseWriter();

    out.endElement("table");
  }

  public String toString()
  {
    return "HtmlPanelGridRenderer[]";
  }
}
