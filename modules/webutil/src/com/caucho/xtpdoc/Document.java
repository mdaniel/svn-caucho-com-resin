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

import com.caucho.vfs.Path;

public class Document {
  private Header _header;
  private Body _body;
  private Path _documentPath;
  private String _contextPath;
  private boolean _topLevel = true;
  private int _level;

  Document()
  {
    this(null, null, false);
  }

  public Document(Path documentPath, String contextPath)
  {
    this(documentPath, contextPath, true);
  }

  public Document(Path documentPath, String contextPath, boolean topLevel)
  {
    _documentPath = documentPath;
    _contextPath = contextPath;
    _topLevel = topLevel;
  }

  public Header getHeader()
  {
    return _header;
  }

  public void setHeader(Header header)
  {
    _header = header;

    _header.setContextPath(_contextPath);
    _header.setTopLevel(_topLevel);
    if (_documentPath != null)
      _header.setDocumentName(_documentPath.getTail());
  }

  public void setBody(Body body)
  {
    _body = body;

    _body.setDocumentPath(_documentPath, _topLevel);
  }

  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    writer.println("<html>");

    _header.writeHtml(writer);
    _body.writeHtml(writer);

    writer.println("</html>");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    if (_topLevel)
      writer.println("\\documentclass{article}");

    _header.writeLaTeX(writer);
    _body.writeLaTeX(writer);
  }

  public String toString()
  {
    return "Document[" + _documentPath + "]";
  }
}
