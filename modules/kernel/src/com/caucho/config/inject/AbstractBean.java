/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.inject;

import com.caucho.config.annotation.ServiceType;
import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.ObserverImpl;
import com.caucho.config.program.BeanArg;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;
import java.io.*;

import javax.annotation.*;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Named;
import javax.inject.Qualifier;

/**
 * Common bean introspection for Produces and ManagedBean.
 */
abstract public class AbstractBean<T>
  implements Bean<T>, ObjectProxy, AnnotatedBean
{
  private static final L10N L = new L10N(AbstractBean.class);
  private static final Logger log
    = Logger.getLogger(AbstractBean.class.getName());

  private static final Set<Annotation> _currentBindings;
  private static final Set<InjectionPoint> _nullInjectionPoints
    = new HashSet<InjectionPoint>();

  private InjectManager _beanManager;

  private String _passivationId;

  public AbstractBean(InjectManager beanManager)
  {
    _beanManager = beanManager;
  }

  public InjectManager getBeanManager()
  {
    return _beanManager;
  }

  public String getId()
  {
    if (_passivationId == null)
      _passivationId = calculatePassivationId();

    return _passivationId;
  }

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

  abstract public T create(CreationalContext<T> creationalContext);

  public void destroy(T instance, CreationalContext<T> env)
  {
  }

  //
  // metadata for the bean
  //

  abstract public Set<Type> getTypes();

  public Class getBeanClass()
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

  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  protected String calculatePassivationId()
  {
    Sha256OutputStream os = new Sha256OutputStream(new NullOutputStream());
    PrintWriter out = new PrintWriter(new OutputStreamWriter(os));

    // XXX: getTypes?
    out.print(getBeanClass());

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

    out.close();

    return Base64.encodeFromByteArray(os.getDigest());
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
    _currentBindings.add(CurrentLiteral.CURRENT);
  }
}
