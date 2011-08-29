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

package com.caucho.hmtp.disco;

import java.util.*;

/**
 * service discovery items
 *
 * http://www.xmpp.org/extensions/xep-0030.html
 *
 * xmlns="http://jabber.org/protocol/disco#items"
 *
 * <code><pre>
 * element query {
 *   attribute node?,
 *   item*
 * }
 *
 * element item {
 *    attribute jid,
 *    attribute node?,
 *    attribute name?,
 *    attribute action { remove, update}?,
 * }
 * </pre></code>
 */
public class DiscoItem implements java.io.Serializable {
  private String _jid;
  private String _node;
  private String _name;

  // remove, update
  private String _action;
  
  private DiscoItem()
  {
  }
  
  public DiscoItem(String jid)
  {
    _jid = jid;
  }
  
  public DiscoItem(String jid, String node)
  {
    _jid = jid;
    _node = node;
  }
  
  public DiscoItem(String jid,
		   String node,
		   String name,
		   String action)
  {
    _jid = jid;
    _node = node;
    _name = name;
    _action = action;
  }
  
  @Override
  public String toString()
  {
    if (_node != null)
      return (getClass().getSimpleName()
	      + "[" + _jid + ",node=" + _node + "]");
    else
      return (getClass().getSimpleName()
	      + "[" + _jid + "]");
  }
}
