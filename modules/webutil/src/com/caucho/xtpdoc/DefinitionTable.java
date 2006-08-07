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

public class DefinitionTable extends Table {
  public DefinitionTable(Document document)
  {
    super(document);
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("table");
    out.writeAttribute("width", "90%");
    out.writeAttribute("cellpadding", "2");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("class", "deftable");
    out.writeAttribute("border", "");

    if (_title != null) {
      out.writeStartElement("caption");
      out.writeCharacters(_title);
      out.writeEndElement();
    }

    for (TableRow row : _rows)
      row.writeHtml(out);

    out.writeEndElement();
  }

  protected void writeRows(PrintWriter out)
    throws IOException
  {
    for (TableRow row : _rows) {
      out.print("\\rowcolor[gray]{0.9}");
      row.writeLaTeX(out);
    }
  }
}
