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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class Body extends ContainerNode {
  private static Logger log = Logger.getLogger(Body.class.getName());

  private Summary _summary;
  private Navigation _navigation;
  private Index _index;
  private String _class;

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

  /*
  public void setLocalTOC(String text)
  {
  }
  */

  public void setClass(String styleClass)
  {
    _class = styleClass;
  }

  public Summary createSummary()
  {
    _summary = new Summary(getDocument());

    addItem(_summary);
    
    return _summary;
  }

  public Localtoc createLocaltoc()
  {
    Localtoc toc = new Localtoc(getDocument());

    addItem(toc);

    return toc;
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

    if (_class != null)
      out.writeAttribute("class", _class);
    
    out.writeAttribute("bgcolor", "white");
    out.writeAttribute("leftmargin", "0");

    out.writeStartElement("table");
    out.writeAttribute("width", "100%");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("cellpadding", "0");
    out.writeAttribute("summary", "");

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
    if (getDocument().getHeader() != null)
      out.writeCharacters(getDocument().getHeader().getTitle());
    out.writeEndElement(); // </font>
    out.writeEndElement(); // </td>
    out.writeEndElement(); // </tr>
    out.writeEndElement(); // </table>
    out.writeEndElement(); // </td>
    
    out.writeEndElement(); // </tr>

    // space
    out.writeStartElement("tr");
    out.writeStartElement("td");
    out.writeAttribute("colspan", "4");
    
    out.writeStartElement("img");
    out.writeAttribute("alt", "");
    out.writeAttribute("width", "1");
    out.writeAttribute("height", "5");
    out.writeAttribute("src", getDocument().getContextPath() + "/images/pixel.gif");
    out.writeEndElement(); // </img>
    
    out.writeEndElement(); // </td>
    out.writeEndElement(); // </tr>

    String cp = getDocument().getContextPath();

    // left
    out.writeStartElement("tr");
    out.writeStartElement("td");
    //out.writeAttribute("bgcolor", "#b9cef7");
    //out.writeAttribute("class", "section");
    out.writeAttribute("colspan", "2");
    out.writeAttribute("background", cp + "/images/left_background.gif");
    
    out.writeStartElement("img");
    out.writeAttribute("alt", "");
    out.writeAttribute("width", "1");
    out.writeAttribute("height", "20");
    out.writeAttribute("src", cp + "/images/pixel.gif");
    out.writeEndElement(); // </img>
    
    out.writeEndElement(); // </td>
    
    out.writeStartElement("td");
    out.writeAttribute("colspan", "2");
    out.writeStartElement("img");
    out.writeAttribute("alt", "");
    out.writeAttribute("width", "1");
    out.writeAttribute("height", "20");
    out.writeAttribute("src", cp + "/images/pixel.gif");
    out.writeEndElement(); // </td>
    out.writeEndElement(); // </tr>

    // navigation
    out.writeStartElement("tr");
    out.writeAttribute("valign", "top");
    out.writeStartElement("td");
    //out.writeAttribute("bgcolor", "#b9cef7");
    out.writeAttribute("class", "leftnav");
    out.writeEndElement();
    
    out.writeStartElement("td");
    // out.writeAttribute("bgcolor", "#b9cef7");
    out.writeAttribute("class", "leftnav");
    
    parseNavigation();

    getDocument().writeLeftNav(out);

    out.writeEndElement(); // td

    out.writeStartElement("td"); //spacer
    out.writeEndElement();
    
    out.writeStartElement("td");
    // actual body

    NavigationItem item = getDocument().getNavigation();

    if (item != null) {
      writeThreadNavigation(out, item);
      out.writeEmptyElement("hr");
    }

    Header header = getDocument().getHeader();
      
    if (header != null && header.getDescription() != null) {
      header.getDescription().writeHtml(out);
    }

    if (header != null && header.getTutorialStartPage() != null) {
      out.writeStartElement("p");
      out.writeStartElement("a");
      out.writeAttribute("href", header.getTutorialStartPage());
      out.writeCharacters("Demo");
      out.writeEndElement();
      out.writeEndElement();
    }

    if (_index != null)
      _index.writeHtml(out);

    super.writeHtml(out);

    if (header != null && header.getTutorialStartPage() != null) {
      out.writeStartElement("p");
      out.writeStartElement("a");
      out.writeAttribute("href", header.getTutorialStartPage());
      out.writeCharacters("Demo");
      out.writeEndElement();
      out.writeEndElement();
    }

    out.writeStartElement("hr");
    out.writeEndElement();

    if (item != null) {
      writeThreadNavigation(out, item);
    }
    
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
    out.writeEmptyElement("br");
    out.writeCharacters("Resin ");
    out.writeStartElement("sup");
    out.writeStartElement("font");
    out.writeAttribute("size", "-1");
    out.writeEntityRef("#174");
    out.writeEndElement();
    out.writeEndElement();
    out.writeCharacters(" is a registered trademark, and Quercus");
    out.writeStartElement("sup");
    out.writeCharacters("tm");
    out.writeEndElement();
    out.writeCharacters(", Amber");
    out.writeStartElement("sup");
    out.writeCharacters("tm");
    out.writeEndElement();
    out.writeCharacters(", and Hessian");
    out.writeStartElement("sup");
    out.writeCharacters("tm");
    out.writeEndElement();
    out.writeCharacters(" are trademarks of Caucho Technology.");
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
  
  public void writeThreadNavigation(XMLStreamWriter out, NavigationItem item)
    throws XMLStreamException
  {
    out.writeStartElement("table");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("width", "100%");
    out.writeStartElement("tr");
      
    out.writeStartElement("td");
    out.writeAttribute("width", "30%");
    out.writeAttribute("align", "left");
    if (item.getPrevious() != null) {
      item.getPrevious().writeLink(out);
    }
    out.writeEndElement();
      
    out.writeStartElement("td");
    out.writeAttribute("width", "40%");
    out.writeStartElement("center");
    if (item.getParent() != null) {
      item.getParent().writeLink(out);
    }
    out.writeEndElement();
    out.writeEndElement();
      
    out.writeStartElement("td");
    out.writeAttribute("width", "30%");
    out.writeAttribute("align", "right");
    if (item.getNext() != null) {
      item.getNext().writeLink(out);
    }
    out.writeEndElement();
      
    out.writeEndElement();
    out.writeEndElement();
  }
      

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\begin{document}");

    super.writeLaTeXTop(out);

    out.println("\\end{document}");
  }
}
