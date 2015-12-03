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
* @author Emil Ong
*/

package com.caucho.soap.jaxws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.soap.SOAPBinding;

import com.caucho.util.L10N;

public abstract class AbstractBinding implements Binding {
  private final static L10N L = new L10N(AbstractBinding.class);

  private List<Handler> _handlerChain = new ArrayList<Handler>();

  public List<Handler> getHandlerChain()
  {
    return _handlerChain;
  }

  public void setHandlerChain(List<Handler> handlerChain)
  {
    _handlerChain = JAXWSUtil.sortHandlerChain(handlerChain);
  }
}

