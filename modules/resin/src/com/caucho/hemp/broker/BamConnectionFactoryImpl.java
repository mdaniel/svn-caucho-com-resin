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

package com.caucho.hemp.broker;

import com.caucho.bam.*;
import com.caucho.config.manager.InjectManager;
import com.caucho.util.*;
import com.caucho.webbeans.manager.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * BamConnectionFactory
 */
public class BamConnectionFactoryImpl implements BamConnectionFactory
{
  private static final L10N L = new L10N(BamConnectionFactoryImpl.class);
  
  private BamBroker _broker;
  
  public BamConnectionFactoryImpl()
  {
    InjectManager manager = InjectManager.getCurrent();

    _broker = manager.getInstanceByType(BamBroker.class);

    if (_broker == null)
      throw new IllegalStateException(L.l("No BamBroker defined in current context"));
  }
  
  /**
   * Creates a session
   */
  public BamConnection getConnection(String uid,
				     String password)
  {
    if (uid == null)
      uid = createUid();
    
    return _broker.getConnection(uid, password);
  }
  
  /**
   * Creates a session
   */
  public BamConnection getConnection(String uid,
				     String password,
				     String resource)
  {
    if (uid == null)
      uid = createUid();
    
    return _broker.getConnection(uid, password, resource);
  }

  private String createUid()
  {
    StringBuilder sb = new StringBuilder();

    Base64.encode(sb, RandomUtil.getRandomLong());

    return sb.toString();
  }
}
