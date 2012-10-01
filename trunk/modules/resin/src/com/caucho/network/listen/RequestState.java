/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is software; you can redistribute it and/or modify
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

package com.caucho.network.listen;

/**
 * Return state for request handling.
 */

enum RequestState {
  REQUEST_COMPLETE {
    @Override
    public boolean isRequestKeepalive() { return true; }
    
    @Override
    public boolean isAcceptAllowed() { return true; }
  },
  
  KEEPALIVE_SELECT {
    @Override
    public boolean isDetach() { return true; }
    
    @Override
    public boolean isKeepaliveSelect() { return true; }
  },
  
  ASYNC {
    @Override
    public boolean isAsyncOrDuplex() { return true; }
    
    @Override
    public boolean isDetach() { return true; }
  },
  
  DUPLEX {
    @Override
    public boolean isAsyncOrDuplex() { return true; }
    
    @Override
    public boolean isDetach() { return true; }
  },
  
  CLOSED {
    @Override
    public boolean isAcceptAllowed() { return true; }
    
    @Override
    public boolean isClosed() { return true; }
  },
  
  EXIT {
    @Override
    public boolean isClosed() { return true; }
  };
  
  public boolean isAsyncOrDuplex()
  {
    return false;
  }
  
  public boolean isDetach()
  {
    return false;
  }
  
  public boolean isRequestKeepalive()
  {
    return false;
  }
  
  public boolean isKeepaliveSelect()
  {
    return false;
  }
  
  public boolean isAcceptAllowed()
  {
    return false;
  }
  
  public boolean isClosed()
  {
    return false;
  }
}
