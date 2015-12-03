/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.jca.ra;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * Convenience class for the ResourceAdapter.
 */
public class AbstractResourceAdapter implements ResourceAdapter {
  /**
   * Called when the resource starts.
   */
  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
  }
  
  /**
   * Called when the resource adapter is stopped.
   */
  public void stop()
  {
  }

  /**
   * Called during activation of a message endpoint.
   */
  public void endpointActivation(MessageEndpointFactory endpointFactory,
                                 ActivationSpec spec)
    throws NotSupportedException, ResourceException
  {
  }
  
  /**
   * Called during deactivation of a message endpoint.
   */
  public void endpointDeactivation(MessageEndpointFactory endpointFactory,
                                   ActivationSpec spec)
  {
  }
  
  /**
   * Called during crash recovery.
   */
  public XAResource []getXAResources(ActivationSpec []specs)
    throws ResourceException
  {
    return null;
  }
}
