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
import com.caucho.xmpp.disco.DiscoInfoQuery;
import com.caucho.xmpp.disco.DiscoIdentity;
import com.caucho.xmpp.disco.DiscoFeature;
import com.caucho.xmpp.im.ImSessionQuery;
import com.caucho.bam.BamStream;
import com.caucho.bam.BamConnection;
import com.caucho.config.*;
import com.caucho.bam.AbstractBamStream;
import com.caucho.bam.SimpleBamService;
import com.caucho.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * GenericService implementation to simplify configuring a service.
 */
public class HempDomainService extends SimpleBamService
{
  private static final L10N L = new L10N(HempDomainService.class);
  private static final Logger log
    = Logger.getLogger(HempDomainService.class.getName());

  private String _jid;

  HempDomainService(HempBroker broker, String jid)
  {
    // setBroker(broker);
    setJid(jid);

    // init();
  }
  
  @Override
  public boolean startAgent(String jid)
  {
    if (jid.indexOf('/') < 0 && jid.indexOf('@') < 0)
      return true;
    else
      return false;
  }

  @QuerySet
  public boolean querySet(long id,
			  String to,
			  String from,
			  ImSessionQuery query)
  {
    getBrokerStream().queryResult(id, from, to, null);

    return true;
  }
}
