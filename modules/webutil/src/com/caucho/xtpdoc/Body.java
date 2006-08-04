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

import com.caucho.config.*;
import com.caucho.vfs.Path;

public class Body implements ObjectWithParent {
  private Object _parent;
  private boolean _topLevel;
  private Path _rootPath;
  private Navigation _navigation;
  private Summary _summary;
  private ArrayList<Section> _sections = new ArrayList<Section>();

  void setDocumentPath(Path documentPath, boolean topLevel)
  {
    _topLevel = topLevel;

    if (topLevel && documentPath != null) {
      if (_summary != null)
	_summary.setRootPath(documentPath.getParent());
      _rootPath = documentPath.getParent();

      parseNavigation(_rootPath);
    }

    if (documentPath != null) {
      String documentName = documentPath.getTail();

      for (Section section : _sections)
        section.setDocumentName(documentName);
    }
  }

  private void parseNavigation(Path rootPath)
  {
    // We can parse the table of contents now because the rest of the
    // attributes are just formatting
    Path toc = rootPath.lookup("toc.xml");

    if (toc.exists()) {
      Config config = new Config();

      _navigation = new Navigation(rootPath, 0);

      try {
        config.configure(_navigation, toc);
      } catch (Exception e) {
        e.getCause().printStackTrace();
        _navigation = null;
      }
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

  public void setLocaltoc(Localtoc localtoc)
  {
  }

  public void addFaq(Faq faq)
  {
    _sections.add(faq);
  }

  public void addS1(S1 section)
  {
    _sections.add(section);
  }

  public void writeHtml(PrintWriter out)
    throws IOException
  {
    out.println("<body bgcolor='white' leftmargin='0'>");

    out.println("<table width='100%' border='0' cellspacing='0'>");

    out.println("<tr valign='top'><td bgcolor='#b9cef7' class='leftnav'>");
    
    if (_navigation != null) {
      _navigation.writeLeftNav(out);
    }
    out.println("</td><td>");

    if (_summary != null)
      _summary.writeHtml(out);

    for (Section section : _sections)
      section.writeHtml(out);
    
    out.println("</td></tr></table>");

    out.println("</body>");
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
