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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import java.util.Locale;

public class LocaleInfo {
  private Locale _collate;
  private Locale _ctype;
  private Locale _monetary;
  private Locale _numeric;
  private Locale _time;
  private Locale _messages;

  LocaleInfo()
  {
    setAll(Locale.getDefault());
  }

  public void setAll(Locale locale)
  {
    setCollate(locale);
    setCtype(locale);
    setMonetary(locale);
    setNumeric(locale);
    setTime(locale);
    setMessages(locale);
  }

  public Locale getCollate()
  {
    return _collate;
  }

  public void setCollate(Locale locale)
  {
    _collate = locale;
  }

  public Locale getCtype()
  {
    return _ctype;
  }

  public void setCtype(Locale locale)
  {
    _ctype = locale;
  }

  public Locale getMonetary()
  {
    return _ctype;
  }

  public void setMonetary(Locale locale)
  {
    _monetary = locale;
  }

  public Locale getTime()
  {
    return _time;
  }

  public void setTime(Locale locale)
  {
    _time = locale;
  }

  public Locale getNumeric()
  {
    return _numeric;
  }

  public void setNumeric(Locale locale)
  {
    _numeric = locale;
  }

  public Locale getMessages()
  {
    return _messages;
  }

  public void setMessages(Locale locale)
  {
    _messages = locale;
  }
}
