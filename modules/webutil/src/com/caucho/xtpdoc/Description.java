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

public class Description implements ContentItem {
  private Document _document;
  protected ArrayList<ContentItem> _contentItems = new ArrayList<ContentItem>();

  public Description(Document document)
  {
    _document = document;
  }

  public Paragraph createP()
  {
    Paragraph paragraph = new Paragraph(_document);
    _contentItems.add(paragraph);
    return paragraph;
  }

  public Figure createFigure()
  {
    Figure figure = new Figure(_document);
    _contentItems.add(figure);
    return figure;
  }

  public Glossary createGlossary()
  {
    Glossary glossary = new Glossary(_document);
    _contentItems.add(glossary);
    return glossary;
  }
  
  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    for (ContentItem item : _contentItems)
      item.writeHtml(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    for (ContentItem item : _contentItems)
      item.writeLaTeX(out);
  }
}
