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

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.annotation.ProxyProduces;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.http.security.AuthenticatorRole;
import com.caucho.v5.inject.Module;

/**
 * Configuration for the cdi bean component.
 */
@Module
public class WebComponent {
  private CandiManager _injectManager;
  private BeanManagerBase _beanManager;

  private String _className;

  private BeanEntry _injectionPointEntry;

  private ArrayList<BeanEntry> _beanList = new ArrayList<BeanEntry>();

  public WebComponent(CandiManager injectManager,
                      BeanManagerBase beanManager,
                      String className)
  {
    _injectManager = injectManager;
    _beanManager = beanManager;
    _className = className;
  }

  public void addComponent(BaseType type, Annotated annotated, Bean<?> bean)
  {
    for (BeanEntry beanEntry : _beanList) {
      if (beanEntry.getType().equals(type) && beanEntry.isMatch(bean)) {
        return;
      }
    }

    if (bean instanceof ProducesMethodBean<?,?>
        && ((ProducesMethodBean<?,?>) bean).isInjectionPoint()) {
      _injectionPointEntry = new BeanEntry(type, annotated, bean);
    }
    
    _beanList.add(new BeanEntry(type, annotated, bean));
    
    Collections.sort(_beanList);
  }
  
  public void resolveSpecializes()
  {
    for (int i = _beanList.size() - 1; i >= 0; i--) {
      BeanEntry entry = _beanList.get(i);
      
      Annotated ann = entry.getAnnotated();
      
      if (ann == null || ! ann.isAnnotationPresent(Specializes.class)) {
        continue;
      }
    }
  }

  public void createProgram(ArrayList<ConfigProgram> initList,
                            Field field,
                            ArrayList<Annotation> bindList)
    throws ConfigException
  {
    /*
    ComponentImpl comp = bind(WebBeansContainer.location(field), bindList);

    comp.createProgram(initList, field);
    */
  }

  public Set<Bean<?>> resolve(Type type, Annotation []bindings)
  {
    BaseType baseType = _injectManager.createTargetBaseType(type);

    return resolve(baseType, bindings, null);
  }

  public Set<Bean<?>> resolve(Type type, 
                              Annotation []bindings,
                              InjectionPoint ij)
  {
    BaseType baseType = _injectManager.createTargetBaseType(type);

    return resolve(baseType, bindings, ij);
  }

  public Set<Bean<?>> resolve(BaseType type, 
                              Annotation []qualifiers,
                              InjectionPoint ij)
  {
    LinkedHashSet<Bean<?>> beans = null;

    // ioc/0775, #4649
    /*
    if (_injectionPointEntry != null) {
      beans = new LinkedHashSet<Bean<?>>();
      beans.add(_injectionPointEntry.getBean());
      return beans;
    }
    */
    
    boolean isVariable = ! type.isGenericRaw();

    for (BeanEntry beanEntry : _beanList) {
      if (beanEntry.isMatch(type, qualifiers)) {
        if (beans == null) {
          beans = new LinkedHashSet<>();
        }
        
        if (isVariable && ! beanEntry.getType().isGenericVariable()) {
          // ioc/024k
          isVariable = false;
          beans.clear();
        }
        
        if (ij == null && isProxy(beanEntry.getBean())) {
          // skip @Lookup style proxies unless injection point
          
          continue;
        }

        beans.add(beanEntry.getBean());
      }
    }

    return beans;
  }
  
  /**
   * Skip proxy @Loookup from beans query to avoid false positives.
   */
  private boolean isProxy(Bean<?> bean)
  {
    if (! (bean instanceof ProducesMethodBean)) {
      return false;
    }
    
    ProducesMethodBean<?,?> mBean = (ProducesMethodBean<?,?>) bean;
    
    Annotated annType = mBean.getAnnotated();
    
    if (annType != null && annType.isAnnotationPresent(ProxyProduces.class)) {
      return true;
    }
    
    return false;
  }

  public ArrayList<Bean<?>> getBeanList()
  {
    ArrayList<Bean<?>> list = new ArrayList<Bean<?>>();

    for (BeanEntry beanEntry : _beanList) {
      Bean<?> bean = beanEntry.getBean();

      if (! list.contains(bean)) {
        list.add(bean);
      }
    }

    return list;
  }

  public ArrayList<Bean<?>> getEnabledBeanList()
  {
    ArrayList<Bean<?>> list = new ArrayList<Bean<?>>();

    int priority = 0;

    for (BeanEntry beanEntry : _beanList) {
      Bean<?> bean = beanEntry.getBean();

      int beanPriority = _beanManager.getDeploymentPriority(bean);

      if (priority <= beanPriority) {
        list.add(bean);
      }
    }

    return list;
  }

  /**
   * 
   */
  public void validate()
  {
    for (BeanEntry beanEntry : _beanList) {
      Bean<?> bean = beanEntry.getBean();

      int beanPriority = _beanManager.getDeploymentPriority(bean);

      if (beanPriority >= 0) {
        // validation
        for (InjectionPoint ip : bean.getInjectionPoints()) {
          _injectManager.validate(ip);
        }
      }
    }
  }

  static String getName(Type type)
  {
    if (type instanceof Class<?>)
      return ((Class<?>) type).getName();
    else
      return String.valueOf(type);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _className + "]";
  }

  class BeanEntry implements Comparable<BeanEntry> {
    private Bean<?> _bean;
    private BaseType _type;
    private Annotated _annotated;
    private QualifierBinding []_qualifiers;

    BeanEntry(BaseType type,
              Annotated annotated,
              Bean<?> bean)
    {
      _type = type;
      _annotated = annotated;
      _bean = bean;

      Set<Annotation> qualifiers = bean.getQualifiers();

      _qualifiers = new QualifierBinding[qualifiers.size()];

      int i = 0;
      for (Annotation qualifier : qualifiers) {
        _qualifiers[i++] = new QualifierBinding(qualifier);
      }
    }

    Bean<?> getBean()
    {
      return _bean;
    }

    Annotated getAnnotated()
    {
      return _annotated;
    }
    
    BaseType getType()
    {
      return _type;
    }

    boolean isMatch(Bean<?> bean)
    {
      // ioc/0213
      return _bean == bean;
    }

    boolean isMatch(BaseType type, Annotation []qualifiers)
    {
      return isMatch(type) && isMatch(qualifiers);
    }

    boolean isMatch(BaseType type)
    {
      boolean isMatch = type.isAssignableFrom(_type);
      
      return isMatch;
    }

    boolean isMatch(Annotation []qualifierArgs)
    {
      for (Annotation arg : qualifierArgs) {
        if (! isMatch(arg)) {
          // ioc/0270
          /*
          if (! arg.annotationType().isAnnotationPresent(Qualifier.class)) {
            throw new ConfigException(L.l("'{0}' is an invalid binding annotation because it does not have a @Qualifier meta-annotation.",
                                          arg));
          }
          */

          return false;
        }
      }

      return true;
    }

    private boolean isMatch(Annotation arg)
    {
      if (arg.annotationType() == Any.class) {
        return true;
      }

      for (QualifierBinding binding : _qualifiers) {
        if (binding.isMatch(arg)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public int compareTo(BeanEntry test)
    {
      Bean<?> bean = test._bean;
      
      String beanClassName = _bean.getBeanClass().getName();
      String beanClassNameTest = bean.getBeanClass().getName(); 
      
      int beanTypeCompare = beanClassName.compareTo(beanClassNameTest);
      
      if (beanTypeCompare != 0) {
        return beanTypeCompare;
      }
      
      Iterator<Annotation> qualifierIterA = _bean.getQualifiers().iterator();
      Iterator<Annotation> qualifierIterB = bean.getQualifiers().iterator();
      
      while (qualifierIterA.hasNext() && qualifierIterB.hasNext()) {
        Annotation qualifierA = qualifierIterA.next();
        Annotation qualifierB = qualifierIterB.next();
        
        String annTypeA = qualifierA.annotationType().getName();
        String annTypeB = qualifierB.annotationType().getName();
        
        int cmp = annTypeA.compareTo(annTypeB);
        
        if (cmp != 0) {
          return cmp;
        }
      }
      
      return _bean.getClass().getName().compareTo(bean.getClass().getName());
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _bean + "]";
    }
  }
}
