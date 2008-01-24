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

package com.caucho.webbeans.manager;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.util.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.event.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;

/**
 * Matches bindings
 */
public class ObserverMap {
  private static L10N L = new L10N(ObserverMap.class);
  
  private Class _type;

  private ArrayList<ObserverImpl> _observerList
    = new ArrayList<ObserverImpl>();

  public ObserverMap(Class type)
  {
    _type = type;
  }

  public void addObserver(ObserverImpl observer)
  {
    for (int i = _observerList.size() - 1; i >= 0; i--) {
      ObserverImpl oldObserver = _observerList.get(i);

      if (observer.equals(oldObserver)) {
	return;
      }
    }

    _observerList.add(observer);
  }

  public void raiseEvent(Object event, Annotation []bindList)
  {
    for (int i = 0; i < _observerList.size(); i++) {
      ObserverImpl observer = _observerList.get(i);

      if (observer.isMatch(bindList)) {
	observer.raiseEvent(event);
      }
    }
  }
}
