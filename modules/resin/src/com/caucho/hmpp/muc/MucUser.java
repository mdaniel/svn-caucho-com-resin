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

package com.caucho.hmpp.muc;

import com.caucho.hmpp.muc.MucStatus;
import com.caucho.hmpp.muc.MucInvite;
import com.caucho.hmpp.muc.MucDestroy;
import com.caucho.hmpp.muc.MucDecline;
import java.io.Serializable;
import java.util.*;

/**
 * Muc user query
 *
 * http://jabber.org/protocol/muc#user
 *
 * <code><pre>
 * element x {
 *   decline?
 *   &amp; destroy?
 *   &amp; invite*
 *   &amp; item?
 *   &amp; password?
 *   &amp; status*
 * }
 *
 * element decline {
 *   @from?
 *   &amp; @to?
 *   &amp; reason?
 * }
 *
 * element invite {
 *   @from?
 *   &amp; @to?
 *   &amp; reason?
 * }
 *
 * element destroy {
 *   @jid?
 *   &amp; reason?
 * }
 *
 * element item {
 *   @affiliation?
 *   &amp; @jid?
 *   &amp; @nick?
 *   &amp; @role?
 *   &amp; actor?
 *   &amp; reason?
 *   &amp; continue?
 * }
 *
 * element actor {
 *   @jid
 * }
 *
 * element continue {
 *   @thread?
 * }
 *
 * element status {
 *   @code
 * }
 * </pre></code>
 */
public class MucUser implements java.io.Serializable {
  private MucDecline _decline;
  private MucDestroy _destroy;
  private ArrayList<MucInvite> _invite;
  private MucUserItem _item;
  private String _password;
  private ArrayList<MucStatus> _status;
  
  public MucUser()
  {
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
