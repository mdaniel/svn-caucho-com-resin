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

public class TableData extends FormattedTextWithAnchors implements TableCell {
  private String _rowspan;
  private String _colspan;
  private String _width;
  private String _id;
  private String _scope;

  public TableData(Document document)
  {
    super(document);
  }

  public void setRowspan(String rowspan)
  {
    _rowspan = rowspan;
  }

  public void setColspan(String colspan)
  {
    _colspan = colspan;
  }

  public void setWidth(String width)
  {
    _width = width;
  }
  
  public void setId(String id)
  {
    _id = id;
  }
  
  public void setScope(String scope)
  {
    _scope = scope;
  }

  public Def createDef()
  {
    Def def = new Def(getDocument());
    addItem(def);
    return def;
  }

  @Override
  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("td");

    if (_width != null)
      out.writeAttribute("width", _width);

    if (_colspan != null)
      out.writeAttribute("colspan", _colspan);

    if (_rowspan != null)
      out.writeAttribute("rowspan", _rowspan);

    if (_scope != null)
      out.writeAttribute("scope", _scope);

    if (_id != null)
      out.writeAttribute("id", _id);

    super.writeHtml(out);

    out.writeEndElement(); // td
  }
}
