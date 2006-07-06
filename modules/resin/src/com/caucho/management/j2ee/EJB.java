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
 * @author Sam
 */


package com.caucho.management.j2ee;

import com.caucho.ejb.cfg.EjbBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;

/**
 * Base class management interface for enterpise java beans.
 */
abstract public class EJB
  extends J2EEManagedObject
{
  private final EjbBean _ejbBean;

  public EJB(EjbBean ejbBean)
  {
    _ejbBean = ejbBean;
  }

  protected ObjectName createObjectName(Hashtable<String, String> properties)
    throws MalformedObjectNameException
  {
    properties.put("EJBModule", quote(_ejbBean.getEJBModuleName()));

    return super.createObjectName(properties);
  }

  protected String getName()
  {
    return _ejbBean.getEJBName();
  }
}
