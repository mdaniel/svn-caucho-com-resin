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
import com.caucho.quercus.module.Construct;
import com.caucho.quercus.module.Optional;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;

public class DOMAttr extends DOMNode {

  private Attr _attr;
  private Env _env;

  @Construct
  public DOMAttr(Env env,
                 String name,
                 @Optional String value)
  {
    _env = env;
    Document doc = DOMDocument.createDocument();
    _attr = doc.createAttribute(name);
    if (!"".equals(value))
      _attr.setValue(value);
  }
  
  public DOMAttr(Env env,
                 Attr attr)
  {
    _env = env;
    _attr = attr;
  }
  
  public Attr getNode()
  {
    return _attr;
  }

  public Env getEnv()
  {
    return _env;
  }

  public boolean isId()
  {
    return _attr.isId();
  }

  public String getName()
  {
    return _attr.getName();
  }
  
  public DOMNode getOwnerElement()
  {
    return DOMNodeFactory.createDOMNode(_env, _attr.getOwnerElement());
  }
  
  public DOMTypeInfo getSchemaTypeInfo()
  {
    return new DOMTypeInfo(_attr.getSchemaTypeInfo());
  }
  
  public boolean getSpecified()
  {
    return _attr.getSpecified();
  }
  
  public String getValue()
  {
    return _attr.getValue();
  }
  
  public void setValue(String value)
  {
    _attr.setValue(value);
  }
}