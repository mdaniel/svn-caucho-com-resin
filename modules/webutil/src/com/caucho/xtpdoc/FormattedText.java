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

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import com.caucho.config.types.*;

public class FormattedText implements ContentItem {
  private Document _document;
  protected ArrayList<ContentItem> _contentItems = new ArrayList<ContentItem>();

  public FormattedText(Document document)
  {
    _document = document;
  }

  protected Document getDocument()
  {
    return _document;
  }

  protected void addItem(ContentItem item)
  {
    _contentItems.add(item);
  }

  public void setOccur(String occur)
  {
  }

  public void addText(String text)
  {
    _contentItems.add(new Text(text));
  }

  public void addG(GlossaryText text)
  {
    _contentItems.add(text);
  }

  public LineBreak createBr()
  {
    LineBreak lineBreak = new LineBreak(_document);
    _contentItems.add(lineBreak);
    return lineBreak;
  }

  public ItalicizedText createI()
  {
    ItalicizedText text = new ItalicizedText(_document);
    _contentItems.add(text);
    return text;
  }

  public BoldText createB()
  {
    BoldText text = new BoldText(_document);
    _contentItems.add(text);
    return text;
  }

  public EmphasizedText createEm()
  {
    EmphasizedText text = new EmphasizedText(_document);
    _contentItems.add(text);
    return text;
  }

  public SuperText createSup()
  {
    SuperText text = new SuperText(_document);
    _contentItems.add(text);
    return text;
  }

  public PreFormattedText createPre()
  {
    PreFormattedText pretext = new PreFormattedText(_document);
    _contentItems.add(pretext);
    return pretext;
  }

  public Variable createVar()
  {
    Variable variable = new Variable(_document);
    _contentItems.add(variable);
    return variable;
  }

  public Code createCode()
  {
    Code code = new Code(_document);
    _contentItems.add(code);
    return code;
  }

  public Url createUrl()
  {
    Url url = new Url(_document);
    _contentItems.add(url);
    return url;
  }

  public Example createExample()
  {
    Example example = new Example(_document);
    _contentItems.add(example);
    return example;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    for (ContentItem item : _contentItems)
      item.writeHtml(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    for (ContentItem item : _contentItems)
      item.writeLaTeX(out);
  }
}
