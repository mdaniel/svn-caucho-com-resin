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

import java.util.logging.Logger;

import java.net.*;

import java.io.PrintWriter;
import java.io.IOException;

public class Anchor extends FormattedText {
  private static final Logger log = Logger.getLogger(Anchor.class.getName());

  private String _documentName;
  private String _configTag;
  private String _href;

  private String getDocumentName()
  {
    if (_documentName != null)
      return _documentName;

    Object o = getParent();

    while (o != null) {
      if (o instanceof Section) {
        _documentName = ((Section) o).getDocumentName();

        return _documentName;
      } else if (o instanceof ObjectWithParent) {
        o = ((ObjectWithParent) o).getParent();
      } else {
        break;
      }
    }

    return _documentName;
  }

  public void setConfigTag(String configTag)
  {
    _configTag = configTag;
  }

  public void setHref(String href)
  {
    _href = href;
  }

  public void writeHtml(PrintWriter out)
    throws IOException
  {
    if (_href.startsWith("javadoc|")) {
      String name = _href.substring("javadoc|".length());

      name = name.replace('.', '/') + ".html";
      
      out.print("<a href='http://www.caucho.com/resin-javadoc/" + name + "'>");
    }
    else
      out.print("<a href='" + _href + "'>");

    super.writeHtml(out);

    out.print("</a>");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    try {
      URI uri = new URI(_href);

      if (uri.getScheme() != null) {
        writer.print("\\href{" + _href + "}");

        writer.print("{");

        super.writeLaTeX(writer);

        writer.print("}");

        return;
      } else if (uri.getPath() != null && uri.getPath().length() != 0) {
        writer.print("\\hyperlink{" + uri.getPath());

        if (uri.getFragment() != null)
          writer.print(":" + uri.getFragment());

        writer.print("}{");
        super.writeLaTeX(writer);
        writer.print("}");

        return;
      } else if (uri.getFragment() != null && uri.getFragment().length() != 0) {
        String documentName = getDocumentName();

        if (documentName != null) {
          writer.print("\\hyperlink{" + documentName + ":" + uri.getFragment());
          writer.print("}{");
          super.writeLaTeX(writer);
          writer.print("}");

          return;
        }
      }

    } catch (Exception e) {
    }

    writer.print("\\href{" + _href + "}");

    writer.print("{");
    super.writeLaTeX(writer);
    writer.print("}");
  }
}
