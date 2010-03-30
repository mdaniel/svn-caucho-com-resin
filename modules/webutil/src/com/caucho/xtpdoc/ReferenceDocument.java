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

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.Config;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class ReferenceDocument extends Document {
  private static Logger log = 
    Logger.getLogger(ReferenceDocument.class.getName());
  private static L10N L = new L10N(ReferenceDocument.class);

  private References _references;

  ReferenceDocument()
  {
    super();
  }

  public ReferenceDocument(Path documentPath, String contextPath)
  {
    super(documentPath, contextPath);
  }

  public ReferenceDocument(ServletContext webApp,
                           Path documentPath,
                           String contextPath,
                           String uri,
                           String encoding)
  {
    super(webApp, documentPath, contextPath, uri, encoding);
  }

  void setReferences(References references)
  {
    _references = references;
  }

  @Override
  ReferenceDocument getReferenceDocument()
  {
    return this;
  }

  @Override
  public Body createBody()
  {
    return new ReferenceBody(this);
  }

  private class ReferenceBody extends Body {
    private final HashMap<String,Defun> _defuns = new HashMap<String,Defun>();

    public ReferenceBody(Document document) 
    {
      super(document);
    }

    @Override
    public Defun createDefun()
    {
      Defun defun = new ReferenceDefun(getDocument());
      addItem(defun);

      return defun;
    }

    public Defun getDefun(String ref)
    {
      // XXX implement parent:child lookup
      return _defuns.get(ref);
    }

    @Override
    protected void writeContent(XMLStreamWriter out)
      throws XMLStreamException
    {
      if (_references == null) {
        super.writeContent(out);
        return;
      }
        
      for (String ref : _references.getReferences()) {
        Defun defun = getDefun(ref);

        if (defun == null)
          log.log(Level.FINE, L.l("Unknown reference '{0}'", ref));
        else
          defun.writeHtml(out);
      }
    }

    private class ReferenceDefun extends Defun {
      public ReferenceDefun(Document document)
      {
        super(document);
      }

      @PostConstruct
      public void init()
      {
        _defuns.put(getName(), this);
      }
    }
  }
}
