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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.ejb.cfg;

import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.jms.Destination;

public class MessageDestination {
  private final L10N L = new L10N(MessageDestination.class);

  private String _messageDestinationName;
  private String _mappedName;

  private Destination _destination;

  public MessageDestination()
  {
  }

  public void setDescription(String description)
  {
  }

  public void setDisplayName(String displayName)
  {
  }

  public void setIcon(String icon)
  {
  }

  public void setMessageDestinationName(String messageDestinationName)
  {
    _messageDestinationName = messageDestinationName;
  }

  public String getMessageDestinationName()
  {
    return _messageDestinationName;
  }

  public void setMappedName(String mappedName)
  {
    _mappedName = mappedName;
  }

  @PostConstruct
  public void init()
    throws NamingException
  {
  }

  private void resolve()
    throws NamingException
  {
    if (_mappedName == null) {
      _destination = (Destination) Jndi.lookup(_messageDestinationName);

      if (_destination == null) {
        throw new NamingException(L.l("<message-destination> '{0}' could not be resolved",
                                      _messageDestinationName));
      }
    }
    else {
      _destination = (Destination) Jndi.lookup(_mappedName);

      if (_destination == null) {
        throw new NamingException(L.l("<message-destination> '{0}' with mapped-name '{1}' could not be resolved",
                                      _messageDestinationName, _mappedName));
      }
    }
  }

  public Destination getResolvedDestination()
    throws NamingException
  {
    if (_destination == null)
      resolve();

    return _destination;
  }

  public String toString()
  {
    return getClass().getSimpleName() + " [" + _messageDestinationName + "]";
  }
}
