/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.soa.encoding;

import com.caucho.config.ConfigurationException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Logger;

/**
 * Invokes a service Provider.
 */
public abstract class ProviderEncoding implements ServiceEncoding {
  private static final L10N L = new L10N(ProviderEncoding.class);
  private static final Logger log =
    Logger.getLogger(ProviderEncoding.class.getName());

  protected final Class _class;
  protected final Provider _provider;

  protected ProviderEncoding(Object service)
    throws ConfigurationException
  {
    _provider = (Provider) service;
    _class = service.getClass();
  }

  public static ProviderEncoding createProviderEncoding(Object service)
    throws ConfigurationException
  {
    Class cl = service.getClass();
    Type[] interfaces = cl.getGenericInterfaces();

    for (int i = 0; i < interfaces.length; i++) {
      if (interfaces[i] instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) interfaces[i];

        if (Provider.class.equals(pType.getRawType())) {
          Type[] args = pType.getActualTypeArguments();

          if (args.length == 1) {
            if (Source.class.equals(args[0]))
              return new SourceProviderEncoding(service);
            else if (SOAPMessage.class.equals(args[0]))
              return new SOAPMessageProviderEncoding(service);
          }
        }
      }
    }

    throw new ConfigurationException(L.l("Class {0} does not implement valid Provider", cl.getName()));
  }

  public void setService(Object service)
  {
  }

  @PostConstruct
  public void init()
  {
  }
}
