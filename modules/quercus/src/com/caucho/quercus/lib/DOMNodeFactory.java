/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.Env;

import org.w3c.dom.*;

public class DOMNodeFactory {

  public static DOMNode createDOMNode(Env env, Node node)
  {
    if (node == null)
      return null;
    
    if (node instanceof Attr)
      return new DOMAttr(env, (Attr) node);
    else if (node instanceof Element)
      return new DOMElement(env, (Element) node);
    else if (node instanceof Comment)
      return new DOMComment(env, (Comment) node);
    else if (node instanceof Text)
      return new DOMText(env, (Text) node);
    else if (node instanceof Document)
      return new DOMDocument(env, (Document) node);
    else if (node instanceof DocumentType)
      return new DOMDocumentType(env, (DocumentType) node);
    else if (node instanceof DocumentFragment)
      return new DOMDocumentFragment(env, (DocumentFragment) node);
    else if (node instanceof Entity)
      return new DOMEntity(env, (Entity) node);
    else if (node instanceof EntityReference)
      return new DOMEntityReference(env, (EntityReference) node);
    else if (node instanceof Notation)
      return new DOMNotation(env, (Notation) node);
    else if (node instanceof ProcessingInstruction)
      return new DOMProcessingInstruction(env, (ProcessingInstruction) node);
    else
      return null;
  }
}
