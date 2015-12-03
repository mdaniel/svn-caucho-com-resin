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

package com.caucho.quercus.lib.i18n;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;

abstract public class Encoder
{
  protected static final int ERROR_CHARACTER = 0xFFFE;

  protected String _charset;
  protected String _replacement;
  protected boolean _isIgnore;
  protected boolean _isReplaceUnicode = false;

  protected Encoder(String charset)
  {
    _charset = charset;
  }

  public static Encoder create(String charset)
  {
    if (charset.equalsIgnoreCase("utf8")
        || charset.equalsIgnoreCase("utf-8"))
      return new Utf8Encoder();
    else if (charset.equalsIgnoreCase("big5")
             || charset.equalsIgnoreCase("big-5"))
      return new Big5Encoder(charset);
    else
      return new GenericEncoder(charset);
  }

  public boolean isUtf8()
  {
    return false;
  }

  public boolean isIgnore()
  {
    return _isIgnore;
  }

  public void setIgnoreErrors(boolean isIgnore)
  {
    _isIgnore = isIgnore;
  }

  public void setReplacement(String replacement)
  {
    _replacement = replacement;
  }

  public final void setReplaceUnicode(boolean isReplaceUnicode)
  {
    _isReplaceUnicode = isReplaceUnicode;
  }

  public void reset()
  {
  }

  abstract public boolean isEncodable(StringValue str,
                                      int start, int end);

  public StringValue encode(StringValue sb, CharSequence str)
  {
    return encode(sb, str, 0, str.length());
  }

  public StringValue encode(StringValue sb, CharSequence str, boolean isReset)
  {
    return encode(sb, str, 0, str.length(), isReset);
  }

  abstract public StringValue encode(StringValue sb, CharSequence str,
                                     int start, int end);

  public final StringValue encode(StringValue sb, CharSequence str,
                                  int start, int end, boolean isReset)
  {
    if (isReset)
      reset();

    return encode(sb, str, start, end);
  }

}
