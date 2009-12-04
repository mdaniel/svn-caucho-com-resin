/*
 * Copyright (c) 1998-2001 Caucho Technology -- all rights reserved
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

package com.caucho.ejb;

final public class Version {
  public static String VERSION = "Resin-EJB 3.0.s031104 (built Tue Nov  4 09:13:01 PST 2004)";

  private static int version = 0;

  public static int getVersionId()
  {
    if (version != 0)
      return version;

    String v = VERSION;
    version = 1391;
    for (int i = 0; i < v.length(); i++)
      version = 65521 * version + v.charAt(i);

    if (version == 0)
      version = 1;

    return version;
  }
}
