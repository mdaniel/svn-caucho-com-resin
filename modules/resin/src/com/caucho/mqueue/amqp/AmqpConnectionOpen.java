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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.mqueue.amqp;

import java.util.List;
import java.util.Map;

/**
 * AMQP connection-open frame 
 */
public class AmqpConnectionOpen extends AmqpAbstractPacket {
  public static final int CODE = AmqpConstants.FT_CONN_OPEN;
  
  private String _containerId; // required
  private String _hostname;
  private int _maxFrameSize = Integer.MAX_VALUE; 
  private int _channelMax = 65535;
  private long _idleTimeout;
  private List<String> _outgoingLocales;
  private List<String> _incomingLocales;
  private List<String> _offeredCapabilities;
  private List<String> _desiredCapabilities;
  
  private Map _properties;
}
