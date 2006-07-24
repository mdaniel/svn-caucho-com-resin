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

public class Section implements ContentItem {
  private int _depth = 0;
  private String _name;
  private String _title;

  private ArrayList<ContentItem> _contentItems = new ArrayList<ContentItem>();

  private void setDepth(int depth)
  {
    _depth = depth;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void addP(Paragraph paragraph)
  {
    _contentItems.add(paragraph);
  }

  public void addOL(OrderedList orderedList)
  {
    _contentItems.add(orderedList);
  }

  public void addUL(UnorderedList unorderedList)
  {
    _contentItems.add(unorderedList);
  }

  public void addFigure(Figure figure)
  {
    _contentItems.add(figure);
  }

  public void addExample(Example example)
  {
    _contentItems.add(example);
  }

  public void addTable(Table table)
  {
    _contentItems.add(table);
  }
  
  public void addSection(Section subsection)
  {
    subsection.setDepth(_depth + 1);

    _contentItems.add(subsection);
  }

  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    writer.println("<div class='section'>" + _title + "</div>");

    for (ContentItem item : _contentItems) {
      item.writeHtml(writer);
    }
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    writer.println("\\section{" + _title + "}");

    for (ContentItem item : _contentItems) {
      item.writeLaTeX(writer);
    }
  }
}
