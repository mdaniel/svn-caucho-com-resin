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

public abstract class Section implements ContentItem {
  protected Document _document;
  protected FormattedTextWithAnchors _description;
  protected String _name;
  protected String _title;
  protected String _version;
  protected String _type;

  protected ArrayList<ContentItem> _contentItems = new ArrayList<ContentItem>();

  public Section(Document document)
  {
    _document = document;
  }

  public Document getDocument()
  {
    return _document;
  }

  // 
  // XXX: Stubbed
  //
  
  public void setOccur(String occur)
  {
  }

  public void setLocalTOCIndent(String localTOCIndent)
  {
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public void setProduct(String product)
  {
  }

  public void setIndex(String index)
  {
  }

  //
  // XXX: End stubbed
  //
  
  public void setType(String type)
  {
    _type = type;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public DefinitionList createDl()
  {
    DefinitionList list = new DefinitionList(_document);
    _contentItems.add(list);
    return list;
  }

  public FormattedTextWithAnchors createDescription()
  {
    _description = new FormattedTextWithAnchors(_document);
    return _description;
  }

  public BlockQuote createBlockquote()
  {
    BlockQuote blockQuote = new BlockQuote(_document);
    _contentItems.add(blockQuote);
    return blockQuote;
  }

  public Paragraph createP()
  {
    Paragraph paragraph = new Paragraph(_document);
    _contentItems.add(paragraph);
    return paragraph;
  }

  public PreFormattedText createPre()
  {
    PreFormattedText pretext = new PreFormattedText(_document);
    _contentItems.add(pretext);
    return pretext;
  }

  public OrderedList createOl()
  {
    OrderedList orderedList = new OrderedList(_document);
    _contentItems.add(orderedList);
    return orderedList;
  }

  public UnorderedList createUl()
  {
    UnorderedList unorderedList = new UnorderedList(_document);
    _contentItems.add(unorderedList);
    return unorderedList;
  }

  public Figure createFigure()
  {
    Figure figure = new Figure(_document);
    _contentItems.add(figure);
    return figure;
  }

  public Example createExample()
  {
    Example example = new Example(_document);
    _contentItems.add(example);
    return example;
  }

  public Table createTable()
  {
    Table table = new Table(_document);
    _contentItems.add(table);
    return table;
  }

  public DefinitionTable createDeftable()
  {
    DefinitionTable definitionTable = new DefinitionTable(_document);
    _contentItems.add(definitionTable);
    return definitionTable;
  }

  public DefinitionTable createDeftableChildtags()
  {
    DefinitionTable definitionTable = new DefinitionTable(_document);
    _contentItems.add(definitionTable);
    return definitionTable;
  }

  public DefinitionTable createDeftableParameters()
  {
    DefinitionTable definitionTable = new DefinitionTable(_document);
    _contentItems.add(definitionTable);
    return definitionTable;
  }

  public Example createResults()
  {
    Example results = new Example(_document);
    _contentItems.add(results);
    return results;
  }

  public Def createDef()
  {
    Def def = new Def(_document);
    _contentItems.add(def);
    return def;
  }

  public FormattedTextWithAnchors createNote()
  {
    FormattedTextWithAnchors note = new FormattedTextWithAnchors(_document);
    _contentItems.add(new NamedText("Note", note));
    return note;
  }

  public FormattedTextWithAnchors createWarn()
  {
    FormattedTextWithAnchors warning = new FormattedTextWithAnchors(_document);
    _contentItems.add(new NamedText("Warning", warning));
    return warning;
  }

  public FormattedText createParents()
  {
    FormattedText parents = new FormattedText(_document);
    _contentItems.add(new NamedText("child of", parents));
    return parents;
  }
 
  public FormattedText createDefault()
  {
    FormattedText def = new FormattedText(_document);
    _contentItems.add(new NamedText("default", def));
    return def;
  }

  public Glossary createGlossary()
  {
    Glossary glossary = new Glossary(_document);
    _contentItems.add(glossary);
    return glossary;
  }
 
  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    for (ContentItem item : _contentItems)
      item.writeHtml(out);
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    String label = _document.getDocumentPath().getUserPath() + ":" + _title;

    label = label.replace(" ", "-");

    out.println("\\label{" + label + "}");
    out.println("\\hypertarget{" + label + "}{}");

    for (ContentItem item : _contentItems)
      item.writeLaTeX(out);

    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");
  }

  abstract public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException;
}
