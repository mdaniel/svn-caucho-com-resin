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

public class Def extends VerboseFormattedText {
  private String _title;

  public void setTitle(String title)
  {
    _title = title;
  }

  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    writer.println("<table class=\"egpad\" cellspacing=\"0\" width=\"90%\">");

    if (_title != null) {
      writer.println("<caption><font size=\"+1\">" + _title + 
                     "</font></caption>");
    }

    writer.println("<tr><td class=\"def\" bgcolor=\"#cccccc\">");
    writer.println("<pre><div class=\"def\">");

    super.writeHtml(writer);

    writer.println("</pre></td></tr></table>");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    writer.println("\\begin{center}");
    writer.println("\\begin{Verbatim}[fontfamily=courier,");
    writer.println("                  fontsize=\\footnotesize,");

    if (_title != null) {
      writer.println("                  label=" + _title + ",");
      writer.println("                  labelposition=bottomline,");
    }

    writer.println("                  samepage=true]");

    super.writeLaTeX(writer);

    writer.println();
    writer.println("\\end{Verbatim}");
    writer.println("\\end{center}");
  }
}
