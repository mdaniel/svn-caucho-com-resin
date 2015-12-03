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

import com.caucho.config.ConfigException;

public class ViewFileLink implements ContentItem {
  private String _file;
  private Document _document;

  public ViewFileLink(Document document)
  {
    _document = document;
  }

  public void setFile(String file)
  {
    _file = file;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    if (! _document.isDisableAction()) {
      String cp = _document.getContextPath();
    
      out.writeStartElement("a");
      out.writeAttribute("href", "viewfile?file=" + _file);
      out.writeCharacters(_file);
      out.writeEndElement(); // a
    }
    else {
      out.writeCharacters(_file);
    }
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("\\href{" + _file + "}{" + _file + "}");
  }

  public void writeLaTeXVerbatim(PrintWriter out)
    throws IOException
  {
    throw new ConfigException("<viewfile-link> not allowed in a verbatim context");
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  @Override
  public void writeAsciiDoc(PrintWriter out)
    throws IOException
  {
  }
}
