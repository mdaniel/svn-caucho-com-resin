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

public class Example extends VerboseFormattedTextWithAnchors {
  private String _title;
  private String _language = null;

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setLanguage(String language)
  {
    _language = language;
  }

  public void writeHtml(PrintWriter out)
    throws IOException
  {
    if (_title != null) {
      out.println("<center><b>" + _title + "</b></center>");
    }
    
    out.println("<div class='example'><pre>");

    super.writeHtml(out);

    out.println("</pre></div>");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    if (_language != null) {
      writer.println("\\lstset{fancyvrb,language=" + _language + ",");
      writer.println("         showstringspaces=false,basicstyle=\\small,");
      writer.println("         stringstyle=\\color[gray]{0.6}}");
    }

    writer.println("\\begin{center}");
    writer.println("\\begin{Verbatim}[frame=single,fontfamily=courier,");
    writer.println("                  framerule=1pt,");
    writer.println("                  fontsize=\\footnotesize,");

    if (_title != null) {
      writer.print("                  labelposition=bottomline,label=\\fbox{");
      writer.println(LaTeXUtil.escapeForLaTeX(_title) + "},");
    }

    writer.println("                  samepage=true]");

    super.writeLaTeX(writer);

    // make room for the title box
    if (_title != null)
      writer.println();

    writer.println();
    writer.println("\\end{Verbatim}");

    writer.println("\\end{center}");

    if (_language != null)
      writer.println("\\lstset{fancyvrb=false}");
  }
}
