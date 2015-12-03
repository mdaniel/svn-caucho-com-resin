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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;

public class Summary implements ContentItem {
  private static final Logger log = Logger.getLogger(Summary.class.getName());

  private Document _document;
  private ATOCControl _atocControl;
  private Navigation _navigation;

  private boolean _isSkipDescription;

  public Summary(Document document)
  {
    _document = document;
  }

  void setNavigation(Navigation navigation)
  {
    _navigation = navigation;
  }

  public void setATOC(String atoc)
  {
  }

  public void setSkipDescription(boolean isSkip)
  {
    _isSkipDescription = isSkip;
  }

  public void setObjSummary(String objSummary)
  {
  }

  public void setObjsummary(String objSummary)
  {
  }

  public void setNoObjSummary(String noObjSummary)
  {
  }

  public void setObjSummaryInLocalTOC(String objSummary)
  {
  }

  public void setDescription(String description)
  {
  }

  public void setLocalTOC(String localTOC)
  {
    // XXX
  }

  public void setATOCControl(ATOCControl atocControl)
  {
    _atocControl = atocControl;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    _document.fillChildNavigation();
    
    if (_document.getNavigation() == null) {
    }
    else if (_isSkipDescription) {
      //out.writeStartElement("ol");
      _document.getNavigation().writeHtml(out, "", 1, 2, 5);
      //out.writeEndElement(); // ol
    }
    else {
      //out.writeStartElement("ol");
      _document.getNavigation().writeHtml(out, "", 1, 0, 5);
      //out.writeEndElement(); // ol
    }
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeXVerbatim(PrintWriter out)
    throws IOException
  {
    throw new ConfigException("<summary> not allowed in a verbatim context");
  }

  @Override
  public void writeAsciiDoc(PrintWriter out)
    throws IOException
  {
  }
}
