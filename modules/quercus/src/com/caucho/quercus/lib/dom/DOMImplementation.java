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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.xml.QDOMImplementation;

import org.w3c.dom.DocumentType;

public class DOMImplementation
{
  private final QDOMImplementation _impl = new QDOMImplementation();

  public DOMImplementation()
  {
  }

  static public boolean hasFeature(String feature, String version)
  {
    return (new QDOMImplementation()).hasFeature(feature, version);
  }

  @ReturnNullAsFalse
  static public DOMDocumentType createDocumentType(@NotNull String qualifiedName,
                                                   @Optional String publicId,
                                                   @Optional String systemId)
  {
    if (qualifiedName == null)
      return null;

    if ((publicId != null && publicId.length() > 0)
        && (publicId != null && publicId.length() > 0))
      return new DOMDocumentType(qualifiedName, publicId, systemId);
    else
      return new DOMDocumentType(qualifiedName);
  }

  static public DOMDocument createDocument(@Optional String namespaceURI,
                                           @Optional String name,
                                           @Optional DocumentType docType)
  {
    DOMDocument doc = new DOMDocument(new DOMImplementation());

    if (docType != null)
      doc.setDoctype(docType);

    if (name != null && name.length() > 0) {
      DOMElement elt = new DOMElement(name, null, namespaceURI);
      doc.appendChild(elt);
    }

    return doc;
  }

  QDOMImplementation getQDOMImplementation()
  {
    return _impl;
  }
}
