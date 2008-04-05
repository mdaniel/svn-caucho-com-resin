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

package com.caucho.hmpp;

import com.caucho.hmpp.QueryHandler;
import com.caucho.hmpp.HmppError;
import java.io.Serializable;
import com.caucho.hemp.*;

/**
 * Configuration for a service
 */
public class AbstractQueryHandler implements QueryHandler {
  public Serializable onQuery(String fromJid,
			      String toJid,
			      Serializable query)
  {
    throw new IllegalStateException();
  }
  
  public boolean onQueryGet(String id,
			    String fromJid,
			    String toJid,
			    Serializable query)
  {
    return false;
  }
  
  public boolean onQuerySet(String id,
			    String fromJid,
			    String toJid,
			    Serializable query)
  {
    return false;
  }
  
  public void onQueryResult(String id,
			    String fromJid,
			    String toJid,
			    Serializable value)
  {
  }
  
  public void onQueryError(String id,
			   String fromJid,
			   String toJid,
			   Serializable query,
			   HmppError error)
  {
  }
}
