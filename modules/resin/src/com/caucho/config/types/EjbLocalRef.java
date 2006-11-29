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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import com.caucho.naming.Jndi;
import com.caucho.naming.LinkProxy;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;

/**
 * Configuration for the ejb-ref
 */
public class EjbLocalRef {
  private static L10N L = new L10N(EjbLocalRef.class);

  private String _name;
  private String _type;
  private String _home;
  private String _remote;
  private String _link;

  public void setEjbRefName(String name)
  {
    _name = name;
  }

  public void setEjbRefType(String type)
  {
    _type = type;
  }

  public void setLocalHome(String home)
  {
    _home = home;
  }

  public void setLocal(String remote)
  {
    _remote = remote;
  }

  public void setEjbLink(String link)
  {
    _link = link;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_link != null) {
      LinkProxy proxy = new LinkProxy(_link);
      Jndi.bindDeepShort(_name, proxy);
    }
  }

  public String toString()
  {
    return "EjbLocalRef[" + _name + "]";
  }
}
