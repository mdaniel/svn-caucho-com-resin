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
 * @author Scott Ferguson
 */

package com.caucho;

final public class Version {
  public static final String COPYRIGHT =
    "Copyright(c) 1998-2006 Caucho Technology.  All rights reserved.";

  public static String FULL_VERSION = "Resin-3.0.s060424 (built Mon, 24 Apr 2006 03:17:51 PDT)";
  public static String VERSION = "3.0.s060424";
  public static String VERSION_DATE = "20060424T031751";

  public static void main(String []argv)
  {
    System.out.println(FULL_VERSION);
    System.out.println(COPYRIGHT);
  }
}
