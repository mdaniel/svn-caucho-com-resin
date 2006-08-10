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

import java.util.logging.Logger;

import com.caucho.vfs.Path;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

public class Document {
  private static Logger log = Logger.getLogger(ResinDocServlet.class.getName());

  private Header _header;
  private Body _body;
  private Path _documentPath;
  private String _contextPath;
  private int _level;

  Document()
  {
    this(null, null);
  }

  public Document(Path documentPath, String contextPath)
  {
    _documentPath = documentPath;
    _contextPath = contextPath;
  }

  public Path getDocumentPath()
  {
    return _documentPath;
  }

  public String getContextPath()
  {
    return _contextPath;
  }

  public Header getHeader()
  {
    return _header;
  }

  public String getName()
  {
    // XXX
    return "";
  }

  public Header createHeader()
  {
    _header = new Header(this);
    return _header;
  }

  public Body createBody()
  {
    _body = new Body(this);
    return _body;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartDocument("UTF-8", "1.0");
    out.writeStartElement("html");

    _header.writeHtml(out);
    _body.writeHtml(out);

    out.writeEndElement();
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\documentclass{article}");

    _header.writeLaTeXTop(out);
    _body.writeLaTeXTop(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    _header.writeLaTeX(out);
    _body.writeLaTeX(out);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    _header.writeLaTeXEnclosed(out);
    _body.writeLaTeXEnclosed(out);
  }

  public String toString()
  {
    return "Document[" + _documentPath + "]";
  }
}
