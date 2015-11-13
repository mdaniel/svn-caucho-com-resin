/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.custom;

import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.v5.config.candi.BeanManagerBase;
import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.ManagedBeanImpl;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.inject.Module;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class ManagedBeanCustom<X> extends ManagedBeanImpl<X>
{
  private NameCfg _qName;
  private String _filename;
  private int _line;
  
  public ManagedBeanCustom(CandiManager injectManager,
                           BeanManagerBase beanManager,
                           AnnotatedType<X> beanType,
                           boolean isSessionBean,
                            NameCfg qName,
                            String filename,
                            int line)
  {
    super(injectManager, beanManager, beanType, isSessionBean);
    
    _qName = qName;
    _filename = filename;
    _line = line;
  }
  
  @Override
  public String toDisplayString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(toDisplayStringImpl());
    
    sb.append("\n  in ");
    
    if (_filename != null)
      sb.append(_filename + ":" + _line + ": ");
    
    sb.append("<").append(_qName.getName()).append(">");
    
    
    return sb.toString();
  }
}
