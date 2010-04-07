/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.config.types.RawString;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.logging.Logger;

public class Anchor extends FormattedText {
  private static final Logger log = Logger.getLogger(Anchor.class.getName());

  private String _configTag;
  private String _href = "";

  public Anchor(Document document)
  {
    super(document);
  }

  public void setConfigTag(String configTag)
  {
    _configTag = configTag;
  }

  public void setHref(String href)
  {
    _href = href;
  }

  private void setDefaultText(String text)
  {
    if (getItems().isEmpty()) {
      addText(new RawString(text));
    }
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    if (_configTag != null) {
      ReferenceDocument referenceDocument 
        = getDocument().getReferenceDocument();

      if (referenceDocument != null) {
        out.writeStartElement("a");
        out.writeAttribute("href", referenceDocument.getURI() 
                                   + '#' + _configTag);

        if (getDocument().isJavascriptEnabled()) {
          out.writeAttribute("onmouseover", 
                             "popup.mouseOverHandler(this, "
                                                  + "'" + _configTag + "')");
          out.writeAttribute("onmouseout", "popup.mouseOutHandler()");
        }
      }

      setDefaultText(_configTag);
      super.writeHtml(out);

      if (referenceDocument != null)
        out.writeEndElement(); // a

      return;
    }

    out.writeStartElement("a");

    if (_href.startsWith("javadoc|")) {
      String name = _href.substring("javadoc|".length());

      // XXX: method name is just stripped here
      int i = name.indexOf('|');

      while (i >= 0) {
        if (i == 0)
          name = name.substring(1);
        else if (i > 0)
          name = name.substring(0, i);

        i = name.indexOf('|');
      }

      setDefaultText(name);

      name = name.replace('.', '/') + ".html";

      out.writeAttribute("href", "http://www.caucho.com/resin-javadoc/" + name);
    }
    else if (_href.indexOf('|') >= 0) {
      String href = getDocument().getContextPath() + '/' + _href.replace('|', '/');
      
      out.writeAttribute("href", href);
    }
    else
      out.writeAttribute("href", _href);

    super.writeHtml(out);

    out.writeEndElement();
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    if (_href == null) {
      super.writeLaTeX(out);
    } else if (_href.startsWith("doc|")) {
      String link = _href.substring("doc|".length()).replace("|", "/");
      link = link.replace("#", ":");

      out.print("\\hyperlink{" + link + "}{");

      super.writeLaTeX(out);

      out.print("}");
    } else {
      try {
        URI uri = new URI(_href);

        if (uri.getScheme() != null) {
          out.print("\\href{" + _href + "}");

          out.print("{");

          super.writeLaTeX(out);

          out.print("}");

          return;
        } else if (uri.getPath() != null && uri.getPath().length() != 0) {
          out.print("\\hyperlink{" + uri.getPath());

          if (uri.getFragment() != null)
            out.print(":" + uri.getFragment());

          out.print("}{");
          super.writeLaTeX(out);
          out.print("}");

          return;
        } else if (uri.getFragment() != null && 
                   uri.getFragment().length() != 0) {
          // XXX
          String documentName = getDocument().getDocumentPath().getTail();

          if (documentName != null) {
            out.print("\\hyperlink{" + documentName + ":" + uri.getFragment());
            out.print("}{");
            super.writeLaTeX(out);
            out.print("}");

            return;
          }
        }

      } catch (Exception e) {
      }

      out.print("\\href{" + _href + "}");

      out.print("{");
      super.writeLaTeX(out);
      out.print("}");
    }
  }
}
