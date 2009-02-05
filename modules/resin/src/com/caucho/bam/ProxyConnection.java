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

package com.caucho.bam;

import com.caucho.util.L10N;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * Abstract bam connection
 */
public class ProxyBamConnection extends AbstractBamConnection {
  private static final L10N L = new L10N(ProxyBamConnection.class);
  
  private static final Logger log
    = Logger.getLogger(ProxyBamConnection.class.getName());

  private String _jid;
  private BamStream _brokerStream;

  public ProxyBamConnection(String jid, BamStream brokerStream)
  {
    _jid = jid;

    if (_jid == null)
      throw new NullPointerException(L.l("jid may not be null"));
    
    _brokerStream = brokerStream;

    if (_brokerStream == null)
      throw new NullPointerException(L.l("brokerStream may not be null"));
  }

  @Override
  public String getJid()
  {
    return _jid;
  }

  @Override
  public BamStream getBrokerStream()
  {
    return _brokerStream;
  }

  @Override
  public BamStream getAgentStream()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public void setAgentStream(BamStream bamStream)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public boolean isClosed()
  {
    return _brokerStream != null;
  }

  public void close()
  {
    _brokerStream = null;
  }
}
