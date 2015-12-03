/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext;

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.lib.gettext.expr.PluralExpr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

abstract class GettextParser
{
  PluralExpr _pluralExpr;
  String _charset;

  PluralExpr getPluralExpr()
  {
    return _pluralExpr;
  }

  /*
   * Returns the charset defined in the PO/MO file.
   */
  public String getCharset()
  {
    return _charset;
  }
  
  /**
   * Extracts the charset from the gettext metadata.
   */
  protected static String getCharset(StringValue metadata)
  {
    String header = "charset=";
    int i = metadata.indexOf(header);

    if (i < 0)
      return "UTF-8";

    i = i + header.length();
    int len = metadata.length();

    int j = i + 1;
    for (; j < len; j++) {
      char ch = metadata.charAt(j);

      switch (ch) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          return metadata.substring(i, j).toString();
        default:
          continue; 
      }
    }

    return metadata.substring(i, j).toString();
  }

  /**
   * Returns the gettext translations.
   *
   * @return translations from file, or null on error
   */
  abstract HashMap<StringValue, ArrayList<StringValue>> readTranslations()
    throws IOException;

  abstract void close();
}
