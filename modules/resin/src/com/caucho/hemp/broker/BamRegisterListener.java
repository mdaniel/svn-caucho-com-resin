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

import com.caucho.bam.Actor;
import com.caucho.config.inject.BeanRegistrationListener;
import com.caucho.config.inject.CauchoBean;
import com.caucho.hemp.broker.HempBroker;

import java.lang.annotation.Annotation;
import javax.inject.manager.BeanManager;

/**
 * Broker
 */
public class BamRegisterListener implements BeanRegistrationListener
{
  public boolean isMatch(Annotation ann)
  {
    return ann instanceof com.caucho.remote.BamService;
  }

  public void start(BeanManager manager, CauchoBean bean)
  {
    HempBroker broker = HempBroker.getCurrent();
    
    Actor service = (Actor) manager.getReference(bean);

    com.caucho.remote.BamService serviceAnn
      = getAnnotation(bean, com.caucho.remote.BamService.class);

    int threadMax = 1;

    String name = serviceAnn.name();
    if (name == null || "".equals(name))
      name = service.getClass().getSimpleName();

    String jid = name;
    if (jid.indexOf('@') < 0 && jid.indexOf('/') < 0)
      jid = name + "@" + broker.getJid();

    service.setJid(jid);

    service = new MemoryQueueServiceFilter(service,
					   broker,
					   threadMax);

    broker.addActor(service);

    // XXX: shutdown
  }

  private <T> T getAnnotation(CauchoBean bean, Class<T> annType)
  {
    for (Annotation ann : bean.getAnnotations()) {
      if (ann.annotationType().equals(annType))
	return (T) ann;
    }

    return null;
  }
}
