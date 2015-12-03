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

public class ListItem extends FormattedTextWithAnchors {
  public ListItem(Document document)
  {
    super(document);
  }

  public Paragraph createP()
  {
    Paragraph paragraph = new Paragraph(getDocument());
    addItem(paragraph);
    return paragraph;
  }

  public DefinitionTable createDeftable()
  {
    DefinitionTable deftable = new DefinitionTable(getDocument());
    addItem(deftable);
    return deftable;
  }

  public Def createDef()
  {
    Def def = new Def(getDocument());
    addItem(def);
    return def;
  }

  public Example createExample()
  {
    Example example = new Example(getDocument());
    addItem(example);
    return example;
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

  public Image createImg()
  {
    Image image = new Image(getDocument());
    addItem(image);
    return image;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("li");

    super.writeHtml(out);

    out.writeEndElement(); // li
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("\\item ");

    super.writeLaTeX(out);

    out.println();
  }
}
