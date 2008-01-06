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

package com.caucho.iiop.orb;

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;

public class IiopSocketPool
{
  private String _host;
  private int _port;

  private Path _path;

  private ReadWritePair _pair;
  private long _lastFreeTime;

  private long _maxIdleTime = 10000L;

  public IiopSocketPool(String host, int port)
  {
    _host = host;
    _port = port;
    HashMap<String,Object> props = new HashMap<String,Object>();
    props.put("timeout", new Long(600000));
    _path = Vfs.lookup("tcp://" + host + ":" + port, props);
  }

  public ReadWritePair open()
    throws IOException
  {
    synchronized (this) {
      long now = Alarm.getCurrentTime();

      ReadWritePair pair = _pair;
      _pair = null;
      
      if (pair != null && now < _lastFreeTime + _maxIdleTime) {
	return pair;
      }
      else if (pair != null) {
	closePair(pair);
      }
    }

    return _path.openReadWrite();
  }

  public void free(ReadWritePair pair)
  {
    ReadWritePair closePair = null;
    
    synchronized (this) {
      closePair = _pair;
      _pair = pair;

      _lastFreeTime = Alarm.getCurrentTime();
    }

    if (closePair != null)
      closePair(closePair);
  }

  private static void closePair(ReadWritePair pair)
  {
    pair.getReadStream().close();

    try {
      pair.getWriteStream().close();
    } catch (IOException e) {
    }
  }

  public String toString()
  {
    return "IiopSocketPool[" + _host + ", " + _port + "]";
  }
}
