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

package com.caucho.config.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ejb.Timer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.ConfigException;
import com.caucho.config.Configured;
import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.event.EventManager;
import com.caucho.config.event.ObserverMethodImpl;
import com.caucho.config.inject.AbstractIntrospectedBean;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionTargetBuilder;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ManagedProducesBuilder;
import com.caucho.config.inject.PassivationSetter;
import com.caucho.config.inject.ProducesBuilder;
import com.caucho.config.inject.ScheduleBean;
import com.caucho.config.inject.ScopeAdapterBean;
import com.caucho.config.timer.ScheduleIntrospector;
import com.caucho.config.timer.TimeoutCaller;
import com.caucho.config.timer.TimerTask;
import com.caucho.inject.Module;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class XmlManagedBeanImpl<X> extends ManagedBeanImpl<X>
{
  private QName _qName;
  private String _filename;
  private int _line;
  
  public XmlManagedBeanImpl(InjectManager injectManager,
                            AnnotatedType<X> beanType,
                            boolean isSessionBean,
                            QName qName,
                            String filename,
                            int line)
  {
    super(injectManager, beanType, isSessionBean);
    
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
