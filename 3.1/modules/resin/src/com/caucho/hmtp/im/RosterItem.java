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

package com.caucho.hmtp.im;

import java.io.Serializable;
import java.util.*;

/**
 * Roster query (jabber:iq:roster defined in rfc3921)
 *
 * <code><pre>
 * element query {
 *   item*
 * }
 *
 * element item {
 *   jid,
 *   ask?,
 *   name?,
 *   subscription?,
 *   group*
 * }
 * </pre></code>
 */
public class RosterItem implements Serializable {
  private final String _jid;
  private String []_group;
  // subscribe
  private String _ask;
  // description of the item
  private String _name;
  // none, to, from, both, remove
  private String _subscription;

  /**
   * zero-arg for hessian
   */
  private RosterItem()
  {
    _jid = null;
    _group = null;
    _subscription = "none";
  }

  public RosterItem(String jid, String []group)
  {
    _jid = jid;
    _group = group;
    _subscription = "none";

    if (_jid == null)
      throw new NullPointerException();
  }

  public String getJid()
  {
    return _jid;
  }

  public String []getGroup()
  {
    return _group;
  }

  public void setGroup(String []group)
  {
    _group = group;
  }

  public String getAsk()
  {
    return _ask;
  }

  public void setAsk(String ask)
  {
    _ask = ask;
  }

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getSubscription()
  {
    return _subscription;
  }

  public void setSubscription(String subscription)
  {
    _subscription = subscription;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());

    sb.append("[");
    sb.append(_jid);
    sb.append(",sub=");
    sb.append(_subscription);
    if (_ask != null)
      sb.append(",ask=" + _ask);
    sb.append("]");

    return sb.toString();
  }
}
