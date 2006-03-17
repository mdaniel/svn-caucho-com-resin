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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.Env;

import org.w3c.dom.DocumentType;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Notation;

public class DOMDocumentType extends DOMNode {

  private Env _env;
  private DocumentType _docType;

  public DOMDocumentType(Env env,
                         DocumentType docType)
  {
    _env = env;
    _docType = docType;
  }

  public DocumentType getNode()
  {
    return _docType;
  }

  public Env getEnv()
  {
    return _env;
  }

  public String getPublicId()
  {
    return _docType.getPublicId();
  }

  public String getSystemId()
  {
    return _docType.getSystemId();
  }

  public String getName()
  {
    return _docType.getName();
  }

  public DOMNamedNodeMap getEntities()
  {
    NamedNodeMap entities = _docType.getEntities();
    int length = entities.getLength();

    DOMNamedNodeMap result = new DOMNamedNodeMap(_env, entities);

    for (int i = 0; i < length; i++) {
      result.put(_env.wrapJava(new DOMEntity(_env, (Entity) entities.item(i))));
    }

    return result;
  }

  public DOMNamedNodeMap getNotations()
  {
    NamedNodeMap notations = _docType.getNotations();
    int length = notations.getLength();

    DOMNamedNodeMap result = new DOMNamedNodeMap(_env, notations);

    for (int i = 0; i < length; i++) {
      result.put(_env.wrapJava(new DOMNotation(_env, (Notation) notations.item(i))));
    }

    return result;
  }
 
  public String getInternalSubset()
  {
    return _docType.getInternalSubset();
  }
}
