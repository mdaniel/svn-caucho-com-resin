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

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.Arg;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.cfg.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Producer;

/*
 * Configuration for a @Produces method
 */
public class ProducesFieldBean<X,T> extends AbstractIntrospectedBean<T>
  implements InjectionTarget<T>
{
  private static final L10N L = new L10N(ProducesFieldBean.class);

  private final Bean _producerBean;
  private final AnnotatedField _beanField;

  private Producer<T> _producer;

  private boolean _isBound;

  protected ProducesFieldBean(InjectManager manager,
                              Bean producerBean,
                              AnnotatedField beanField)
  {
    super(manager, beanField.getBaseType(), beanField);

    _producerBean = producerBean;
    _beanField = beanField;

    if (beanField == null)
      throw new NullPointerException();
  }

  public static ProducesFieldBean create(InjectManager manager,
                                         Bean producer,
                                         AnnotatedField beanField)
  {
    ProducesFieldBean bean
      = new ProducesFieldBean(manager, producer, beanField);
    bean.introspect();
    bean.introspect(beanField);

    return bean;
  }

  public Producer<T> getProducer()
  {
    return _producer;
  }

  public void setProducer(Producer<T> producer)
  {
    _producer = producer;
  }

  protected AnnotatedField getField()
  {
    return _beanField;
  }

  @Override
  protected String getDefaultName()
  {
    return _beanField.getJavaMember().getName();
  }

  public boolean isInjectionPoint()
  {
    return false;
  }

  /**
   * Returns the declaring bean
   */
  public Bean<X> getParentBean()
  {
    return _producerBean;
  }

  public T create(CreationalContext<T> createEnv)
  {
    return produce(createEnv);
  }

  public InjectionTarget<T> getInjectionTarget()
  {
    return this;
  }

  /**
   * Produces a new bean instance
   */
  public T produce(CreationalContext<T> cxt)
  {
    Class<?> type = _producerBean.getBeanClass();

    X factory = (X) getBeanManager().getReference(_producerBean, type, cxt);

    if (factory == null) {
      throw new IllegalStateException(L.l("{0}: unexpected null factory for {1}",
                                          this, _producerBean));
    }
    
    CreationalContextImpl<T> env = (CreationalContextImpl<T>) cxt;

    return produce(factory, env.getInjectionPoint());
  }

  /**
   * Produces a new bean instance
   */
  private T produce(X bean, InjectionPoint ij)
  {
    try {
      Field field = _beanField.getJavaMember();
      field.setAccessible(true);
      
      T value = (T) _beanField.getJavaMember().get(bean);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /*
  @Override
  public X instantiate()
  {
    return createNew(null, null);
  }
  */

  public void inject(T instance, CreationalContext<T> cxt)
  {
  }

  public void postConstruct(T instance)
  {
  }

  @Override
  public void bind()
  {
  }

  /*
  public Bean bindInjectionPoint(InjectionPoint ij)
  {
    return new ProducesInjectionPointBean(this, ij);
  }
  */

  /**
   * Disposes a bean instance
   */
  public void preDestroy(T instance)
  {
  }

  /**
   * Returns the owning producer
   */
  public AnnotatedMember<X> getProducerMember()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the owning disposer
   */
  public AnnotatedField<X> getAnnotatedDisposer()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AnnotatedParameter<X> getDisposedParameter()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected InjectionPointBean createInjectionPointBean(BeanManager manager)
  {
    return new InjectionPointBean(manager, null);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    Field field = _beanField.getJavaMember();

    sb.append(getTargetSimpleName());
    sb.append(", ");
    sb.append(field.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(field.getName());
    sb.append("()");

    sb.append(", {");

    boolean isFirst = true;
    for (Annotation ann : getQualifiers()) {
      if (! isFirst)
        sb.append(", ");

      sb.append(ann);

      isFirst = false;
    }

    sb.append("}");

    if (getName() != null) {
      sb.append(", name=");
      sb.append(getName());
    }

    sb.append("]");

    return sb.toString();
  }
}
