/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import javax.annotation.PostConstruct;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;

public class Anchor extends FormattedText {
  private static final Logger log = Logger.getLogger(Anchor.class.getName());

  private String _configTag;
  private String _javadoc;
  private String _href;
  
  private String _doc;

  public Anchor(Document document)
  {
    super(document);
  }

  public void setJavadoc(String javadoc)
  {
    check();

    _javadoc = javadoc;
  }

  public void setConfigTag(String configTag)
  {
    check();

    _configTag = configTag;
  }

  public void setHref(String href)
  {
    check();

    _href = href;
  }
  
  public void setDoc(String href)
  {
    check();
    
    _doc = href;
  }
  
  private void check()
  {
    if (_configTag != null || _javadoc != null || _href != null || _doc != null)
      throw new ConfigException("Anchors must have exactly one of href, config-tag, javadoc, or doc attributes");
  }

  private void setDefaultText(String text)
  {
    if (getItems().isEmpty()) {
      addText(new RawString(text));
    }
  }

  @PostConstruct
  public void init()
  {
    if (_javadoc == null && _configTag == null && _href == null && _doc == null)
      throw new ConfigException("Anchors must have exactly one of href, config-tag, or javadoc attributes");
  }

  private void writeConfigTagHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    ReferenceDocument referenceDocument 
      = getDocument().getReferenceDocument();

    boolean isLinked = referenceDocument != null 
                       && referenceDocument.getURI() != null;

    if (isLinked) {
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

    if (_configTag != null) {      
      setDefaultText("<" + _configTag + ">");
    }
    
    super.writeHtml(out);

    if (isLinked)
      out.writeEndElement(); // a
  }

  private void writeJavadocHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    String path 
      = "http://www.caucho.com/resin-javadoc/" + _javadoc.replace('.', '/');

    int i = path.indexOf('#');

    if (i >= 0) {
      path = path.substring(0, i) + "." + path.substring(i + 1) + ".html";
    }
    else {
      path = path + ".html";
    }

    setDefaultText(_javadoc);

    out.writeAttribute("href", path);
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    if (_configTag != null) {
      writeConfigTagHtml(out);
    }
    else {
      out.writeStartElement("a");

      if (_javadoc != null) {
        writeJavadocHtml(out);
      }
      else if (_doc != null) {
        String href  = getDocument().getContextPath() + '/' + _doc;

        out.writeAttribute("href", href);
      }
      // XXX we should deprecate this syntax
      else if (_href.indexOf('|') >= 0) {
        String href 
          = getDocument().getContextPath() + '/' + _href.replace('|', '/');

        out.writeAttribute("href", href);
      }
      else
        out.writeAttribute("href", _href);

      super.writeHtml(out);

      out.writeEndElement();
    }
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    if (_configTag != null) {
      setDefaultText(_configTag);
      super.writeLaTeX(out);
    }
    else if (_javadoc != null) {
      setDefaultText(_javadoc);
      super.writeLaTeX(out);
    }
    else if (_href == null) {
      super.writeLaTeX(out);
    } 
    else if (_href.startsWith("doc|")) {
      String link = _href.substring("doc|".length()).replace("|", "/");
      link = link.replace("#", ":");

      out.print("\\hyperlink{" + link + "}{");

      super.writeLaTeX(out);

      out.print("}");
    }
    else {
      // XXX javadoc?
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

  @Override
  public void writeAsciiDoc(PrintWriter out)
    throws IOException
  {
    if (_configTag != null) {
      setDefaultText(_configTag);
      super.writeAsciiDoc(out);
    }
    else if (_javadoc != null) {
      setDefaultText(_javadoc);
      super.writeAsciiDoc(out);
    }
    else if (_href == null) {
      super.writeAsciiDoc(out);
    } 
    /*
    else if (_href.startsWith("doc|")) {
      String link = _href.substring("doc|".length()).replace("|", "/");
      link = link.replace("#", ":");

      out.print("\\hyperlink{" + link + "}{");

      super.writeLaTeX(out);

      out.print("}");
    }
    */
    else {
      out.print(_href);
      out.print(" ");
//      super.writeAsciiDoc(out);
  //    out.print("}");
       
    }
  }
}
