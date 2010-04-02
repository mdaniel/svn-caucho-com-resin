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

public class DefunParents implements ContentItem {
  private final Document _document;
  private String _text;
  private String []_parents;

  public DefunParents(Document document)
  {
    _document = document;
  }

  public void setText(String text)
  {
    _text = text;
    _parents = text.split("[ ,]+");
  }

  public String getCssClass()
  {
    return "reference-parents";
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("div");
    out.writeAttribute("class", getCssClass());

    out.writeCharacters("child of ");

    ReferenceDocument referenceDocument = _document.getReferenceDocument();

    for (int i = 0; i < _parents.length; i++) {
      String parent = _parents[i];

      if (referenceDocument != null) {
        out.writeStartElement("a");
        out.writeAttribute("href", referenceDocument.getURI() + '#' + parent);
      }

      out.writeCharacters(parent);

      if (referenceDocument != null)
        out.writeEndElement(); // a

      if (i < _parents.length - 1)
        out.writeCharacters(",");
    }

    out.writeEndElement(); // div
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("child of " + _text);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }
}
