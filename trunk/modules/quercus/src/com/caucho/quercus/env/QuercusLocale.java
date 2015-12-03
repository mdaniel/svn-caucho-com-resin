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

package com.caucho.quercus.env;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class QuercusLocale
{
  private final static QuercusLocale DEFAULT_US
    = new QuercusLocale(Locale.US, "utf-8");

  private final Locale _locale;
  private final String _charset;

  private DecimalFormatSymbols _format;

  public QuercusLocale(Locale locale, String charset)
  {
    _locale = locale;
    _charset = charset;
  }

  public Locale getLocale()
  {
    return _locale;
  }

  public String getCharset()
  {
    return _charset;
  }

  public char getDecimalSeparator()
  {
    if (_format == null) {
      _format = DecimalFormatSymbols.getInstance(_locale);
    }

    return _format.getDecimalSeparator();
  }

  public static QuercusLocale getDefault()
  {
    return DEFAULT_US;
  }

  public String toString()
  {
    return _locale.toString();
  }
}
