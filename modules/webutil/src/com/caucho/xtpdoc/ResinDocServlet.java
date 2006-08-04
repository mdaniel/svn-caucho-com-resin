/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.FactoryConfigurationError;

import com.caucho.config.Config;
import com.caucho.config.LineConfigException;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.WriterStreamImpl;

import com.caucho.xml.stream.XMLStreamWriterImpl;

public class ResinDocServlet extends HttpServlet {
  private Config _config;
  private Path _pwd;
  private XMLOutputFactory _outputFactory;

  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);
    
    _config = new Config();
    _pwd = Vfs.lookup().createRoot();

    /* XXX
    try {
      _outputFactory = XMLOutputFactory.newInstance();
    } catch (FactoryConfigurationError e) {
      throw new ServletException("Error configuring factory", e);
    }

    if (_outputFactory == null)
      throw new ServletException("Error configuring factory");
      */
  }

  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    PrintWriter out = response.getWriter();
    String servletPath = request.getServletPath();

    Path path = _pwd.lookup(servletPath);

    Document document = new Document(path, request.getContextPath());

    try {
      // XXX
      // XMLStreamWriter xmlOut = _outputFactory.createXMLStreamWriter(out);
      
      WriterStreamImpl writerStreamImpl = new WriterStreamImpl();
      writerStreamImpl.setWriter(out);
      WriteStream writeStream = new WriteStream(writerStreamImpl);

      XMLStreamWriter xmlOut = new XMLStreamWriterImpl(writeStream);

      _config.configure(document, path);

      document.writeHtml(xmlOut);
    } catch (IOException e) {
      throw e;
    } catch (LineConfigException e) {
      if (e.getLineNumber() < 0)
        throw new ServletException("Error configuring document", e);

      try {
        out.println("<html>");
        out.println("<body>");

        out.println("Error configuring document: " + e);

        ReadStream readStream = path.openRead();

        String line = readStream.readLine();

        out.println("<pre>");

        for (int i = 1; line != null; i++, line = readStream.readLine()) {
          if (i == e.getLineNumber())
            out.print("<div style='background-color: fa8072'>");

          line = line.replace("<", "&lt;");
          line = line.replace(">", "&gt;");
          out.print(line);

          if (i == e.getLineNumber())
            out.print("</div>");
          else
            out.println();
        }

        out.println("</pre>");

        out.println("</body>");
        out.println("</html>");
      } catch (IOException iOException) {
        throw new ServletException("Error configuring document", e);
      }
    } catch (Exception e) {
      throw new ServletException("Error configuring document", e);
    }
  }
}

