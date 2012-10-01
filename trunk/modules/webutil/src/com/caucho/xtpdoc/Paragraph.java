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

import com.caucho.config.types.RawString;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class Paragraph extends FormattedTextWithAnchors {

  private static Logger log = Logger.getLogger(Paragraph.class.getName());

  public Paragraph(Document document)
  {
    super(document);
  }

  @Override
  public void addText(RawString text)
  {
    // Cannot use trim because that will break anchor formatting like <a>.
    // addItem(new Text(text.getValue().trim()));
    addItem(new Text(text.getValue()));
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("p");

    super.writeHtml(out);

    out.writeEndElement();
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.println();
    out.println();

    super.writeLaTeX(out);
  }

  @Override
  public void writeAsciiDoc(PrintWriter out)
    throws IOException
  {
    out.println();
    out.println();

    super.writeAsciiDoc(out);
  }


  @Override
  public Example createExample()
  {
    //throw new IllegalStateException("Close block with </p> before <example>");
    log.fine(getLocation() + "Close block with </p> before <example>");
    return super.createExample();
  }
}
