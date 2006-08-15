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

import java.util.logging.Logger;

import com.caucho.vfs.Path;

import com.caucho.config.Config;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

public class NavigationItem {
  private static final Logger log 
    = Logger.getLogger(NavigationItem.class.getName());

  private int _maxDepth = 4;
  private int _depth;
  private String _link;
  private String _title;
  private String _product;
  private Document _document;
  private ContentItem _description;
  private boolean _atocDescend = false;
  private Navigation _child;
  private ArrayList<NavigationItem> _items = new ArrayList<NavigationItem>();
  private Path _rootPath;

  public NavigationItem(Document document, int depth)
  {
    _document = document;
    _depth = depth;
    _rootPath = document.getDocumentPath().getParent();
  }

  public void init()
  {
    if (_depth > _maxDepth)
      return;

    if (_rootPath != null && _link != null) {
      Path linkPath = _rootPath.lookup(_link);

      if (linkPath.exists()) {
        Config config = new Config();

        Document document = new Document(linkPath, _document.getContextPath());

        try {
          config.configure(document, linkPath);

          if (document.getHeader() != null)
            _description = document.getHeader().getDescription();
        } catch (NullPointerException e) {
          log.info("error configuring " + linkPath + ": " + e);
          e.printStackTrace();
        } catch (Exception e) {
          log.info("error configuring " + linkPath + ": " + e);
        }

        if (_atocDescend) {
          Path linkRoot = linkPath.getParent();
          Path subToc = linkPath.getParent().lookup("toc.xml");

          if (subToc.exists()) {
            _child = new Navigation(document, _depth + 1);

            try {
              config.configure(_child, subToc);
            } catch (Exception e) {
              log.info("Failed to configure " + subToc + ": " + e);

              _child = null;
            }
          } else {
            log.info(subToc + " does not exist!");
          }
        }
      }
    }
  }

  public ContentItem createDescription()
  {
    _description = new FormattedText(_document);
    return _description;
  }

  public void setATOCDescend(boolean atocDescend)
  {
    _atocDescend = atocDescend;
  }

  public void setLink(String link)
  {
    _link = link;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setProduct(String product)
  {
    _product = product;
  }

  public NavigationItem createItem()
  {
    NavigationItem item = new NavigationItem(_document, _depth + 1);
    _items.add(item);
    return item;
  }

  public void writeHtml(XMLStreamWriter out, String path)
    throws XMLStreamException
  {
    if (_maxDepth < _depth)
      return;

    String depthString = (_depth == 0) ? "top" : ("" + _depth);

    if (_child != null || _items.size() > 0) {
      out.writeStartElement("dl");
      out.writeAttribute("class", "atoc-toplevel atoc-toplevel-" + depthString);

      out.writeStartElement("dt");
      out.writeAttribute("class", "atoc-toplevel atoc-toplevel-" + 
                                  (_depth + 1));
    } else {
      out.writeStartElement("dt");
      out.writeAttribute("class", "atoc-toplevel atoc-toplevel-" + depthString);
    }

    out.writeStartElement("b");

    if (_link != null) {
      out.writeStartElement("a");
      out.writeAttribute("href", path + _link);
      out.writeCharacters(_title);
      out.writeEndElement(); // a
    }

    out.writeEndElement(); // b

    out.writeEndElement(); // dt

    out.writeStartElement("dd");
    out.writeAttribute("class", "atoc-toplevel atoc-toplevel-" + depthString);

    // XXX: brief/paragraph/none
    if (_description != null) {
      out.writeStartElement("p");
      _description.writeHtml(out);
      out.writeEndElement(); // p
    }

    if (_link != null) {
      int p = _link.lastIndexOf('/');
      String tail;
      if (p >= 0)
        tail = path + _link.substring(0, p + 1);
      else
        tail = path;

      if (_child != null)
        _child.writeHtml(out, tail);

      for (NavigationItem item : _items)
        item.writeHtml(out, tail);
    }

    out.writeEndElement(); // dd

    if (_child != null || _items.size() > 0)
      out.writeEndElement(); // dl
  }

  public void writeLeftNav(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("a");
    out.writeAttribute("href", _link);
    out.writeCharacters(_title);
    out.writeEndElement(); // a

    out.writeEmptyElement("br");
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
  }
}
