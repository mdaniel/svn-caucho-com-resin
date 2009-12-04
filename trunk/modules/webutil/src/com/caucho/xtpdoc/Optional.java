/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

public class Optional extends FormattedText {
  private String _id;
  private String _title;

  public Optional(Document document)
  {
    super(document);
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public Paragraph createP()
  {
    Paragraph paragraph = new Paragraph(getDocument());
    addItem(paragraph);
    return paragraph;
  }

  public OrderedList createOl()
  {
    OrderedList orderedList = new OrderedList(getDocument());
    addItem(orderedList);
    return orderedList;
  }

  public UnorderedList createUl()
  {
    UnorderedList unorderedList = new UnorderedList(getDocument());
    addItem(unorderedList);
    return unorderedList;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    String id = _id;

    if (id == null) 
      id = _title.toLowerCase().replace(" ", "_").replace("?", "p");

    out.writeStartElement("a");
    out.writeAttribute("class", "showOptional");
    out.writeAttribute("id", "s" + id);
    out.writeAttribute("href", "javascript:show('h" + id + "');" +
                                          "show('"  + id + "');" +
                                          "hide('s" + id + "');");
    out.writeCharacters(_title);
    out.writeEntityRef("nbsp");
    out.writeEntityRef("rarr");
    out.writeEndElement(); // a

    out.writeStartElement("a");
    out.writeAttribute("class", "hideOptional");
    out.writeAttribute("id", "h" + id);
    out.writeAttribute("href", "javascript:hide('h" + id + "');" +
                                          "hide('"  + id + "');" +
                                          "show('s" + id + "');");
    out.writeAttribute("style", "display: none");
    out.writeCharacters(_title);
    out.writeEntityRef("nbsp");
    out.writeEntityRef("darr");
    out.writeEndElement(); // a

    out.writeEmptyElement("br");

    out.writeStartElement("div");
    out.writeAttribute("id", id);
    out.writeAttribute("class", "optional");
    out.writeAttribute("style", "display: none");

    super.writeHtml(out);

    out.writeEndElement(); // div

    out.writeEmptyElement("br");
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("\\textbf{" + _title + "}\\\\");
    super.writeLaTeX(out);
  }
}
