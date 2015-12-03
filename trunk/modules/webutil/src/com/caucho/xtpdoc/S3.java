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

public class S3 extends Section {
  public S3(Document document, String parentHref)
  {
    super(document, parentHref);
  }

  public S4 createS4()
  {
    S4 s4 = new S4(getDocument(), getHref());
    addItem(s4);
    return s4;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("div");
    out.writeAttribute("class", "s3");
    
    out.writeStartElement("a");
    out.writeAttribute("name", getHref());
    out.writeEndElement();

    if (_title != null) {
      out.writeStartElement("h3");
      out.writeCharacters(_title);
      out.writeEndElement();
    }
    
    for (ContentItem item : getItems())
      item.writeHtml(out);
    
    out.writeEndElement();
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");

    if (_title != null) 
      out.println("\\subsubsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    super.writeLaTeX(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");

    if (_title != null) {
      if (isArticle())
        out.println("\\subsubsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");
      else
        out.println("\\paragraph{" + LaTeXUtil.escapeForLaTeX(_title) + "}");
    }

    super.writeLaTeX(out);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");

    if (_title != null) 
      out.println("\\textbf{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    super.writeLaTeX(out);
  }

  @Override
  public void writeAsciiDoc(PrintWriter out)
    throws IOException
  {
    if (isWebOnly())
      return;
    
    out.println();
    out.println();
    out.println("===== " + _title + " =====");
        
    super.writeAsciiDoc(out);
  }
}
