/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.ramp.embed;

import io.baratine.service.ServiceInitializer;
import io.baratine.service.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.util.L10N;

public class ManagerBuilderImpl implements ManagerBuilder
{
  private static final Logger log
    = Logger.getLogger(ManagerBuilderImpl.class.getName());
  private static final L10N L = new L10N(ManagerBuilderImpl.class);

  @Override
  public ServiceManager build()
  {
    ServiceManagerAmp ampManager = Amp.newManager();
    
    ManagerBaratineImpl manager = new ManagerBaratineImpl(ampManager);
    
    bind(ampManager);
    
    return manager;
  }
  
  private void bind(ServiceManagerAmp manager)
  {
    ArrayList<ServiceInitializer> providerList = new ArrayList<>();

    Iterator<ServiceInitializer> iter;

    iter = ServiceLoader.load(ServiceInitializer.class).iterator();

    while (iter.hasNext()) {
      try {
        providerList.add(iter.next());
      } catch (Throwable e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, e.toString(), e);
        }
        else {
          log.fine(L.l("{0} while processing {1}",
                       e.toString(), ServiceInitializer.class.getName()));
        }
      }
    }

    Collections.sort(providerList, (a,b)->
        a.getClass().getSimpleName().compareTo(b.getClass().getSimpleName()));

    for (ServiceInitializer provider : providerList) {
      try {
        provider.init(manager);
      } catch (Throwable e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, e.toString(), e);
        }
        else {
          log.fine(L.l("'{0}' while processing {1}", e.toString(), provider));
        }
      }
    }
  }
}
