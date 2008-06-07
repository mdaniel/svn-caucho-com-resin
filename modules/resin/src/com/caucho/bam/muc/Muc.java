/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.bam.muc;

import java.util.*;

/**
 * XEP-0045
 * http://www.xmpp.org/extensions/xep-0045.html
 *
 * Muc query
 *
 * <code><pre>
 * namespace = http://jabber.org/protocol/muc
 *
 * element x {
 *   history?,
 *   password?
 * }
 *
 * element history {
 *   attribute maxchars?,
 *   attribute maxstanzas?
 *   attribute seconds?
 *   attribute since?
 * }
 * </pre></code>
 */
public class Muc implements java.io.Serializable {
  private String _password;
  
  private int _historyMaxChars;
  private int _historyMaxStanzas;
  private int _historySeconds;
  private Date _historySince;
  
  public Muc()
  {
  }

  public Muc(String password,
	     int historyMaxChars,
	     int historyMaxStanzas,
	     int historySeconds,
	     Date historySince)
  {
    _password = password;
    
    _historyMaxChars = historyMaxChars;
    _historyMaxStanzas = historyMaxStanzas;
    _historySeconds = historySeconds;
    _historySince = historySince;
  }

  public String getPassword()
  {
    return _password;
  }

  public int getHistoryMaxChars()
  {
    return _historyMaxChars;
  }

  public int getHistoryMaxStanzas()
  {
    return _historyMaxStanzas;
  }

  public int getHistorySeconds()
  {
    return _historySeconds;
  }

  public Date getHistorySince()
  {
    return _historySince;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName()).append("[");
    
    if (_historyMaxChars > 0)
      sb.append(",max-chars=").append(_historyMaxChars);
    
    if (_historyStanzas > 0)
      sb.append(",max-stanzas=").append(_historyMaxStanzas);
    
    if (_historySeconds > 0)
      sb.append(",seconds=").append(_historySeconds);
    
    if (_historySince != null)
      sb.append(",since=").append(_historySince);

    sb.append("]");

    return sb.toString();
  }
}
