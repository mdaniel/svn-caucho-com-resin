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

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

public class Defun extends Section {
  public Defun(Document document)
  {
    super(document);
  }

  public S2 createS2()
  {
    S2 s2 = new S2(getDocument());
    addItem(s2);
    return s2;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("table");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellpadding", "5");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("width", "100%");

    out.writeStartElement("tr");
    out.writeAttribute("class", "section");
    out.writeStartElement("td");
    out.writeStartElement("font");
    out.writeAttribute("size", "+2");
    out.writeCharacters(_title);
    out.writeEndElement(); // font
    out.writeEndElement(); // td
    out.writeEndElement(); // tr

    out.writeEndElement(); // table
    
    out.writeStartElement("div");
    out.writeAttribute("class", "desc");
    
    super.writeHtml(out);
    
    out.writeEndElement(); // div
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\subsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    super.writeLaTeX(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.println("\\subsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    super.writeLaTeX(out);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");

    if (_title != null)
      out.println("\\subsubsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    super.writeLaTeX(out);
  }
}
