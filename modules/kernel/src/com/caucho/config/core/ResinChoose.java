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

package com.caucho.config.core;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import com.caucho.config.annotation.NoAspect;
import com.caucho.config.type.FlowBean;

/**
 * Executes code when an expression is valid.
 */
@NoAspect
public class ResinChoose extends ResinControl implements FlowBean {
  private ArrayList<ResinWhen> _whenList = new ArrayList<ResinWhen>();
  private ResinWhen _otherwise;
  
  /**
   * Adds a new when clause.
   */
  public void addWhen(ResinWhen when)
  {
    _whenList.add(when);
  }
  
  /**
   * Adds the otherwise clause.
   */
  public void setOtherwise(ResinWhen when)
  {
    _otherwise = when;
    _otherwise.setTest(true);
  }

  @PostConstruct
  public void init()
    throws Throwable
  {
    Object object = getObject();
    
    if (object == null)
      return;
    
    for (int i = 0; i < _whenList.size(); i++) {
      ResinWhen when = _whenList.get(i);

      if (when.configure(object))
        return;
    }

    if (_otherwise != null)
      _otherwise.configure(object);
  }
}

