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

package com.caucho.server.resin;

import java.util.logging.Logger;

import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleActor;
import com.caucho.boot.WatchdogStopQuery;
import com.caucho.util.L10N;

/**
 * Service for handling the distributed cache
 */
public class ResinActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ResinActor.class.getName());

  private static final L10N L = new L10N(ResinActor.class);

  private Resin _resin;
  
  ResinActor(Resin resin)
  {
    _resin = resin;
    
    setJid("resin");
  }

  @QuerySet
  public void stop(long id,
		   String to,
		   String from,
		   WatchdogStopQuery query)
  {
    log.info(_resin + " stop request from watchdog '" + from + "'");

    _resin.startShutdown(L.l("Resin shutdown from watchdog stop '"
                             + from + "'"));

    getLinkStream().queryResult(id, from, to, query);
  }

  public void destroy()
  {
    _resin.startShutdown(L.l("Resin shutdown from ResinActor"));
  }
}
