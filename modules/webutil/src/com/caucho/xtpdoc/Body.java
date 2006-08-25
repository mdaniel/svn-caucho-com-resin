/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.ArrayList;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import com.caucho.config.*;
import com.caucho.vfs.Path;

public class Body extends ContainerNode {
  private static Logger log = Logger.getLogger(Body.class.getName());

  private Summary _summary;
  private Navigation _navigation;
  private Index _index;

  public Body(Document document)
  {
    super(document);
  }

  private void parseNavigation()
  {
    NavigationItem item = getDocument().getNavigation();

    if (item != null)
      _navigation = item.getNavigation();
  }

  public void setLocalTOC(String text)
  {
  }

  public Summary createSummary()
  {
    _summary = new Summary(getDocument());
    return _summary;
  }

  public void setLocaltoc(Localtoc localtoc)
  {
  }

  public Faq createFaq()
  {
    Faq faq = new Faq(getDocument());
    addItem(faq);
    return faq;
  }

  public S1 createS1()
  {
    S1 s1 = new S1(getDocument());
    addItem(s1);
    return s1;
  }
  
  public Defun createDefun()
  {
    Defun defun = new Defun(getDocument());
    addItem(defun);
    return defun;
  }

  public Index createIxx()
  {
    _index = new Index(getDocument());
    return _index;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("body");
    out.writeAttribute("bgcolor", "white");
    out.writeAttribute("leftmargin", "0");

    out.writeStartElement("table");
    out.writeAttribute("width", "100%");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");

    // logo block
    out.writeStartElement("tr");

    // spacer
    out.writeStartElement("td");
    out.writeAttribute("width", "2");
    out.writeStartElement("img");
    out.writeAttribute("alt", "");
    out.writeAttribute("width", "2");
    out.writeAttribute("height", "1");
    out.writeAttribute("src", getDocument().getContextPath() + "/images/pixel.gif");
    out.writeEndElement(); // </img>
    out.writeEndElement(); // </td>

    // logo
    out.writeStartElement("td");
    out.writeAttribute("width", "150");
    out.writeStartElement("img");
    out.writeAttribute("alt", "");
    out.writeAttribute("width", "150");
    out.writeAttribute("height", "63");
    out.writeAttribute("src", getDocument().getContextPath() + "/images/caucho-white.jpg");
    out.writeEndElement(); // </img>
    out.writeEndElement(); // </td>

    // spacer
    out.writeStartElement("td");
    out.writeAttribute("width", "10");
    out.writeStartElement("img");
    out.writeAttribute("alt", "");
    out.writeAttribute("width", "10");
    out.writeAttribute("height", "1");
    out.writeAttribute("src", getDocument().getContextPath() + "/images/pixel.gif");
    out.writeEndElement(); // </img>
    out.writeEndElement(); // </td>

    // top label
    out.writeStartElement("td");
    out.writeStartElement("table");
    out.writeAttribute("width", "100%");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("cellpadding", "0");
    out.writeAttribute("border", "0");
    out.writeStartElement("tr");
    out.writeAttribute("class", "toptitle");
    out.writeStartElement("td");
    //out.writeAttribute("rowspan", "2");
    out.writeAttribute("width", "90%");
    out.writeAttribute("background", getDocument().getContextPath() + "/images/hbleed.gif");
    out.writeStartElement("font");
    out.writeAttribute("class", "toptitle");
    out.writeAttribute("size", "+3");
    out.writeEntityRef("nbsp");
    out.writeCharacters(getDocument().getHeader().getTitle());
    out.writeEndElement(); // </font>
    out.writeEndElement(); // </td>
    out.writeEndElement(); // </tr>
    out.writeEndElement(); // </table>
    out.writeEndElement(); // </td>
    
    out.writeEndElement(); // </tr>

    // XXX: space

    // navigation
    out.writeStartElement("tr");
    out.writeAttribute("valign", "top");
    out.writeStartElement("td");
    out.writeAttribute("colspan", "2");
    out.writeAttribute("bgcolor", "#b9cef7");
    out.writeAttribute("class", "leftnav");
    
    parseNavigation();

    getDocument().writeLeftNav(out);

    out.writeEndElement(); // td

    out.writeStartElement("td"); //spacer
    out.writeEndElement();
    
    out.writeStartElement("td");

    if (_summary != null) {
      _summary.setNavigation(_navigation);
      _summary.writeHtml(out);
    }

    if (_index != null)
      _index.writeHtml(out);

    super.writeHtml(out);

    out.writeStartElement("hr");
    out.writeEndElement();

    // nav

    out.writeStartElement("table");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("width", "100%");
    out.writeStartElement("tr");
    out.writeStartElement("td");
    out.writeStartElement("em");
    out.writeStartElement("small");
    out.writeCharacters("Copyright ");
    out.writeEntityRef("copy");
    out.writeCharacters(" 1998-2006 Caucho Technology, Inc. All rights reserved.");
    out.writeStartElement("br");
    out.writeEndElement();
    out.writeCharacters("Resin is a registered trademark");
    out.writeEndElement(); // small
    out.writeEndElement(); // em
    out.writeEndElement(); // td
    out.writeEndElement(); // tr
    out.writeEndElement(); // table
    
    out.writeEndElement(); // td
    out.writeEndElement(); // tr
    out.writeEndElement(); // table

    out.writeEndElement(); //body
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\begin{document}");

    super.writeLaTeXTop(out);

    out.println("\\end{document}");
  }
}
