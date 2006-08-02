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

import com.caucho.vfs.Path;

public class Body implements ObjectWithParent {
  private Object _parent;
  private boolean _topLevel;
  private Summary _summary;
  private ArrayList<Section> _sections = new ArrayList<Section>();

  void setDocumentPath(Path documentPath, boolean topLevel)
  {
    _topLevel = topLevel;

    if (topLevel && documentPath != null)
      _summary.setRootPath(documentPath.getParent());

    if (documentPath != null) {
      String documentName = documentPath.getTail();

      for (Section section : _sections)
        section.setDocumentName(documentName);
    }
  }

  public void setParent(Object parent)
  {
    _parent = parent;
  }

  public Object getParent()
  {
    return _parent;
  }

  public void setSummary(Summary summary)
  {
    _summary = summary;
  }

  public void addFaq(Faq faq)
  {
    _sections.add(faq);
  }

  public void addS1(S1 section)
  {
    _sections.add(section);
  }

  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    writer.println("<body>");

    _summary.writeHtml(writer);

    for (Section section : _sections)
      section.writeHtml(writer);

    writer.println("</body>");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    if (_topLevel) {
      writer.println("\\begin{document}");

      for (Section section : _sections)
        section.writeLaTeX(writer);

      writer.println("\\end{document}");
    } else {
      for (Section section : _sections)
        section.writeLaTeX(writer);
    }
  }
}
