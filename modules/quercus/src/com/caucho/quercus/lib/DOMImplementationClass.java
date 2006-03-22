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
import com.caucho.quercus.module.Optional;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

public class DOMImplementationClass {

  private DOMImplementation _domImplementation;
  private Env _env;

  public DOMImplementationClass(Env env)
  {
    _env = env;

    Document doc = DOMDocument.createDocument();
    _domImplementation = doc.getImplementation();
  }
  
  public void setDomImplementation(DOMImplementation domImplementation)
  {
    _domImplementation = domImplementation;
  }
  
  public DOMDocument createDocument(@Optional String namespaceURI,
                                    @Optional String qualifiedName,
                                    @Optional DOMDocumentType doctype)
  {
    DocumentType dt = null;

    if (doctype != null)
      dt = doctype.getNode();

    return new DOMDocument(_env, _domImplementation.createDocument(namespaceURI, qualifiedName, dt));
  }

  public DOMDocumentType createDocumentType(@Optional String qualifiedName,
                                            @Optional String publicId,
                                            @Optional String systemId)
  {
    return new DOMDocumentType(_env, _domImplementation.createDocumentType(qualifiedName, publicId, systemId));
  }

  public boolean hasFeature(String feature,
                            String version)
  {
    if (_domImplementation == null)
      return false;

    return _domImplementation.hasFeature(feature, version);
  }

}
