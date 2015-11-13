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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.util.Nonbinding;

import com.caucho.v5.config.custom.CookieCustomBean;
import com.caucho.v5.util.Base64Util;
import com.caucho.v5.util.Fnv256OutputStream;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.NullOutputStream;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

/**
 * Common bean introspection for Produces and ManagedBean.
 */
abstract public class BeanBase<T>
  implements Bean<T>, ObjectFactoryNaming, AnnotatedBean
{
  private static final L10N L = new L10N(BeanBase.class);
  private static final Logger log
    = Logger.getLogger(BeanBase.class.getName());

  private static final Set<Annotation> _currentBindings;
  private static final Set<InjectionPoint> _nullInjectionPoints
    = new HashSet<InjectionPoint>();

  private CandiManager _beanManager;

  private String _passivationId;

  public BeanBase(CandiManager beanManager)
  {
    _beanManager = beanManager;
  }

  public CandiManager getInjectManager()
  {
    return _beanManager;
  }
  
  public BeanManagerBase getBeanManager()
  {
    return _beanManager.getBeanManager();
  }

  public String getId()
  {
    if (_passivationId == null)
      _passivationId = calculatePassivationId();

    return _passivationId;
  }

  @Override
  public Annotated getAnnotated()
  {
    return null;
  }

  public AnnotatedType<T> getAnnotatedType()
  {
    Annotated annotated = getAnnotated();

    if (annotated instanceof AnnotatedType<?>)
      return (AnnotatedType<T>) annotated;
    else
      return null;
  }

  public InjectionTarget<T> getInjectionTarget()
  {
    return null;
  }

  public void introspect()
  {
  }

  @Override
  abstract public T create(CreationalContext<T> creationalContext);

  public void destroy(T instance, CreationalContext<T> env)
  {
  }

  //
  // metadata for the bean
  //

  abstract public Set<Type> getTypes();

  public Class<?> getBeanClass()
  {
    return null;
  }
  
  public Class<?> getJavaClass()
  {
    return null;
  }

  public Set<Annotation> getQualifiers()
  {
    return _currentBindings;
  }

  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return null;
  }

  public Set<InjectionPoint> getInjectionPoints()
  {
    return _nullInjectionPoints;
  }

  public String getName()
  {
    return null;
  }

  @Override
  public boolean isAlternative()
  {
    return false;
  }

  public boolean isNullable()
  {
    return false;
  }

  public boolean isPassivationCapable()
  {
    return false;
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  protected String calculatePassivationId()
  {
    try {
      Fnv256OutputStream os = new Fnv256OutputStream(new NullOutputStream());
      WriteStream out = Vfs.openWrite(os);

      out.print(getJavaClass());

      ArrayList<String> typeList = new ArrayList<String>();

      for (Type type : getTypes()) {
        typeList.add(type.toString());
      }
      Collections.sort(typeList);

      for (String typeString : typeList) {
        out.print(";");
        out.print(typeString);
      }

      if (getName() != null) {
        out.print(";name=");
        out.print(getName());
      }

      ArrayList<String> annList = new ArrayList<String>();

      for (Annotation ann : getQualifiers()) {
        annList.add(bindingToString(ann));
      }

      Collections.sort(annList);

      for (String annString : annList) {
        out.print(";");
        out.print(annString);
      }
      
      // ioc/0l34
      if (getAnnotatedType() != null) {
        CookieCustomBean cookie = getAnnotatedType().getAnnotation(CookieCustomBean.class);
        
        if (cookie != null) {
          out.print(";cookie=" + cookie.value());
        }
      }

      out.close();

      return Base64Util.encodeFromByteArray(os.getDigest());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private String bindingToString(Annotation ann)
  {
    StringBuilder sb = new StringBuilder(ann.annotationType().getName());

    ArrayList<String> propList = new ArrayList<String>();

    for (Method method : ann.annotationType().getDeclaredMethods()) {
      if (method.getName().equals("annotationType"))
        continue;

      if (method.isAnnotationPresent(Nonbinding.class))
        continue;

      if (method.getParameterTypes().length != 0)
        continue;

      try {
        String prop = method.getName() + "," + method.invoke(ann);

        propList.add(prop);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString());
      }
    }

    Collections.sort(propList);
    for (String prop : propList) {
      sb.append(",").append(prop);
    }

    return sb.toString();
  }

  /**
   * Creates the object from the proxy.
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
  {
    return _beanManager.getReference(this);
  }

  public String toDisplayString()
  {
    if (getInjectionTarget() instanceof DisplayableInjectionTarget)
      return ((DisplayableInjectionTarget) getInjectionTarget()).toDisplayString();
    
    if (getBeanClass() == null)
      return toString();
    
    String display = toDisplayStringImpl();
    
    Class<?> beanClass = getBeanClass();
    
    if (beanClass.getClassLoader() != null) {
      URL url = beanClass.getResource("");
      
      if (url != null)
        return display + "\n  in " + url;
    }
    
    return display;
    
  }
  
  protected String toDisplayStringImpl()
  {
    StringBuilder sb = new StringBuilder();
    
    Class<?> beanClass = getBeanClass();

    sb.append(beanClass.getName());
    sb.append("[");

    ArrayList<Annotation> bindings
      = new ArrayList<Annotation>(getQualifiers());

    for (int i = 0; i < bindings.size(); i++) {
      Annotation ann = bindings.get(i);

      if (i != 0)
        sb.append(", ");

      sb.append(ann);
    }

    if (getName() != null) {
      sb.append(", ");
      sb.append("@Named=");
      sb.append(getName());
    }

    if (getScope() != null && getScope() != Dependent.class) {
      sb.append(", @");
      sb.append(getScope().getSimpleName());
    }
    
    sb.append(", " + getClass().getSimpleName());

    sb.append("]");

    return sb.toString();
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (getBeanClass() != null) {
      sb.append(getBeanClass().getSimpleName());
    }
    
    sb.append(", {");

    ArrayList<Annotation> bindings
      = new ArrayList<Annotation>(getQualifiers());

    for (int i = 0; i < bindings.size(); i++) {
      Annotation ann = bindings.get(i);

      if (i != 0)
        sb.append(", ");

      sb.append(ann);
    }

    sb.append("}");

    if (getName() != null) {
      sb.append(", ");
      sb.append("name=");
      sb.append(getName());
    }

    if (getScope() != null && getScope() != Dependent.class) {
      sb.append(", @");
      sb.append(getScope().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  static {
    _currentBindings = new HashSet<Annotation>();
    _currentBindings.add(DefaultLiteral.DEFAULT);
  }
}
