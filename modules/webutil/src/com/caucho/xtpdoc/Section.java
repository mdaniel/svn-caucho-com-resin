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

public abstract class Section implements ContentItem, ObjectWithParent {
  private Object _parent;
  protected String _documentName;
  protected String _name;
  protected String _title;
  protected String _version;
  protected boolean _topLevel = true;

  protected ArrayList<ContentItem> _contentItems = new ArrayList<ContentItem>();

  void setDocumentName(String documentName)
  {
    _documentName = documentName;

    for (ContentItem item : _contentItems) {
      if (item instanceof Section)
        ((Section) item).setDocumentName(documentName);
    }
  }

  String getDocumentName()
  {
    return _documentName;
  }

  void setTopLevel(boolean topLevel)
  {
    _topLevel = topLevel;

    for (ContentItem item : _contentItems) {
      if (item instanceof Section)
        ((Section) item).setTopLevel(_topLevel);
    }
  }

  public void setParent(Object parent)
  {
    _parent = parent;
  }

  public Object getParent()
  {
    return _parent;
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

  //
  // XXX: End stubbed
  //

  public void setTitle(String title)
  {
    _title = title;
  }

  public void addP(Paragraph paragraph)
  {
    _contentItems.add(paragraph);
  }

  public void addPre(PreFormattedText pretext)
  {
    _contentItems.add(pretext);
  }

  public void addOL(OrderedList orderedList)
  {
    _contentItems.add(orderedList);
  }

  public void addUL(UnorderedList unorderedList)
  {
    _contentItems.add(unorderedList);
  }

  public void addFigure(Figure figure)
  {
    _contentItems.add(figure);
  }

  public void addExample(Example example)
  {
    _contentItems.add(example);
  }

  public void addTable(Table table)
  {
    _contentItems.add(table);
  }

  public void addDefTable(DefinitionTable definitionTable)
  {
    _contentItems.add(definitionTable);
  }

  public void addDefTableChildTags(DefinitionTable definitionTable)
  {
    _contentItems.add(definitionTable);
  }

  public void addDefTableParameters(DefinitionTable definitionTable)
  {
    _contentItems.add(definitionTable);
  }

  public void addResults(Example results)
  {
    _contentItems.add(results);
  }

  public void addDef(Def def)
  {
    _contentItems.add(def);
  }

  public void addNote(FormattedTextWithAnchors note)
  {
    _contentItems.add(new NamedText("Note", note));
  }

  public void addWarn(FormattedTextWithAnchors warning)
  {
    _contentItems.add(new NamedText("Warning", warning));
  }

  public void addParents(FormattedText parents)
  {
    _contentItems.add(new NamedText("child of", parents));
  }
 
  public void addDefault(FormattedText def)
  {
    _contentItems.add(new NamedText("default", def));
  }

  public void addGlossary(Glossary glossary)
  {
    _contentItems.add(glossary);
  }
 
  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    for (ContentItem item : _contentItems)
      item.writeHtml(writer);
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    String label = (_documentName + ":" + _title).replace(" ", "-");

    writer.println("\\label{" + label + "}");
    writer.println("\\hypertarget{" + label + "}{}");

    for (ContentItem item : _contentItems)
      item.writeLaTeX(writer);
  }
}
