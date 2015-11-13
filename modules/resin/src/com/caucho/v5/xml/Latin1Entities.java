/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.xml;

/**
 * Latin1 encodings (ISO-8859-1) doesn't need to escape the printable Latin1
 * characters.
 */
class Latin1Entities extends HtmlEntities {
  private static HtmlEntities _html40;
  private static HtmlEntities _html32;

  static Entities create(double version)
  {
    if (version == 0 || version >= 4.0) {
      if (_html40 == null)
        _html40 = new Latin1Entities(4.0);
      return _html40;
    }
    else {
      if (_html32 == null)
        _html32 = new Latin1Entities(3.2);
      return _html32;
    }
  }

  protected Latin1Entities(double version)
  {
    super(version);

    for (int i = 161; i < 256; i++) {
      _latin1[i] = String.valueOf((char) i).toCharArray();
      _attrLatin1[i] = _latin1[i];
    }
  }
}
