/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.util.HashSet;
import java.util.Iterator;

public class DefunParents extends ContainerNode implements Iterable<String> {
  private static final Text COMMA = new Text(",");

  private final HashSet<String> _parents = new HashSet<String>();

  public DefunParents(Document document)
  {
    super(document);
  }

  public void setText(String text)
  {
    String []parents = text.split("[ ,]+");

    for (int i = 0; i < parents.length; i++) {
      String parent = parents[i];

      _parents.add(parent);

      Anchor anchor = new Anchor(getDocument());
      anchor.setConfigTag(parent);

      addItem(anchor);

      if (i < parents.length - 1)
        anchor.addItem(COMMA);
    }
  }

  public String getCssClass()
  {
    return "reference-parents";
  }

  public Iterator<String> iterator()
  {
    return _parents.iterator();
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("div");
    out.writeAttribute("class", getCssClass());

    out.writeCharacters("child of ");

    super.writeHtml(out);

    out.writeEndElement(); // div
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("child of ");

    super.writeLaTeX(out);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }
}
